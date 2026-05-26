package io.metersphere.track.consumer;

import com.alibaba.fastjson.JSON;
import io.metersphere.requirement.pool.dto.RequirementSyncMessage;
import io.metersphere.requirement.pool.service.RequirementPoolService;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 需求同步消息消费者
 * 消费RocketMQ消息，根据operationType分发到不同的业务逻辑
 */
@Slf4j
@Component
public class RequirementSyncConsumer implements InitializingBean {

    @Value("${rocketmq.name-server:}")
    private String nameServer;

    @Value("${rocketmq.topic.requirement-sync:topic-requirement-to-metersphere}")
    private String topic;

    @Value("${rocketmq.consumer.group:consumer-requirement-to-metersphere}")
    private String consumerGroup;

    @Resource
    private RequirementPoolService requirementPoolService;

    private DefaultMQPushConsumer consumer;

    @Override
    public void afterPropertiesSet() throws Exception {
        consumerGroup = StringUtils.trimToEmpty(consumerGroup);
        nameServer = StringUtils.trimToEmpty(nameServer);
        topic = StringUtils.trimToEmpty(topic);
        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeMessageBatchMaxSize(1);
        consumer.subscribe(topic, "*");

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    try {
                        String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                        RequirementSyncMessage syncMessage = JSON.parseObject(body, RequirementSyncMessage.class);
                        log.info("[需求MQ-接收] msgId={}, topic={}, dmpNum={}, operationType={}, traceId={}",
                                msg.getMsgId(), msg.getTopic(), syncMessage.getDmpNum(), syncMessage.getOperationType(), syncMessage.getTraceId());
                        requirementPoolService.handleSyncMessage(syncMessage);
                        log.info("[需求MQ-消费成功] msgId={}, dmpNum={}, traceId={}",
                                msg.getMsgId(), syncMessage.getDmpNum(), syncMessage.getTraceId());
                    } catch (Exception e) {
                        log.error("消费需求同步消息失败, msgId={}", msg.getMsgId(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
