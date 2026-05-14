package io.metersphere.config;

import com.ruiinsurance.cloudlaunch.rocketmq.RocketMQAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(name = "requirement.sync.mq.client", havingValue = "cloudlaunch")
@Import(RocketMQAutoConfiguration.class)
public class RequirementSyncCloudlaunchConfiguration {
}
