package io.metersphere.requirement.sync.service;

import io.metersphere.base.domain.Project;
import io.metersphere.base.domain.TestPlan;
import io.metersphere.base.domain.TestPlanNode;
import io.metersphere.base.domain.TestPlanPrincipal;
import io.metersphere.base.mapper.ProjectMapper;
import io.metersphere.base.mapper.TestPlanNodeMapper;
import io.metersphere.base.mapper.TestPlanPrincipalMapper;
import io.metersphere.base.mapper.ext.RequirementTestPlanSyncMapper;
import io.metersphere.commons.constants.TestPlanStatus;
import io.metersphere.commons.exception.MSException;
import io.metersphere.requirement.pool.dto.RequirementSyncMessage;
import io.metersphere.requirement.sync.dto.RequirementSystemMapping;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class RequirementTestPlanSyncService {

    private static final String OPERATION_CREATED = "CREATED";
    private static final String OPERATION_UPDATED = "UPDATED";
    private static final String OPERATION_CANCELLED = "CANCELLED";

    private static final String INT_STAGE_TEST_PLAN = "TEST_PLAN";
    private static final String INT_STAGE_TEST_PREPARATION = "TEST_PREPARATION";
    private static final String INT_STAGE_TEST_EXECUTION = "TEST_EXECUTION";
    private static final String INT_STAGE_DONE = "DONE";

    private static final String APPROVAL_APPROVED = "APPROVED";
    private static final String APPROVAL_REJECTED = "REJECTED";

    @Resource
    private RequirementTestPlanSyncMapper requirementTestPlanSyncMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private TestPlanNodeMapper testPlanNodeMapper;
    @Resource
    private TestPlanPrincipalMapper testPlanPrincipalMapper;

    /**
     * 正式的全流程平台需求同步入口。
     * 需求池不再参与主流程，消息直接创建、更新或取消测试计划。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleSyncMessage(RequirementSyncMessage msg) {
        if (msg == null) {
            return;
        }
        ensureTraceId(msg);
        String dmpNum = StringUtils.trimToEmpty(msg.getDmpNum());
        if (StringUtils.isBlank(dmpNum)) {
            MSException.throwException("全流程平台需求消息缺少需求编号");
        }

        // 审批结果由全流程平台回传，同样走需求同步 Topic，但不要求 operationType。
        if (StringUtils.isNotBlank(msg.getApprovalStatus())) {
            handleApproval(msg, dmpNum);
            return;
        }

        String operationType = StringUtils.upperCase(StringUtils.trimToEmpty(msg.getOperationType()));
        if (StringUtils.isBlank(operationType)) {
            MSException.throwException("全流程平台需求消息缺少操作类型");
        }
        if (msg.getEventTime() == null) {
            MSException.throwException("全流程平台需求消息缺少事件时间");
        }

        TestPlan existing = requirementTestPlanSyncMapper.selectByRequirementNumber(dmpNum);
        if (isOldOrDuplicateMessage(existing, msg.getEventTime())) {
            log.info("[需求MQ-幂等丢弃] dmpNum={}, operationType={}, incomingEventTime={}, currentEventTime={}, traceId={}",
                    dmpNum, operationType, msg.getEventTime(), existing.getRequirementSyncEventTime(), msg.getTraceId());
            return;
        }

        switch (operationType) {
            case OPERATION_CREATED:
                if (existing == null) {
                    createSyncedTestPlan(msg, dmpNum);
                } else {
                    updateSyncedTestPlan(msg, existing);
                }
                break;
            case OPERATION_UPDATED:
                if (existing == null) {
                    createSyncedTestPlan(msg, dmpNum);
                } else {
                    updateSyncedTestPlan(msg, existing);
                }
                break;
            case OPERATION_CANCELLED:
                cancelSyncedTestPlan(msg, existing, dmpNum);
                break;
            default:
                log.warn("[需求MQ-忽略] 未知operationType, dmpNum={}, operationType={}, traceId={}",
                        dmpNum, operationType, msg.getTraceId());
                break;
        }
    }

    private boolean isOldOrDuplicateMessage(TestPlan existing, Long eventTime) {
        return existing != null
                && existing.getRequirementSyncEventTime() != null
                && eventTime != null
                && eventTime <= existing.getRequirementSyncEventTime();
    }

    private void createSyncedTestPlan(RequirementSyncMessage msg, String dmpNum) {
        String requirementName = StringUtils.trimToEmpty(msg.getName1());
        String systemName = StringUtils.trimToEmpty(msg.getSystemName());
        if (StringUtils.isBlank(requirementName)) {
            MSException.throwException("全流程平台需求消息缺少需求名称");
        }
        if (StringUtils.isBlank(systemName)) {
            MSException.throwException("全流程平台需求消息缺少所属系统");
        }

        RequirementSystemMapping mapping = requirementTestPlanSyncMapper.selectEnabledSystemMapping(systemName);
        if (mapping == null) {
            MSException.throwException("所属系统未配置测试计划映射：" + systemName);
        }
        if (StringUtils.isAnyBlank(mapping.getProjectId(), mapping.getNodeId(), mapping.getPrincipalId())) {
            MSException.throwException("所属系统映射不完整：" + systemName);
        }

        Project project = projectMapper.selectByPrimaryKey(mapping.getProjectId());
        if (project == null) {
            MSException.throwException("所属系统映射的项目不存在：" + mapping.getProjectId());
        }
        TestPlanNode node = testPlanNodeMapper.selectByPrimaryKey(mapping.getNodeId());
        if (node == null) {
            MSException.throwException("所属系统映射的测试计划模块不存在：" + mapping.getNodeId());
        }
        if (!StringUtils.equals(project.getId(), node.getProjectId())) {
            MSException.throwException("所属系统映射的项目与模块不一致：" + systemName);
        }

        long now = System.currentTimeMillis();
        TestPlan plan = new TestPlan();
        plan.setId(UUID.randomUUID().toString());
        plan.setWorkspaceId(project.getWorkspaceId());
        plan.setName(requirementName);
        plan.setStatus(TestPlanStatus.Prepare.name());
        plan.setCreateTime(now);
        plan.setUpdateTime(now);
        plan.setCreator(mapping.getPrincipalId());
        plan.setProjectId(project.getId());
        plan.setExecutionTimes(0);
        plan.setAutomaticStatusUpdate(false);
        plan.setRepeatCase(false);
        plan.setNodeId(node.getId());
        plan.setNodePath(buildNodePath(node));
        plan.setRequirementNumber(dmpNum);
        plan.setRequirementDocUrl(StringUtils.trimToNull(msg.getDocUrl()));
        plan.setRequirementSystemName(systemName);
        plan.setRequirementSyncEventTime(msg.getEventTime());
        plan.setIntStage(StringUtils.defaultIfBlank(resolveIntStage(msg.getActName()), INT_STAGE_TEST_PLAN));

        requirementTestPlanSyncMapper.insertSyncedTestPlan(plan);

        TestPlanPrincipal principal = new TestPlanPrincipal();
        principal.setTestPlanId(plan.getId());
        principal.setPrincipalId(mapping.getPrincipalId());
        testPlanPrincipalMapper.insert(principal);

        log.info("[需求MQ-自动建计划] dmpNum={}, planId={}, projectId={}, nodeId={}, intStage={}, traceId={}",
                dmpNum, plan.getId(), plan.getProjectId(), plan.getNodeId(), plan.getIntStage(), msg.getTraceId());
    }

    private void updateSyncedTestPlan(RequirementSyncMessage msg, TestPlan existing) {
        long now = System.currentTimeMillis();
        TestPlan update = new TestPlan();
        update.setId(existing.getId());
        update.setName(StringUtils.trimToNull(msg.getName1()));
        update.setRequirementDocUrl(StringUtils.trimToNull(msg.getDocUrl()));
        update.setRequirementSystemName(StringUtils.trimToNull(msg.getSystemName()));
        update.setRequirementSyncEventTime(msg.getEventTime());
        update.setUpdateTime(now);

        if (!StringUtils.equalsAny(existing.getStatus(), TestPlanStatus.Cancelled.name(), TestPlanStatus.Archived.name())) {
            update.setIntStage(resolveIntStage(msg.getActName()));
        }

        requirementTestPlanSyncMapper.updateSyncedTestPlan(update);
        log.info("[需求MQ-更新计划] dmpNum={}, planId={}, intStage={}, traceId={}",
                existing.getRequirementNumber(), existing.getId(), update.getIntStage(), msg.getTraceId());
    }

    private void cancelSyncedTestPlan(RequirementSyncMessage msg, TestPlan existing, String dmpNum) {
        if (existing == null) {
            log.warn("[需求MQ-取消忽略] 未找到关联测试计划, dmpNum={}, traceId={}", dmpNum, msg.getTraceId());
            return;
        }
        requirementTestPlanSyncMapper.cancelSyncedTestPlan(existing.getId(), msg.getEventTime(), System.currentTimeMillis());
        log.info("[需求MQ-取消计划] dmpNum={}, planId={}, traceId={}", dmpNum, existing.getId(), msg.getTraceId());
    }

    private void handleApproval(RequirementSyncMessage msg, String dmpNum) {
        TestPlan existing = requirementTestPlanSyncMapper.selectByRequirementNumber(dmpNum);
        if (existing == null) {
            MSException.throwException("审批结果未找到关联测试计划：" + dmpNum);
        }

        long approvalTime = msg.getApprovalTime() != null ? msg.getApprovalTime() : System.currentTimeMillis();
        if (existing.getRequirementApprovalTime() != null && approvalTime <= existing.getRequirementApprovalTime()) {
            log.info("[需求MQ-审批幂等丢弃] dmpNum={}, approvalTime={}, currentApprovalTime={}, traceId={}",
                    dmpNum, approvalTime, existing.getRequirementApprovalTime(), msg.getTraceId());
            return;
        }

        String approvalStatus = StringUtils.upperCase(StringUtils.trimToEmpty(msg.getApprovalStatus()));
        String intStage;
        if (APPROVAL_APPROVED.equals(approvalStatus)) {
            intStage = INT_STAGE_TEST_PREPARATION;
        } else if (APPROVAL_REJECTED.equals(approvalStatus)) {
            intStage = INT_STAGE_TEST_PLAN;
        } else {
            log.warn("[需求MQ-审批忽略] 未知审批结果, dmpNum={}, approvalStatus={}, traceId={}",
                    dmpNum, approvalStatus, msg.getTraceId());
            return;
        }

        requirementTestPlanSyncMapper.updateApproval(
                existing.getId(),
                approvalStatus,
                StringUtils.trimToNull(msg.getApprovalComment()),
                approvalTime,
                intStage,
                System.currentTimeMillis()
        );
        log.info("[需求MQ-审批结果] dmpNum={}, planId={}, approvalStatus={}, intStage={}, traceId={}",
                dmpNum, existing.getId(), approvalStatus, intStage, msg.getTraceId());
    }

    private String resolveIntStage(String actName) {
        String value = StringUtils.trimToEmpty(actName);
        switch (value) {
            case "测试计划":
            case "计划审批":
            case "退回计划编制":
                return INT_STAGE_TEST_PLAN;
            case "测试准备":
                return INT_STAGE_TEST_PREPARATION;
            case "测试执行":
            case "测试执行环节":
                return INT_STAGE_TEST_EXECUTION;
            case "测试完成":
            case "办结":
                return INT_STAGE_DONE;
            default:
                return null;
        }
    }

    private String buildNodePath(TestPlanNode node) {
        LinkedList<String> names = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        TestPlanNode current = node;
        while (current != null && StringUtils.isNotBlank(current.getId()) && visited.add(current.getId())) {
            if (StringUtils.isNotBlank(current.getName())) {
                names.addFirst(current.getName());
            }
            if (StringUtils.isBlank(current.getParentId())) {
                break;
            }
            current = testPlanNodeMapper.selectByPrimaryKey(current.getParentId());
        }
        return String.join("/", names);
    }

    private void ensureTraceId(RequirementSyncMessage msg) {
        if (StringUtils.isBlank(msg.getTraceId())) {
            msg.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        }
    }
}
