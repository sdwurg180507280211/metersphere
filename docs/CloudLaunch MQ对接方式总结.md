# CloudLaunch MQ对接方式总结

本文总结 `/Users/zhaozhiwei/Desktop/ruiinsurance` 中 CloudLaunch RocketMQ 封装依赖的接入方式，用于 MeterSphere 全流程平台对接时参考。

## 1. 资料位置

本地依赖目录：

```text
/Users/zhaozhiwei/Desktop/ruiinsurance
```

该目录不是完整应用工程，而是一组 Maven 本地依赖包和源码包，主要包括：

```text
cloudlaunch-message-rocketmq/1.0.0
mq-client-rocketmq/2.0.0-RELEASE
mq-client/2.0.0-RELEASE
cloudlaunch-common/1.0.0
cloudlaunch-util/1.0.0
cloudlaunch-logback-appender/1.0.0
cloudlaunch-dependencies/1.0.0
cloudlaunch/1.0.0
mq-client-parent/2.0.0-RELEASE
```

## 2. 整体结论

CloudLaunch MQ 是瑞保内部对 RocketMQ 的封装。

业务系统不直接使用：

```java
org.apache.rocketmq.client.producer.DefaultMQProducer
org.apache.rocketmq.client.consumer.DefaultMQPushConsumer
```

而是使用 CloudLaunch 提供的封装 API：

```java
com.ruiinsurance.cloudlaunch.rocketmq.api.producer.MessageProducer
com.ruiinsurance.cloudlaunch.rocketmq.api.consumer.AbstractMessageListener
com.ruiinsurance.cloudlaunch.rocketmq.api.Message
```

CloudLaunch 会通过 Spring Boot 自动配置读取：

```properties
cloudlaunch.message.broker-clusters.*
```

并自动注册 Producer / Consumer Bean。

## 3. 核心依赖

核心依赖为：

```xml
<dependency>
    <groupId>com.ruiinsurance</groupId>
    <artifactId>cloudlaunch-message-rocketmq</artifactId>
    <version>1.0.0</version>
</dependency>
```

该依赖内部依赖：

```xml
<dependency>
    <groupId>com.ruiinsurance</groupId>
    <artifactId>mq-client-rocketmq</artifactId>
    <version>2.0.0-RELEASE</version>
</dependency>
```

底层 RocketMQ 版本由 `mq-client-parent` 管理，主要是：

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>4.9.2</version>
</dependency>
```

## 4. Spring Boot 自动配置

`cloudlaunch-message-rocketmq-1.0.0.jar` 中包含：

```text
META-INF/spring.factories
```

内容包含自动配置类：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.ruiinsurance.cloudlaunch.rocketmq.RocketMQAutoConfiguration
```

主要自动配置类：

```java
com.ruiinsurance.cloudlaunch.rocketmq.RocketMQAutoConfiguration
```

它会注册：

```java
com.ruiinsurance.cloudlaunch.rocketmq.RocketMQBeanRegistrar
```

`RocketMQBeanRegistrar` 会读取 Spring Environment 中的配置项，并根据配置动态注册 Producer 和 Consumer。

## 5. 配置根路径

CloudLaunch 读取的配置根路径为：

```text
cloudlaunch.message.broker-clusters
```

内部常量为：

```java
public static final String BROKER_PREFIX = "cloudlaunch.message.broker-clusters";
```

配置整体结构：

```yaml
cloudlaunch:
  message:
    broker-clusters:
      <clusterName>:
        server-address: <NameServer地址>
        producers:
          <producerBeanName>:
            producer-id: <Producer Group>
            topic: <Topic>
            tag: <Tag>
            send-timeout: <发送超时毫秒>
        consumers:
          <consumerBeanName>:
            consumer-id: <Consumer Group>
            message-listener: <监听类全限定名>
            topic: <Topic>
            tag: <Tag>
            consume-thread-nums: <消费线程数>
            consumeTimeout: <消费超时>
            MessageModel: CLUSTERING
            ConsumeFromWhere: CONSUME_FROM_FIRST_OFFSET
```

## 6. Producer 配置方式

示例 YAML：

```yaml
cloudlaunch:
  message:
    broker-clusters:
      broker-int:
        server-address: 10.0.19.114:9876;10.0.19.115:9876;10.0.19.116:9876
        producers:
          producer-aiuw:
            producer-id: producer-Message-TEST
            topic: topic-Message-TEST
            tag: da
            send-timeout: 3000
```

等价 properties 写法：

```properties
cloudlaunch.message.broker-clusters.broker-int.server-address=10.0.19.114:9876;10.0.19.115:9876;10.0.19.116:9876
cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw.producer-id=producer-Message-TEST
cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw.topic=topic-Message-TEST
cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw.tag=da
cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw.send-timeout=3000
```

字段说明：

| 字段 | 含义 |
|---|---|
| `broker-int` | 逻辑集群名 |
| `server-address` | RocketMQ NameServer 地址 |
| `producer-aiuw` | Spring Producer Bean 名 |
| `producer-id` | RocketMQ Producer Group |
| `topic` | 该 Producer 默认发送的 Topic |
| `tag` | 该 Producer 默认发送的 Tag |
| `send-timeout` | 发送超时时间，单位毫秒 |

## 7. Producer Bean 注册逻辑

对于配置：

```properties
cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw.*
```

CloudLaunch 会注册一个 Spring Bean：

```text
producer-aiuw
```

Bean 类型：

```java
com.ruiinsurance.cloudlaunch.rocketmq.producer.MessageProducerBean
```

业务代码中按名称注入：

```java
@Resource(name = "producer-aiuw")
private MessageProducer messageProducer;
```

接口类型：

```java
com.ruiinsurance.cloudlaunch.rocketmq.api.producer.MessageProducer
```

## 8. Producer 发送代码

CloudLaunch 应用侧发送消息使用：

```java
import com.ruiinsurance.cloudlaunch.rocketmq.api.Message;
import com.ruiinsurance.cloudlaunch.rocketmq.api.producer.MessageProducer;
import com.ruiinsurance.cloudlaunch.rocketmq.api.producer.SendResult;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;

public class DemoProducer {

    @Resource(name = "producer-aiuw")
    private MessageProducer messageProducer;

    public void send(String key, String body) {
        Message message = new Message();
        message.setMessageKey(key);
        message.setMessageBody(body.getBytes(StandardCharsets.UTF_8));

        SendResult result = messageProducer.send(message);

        String messageId = result.getMessageId();
    }
}
```

注意：

- `Message.messageBody` 类型是 `byte[]`。
- `topic` 和 `tag` 通常来自配置，不需要在每次发送时手动设置。
- `messageKey` 建议设置为业务追踪 ID 或业务唯一键。

## 9. Producer API

接口：

```java
com.ruiinsurance.cloudlaunch.rocketmq.api.producer.MessageProducer
```

方法：

```java
SendResult send(Message message);
void sendOneway(Message message);
void sendAsync(Message message, com.ruiinsurance.mq.client.api.SendCallback callback);
```

发送结果：

```java
com.ruiinsurance.cloudlaunch.rocketmq.api.producer.SendResult
```

常用方法：

```java
result.getMessageId();
result.getSendStatus();
result.getTopic();
```

## 10. Consumer 配置方式

示例 YAML：

```yaml
cloudlaunch:
  message:
    broker-clusters:
      broker-int:
        server-address: 10.0.19.114:9876;10.0.19.115:9876;10.0.19.116:9876
        consumers:
          consumer-auth:
            consumer-id: consumer-SYNC-RDMS-UAT
            message-listener: com.ruiinsurance.demo.consumer.ConsumerAuth
            topic: topic-SYNC-RDMS-UAT
            tag:
            consume-thread-nums: 5
            consume-timeout: 15
```

等价 properties 写法：

```properties
cloudlaunch.message.broker-clusters.broker-int.consumers.consumer-auth.consumer-id=consumer-SYNC-RDMS-UAT
cloudlaunch.message.broker-clusters.broker-int.consumers.consumer-auth.message-listener=com.ruiinsurance.demo.consumer.ConsumerAuth
cloudlaunch.message.broker-clusters.broker-int.consumers.consumer-auth.topic=topic-SYNC-RDMS-UAT
cloudlaunch.message.broker-clusters.broker-int.consumers.consumer-auth.tag=
cloudlaunch.message.broker-clusters.broker-int.consumers.consumer-auth.consume-thread-nums=5
cloudlaunch.message.broker-clusters.broker-int.consumers.consumer-auth.consume-timeout=15
```

字段说明：

| 字段 | 含义 |
|---|---|
| `consumer-auth` | Spring Consumer Bean 名 |
| `consumer-id` | RocketMQ Consumer Group |
| `message-listener` | 消息监听类全限定名 |
| `topic` | 订阅 Topic |
| `tag` | 订阅 Tag，空或 `*` 表示全部 |
| `consume-thread-nums` | 消费线程数 |
| `consume-timeout` | 消费超时 |

## 11. Consumer Bean 注册逻辑

对于配置：

```properties
cloudlaunch.message.broker-clusters.broker-int.consumers.consumer-auth.*
```

CloudLaunch 会注册两个 Bean：

### 11.1 Listener Bean

Bean 名：

```text
consumer-auth-message-listener
```

Bean Class 来自配置：

```properties
message-listener=com.ruiinsurance.demo.consumer.ConsumerAuth
```

### 11.2 Consumer Bean

Bean 名：

```text
consumer-auth
```

Bean 类型：

```java
com.ruiinsurance.mq.client.api.bean.ConsumerBean
```

Consumer 会订阅配置中的：

```text
topic + tag
```

并将消息转发给对应 Listener。

## 12. Consumer 监听类写法

监听类继承：

```java
com.ruiinsurance.cloudlaunch.rocketmq.api.consumer.AbstractMessageListener
```

实现方法：

```java
public Action consume(Message message)
```

示例：

```java
import com.ruiinsurance.cloudlaunch.rocketmq.api.Message;
import com.ruiinsurance.cloudlaunch.rocketmq.api.consumer.AbstractMessageListener;
import com.ruiinsurance.cloudlaunch.rocketmq.api.consumer.Action;

import java.nio.charset.StandardCharsets;

public class DemoConsumer extends AbstractMessageListener {

    @Override
    public Action consume(Message message) {
        try {
            String messageId = message.getMessageId();
            String key = message.getMessageKey();
            String body = new String(message.getMessageBody(), StandardCharsets.UTF_8);

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

## 13. 配置键名注意事项

从依赖代码分析到的字段包括：

```text
consumeTimeout
MessageModel
ConsumeFromWhere
```

部分截图或示例中写法是：

```text
consume-timeout
```

因此落地时需要以实际依赖版本支持的键名为准。保守建议同时确认或使用依赖代码中明确读取的键名：

```properties
cloudlaunch.message.broker-clusters.broker-int.consumers.consumer-auth.consumeTimeout=15
cloudlaunch.message.broker-clusters.broker-int.consumers.consumer-auth.MessageModel=CLUSTERING
cloudlaunch.message.broker-clusters.broker-int.consumers.consumer-auth.ConsumeFromWhere=CONSUME_FROM_FIRST_OFFSET
```

## 14. 完整链路

CloudLaunch 完整链路如下：

```text
应用启动
-> Spring Boot 加载 cloudlaunch-message-rocketmq 自动配置
-> RocketMQBeanRegistrar 扫描 cloudlaunch.message.broker-clusters.*
-> 根据 producers.* 注册 MessageProducerBean
-> 根据 consumers.* 注册 ConsumerBean 和 Listener Bean
-> Producer Bean start
-> Consumer Bean start

业务发送消息
-> 注入 @Resource(name = "producer-aiuw") MessageProducer
-> 构造 Message(messageKey, messageBody)
-> messageProducer.send(message)
-> CloudLaunch 根据配置填充 topic/tag
-> 底层 RocketMQ Producer 发送

业务消费消息
-> CloudLaunch Consumer 收到 RocketMQ 消息
-> 转换为 CloudLaunch Message
-> 调用 AbstractMessageListener.consume(message)
-> 返回 CommitMessage / ReconsumeLater
```

## 15. 与其他接入方式对比

| 方式 | 依赖 | 配置 | 代码风格 |
|---|---|---|---|
| 原生 RocketMQ | `org.apache.rocketmq:rocketmq-client` | 自定义 `rocketmq.name-server` 等 | `DefaultMQProducer` / `DefaultMQPushConsumer` |
| CloudLaunch | `com.ruiinsurance:cloudlaunch-message-rocketmq` | `cloudlaunch.message.broker-clusters.*` | 注入 `MessageProducer` / 继承 `AbstractMessageListener` |
| MQ学习资料示例 | `com.hualife:mq-client-rocketmq` | `rocketmq.producers.*` / `rocketmq.consumers.*` | `ProducerFactory` / `MessageListener` / `ConsumerFactory` |

## 16. MeterSphere 当前适配方式

当前 MeterSphere 已接入 CloudLaunch 可选适配：

### 16.1 发送接口抽象

```java
io.metersphere.requirement.pool.producer.RequirementSyncMessageSender
```

原生 RocketMQ 实现：

```java
io.metersphere.requirement.pool.producer.RequirementSyncProducer
```

CloudLaunch 实现：

```java
io.metersphere.requirement.pool.producer.RequirementSyncCloudlaunchProducer
```

### 16.2 CloudLaunch Producer

注入 Bean：

```java
@Resource(name = "producer-aiuw")
private MessageProducer messageProducer;
```

发送：

```java
Message message = new Message();
message.setMessageKey(msg.getTraceId());
message.setMessageBody(body.getBytes(StandardCharsets.UTF_8));
messageProducer.send(message);
```

### 16.3 CloudLaunch Consumer

监听类：

```java
io.metersphere.track.consumer.RequirementSyncCloudlaunchListener
```

继承：

```java
AbstractMessageListener
```

实现：

```java
public Action consume(Message message)
```

## 17. MeterSphere 配置示例

### 17.1 本地默认原生 RocketMQ

不配置时默认启用：

```properties
requirement.sync.mq.client=rocketmq
```

### 17.2 CloudLaunch 模式

启用 CloudLaunch：

```properties
requirement.sync.mq.client=cloudlaunch
```

需求同步 Topic 应按本项目正式文档配置。

根据 `docs/全流程平台对接/全流程平台对接功能需求说明书.docx`：

```text
需求平台 -> MeterSphere: topic-requirement-to-metersphere
MeterSphere -> 需求平台: topic-metersphere-to-requirement
```

当前需求池页面模拟的是“需求平台 -> MeterSphere”，因此 Producer/Consumer 都使用：

```text
topic-requirement-to-metersphere
```

示例：

```properties
requirement.sync.mq.client=cloudlaunch

cloudlaunch.message.broker-clusters.broker-int.server-address=10.0.19.114:9876;10.0.19.115:9876;10.0.19.116:9876

cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw.producer-id=producer-requirement-to-metersphere
cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw.topic=topic-requirement-to-metersphere
cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw.tag=*
cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw.send-timeout=3000

cloudlaunch.message.broker-clusters.broker-int.consumers.requirementSyncConsumer.consumer-id=consumer-requirement-to-metersphere
cloudlaunch.message.broker-clusters.broker-int.consumers.requirementSyncConsumer.message-listener=io.metersphere.track.consumer.RequirementSyncCloudlaunchListener
cloudlaunch.message.broker-clusters.broker-int.consumers.requirementSyncConsumer.topic=topic-requirement-to-metersphere
cloudlaunch.message.broker-clusters.broker-int.consumers.requirementSyncConsumer.tag=*
cloudlaunch.message.broker-clusters.broker-int.consumers.requirementSyncConsumer.consume-thread-nums=5
cloudlaunch.message.broker-clusters.broker-int.consumers.requirementSyncConsumer.consumeTimeout=15
cloudlaunch.message.broker-clusters.broker-int.consumers.requirementSyncConsumer.MessageModel=CLUSTERING
cloudlaunch.message.broker-clusters.broker-int.consumers.requirementSyncConsumer.ConsumeFromWhere=CONSUME_FROM_FIRST_OFFSET
```

## 18. 注意事项

### 18.1 外网环境不要启用 CloudLaunch 内网配置

当前外网环境无法访问内网 MQ 地址时，不要配置：

```properties
requirement.sync.mq.client=cloudlaunch
```

也不要放入真实内网：

```properties
cloudlaunch.message.broker-clusters.*
```

否则应用启动时会尝试连接内网 MQ，可能导致启动失败或阻塞。

### 18.2 Producer Bean 名必须和代码一致

当前 MeterSphere CloudLaunch Producer 注入的是：

```java
@Resource(name = "producer-aiuw")
```

因此配置中 Producer 名必须是：

```properties
cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw.*
```

如果配置名换成其他值，代码中的 `@Resource(name = "producer-aiuw")` 也要同步调整。

### 18.3 message-listener 必须写 MeterSphere 监听类

当前 MeterSphere CloudLaunch Consumer 监听类为：

```text
io.metersphere.track.consumer.RequirementSyncCloudlaunchListener
```

配置必须保持一致：

```properties
cloudlaunch.message.broker-clusters.broker-int.consumers.requirementSyncConsumer.message-listener=io.metersphere.track.consumer.RequirementSyncCloudlaunchListener
```

### 18.4 Topic 要使用项目正式 Topic

CloudLaunch 示例截图中的：

```text
topic-Message-TEST
topic-SYNC-RDMS-UAT
```

只是示例或测试 Topic，不是本项目正式 Topic。

本项目需求同步正式 Topic 是：

```text
topic-requirement-to-metersphere
```

状态回传正式 Topic 是：

```text
topic-metersphere-to-requirement
```

## 19. 建议

如果内网要求统一使用 CloudLaunch SDK，则使用本文中的 CloudLaunch 方案。

如果内网允许直接使用 Apache RocketMQ 原生 Client，并且 NameServer 网络可达，则可以优先使用原生 RocketMQ 方案，减少私有依赖引入。

不建议同时长期维护三套 MQ 接入实现：

```text
原生 RocketMQ
CloudLaunch
Hualife mq-client-rocketmq
```

最终应根据内网平台要求收敛到一种生产接入方式。
