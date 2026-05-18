package io.metersphere;

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
 * - 保留 ShiroConfig：SDK 中 SessionUtils、ApiKeyFilter、CsrfFilter 等
 *   大量使用 SecurityUtils.getSubject()，必须有 SecurityManager
 * - 排除 Quartz、LDAP、Neo4j：本模块不需要这些功能
 * - 参考 report-stat（ReportApplication）的排除配置
 */
@SpringBootApplication(exclude = {
        QuartzAutoConfiguration.class,
        LdapAutoConfiguration.class,
        Neo4jAutoConfiguration.class
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
