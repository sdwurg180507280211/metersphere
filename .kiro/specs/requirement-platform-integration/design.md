# 设计文档：全流程平台对接

## 概述

本文档描述需求平台与 MeterSphere 测试平台对接功能的内部设计。V2.0 调整后，需求同步不再经过“需求池”中间环节，而是由 MeterSphere 消费 RocketMQ 消息后直接创建、更新或取消测试计划。

**文档版本**：V2.0

**修订日期**：2026年7月08日

## 变更说明

| 项目 | 说明 |
|------|------|
| 需求池 | 已废弃，不再作为主流程入口。代码和数据库表暂时保留，不继续扩展；后续对接测试工具时可考虑删除或复用 |
| 主流程 | 由“需求平台 -> 需求池 -> 手动创建测试计划”调整为“需求平台 -> 直接同步创建测试计划” |
| 新增字段 | 测试计划新增 `requirement_doc_url` 字段，用于存储需求平台传入的需求规格说明书链接 |
| 所属系统映射 | 一个需求会根据所属系统决定测试计划创建在哪个项目、模块节点和负责人下；映射表后续由业务方明确提供 |
| 审批流程 | 本版加入草案：测试人员提交继续测试/免测结论，需求平台审批后回传结果 |

## 核心设计目标

1. 可靠性：消息幂等处理，防止重复消费导致重复测试计划
2. 一致性：保证一个需求编号只关联一个测试计划
3. 可追溯：保留 `traceId`、`eventTime` 和关键处理日志
4. 可维护：废弃需求池主流程，减少测试人员手动操作
5. 可扩展：审批工作流先按独立状态草案设计，后续可根据业务确认调整

## 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 消息队列 | RocketMQ | 平台间异步通信 |
| 后端框架 | Spring Boot | 业务逻辑实现 |
| 数据持久化 | MyBatis + MySQL | 数据存储 |
| 前端框架 | Vue 2 | 测试计划列表展示需求字段 |

## 架构设计

### 总体架构

```text
┌─────────────────┐         RocketMQ          ┌──────────────────┐
│   需求平台       │ ────────────────────────> │  MeterSphere     │
│                 │ topic-requirement-to-ms   │                  │
│                 │                           │  同步创建测试计划 │
│                 │ topic-ms-to-requirement   │                  │
│                 │ <──────────────────────── │  状态回传         │
└─────────────────┘         RocketMQ          └──────────────────┘
```

### 核心流程

1. 需求平台发送需求同步消息，消息类型为 `CREATED`、`UPDATED`、`CANCELLED`
2. `RequirementSyncConsumer` 消费消息并解析为 `RequirementSyncMessage`
3. 同步服务基于 `dmpNum + eventTime` 做幂等和乱序判断
4. `CREATED` 消息直接创建测试计划
5. `UPDATED` 消息更新已关联测试计划的需求相关字段
6. `CANCELLED` 消息取消已关联测试计划
7. 测试计划状态变化后，回传状态和报告链接到需求平台
8. 测试人员提交测试评估结论，需求平台审批后回传审批结果

## 模块设计

### 需求同步模块

职责：

- 消费 RocketMQ 需求同步消息
- 校验必填字段
- 处理幂等和乱序消息
- 根据 `operationType` 分发到创建、更新、取消逻辑
- 记录关键日志和 `traceId`

核心类：

| 类 | 说明 |
|----|------|
| `RequirementSyncConsumer` | RocketMQ 消费者 |
| `RequirementSyncMessage` | 需求同步消息 DTO |
| `RequirementPoolService.handleSyncMessage` | 现有入口可复用，但内部逻辑需由写入需求池改为直接操作测试计划 |
| `TestPlanService` | 测试计划创建、更新、取消 |
| `ExtTestPlanMapper` | 按需求编号查询测试计划 |

### 测试计划同步创建模块

职责：

- `CREATED`：创建测试计划
- `UPDATED`：更新测试计划需求字段
- `CANCELLED`：取消测试计划
- 根据后续提供的所属系统映射表确定测试计划所属项目、模块节点和负责人
- 将需求平台传入的 `docUrl` 写入 `requirement_doc_url`

创建规则：

| 字段 | 取值 |
|------|------|
| `test_plan.requirement_number` | `RequirementSyncMessage.dmpNum` |
| `test_plan.name` | `RequirementSyncMessage.name1` |
| `test_plan.requirement_doc_url` | `RequirementSyncMessage.docUrl`，由需求平台传入 |
| `test_plan.status` | 默认沿用测试计划创建逻辑 |
| `test_plan.project_id` | 根据所属系统映射表确定 |
| `test_plan.node_id` / `node_path` | 根据所属系统映射表确定或创建 |
| `test_plan.principal` | 根据所属系统映射表确定负责人 |

更新规则：

| 场景 | 处理 |
|------|------|
| 已存在 `requirement_number = dmpNum` 的测试计划 | 更新计划名称、说明书链接、所属模块等需求相关字段 |
| 不存在关联测试计划且收到 UPDATED | 可按 CREATED 处理，直接创建测试计划 |
| 收到旧消息 | 丢弃，不更新 |

取消规则：

| 场景 | 处理 |
|------|------|
| 已存在关联测试计划 | 将测试计划状态设为 `Cancelled` |
| 不存在关联测试计划 | 记录日志并忽略 |
| 重复取消 | 幂等忽略 |

### 状态回传模块

职责：

- 监听测试计划状态变化
- 对关联需求的测试计划生成回传消息
- 发送到 RocketMQ 回传 Topic
- 回传失败时记录日志，主流程不阻塞

触发条件：

- `requirement_number != null`
- 测试计划状态达到需要回传的状态

V1 现状：

- 当前实现重点回传 `Completed` 状态
- 其他状态是否回传需和需求平台确认

### 测试评估与审批模块（草案）

职责：

- 记录测试人员的测试评估结论：继续测试 / 提交免测
- 将测试评估结论回传需求平台
- 接收需求平台审批结果：通过 / 驳回
- 根据审批结果控制测试计划关键字段是否可编辑
- 审批状态独立于测试计划执行状态

建议状态字段：

| 字段 | 说明 |
|------|------|
| `requirement_assessment_status` | 测试评估状态：PENDING / CONTINUE_TEST_SUBMITTED / EXEMPT_TEST_SUBMITTED |
| `requirement_approval_status` | 需求平台审批状态：NONE / SUBMITTED / APPROVED / REJECTED |

推荐流转：

```text
需求同步创建测试计划
  -> 待评估
      -> 继续测试：填写计划开始/结束时间，提交需求平台审批
          -> 已提交：关键字段禁止编辑
              -> 审批通过：进入正常测试执行流程
              -> 审批驳回：开放编辑，重新提交
      -> 提交免测：填写免测信息，提交需求平台审批
          -> 已提交：关键字段禁止编辑
              -> 审批通过：进入免测结束流程
              -> 审批驳回：开放编辑，重新提交
```

继续测试提交信息：

| 字段 | 说明 |
|------|------|
| `dmpNum` | 需求编号 |
| `planId` | 测试计划 ID |
| `plannedStartTime` | 计划开始时间 |
| `plannedEndTime` | 计划结束时间 |
| `principalUsers` | 测试负责人 |

免测提交信息：

| 字段 | 说明 |
|------|------|
| `systemName` | 系统名称 |
| `dmpNum` | 需求编号 |
| `requirementSummary` | 需求简述 |
| `relatedUsers` | 关联人员 |
| `exemptReason` | 免测说明 |

## 数据模型

### 测试计划表扩展（test_plan）

新增字段：

| 字段名 | 类型 | 长度 | 是否必填 | 说明 |
|--------|------|------|----------|------|
| `requirement_number` | VARCHAR | 64 | 否 | 需求编号。同步创建时为 `dmpNum`，手工创建时为 null |
| `requirement_doc_url` | VARCHAR | 1024 | 否 | 需求规格说明书链接。同步创建时来自需求平台传入的 `docUrl`，手工创建时为 null |
| `requirement_assessment_status` | VARCHAR | 64 | 否 | 测试评估状态 |
| `requirement_approval_status` | VARCHAR | 64 | 否 | 需求平台审批状态 |

索引：

| 索引 | 说明 |
|------|------|
| `uk_requirement_number` | 保证一个需求编号最多关联一个测试计划。MySQL 唯一索引允许多个 null，因此不影响手工创建测试计划 |

### 需求池表（requirement_pool）

`requirement_pool` 表和相关代码已废弃，不再作为主流程入口。

处理策略：

- 不删除现有代码和表，避免影响已有环境
- 不再新增需求池页面能力
- 不再从需求池手动创建测试计划
- 后续如确认不再对接测试工具，可评估删除

## RocketMQ 消息格式

### 需求同步消息（需求平台 -> MeterSphere）

Topic：`topic-requirement-to-metersphere`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `dmpNum` | String | 是 | 需求编号，唯一标识 |
| `name1` | String | 是 | 需求名称 |
| `operationType` | String | 是 | CREATED / UPDATED / CANCELLED |
| `eventTime` | Long | 是 | 消息事件时间，用于幂等和乱序判断 |
| `systemName` | String | 否 | 所属系统，用于匹配后续提供的“所属系统 -> 项目/模块/负责人”映射表 |
| `docUrl` | String | 否 | 需求规格说明书链接，由需求平台传入，写入 `requirement_doc_url` |
| `traceId` | String | 否 | 链路追踪 ID |

其他字段按既有消息结构保留，如 `reqManagerName`、`actName`、`createTime`、`parentWfinstCode`、`reqFatherClass`、`reqSonClass`、`upTime`、`assigneeName`、`createdept`、`createUser1`、`deptName`、`startUserName`。

### 状态回传消息（MeterSphere -> 需求平台）

Topic：`topic-metersphere-to-requirement`

| 字段 | 类型 | 说明 |
|------|------|------|
| `dmpNum` | String | 需求编号 |
| `planStatus` | String | 测试计划状态 |
| `plannedStartTime` | Long | 计划开始时间 |
| `plannedEndTime` | Long | 计划结束时间 |
| `actualStartTime` | Long | 实际开始时间 |
| `actualEndTime` | Long | 实际结束时间 |
| `principalUsers` | String | 测试负责人 |
| `planShareUrl` | String | 测试报告分享链接 |
| `syncTime` | Long | 同步时间 |
| `traceId` | String | 链路追踪 ID |

### 测试评估提交消息（MeterSphere -> 需求平台，草案）

Topic：`topic-metersphere-to-requirement`

| 字段 | 类型 | 说明 |
|------|------|------|
| `dmpNum` | String | 需求编号 |
| `planId` | String | 测试计划 ID |
| `assessmentResult` | String | CONTINUE_TEST / EXEMPT_TEST |
| `plannedStartTime` | Long | 计划开始时间，继续测试时填写 |
| `plannedEndTime` | Long | 计划结束时间，继续测试时填写 |
| `systemName` | String | 系统名称，免测时填写 |
| `requirementSummary` | String | 需求简述，免测时填写 |
| `relatedUsers` | String | 关联人员，免测时填写 |
| `exemptReason` | String | 免测说明，免测时填写 |
| `submitTime` | Long | 提交时间 |
| `traceId` | String | 链路追踪 ID |

### 审批结果回传消息（需求平台 -> MeterSphere，草案）

Topic：`topic-requirement-to-metersphere`

| 字段 | 类型 | 说明 |
|------|------|------|
| `dmpNum` | String | 需求编号 |
| `planId` | String | 测试计划 ID |
| `approvalStatus` | String | APPROVED / REJECTED |
| `approvalComment` | String | 审批意见 |
| `approvalTime` | Long | 审批时间 |
| `traceId` | String | 链路追踪 ID |

## API 设计

本期不新增需求池 API。

内部可复用或新增以下能力：

| 能力 | 说明 |
|------|------|
| 按需求编号查询测试计划 | `ExtTestPlanMapper.selectByRequirementNumber` |
| 同步创建测试计划 | 消费 `CREATED` 消息后调用测试计划创建逻辑 |
| 同步更新测试计划 | 消费 `UPDATED` 消息后更新需求相关字段 |
| 同步取消测试计划 | 消费 `CANCELLED` 消息后设置状态为 `Cancelled` |
| 提交测试评估 | 测试人员提交继续测试或免测结论，发送到需求平台 |
| 接收审批结果 | 需求平台审批后回传，通过或驳回 |

## 前端设计

测试计划列表新增字段展示：

| 字段 | 展示方式 |
|------|----------|
| `requirementNumber` | 展示关联需求编号 |
| `requirementDocUrl` | 展示为可点击链接，无值时展示 `--` |
| `requirementAssessmentStatus` | 展示测试评估状态 |
| `requirementApprovalStatus` | 展示需求平台审批状态 |

测试计划编辑规则：

- 待评估或审批驳回：允许编辑关键字段
- 已提交审批：禁止编辑关键字段
- 审批通过：继续测试进入正常执行，免测进入免测结束流程

不新增需求池页面能力。

## 幂等与异常处理

### 幂等规则

| 场景 | 处理 |
|------|------|
| 相同 `dmpNum` 且相同 `eventTime` | 重复消息，忽略 |
| 相同 `dmpNum` 且新消息 `eventTime` 小于已处理时间 | 乱序旧消息，忽略 |
| 相同 `dmpNum` 且新消息 `eventTime` 更大 | 正常处理 |

### 异常处理

| 异常 | 处理 |
|------|------|
| `dmpNum` 为空 | 记录错误并拒绝处理 |
| `operationType` 为空或未知 | 记录警告并忽略 |
| `CREATED` 时 `name1` 为空 | 记录错误并拒绝创建 |
| 所属系统映射缺失 | 记录错误并拒绝创建，等待补充映射表后重试或人工补偿 |
| 模块树创建失败 | 抛出异常，让 RocketMQ 重试 |
| 数据库唯一索引冲突 | 重新查询关联测试计划，按更新处理 |
| 审批结果找不到测试计划 | 记录错误并忽略或进入人工补偿 |
| 审批消息重复 | 幂等忽略 |

## 上线与回滚

上线内容：

- 数据库新增 `test_plan.requirement_doc_url`
- 数据库新增测试评估状态、审批状态字段（字段名以最终实现为准）
- 后端同步逻辑改造为直接创建测试计划
- 后端新增测试评估提交和审批结果回调处理
- 前端测试计划列表展示说明书链接
- 前端增加继续测试 / 提交免测操作入口
- 对接文档更新消息字段 `docUrl`

回滚策略：

- 代码回滚到上一版本
- `requirement_doc_url` 字段可保留，不影响旧逻辑
- 如同步逻辑异常，可临时关闭 RocketMQ 消费者配置，避免继续消费消息

## 本期不做

| 功能 | 说明 |
|------|------|
| 审批工作流最终字段 | 本文仅为草案，字段名、状态值、是否阻塞测试执行需业务确认 |
| 需求池主流程 | 已废弃，不作为本期入口 |
| 多测试计划拆分 | 一条需求仅关联一个主测试计划 |
| 配置化字段映射 | 本期字段映射固定在代码中 |
