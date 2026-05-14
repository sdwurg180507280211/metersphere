# MQ学习资料对接方式总结

本文总结 `/Users/zhaozhiwei/Desktop/mq学习资料` 中 MQ 示例项目的接入方式，用于后续 MeterSphere 全流程平台对接时参考。

## 1. 示例项目位置

示例项目路径：

```text
/Users/zhaozhiwei/Desktop/mq学习资料/mq-test-demo-1.0.1.02/mq-test-demo
```

主要文件：

```text
pom.xml
src/main/resources/message-queue.properties
src/main/java/com/hualife/mq/demo/controller/TestController.java
src/main/java/com/hualife/mq/demo/consumer/listener/DemoMessageListener1.java
src/main/java/com/hualife/mq/demo/consumer/listener/DemoMessageListener2.java
src/main/java/com/hualife/mq/demo/start/StartupAware.java
```

## 2. 整体结论

该示例使用的是内部封装 MQ SDK：

```text
com.hualife:mq-client-rocketmq
```

它不是原生 Apache RocketMQ 接入方式，也不是 CloudLaunch 接入方式。

应用代码没有直接使用：

```java
org.apache.rocketmq.client.producer.DefaultMQProducer
org.apache.rocketmq.client.consumer.DefaultMQPushConsumer
```

而是使用封装 SDK 提供的：

```java
com.hualife.mq.client.api.Message
com.hualife.mq.client.api.MessageListener
com.hualife.mq.client.impl.rocketmq.ProducerFactory
com.hualife.mq.client.impl.rocketmq.ConsumerFactory
```

底层仍然是 RocketMQ，但业务系统通过封装 SDK 访问。

## 3. Maven 依赖

示例项目 `pom.xml` 中核心依赖为：

```xml
<dependency>
    <groupId>com.hualife</groupId>
    <artifactId>mq-client-rocketmq</artifactId>
    <version>1.0.1.02-RELEASE</version>
</dependency>
```

这说明该 demo 依赖内部 Maven 包。如果 MeterSphere 后续要完全按此方式接入，也需要能获取这个私有依赖。

## 4. 配置文件

MQ 配置位于：

```text
src/main/resources/message-queue.properties
```

配置分为全局配置、Producer 配置、Consumer 配置。

### 4.1 全局配置

示例：

```properties
AgentModel=0
log.configfile.isUseExterConfig=true
log.configfile.position=log4j.properties
log.configfile.type=properties
failmessage.configfile.position=D:/logs/failmsgs.xls
```

含义大致为：

| 配置 | 说明 |
|---|---|
| `AgentModel` | SDK 运行模式配置 |
| `log.configfile.*` | MQ SDK 日志配置 |
| `failmessage.configfile.position` | 失败消息记录文件位置 |

### 4.2 Producer 配置

示例：

```properties
rocketmq.producers.producer-test-kfzzx.producer-id=producer-test-kfzzx
rocketmq.producers.producer-test-kfzzx.server-address=10.0.19.114:9876;10.0.19.115:9876;10.0.19.116:9876
rocketmq.producers.producer-test-kfzzx.send-timeout=3000

rocketmq.producers.producer-test-kfzzx2.producer-id=producer-test-kfzzx2
rocketmq.producers.producer-test-kfzzx2.server-address=10.0.19.114:9876;10.0.19.115:9876;10.0.19.116:9876
rocketmq.producers.producer-test-kfzzx2.send-timeout=3000
```

配置格式：

```properties
rocketmq.producers.<producer配置名>.producer-id=<Producer Group>
rocketmq.producers.<producer配置名>.server-address=<NameServer地址>
rocketmq.producers.<producer配置名>.send-timeout=<发送超时时间>
```

字段说明：

| 字段 | 说明 |
|---|---|
| `<producer配置名>` | 代码中通过 `ProducerFactory.getProducer(...)` 获取 Producer 时使用的名称 |
| `producer-id` | RocketMQ Producer Group |
| `server-address` | RocketMQ NameServer 地址，多个地址用分号分隔 |
| `send-timeout` | 消息发送超时时间，单位毫秒 |

### 4.3 Consumer 配置

示例：

```properties
rocketmq.consumers.consumer-wftest-testgrp1.consumer-id=consumer-wftest-testgrp1
rocketmq.consumers.consumer-wftest-testgrp1.message-listener=com.hualife.mq.demo.consumer.listener.DemoMessageListener1
rocketmq.consumers.consumer-wftest-testgrp1.topic=wftest
rocketmq.consumers.consumer-wftest-testgrp1.tag=TagA
rocketmq.consumers.consumer-wftest-testgrp1.consume-thread-nums=10
rocketmq.consumers.consumer-wftest-testgrp1.server-address=10.0.19.114:9876;10.0.19.115:9876;10.0.19.116:9876

rocketmq.consumers.consumer-wftest-testgrp2.consumer-id=consumer-wftest-testgrp2
rocketmq.consumers.consumer-wftest-testgrp2.message-listener=com.hualife.mq.demo.consumer.listener.DemoMessageListener2
rocketmq.consumers.consumer-wftest-testgrp2.topic=wftest
rocketmq.consumers.consumer-wftest-testgrp2.tag=TagB
rocketmq.consumers.consumer-wftest-testgrp2.consume-thread-nums=10
rocketmq.consumers.consumer-wftest-testgrp2.server-address=10.0.19.114:9876;10.0.19.115:9876;10.0.19.116:9876
```

配置格式：

```properties
rocketmq.consumers.<consumer配置名>.consumer-id=<Consumer Group>
rocketmq.consumers.<consumer配置名>.message-listener=<监听类全限定名>
rocketmq.consumers.<consumer配置名>.topic=<Topic>
rocketmq.consumers.<consumer配置名>.tag=<Tag>
rocketmq.consumers.<consumer配置名>.consume-thread-nums=<消费线程数>
rocketmq.consumers.<consumer配置名>.server-address=<NameServer地址>
```

字段说明：

| 字段 | 说明 |
|---|---|
| `<consumer配置名>` | Consumer 配置对象名 |
| `consumer-id` | RocketMQ Consumer Group |
| `message-listener` | 消息监听类全限定名 |
| `topic` | 订阅的 Topic |
| `tag` | 订阅的 Tag |
| `consume-thread-nums` | 消费线程数 |
| `server-address` | RocketMQ NameServer 地址 |

## 5. Producer 发送方式

Producer 示例代码位于：

```text
src/main/java/com/hualife/mq/demo/controller/TestController.java
```

关键导入：

```java
import com.hualife.mq.client.api.Message;
import com.hualife.mq.client.api.SendResult;
import com.hualife.mq.client.impl.configration.entity.MessageProducerBean;
import com.hualife.mq.client.impl.rocketmq.ProducerFactory;
```

核心流程：

```java
MessageProducerBean producer = ProducerFactory.getProducer("producer-test-kfzzx");

Message message = new Message();
message.setTopic("wftest");
message.setTag("TagA");
message.setBody(("Spring xml" + i).getBytes());
message.setKey("spring1_" + System.currentTimeMillis());

SendResult sendResult = producer.send(message);
String messageId = sendResult.getMessageId();
```

发送流程可以概括为：

```text
ProducerFactory.getProducer("producer配置名")
-> 构造 Message
-> 设置 topic/tag/body/key
-> producer.send(message)
-> 返回 SendResult
```

### 5.1 Message 关键字段

| 字段 | 说明 |
|---|---|
| `topic` | 消息发送 Topic |
| `tag` | 消息 Tag |
| `body` | 消息体，字节数组 |
| `key` | 消息 Key，建议用于业务追踪或幂等 |

### 5.2 SendResult

发送成功后可获取：

```java
sendResult.getMessageId()
```

建议日志中记录：

```text
topic
tag
messageId
key
业务主键
traceId
```

## 6. Consumer 消费方式

Consumer 示例代码位于：

```text
src/main/java/com/hualife/mq/demo/consumer/listener/DemoMessageListener1.java
src/main/java/com/hualife/mq/demo/consumer/listener/DemoMessageListener2.java
```

关键导入：

```java
import com.hualife.mq.client.api.Action;
import com.hualife.mq.client.api.ConsumeContext;
import com.hualife.mq.client.api.Message;
import com.hualife.mq.client.api.MessageListener;
```

监听类实现：

```java
public class DemoMessageListener1 implements MessageListener {

    @Override
    public Action consume(Message message, ConsumeContext context) {
        try {
            String messageId = message.getMsgID();
            String key = message.getKey();
            byte[] body = message.getBody();

            // 业务处理

            return Action.CommitMessage;
        } catch (Exception e) {
            return Action.ReconsumeLater;
        }
    }
}
```

消费结果：

| 返回值 | 含义 |
|---|---|
| `Action.CommitMessage` | 消费成功，提交消息 |
| `Action.ReconsumeLater` | 消费失败，稍后重试 |

## 7. Consumer 启动方式

Consumer 启动代码位于：

```text
src/main/java/com/hualife/mq/demo/start/StartupAware.java
```

核心逻辑：

```java
import com.hualife.mq.client.impl.rocketmq.ConsumerFactory;

public class StartupAware implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ConsumerFactory.consumerStart();
    }
}
```

即：Spring 容器启动完成后，调用：

```java
ConsumerFactory.consumerStart();
```

SDK 会读取 `message-queue.properties` 中的：

```properties
rocketmq.consumers.*
```

然后创建并启动 Consumer。

## 8. 完整链路

该 demo 的完整链路如下：

```text
应用启动
-> Spring 加载配置
-> StartupAware.setApplicationContext(...)
-> ConsumerFactory.consumerStart()
-> SDK 读取 rocketmq.consumers.* 配置
-> 启动 Consumer 订阅 topic/tag

浏览器/接口调用发送接口
-> TestController
-> ProducerFactory.getProducer("producer-test-kfzzx")
-> 构造 Message(topic, tag, body, key)
-> producer.send(message)
-> RocketMQ
-> DemoMessageListener.consume(...)
-> 返回 CommitMessage / ReconsumeLater
```

## 9. 与 MeterSphere 当前实现的关系

当前 MeterSphere 已经实现了原生 RocketMQ 接入：

```java
DefaultMQProducer
DefaultMQPushConsumer
```

并临时接入了 CloudLaunch 可选适配：

```java
com.ruiinsurance.cloudlaunch.rocketmq.api.producer.MessageProducer
com.ruiinsurance.cloudlaunch.rocketmq.api.consumer.AbstractMessageListener
```

而本资料中的接入方式是第三套：

```java
com.hualife.mq.client.*
```

三者关系如下：

| 方式 | 依赖 | 配置 | 代码风格 |
|---|---|---|---|
| 原生 RocketMQ | `org.apache.rocketmq:rocketmq-client` | 自定义 `rocketmq.name-server` 等 | `DefaultMQProducer` / `DefaultMQPushConsumer` |
| CloudLaunch | `com.ruiinsurance:cloudlaunch-message-rocketmq` | `cloudlaunch.message.broker-clusters.*` | 注入 `MessageProducer` / 继承 `AbstractMessageListener` |
| MQ学习资料示例 | `com.hualife:mq-client-rocketmq` | `rocketmq.producers.*` / `rocketmq.consumers.*` | `ProducerFactory` / `MessageListener` / `ConsumerFactory` |

## 10. 对 MeterSphere 的参考价值

该资料最有价值的信息是：

```text
10.0.19.114:9876;10.0.19.115:9876;10.0.19.116:9876
```

这组地址使用标准 RocketMQ NameServer 端口 `9876`。

因此，如果网络可达、权限允许，MeterSphere 使用原生 RocketMQ Client 也有较大概率可以连接该 MQ 集群。

可映射关系：

| MQ学习资料字段 | MeterSphere 原生 RocketMQ 配置 |
|---|---|
| `server-address` | `rocketmq.name-server` |
| `producer-id` | `rocketmq.producer.group` |
| `consumer-id` | `rocketmq.consumer.group` |
| `topic` | `rocketmq.topic.requirement-sync` |
| `tag` | `rocketmq.tag.requirement-sync` |
| `send-timeout` | 可映射为 Producer 发送超时 |
| `consume-thread-nums` | 可映射为 Consumer 消费线程数 |

## 11. 本项目正式 Topic

注意：MQ 学习资料中的：

```text
wftest
TagA
TagB
topic-Message-TEST
topic-SYNC-RDMS-UAT
```

都是示例或测试 Topic，不是 MeterSphere 全流程平台对接的正式 Topic。

根据 `docs/全流程平台对接/全流程平台对接功能需求说明书.docx`，本项目正式 Topic 是：

### 需求平台 -> MeterSphere

```text
topic-requirement-to-metersphere
```

相关 Group：

```text
Producer Group: producer-requirement-to-metersphere
Consumer Group: consumer-requirement-to-metersphere
```

### MeterSphere -> 需求平台

```text
topic-metersphere-to-requirement
```

相关 Group：

```text
Producer Group: producer-metersphere-to-requirement
Consumer Group: consumer-metersphere-to-requirement
```

## 12. 建议

短期建议继续优先验证原生 RocketMQ 方式：

```properties
requirement.sync.mq.client=rocketmq
rocketmq.name-server=10.0.19.114:9876;10.0.19.115:9876;10.0.19.116:9876
rocketmq.topic.requirement-sync=topic-requirement-to-metersphere
rocketmq.producer.group=producer-requirement-to-metersphere
rocketmq.consumer.group=consumer-requirement-to-metersphere
rocketmq.tag.requirement-sync=*
```

如果原生 RocketMQ Client 无法连接或对方要求必须使用内部 SDK，再考虑切换到：

```text
com.hualife:mq-client-rocketmq
```

或 CloudLaunch 封装方式。

不建议同时长期维护三套 MQ 接入实现，避免配置和排障复杂度过高。
