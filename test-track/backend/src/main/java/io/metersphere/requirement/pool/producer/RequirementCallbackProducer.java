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
    public void afterPropertiesSet() {
        producerGroup = StringUtils.trimToEmpty(producerGroup);
        nameServer = StringUtils.trimToEmpty(nameServer);
        topic = StringUtils.trimToEmpty(topic);
        if (StringUtils.isAnyBlank(nameServer, topic, producerGroup)) {
            log.warn("[需求回传MQ-生产者] RocketMQ配置不完整，跳过生产者启动, nameServer={}, topic={}, producerGroup={}",
                    nameServer, topic, producerGroup);
            return;
        }

        DefaultMQProducer mqProducer = null;
        try {
            mqProducer = new DefaultMQProducer(producerGroup);
            mqProducer.setNamesrvAddr(nameServer);
            mqProducer.start();
            producer = mqProducer;
            log.info("[需求回传MQ-生产者] RocketMQ生产者启动成功, nameServer={}, topic={}, producerGroup={}",
                    nameServer, topic, producerGroup);
        } catch (Exception e) {
            log.warn("[需求回传MQ-生产者] RocketMQ生产者启动失败，已降级为不可发送需求回传消息，不影响test-track启动, nameServer={}, topic={}, producerGroup={}",
                    nameServer, topic, producerGroup, e);
            if (mqProducer != null) {
                mqProducer.shutdown();
            }
        }
    }

    @Override
    public void sendCallbackMessage(RequirementCallbackMessage msg) throws Exception {
        ensureTraceId(msg);
        if (producer == null) {
            log.warn("[需求回传MQ-发送跳过] RocketMQ生产者未启动, topic={}, dmpNum={}, planStatus={}, traceId={}",
                    topic, msg.getDmpNum(), msg.getPlanStatus(), msg.getTraceId());
            throw new IllegalStateException("RocketMQ生产者未启动，无法发送需求回传消息");
        }
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
