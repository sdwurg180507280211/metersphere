package io.metersphere.service;

import io.metersphere.base.domain.CustomField;
import io.metersphere.base.domain.IssuesWithBLOBs;
import io.metersphere.base.mapper.IssuesMapper;
import io.metersphere.commons.constants.IssuesManagePlatform;
import io.metersphere.commons.utils.JSON;
import io.metersphere.constants.SystemCustomField;
import io.metersphere.dto.CustomFieldResourceDTO;
import io.metersphere.xpack.track.dto.request.IssuesUpdateRequest;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 将缺陷编辑和 Excel 覆盖导入中的状态修改统一收敛到状态流转服务。
 *
 * Local 缺陷的“状态”字段从普通字段更新中剥离，先通过 IssueStatusTransitionService 完成状态流转，
 * 再保存其它普通字段。这样可以统一执行责任人校验、流转规则、reopened 复测次数累加和流转历史记录，
 * 同时避免非法状态流转发生在普通字段或关联关系已经写入之后。
 */
@Aspect
@Component
public class IssueUpdateStatusTransitionAspect {

    @Resource
    private IssuesMapper issuesMapper;
    @Resource
    private BaseCustomFieldService baseCustomFieldService;
    @Resource
    private IssueStatusTransitionService issueStatusTransitionService;
    @Resource
    private PlatformTransactionManager transactionManager;

    @Around("execution(* io.metersphere.service.IssuesService.updateIssues(..)) && args(request)")
    public Object aroundUpdateIssue(ProceedingJoinPoint joinPoint, IssuesUpdateRequest request) throws Throwable {
        StatusChange statusChange = prepareStatusChange(request);
        if (statusChange == null) {
            return joinPoint.proceed();
        }

        try {
            return executeInTransaction(() -> {
                if (statusChange.needTransition()) {
                    // 状态先流转：非法流转在普通字段和关联关系写入前直接失败。
                    issueStatusTransitionService.transitionStatus(statusChange.issueId, statusChange.targetStatus, null);
                }
                Object result = joinPoint.proceed();
                if (statusChange.needTransition()) {
                    return ((IssuesService) joinPoint.getTarget()).getIssue(statusChange.issueId);
                }
                return result;
            });
        } finally {
            statusChange.restoreFields();
        }
    }

    @Around("execution(* io.metersphere.service.IssuesService.updateImportData(..)) && args(requests)")
    public Object aroundUpdateImport(ProceedingJoinPoint joinPoint, List<IssuesUpdateRequest> requests) throws Throwable {
        if (CollectionUtils.isEmpty(requests)) {
            return joinPoint.proceed();
        }

        List<StatusChange> preparedChanges = new ArrayList<>();
        Map<String, StatusChange> transitionChanges = new LinkedHashMap<>();
        for (IssuesUpdateRequest request : requests) {
            StatusChange statusChange = prepareStatusChange(request);
            if (statusChange != null) {
                preparedChanges.add(statusChange);
                // 同一缺陷在导入文件中重复出现时，以最后一行的目标状态为准，避免重复流转。
                transitionChanges.put(statusChange.issueId, statusChange);
            }
        }

        if (preparedChanges.isEmpty()) {
            return joinPoint.proceed();
        }

        try {
            return executeInTransaction(() -> {
                // Excel 覆盖导入同样先完成状态流转，再执行普通字段批量更新。
                for (StatusChange statusChange : transitionChanges.values()) {
                    if (statusChange.needTransition()) {
                        issueStatusTransitionService.transitionStatus(statusChange.issueId, statusChange.targetStatus, null);
                    }
                }
                return joinPoint.proceed();
            });
        } finally {
            preparedChanges.forEach(StatusChange::restoreFields);
        }
    }

    private StatusChange prepareStatusChange(IssuesUpdateRequest request) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            return null;
        }

        IssuesWithBLOBs issue = issuesMapper.selectByPrimaryKey(request.getId());
        if (issue == null || (StringUtils.isNotBlank(issue.getPlatform())
                && !StringUtils.equalsIgnoreCase(issue.getPlatform(), IssuesManagePlatform.Local.toString()))) {
            return null;
        }

        CustomField statusField = baseCustomFieldService.getCustomFieldByName(issue.getProjectId(), SystemCustomField.ISSUE_STATUS);
        if (statusField == null) {
            return null;
        }

        String targetStatus = getStatusValue(request.getEditFields(), statusField.getId());
        if (StringUtils.isBlank(targetStatus)) {
            targetStatus = getStatusValue(request.getAddFields(), statusField.getId());
        }
        if (StringUtils.isBlank(targetStatus)) {
            return null;
        }

        StatusChange statusChange = new StatusChange();
        statusChange.request = request;
        statusChange.issueId = issue.getId();
        statusChange.currentStatus = issueStatusTransitionService.getCurrentStatus(issue);
        statusChange.targetStatus = targetStatus;
        statusChange.originalEditFields = request.getEditFields();
        statusChange.originalAddFields = request.getAddFields();

        request.setEditFields(removeStatusField(request.getEditFields(), statusField.getId()));
        request.setAddFields(removeStatusField(request.getAddFields(), statusField.getId()));
        return statusChange;
    }

    private String getStatusValue(List<CustomFieldResourceDTO> fields, String statusFieldId) {
        if (CollectionUtils.isEmpty(fields)) {
            return null;
        }
        for (CustomFieldResourceDTO field : fields) {
            if (isStatusField(field, statusFieldId)) {
                return parseStatusValue(field.getValue());
            }
        }
        return null;
    }

    private List<CustomFieldResourceDTO> removeStatusField(List<CustomFieldResourceDTO> fields, String statusFieldId) {
        if (CollectionUtils.isEmpty(fields)) {
            return fields;
        }
        return fields.stream()
                .filter(field -> !isStatusField(field, statusFieldId))
                .collect(Collectors.toList());
    }

    private boolean isStatusField(CustomFieldResourceDTO field, String statusFieldId) {
        return field != null && (StringUtils.equals(field.getFieldId(), statusFieldId)
                || StringUtils.equals(field.getName(), SystemCustomField.ISSUE_STATUS));
    }

    private String parseStatusValue(String value) {
        if (StringUtils.isBlank(value) || StringUtils.equalsAny(value, "null", "[]")) {
            return null;
        }
        try {
            return JSON.parseObject(value, String.class);
        } catch (Exception ignore) {
            String status = value.trim();
            if (status.startsWith("\"") && status.endsWith("\"") && status.length() >= 2) {
                status = status.substring(1, status.length() - 1);
            }
            return status;
        }
    }

    private Object executeInTransaction(TransactionalOperation operation) throws Throwable {
        AtomicReference<Throwable> error = new AtomicReference<>();
        Object result = new TransactionTemplate(transactionManager).execute(status -> {
            try {
                return operation.execute();
            } catch (Throwable throwable) {
                status.setRollbackOnly();
                error.set(throwable);
                return null;
            }
        });
        if (error.get() != null) {
            throw error.get();
        }
        return result;
    }

    @FunctionalInterface
    private interface TransactionalOperation {
        Object execute() throws Throwable;
    }

    private static class StatusChange {
        private IssuesUpdateRequest request;
        private String issueId;
        private String currentStatus;
        private String targetStatus;
        private List<CustomFieldResourceDTO> originalEditFields;
        private List<CustomFieldResourceDTO> originalAddFields;

        private boolean needTransition() {
            return !StringUtils.equals(currentStatus, targetStatus);
        }

        private void restoreFields() {
            request.setEditFields(originalEditFields);
            request.setAddFields(originalAddFields);
        }
    }
}
