package io.metersphere.requirement.workflow.service;

import io.metersphere.base.domain.TestPlan;
import io.metersphere.base.domain.User;
import io.metersphere.base.mapper.ext.RequirementTestPlanSyncMapper;
import io.metersphere.commons.constants.TestPlanStatus;
import io.metersphere.commons.exception.MSException;
import io.metersphere.plan.service.TestPlanService;
import io.metersphere.requirement.pool.dto.RequirementCallbackMessage;
import io.metersphere.requirement.pool.producer.RequirementCallbackMessageSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RequirementPlanWorkflowService {

    private static final String ASSESSMENT_CONTINUE_TEST = "CONTINUE_TEST";
    private static final String APPROVAL_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_APPROVED = "APPROVED";

    @Resource
    private RequirementTestPlanSyncMapper requirementTestPlanSyncMapper;
    @Resource
    private RequirementCallbackMessageSender requirementCallbackMessageSender;
    @Resource
    private TestPlanService testPlanService;

    /**
     * 测试计划编制完成后提交全流程平台审批。
     * 发送成功后本地仍处于“测试计划”阶段，只记录为等待审批；
     * 审批通过的入站消息再将业务阶段推进到“测试准备”。
     */
    public void submitPlanApproval(String planId) {
        if (StringUtils.isBlank(planId)) {
            MSException.throwException("测试计划ID不能为空");
        }

        TestPlan plan = requirementTestPlanSyncMapper.selectById(planId);
        if (plan == null) {
            MSException.throwException("测试计划不存在");
        }
        if (StringUtils.isBlank(plan.getRequirementNumber())) {
            MSException.throwException("仅全流程平台同步创建的测试计划支持提交审批");
        }
        if (StringUtils.equalsAny(plan.getStatus(), TestPlanStatus.Cancelled.name(), TestPlanStatus.Archived.name())) {
            MSException.throwException("已取消或已归档的测试计划不能提交审批");
        }
        if (APPROVAL_SUBMITTED.equals(plan.getRequirementApprovalStatus())) {
            MSException.throwException("测试计划已提交审批，请等待审批结果");
        }
        if (APPROVAL_APPROVED.equals(plan.getRequirementApprovalStatus())) {
            MSException.throwException("测试计划审批已通过，无需重复提交");
        }
        if (plan.getPlannedStartTime() == null || plan.getPlannedEndTime() == null) {
            MSException.throwException("请先填写计划开始时间和计划结束时间");
        }
        if (plan.getPlannedEndTime() < plan.getPlannedStartTime()) {
            MSException.throwException("计划结束时间不能早于计划开始时间");
        }

        String principalUsers = getPrincipalUsers(planId);
        if (StringUtils.isBlank(principalUsers)) {
            MSException.throwException("请先设置测试负责人");
        }

        long submitTime = System.currentTimeMillis();
        RequirementCallbackMessage message = new RequirementCallbackMessage();
        message.setDmpNum(plan.getRequirementNumber());
        message.setPlanId(plan.getId());
        message.setAssessmentResult(ASSESSMENT_CONTINUE_TEST);
        message.setPlannedStartTime(plan.getPlannedStartTime());
        message.setPlannedEndTime(plan.getPlannedEndTime());
        message.setPrincipalUsers(principalUsers);
        message.setSubmitTime(submitTime);

        try {
            requirementCallbackMessageSender.sendCallbackMessage(message);
        } catch (Exception e) {
            log.error("提交测试计划审批消息失败, planId={}, dmpNum={}", plan.getId(), plan.getRequirementNumber(), e);
            MSException.throwException("提交全流程平台审批失败，请稍后重试");
            return;
        }

        // 只有MQ发送成功后才更新本地状态，避免页面显示“等待审批”但实际未发出。
        requirementTestPlanSyncMapper.markApprovalSubmitted(plan.getId(), submitTime);
        log.info("[INT计划审批-已提交] planId={}, dmpNum={}, plannedStartTime={}, plannedEndTime={}",
                plan.getId(), plan.getRequirementNumber(), plan.getPlannedStartTime(), plan.getPlannedEndTime());
    }

    private String getPrincipalUsers(String planId) {
        List<User> users = testPlanService.getPlanPrincipal(planId);
        if (users == null || users.isEmpty()) {
            return StringUtils.EMPTY;
        }
        return users.stream()
                .map(User::getName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(","));
    }
}
