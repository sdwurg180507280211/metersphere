package io.metersphere.analyticsstat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 
 * 功能：
 * 1. 配置静态资源映射，支持 qiankun 微前端加载
 * 2. 配置默认视图控制器
 * 
 * 资源映射说明：
 * - /analytics-stat/** -> classpath:/static/
 * - qiankun 通过 /analytics-stat/js/xxx.js 访问静态资源
 * - Spring Boot 从 classpath:/static/ 目录提供文件
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源处理器
     * 
     * 映射规则：
     * - /analytics-stat/js/** -> classpath:/static/js/
     * - /analytics-stat/css/** -> classpath:/static/css/
     * - /analytics-stat/fonts/** -> classpath:/static/fonts/
     * - /analytics-stat/** -> classpath:/static/（兜底）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /analytics-stat/** 到 classpath:/static/
        // 这样 qiankun 请求 /analytics-stat/js/xxx.js 时
        // Spring Boot 会从 classpath:/static/js/xxx.js 提供文件
        registry.addResourceHandler("/analytics-stat/**")
                .addResourceLocations("classpath:/static/");
        
        // 默认静态资源映射（独立运行时使用）
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/");
    }

    /**
     * 配置视图控制器
     * 
     * 将根路径重定向到 index.html
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 根路径返回 index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        // /analytics-stat 路径也返回 index.html（qiankun 入口）
        registry.addViewController("/analytics-stat").setViewName("forward:/index.html");
        registry.addViewController("/analytics-stat/").setViewName("forward:/index.html");
    }
}
