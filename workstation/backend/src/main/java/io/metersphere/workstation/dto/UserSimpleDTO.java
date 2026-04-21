package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户简要信息 DTO
 * 
 * 用于用户选择器组件显示用户列表
 * 包含用户的基本信息和头像
 * 
 * @author MeterSphere
 */
@Getter
@Setter
public class UserSimpleDTO {
    
    /**
     * 用户 ID
     */
    private String id;
    
    /**
     * 用户名（登录账号）
     */
    private String name;
    
    /**
     * 用户邮箱
     */
    private String email;
    
    /**
     * 用户头像 URL
     * 例如：/avatar/user-001.png
     */
    private String avatar;
    
    /**
     * 用户真实姓名
     */
    private String realName;
}
