package io.metersphere;

import io.metersphere.autoconfigure.MinIOConfig;
import io.metersphere.autoconfigure.OpenApiConfig;
import io.metersphere.autoconfigure.PermissionConfig;
import io.metersphere.autoconfigure.RsaConfig;
import io.metersphere.config.KafkaProperties;
import io.metersphere.config.MinioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(exclude = {
        QuartzAutoConfiguration.class,
        LdapAutoConfiguration.class,
        Neo4jAutoConfiguration.class,
        RsaConfig.class,
        PermissionConfig.class,
        OpenApiConfig.class
})
@PropertySource(value = {
        "classpath:commons.properties",
        "file:/opt/metersphere/conf/metersphere.properties",
}, encoding = "UTF-8", ignoreResourceNotFound = true)
@ServletComponentScan
@EnableConfigurationProperties({
        KafkaProperties.class,
        MinioProperties.class
})
public class WorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
