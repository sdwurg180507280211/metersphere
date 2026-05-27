package io.metersphere.requirement.pool.producer;

import com.alibaba.fastjson.JSON;
import io.metersphere.requirement.pool.dto.RequirementCallbackMessage;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class RequirementCallbackProducer implements InitializingBean, RequirementCallbackMessageSender {

    @Value("${rocketmq.name-server:}")
    private String nameServer;

    @Value("${rocketmq.topic.status-callback:topic-metersphere-to-requirement}")
    private String topic;

    @Value("${rocketmq.producer.status-callback:producer-metersphere-to-requirement}")
    private String producerGroup;

    private DefaultMQProducer producer;

    @Override
    public void afterPropertiesSet() throws Exception {
        producerGroup = StringUtils.trimToEmpty(producerGroup);
        nameServer = StringUtils.trimToEmpty(nameServer);
        topic = StringUtils.trimToEmpty(topic);
        producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.start();
    }

    @Override
    public void sendCallbackMessage(RequirementCallbackMessage msg) throws Exception {
        ensureTraceId(msg);
        String body = JSON.toJSONString(msg);
        log.info("[需求回传MQ-发送] client=rocketmq, topic={}, dmpNum={}, planStatus={}, traceId={}", topic, msg.getDmpNum(), msg.getPlanStatus(), msg.getTraceId());
        Message message = new Message(topic, body.getBytes(StandardCharsets.UTF_8));
        SendResult result = producer.send(message);
        log.info("[需求回传MQ-发送成功] client=rocketmq, msgId={}, sendStatus={}, dmpNum={}, traceId={}", result.getMsgId(), result.getSendStatus(), msg.getDmpNum(), msg.getTraceId());
    }

    private void ensureTraceId(RequirementCallbackMessage msg) {
        if (StringUtils.isBlank(msg.getTraceId())) {
            msg.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        }
    }

    @PreDestroy
    public void destroy() {
        if (producer != null) {
            producer.shutdown();
        }
    }
}
