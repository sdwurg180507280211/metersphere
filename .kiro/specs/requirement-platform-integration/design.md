# 设计文档：全流程平台对接

## 概述

本文档对需求平台与MeterSphere测试平台对接功能的详细设计进行全面而准确的描述，为开发人员在实现软件功能时提供指导和参考。详细的设计规范和流程将有助于保证软件的稳定性、可维护性和可扩展性。

实现需求平台与 MeterSphere 测试平台的双向数据对接，通过 RocketMQ 消息队列实现异步通信。核心流程包括：需求同步入池、需求池管理、测试计划创建、状态回传。

**文档版本**：V1.0
**编制日期**：2026年3月12日

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
│                 │  topic-requirement-to-ms   │                  │
│                 │                            │  ┌─────────────┐ │
│                 │                            │  │ 需求池模块  │ │
│                 │                            │  └─────────────┘ │
│                 │                            │  ┌─────────────┐ │
│                 │  topic-ms-to-requirement   │  │ 测试计划模块│ │
│                 │ <──────────────────────────│  └─────────────┘ │
└─────────────────┘         RocketMQ          └──────────────────┘
```

**核心流程**：
1. 需求平台通过RocketMQ发送需求消息
2. MeterSphere消费消息，写入需求池
3. 测试人员在需求池中点击创建测试计划（需求池入口）
4. 测试计划状态变化时，通过RocketMQ回传给需求平台

**两个创建测试计划入口**：
1. **需求池入口**：从需求池列表页点击"创建测试计划"按钮，自动关联需求，计划名称自动填充且只读
2. **测试计划入口**：在测试计划模块点击"新建测试计划"按钮，不关联需求，计划名称由用户输入可编辑

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
- `TestPlanController`：接口控制器（扩展）
- `TestPlanService`：业务逻辑层（扩展）
- `RequirementPlanRelationService`：关联关系管理

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
| id | VARCHAR | 50 | 是 | 主键 |
| dmp_num | VARCHAR | 100 | 是 | 需求编号（唯一索引） |
| requirement_name | VARCHAR | 500 | 是 | 需求名称 |
| pool_status | VARCHAR | 20 | 是 | 需求池状态：PENDING/CREATED/CANCELLED |
| parent_wfinst_code | VARCHAR | 100 | 否 | 主流程编码 |
| req_father_class | VARCHAR | 100 | 否 | 需求大类 |
| req_son_class | VARCHAR | 100 | 否 | 需求子类 |
| system_name | VARCHAR | 200 | 否 | 所属系统 |
| act_name | VARCHAR | 100 | 否 | 当前环节 |
| req_manager_name | VARCHAR | 100 | 否 | 需求负责人 |
| assignee_name | VARCHAR | 100 | 否 | 当前处理人 |
| created_dept | VARCHAR | 200 | 否 | 需求申请部门 |
| create_user | VARCHAR | 100 | 否 | 需求申请人 |
| dept_name | VARCHAR | 200 | 否 | 需求负责人处室 |
| start_user_name | VARCHAR | 100 | 否 | 创建人 |
| create_time | BIGINT | - | 否 | 需求提出时间（时间戳） |
| up_time | BIGINT | - | 否 | 预计上线时间（时间戳） |
| linked_plan_id | VARCHAR | 50 | 否 | 关联的测试计划ID |
| last_sync_time | BIGINT | - | 是 | 最后同步时间（时间戳） |
| event_time | BIGINT | - | 否 | 消息事件时间（时间戳） |
| trace_id | VARCHAR | 100 | 否 | 追踪ID |
| created_at | BIGINT | - | 是 | 创建时间（时间戳） |
| updated_at | BIGINT | - | 是 | 更新时间（时间戳） |

**索引设计**:
- PRIMARY KEY: `id`
- UNIQUE INDEX: `uk_dmp_num` ON `dmp_num`
- INDEX: `idx_pool_status` ON `pool_status`
- INDEX: `idx_system_name` ON `system_name`
- INDEX: `idx_create_time` ON `create_time`

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
    "reqFatherClass": ["功能需求"]
  },
  "combine": {
    "dmpNum": { "operator": "like", "value": "REQ-2024" },
    "requirementName": { "operator": "like", "value": "需求" },
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

**后端实现示例**:
```xml
<!-- Mapper XML 中的默认排序处理 -->
<if test="request.orders == null or request.orders.size() == 0">
  ORDER BY create_time DESC
</if>
<if test="request.orders != null and request.orders.size() > 0">
  ORDER BY
  <foreach collection="request.orders" item="order" separator=",">
    ${order.name} ${order.type}
  </foreach>
</if>
```

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
      "systemName": "系统名称",
      "reqManagerName": "张三",
      "createTime": 1234567890000,
      "linkedPlanId": null,
      "linkedPlanName": null
    }
  ]
}
```


### 3. 从需求池创建测试计划

**接口路径**: `POST /test-plan/create-from-requirement`

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

**接口路径**: `POST /test-plan/create`

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

### 状态回传消息（MeterSphere → 需求平台）

**Topic**: `topic-metersphere-to-requirement`

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
  "planShareUrl": "/track/share-plan-report?shareId={shareId}",
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
- `planShareUrl`：测试计划报告分享链接，格式：`/track/share-plan-report?shareId={shareId}`

---

## 核心类设计

### 实体类

```java
// RequirementPool.java
@Data
public class RequirementPool {
    private String id;
    private String dmpNum;              // 需求编号（唯一）
    private String requirementName;     // 需求名称
    private String poolStatus;          // PENDING/CREATED/CANCELLED
    private String parentWfinstCode;    // 主流程编码
    private String reqFatherClass;      // 需求大类
    private String reqSonClass;         // 需求子类
    private String systemName;          // 所属系统
    private String actName;             // 当前环节
    private String reqManagerName;      // 需求负责人
    private String assigneeName;        // 当前处理人
    private String createdDept;         // 需求申请部门
    private String createUser;          // 需求申请人
    private String deptName;            // 需求负责人处室
    private String startUserName;       // 创建人
    private Long createTime;            // 需求提出时间
    private Long upTime;                // 预计上线时间
    private String linkedPlanId;        // 关联的测试计划ID
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
    private String createdept;          // 需求申请部门
    private String createUser1;         // 需求申请人
    private String deptName;            // 需求负责人处室
    private String startUserName;       // 创建人
    private Long eventTime;             // 消息事件时间
    private String traceId;             // 追踪ID
}

// RequirementCallbackMessage.java
@Data
public class RequirementCallbackMessage {
    private String dmpNum;              // 需求编号（关联主键）
    private String planStatus;           // 测试计划状态（需求平台最关心的字段）
    private Long plannedStartTime;       // 计划开始时间
    private Long plannedEndTime;         // 计划结束时间
    private Long actualStartTime;        // 实际开始时间
    private Long actualEndTime;          // 实际结束时间
    private String principalUsers;       // 负责人
    private String planShareUrl;         // 测试计划报告分享链接：/track/share-plan-report?shareId={shareId}
    private Long syncTime;               // 同步时间
    private String traceId;              // 追踪ID
}

// RequirementPoolQueryCondition.java
@Data
public class RequirementPoolQueryCondition {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String dmpNum;
    private String requirementName;
    private String poolStatus;
    private String systemName;
    private String reqFatherClass;
    private Long createTimeStart;
    private Long createTimeEnd;
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

// RequirementPoolService.java
public interface RequirementPoolService {
    // 分页查询需求池
    Page<RequirementPool> listRequirements(RequirementPoolQueryCondition query);

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

**页面路径**: `/requirement-pool`

**页面布局**:
```
┌─────────────────────────────────────────────────────────────┐
│ 需求池                                                       │
├─────────────────────────────────────────────────────────────┤
│ 高级搜索：                                                   │
│ [ms-table-header 公共组件] （支持自定义字段搜索）            │
├─────────────────────────────────────────────────────────────┤
│ 需求编号 │ 需求名称 │ 状态 │ 系统 │ 负责人 │ 提出时间 │ 操作 │
│─────────┼─────────┼─────┼─────┼───────┼─────────┼──────│
│ REQ-001 │ 需求A   │ 未创建│ 系统A│ 张三   │ 2024-01-01│[创建]│
│ REQ-002 │ 需求B   │ 已创建│ 系统B│ 李四   │ 2024-01-02│[创建]│
│ REQ-003 │ 需求C   │ 已取消│ 系统C│ 王五   │ 2024-01-03│[创建]│
└─────────────────────────────────────────────────────────────┘
```

**默认排序实现（按创建时间降序）**:

需求池列表默认按 `createTime` 字段**降序排列**，最新同步过来的需求最先显示。在后端查询时，会在 SQL 中添加默认排序条件：

```sql
-- 默认排序：按创建时间降序
ORDER BY create_time DESC
```

在前端，通过 page.condition 的 orders 参数设置默认排序：

```javascript
// 需求池查询默认排序
this.page.condition.orders = [
  { name: 'create_time', type: 'desc' }
];
```

**状态列实现（模仿测试计划状态字段）**:

```vue
<!-- 需求池列表页状态列 -->
<ms-table-column
    prop="poolStatus"
    :filters="statusFilters"  <!-- 状态筛选器，模仿测试计划 -->
    :filtered-value="['PENDING', 'CREATED']"  <!-- 默认筛选值 -->
    column-key="poolStatus"
    :label="$t('requirement.pool.status')"
    min-width="80px"
>
  <template v-slot:default="scope">
    <span :class="getStatusClass(scope.row.poolStatus)">
      {{ getStatusText(scope.row.poolStatus) }}
    </span>
  </template>
</ms-table-column>

<!-- 状态筛选器数据 -->
statusFilters: [
  {
    text: this.$t('requirement.pool.status.pending'),
    value: 'PENDING'
  },
  {
    text: this.$t('requirement.pool.status.created'),
    value: 'CREATED'
  },
  {
    text: this.$t('requirement.pool.status.cancelled'),
    value: 'CANCELLED'
  }
]
```

**字段说明**:
- 状态：PENDING（未创建）、CREATED（已创建）、CANCELLED（已取消）
- 操作列：
  - 未创建状态：显示可点击的"创建测试计划"按钮
  - 已创建状态：显示**置灰不可点击的"创建测试计划"按钮**（不显示查看链接）
  - 已取消状态：显示**置灰不可点击的"创建测试计划"按钮**

**高级搜索 UI 组件**:
- 使用 `ms-table-header` 公共组件（位于 `framework/sdk-parent`）
- 支持配置化的高级搜索功能
- 使用 page.condition 机制处理搜索条件

### 需求池操作优化

**简化操作流程**：
- 不需要需求详情页
- 需求编号不再作为链接，直接在列表页操作
- 操作列按钮根据状态动态显示

**列表页操作优化**：
```
┌─────────────────────────────────────────────────────────────┐
│ 需求池                                                       │
├─────────────────────────────────────────────────────────────┤
│ 筛选条件：                                                   │
│ [需求编号] [需求名称] [状态▼] [所属系统▼] [需求大类▼] [搜索]│
├─────────────────────────────────────────────────────────────┤
│ 需求编号 │ 需求名称 │ 状态 │ 系统 │ 负责人 │ 提出时间 │ 操作 │
│─────────┼─────────┼─────┼─────┼───────┼─────────┼──────│
│ REQ-001 │ 需求A   │ 未创建│ 系统A│ 张三   │ 2024-01-01│[创建]│
│ REQ-002 │ 需求B   │ 已创建│ 系统B│ 李四   │ 2024-01-02│[创建]│
│ REQ-003 │ 需求C   │ 已取消│ 系统C│ 王五   │ 2024-01-03│[创建]│
└─────────────────────────────────────────────────────────────┘
```

**操作列按钮状态说明**：
- 未创建状态 →「创建测试计划」按钮**可点击**
- 已创建状态 →「创建测试计划」按钮**置灰不可点击**
- 已取消状态 →「创建测试计划」按钮**置灰不可点击**

### 创建测试计划弹窗

#### 从需求池创建测试计划

**触发方式**: 在需求池列表或详情页点击"创建测试计划"按钮

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

### 后端文件

| 文件路径 | 修改类型 | 说明 |
|----------|----------|------|
| `test-track/backend/src/main/resources/db/migration/2.10.23.ddl/V19__requirement_pool.sql` | 新增 | 数据库迁移脚本 |
| `test-track/backend/src/main/java/io/metersphere/track/domain/RequirementPool.java` | 新增 | 需求池实体类 |
| `test-track/backend/src/main/java/io/metersphere/track/dto/RequirementSyncMessage.java` | 新增 | 同步消息DTO |
| `test-track/backend/src/main/java/io/metersphere/track/dto/RequirementCallbackMessage.java` | 新增 | 回传消息DTO |
| `test-track/backend/src/main/java/io/metersphere/track/dto/RequirementPoolQueryCondition.java` | 新增 | 查询条件DTO |
| `test-track/backend/src/main/java/io/metersphere/track/mapper/RequirementPoolMapper.java` | 新增 | Mapper接口 |
| `test-track/backend/src/main/java/io/metersphere/track/mapper/RequirementPoolMapper.xml` | 新增 | Mapper XML |
| `test-track/backend/src/main/java/io/metersphere/track/service/RequirementSyncService.java` | 新增 | 同步服务 |
| `test-track/backend/src/main/java/io/metersphere/track/service/RequirementPoolService.java` | 新增 | 需求池服务 |
| `test-track/backend/src/main/java/io/metersphere/track/consumer/RequirementSyncConsumer.java` | 新增 | MQ消费者 |
| `test-track/backend/src/main/java/io/metersphere/track/producer/RequirementCallbackProducer.java` | 新增 | MQ生产者 |
| `test-track/backend/src/main/java/io/metersphere/track/controller/RequirementPoolController.java` | 新增 | 需求池控制器 |
| `test-track/backend/src/main/java/io/metersphere/track/service/TestPlanService.java` | 修改 | 扩展创建方法 |
| `test-track/backend/src/main/java/io/metersphere/track/domain/TestPlan.java` | 修改 | 添加requirementNumber字段 |
| `test-track/backend/pom.xml` | 修改 | 添加RocketMQ依赖 |

### 前端文件

| 文件路径 | 修改类型 | 说明 |
|----------|----------|------|
| `test-track/frontend/src/views/requirement-pool/` | 新增目录 | 需求池模块 |
| `test-track/frontend/src/views/requirement-pool/list.vue` | 新增 | 需求池列表页 |
| `test-track/frontend/src/views/requirement-pool/components/` | 新增目录 | 需求池模块组件（不创建单独的CreatePlanDialog） |
| `test-track/frontend/src/api/requirement-pool.js` | 新增 | 需求池API接口 |
| `test-track/frontend/src/router/index.js` | 修改 | 添加需求池路由 |

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

### 高级搜索组件配置（components）

**前端配置文件**：`test-track/frontend/src/components/search/search-components.js`

```javascript
import { OPERATORS } from './search-operators';

export const REQUIREMENT_POOL_LIST = [
  // 需求编号：文本输入，模糊匹配
  {
    key: "dmpNum",
    name: 'MsTableSearchInput',
    label: '需求编号',
    operator: {
      value: OPERATORS.LIKE.value,
      options: [OPERATORS.LIKE, OPERATORS.NOT_LIKE]
    }
  },
  // 需求名称：文本输入，模糊匹配
  {
    key: "requirementName",
    name: 'MsTableSearchInput',
    label: '需求名称',
    operator: {
      value: OPERATORS.LIKE.value,
      options: [OPERATORS.LIKE, OPERATORS.NOT_LIKE]
    }
  },
  // 需求状态：下拉选择，多选
  {
    key: "poolStatus",
    name: 'MsTableSearchSelect',
    label: '需求状态',
    operator: {
      options: [OPERATORS.IN, OPERATORS.NOT_IN]
    },
    options: [
      { label: '未创建', value: 'PENDING' },
      { label: '已创建', value: 'CREATED' },
      { label: '已取消', value: 'CANCELLED' }
    ],
    props: {
      multiple: true
    }
  },
  // 所属系统：下拉选择，多选（动态选项）
  {
    key: "systemName",
    name: 'MsTableSearchSelect',
    label: '所属系统',
    operator: {
      options: [OPERATORS.IN, OPERATORS.NOT_IN]
    },
    options: [],  // 从后端获取所有 systemName 去重列表
    props: {
      multiple: true
    }
  },
  // 需求大类：下拉选择，多选（动态选项）
  {
    key: "reqFatherClass",
    name: 'MsTableSearchSelect',
    label: '需求大类',
    operator: {
      options: [OPERATORS.IN, OPERATORS.NOT_IN]
    },
    options: [],  // 从后端获取所有 reqFatherClass 去重列表
    props: {
      multiple: true
    }
  },
  // 需求提出时间：日期时间选择器
  {
    key: "createTime",
    name: 'MsTableSearchDateTimePicker',
    label: '需求提出时间',
    operator: {
      options: [OPERATORS.BETWEEN, OPERATORS.GT, OPERATORS.LT]
    }
  },
  // 预计上线时间：日期时间选择器
  {
    key: "upTime",
    name: 'MsTableSearchDateTimePicker',
    label: '预计上线时间',
    operator: {
      options: [OPERATORS.BETWEEN, OPERATORS.GT, OPERATORS.LT]
    }
  },
  // 需求负责人：文本输入，模糊匹配
  {
    key: "reqManagerName",
    name: 'MsTableSearchInput',
    label: '需求负责人',
    operator: {
      value: OPERATORS.LIKE.value,
      options: [OPERATORS.LIKE, OPERATORS.NOT_LIKE]
    }
  }
];
```

### 前端使用示例（RequirementPoolList.vue）

**注意**：`ms-table-header` 是 `framework/sdk-parent` 提供的公共组件，不需要重新开发。

```vue
<template>
  <el-card class="table-card" v-loading="cardLoading">
    <template v-slot:header>
      <!-- 使用公共组件 MsTableHeader -->
      <ms-table-header
        :condition.sync="page.condition"
        @search="search"
      />
    </template>

    <ms-table
      :data="page.data"
      :total="page.total"
      :condition.sync="page.condition"
      @handlePageChange="handlePageChange"
      @order="search"
      :screen-height="screenHeight"
    >
      <!-- 表格列定义 -->
    </ms-table>
  </el-card>
</template>

<script>
import { REQUIREMENT_POOL_LIST } from '@/components/search/search-components';
import { getPageInfo } from '@/utils/pageInfo';

export default {
  data() {
    return {
      page: getPageInfo({
        components: REQUIREMENT_POOL_LIST,  // 初始化高级搜索组件配置
        custom: false
      }),
      tableHeaderKey: 'REQUIREMENT_POOL_LIST'
    };
  },
  methods: {
    search() {
      this.page.currentPage = 1;
      this.getRequirementList();
    },
    handlePageChange() {
      this.getRequirementList();
    },
    getRequirementList() {
      // 恢复上次排序
      this.page.condition.orders = getLastTableSortField(this.tableHeaderKey);

      getRequirementPoolList(
        this.page.currentPage,
        this.page.pageSize,
        this.page.condition
      ).then(response => {
        this.page.data = response.data.listObject;
        this.page.total = response.data.itemCount;
      });
    }
  }
};
</script>
```

### 后端 Mapper XML 处理（RequirementPoolMapper.xml）

```xml
<!-- 需求池列表查询 - 高级搜索 -->
<select id="selectByPageCondition" resultMap="BaseResultMap">
  SELECT * FROM requirement_pool
  <where>
    <!-- 处理 filters（精确匹配） -->
    <if test="request.filters != null and request.filters.size() > 0">
      <foreach collection="request.filters.entrySet()" index="key" item="values">
        <if test="values != null and values.size() > 0">
          <!-- 需求状态 -->
          <if test="key == 'poolStatus'">
            AND pool_status IN
            <foreach collection="values" item="value" open="(" close=")" separator=",">
              #{value}
            </foreach>
          </if>
          <!-- 所属系统 -->
          <if test="key == 'systemName'">
            AND system_name IN
            <foreach collection="values" item="value" open="(" close=")" separator=",">
              #{value}
            </foreach>
          </if>
          <!-- 需求大类 -->
          <if test="key == 'reqFatherClass'">
            AND req_father_class IN
            <foreach collection="values" item="value" open="(" close=")" separator=",">
              #{value}
            </foreach>
          </if>
        </if>
      </foreach>
    </if>

    <!-- 处理 combine（模糊匹配） -->
    <if test="request.combine != null">
      <foreach collection="request.combine.entrySet()" index="key" item="condition">
        <!-- 需求编号 -->
        <if test="key == 'dmpNum'">
          <if test="condition.operator == 'like'">
            AND dmp_num LIKE CONCAT('%', #{condition.value}, '%')
          </if>
          <if test="condition.operator == 'not like'">
            AND dmp_num NOT LIKE CONCAT('%', #{condition.value}, '%')
          </if>
        </if>
        <!-- 需求名称 -->
        <if test="key == 'requirementName'">
          <if test="condition.operator == 'like'">
            AND requirement_name LIKE CONCAT('%', #{condition.value}, '%')
          </if>
          <if test="condition.operator == 'not like'">
            AND requirement_name NOT LIKE CONCAT('%', #{condition.value}, '%')
          </if>
        </if>
        <!-- 需求提出时间 -->
        <if test="key == 'createTime'">
          <if test="condition.operator == 'between'">
            AND create_time BETWEEN #{condition.value[0]} AND #{condition.value[1]}
          </if>
          <if test="condition.operator == 'gt'">
            AND create_time > #{condition.value}
          </if>
          <if test="condition.operator == 'lt'">
            AND create_time < #{condition.value}
          </if>
        </if>
        <!-- 预计上线时间 -->
        <if test="key == 'upTime'">
          <if test="condition.operator == 'between'">
            AND up_time BETWEEN #{condition.value[0]} AND #{condition.value[1]}
          </if>
          <if test="condition.operator == 'gt'">
            AND up_time > #{condition.value}
          </if>
          <if test="condition.operator == 'lt'">
            AND up_time < #{condition.value}
          </if>
        </if>
        <!-- 需求负责人 -->
        <if test="key == 'reqManagerName'">
          <if test="condition.operator == 'like'">
            AND req_manager_name LIKE CONCAT('%', #{condition.value}, '%')
          </if>
          <if test="condition.operator == 'not like'">
            AND req_manager_name NOT LIKE CONCAT('%', #{condition.value}, '%')
          </if>
        </if>
      </foreach>
    </if>
  </where>

  <!-- 处理 orders（排序） -->
  <if test="request.orders != null and request.orders.size() > 0">
    ORDER BY
    <foreach collection="request.orders" item="order" separator=",">
      ${order.name} ${order.type}
    </foreach>
  </if>
  <if test="request.orders == null or request.orders.size() == 0">
    ORDER BY create_time DESC
  </if>
</select>
```

### 后端 Request 类（RequirementPoolRequest.java）

```java
package io.metersphere.track.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RequirementPoolRequest {
    private Map<String, List<String>> filters;
    private Map<String, CombineCondition> combine;
    private List<OrderCondition> orders;
    private List<ComponentConfig> components;

    @Data
    public static class CombineCondition {
        private String operator;
        private Object value;
    }

    @Data
    public static class OrderCondition {
        private String name;
        private String type;
    }

    @Data
    public static class ComponentConfig {
        private String key;
        private String name;
        private String label;
        // ... 其他组件配置字段
    }
}
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

---

## RocketMQ 配置

```properties
# RocketMQ 配置
rocketmq.name-server=10.12.105.254:9876
rocketmq.producer.group=metersphere-producer-group
rocketmq.consumer.group=metersphere-consumer-group

# Topic 配置
rocketmq.topic.requirement-sync=topic-requirement-to-metersphere
rocketmq.topic.status-callback=topic-metersphere-to-requirement
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
rocketmq.producer.group=metersphere-producer-group
rocketmq.consumer.group=metersphere-consumer-group

# Topic 配置
rocketmq.topic.requirement-sync=topic-requirement-to-metersphere
rocketmq.topic.status-callback=topic-metersphere-to-requirement
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

文件路径：`test-track/backend/src/main/resources/db/migration/2.10.23.ddl/V19__requirement_pool.sql`

```sql
SET SESSION innodb_lock_wait_timeout = 7200;

-- 创建需求池表
CREATE TABLE IF NOT EXISTS `requirement_pool` (
  `id` VARCHAR(50) NOT NULL COMMENT '主键',
  `dmp_num` VARCHAR(100) NOT NULL COMMENT '需求编号（唯一）',
  `requirement_name` VARCHAR(500) NOT NULL COMMENT '需求名称',
  `pool_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '需求池状态：PENDING/CREATED/CANCELLED',
  `parent_wfinst_code` VARCHAR(100) DEFAULT NULL COMMENT '主流程编码',
  `req_father_class` VARCHAR(100) DEFAULT NULL COMMENT '需求大类',
  `req_son_class` VARCHAR(100) DEFAULT NULL COMMENT '需求子类',
  `system_name` VARCHAR(200) DEFAULT NULL COMMENT '所属系统',
  `act_name` VARCHAR(100) DEFAULT NULL COMMENT '当前环节',
  `req_manager_name` VARCHAR(100) DEFAULT NULL COMMENT '需求负责人',
  `assignee_name` VARCHAR(100) DEFAULT NULL COMMENT '当前处理人',
  `created_dept` VARCHAR(200) DEFAULT NULL COMMENT '需求申请部门',
  `create_user` VARCHAR(100) DEFAULT NULL COMMENT '需求申请人',
  `dept_name` VARCHAR(200) DEFAULT NULL COMMENT '需求负责人处室',
  `start_user_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `create_time` BIGINT DEFAULT NULL COMMENT '需求提出时间（时间戳）',
  `up_time` BIGINT DEFAULT NULL COMMENT '预计上线时间（时间戳）',
  `linked_plan_id` VARCHAR(50) DEFAULT NULL COMMENT '关联的测试计划ID',
  `last_sync_time` BIGINT NOT NULL COMMENT '最后同步时间（时间戳）',
  `event_time` BIGINT DEFAULT NULL COMMENT '消息事件时间（时间戳）',
  `trace_id` VARCHAR(100) DEFAULT NULL COMMENT '追踪ID',
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

### 实体类和Mapper实现

**步骤2：创建实体类**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/domain/RequirementPool.java`

```java
package io.metersphere.track.domain;

import lombok.Data;
import java.io.Serializable;

@Data
public class RequirementPool implements Serializable {
    private String id;
    private String dmpNum;
    private String requirementName;
    private String poolStatus;
    private String parentWfinstCode;
    private String reqFatherClass;
    private String reqSonClass;
    private String systemName;
    private String actName;
    private String reqManagerName;
    private String assigneeName;
    private String createdDept;
    private String createUser;
    private String deptName;
    private String startUserName;
    private Long createTime;
    private Long upTime;
    private String linkedPlanId;
    private Long lastSyncTime;
    private Long eventTime;
    private String traceId;
    private Long createdAt;
    private Long updatedAt;
}
```

**步骤3：创建Mapper接口**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/mapper/RequirementPoolMapper.java`

```java
package io.metersphere.track.mapper;

import io.metersphere.track.domain.RequirementPool;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface RequirementPoolMapper {
    int insert(RequirementPool record);
    int updateByPrimaryKey(RequirementPool record);
    RequirementPool selectByPrimaryKey(String id);
    RequirementPool selectByDmpNum(String dmpNum);
    List<RequirementPool> selectByCondition(@Param("condition") RequirementPoolQueryCondition condition);
    int updateStatusByDmpNum(@Param("dmpNum") String dmpNum,
                             @Param("status") String status,
                             @Param("linkedPlanId") String linkedPlanId,
                             @Param("updatedAt") Long updatedAt);
}
```

**步骤4：创建Mapper XML**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/mapper/RequirementPoolMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="io.metersphere.track.mapper.RequirementPoolMapper">
    <resultMap id="BaseResultMap" type="io.metersphere.track.domain.RequirementPool">
        <id column="id" property="id"/>
        <result column="dmp_num" property="dmpNum"/>
        <result column="requirement_name" property="requirementName"/>
        <result column="pool_status" property="poolStatus"/>
        <result column="parent_wfinst_code" property="parentWfinstCode"/>
        <result column="req_father_class" property="reqFatherClass"/>
        <result column="req_son_class" property="reqSonClass"/>
        <result column="system_name" property="systemName"/>
        <result column="act_name" property="actName"/>
        <result column="req_manager_name" property="reqManagerName"/>
        <result column="assignee_name" property="assigneeName"/>
        <result column="created_dept" property="createdDept"/>
        <result column="create_user" property="createUser"/>
        <result column="dept_name" property="deptName"/>
        <result column="start_user_name" property="startUserName"/>
        <result column="create_time" property="createTime"/>
        <result column="up_time" property="upTime"/>
        <result column="linked_plan_id" property="linkedPlanId"/>
        <result column="last_sync_time" property="lastSyncTime"/>
        <result column="event_time" property="eventTime"/>
        <result column="trace_id" property="traceId"/>
        <result column="created_at" property="createdAt"/>
        <result column="updated_at" property="updatedAt"/>
    </resultMap>

    <insert id="insert">
        INSERT INTO requirement_pool (
            id, dmp_num, requirement_name, pool_status, parent_wfinst_code,
            req_father_class, req_son_class, system_name, act_name, req_manager_name,
            assignee_name, created_dept, create_user, dept_name, start_user_name,
            create_time, up_time, last_sync_time, event_time, trace_id,
            created_at, updated_at
        ) VALUES (
            #{id}, #{dmpNum}, #{requirementName}, #{poolStatus}, #{parentWfinstCode},
            #{reqFatherClass}, #{reqSonClass}, #{systemName}, #{actName}, #{reqManagerName},
            #{assigneeName}, #{createdDept}, #{createUser}, #{deptName}, #{startUserName},
            #{createTime}, #{upTime}, #{lastSyncTime}, #{eventTime}, #{traceId},
            #{createdAt}, #{updatedAt}
        )
    </insert>

    <select id="selectByDmpNum" resultMap="BaseResultMap">
        SELECT * FROM requirement_pool WHERE dmp_num = #{dmpNum}
    </select>

    <update id="updateStatusByDmpNum">
        UPDATE requirement_pool
        SET pool_status = #{status}, linked_plan_id = #{linkedPlanId}, updated_at = #{updatedAt}
        WHERE dmp_num = #{dmpNum}
    </update>
</mapper>
```

### RocketMQ消费者实现

**步骤5：创建消息DTO**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/dto/RequirementSyncMessage.java`

```java
package io.metersphere.track.dto;

import lombok.Data;

@Data
public class RequirementSyncMessage {
    private String dmpNum;
    private String name1;
    private String operationType; // CREATED/UPDATED/CANCELLED
    private String reqManagerName;
    private String actName;
    private Long createTime;
    private String parentWfinstCode;
    private String reqFatherClass;
    private String reqSonClass;
    private String systemName;
    private Long upTime;
    private String assigneeName;
    private String createdept;
    private String createUser1;
    private String deptName;
    private String startUserName;
    private Long eventTime;
    private String traceId;
}
```

**步骤6：创建消费者**

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

**步骤7：创建同步服务**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/service/RequirementSyncService.java`

```java
package io.metersphere.track.service;

import io.metersphere.track.domain.RequirementPool;
import io.metersphere.track.dto.RequirementSyncMessage;
import io.metersphere.track.mapper.RequirementPoolMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.UUID;

@Service
public class RequirementSyncService {
    @Resource
    private RequirementPoolMapper requirementPoolMapper;

    @Transactional
    public void handleSyncMessage(RequirementSyncMessage message) {
        // 幂等检查
        RequirementPool existing = requirementPoolMapper.selectByDmpNum(message.getDmpNum());
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
        pool.setId(existing != null ? existing.getId() : UUID.randomUUID().toString());
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
        pool.setCreateUser(message.getCreateUser1());
        pool.setDeptName(message.getDeptName());
        pool.setStartUserName(message.getStartUserName());
        pool.setEventTime(message.getEventTime());
        pool.setTraceId(message.getTraceId());
        pool.setLastSyncTime(System.currentTimeMillis());
        pool.setUpdatedAt(System.currentTimeMillis());

        if (existing == null) {
            pool.setPoolStatus("PENDING");
            pool.setCreatedAt(System.currentTimeMillis());
            requirementPoolMapper.insert(pool);
        } else {
            requirementPoolMapper.updateByPrimaryKey(pool);
        }
    }

    private void cancelRequirement(String dmpNum) {
        requirementPoolMapper.updateStatusByDmpNum(dmpNum, "CANCELLED", null, System.currentTimeMillis());
    }
}
```

**步骤8：创建需求池查询服务**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/dto/RequirementPoolQueryCondition.java`

```java
package io.metersphere.track.dto;

import lombok.Data;

@Data
public class RequirementPoolQueryCondition {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String dmpNum;
    private String requirementName;
    private String poolStatus;
    private String systemName;
    private String reqFatherClass;
    private Long createTimeStart;
    private Long createTimeEnd;
}
```

文件路径：`test-track/backend/src/main/java/io/metersphere/track/service/RequirementPoolService.java`

```java
package io.metersphere.track.service;

import io.metersphere.track.domain.RequirementPool;
import io.metersphere.track.dto.RequirementPoolQueryCondition;
import io.metersphere.track.mapper.RequirementPoolMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;

@Service
public class RequirementPoolService {
    @Resource
    private RequirementPoolMapper requirementPoolMapper;

    public List<RequirementPool> list(RequirementPoolQueryCondition condition) {
        return requirementPoolMapper.selectByCondition(condition);
    }

    public RequirementPool getByDmpNum(String dmpNum) {
        return requirementPoolMapper.selectByDmpNum(dmpNum);
    }

    public void updateStatus(String dmpNum, String status, String linkedPlanId) {
        requirementPoolMapper.updateStatusByDmpNum(dmpNum, status, linkedPlanId, System.currentTimeMillis());
    }
}
```

**步骤9：创建控制器**

文件路径：`test-track/backend/src/main/java/io/metersphere/track/controller/RequirementPoolController.java`

```java
package io.metersphere.track.controller;

import io.metersphere.track.domain.RequirementPool;
import io.metersphere.track.dto.RequirementPoolQueryCondition;
import io.metersphere.track.service.RequirementPoolService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/requirement-pool")
public class RequirementPoolController {
    @Resource
    private RequirementPoolService requirementPoolService;

    @PostMapping("/list")
    public List<RequirementPool> list(@RequestBody RequirementPoolQueryCondition condition) {
        return requirementPoolService.list(condition);
    }

    @GetMapping("/{dmpNum}")
    public RequirementPool getDetail(@PathVariable String dmpNum) {
        return requirementPoolService.getByDmpNum(dmpNum);
    }
}
```

### 测试计划扩展实现

**步骤10：扩展测试计划服务**

在 `TestPlanService` 中添加方法：

```java
// 从需求池创建测试计划
@Transactional
public TestPlan createFromRequirement(CreateFromRequirementRequest request) {
    // 检查需求是否存在
    RequirementPool requirement = requirementPoolService.getByDmpNum(request.getDmpNum());
    if (requirement == null) {
        throw new RuntimeException("需求不存在");
    }
    if ("CANCELLED".equals(requirement.getPoolStatus())) {
        throw new RuntimeException("需求已取消");
    }
    if (requirement.getLinkedPlanId() != null) {
        throw new RuntimeException("该需求已创建测试计划");
    }

    // 创建测试计划
    TestPlan plan = new TestPlan();
    plan.setId(UUID.randomUUID().toString());
    plan.setName(requirement.getRequirementName()); // 使用需求名称，不可编辑
    plan.setProjectId(request.getProjectId());
    plan.setPrincipal(request.getPrincipalId());
    plan.setRequirementNumber(request.getDmpNum()); // 设置需求编号（非null）
    plan.setStatus("PENDING");
    plan.setCreateTime(System.currentTimeMillis());
    testPlanMapper.insert(plan);

    // 更新需求池状态
    requirementPoolService.updateStatus(request.getDmpNum(), "CREATED", plan.getId());

    return plan;
}

// 直接创建测试计划
@Transactional
public TestPlan create(CreateTestPlanRequest request) {
    TestPlan plan = new TestPlan();
    plan.setId(UUID.randomUUID().toString());
    plan.setName(request.getName()); // 用户输入的名称
    plan.setProjectId(request.getProjectId());
    plan.setPrincipal(request.getPrincipalId());
    plan.setRequirementNumber(null); // 设置为null，表示非需求关联
    plan.setStatus("PENDING");
    plan.setCreateTime(System.currentTimeMillis());
    testPlanMapper.insert(plan);

    return plan;
}
```

### 状态回传实现

**步骤11：创建回传消息DTO**

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
    private String planShareUrl;         // 测试计划报告分享链接：/track/share-plan-report?shareId={shareId}
    private Long syncTime;               // 同步时间
    private String traceId;              // 追踪ID
}
```

**步骤12：创建回传生产者**

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

**步骤13：在测试计划状态更新时触发回传**

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
