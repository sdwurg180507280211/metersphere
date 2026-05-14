package io.metersphere.track.consumer;

import com.alibaba.fastjson.JSON;
import com.ruiinsurance.cloudlaunch.rocketmq.api.Message;
import com.ruiinsurance.cloudlaunch.rocketmq.api.consumer.AbstractMessageListener;
import com.ruiinsurance.cloudlaunch.rocketmq.api.consumer.Action;
import io.metersphere.requirement.pool.dto.RequirementSyncMessage;
import io.metersphere.requirement.pool.service.RequirementPoolService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.nio.charset.StandardCharsets;

@Slf4j
@ConditionalOnProperty(name = "requirement.sync.mq.client", havingValue = "cloudlaunch")
public class RequirementSyncCloudlaunchListener extends AbstractMessageListener {

    @Resource
    private RequirementPoolService requirementPoolService;

    @Override
    public Action consume(Message message) {
        try {
            String body = new String(message.getMessageBody(), StandardCharsets.UTF_8);
            RequirementSyncMessage syncMessage = JSON.parseObject(body, RequirementSyncMessage.class);
            log.info("[需求MQ-接收] client=cloudlaunch, msgId={}, msgKey={}, dmpNum={}, operationType={}, traceId={}",
                    message.getMessageId(), message.getMessageKey(), syncMessage.getDmpNum(), syncMessage.getOperationType(), syncMessage.getTraceId());
            requirementPoolService.handleSyncMessage(syncMessage);
            log.info("[需求MQ-消费成功] client=cloudlaunch, msgId={}, dmpNum={}, traceId={}",
                    message.getMessageId(), syncMessage.getDmpNum(), syncMessage.getTraceId());
            return Action.CommitMessage;
        } catch (Exception e) {
            log.error("消费需求同步消息失败, client=cloudlaunch, msgId={}", message.getMessageId(), e);
            return Action.ReconsumeLater;
        }
    }
}
