package io.metersphere.workflow.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "io.metersphere.workflow.mapper")
public class WorkflowMybatisConfig {
}
