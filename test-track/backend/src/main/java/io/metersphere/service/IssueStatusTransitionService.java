package io.metersphere.service;

import io.metersphere.base.domain.CustomField;
import io.metersphere.base.domain.CustomFieldIssues;
import io.metersphere.base.domain.CustomFieldIssuesExample;
import io.metersphere.base.domain.Issues;
import io.metersphere.base.domain.IssuesWithBLOBs;
import io.metersphere.base.mapper.CustomFieldIssuesMapper;
import io.metersphere.base.mapper.IssuesMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.JSON;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.commons.user.SessionUser;
import io.metersphere.constants.SystemCustomField;
import io.metersphere.dto.IssueChangeLogDetailDTO;
import io.metersphere.dto.CustomFieldResourceDTO;
import io.metersphere.i18n.Translator;
import io.metersphere.commons.utils.LogUtil;
import java.util.Collections;
import java.util.UUID;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;

@Service
@Transactional(rollbackFor = Exception.class)
public class IssueStatusTransitionService {

    @Resource
    private IssueChangeLogService issueChangeLogService;

    @Resource
    private IssuesMapper issuesMapper;

    @Resource
    private BaseCustomFieldService baseCustomFieldService;

    @Resource
    private CustomFieldIssuesMapper customFieldIssuesMapper;

    @Resource
    private CustomFieldIssuesService customFieldIssuesService;

    // 处理人字段ID
    private static final String PROCESSOR_FIELD_ID = "a577bc60-75fe-47ec-8aa6-32dca23bf3d6";
    // 创建人字段ID
    private static final String CREATOR_FIELD_ID = "09642424-7b1b-4004-867e-ff9c798a1933";
    
    // 状态责任人类型
    private static final String CREATOR = "CREATOR";
    private static final String PROCESSOR = "PROCESSOR";
    
    // 状态责任人定义：status -> ownerType (CREATOR或PROCESSOR)
    private static final Map<String, String> STATUS_OWNER = new HashMap<String, String>() {{
        put("new", CREATOR);           // 创建人负责：测试人员创建缺陷
        put("closed", CREATOR);        // 创建人负责：测试人员验证关闭
        put("reopened", CREATOR);      // 创建人负责：测试人员重新打开
        put("cancelled", CREATOR);     // 创建人负责：测试人员取消
        put("accepted", PROCESSOR);    // 处理人负责：开发人员接收缺陷
        put("resolved", PROCESSOR);    // 处理人负责：开发人员解决缺陷
        put("invalid", PROCESSOR);     // 处理人负责：开发人员判断无效
        put("on_hold", PROCESSOR);     // 处理人负责：开发人员挂起
    }};

    // 状态流转规则定义：fromStatus -> [toStatus1, toStatus2, ...]
    private static final Map<String, List<String>> TRANSITION_RULES = new HashMap<String, List<String>>() {{
        put("new", Arrays.asList("accepted"));  // 新建只能流转到已接收
        put("accepted", Arrays.asList("resolved", "invalid", "on_hold"));  // 已接收可以流转到已解决、无效、挂起
        put("resolved", Arrays.asList("closed", "reopened"));  // 已解决可以流转到关闭、重新打开
        put("invalid", Arrays.asList("cancelled", "reopened"));  // 无效可以流转到已取消、重新打开
        put("on_hold", Arrays.asList("resolved"));  // 挂起只能流转到已解决
        put("reopened", Arrays.asList("accepted"));  // 重新打开只能流转到已接收
        put("closed", Arrays.asList("reopened"));  // 关闭可以由创建人重新打开
        // cancelled 是终止状态，无流转
    }};

    /**
     * 获取当前状态可流转的目标状态列表
     */
    public List<String> getAvailableTransitions(String issueId, String projectId) {
        if (StringUtils.isBlank(issueId)) {
            return new ArrayList<>();
        }

        IssuesWithBLOBs issue = issuesMapper.selectByPrimaryKey(issueId);
        if (issue == null) {
            return new ArrayList<>();
        }

        // 检查当前用户是否是当前状态的责任人
        if (!canEditStatus(issueId)) {
            return new ArrayList<>();
        }

        String status = getCurrentStatus(issue);
        if (StringUtils.isBlank(status)) {
            status = "new";
        }
        final String currentStatus = status;

        // 返回当前状态可流转的所有状态
        return TRANSITION_RULES.getOrDefault(currentStatus, new ArrayList<>());
    }

    /**
     * 检查是否允许状态流转（只检查流转规则，不检查权限）
     */
    private boolean canTransition(String fromStatus, String toStatus) {
        // 检查流转规则是否允许
        List<String> allowedTransitions = TRANSITION_RULES.get(fromStatus);
        return allowedTransitions != null && allowedTransitions.contains(toStatus);
    }

    /**
     * 执行状态流转并记录历史
     */
    public void transitionStatus(String issueId, String toStatus, String comment) {
        if (StringUtils.isBlank(issueId) || StringUtils.isBlank(toStatus)) {
            MSException.throwException(Translator.get("issue_status_transition_param_error"));
        }

        IssuesWithBLOBs issue = issuesMapper.selectByPrimaryKey(issueId);
        if (issue == null) {
            MSException.throwException(Translator.get("issue_not_found"));
        }

        // 检查当前用户是否是当前状态的责任人
        if (!canEditStatus(issueId)) {
            MSException.throwException("只有当前状态的责任人可以编辑状态");
        }

        // 获取当前状态
        final String currentStatus = getCurrentStatus(issue);
        
        // 检查流转规则是否允许
        if (!canTransition(currentStatus, toStatus)) {
            MSException.throwException(Translator.get("issue_status_transition_not_allowed"));
        }

        // 更新缺陷状态
        CustomField customField = baseCustomFieldService.getCustomFieldByName(issue.getProjectId(), SystemCustomField.ISSUE_STATUS);
        if (customField != null) {
            String fieldId = customField.getId();
            CustomFieldResourceDTO resource = new CustomFieldResourceDTO();
            resource.setFieldId(fieldId);
            resource.setResourceId(issue.getId());
            resource.setValue(JSON.toJSONString(toStatus));
            customFieldIssuesService.editFields(issue.getId(), Collections.singletonList(resource));
        }

        // 我在做：更新issues表的update_time字段
        // 目的是：确保状态流转时，缺陷的更新时间能正确反映最新修改时间
        // 如果不这样做，就无法实现：列表页面的更新时间字段在状态流转后保持不变，用户无法看到最新修改时间
        IssuesWithBLOBs updateIssue = new IssuesWithBLOBs();
        updateIssue.setId(issueId);
        updateIssue.setUpdateTime(System.currentTimeMillis());
        issuesMapper.updateByPrimaryKeySelective(updateIssue);

        // 只要流转到 reopened，自动增加"复测次数"（系统字段），并与状态变更一起写入审计日志
        List<IssueChangeLogDetailDTO> extraLogDetails = new ArrayList<>();
        if (StringUtils.equals(toStatus, "reopened")) {
            IssueChangeLogDetailDTO retestCountLog = increaseRetestCount(issue);
            if (retestCountLog != null) {
                extraLogDetails.add(retestCountLog);
            }
        }

        // 保存流转记录
        saveTransitionRecord(issueId, currentStatus, toStatus, comment, issue.getProjectId(), extraLogDetails);
    }

    /**
     * 保存状态流转记录
     */
    public void saveTransitionRecord(String issueId, String fromStatus, String toStatus, String comment, String projectId) {
        saveTransitionRecord(issueId, fromStatus, toStatus, comment, projectId, new ArrayList<>());
    }

    public void saveTransitionRecord(String issueId, String fromStatus, String toStatus, String comment, String projectId,
                                     List<IssueChangeLogDetailDTO> extraDetails) {
        SessionUser user = SessionUtils.getUser();
        if (user == null) {
            return;
        }

        IssueChangeLogDetailDTO d = new IssueChangeLogDetailDTO();
        d.setFieldType(IssueChangeLogService.FIELD_TYPE_SYSTEM);
        d.setFieldKey("status");
        d.setFieldName("状态");
        d.setOldValue(fromStatus);
        d.setNewValue(toStatus);

        List<IssueChangeLogDetailDTO> details = new ArrayList<>();
        details.add(d);
        if (CollectionUtils.isNotEmpty(extraDetails)) {
            details.addAll(extraDetails);
        }

        issueChangeLogService.saveLog(issueId, projectId, IssueChangeLogService.SOURCE_STATUS, details);
    }

    /**
     * 只要流转到 reopened 场景下调用：复测次数 +1（如果不存在，则按0初始化后 +1）。
     *
     * @return 返回用于审计日志的明细（FIELD_TYPE_CUSTOM），若字段不存在则返回null（不阻断状态流转）
     */
    private IssueChangeLogDetailDTO increaseRetestCount(IssuesWithBLOBs issue) {
        if (issue == null || StringUtils.isBlank(issue.getProjectId())) {
            return null;
        }

        try {
            CustomField retestCountField = baseCustomFieldService.getCustomFieldByName(issue.getProjectId(), SystemCustomField.ISSUE_RETEST_COUNT);
            if (retestCountField == null) {
                LogUtil.warn("项目 {} 未找到系统字段：{}，跳过复测次数累加", issue.getProjectId(), SystemCustomField.ISSUE_RETEST_COUNT);
                return null;
            }

            Integer oldCount = getCustomFieldValueAsInt(issue.getId(), retestCountField.getId());
            int newCount = (oldCount == null ? 0 : oldCount) + 1;

            CustomFieldResourceDTO resource = new CustomFieldResourceDTO();
            resource.setFieldId(retestCountField.getId());
            resource.setResourceId(issue.getId());
            resource.setValue(JSON.toJSONString(newCount));
            customFieldIssuesService.editFields(issue.getId(), Collections.singletonList(resource));

            IssueChangeLogDetailDTO d = new IssueChangeLogDetailDTO();
            d.setFieldType(IssueChangeLogService.FIELD_TYPE_CUSTOM);
            d.setFieldId(retestCountField.getId());
            d.setFieldName(SystemCustomField.ISSUE_RETEST_COUNT);
            d.setOldValue(oldCount == null ? null : String.valueOf(oldCount));
            d.setNewValue(String.valueOf(newCount));
            return d;
        } catch (Exception e) {
            LogUtil.error("累加复测次数失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从 custom_field_issues 读取 int 类型字段值。
     *
     * 兼容 value 可能是 "1" / 1 / null / [] 等格式。
     */
    private Integer getCustomFieldValueAsInt(String resourceId, String fieldId) {
        if (StringUtils.isBlank(resourceId) || StringUtils.isBlank(fieldId)) {
            return null;
        }

        CustomFieldIssuesExample example = new CustomFieldIssuesExample();
        example.createCriteria()
                .andFieldIdEqualTo(fieldId)
                .andResourceIdEqualTo(resourceId);
        List<CustomFieldIssues> list = customFieldIssuesMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }

        String raw = list.get(0).getValue();
        if (StringUtils.isBlank(raw) || StringUtils.equalsAny(raw, "null", "[]")) {
            return null;
        }

        try {
            return JSON.parseObject(raw, Integer.class);
        } catch (Exception ignore) {
            // fall through
        }

        try {
            String v = raw.trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1);
            }
            return Integer.parseInt(v);
        } catch (Exception e) {
            LogUtil.warn("解析自定义字段int值失败，resourceId={}, fieldId={}, value={}", new Object[]{resourceId, fieldId, raw});
            return null;
        }
    }

    /**
     * 获取缺陷的状态流转历史
     * @deprecated 该方法已废弃，历史记录由 Controller 直接调用 IssueChangeLogService.getHistory() 获取，
     *             本方法未被任何地方调用，保留仅作说明。
     */
    @Deprecated
    public List getTransitionHistory(String issueId) {
        return new ArrayList<>();
    }


    /**
     * 获取缺陷的当前状态
     */
    public String getCurrentStatus(Issues issue) {
        if (issue == null || StringUtils.isBlank(issue.getProjectId())) {
            LogUtil.warn("获取缺陷状态失败：缺陷信息或项目ID为空");
            return "new";
        }

        try {
            // 从custom_field_issues表中获取状态
            CustomField customField = baseCustomFieldService.getCustomFieldByName(issue.getProjectId(), SystemCustomField.ISSUE_STATUS);
            if (customField == null) {
                LogUtil.warn("获取缺陷状态失败：项目 {} 未找到状态字段", issue.getProjectId());
                return "new";
            }

            CustomFieldIssuesExample example = new CustomFieldIssuesExample();
            example.createCriteria()
                    .andFieldIdEqualTo(customField.getId())
                    .andResourceIdEqualTo(issue.getId());
            List<CustomFieldIssues> customFieldIssues = customFieldIssuesMapper.selectByExample(example);

            if (CollectionUtils.isEmpty(customFieldIssues)) {
                LogUtil.debug("缺陷 {} 未设置状态值，返回默认状态", issue.getId());
                return "new";
            }

            String status = customFieldIssues.get(0).getValue();
            if (StringUtils.isNotBlank(status)) {
                // 移除JSON字符串的引号
                String finalStatus = status.replaceAll("\"", StringUtils.EMPTY);
                LogUtil.debug("获取缺陷 {} 当前状态：{}", issue.getId(), finalStatus);
                return finalStatus;
            }
            return "new";
        } catch (Exception e) {
            LogUtil.error("获取缺陷状态异常：" + e.getMessage(), e);
            // 异常时返回不可流转的哨兵状态，确保默认禁止流转并避免接口500
            return "error";
        }
    }

    /**
     * 获取缺陷的处理人ID
     */
    private String getProcessor(String issueId) {
        if (StringUtils.isBlank(issueId)) {
            return null;
        }

        try {
            CustomFieldIssuesExample example = new CustomFieldIssuesExample();
            example.createCriteria()
                    .andResourceIdEqualTo(issueId)
                    .andFieldIdEqualTo(PROCESSOR_FIELD_ID);
            List<CustomFieldIssues> list = customFieldIssuesMapper.selectByExample(example);

            if (CollectionUtils.isEmpty(list)) {
                return null;
            }

            String value = list.get(0).getValue();
            if (StringUtils.isBlank(value) || StringUtils.equalsAny(value, "null", "[]")) {
                return null;
            }

            // member字段的值是JSON数组格式，例如：["user_id"] 或 ["user_id1", "user_id2"]
            try {
                List<String> userIds = JSON.parseArray(value, String.class);
                if (CollectionUtils.isEmpty(userIds)) {
                    return null;
                }
                // 返回第一个用户ID（处理人字段是单个member，应该只有一个值）
                return userIds.get(0);
            } catch (Exception e) {
                // 如果不是JSON数组格式，尝试直接返回（兼容处理）
                return value.replaceAll("\"", StringUtils.EMPTY);
            }
        } catch (Exception e) {
            LogUtil.error("获取处理人失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取缺陷的创建人ID
     */
    private String getCreator(Issues issue) {
        if (issue == null) {
            return null;
        }
        // 优先从issues表的creator字段获取
        if (StringUtils.isNotBlank(issue.getCreator())) {
            return issue.getCreator();
        }
        // 如果creator字段为空，从自定义字段获取
        try {
            CustomFieldIssuesExample example = new CustomFieldIssuesExample();
            example.createCriteria()
                    .andResourceIdEqualTo(issue.getId())
                    .andFieldIdEqualTo(CREATOR_FIELD_ID);
            List<CustomFieldIssues> list = customFieldIssuesMapper.selectByExample(example);
            if (CollectionUtils.isNotEmpty(list)) {
                String value = list.get(0).getValue();
                if (StringUtils.isNotBlank(value) && !StringUtils.equalsAny(value, "null", "[]")) {
                    try {
                        List<String> userIds = JSON.parseArray(value, String.class);
                        if (CollectionUtils.isNotEmpty(userIds)) {
                            return userIds.get(0);
                        }
                    } catch (Exception e) {
                        return value.replaceAll("\"", StringUtils.EMPTY);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error("获取创建人失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 检查当前用户是否有权限编辑状态（基于目标状态责任人）
     * 逻辑：检查当前状态可以流转到的目标状态，如果所有目标状态都由同一个人负责，那么应该由那个人来编辑
     */
    private boolean canEditStatus(String issueId) {
        IssuesWithBLOBs issue = issuesMapper.selectByPrimaryKey(issueId);
        if (issue == null) {
            return false;
        }
        
        String currentStatus = getCurrentStatus(issue);
        if (StringUtils.isBlank(currentStatus)) {
            currentStatus = "new";
        }
        
        // 获取当前状态可以流转到的目标状态列表
        List<String> targetStatuses = TRANSITION_RULES.get(currentStatus);
        if (CollectionUtils.isEmpty(targetStatuses)) {
            // 如果没有可流转状态（终止状态），检查当前状态的责任人
            String ownerType = STATUS_OWNER.get(currentStatus);
            if (StringUtils.isBlank(ownerType)) {
                return false;
            }
            return checkUserIsOwner(issue, issueId, ownerType);
        }
        
        // 检查所有目标状态的责任人是否一致
        Set<String> ownerTypes = new HashSet<>();
        for (String targetStatus : targetStatuses) {
            String ownerType = STATUS_OWNER.get(targetStatus);
            if (StringUtils.isNotBlank(ownerType)) {
                ownerTypes.add(ownerType);
            }
        }
        
        // 如果所有目标状态都由同一个人负责，那么应该由那个人来编辑
        if (ownerTypes.size() == 1) {
            String targetOwnerType = ownerTypes.iterator().next();
            return checkUserIsOwner(issue, issueId, targetOwnerType);
        }
        
        // 如果目标状态由不同的人负责，或者没有定义责任人，返回false
        return false;
    }
    
    /**
     * 检查当前用户是否是指定的责任人类型
     */
    private boolean checkUserIsOwner(Issues issue, String issueId, String ownerType) {
        SessionUser user = SessionUtils.getUser();
        if (user == null) {
            return false;
        }
        
        if (CREATOR.equals(ownerType)) {
            // 检查是否是创建人
            String creatorId = getCreator(issue);
            return StringUtils.equals(creatorId, user.getId());
        } else if (PROCESSOR.equals(ownerType)) {
            // 检查是否是处理人
            String processorId = getProcessor(issueId);
            if (StringUtils.isBlank(processorId)) {
                return false;
            }
            return StringUtils.equals(processorId, user.getId());
        }
        
        return false;
    }
}

