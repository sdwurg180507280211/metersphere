package io.metersphere;

import io.metersphere.autoconfigure.OpenApiConfig;
import io.metersphere.autoconfigure.PermissionConfig;
import io.metersphere.autoconfigure.RsaConfig;
import io.metersphere.autoconfigure.ShiroConfig;
import io.metersphere.config.KafkaProperties;
import io.metersphere.config.MinioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.PropertySource;

/**
 * 分析统计微服务启动类
 *
 * 功能范围：
 * - 综合查询
 * - SQL查询台
 * - 统计图表
 * - 数据字典
 *
 * 技术栈：
 * - Spring Boot 3.2.12
 * - Java 17
 * - 微前端架构 (qiankun)
 *
 * 说明：
 * - 排除ShiroConfig：不需要独立的认证体系，通过Gateway统一认证
 * - 排除ShiroAutoConfiguration：排除Shiro的Spring Boot自动配置
 * - 排除RsaConfig、PermissionConfig、OpenApiConfig：简化配置
 */
@SpringBootApplication(exclude = {
        QuartzAutoConfiguration.class,
        LdapAutoConfiguration.class,
        Neo4jAutoConfiguration.class,
        ShiroConfig.class,
        RsaConfig.class,
        PermissionConfig.class,
        OpenApiConfig.class
})
@PropertySource(value = {
        "classpath:commons.properties",
        "file:/opt/metersphere/conf/metersphere.properties",
}, encoding = "UTF-8", ignoreResourceNotFound = true)
@ServletComponentScan
@EnableDiscoveryClient
@EnableConfigurationProperties({
        KafkaProperties.class,
        MinioProperties.class
})
public class AnalyticsStatApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsStatApplication.class, args);
    }

}
