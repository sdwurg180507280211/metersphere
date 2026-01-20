package io.metersphere.controller;

import io.metersphere.commons.constants.PermissionConstants;
import io.metersphere.dto.IssueChangeLogDTO;
import io.metersphere.request.issues.IssueStatusTransitionRequest;
import io.metersphere.security.CheckOwner;
import io.metersphere.service.IssueChangeLogService;
import io.metersphere.service.IssueStatusTransitionService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RequestMapping("/issues/transition")
@RestController
public class IssueStatusTransitionController {

    @Resource
    private IssueStatusTransitionService issueStatusTransitionService;

    @Resource
    private IssueChangeLogService issueChangeLogService;

    /**
     * 获取缺陷的状态流转历史
     */
    @GetMapping("/history/{issueId}")
    @RequiresPermissions(PermissionConstants.PROJECT_TRACK_ISSUE_READ)
    @CheckOwner(resourceId = "#issueId", resourceType = "issues")
    public List<IssueChangeLogDTO> getTransitionHistory(@PathVariable String issueId) {
        return issueChangeLogService.getHistory(issueId);
    }

    /**
     * 获取可流转的状态列表
     */
    @GetMapping("/available/{issueId}")
    @RequiresPermissions(PermissionConstants.PROJECT_TRACK_ISSUE_READ)
    @CheckOwner(resourceId = "#issueId", resourceType = "issues")
    public List<String> getAvailableTransitions(@PathVariable String issueId) {
        return issueStatusTransitionService.getAvailableTransitions(issueId, null);
    }

    /**
     * 执行状态流转
     */
    @PostMapping("/transition")
    @RequiresPermissions(PermissionConstants.PROJECT_TRACK_ISSUE_READ_EDIT)
    @CheckOwner(resourceId = "#request.issueId", resourceType = "issues")
    public void transitionStatus(@RequestBody IssueStatusTransitionRequest request) {
        issueStatusTransitionService.transitionStatus(request.getIssueId(), request.getToStatus(), request.getComment());
    }
}

