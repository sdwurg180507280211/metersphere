package io.metersphere.track.consumer;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequirementSyncConsumer implements InitializingBean {

    private static final String NAME_SERVER = "192.168.8.101:9876";
    private static final String CONSUMER_GROUP = "metersphere-consumer-group";
    private static final String TOPIC = "topic-requirement-to-metersphere";

    @Override
    public void afterPropertiesSet() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(CONSUMER_GROUP);
        consumer.setNamesrvAddr(NAME_SERVER);
        consumer.subscribe(TOPIC, "*");

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    try {
                        String body = new String(msg.getBody(), "UTF-8");
                        System.out.println("========================================");
                        System.out.println("收到需求消息:");
                        System.out.println("MsgId: " + msg.getMsgId());
                        System.out.println("内容: " + body);
                        System.out.println("========================================");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
        System.out.println("需求同步消费者启动成功，连接到: " + NAME_SERVER);
    }
}
