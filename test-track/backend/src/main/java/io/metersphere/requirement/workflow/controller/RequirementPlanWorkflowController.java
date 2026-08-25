package io.metersphere.requirement.workflow.controller;

import io.metersphere.commons.constants.PermissionConstants;
import io.metersphere.requirement.workflow.service.RequirementPlanWorkflowService;
import io.metersphere.security.CheckOwner;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/requirement/plan-workflow")
@RestController
public class RequirementPlanWorkflowController {

    @Resource
    private RequirementPlanWorkflowService requirementPlanWorkflowService;

    @PostMapping("/submit-approval/{planId}")
    @RequiresPermissions(PermissionConstants.PROJECT_TRACK_PLAN_READ_EDIT)
    @CheckOwner(resourceId = "#planId", resourceType = "test_plan")
    public void submitApproval(@PathVariable String planId) {
        requirementPlanWorkflowService.submitPlanApproval(planId);
    }
}
