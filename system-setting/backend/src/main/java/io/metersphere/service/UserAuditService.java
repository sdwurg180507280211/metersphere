package io.metersphere.service;

import io.metersphere.base.domain.User;
import io.metersphere.base.mapper.UserMapper;
import io.metersphere.commons.annotation.AuditLog;
import io.metersphere.commons.service.AuditLogService;
import io.metersphere.commons.utils.JSON;
import io.metersphere.dto.UserDTO;
import io.metersphere.request.member.EditPassWordRequest;
import io.metersphere.request.member.UserRequest;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户审计服务示例
 * 展示如何使用新的审计日志系统记录用户操作
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserAuditService {
    
    @Resource
    private UserService userService;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private AuditLogService auditLogService;
    
    /**
     * 创建用户（使用注解方式）
     * 演示如何使用 @AuditLog 注解自动记录审计日志
     */
    @AuditLog(
        module = "USER_MANAGEMENT",
        content = "创建用户: #{#userRequest.name} (#{#userRequest.email})",
        sourceId = "#{#result.id}",
        modifiedValue = "#{#result}",
        history = true
    )
    public UserDTO createUserWithAudit(UserRequest userRequest) {
        // 调用原有的用户创建逻辑
        return userService.insert(userRequest);
    }
    
    /**
     * 更新用户（使用注解方式，记录变更前后数据）
     * 演示如何记录数据变更的审计日志
     */
    @AuditLog(
        module = "USER_MANAGEMENT", 
        content = "更新用户: #{#userRequest.name} (#{#userRequest.email})",
        sourceId = "#{#userRequest.id}",
        recordOriginal = true,
        originalValue = "#{#originalUser}",
        modifiedValue = "#{#result}",
        history = true
    )
    public UserDTO updateUserWithAudit(UserRequest userRequest) {
        // 获取变更前的用户数据
        User originalUser = userMapper.selectByPrimaryKey(userRequest.getId());
        
        // 调用原有的用户更新逻辑
        UserDTO result = userService.updateUser(userRequest);
        
        return result;
    }
    
    /**
     * 删除用户（使用注解方式）
     * 演示如何记录删除操作的审计日志
     */
    @AuditLog(
        module = "USER_MANAGEMENT",
        content = "删除用户: #{#user.name} (#{#user.email})",
        sourceId = "#{#userId}",
        originalValue = "#{#user}",
        history = true
    )
    public void deleteUserWithAudit(String userId) {
        // 获取要删除的用户信息
        User user = userMapper.selectByPrimaryKey(userId);
        
        // 调用原有的用户删除逻辑
        userService.deleteUser(userId);
    }
    
    /**
     * 修改密码（使用编程方式）
     * 演示如何使用编程方式记录审计日志
     */
    public void changePasswordWithAudit(EditPassWordRequest request) {
        try {
            // 获取用户信息
            User user = userMapper.selectByPrimaryKey(request.getId());
            
            // 调用原有的密码修改逻辑
            userService.editPassword(request);
            
            // 手动发送审计日志（密码修改不记录具体内容）
            auditLogService.sendUserOperationLog(
                "UPDATE",
                "USER_MANAGEMENT", 
                "修改用户密码: " + user.getName(),
                request.getId(),
                null, // 不记录密码相关的原始数据
                null  // 不记录密码相关的变更数据
            );
            
        } catch (Exception e) {
            // 记录失败的操作
            auditLogService.sendSystemOperationLog(
                "UPDATE",
                "USER_MANAGEMENT",
                "修改用户密码失败: " + e.getMessage(),
                request.getId()
            );
            throw e;
        }
    }
    
    /**
     * 批量操作用户（使用编程方式）
     * 演示如何记录批量操作的审计日志
     */
    public void batchOperateUsersWithAudit(String operation, java.util.List<String> userIds) {
        try {
            // 获取用户信息
            java.util.List<User> users = userIds.stream()
                .map(userMapper::selectByPrimaryKey)
                .collect(java.util.stream.Collectors.toList());
            
            // 执行批量操作（这里只是示例）
            switch (operation.toUpperCase()) {
                case "ENABLE":
                    // 批量启用用户逻辑
                    break;
                case "DISABLE":
                    // 批量禁用用户逻辑
                    break;
                case "DELETE":
                    // 批量删除用户逻辑
                    break;
            }
            
            // 为每个用户记录审计日志
            for (User user : users) {
                auditLogService.sendUserOperationLog(
                    operation.toUpperCase(),
                    "USER_MANAGEMENT",
                    "批量" + operation + "用户: " + user.getName(),
                    user.getId(),
                    JSON.toJSONString(user),
                    null
                );
            }
            
        } catch (Exception e) {
            // 记录批量操作失败
            auditLogService.sendSystemOperationLog(
                operation.toUpperCase(),
                "USER_MANAGEMENT",
                "批量" + operation + "用户失败: " + e.getMessage() + ", 用户数量: " + userIds.size(),
                String.join(",", userIds)
            );
            throw e;
        }
    }
    
    /**
     * 用户登录审计（使用编程方式）
     * 演示如何记录登录操作的审计日志
     */
    public void recordLoginAudit(String userId, String loginResult, String errorMessage) {
        try {
            User user = userMapper.selectByPrimaryKey(userId);
            String userName = user != null ? user.getName() : "未知用户";
            
            if ("SUCCESS".equals(loginResult)) {
                auditLogService.sendSystemOperationLog(
                    "LOGIN",
                    "USER_MANAGEMENT",
                    "用户登录成功: " + userName,
                    userId
                );
            } else {
                auditLogService.sendSystemOperationLog(
                    "LOGIN",
                    "USER_MANAGEMENT", 
                    "用户登录失败: " + userName + " - " + errorMessage,
                    userId
                );
            }
            
        } catch (Exception e) {
            // 记录审计日志失败不应该影响主流程
            // 只记录错误日志
            System.err.println("记录登录审计日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 用户权限变更审计（使用编程方式）
     * 演示如何记录权限变更的审计日志
     */
    public void recordPermissionChangeAudit(String userId, java.util.List<String> oldRoles, 
                                          java.util.List<String> newRoles) {
        try {
            User user = userMapper.selectByPrimaryKey(userId);
            String userName = user != null ? user.getName() : "未知用户";
            
            String content = String.format("用户权限变更: %s, 原角色: [%s], 新角色: [%s]",
                userName,
                String.join(", ", oldRoles),
                String.join(", ", newRoles)
            );
            
            auditLogService.sendUserOperationLog(
                "UPDATE",
                "USER_MANAGEMENT",
                content,
                userId,
                JSON.toJSONString(oldRoles),
                JSON.toJSONString(newRoles)
            );
            
        } catch (Exception e) {
            System.err.println("记录权限变更审计日志失败: " + e.getMessage());
        }
    }
}