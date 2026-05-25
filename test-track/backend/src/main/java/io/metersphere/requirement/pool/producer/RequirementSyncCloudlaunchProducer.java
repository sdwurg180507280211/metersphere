package io.metersphere.requirement.pool.producer;

import com.alibaba.fastjson.JSON;
import com.ruiinsurance.cloudlaunch.rocketmq.api.Message;
import com.ruiinsurance.cloudlaunch.rocketmq.api.producer.MessageProducer;
import com.ruiinsurance.cloudlaunch.rocketmq.api.producer.SendResult;
import io.metersphere.requirement.pool.dto.RequirementSyncMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "requirement.sync.mq.client", havingValue = "cloudlaunch")
public class RequirementSyncCloudlaunchProducer implements RequirementSyncMessageSender {

    @Resource(name = "producer-aiuw")
    private MessageProducer messageProducer;

    @Override
    public void sendSyncMessage(RequirementSyncMessage msg) throws Exception {
        ensureTraceId(msg);
        String body = JSON.toJSONString(msg);
        Message message = new Message();
        message.setMessageKey(msg.getTraceId());
        message.setMessageBody(body.getBytes(StandardCharsets.UTF_8));
        log.info("[需求MQ-发送] client=cloudlaunch, dmpNum={}, operationType={}, traceId={}",
                msg.getDmpNum(), msg.getOperationType(), msg.getTraceId());
        SendResult result = messageProducer.send(message);
        log.info("[需求MQ-发送成功] client=cloudlaunch, msgId={}, sendStatus={}, dmpNum={}, traceId={}",
                result.getMessageId(), result.getSendStatus(), msg.getDmpNum(), msg.getTraceId());
    }

    private void ensureTraceId(RequirementSyncMessage msg) {
        if (StringUtils.isBlank(msg.getTraceId())) {
            msg.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        }
    }
}
