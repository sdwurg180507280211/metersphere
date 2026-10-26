# RocketMQ 双模式接入说明

## 背景

需求池需求同步当前支持两种 MQ 接入方式：

```properties
requirement.sync.mq.client=rocketmq
```

或：

```properties
requirement.sync.mq.client=cloudlaunch
```

两种方式底层都是 RocketMQ，区别在于接入层不同：

- `rocketmq`：MeterSphere 直接使用 RocketMQ 官方客户端。
- `cloudlaunch`：MeterSphere 使用瑞保 CloudLaunch 封装 SDK，SDK 底层再调用 RocketMQ 客户端。

正式环境已验证两种方式均可跑通。

---

## 原生 RocketMQ 模式

### 配置示例

```properties
requirement.sync.mq.client=rocketmq

rocketmq.name-server=10.0.2.92:2876;10.0.2.93:2876;10.0.2.94:2876
rocketmq.topic.requirement-sync=topic-requirement-to-metersphere
rocketmq.producer.group=producer-requirement-to-metersphere
rocketmq.consumer.group=consumer-requirement-to-metersphere
```

`rocketmq.name-server` 支持集群地址，多个地址使用分号分隔。

### 代码方式

原生方式直接使用 RocketMQ 官方客户端：

```java
DefaultMQProducer
DefaultMQPushConsumer
```

Producer 手动创建、启动和关闭：

```java
producer = new DefaultMQProducer(producerGroup);
producer.setNamesrvAddr(nameServer);
producer.start();
```

Consumer 手动创建、订阅和注册监听器：

```java
consumer = new DefaultMQPushConsumer(consumerGroup);
consumer.setNamesrvAddr(nameServer);
consumer.subscribe(topic, "*");
consumer.registerMessageListener(...);
consumer.start();
```

### 日志示例

```text
[需求MQ-模拟生产者] 创建按钮触发发送
[需求MQ-发送] client=rocketmq
[需求MQ-发送成功] sendStatus=SEND_OK
[需求MQ-接收]
[需求MQ-业务分发]
[需求MQ-落库完成]
[需求MQ-消费成功]
```

---

## CloudLaunch 模式

### 配置示例

```properties
requirement.sync.mq.client=cloudlaunch

cloudlaunch.message.broker-clusters.broker-int.server-address=10.0.2.92:2876;10.0.2.93:2876;10.0.2.94:2876

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

### 代码方式

CloudLaunch 方式使用瑞保封装 SDK：

```java
MessageProducer
AbstractMessageListener
```

Producer 通过配置名注入：

```java
@Resource(name = "producer-aiuw")
private MessageProducer messageProducer;
```

Consumer Listener 继承封装类：

```java
public class RequirementSyncCloudlaunchListener extends AbstractMessageListener
```

CloudLaunch SDK 会读取 `cloudlaunch.message.broker-clusters` 配置，自动注册 Producer 和 Consumer Bean。

---

## 对比

| 维度 | 原生 RocketMQ | CloudLaunch |
|---|---|---|
| 底层协议 | RocketMQ | RocketMQ |
| 客户端 | 官方 `rocketmq-client` | 瑞保封装 SDK，底层仍是 `rocketmq-client` |
| 配置前缀 | `rocketmq.*` | `cloudlaunch.message.*` |
| Producer 创建 | MeterSphere 手动创建 | CloudLaunch 自动注册 |
| Consumer 创建 | MeterSphere 手动创建 | CloudLaunch 自动注册 |
| Topic 配置 | MeterSphere 读取 | CloudLaunch 配置绑定 |
| Tag 支持 | 当前 Consumer 订阅 `*`，Producer 未显式设置 tag | 配置中天然支持 tag |
| 生命周期 | MeterSphere 自己管理 `start/shutdown` | SDK 管理 |
| 日志可控性 | 高 | 中等，部分逻辑在封装层 |
| 调试难度 | 低 | 中等偏高 |
| 内网规范适配 | 取决于是否允许原生客户端 | 更符合内网封装规范 |
| 依赖复杂度 | 低 | 高，需要私有依赖 |
| 可移植性 | 高 | 低，依赖瑞保私有包 |

---

## 原生 RocketMQ 优点

### 简单直接

调用链短：

```text
MeterSphere -> RocketMQ Client -> MQ Server
```

问题排查更直接，可以直接看：

```text
nameServer
topic
producer group
consumer group
msgId
sendStatus
subscribe
consume result
```

### 依赖少

主要依赖：

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>4.9.2</version>
</dependency>
```

不依赖瑞保私有 Maven 包。

### 跨环境更方便

开发、本地、测试、正式都可以统一使用官方 RocketMQ 方式。

只要网络、Topic、Group 权限正确即可。

### 可控性强

可以直接控制：

```text
发送超时
重试次数
消费线程数
消费模式
tag
消息 key
消费失败策略
日志
异常处理
```

---

## 原生 RocketMQ 缺点

### 可能不符合内网统一接入规范

如果公司要求所有系统都通过 CloudLaunch 封装 SDK 接入 MQ，则原生方式虽然能跑通，但可能不符合规范要求。

### 生命周期需要自行维护

MeterSphere 需要自己管理：

```java
producer.start();
producer.shutdown();
consumer.start();
consumer.shutdown();
```

### 当前 tag 支持不完整

当前原生 Producer 未显式设置 tag：

```java
new Message(topic, body)
```

Consumer 订阅所有 tag：

```java
consumer.subscribe(topic, "*")
```

如果正式 Topic 后续要求 tag 隔离，建议补充配置：

```properties
rocketmq.tag.requirement-sync=*
```

---

## CloudLaunch 优点

### 符合瑞保内网封装规范

配置结构与内网给出的标准一致：

```yaml
cloudlaunch:
  message:
    broker-clusters:
      broker-int:
        producers:
        consumers:
```

如果生产运维或架构组要求使用统一封装，CloudLaunch 更容易满足规范。

### 配置能力完整

天然支持：

```text
broker cluster
producer-id
topic
tag
send-timeout
consumer-id
message-listener
consume-thread-nums
consumeTimeout
MessageModel
ConsumeFromWhere
```

### 生命周期由 SDK 管理

业务代码只需要注入 Producer 或实现 Listener。

Producer：

```java
@Resource(name = "producer-aiuw")
private MessageProducer messageProducer;
```

Consumer：

```java
extends AbstractMessageListener
```

### 贴近内网已有系统

如果其他系统都使用 CloudLaunch，对接、排查、运维口径更统一。

---

## CloudLaunch 缺点

### 私有依赖复杂

需要私有依赖：

```text
cloudlaunch-message-rocketmq
mq-client
mq-client-rocketmq
cloudlaunch-common
cloudlaunch-util
cloudlaunch-logback-appender
相关 parent/bom pom
```

如果构建机没有私服或本地包，编译会失败。

### 排查链路更长

调用链更长：

```text
MeterSphere -> CloudLaunch SDK -> mq-client -> RocketMQ Client -> MQ Server
```

问题可能出在：

```text
MeterSphere 配置
CloudLaunch 自动注册 Bean
producer bean 名
listener class 名
SDK 封装逻辑
底层 RocketMQ
```

### 配置容易写错

Producer Bean 名必须和代码注入名称一致：

```properties
cloudlaunch.message.broker-clusters.broker-int.producers.producer-aiuw...
```

代码中对应：

```java
@Resource(name = "producer-aiuw")
```

Consumer Listener 也必须配置完整类名：

```properties
cloudlaunch.message.broker-clusters.broker-int.consumers.requirementSyncConsumer.message-listener=io.metersphere.track.consumer.RequirementSyncCloudlaunchListener
```

### 自动注册不如原生直观

CloudLaunch 会扫描：

```text
cloudlaunch.message.broker-clusters
```

然后动态注册 Producer 和 Consumer。这个过程较隐式，排查时需要理解 SDK 的自动注册逻辑。

---

## 选型建议

正式环境已验证原生 RocketMQ 可以完整跑通：

```text
发送 -> 接收 -> 业务分发 -> 落库 -> 消费成功
```

因此当前建议：

```text
生产优先使用原生 RocketMQ
CloudLaunch 保留为兼容备用
```

推荐原生 RocketMQ 的原因：

```text
1. 正式环境已跑通。
2. 日志清晰。
3. 依赖更少。
4. 排查更直接。
5. 不依赖 CloudLaunch 私有 SDK 的构建环境。
```

生产优先配置：

```properties
requirement.sync.mq.client=rocketmq
```

以下场景再切换到 CloudLaunch：

```text
1. 架构或运维要求必须使用 CloudLaunch SDK。
2. 原生 RocketMQ 后续被权限限制。
3. 对方只提供 CloudLaunch 配置，不提供原生接入说明。
4. 需要严格复用内网统一 MQ 接入规范。
```

切换方式：

```properties
requirement.sync.mq.client=cloudlaunch
```

---

## 最终结论

原生 RocketMQ：更简单、更透明、更好排查，当前建议作为主用方式。

CloudLaunch：更符合内网封装规范，但依赖和排查复杂，建议作为兼容备用方式。

当前双模式设计可以同时满足：

```text
正式环境已跑通的原生链路
内网 CloudLaunch SDK 规范接入
```
