package io.metersphere.simulator;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

public class RequirementProducer {
    private DefaultMQProducer producer;
    private String nameServer;
    private String topic;

    public RequirementProducer(String nameServer, String topic) {
        this.nameServer = nameServer;
        this.topic = topic;
    }

    public void start() throws Exception {
        producer = new DefaultMQProducer("test-producer-group");
        producer.setNamesrvAddr(nameServer);
        producer.start();
        System.out.println("生产者启动成功，连接到: " + nameServer);
    }

    public void sendMessage(RequirementSyncMessage message) throws Exception {
        String json = JSON.toJSONString(message);
        Message msg = new Message(topic, json.getBytes("UTF-8"));
        SendResult result = producer.send(msg);
        System.out.println("消息发送成功: " + result.getMsgId());
        System.out.println("消息内容: " + json);
    }

    public void shutdown() {
        if (producer != null) {
            producer.shutdown();
            System.out.println("生产者已关闭");
        }
    }
}
