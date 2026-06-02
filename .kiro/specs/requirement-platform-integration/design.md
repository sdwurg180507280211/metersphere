# 设计文档：全流程平台对接

## 概述

本文档对需求平台与MeterSphere测试平台对接功能的详细设计进行全面而准确的描述，为开发人员在实现软件功能时提供指导和参考。详细的设计规范和流程将有助于保证软件的稳定性、可维护性和可扩展性。

实现需求平台与 MeterSphere 测试平台的双向数据对接，通过 RocketMQ 消息队列实现异步通信。核心流程包括：需求同步入池、需求池管理、测试计划创建、状态回传。

**文档版本**：V1.2
**修订日期**：2026年4月16日

## 核心设计目标

1. **可靠性**: 消息幂等处理，防止重复消费导致数据污染
2. **一致性**: 保证需求与测试计划一对一关联，状态正确流转
3. **可追溯**: 全链路 traceId 追踪，完整审计日志
4. **可扩展**: 预留配置化扩展点，支持后续版本增强

**功能目标**：
1. 实现需求平台到MeterSphere的需求数据同步（通过RocketMQ）
2. 实现MeterSphere需求池功能，支持需求展示、筛选、查询
3. 实现从需求池创建测试计划的流程，一个需求对应一个测试计划
4. 实现测试计划状态和报告链接回传到需求平台
5. 实现幂等处理、异常处理、历史数据初始化

**性能目标**：
- 消息消费延迟 < 5秒
- 需求池列表查询响应时间 < 2秒
- 支持并发创建测试计划，保证数据一致性

**安全目标**：
- 消息传输安全（RocketMQ认证）
- 接口访问权限控制
- 数据审计和追溯

---

## 参考文档

- 《需求平台与测试平台需求对接方案》（V3.0）
- 《开发排期（截至2026-06-30）》
- 《需求平台字段确认清单2》
- MeterSphere现有架构文档

---

## 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 消息队列 | RocketMQ 4.9.2 | 平台间异步通信 |
| 后端框架 | Spring Boot | 业务逻辑实现 |
| 数据持久化 | MyBatis + MySQL | 数据存储 |
| 前端框架 | Vue 2 | 需求池页面 |

---

## 架构设计

### 总体架构

```
┌─────────────────┐         RocketMQ          ┌──────────────────┐
│   需求平台      │ ──────────────────────────>│  MeterSphere     │
│                 │  topic-requirement-to-metersphere │            │
│                 │                            │  ┌─────────────┐ │
│                 │                            │  │ 需求池模块  │ │
│                 │                            │  └─────────────┘ │
│                 │                            │  ┌─────────────┐ │
│                 │  topic-metersphere-to-requirement │ │ 测试计划模块│ │
│                 │ <──────────────────────────│  └─────────────┘ │
└─────────────────┘         RocketMQ          └──────────────────┘
```

**核心流程**：
1. 需求平台通过RocketMQ发送需求消息
2. MeterSphere消费消息，写入需求池
3. 测试人员可在需求池手工创建需求（补录入口）
4. 测试人员在需求池中点击创建测试计划（需求池入口）
5. 测试计划状态变化时，通过RocketMQ回传给需求平台

**关键入口**：
1. **需求池补录入口**：从需求池列表页左上角点击"创建需求"按钮，手工补录需求数据，创建后默认状态为 `PENDING`
2. **需求池建计划入口**：从需求池列表行点击"创建测试计划"按钮，自动关联需求，计划名称自动填充且只读
3. **测试计划入口**：在测试计划模块点击"新建测试计划"按钮，不关联需求，计划名称由用户输入可编辑

### 模块设计

#### 需求同步模块
**职责**：
- 消费RocketMQ消息
- 解析需求数据
- 幂等处理
- 写入需求池

**核心类**：
- `RequirementSyncConsumer`：消息消费者
- `RequirementSyncService`：同步业务逻辑
- `RequirementPoolMapper`：数据访问层

#### 需求池模块
**职责**：
- 需求池数据管理
- 支持手工创建需求
- 需求列表查询、筛选、分页
- 需求详情展示
- 状态管理

**核心类**：
- `RequirementPoolController`：接口控制器
- `RequirementPoolService`：业务逻辑层
- `RequirementPoolMapper`：数据访问层

#### 测试计划创建模块
**职责**：
- 从需求池创建测试计划
- 需求编号绑定
- 状态联动更新
- 并发控制

**核心类**：
- `RequirementPoolController`：接口控制器（需求池入口）
- `RequirementPoolService`：业务逻辑层（需求池入口）
- `TestPlanService`：业务逻辑层（测试计划落库）
- `RequirementPlanRelationService`：关联关系管理

**说明**：
- 实际落地的包路径：
  - 后端：`io.metersphere.requirement.pool.*`（controller/service/request）、`io.metersphere.base.domain.*`（实体）、`io.metersphere.base.mapper.ext.*`（Mapper）
  - 前端：`test-track/frontend/src/business/requirement-pool/*`

#### 状态回传模块
**职责**：
- 监听测试计划状态变化
- 生成回传消息
- 发送到RocketMQ
- 失败记录和重试

**核心类**：
- `RequirementCallbackProducer`：消息生产者
- `RequirementCallbackService`：回传业务逻辑
- `CallbackRecordMapper`：回传记录数据访问

---

## 数据模型

### 需求池主表（requirement_pool）

| 字段名 | 类型 | 长度 | 是否必填 | 说明 |
|--------|------|------|----------|------|
| id | VARCHAR | 32 | 是 | 主键 |
| dmp_num | VARCHAR | 64 | 是 | 需求编号（唯一索引） |
| requirement_name | VARCHAR | 255 | 是 | 需求名称 |
| pool_status | VARCHAR | 32 | 是 | 需求池状态：PENDING/CREATED/CANCELLED |
| parent_wfinst_code | VARCHAR | 100 | 否 | 主流程编码 |
| act_name | VARCHAR | 100 | 否 | 当前环节 |
| operation_type | VARCHAR | 32 | 否 | 操作类型：CREATED/UPDATED/CANCELLED |
| system_name | VARCHAR | 255 | 否 | 所属系统 |
| req_manager_name | VARCHAR | 64 | 否 | 需求负责人 |
| assignee_name | VARCHAR | 64 | 否 | 当前处理人 |
| created_dept | VARCHAR | 255 | 否 | 需求申请部门 |
| create_user1 | VARCHAR | 64 | 否 | 需求申请人 |
| dept_name | VARCHAR | 255 | 否 | 需求负责人处室 |
| start_user_name | VARCHAR | 64 | 否 | 创建人 |
| req_father_class | VARCHAR | 255 | 否 | 需求大类 |
| req_son_class | VARCHAR | 255 | 否 | 需求子类 |
| create_time | BIGINT | - | 否 | 需求提出时间（时间戳） |
| up_time | BIGINT | - | 否 | 预计上线时间（时间戳） |
| linked_plan_id | VARCHAR | 50 | 否 | 关联的测试计划ID |
| linked_plan_name | VARCHAR | 255 | 否 | 关联的测试计划名称 |
| test_status | VARCHAR | 32 | 否 | 回传的测试状态（模拟需求平台展示） |
| plan_share_url | VARCHAR | 500 | 否 | 回传的报告链接（模拟需求平台展示） |
| last_callback_time | BIGINT | - | 否 | 最后回传时间（时间戳） |
| last_sync_time | BIGINT | - | 是 | 最后同步时间（时间戳） |
| event_time | BIGINT | - | 否 | 消息事件时间（时间戳） |
| trace_id | VARCHAR | 64 | 否 | 追踪ID |
| created_at | BIGINT | - | 是 | 创建时间（时间戳） |
| updated_at | BIGINT | - | 是 | 更新时间（时间戳） |

**索引设计**:
- PRIMARY KEY: `id`
- UNIQUE INDEX: `uk_dmp_num` ON `dmp_num`
- INDEX: `idx_pool_status` ON `pool_status`
- INDEX: `idx_system_name` ON `system_name`
- INDEX: `idx_create_time` ON `create_time`

**实现备注**:
- 字段 `create_user1`：需求平台字段名为 `createUser1`，数据库列名为 `create_user1`（注意不是 `create_user`，避免与系统内置字段冲突）
- 字段 `created_dept`：需求平台字段名为 `createdept`（无下划线），Mapper XML 映射为 `createdDept`
- 字段 `operation_type`：用于区分消息操作类型（CREATED/UPDATED/CANCELLED），与 `actName`（当前环节）是不同概念
- 当前仓库已落地的 `V19__create_requirement_pool_table.sql` 为完整版本，包含所有字段和测试数据

### 测试计划表扩展（test_plan）

**新增字段**:

| 字段名 | 类型 | 长度 | 是否必填 | 说明 |
|--------|------|------|----------|------|
| requirement_number | VARCHAR | 100 | 否 | 需求编号（唯一索引）。从需求池创建时为需求的dmpNum，直接创建时为null |

**新增索引**:
- UNIQUE INDEX: `uk_requirement_number` ON `requirement_number`
- 说明：MySQL唯一索引允许多个null值，因此直接创建的测试计划可以都设置为null

**状态枚举扩展**:
- 在现有状态枚举中新增：`CANCELLED`（已取消）

**测试计划来源区分**：
- `requirement_number != null`：从需求池创建，关联需求
- `requirement_number = null`：直接创建，不关联需求

---

## API 设计

### 1. 需求池列表查询（支持高级搜索）

**路径**: `POST /requirement-pool/list/{goPage}/{pageSize}`

**请求参数**（page.condition 格式）:
```json
{
  "filters": {
    "poolStatus": ["PENDING", "CREATED"],
    "systemName": ["系统A", "系统B"],
    "reqFatherClass": ["功能需求"],
    "actName": ["测试待处理"],
    "assigneeName": ["张三"],
    "createdDept": ["产品部"],
    "operationType": ["CREATED"]
  },
  "combine": {
    "dmpNum": { "operator": "like", "value": "REQ-2024" },
    "requirementName": { "operator": "like", "value": "需求" },
    "poolStatus": { "operator": "in", "value": ["PENDING", "CREATED"] },
    "systemName": { "operator": "in", "value": ["系统A"] },
    "reqFatherClass": { "operator": "in", "value": ["功能需求"] },
    "reqSonClass": { "operator": "in", "value": ["新增功能"] },
    "parentWfinstCode": { "operator": "like", "value": "WF-" },
    "actName": { "operator": "like", "value": "测试" },
    "operationType": { "operator": "like", "value": "CREATED" },
    "assigneeName": { "operator": "like", "value": "张" },
    "createdDept": { "operator": "like", "value": "产品" },
    "createUser1": { "operator": "like", "value": "王" },
    "deptName": { "operator": "like", "value": "一部" },
    "startUserName": { "operator": "like", "value": "赵" },
    "reqManagerName": { "operator": "like", "value": "李" },
    "createTime": {
      "operator": "between",
      "value": [1704067200000, 1735689599000]
    },
    "upTime": {
      "operator": "between",
      "value": [1704067200000, 1735689599000]
    }
  },
  "orders": [
    { "name": "create_time", "type": "desc" }
  ],
  "components": []
}
```

**默认排序规则**:
- 当前端未指定排序时，后端默认按 `create_time DESC` 排序
- 最新同步过来的需求最先显示

**响应结果**（标准分页格式）:
```json
{
  "itemCount": 100,
  "listObject": [
    {
      "id": "xxx",
      "dmpNum": "REQ-2024-001",
      "requirementName": "需求名称",
      "poolStatus": "PENDING",
      "actName": "测试待处理",
      "parentWfinstCode": "WF-001",
      "operationType": "CREATED",
      "systemName": "系统名称",
      "reqManagerName": "张三",
      "assigneeName": "李四",
      "createdDept": "产品部",
      "createUser1": "王五",
      "deptName": "产品一部",
      "startUserName": "赵六",
      "reqFatherClass": "功能需求",
      "reqSonClass": "新增功能",
      "createTime": 1234567890000,
      "upTime": 1234567890000,
      "linkedPlanId": null,
      "linkedPlanName": null
    }
  ]
}
```

### 2. 创建需求

**接口路径**: `POST /requirement-pool/add`

**请求参数**:
```json
{
  "dmpNum": "REQ-2024-001",
  "requirementName": "需求名称",
  "systemName": "未指定",
  "reqManagerName": "待分配",
  "reqFatherClass": "功能需求",
  "reqSonClass": "新增功能",
  "actName": "测试待处理",
  "parentWfinstCode": "-",
  "operationType": "CREATED",
  "assigneeName": "待分配",
  "createdDept": "-",
  "createUser1": "-",
  "deptName": "-",
  "startUserName": "-"
}
```

**说明**:
- `dmpNum`、`requirementName` 必填
- 其余字段有默认值（创建弹窗中预填充）
- 创建成功后默认写入 `PENDING` 状态
- 该接口用于需求池手工补录，不替代 RocketMQ 同步主链路

**异常情况**:
- 需求编号为空：`message: "需求编号不能为空"`
- 需求名称为空：`message: "需求名称不能为空"`
- 需求编号重复：`message: "需求编号已存在"`


### 3. 从需求池创建测试计划

**接口路径**: `POST /requirement-pool/create-test-plan`

**请求参数**:
```json
{
  "dmpNum": "REQ-2024-001",
  "projectId": "当前项目ID",
  "principalId": "负责人ID",
  "plannedStartTime": 1234567890000,
  "plannedEndTime": 1234567890000,
  "description": "描述"
}
```

**说明**:
- 不需要传入 `name` 字段，后端自动使用需求名称
- 计划名称不可编辑，与需求名称保持一致

**响应结果**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "plan-001",
    "name": "需求名称",
    "requirementNumber": "REQ-2024-001",
    "status": "PENDING"
  }
}
```

**异常情况**:
- 需求不存在：`code: 404, message: "需求不存在"`
- 需求已创建测试计划：`code: 400, message: "该需求已创建测试计划"`
- 需求已取消：`code: 400, message: "该需求已取消，无法创建测试计划"`

### 4. 直接创建测试计划

**接口路径**: `POST /test/plan/add`

**请求参数**:
```json
{
  "name": "测试计划名称",
  "projectId": "当前项目ID",
  "principalId": "负责人ID",
  "plannedStartTime": 1234567890000,
  "plannedEndTime": 1234567890000,
  "description": "描述"
}
```

**说明**:
- `requirementNumber` 字段自动设置为 null
- 计划名称由用户输入，可编辑

**响应结果**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "plan-002",
    "name": "测试计划名称",
    "requirementNumber": null,
    "status": "PENDING"
  }
}
```

---

## RocketMQ 消息格式

### 需求同步消息（需求平台 → MeterSphere）

**Topic**: `topic-requirement-to-metersphere`
**Producer ID**: `producer-requirement-to-metersphere`
**Consumer ID**: `consumer-requirement-to-metersphere`

**消息体**:
```json
{
  "dmpNum": "REQ-2024-001",
  "name1": "需求名称",
  "operationType": "CREATED",
  "reqManagerName": "张三",
  "actName": "开发中",
  "createTime": 1710234567000,
  "parentWfinstCode": "WF-001",
  "reqFatherClass": "功能需求",
  "reqSonClass": "新增功能",
  "systemName": "系统名称",
  "upTime": 1714567890000,
  "assigneeName": "李四",
  "createdept": "产品部",
  "createUser1": "王五",
  "deptName": "产品一部",
  "startUserName": "赵六",
  "eventTime": 1710234567000,
  "traceId": "trace-001"
}
```

**operationType 枚举**:
- `CREATED`: 新建需求
- `UPDATED`: 更新需求
- `CANCELLED`: 取消需求

**字段映射说明**:
- `name1`（需求平台） → `requirementName`（MeterSphere 需求池）
- `createdept`（需求平台，无下划线） → `createdDept`（MeterSphere 需求池）
- `createUser1`（需求平台，注意后缀1） → `createUser1`（MeterSphere 需求池，数据库列名 `create_user1`）

### 状态回传消息（MeterSphere → 需求平台）

**Topic**: `topic-metersphere-to-requirement`
**Producer ID**: `producer-metersphere-to-requirement`
**Consumer ID**: `consumer-metersphere-to-requirement`

**消息体**:
```json
{
  "dmpNum": "REQ-2024-001",
  "planStatus": "COMPLETED",
  "plannedStartTime": 1234567890000,
  "plannedEndTime": 1234567890000,
  "actualStartTime": 1234567890000,
  "actualEndTime": 1234567890000,
  "principalUsers": "张三",
  "planShareUrl": "/#/track/testPlan/reportList?resourceId={reportId}",
  "syncTime": 1234567890000,
  "traceId": "trace-002"
}
```

**字段说明**:
- `dmpNum`：需求编号（关联主键）
- `planStatus`：测试计划状态（需求平台最关心的字段）
- `plannedStartTime`：计划开始时间
- `plannedEndTime`：计划结束时间
- `actualStartTime`：实际开始时间
- `actualEndTime`：实际结束时间
- `principalUsers`：负责人
- `planShareUrl`：测试计划报告链接，格式: `/#/track/testPlan/reportList?resourceId={reportId}`（V1 使用内部 DB 报告链接，回传前若无报告则自动生成）

---

## 核心类设计

### 实体类

```java
// RequirementPool.java — 实际包路径: io.metersphere.base.domain
@Data
public class RequirementPool {
    private String id;
    private String dmpNum;              // 需求编号（唯一）
    private String requirementName;     // 需求名称
    private String poolStatus;          // PENDING/CREATED/CANCELLED
    private String parentWfinstCode;    // 主流程编码
    private String actName;             // 当前环节
    private String operationType;       // 操作类型：CREATED/UPDATED/CANCELLED
    private String systemName;          // 所属系统
    private String reqManagerName;      // 需求负责人
    private String assigneeName;        // 当前处理人
    private String createdDept;         // 需求申请部门
    private String createUser1;         // 需求申请人（注意：字段名为createUser1，非createUser）
    private String deptName;            // 需求负责人处室
    private String startUserName;       // 创建人
    private String reqFatherClass;      // 需求大类
    private String reqSonClass;         // 需求子类
    private Long createTime;            // 需求提出时间
    private Long upTime;                // 预计上线时间
    private String linkedPlanId;        // 关联的测试计划ID
    private String linkedPlanName;      // 关联的测试计划名称
    private Long lastSyncTime;          // 最后同步时间
    private Long eventTime;             // 消息事件时间
    private String traceId;             // 追踪ID
    private Long createdAt;             // 创建时间
    private Long updatedAt;             // 更新时间
}
```

### DTO

```java
// RequirementSyncMessage.java
@Data
public class RequirementSyncMessage {
    private String dmpNum;
    private String name1;               // 需求名称
    private String operationType;       // CREATED/UPDATED/CANCELLED
    private String reqManagerName;      // 需求负责人
    private String actName;             // 当前环节
    private Long createTime;            // 需求提出时间
    private String parentWfinstCode;    // 主流程编码
    private String reqFatherClass;      // 需求大类
    private String reqSonClass;         // 需求子类
    private String systemName;          // 所属系统
    private Long upTime;                // 预计上线时间
    private String assigneeName;        // 当前处理人
    private String createdept;          // 需求申请部门（注意：需求平台字段名无下划线）
    private String createUser1;         // 需求申请人（注意：后缀为1）
    private String deptName;            // 需求负责人处室
    private String startUserName;       // 创建人
    private Long eventTime;             // 消息事件时间
    private String traceId;             // 追踪ID
}

// RequirementCallbackMessage.java — 实际包路径: io.metersphere.requirement.pool.dto
@Data
public class RequirementCallbackMessage {
    private String dmpNum;              // 需求编号（关联主键）
    private String planStatus;           // 测试计划状态（V1 仅回传 Completed）
    private Long plannedStartTime;       // 计划开始时间
    private Long plannedEndTime;         // 计划结束时间
    private Long actualStartTime;        // 实际开始时间
    private Long actualEndTime;          // 实际结束时间
    private String principalUsers;       // 负责人（多人逗号分隔）
    private String planShareUrl;         // 报告链接: /#/track/testPlan/reportList?resourceId={reportId}
    private Long syncTime;               // 同步时间
    private String traceId;              // 追踪ID
}

// QueryRequirementPoolRequest.java — 实际包路径: io.metersphere.requirement.pool.request
@Data
public class QueryRequirementPoolRequest extends RequirementPool {
    private List<OrderRequest> orders;    // 排序指令
    private Map<String, List<String>> filters;  // 列头快速筛选
    private Map<String, Object> combine;  // 高级搜索条件
    private String projectId;
    private String workspaceId;
    private String name;                  // 快速搜索（按需求名称）
}
```

### 核心 Service 接口

```java
// RequirementSyncService.java
public interface RequirementSyncService {
    // 处理需求同步消息
    void handleSyncMessage(RequirementSyncMessage message);

    // 幂等检查
    boolean isDuplicate(String dmpNum, Long eventTime);
}

// RequirementPoolService.java — 实际包路径: io.metersphere.requirement.pool.service
public interface RequirementPoolService {
    // 分页查询需求池
    List<RequirementPool> listRequirements(QueryRequirementPoolRequest request);

    // 获取需求详情
    RequirementPool getByDmpNum(String dmpNum);

    // 更新需求池状态
    void updateStatus(String dmpNum, String status, String linkedPlanId);
}

// TestPlanService.java（扩展）
public interface TestPlanService {
    // 从需求创建测试计划（扩展方法）
    TestPlan createFromRequirement(CreateTestPlanFromRequirementRequest request);

    // 直接创建测试计划
    TestPlan create(CreateTestPlanRequest request);
}
```

---

## 界面设计

### 需求池列表页

**页面路径**: `/track/requirement-pool/list`

**页面布局**:
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 需求池                                                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ [创建需求]                                           [ms-table-header 搜索]│
├─────────────────────────────────────────────────────────────────────────────┤
│ 需求编号│需求名称│需求池状态│当前环节│主流程编码│操作类型│所属系统│需求负责人││
│────────┼───────┼─────────┼───────┼─────────┼───────┼───────┼─────────│
│ 当前处理人│需求大类│需求子类│需求申请部门│需求申请人│需求负责人处室│创建人│操作│
│─────────┼───────┼───────┼──────────┼──────────┼────────────┼─────┼────│
│ 预计上线时间│需求提出时间│                                                  │
│───────────┼───────────┤                                                  │
│ REQ-001 │ 需求A │ 未创建  │测试待处理│ WF-001  │CREATED│ 系统A │ 张三   │
│ ...     │       │         │        │         │       │       │        │
└─────────────────────────────────────────────────────────────────────────────┘
```

**页面行为**:
- 左上角主按钮为"创建需求"
- 行内操作按钮为"创建测试计划"
- 两者分别对应需求池 CRUD 新增和需求关联建计划两个不同动作
- 列表支持17个字段列，用户可通过表头设置自定义显示列

**列表字段完整清单（17项，与字段确认清单对齐）**:

| 序号 | 字段ID | 列标签 | 类型 | 可排序 | 可筛选 |
|------|--------|--------|------|--------|--------|
| 1 | dmpNum | 需求编号 | 文本 | 是 | 高级搜索 |
| 2 | requirementName | 需求名称 | 文本 | 是 | 高级搜索 |
| 3 | poolStatus | 需求池状态 | Tag+筛选 | - | 列头筛选+高级搜索 |
| 4 | actName | 当前环节 | 文本 | - | 列头筛选+高级搜索 |
| 5 | parentWfinstCode | 主流程编码 | 文本 | - | 高级搜索 |
| 6 | operationType | 操作类型 | 文本 | - | 列头筛选+高级搜索 |
| 7 | systemName | 所属系统 | 文本 | 是 | 列头筛选+高级搜索 |
| 8 | reqManagerName | 需求负责人 | 文本 | - | 高级搜索 |
| 9 | assigneeName | 当前处理人 | 文本 | - | 列头筛选+高级搜索 |
| 10 | reqFatherClass | 需求大类 | 文本 | - | 列头筛选+高级搜索 |
| 11 | reqSonClass | 需求子类 | 文本 | - | 列头筛选+高级搜索 |
| 12 | createdDept | 需求申请部门 | 文本 | - | 列头筛选+高级搜索 |
| 13 | createUser1 | 需求申请人 | 文本 | - | 高级搜索 |
| 14 | deptName | 需求负责人处室 | 文本 | - | 高级搜索 |
| 15 | startUserName | 创建人 | 文本 | - | 高级搜索 |
| 16 | upTime | 预计上线时间 | 时间戳 | 是 | 高级搜索(日期范围) |
| 17 | createTime | 需求提出时间 | 时间戳 | 是 | 高级搜索(日期范围) |

**默认排序实现（按创建时间降序）**:

需求池列表默认按 `createTime` 字段**降序排列**，最新同步过来的需求最先显示。在后端查询时，会在 SQL 中添加默认排序条件：

```sql
-- 默认排序：按创建时间降序
ORDER BY create_time DESC
```

**状态列实现**:

```vue
<!-- 需求池列表页状态列 -->
<ms-table-column
    prop="poolStatus"
    :filters="[{text: '未创建', value: 'PENDING'}, {text: '已创建', value: 'CREATED'}, {text: '已取消', value: 'CANCELLED'}]"
    column-key="poolStatus"
    label="需求池状态"
    min-width="120px"
>
  <template v-slot:default="scope">
    <el-tag :type="getStatusType(scope.row.poolStatus)" size="mini">
      {{ getStatusText(scope.row.poolStatus) }}
    </el-tag>
  </template>
</ms-table-column>
```

**字段说明**:
- 状态：PENDING（未创建）、CREATED（已创建）、CANCELLED（已取消）
- 操作列：
  - 未创建状态：显示可点击的"创建测试计划"按钮
  - 已创建状态：显示**置灰不可点击的"创建测试计划"按钮**
  - 已取消状态：显示**置灰不可点击的"创建测试计划"按钮**

**高级搜索 UI 组件**:
- 使用 `ms-table-header` 公共组件（位于 `framework/sdk-parent`）
- 支持配置化的高级搜索功能
- 使用 page.condition 机制处理搜索条件

### 创建需求弹窗

**触发方式**: 点击需求池列表页左上角"创建需求"按钮

**弹窗布局（双列布局，宽度960px）**:
```
┌──────────────────────────────────────────────────────────────────────┐
│ 创建需求                                                    [×]      │
├──────────────────────────────────────────────────────────────────────┤
│ 需求编号：[REQ-2024-001*]      │ 需求名称：[需求名称*]             │
│ 所属系统：[未指定]              │ 需求负责人：[待分配]               │
│ 需求大类：[功能需求]            │ 需求子类：[新增功能]               │
│ 预计上线时间：[日期选择器]      │ 主流程编码：[-]                    │
│ 当前环节：[测试待处理]          │ 当前处理人：[待分配]               │
│ 需求申请部门：[-]              │ 需求申请人：[-]                    │
│ 需求负责人处室：[-]            │ 创建人：[-]                        │
│                                                                    │
│                                           [取消]  [保存]           │
└──────────────────────────────────────────────────────────────────────┘
```

**交互说明**:
- 需求编号、需求名称为必填项（带*标记）
- 其余字段均预填充默认值
- 保存时校验 dmpNum 唯一性
- 创建成功后默认状态为 PENDING

### 创建测试计划弹窗

#### 从需求池创建测试计划

**触发方式**: 在需求池列表行点击"创建测试计划"按钮

**弹窗布局**:
```
┌─────────────────────────────────────────────────────────────┐
│ 创建测试计划                                      [×]        │
├─────────────────────────────────────────────────────────────┤
│ 需求编号：REQ-2024-001（只读）                               │
│ 计划名称：需求名称（只读，不可编辑）                         │
│ 负责人：  [选择负责人▼]                                      │
│ 计划开始时间：[2024-01-01]                                   │
│ 计划结束时间：[2024-03-01]                                   │
│ 描述：    [文本框]                                           │
│                                                             │
│                                    [取消]  [确定]           │
└─────────────────────────────────────────────────────────────┘
```

**交互说明**:
- 需求编号：自动填充，只读，不可修改
- 计划名称：自动填充需求名称，**只读，不可编辑**
- 创建成功后：跳转到测试计划详情页

#### 直接创建测试计划（测试计划页面入口）

**触发方式**: 在测试计划模块点击"新建测试计划"按钮

**弹窗布局**:
```
┌─────────────────────────────────────────────────────────────┐
│ 新建测试计划                                      [×]        │
├─────────────────────────────────────────────────────────────┤
│ 计划名称：[输入计划名称]（可编辑）                           │
│ 负责人：  [选择负责人▼]                                      │
│ 计划开始时间：[2024-01-01]                                   │
│ 计划结束时间：[2024-03-01]                                   │
│ 描述：    [文本框]                                           │
│                                                             │
│                                    [取消]  [确定]           │
└─────────────────────────────────────────────────────────────┘
```

**交互说明**:
- 不显示需求编号字段
- 计划名称：用户输入，可编辑
- 后端自动将 `requirementNumber` 设置为 null
- 创建成功后：跳转到测试计划详情页

---

## 涉及文件清单

### 后端文件（已落地）

| 文件路径 | 修改类型 | 说明 |
|----------|----------|------|
| `test-track/backend/src/main/resources/db/migration/2.10.23.ddl/V19__create_requirement_pool_table.sql` | 新增 | 数据库迁移脚本（含建表、test_plan扩展、测试数据） |
| `test-track/backend/src/main/java/io/metersphere/base/domain/RequirementPool.java` | 新增 | 需求池实体类 |
| `test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtRequirementPoolMapper.java` | 新增 | 扩展Mapper接口 |
| `test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtRequirementPoolMapper.xml` | 新增 | Mapper XML（含高级搜索combine/filter/condition） |
| `test-track/backend/src/main/java/io/metersphere/requirement/pool/controller/RequirementPoolController.java` | 新增 | 需求池控制器 |
| `test-track/backend/src/main/java/io/metersphere/requirement/pool/service/RequirementPoolService.java` | 新增 | 需求池业务逻辑层 |
| `test-track/backend/src/main/java/io/metersphere/requirement/pool/request/CreateRequirementPoolRequest.java` | 新增 | 创建需求请求DTO |
| `test-track/backend/src/main/java/io/metersphere/requirement/pool/request/QueryRequirementPoolRequest.java` | 新增 | 查询需求请求DTO |

### 后端文件（回传 - 已落地）

| 文件路径 | 修改类型 | 说明 |
|----------|----------|------|
| `test-track/backend/src/main/java/io/metersphere/requirement/pool/dto/RequirementCallbackMessage.java` | 新增 | 回传消息DTO |
| `test-track/backend/src/main/java/io/metersphere/requirement/pool/producer/RequirementCallbackMessageSender.java` | 新增 | 回传消息发送接口 |
| `test-track/backend/src/main/java/io/metersphere/requirement/pool/producer/RequirementCallbackProducer.java` | 新增 | RocketMQ 回传生产者 |
| `test-track/backend/src/main/java/io/metersphere/plan/service/TestPlanService.java` | 修改 | 新增 sendRequirementCompletedCallback()、getLatestDbReportUrl()、getPrincipalUsers() |
| `test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtRequirementPoolMapper.java` | 修改 | 新增 updateCallbackResult() 方法 |
| `test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtRequirementPoolMapper.xml` | 修改 | 新增 updateCallbackResult SQL、回传字段映射 |
| `test-track/backend/src/main/java/io/metersphere/base/domain/RequirementPool.java` | 修改 | 新增 testStatus、planShareUrl、lastCallbackTime 字段 |
| `test-track/backend/src/main/resources/db/migration/2.10.26/ddl/V19__create_requirement_pool_table.sql` | 修改 | 新增 test_status、plan_share_url、last_callback_time 列 |

### 后端文件（待开发）

| 文件路径 | 修改类型 | 说明 |
|----------|----------|------|
| `test-track/backend/src/main/java/io/metersphere/track/dto/RequirementSyncMessage.java` | 新增 | 同步消息DTO |
| `test-track/backend/src/main/java/io/metersphere/track/dto/RequirementCallbackMessage.java` | 新增 | 回传消息DTO |
| `test-track/backend/src/main/java/io/metersphere/track/consumer/RequirementSyncConsumer.java` | 新增 | MQ消费者 |
| `test-track/backend/src/main/java/io/metersphere/track/producer/RequirementCallbackProducer.java` | 新增 | MQ生产者 |
| `test-track/backend/pom.xml` | 修改 | 添加RocketMQ依赖 |

### 前端文件（已落地）

| 文件路径 | 修改类型 | 说明 |
|----------|----------|------|
| `test-track/frontend/src/business/requirement-pool/list.vue` | 新增 | 需求池列表页（17列+状态Tag+操作按钮） |
| `test-track/frontend/src/business/requirement-pool/components/CreateRequirementDialog.vue` | 新增 | 创建需求弹窗（双列布局+默认值） |
| `test-track/frontend/src/api/requirement-pool.js` | 新增 | 需求池API接口 |
| `test-track/frontend/src/router/modules/track.js` | 修改 | 添加需求池路由 |
| `framework/sdk-parent/frontend/src/utils/default-table-header.js` | 修改 | 添加 REQUIREMENT_POOL_LIST 列配置（17列 + testStatus/planShareUrl 回传展示列） |
| `framework/sdk-parent/frontend/src/components/search/search-components.js` | 修改 | 添加17个高级搜索配置+BUILTIN_ADV_SEARCH_KEYS |
| `test-track/frontend/src/business/plan/components/TestPlanEdit.vue` | 修改 | 支持openFromRequirement()从需求池创建 |
| `test-track/frontend/src/business/requirement-pool/list.vue` | 修改 | 新增 testStatus（el-tag）、planShareUrl（el-link 查看报告）列 |

### 配置文件

| 文件路径 | 修改类型 | 说明 |
|----------|----------|------|
| `/opt/metersphere/conf/metersphere.properties` | 修改 | 添加RocketMQ配置 |

---

## 高级搜索设计（参考缺陷管理）

### page.condition 四大核心参数

| 参数 | 作用 | 数据类型 | 使用场景 |
|------|------|---------|---------|
| **filters** | 列过滤（精确匹配） | Object | 表头筛选、权限过滤 |
| **combine** | 组合查询（模糊匹配） | Object | 高级搜索 |
| **orders** | 排序规则 | Array | 列排序 |
| **components** | 搜索组件配置 | Array | 高级搜索UI |

### BUILTIN_ADV_SEARCH_KEYS 路由机制

`BUILTIN_ADV_SEARCH_KEYS` 是一个前端白名单数组，决定搜索字段的值放入 `combine[key]`（直接SQL处理）还是 `combine.customs[]`（自定义字段系统处理）。

**需求池已注册的 BUILTIN_ADV_SEARCH_KEYS**:
```javascript
'dmpNum', 'requirementName', 'poolStatus', 'systemName', 'reqFatherClass',
'reqSonClass', 'reqManagerName', 'actName', 'parentWfinstCode', 'operationType',
'assigneeName', 'createdDept', 'createUser1', 'deptName', 'startUserName',
'upTime'
```

**路由逻辑**（在 `MsTableAdvSearchBar.vue` / `MsTableAdvSearch.vue` 中）:
```javascript
setCondition(condition, component) {
  const key = component && component.key ? String(component.key) : '';
  const isBuiltin = BUILTIN_ADV_SEARCH_KEY_SET.has(key);
  if (!isBuiltin) {
    // 非内置字段 → combine.customs[]（自定义字段查询）
    this.handleCustomField(condition, component);
    return;
  }
  // 内置字段 → combine[key] = { operator, value }（直接SQL条件）
  condition[key] = {
    operator: component.operator.value,
    value: component.value,
  };
}
```

### 高级搜索组件配置（17项，完整版）

**前端配置文件**：`framework/sdk-parent/frontend/src/components/search/search-components.js`

```javascript
export const REQUIREMENT_POOL_LIST = [
  REQUIREMENT_DMP_NUM,           // 需求编号 - 文本模糊
  REQUIREMENT_NAME,              // 需求名称 - 文本模糊
  REQUIREMENT_POOL_STATUS,       // 需求池状态 - 下拉多选
  REQUIREMENT_SYSTEM_NAME,       // 所属系统 - 下拉多选
  REQUIREMENT_FATHER_CLASS,      // 需求大类 - 下拉多选
  REQUIREMENT_SON_CLASS,         // 需求子类 - 下拉多选
  REQUIREMENT_MANAGER_NAME,      // 需求负责人 - 文本模糊
  REQUIREMENT_ACT_NAME,          // 当前环节 - 文本模糊
  REQUIREMENT_PARENT_WFINST_CODE,// 主流程编码 - 文本模糊
  REQUIREMENT_ASSIGNEE_NAME,     // 当前处理人 - 文本模糊
  REQUIREMENT_CREATED_DEPT,      // 需求申请部门 - 文本模糊
  REQUIREMENT_CREATE_USER,       // 需求申请人 - 文本模糊
  REQUIREMENT_DEPT_NAME,         // 需求负责人处室 - 文本模糊
  REQUIREMENT_START_USER_NAME,   // 创建人 - 文本模糊
  REQUIREMENT_OPERATION_TYPE,    // 操作类型 - 文本模糊
  CREATE_TIME,                   // 需求提出时间 - 日期范围
  REQUIREMENT_UP_TIME,           // 预计上线时间 - 日期范围
];
```

### 后端 Mapper XML 处理（ExtRequirementPoolMapper.xml）

**combine SQL片段**（高级搜索条件，17个字段全部支持）:
```xml
<sql id="combine">
    <!-- 文本模糊搜索字段 -->
    <if test="${condition}.dmpNum != null">
        and requirement_pool.dmp_num <include refid="condition"><property name="object" value="${condition}.dmpNum"/></include>
    </if>
    <if test="${condition}.requirementName != null">
        and requirement_pool.requirement_name <include refid="condition"><property name="object" value="${condition}.requirementName"/></include>
    </if>
    <if test="${condition}.reqManagerName != null">
        and requirement_pool.req_manager_name <include refid="condition"><property name="object" value="${condition}.reqManagerName"/></include>
    </if>
    <if test="${condition}.actName != null">
        and requirement_pool.act_name <include refid="condition"><property name="object" value="${condition}.actName"/></include>
    </if>
    <if test="${condition}.operationType != null">
        and requirement_pool.operation_type <include refid="condition"><property name="object" value="${condition}.operationType"/></include>
    </if>
    <if test="${condition}.assigneeName != null">
        and requirement_pool.assignee_name <include refid="condition"><property name="object" value="${condition}.assigneeName"/></include>
    </if>
    <if test="${condition}.createdDept != null">
        and requirement_pool.created_dept <include refid="condition"><property name="object" value="${condition}.createdDept"/></include>
    </if>
    <if test="${condition}.createUser1 != null">
        and requirement_pool.create_user1 <include refid="condition"><property name="object" value="${condition}.createUser1"/></include>
    </if>
    <if test="${condition}.deptName != null">
        and requirement_pool.dept_name <include refid="condition"><property name="object" value="${condition}.deptName"/></include>
    </if>
    <if test="${condition}.startUserName != null">
        and requirement_pool.start_user_name <include refid="condition"><property name="object" value="${condition}.startUserName"/></include>
    </if>
    <!-- 精确/多选搜索字段 -->
    <if test="${condition}.poolStatus != null">
        and requirement_pool.pool_status <include refid="condition"><property name="object" value="${condition}.poolStatus"/></include>
    </if>
    <if test="${condition}.systemName != null">
        and requirement_pool.system_name <include refid="condition"><property name="object" value="${condition}.systemName"/></include>
    </if>
    <if test="${condition}.reqFatherClass != null">
        and requirement_pool.req_father_class <include refid="condition"><property name="object" value="${condition}.reqFatherClass"/></include>
    </if>
    <if test="${condition}.reqSonClass != null">
        and requirement_pool.req_son_class <include refid="condition"><property name="object" value="${condition}.reqSonClass"/></include>
    </if>
    <if test="${condition}.parentWfinstCode != null">
        and requirement_pool.parent_wfinst_code <include refid="condition"><property name="object" value="${condition}.parentWfinstCode"/></include>
    </if>
    <!-- 日期范围搜索字段 -->
    <if test="${condition}.createTime != null">
        and requirement_pool.create_time <include refid="condition"><property name="object" value="${condition}.createTime"/></include>
    </if>
    <if test="${condition}.upTime != null">
        and requirement_pool.up_time <include refid="condition"><property name="object" value="${condition}.upTime"/></include>
    </if>
</sql>
```

**filter SQL片段**（列头快速筛选，9个字段支持）:
```xml
<sql id="filter">
    poolStatus, systemName, reqFatherClass, reqSonClass, reqManagerName,
    actName, assigneeName, createdDept, operationType
</sql>
```

---

## 关键技术点

### 1. 幂等处理

基于 `dmpNum + eventTime` 进行幂等判断：
```java
RequirementPool existing = requirementPoolMapper.selectByDmpNum(dmpNum);
if (existing != null && existing.getEventTime() >= message.getEventTime()) {
    return; // 已处理过更新的消息，跳过
}
```

### 2. 并发控制

- 数据库唯一索引：`test_plan.requirement_number` 唯一索引
- 乐观锁：需求池状态更新时检查当前状态

### 3. 一对一关联保证

```sql
-- MySQL 唯一索引允许多个 null 值
ALTER TABLE `test_plan` ADD UNIQUE KEY `uk_requirement_number` (`requirement_number`);
```

### 4. 状态流转

```
需求同步消息
    ↓
需求池（PENDING）
    ↓
点击创建测试计划
    ↓
需求池（CREATED） + 测试计划（PENDING）
    ↓
测试执行
    ↓
测试计划（COMPLETED）
    ↓
状态回传
```

### 5. 自定义表头机制

需求池列表使用 MeterSphere 的自定义表头系统：
- `default-table-header.js` 中定义 `REQUIREMENT_POOL_LIST` 列配置（id, key, label, minWidth, sortable, filters）
- `getCustomTableHeader()` 读取默认配置 + localStorage 用户自定义列显隐
- 每列通过 `key`（单字符）进行 localStorage 序列化
- 列表页 `list.vue` 通过 `v-if="item.id === 'xxx'"` 逐列渲染

---

## RocketMQ 配置

```properties
# RocketMQ 配置
rocketmq.name-server=10.12.105.254:9876

# Topic 与客户端 ID 配置
rocketmq.topic.requirement-sync=topic-requirement-to-metersphere
rocketmq.producer.requirement-sync=producer-requirement-to-metersphere
rocketmq.consumer.requirement-sync=consumer-requirement-to-metersphere
rocketmq.topic.status-callback=topic-metersphere-to-requirement
rocketmq.producer.status-callback=producer-metersphere-to-requirement
rocketmq.consumer.status-callback=consumer-metersphere-to-requirement
```

---

## Maven 依赖

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>4.9.2</version>
</dependency>
```

---

## 详细实现方案

### 开发环境准备

**RocketMQ 配置**

在 `metersphere.properties` 中添加：
```properties
# RocketMQ 配置
rocketmq.name-server=10.12.105.254:9876

# Topic 与客户端 ID 配置
rocketmq.topic.requirement-sync=topic-requirement-to-metersphere
rocketmq.producer.requirement-sync=producer-requirement-to-metersphere
rocketmq.consumer.requirement-sync=consumer-requirement-to-metersphere
rocketmq.topic.status-callback=topic-metersphere-to-requirement
rocketmq.producer.status-callback=producer-metersphere-to-requirement
rocketmq.consumer.status-callback=consumer-metersphere-to-requirement
```

**Maven 依赖**

在 `test-track/backend/pom.xml` 中添加：
```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>4.9.2</version>
</dependency>
```

### 数据库实现

**步骤1：创建 Migration 文件**

文件路径：`test-track/backend/src/main/resources/db/migration/2.10.23.ddl/V19__create_requirement_pool_table.sql`

```sql
SET SESSION innodb_lock_wait_timeout = 7200;

-- 创建需求池表
CREATE TABLE IF NOT EXISTS `requirement_pool` (
  `id` VARCHAR(32) NOT NULL COMMENT '主键',
  `dmp_num` VARCHAR(64) NOT NULL COMMENT '需求编号（唯一）',
  `requirement_name` VARCHAR(255) NOT NULL COMMENT '需求名称',
  `pool_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '需求池状态：PENDING/CREATED/CANCELLED',
  `parent_wfinst_code` VARCHAR(100) DEFAULT NULL COMMENT '主流程编码',
  `act_name` VARCHAR(100) DEFAULT NULL COMMENT '当前环节',
  `operation_type` VARCHAR(32) DEFAULT NULL COMMENT '操作类型：CREATED/UPDATED/CANCELLED',
  `system_name` VARCHAR(255) DEFAULT NULL COMMENT '所属系统',
  `req_manager_name` VARCHAR(64) DEFAULT NULL COMMENT '需求负责人',
  `assignee_name` VARCHAR(64) DEFAULT NULL COMMENT '当前处理人',
  `created_dept` VARCHAR(255) DEFAULT NULL COMMENT '需求申请部门',
  `create_user1` VARCHAR(64) DEFAULT NULL COMMENT '需求申请人',
  `dept_name` VARCHAR(255) DEFAULT NULL COMMENT '需求负责人处室',
  `start_user_name` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `req_father_class` VARCHAR(255) DEFAULT NULL COMMENT '需求大类',
  `req_son_class` VARCHAR(255) DEFAULT NULL COMMENT '需求子类',
  `create_time` BIGINT DEFAULT NULL COMMENT '需求提出时间（时间戳）',
  `up_time` BIGINT DEFAULT NULL COMMENT '预计上线时间（时间戳）',
  `linked_plan_id` VARCHAR(50) DEFAULT NULL COMMENT '关联的测试计划ID',
  `linked_plan_name` VARCHAR(255) DEFAULT NULL COMMENT '关联的测试计划名称',
  `last_sync_time` BIGINT NOT NULL COMMENT '最后同步时间（时间戳）',
  `event_time` BIGINT DEFAULT NULL COMMENT '消息事件时间（时间戳）',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '追踪ID',
  `created_at` BIGINT NOT NULL COMMENT '创建时间（时间戳）',
  `updated_at` BIGINT NOT NULL COMMENT '更新时间（时间戳）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dmp_num` (`dmp_num`),
  KEY `idx_pool_status` (`pool_status`),
  KEY `idx_system_name` (`system_name`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求池表';

-- 扩展测试计划表
ALTER TABLE `test_plan` ADD COLUMN `requirement_number` VARCHAR(100) DEFAULT NULL COMMENT '需求编号' AFTER `tags`;
ALTER TABLE `test_plan` ADD UNIQUE KEY `uk_requirement_number` (`requirement_number`);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
```

### RocketMQ消费者实现

**步骤2：创建消息DTO**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/dto/RequirementSyncMessage.java`

```java
package io.metersphere.track.dto;

import lombok.Data;

@Data
public class RequirementSyncMessage {
    private String dmpNum;
    private String name1;               // 需求名称
    private String operationType;       // CREATED/UPDATED/CANCELLED
    private String reqManagerName;      // 需求负责人
    private String actName;             // 当前环节
    private Long createTime;            // 需求提出时间
    private String parentWfinstCode;    // 主流程编码
    private String reqFatherClass;      // 需求大类
    private String reqSonClass;         // 需求子类
    private String systemName;          // 所属系统
    private Long upTime;                // 预计上线时间
    private String assigneeName;        // 当前处理人
    private String createdept;          // 需求申请部门（注意：需求平台字段名无下划线）
    private String createUser1;         // 需求申请人（注意：后缀为1）
    private String deptName;            // 需求负责人处室
    private String startUserName;       // 创建人
    private Long eventTime;             // 消息事件时间
    private String traceId;             // 追踪ID
}
```

**步骤3：创建消费者**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/consumer/RequirementSyncConsumer.java`

```java
package io.metersphere.track.consumer;

import com.alibaba.fastjson.JSON;
import io.metersphere.track.dto.RequirementSyncMessage;
import io.metersphere.track.service.RequirementSyncService;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

@Component
public class RequirementSyncConsumer {
    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.consumer.group}")
    private String consumerGroup;

    @Value("${rocketmq.topic.requirement-sync}")
    private String topic;

    @Resource
    private RequirementSyncService requirementSyncService;

    @PostConstruct
    public void init() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(topic, "*");

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                           ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    try {
                        String body = new String(msg.getBody());
                        RequirementSyncMessage message = JSON.parseObject(body, RequirementSyncMessage.class);
                        requirementSyncService.handleSyncMessage(message);
                    } catch (Exception e) {
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
    }
}
```

### 业务服务层实现

**步骤4：创建同步服务**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/service/RequirementSyncService.java`

```java
package io.metersphere.track.service;

import io.metersphere.base.domain.RequirementPool;
import io.metersphere.track.dto.RequirementSyncMessage;
import io.metersphere.base.mapper.ext.ExtRequirementPoolMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.UUID;

@Service
public class RequirementSyncService {
    @Resource
    private ExtRequirementPoolMapper extRequirementPoolMapper;

    @Transactional
    public void handleSyncMessage(RequirementSyncMessage message) {
        // 幂等检查
        RequirementPool existing = extRequirementPoolMapper.selectByDmpNum(message.getDmpNum());
        if (existing != null && existing.getEventTime() >= message.getEventTime()) {
            return; // 已处理过更新的消息
        }

        String operationType = message.getOperationType();
        if ("CREATED".equals(operationType) || "UPDATED".equals(operationType)) {
            saveOrUpdate(message, existing);
        } else if ("CANCELLED".equals(operationType)) {
            cancelRequirement(message.getDmpNum());
        }
    }

    private void saveOrUpdate(RequirementSyncMessage message, RequirementPool existing) {
        RequirementPool pool = new RequirementPool();
        pool.setId(existing != null ? existing.getId() : UUID.randomUUID().toString().replace("-", ""));
        pool.setDmpNum(message.getDmpNum());
        pool.setRequirementName(message.getName1());
        pool.setReqManagerName(message.getReqManagerName());
        pool.setActName(message.getActName());
        pool.setCreateTime(message.getCreateTime());
        pool.setParentWfinstCode(message.getParentWfinstCode());
        pool.setReqFatherClass(message.getReqFatherClass());
        pool.setReqSonClass(message.getReqSonClass());
        pool.setSystemName(message.getSystemName());
        pool.setUpTime(message.getUpTime());
        pool.setAssigneeName(message.getAssigneeName());
        pool.setCreatedDept(message.getCreatedept());
        pool.setCreateUser1(message.getCreateUser1());
        pool.setDeptName(message.getDeptName());
        pool.setStartUserName(message.getStartUserName());
        pool.setOperationType(message.getOperationType());
        pool.setEventTime(message.getEventTime());
        pool.setTraceId(message.getTraceId());
        pool.setLastSyncTime(System.currentTimeMillis());
        pool.setUpdatedAt(System.currentTimeMillis());

        if (existing == null) {
            pool.setPoolStatus("PENDING");
            pool.setCreatedAt(System.currentTimeMillis());
            extRequirementPoolMapper.insert(pool);
        } else {
            extRequirementPoolMapper.updateByPrimaryKey(pool);
        }
    }

    private void cancelRequirement(String dmpNum) {
        extRequirementPoolMapper.updatePoolStatusAndLinkedPlan(dmpNum, "CANCELLED", null, null);
    }
}
```

### 状态回传实现

**步骤5：创建回传消息DTO**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/dto/RequirementCallbackMessage.java`

```java
package io.metersphere.track.dto;

import lombok.Data;

@Data
public class RequirementCallbackMessage {
    private String dmpNum;              // 需求编号（关联主键）
    private String planStatus;           // 测试计划状态（需求平台最关心的字段）
    private Long plannedStartTime;       // 计划开始时间
    private Long plannedEndTime;         // 计划结束时间
    private Long actualStartTime;        // 实际开始时间
    private Long actualEndTime;          // 实际结束时间
    private String principalUsers;       // 负责人
    private String planShareUrl;         // 测试计划报告链接: /#/track/testPlan/reportList?resourceId={reportId}
    private Long syncTime;               // 同步时间
    private String traceId;              // 追踪ID
}
```

**步骤6：创建回传生产者**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/producer/RequirementCallbackProducer.java`

```java
package io.metersphere.track.producer;

import com.alibaba.fastjson.JSON;
import io.metersphere.track.dto.RequirementCallbackMessage;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

@Component
public class RequirementCallbackProducer {
    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    @Value("${rocketmq.topic.status-callback}")
    private String topic;

    private DefaultMQProducer producer;

    @PostConstruct
    public void init() throws Exception {
        producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.start();
    }

    public void sendCallback(RequirementCallbackMessage message) {
        try {
            String body = JSON.toJSONString(message);
            Message msg = new Message(topic, body.getBytes());
            SendResult result = producer.send(msg);
        } catch (Exception e) {
            // 记录失败日志
        }
    }
}
```

**步骤7：在测试计划状态更新时触发回传**

在 `TestPlanService` 的状态更新方法中添加：

```java
public void updateStatus(String planId, String status) {
    TestPlan plan = testPlanMapper.selectByPrimaryKey(planId);
    plan.setStatus(status);
    testPlanMapper.updateByPrimaryKey(plan);

    // 只有关联需求的测试计划才触发回传（requirementNumber != null）
    if (plan.getRequirementNumber() != null) {
        RequirementCallbackMessage message = new RequirementCallbackMessage();
        message.setDmpNum(plan.getRequirementNumber());
        message.setPlanStatus(status);
        message.setPlannedStartTime(plan.getPlannedStartTime());
        message.setPlannedEndTime(plan.getPlannedEndTime());
        message.setActualStartTime(plan.getActualStartTime());
        message.setActualEndTime(plan.getActualEndTime());
        message.setPrincipalUsers(plan.getPrincipal());
        message.setSyncTime(System.currentTimeMillis());

        requirementCallbackProducer.sendCallback(message);
    }
    // 直接创建的测试计划（requirementNumber = null）不回传
}
```

### V1 实际实现（与设计差异）

**差异 1: 仅回传 Completed 状态**
- V1 仅回传 `Completed` 状态，不在 `updateStatus()` 通用方法中触发
- 回传触发点：`TestPlanService.editTestPlan()` 保存后调用 `sendRequirementCompletedCallback()`
- 触发条件：`requirementNumber != null` && 状态变更 && 新状态为 `Completed`

**差异 2: 报告自动生成**
- 回传前调用 `getLatestDbReportUrl()` 获取最新报告链接
- 若无历史报告，自动调用 `genTestPlanReport()` + `genTestPlanReportContent()` + `countReportByTestPlanReportId()` 生成
- 报告链接格式: `/#/track/testPlan/reportList?resourceId={reportId}`

**差异 3: 回传同时更新需求池**
- 回传成功后调用 `extRequirementPoolMapper.updateCallbackResult()` 更新需求池
- 更新字段: `test_status`、`plan_share_url`、`last_callback_time`
- 需求池页面可直接展示回传结果（模拟需求平台视角）

**差异 4: Producer 实现模式**
- 使用 `InitializingBean.afterPropertiesSet()` 模式（非 `@PostConstruct`）
- 通过 `RequirementCallbackMessageSender` 接口解耦
- 配置 key: `rocketmq.producer.status-callback`（默认 `producer-metersphere-to-requirement`）

---

## 安全设计

### 消息传输安全

**RocketMQ认证**:
- 配置AccessKey和SecretKey
- 启用ACL权限控制
- Topic级别的读写权限隔离

**消息加密**:
- 敏感字段（如有）进行加密传输
- 使用TLS/SSL加密通道

### 接口访问控制

**权限控制**:
- 需求池查询：需要登录用户权限
- 创建测试计划：需要测试计划创建权限
- 接口调用需要携带有效的认证Token

**数据隔离**:
- 需求池全局可见（V1不做资源隔离）
- 测试计划按项目隔离

### 数据审计

**操作日志**:
- 记录需求同步日志（消息ID、时间、操作类型）
- 记录测试计划创建日志（操作人、时间、需求编号）
- 记录状态回传日志（回传时间、回传内容、是否成功）

**追踪机制**:
- 使用traceId贯穿整个流程
- 便于问题排查和数据追溯

### 异常处理

**幂等控制**:
- 基于dmpNum + eventTime进行幂等判断
- 防止重复消费导致数据污染

**并发控制**:
- 数据库唯一索引：test_plan.requirement_number
- 乐观锁：需求池状态更新时检查当前状态

**失败重试**:
- 消息消费失败：RocketMQ自动重试
- 回传失败：记录失败日志，支持手工重放

---

## 状态流转图

```
需求同步消息
    ↓
需求池（PENDING）
    ↓
点击创建测试计划
    ↓
需求池（CREATED） + 测试计划（PENDING）
    ↓
测试执行
    ↓
测试计划（COMPLETED）
    ↓
状态回传
```

## 关键技术点总结

1. **幂等处理**: 基于dmpNum + eventTime，防止重复消费
2. **并发控制**: 数据库唯一索引 + 业务状态检查
3. **一对一关联**: test_plan.requirement_number唯一索引保证
4. **消息可靠性**: RocketMQ事务消息 + 失败重试
5. **数据追溯**: traceId + 操作日志 + 审计字段
6. **自定义表头**: REQUIREMENT_POOL_LIST 17列配置 + localStorage 列显隐
7. **高级搜索**: 17个字段全部注册BUILTIN_ADV_SEARCH_KEYS，combine片段完整覆盖
