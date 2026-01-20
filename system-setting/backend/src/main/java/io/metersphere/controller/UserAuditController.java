package io.metersphere.controller;

import io.metersphere.commons.annotation.AuditLog;
import io.metersphere.dto.UserDTO;
import io.metersphere.request.member.EditPassWordRequest;
import io.metersphere.request.member.UserRequest;
import io.metersphere.service.UserAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户审计控制器示例
 * 展示如何在 Controller 层使用审计日志
 */
@Tag(name = "用户审计管理")
@RestController
@RequestMapping("/user/audit")
public class UserAuditController {
    
    @Resource
    private UserAuditService userAuditService;
    
    /**
     * 创建用户（带审计日志）
     */
    @Operation(summary = "创建用户")
    @PostMapping("/create")
    public UserDTO createUser(@RequestBody UserRequest userRequest) {
        return userAuditService.createUserWithAudit(userRequest);
    }
    
    /**
     * 更新用户（带审计日志）
     */
    @Operation(summary = "更新用户")
    @PostMapping("/update")
    public UserDTO updateUser(@RequestBody UserRequest userRequest) {
        return userAuditService.updateUserWithAudit(userRequest);
    }
    
    /**
     * 删除用户（带审计日志）
     */
    @Operation(summary = "删除用户")
    @DeleteMapping("/delete/{userId}")
    public void deleteUser(@PathVariable String userId) {
        userAuditService.deleteUserWithAudit(userId);
    }
    
    /**
     * 修改密码（带审计日志）
     */
    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public void changePassword(@RequestBody EditPassWordRequest request) {
        userAuditService.changePasswordWithAudit(request);
    }
    
    /**
     * 批量操作用户（带审计日志）
     */
    @Operation(summary = "批量操作用户")
    @PostMapping("/batch/{operation}")
    public void batchOperateUsers(@PathVariable String operation, @RequestBody List<String> userIds) {
        userAuditService.batchOperateUsersWithAudit(operation, userIds);
    }
    
    /**
     * 直接在 Controller 使用注解的示例
     */
    @Operation(summary = "启用用户")
    @AuditLog(
        module = "USER_MANAGEMENT",
        content = "启用用户: #{#userId}",
        sourceId = "#{#userId}",
        history = false
    )
    @PostMapping("/enable/{userId}")
    public void enableUser(@PathVariable String userId) {
        // 启用用户的业务逻辑
        // userService.enableUser(userId);
    }
    
    /**
     * 禁用用户
     */
    @Operation(summary = "禁用用户")
    @AuditLog(
        module = "USER_MANAGEMENT",
        content = "禁用用户: #{#userId}",
        sourceId = "#{#userId}",
        history = false
    )
    @PostMapping("/disable/{userId}")
    public void disableUser(@PathVariable String userId) {
        // 禁用用户的业务逻辑
        // userService.disableUser(userId);
    }
}