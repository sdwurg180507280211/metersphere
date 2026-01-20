# MeterSphere 中的 Kafka 应用与学习指南

## 一、概述

MeterSphere 使用 **Kafka 3.6.1** 作为核心消息中间件，实现微服务之间的异步通信、事件驱动和解耦。本文档详细介绍 Kafka 在项目中的应用场景、配置方式和最佳实践。

---

## 二、Kafka 在 MeterSphere 中的核心作用

### 2.1 主要应用场景

| 场景 | 说明 | 优势 |
|------|------|------|
| **测试执行结果回传** | JMeter/API 测试执行结果通过 Kafka 异步回传 | 解耦执行引擎和结果处理，支持高并发 |
| **微服务事件通知** | 项目创建/删除、测试计划删除等事件广播 | 各服务独立监听，松耦合 |
| **插件热加载通知** | 插件上传/删除后通知所有服务实例 | 实现分布式环境下的插件同步 |
| **实时调试结果推送** | 调试模式下通过 Kafka + WebSocket 实时推送 | 低延迟、高吞吐 |
| **报告清理调度** | 定时清理任务通过 Kafka 触发 | 异步处理，不阻塞主流程 |

---

## 三、Kafka 主题（Topic）定义

### 3.1 主题常量类

**位置**: `framework/sdk-parent/sdk/src/main/java/io/metersphere/commons/constants/KafkaTopicConstants.java`

```java
public interface KafkaTopicConstants {
    // 项目管理相关
    String PROJECT_CREATED_TOPIC = "PROJECT_CREATED";      // 项目创建事件
    String PROJECT_DELETED_TOPIC = "PROJECT_DELETED";      // 项目删除事件
    
    // 测试计划相关
    String TEST_PLAN_DELETED_TOPIC = "TEST_PLAN_DELETED";  // 测试计划删除事件
    String TEST_PLAN_REPORT_TOPIC = "TEST_PLAN_REPORT_TOPIC"; // 测试计划报告
    
    // API 测试相关
    String API_REPORT_TOPIC = "ms-api-exec-topic";         // API 执行结果（生产）
    String DEBUG_TOPICS = "MS-API-DEBUG-TOPIC";            // API 调试结果
    String UI_DEBUG_TOPICS = "MS-UI-DEBUG-TOPIC";          // UI 调试结果
    
    // 系统管理相关
    String PLATFORM_PLUGIN = "PLATFORM_PLUGIN";            // 平台插件变更通知
    String CLEAN_UP_REPORT_SCHEDULE = "CLEAN_UP_REPORT_SCHEDULE"; // 报告清理调度
}
```

### 3.2 主题用途详解

#### 3.2.1 API_REPORT_TOPIC（核心主题）
- **生产者**: JMeter 执行引擎（通过 KafkaBackendClient）
- **消费者**: `api-test` 模块的 `MsKafkaListener`
- **消息内容**: 测试执行结果（RequestResult JSON）
- **消费模式**: 批量消费（batch mode），每批最多 100 条
- **特点**: 
  - 使用手动提交偏移量（MANUAL_IMMEDIATE）
  - 支持线程池并发处理（10 个核心线程）
  - 消息 key 为报告 ID，value 为执行结果


#### 3.2.2 DEBUG_TOPICS（调试主题）
- **生产者**: JMeter 调试执行引擎
- **消费者**: `api-test` 模块的 `MsKafkaListener.debugConsume()`
- **消息内容**: 调试结果 + WebSocket 推送信息
- **消费模式**: 单条消费
- **特点**:
  - 每个服务实例使用唯一 groupId（`${random.uuid}`）
  - 消费后立即通过 WebSocket 推送给前端
  - 支持误报库信息解析

#### 3.2.3 PROJECT_CREATED_TOPIC / PROJECT_DELETED_TOPIC
- **生产者**: `system-setting` 模块的 `SystemProjectService`
- **消费者**: 各业务模块（api-test、test-track、performance-test）
- **消息内容**: 项目 ID（String）
- **用途**: 
  - 创建项目时：初始化各模块的项目默认数据（如测试计划节点、API 模块节点）
  - 删除项目时：级联删除各模块的项目相关数据

#### 3.2.4 PLATFORM_PLUGIN
- **生产者**: `system-setting` 模块的 `PlatformPluginService`
- **消费者**: 各服务实例的 `PlatformPluginListener`
- **消息内容**: `ADD:<pluginId>` 或 `DELETE:<pluginId>`
- **用途**: 插件热加载/卸载，无需重启服务

---

## 四、Kafka 配置详解

### 4.1 配置属性类

**位置**: `framework/sdk-parent/sdk/src/main/java/io/metersphere/config/KafkaProperties.java`

```java
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {
    private String bootstrapServers;        // Kafka 集群地址
    private String maxRequestSize;          // 最大请求大小
    private String acks = "1";              // 确认机制（不设置 all，避免性能问题）
    private String queueSize = "20000";     // Backend Listener 队列大小
    
    // SSL 配置（生产环境）
    private Ssl ssl = new Ssl();
    
    // 日志主题配置
    private Log log = new Log();
    
    // 报告主题配置
    private Report report = new Report();
}
```

### 4.2 消费者配置（KafkaConfig）

**位置**: `api-test/backend/src/main/java/io/metersphere/commons/config/KafkaConfig.java`


```java
@Configuration
public class KafkaConfig {
    
    // 批量消费工厂配置
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> batchFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerConfigs()));
        factory.setConcurrency(1);              // 并发消费者数量
        factory.setBatchListener(true);         // 开启批量监听
        factory.getContainerProperties().setPollTimeout(5000L);  // 拉取超时
        
        // 手动提交偏移量（确保消息处理完成后再提交）
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        return factory;
    }
    
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.MAX_REQUEST_SIZE_CONFIG, kafkaProperties.getMaxRequestSize());
        
        // 批量拉取配置
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);  // 每批最多 100 条
        
        // 消费者健康检查
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 900000);  // 15 分钟
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 5000);   // 5 秒心跳
        
        // 手动提交
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // 从最早的消息开始消费（防止消息丢失）
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // 反序列化器
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        return props;
    }
}
```

### 4.3 关键配置参数说明

| 参数 | 值 | 说明 | 影响 |
|------|-----|------|------|
| `MAX_POLL_RECORDS_CONFIG` | 100 | 每次拉取最大消息数 | 批量处理效率 vs 内存占用 |
| `MAX_POLL_INTERVAL_MS_CONFIG` | 900000 (15分钟) | 两次拉取最大间隔 | 超时会触发 rebalance |
| `HEARTBEAT_INTERVAL_MS_CONFIG` | 5000 (5秒) | 心跳间隔 | 检测消费者存活 |
| `ENABLE_AUTO_COMMIT_CONFIG` | false | 禁用自动提交 | 手动控制，防止消息丢失 |
| `AUTO_OFFSET_RESET_CONFIG` | earliest | 从最早消息开始 | 新消费者不会丢失历史消息 |
| `AckMode` | MANUAL_IMMEDIATE | 手动立即提交 | 处理完立即提交偏移量 |


---

## 五、Kafka 使用实例分析

### 5.1 生产者示例：项目创建事件

**位置**: `system-setting/backend/src/main/java/io/metersphere/service/SystemProjectService.java`

```java
@Service
public class SystemProjectService {
    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;
    
    public String addProject(Project project) {
        // ... 创建项目逻辑 ...
        
        // 发送项目创建事件（异步通知各模块初始化数据）
        kafkaTemplate.send(KafkaTopicConstants.PROJECT_CREATED_TOPIC, project.getId());
        LogUtil.info("send create_project message, project id: " + project.getId());
        
        return project.getId();
    }
    
    public void deleteProject(String projectId) {
        // ... 删除项目逻辑 ...
        
        // 发送项目删除事件（异步通知各模块清理数据）
        kafkaTemplate.send(KafkaTopicConstants.PROJECT_DELETED_TOPIC, projectId);
        LogUtil.info("send delete_project message, project id: " + projectId);
    }
}
```

**关键点**:
- 使用 `KafkaTemplate<String, String>` 发送消息
- 消息内容为简单的项目 ID 字符串
- 发送后立即返回，不等待消费结果（异步）

---

### 5.2 消费者示例：API 测试结果处理

**位置**: `api-test/backend/src/main/java/io/metersphere/api/jmeter/MsKafkaListener.java`

```java
@Configuration
public class MsKafkaListener {
    public static final String CONSUME_ID = "ms-api-exec-consume";
    
    @Resource
    private ApiExecutionQueueService apiExecutionQueueService;
    @Resource
    private TestResultService testResultService;
    
    // 线程池配置（处理消费到的消息）
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
        10,  // 核心线程数
        10,  // 最大线程数
        1, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(10000),  // 队列大小
        new NamedThreadFactory("MS-KAFKA-LISTENER-TASK")
    );
    
    // 批量消费 API 执行结果
    @KafkaListener(
        id = CONSUME_ID, 
        topics = KafkaTopicConstants.API_REPORT_TOPIC, 
        groupId = "${spring.kafka.consumer.group-id}", 
        containerFactory = "batchFactory"  // 使用批量消费工厂
    )
    public void consume(List<ConsumerRecord<?, String>> records, Acknowledgment ack) {
        try {
            records.forEach(item -> {
                LoggerUtil.info("接收到报告【" + item.key() + "】，加入到结果处理队列");
                
                // 创建任务并提交到线程池
                KafkaListenerTask task = new KafkaListenerTask();
                task.setApiExecutionQueueService(apiExecutionQueueService);
                task.setTestResultService(testResultService);
                task.setRecord(item);
                threadPool.execute(task);
            });
        } catch (Exception e) {
            LoggerUtil.error("KAFKA消费失败：", e);
        } finally {
            // 手动提交偏移量（确保所有消息都已提交到线程池）
            ack.acknowledge();
        }
    }
}
```


**关键点**:
1. **批量消费**: 使用 `List<ConsumerRecord>` 接收多条消息
2. **线程池处理**: 消费后立即提交到线程池，避免阻塞 Kafka 消费线程
3. **手动提交**: 使用 `Acknowledgment.acknowledge()` 手动提交偏移量
4. **异常处理**: try-finally 确保即使异常也会提交偏移量（避免重复消费）

---

### 5.3 消费者示例：调试结果实时推送

```java
@Configuration
public class MsKafkaListener {
    public static final String DEBUG_CONSUME_ID = "ms-api-debug-consume";
    
    // 单条消费调试结果（实时性要求高）
    @KafkaListener(
        id = DEBUG_CONSUME_ID, 
        topics = KafkaTopicConstants.DEBUG_TOPICS, 
        groupId = "${spring.kafka.consumer.debug.group-id}"
    )
    public void debugConsume(ConsumerRecord<?, String> record) {
        try {
            LoggerUtil.info("接收到执行结果：", record.key());
            
            // 检查是否有 WebSocket 连接在等待结果
            if (ObjectUtils.isNotEmpty(record.value()) && WebSocketUtil.has(record.key().toString())) {
                MsgDTO dto = JSONUtil.parseObject(record.value(), MsgDTO.class);
                
                // 解析执行结果
                if (StringUtils.isNotBlank(dto.getContent()) && dto.getContent().startsWith("result_")) {
                    String content = dto.getContent().substring(7);
                    RequestResult baseResult = JSONUtil.parseObject(content, RequestResult.class);
                    
                    // 解析误报库信息
                    RequestResultExpandDTO expandDTO = ResponseUtil.parseByRequestResult(baseResult);
                    dto.setContent("result_" + JSON.toJSONString(expandDTO));
                }
                
                // 通过 WebSocket 推送给前端
                WebSocketUtil.sendMessageSingle(dto);
            }
        } catch (Exception e) {
            LoggerUtil.error("KAFKA消费失败：", e);
        }
    }
}
```

**关键点**:
1. **单条消费**: 调试场景需要实时推送，不使用批量模式
2. **WebSocket 集成**: 消费后立即通过 WebSocket 推送给前端用户
3. **消息过滤**: 只处理有 WebSocket 连接的消息（避免无效处理）

---

### 5.4 消费者示例：项目创建监听

**位置**: `api-test/backend/src/main/java/io/metersphere/listener/ProjectCreatedListener.java`

```java
@Component
public class ProjectCreatedListener {
    public static final String CONSUME_ID = "project-created";
    
    @Resource
    private ApiModuleService apiModuleService;
    
    @KafkaListener(
        id = CONSUME_ID, 
        topics = KafkaTopicConstants.PROJECT_CREATED_TOPIC, 
        groupId = "${spring.application.name}"  // 使用服务名作为 groupId
    )
    public void consume(ConsumerRecord<?, String> record) {
        String projectId = record.value();
        LogUtil.info("API-TEST 服务收到项目创建消息: " + projectId);
        
        // 初始化项目的 API 模块默认节点
        apiModuleService.initDefaultNode(projectId);
    }
}
```

**关键点**:
1. **服务级 groupId**: 使用 `${spring.application.name}` 确保每个服务只消费一次
2. **简单消息**: 只传递项目 ID，减少消息大小
3. **幂等性**: 初始化逻辑需要支持重复执行（Kafka 可能重复投递）


---

## 六、JMeter 与 Kafka 集成

### 6.1 JMeter Backend Listener 配置

MeterSphere 在性能测试中使用 JMeter 的 Backend Listener 将测试结果发送到 Kafka。

**位置**: `performance-test/backend/src/main/java/io/metersphere/parse/xml/reader/JmeterDocumentParser.java`

```java
private void processBackendListener(Element backendListener) {
    KafkaProperties kafkaProperties = CommonBeanFactory.getBean(KafkaProperties.class);
    
    // 配置 Kafka Backend Client
    appendStringProp(backendListener, "classname", 
        "io.github.rahulsinghai.jmeter.backendlistener.kafka.KafkaBackendClient");
    
    // Kafka 连接配置
    appendKafkaProp(collectionProp, "kafka.bootstrap.servers", kafkaProperties.getBootstrapServers());
    appendKafkaProp(collectionProp, "kafka.topic", kafkaProperties.getTopic());
    appendKafkaProp(collectionProp, "kafka.acks", kafkaProperties.getAcks());
    
    // 性能优化配置
    appendKafkaProp(collectionProp, "kafka.batch.size", kafkaProperties.getBatchSize());
    appendKafkaProp(collectionProp, "kafka.compression.type", kafkaProperties.getCompressionType());
    
    // 测试上下文信息
    appendKafkaProp(collectionProp, "test.id", context.getTestId());
    appendKafkaProp(collectionProp, "test.name", context.getTestName());
    appendKafkaProp(collectionProp, "test.reportId", context.getReportId());
}
```

### 6.2 性能测试结果消费

**位置**: `performance-test/backend/src/main/java/io/metersphere/consumer/LoadTestConsumer.java`

```java
@Service
public class LoadTestConsumer {
    public static final String CONSUME_ID = "load-test-data";
    
    @KafkaListener(
        id = CONSUME_ID, 
        topics = "${kafka.test.topic}",  // 从配置文件读取主题名
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<?, String> record) {
        // 解析性能测试报告
        LoadTestReport loadTestReport = JSON.parseObject(record.value(), LoadTestReport.class);
        
        // 获取所有实现了 LoadTestFinishEvent 接口的 Bean
        Map<String, LoadTestFinishEvent> subTypes = 
            CommonBeanFactory.getBeansOfType(LoadTestFinishEvent.class);
        
        // 依次执行所有处理器（策略模式）
        subTypes.forEach((k, t) -> {
            try {
                t.execute(loadTestReport);
            } catch (Exception e) {
                LogUtil.error(e);
            }
        });
    }
}
```

**关键点**:
1. **动态主题**: 主题名从配置文件读取，支持不同环境
2. **策略模式**: 使用 Spring 的 Bean 查找机制，支持扩展处理器
3. **异常隔离**: 单个处理器异常不影响其他处理器

---

## 七、Kafka 最佳实践

### 7.1 消息设计原则


| 原则 | 说明 | 示例 |
|------|------|------|
| **消息轻量化** | 只传递必要信息，大数据存储到数据库/MinIO | 项目删除只传 ID，不传整个对象 |
| **JSON 序列化** | 统一使用 JSON 格式，便于调试和跨语言 | `JSON.toJSONString(object)` |
| **消息 Key 设计** | 使用业务 ID 作为 key，支持分区和去重 | 报告 ID、项目 ID |
| **幂等性设计** | 消费逻辑支持重复执行 | 初始化节点前先检查是否存在 |

### 7.2 消费者设计原则

| 原则 | 说明 | 实现方式 |
|------|------|----------|
| **快速消费** | 消费逻辑尽量简单，耗时操作提交到线程池 | `MsKafkaListener` 使用线程池 |
| **手动提交** | 使用手动提交确保消息处理完成 | `ack.acknowledge()` |
| **异常处理** | 捕获异常，避免消费线程崩溃 | try-catch + 日志记录 |
| **监控日志** | 记录消费关键信息，便于排查问题 | `LogUtil.info("接收到报告...")` |

### 7.3 生产者设计原则

| 原则 | 说明 | 配置 |
|------|------|------|
| **异步发送** | 使用 `send()` 异步发送，不阻塞主流程 | `kafkaTemplate.send(topic, message)` |
| **acks=1** | 平衡性能和可靠性，leader 确认即可 | `kafka.acks=1` |
| **批量发送** | 配置合理的 batch.size，提高吞吐量 | `kafka.batch.size` |
| **压缩** | 使用压缩减少网络传输 | `kafka.compression.type` |

### 7.4 分布式环境注意事项

#### 7.4.1 GroupId 设计

```java
// 场景 1: 服务级消费（每个服务只消费一次）
@KafkaListener(
    topics = KafkaTopicConstants.PROJECT_CREATED_TOPIC,
    groupId = "${spring.application.name}"  // api-test, test-track 等
)

// 场景 2: 实例级消费（每个实例都消费）
@KafkaListener(
    topics = KafkaTopicConstants.PLATFORM_PLUGIN,
    groupId = "plugin-listener_" + "${random.uuid}"  // 每个实例唯一
)
```

#### 7.4.2 消息顺序性

- **单分区保证顺序**: 使用相同的 key 发送到同一分区
- **跨分区无序**: 不同分区的消息无法保证顺序
- **业务设计**: 避免依赖消息顺序，使用版本号或时间戳

---

## 八、Kafka 可扩展应用场景

### 8.1 当前未使用但可以引入的场景

| 场景 | 说明 | 优势 |
|------|------|------|
| **审计日志异步写入** | 用户操作日志通过 Kafka 异步写入 | 不影响主流程性能 |
| **数据变更 CDC** | 数据库变更事件发送到 Kafka | 支持数据同步、缓存更新 |
| **定时任务调度** | 使用 Kafka 触发定时任务 | 分布式任务调度 |
| **实时数据分析** | 测试数据流式处理 | 实时统计、监控告警 |
| **消息重试队列** | 失败消息发送到重试主题 | 提高系统容错性 |

### 8.2 高级搜索场景（潜在应用）

基于你当前开发的高级搜索功能，可以考虑：


```java
// 场景：搜索索引异步更新
@Service
public class SearchIndexService {
    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;
    
    // 测试用例更新时发送索引更新事件
    public void updateTestCase(TestCase testCase) {
        // ... 更新数据库 ...
        
        // 异步更新搜索索引
        IndexUpdateEvent event = new IndexUpdateEvent();
        event.setEntityType("test_case");
        event.setEntityId(testCase.getId());
        event.setOperation("UPDATE");
        
        kafkaTemplate.send("SEARCH_INDEX_UPDATE", JSON.toJSONString(event));
    }
}

// 消费者：更新 Elasticsearch 或其他搜索引擎
@Component
public class SearchIndexListener {
    @KafkaListener(topics = "SEARCH_INDEX_UPDATE", groupId = "search-index-updater")
    public void consume(ConsumerRecord<?, String> record) {
        IndexUpdateEvent event = JSON.parseObject(record.value(), IndexUpdateEvent.class);
        
        // 更新搜索索引（Elasticsearch、Solr 等）
        searchEngine.updateIndex(event);
    }
}
```

---

## 九、Kafka 学习路径

### 9.1 基础概念（必须掌握）

1. **核心组件**
   - Broker: Kafka 服务器节点
   - Topic: 消息主题（逻辑分类）
   - Partition: 分区（物理存储单元）
   - Producer: 生产者（发送消息）
   - Consumer: 消费者（接收消息）
   - Consumer Group: 消费者组（负载均衡）

2. **消息模型**
   - 发布-订阅模式
   - 点对点模式（通过 Consumer Group 实现）

3. **偏移量（Offset）**
   - 消息在分区中的唯一标识
   - 消费者通过偏移量追踪消费进度
   - 自动提交 vs 手动提交

### 9.2 进阶概念（深入理解）

1. **分区策略**
   - 轮询分区（默认）
   - Key Hash 分区（相同 key 到同一分区）
   - 自定义分区器

2. **消费者 Rebalance**
   - 触发条件：消费者加入/退出、分区变化
   - 影响：短暂停止消费
   - 优化：合理设置心跳和超时参数

3. **消息可靠性**
   - acks 参数：0（不确认）、1（leader 确认）、all（所有副本确认）
   - 副本机制：ISR（In-Sync Replicas）
   - 幂等性生产者：避免重复消息

4. **性能优化**
   - 批量发送：batch.size、linger.ms
   - 压缩：gzip、snappy、lz4
   - 批量消费：max.poll.records

### 9.3 实战练习（基于 MeterSphere）

#### 练习 1: 实现用户操作审计日志

**目标**: 用户的增删改操作通过 Kafka 异步记录到审计日志表


**步骤**:
1. 在 `KafkaTopicConstants` 添加主题：`String AUDIT_LOG_TOPIC = "AUDIT_LOG";`
2. 创建生产者：在 Service 层操作后发送审计事件
3. 创建消费者：监听主题并写入审计日志表
4. 测试：执行用户操作，验证审计日志是否正确记录

#### 练习 2: 实现测试用例批量导入进度推送

**目标**: Excel 导入测试用例时，通过 Kafka + WebSocket 实时推送进度

**步骤**:
1. 导入服务发送进度消息到 Kafka
2. 消费者接收消息并通过 WebSocket 推送给前端
3. 前端显示进度条

#### 练习 3: 实现分布式缓存失效通知

**目标**: 数据更新后通知所有服务实例清除缓存

**步骤**:
1. 定义缓存失效主题：`CACHE_INVALIDATE`
2. 数据更新后发送缓存 key 到 Kafka
3. 每个服务实例监听主题并清除本地缓存
4. 使用唯一 groupId 确保每个实例都收到消息

---

## 十、常见问题与排查

### 10.1 消息丢失

**原因**:
- acks=0，生产者不等待确认
- 消费者自动提交，处理失败但偏移量已提交
- Broker 宕机，副本未同步

**解决**:
- 设置 acks=1 或 acks=all
- 使用手动提交偏移量
- 配置副本数 >= 2

### 10.2 消息重复

**原因**:
- 网络抖动导致重试
- 消费者处理成功但提交偏移量失败
- Rebalance 导致重复消费

**解决**:
- 生产者开启幂等性：`enable.idempotence=true`
- 消费者实现幂等性逻辑（数据库唯一索引、Redis 去重）
- 使用事务消息（Kafka 0.11+）

### 10.3 消费延迟

**原因**:
- 消费速度慢于生产速度
- 消费逻辑耗时过长
- 分区数不足，无法并行消费

**解决**:
- 增加消费者实例（不超过分区数）
- 优化消费逻辑，耗时操作异步处理
- 增加分区数（需要重新分配）
- 使用批量消费提高吞吐量

### 10.4 Rebalance 频繁

**原因**:
- 消费者处理时间超过 `max.poll.interval.ms`
- 心跳超时

**解决**:
- 增大 `max.poll.interval.ms`（MeterSphere 设置为 15 分钟）
- 减少 `max.poll.records`，降低单次处理时间
- 优化消费逻辑，避免长时间阻塞

### 10.5 排查工具

```bash
# 查看主题列表
 

# 查看主题详情
kafka-topics.sh --describe --topic ms-api-exec-topic --bootstrap-server localhost:9092

# 查看消费者组状态
kafka-consumer-groups.sh --describe --group api-test --bootstrap-server localhost:9092

# 查看消费延迟（Lag）
kafka-consumer-groups.sh --describe --group api-test --bootstrap-server localhost:9092 | grep LAG

# 消费指定主题（测试）
kafka-console-consumer.sh --topic ms-api-exec-topic --from-beginning --bootstrap-server localhost:9092
```


---

## 十一、MeterSphere Kafka 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Kafka Cluster (3.6.1)                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │  Broker 1        │  │  Broker 2        │  │  Broker 3        │  │
│  │  - Partition 0   │  │  - Partition 1   │  │  - Partition 2   │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                  ▲
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
        │ 生产者                   │                         │ 消费者
        │                         │                         │
┌───────▼────────┐       ┌────────▼────────┐       ┌───────▼────────┐
│ system-setting │       │  JMeter Engine  │       │   api-test     │
│                │       │                 │       │                │
│ - 项目创建/删除 │       │ - Backend       │       │ - MsKafka      │
│ - 插件上传/删除 │       │   Listener      │       │   Listener     │
│                │       │ - 性能测试结果   │       │ - 批量消费     │
└────────────────┘       └─────────────────┘       │ - 线程池处理   │
                                                    └────────────────┘
┌────────────────┐                                 ┌────────────────┐
│  test-track    │                                 │ performance-   │
│                │                                 │ test           │
│ - 测试计划删除  │                                 │                │
│ - 项目初始化    │                                 │ - LoadTest     │
│                │                                 │   Consumer     │
└────────────────┘                                 │ - 报告清理     │
                                                    └────────────────┘

主题（Topics）:
┌─────────────────────────────────────────────────────────────────┐
│ PROJECT_CREATED          → api-test, test-track, performance    │
│ PROJECT_DELETED          → api-test, test-track, performance    │
│ TEST_PLAN_DELETED        → api-test, test-track                 │
│ API_REPORT_TOPIC         → api-test (批量消费)                  │
│ DEBUG_TOPICS             → api-test (实时推送)                  │
│ TEST_PLAN_REPORT_TOPIC   → test-track                           │
│ PLATFORM_PLUGIN          → 所有服务实例（唯一 groupId）          │
│ CLEAN_UP_REPORT_SCHEDULE → api-test, performance               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 十二、总结与建议

### 12.1 MeterSphere Kafka 使用特点

1. **事件驱动架构**: 微服务通过 Kafka 解耦，支持独立部署和扩展
2. **异步处理**: 测试执行、报告生成等耗时操作异步化，提升响应速度
3. **分布式协调**: 插件热加载、缓存失效等场景通过 Kafka 实现分布式通知
4. **高吞吐量**: 批量消费 + 线程池处理，支持高并发测试场景

### 12.2 学习建议

1. **从源码学习**: 阅读 `MsKafkaListener`、`KafkaConfig` 等核心类
2. **动手实践**: 基于现有代码实现新的 Kafka 应用场景
3. **监控调优**: 使用 Kafka 管理工具监控消费延迟、分区分布
4. **异常处理**: 重点关注消息丢失、重复、延迟等问题的处理

### 12.3 扩展阅读

- [Kafka 官方文档](https://kafka.apache.org/documentation/)
- [Spring Kafka 官方文档](https://spring.io/projects/spring-kafka)
- [Kafka 权威指南](https://www.confluent.io/resources/kafka-the-definitive-guide/)

---

**文档版本**: v1.0  
**更新日期**: 2026-01-19  
**适用版本**: MeterSphere v2.10-lts  
**作者**: Kiro AI Assistant
