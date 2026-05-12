package io.metersphere.requirement.pool.producer;

import com.alibaba.fastjson.JSON;
import io.metersphere.requirement.pool.dto.RequirementSyncMessage;
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

/**
 * 需求同步消息生产者
 * 模拟需求平台推送消息，"创建需求"按钮通过此Producer发MQ消息
 */
@Slf4j
@Component
public class RequirementSyncProducer implements InitializingBean {

    @Value("${rocketmq.name-server:}")
    private String nameServer;

    @Value("${rocketmq.topic.requirement-sync:topic-requirement-to-metersphere}")
    private String topic;

    @Value("${rocketmq.producer.group:producer-requirement-to-metersphere}")
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

    /**
     * 同步发送需求消息
     */
    public SendResult sendSyncMessage(RequirementSyncMessage msg) throws Exception {
        ensureTraceId(msg);
        String body = JSON.toJSONString(msg);
        log.info("[需求MQ-发送] topic={}, dmpNum={}, operationType={}, traceId={}", topic, msg.getDmpNum(), msg.getOperationType(), msg.getTraceId());
        Message message = new Message(topic, body.getBytes(StandardCharsets.UTF_8));
        SendResult result = producer.send(message);
        log.info("[需求MQ-发送成功] msgId={}, sendStatus={}, dmpNum={}, traceId={}", result.getMsgId(), result.getSendStatus(), msg.getDmpNum(), msg.getTraceId());
        return result;
    }

    private void ensureTraceId(RequirementSyncMessage msg) {
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
