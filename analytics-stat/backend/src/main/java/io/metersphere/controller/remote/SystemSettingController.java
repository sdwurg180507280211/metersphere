package io.metersphere.controller.remote;

import io.metersphere.service.remote.BaseSystemSettingService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 系统设置远程调用Controller
 * 
 * 功能：将系统级公共API请求转发到 system-setting 服务
 * 
 * 转发路径：
 * - /system/* → system-setting 服务
 * - /module/* → system-setting 服务  
 * - /license/* → system-setting 服务
 * 
 * 原理：
 * 在qiankun微前端架构中,子应用的所有请求都会带上模块前缀(如 /analytics)
 * 例如: /analytics/system/theme
 * 
 * Gateway会将请求路由到 analytics-stat 服务
 * 本Controller接收请求后,通过 BaseSystemSettingService 转发到 system-setting 服务
 * 
 * 参考：project-management/backend/.../controller/remote/SystemSettingController.java
 */
@RestController
@RequestMapping(path = {
        "/system",
        "/module",
        "/license"
})
public class SystemSettingController {
    
    @Resource
    BaseSystemSettingService baseSystemSettingService;

    /**
     * 转发POST请求到 system-setting 服务
     */
    @PostMapping("/**")
    public Object post(HttpServletRequest request, @RequestBody Object param) {
        return baseSystemSettingService.post(request, param);
    }

    /**
     * 转发GET请求到 system-setting 服务
     */
    @GetMapping("/**")
    public Object get(HttpServletRequest request) {
        return baseSystemSettingService.get(request);
    }
}
