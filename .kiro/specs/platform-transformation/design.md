# 设计文档：测试平台流程改造

## 概述

本文档对测试平台流程改造功能的详细设计进行全面准确的描述。核心目标是在 MeterSphere 测试跟踪模块中新增一个以**需求规格书驱动**的“需求测试流程”模块。

本次改造不应把新流程硬塞进现有测试计划或测试用例页面，而应采用**新模块为主、少量集成点改造**的方式落地。现有 `test_plan`、`test_case`、`issues`、`test_plan_report` 等能力可以被复用，但新的流程主数据应独立建模。

**文档版本**：V1.1
**修订日期**：2026年6月1日

---

## 核心设计目标

1. **需求驱动**：流程主线围绕一条需求规格书展开
2. **交互收敛**：主界面为系统名称树 + 需求列表，点击需求后按状态打开弹窗/抽屉
3. **V1 闭环**：优先打通计划、准备、评审、执行、报告的最小闭环
4. **复用现有能力**：复用用例、缺陷、附件、报告、MQ、通知等已有能力
5. **低侵入**：不重写现有 test-track 核心模块，只改必要入口和集成点
6. **可扩展**：为 V2 Office/WPS 在线嵌入编辑、思维图深度编辑和全量同步预留字段

---

## 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3.x | 业务逻辑实现 |
| 数据持久化 | MyBatis + MySQL | 数据存储 |
| 消息队列 | RocketMQ 4.9.2 | 与全研发双向同步 |
| 前端框架 | Vue 2.7 + qiankun | 微前端 |
| UI 组件库 | Element UI 2.15 | 表格、树、弹窗、抽屉 |
| 文件存储 | MinIO/AttachmentService | V1 上传 Office/WPS 文件 |
| 文档嵌入 | Office/WPS Web SDK | V2 深度嵌入编辑 |
| 状态管理 | Pinia | 前端状态 |

---

## 架构设计

### 总体架构

```
┌─────────────────┐         RocketMQ          ┌──────────────────────────┐
│   全研发平台    │ ──────────────────────────>│  MeterSphere test-track  │
│                 │  topic-requirement-to-ms  │                          │
│                 │                            │  ┌────────────────────┐  │
│                 │                            │  │ 需求测试流程模块    │  │
│                 │                            │  │ requirement-flow    │  │
│                 │                            │  └────────────────────┘  │
│                 │                            │  ┌────────────────────┐  │
│                 │  topic-ms-to-requirement   │  │ 现有用例/缺陷/报告 │  │
│                 │ <──────────────────────────│  │ 复用，不硬改       │  │
└─────────────────┘         RocketMQ          └──────────────────────────┘
```

### 交互架构

```
┌───────────────────────────────────────────────────────────────┐
│ 需求测试流程                                                   │
├───────────────┬───────────────────────────────────────────────┤
│ 系统名称树     │ 需求列表                                       │
│ - CMS-        │ 需求编号 | 名称 | 当前状态 | 计划时间 | 报告 | 操作 │
│ - CMS2.0-     │                                               │
│ - SMGS-       │ 点击某条需求，根据状态打开对应弹窗/抽屉          │
└───────────────┴───────────────────────────────────────────────┘

状态弹窗/抽屉：
- 测试计划弹窗
- 测试准备弹窗
- 用例评审弹窗
- 测试执行弹窗
- 测试报告弹窗
```

### 模块命名建议

建议使用 `requirement/flow` 或 `workflow` 包。为了和已有 `requirement/pool` 保持语义一致，推荐：

```
io.metersphere.requirement.flow
```

如果希望简短，也可使用：

```
io.metersphere.workflow
```

本文档后续统一以 `requirement/flow` 描述。

---

## 后端模块设计

### 包结构

```
test-track/backend/src/main/java/io/metersphere/requirement/flow/
  controller/
    RequirementFlowController.java           # 主流程列表、详情、状态入口
    RequirementFlowPlanController.java       # 测试计划弹窗
    RequirementFlowPreparationController.java# 测试准备弹窗
    RequirementFlowReviewController.java     # 用例评审弹窗
    RequirementFlowExecutionController.java  # 测试执行弹窗
    RequirementFlowReportController.java     # 测试报告弹窗
    RequirementFlowSystemController.java     # 系统名称树
  service/
    RequirementFlowService.java
    RequirementFlowPlanService.java
    RequirementFlowPreparationService.java
    RequirementFlowReviewService.java
    RequirementFlowExecutionService.java
    RequirementFlowReportService.java
    RequirementFlowSystemService.java
    RequirementFlowSyncService.java
  dto/
    RequirementFlowDTO.java
    RequirementFlowDetailDTO.java
    RequirementFlowPlanDTO.java
    RequirementFlowPreparationDTO.java
    RequirementFlowReviewDTO.java
    RequirementFlowExecutionDTO.java
    RequirementFlowReportDTO.java
    RequirementFlowSystemNodeDTO.java
  request/
    QueryRequirementFlowRequest.java
    SaveFlowPlanRequest.java
    SaveFlowPreparationRequest.java
    StartFlowReviewRequest.java
    SubmitFlowReviewRequest.java
    ExecuteFlowCaseRequest.java
    CompleteFlowExecutionRequest.java
  producer/
    RequirementFlowStatusProducer.java       # 状态同步至全研发
  enums/
    RequirementFlowStage.java                # TEST_PLAN / PREPARATION / REVIEW / EXECUTION / DONE
    RequirementFlowStatus.java
    FlowExecutionResult.java                 # PASS / FAIL / BLOCK / SKIP / INVALID
  state/
    RequirementFlowStateMachine.java
```

### 需要修改的现有集成点

| 文件/模块 | 修改原因 |
|----------|----------|
| `test-track/frontend/src/router/modules/track.js` | 新增需求测试流程路由 |
| 菜单/权限配置 | 新增菜单和权限点 |
| `RequirementSyncConsumer` | 需求入池时同步创建或更新流程主记录 |
| `RequirementPoolService` | 需求池与新流程建立关联 |

---

## 前端模块设计

### 目录结构

```
test-track/frontend/src/business/requirement-flow/
  RequirementFlow.vue                         # 主页面：系统树 + 需求列表
  components/
    SystemTreePanel.vue                       # 系统名称树
    RequirementFlowTable.vue                  # 需求流程列表
    FlowStageTag.vue                          # 当前状态标签
    FlowProgress.vue                          # 阶段进度展示
    dialogs/
      FlowPlanDialog.vue                      # 测试计划弹窗/抽屉
      FlowPreparationDialog.vue               # 测试准备弹窗/抽屉
      FlowReviewDialog.vue                    # 用例评审弹窗/抽屉
      FlowExecutionDialog.vue                 # 测试执行弹窗/抽屉
      FlowReportDialog.vue                    # 测试报告弹窗/抽屉
    plan/
      PlanTaskTable.vue                       # 计划子任务：时间、人天、人员
      PersonnelAllocation.vue                 # 人员分配
    preparation/
      PreparationMaterial.vue                 # 需求分析、用例文件
      OfficeFilePanel.vue                     # V1 文件上传/链接，V2 嵌入
      MindMapEntry.vue                        # 思维图入口
    review/
      ReviewCaseList.vue                      # 评审用例列表
      ReviewLockBanner.vue                    # 锁定状态提示
    execution/
      ExecutionCaseTable.vue                  # 执行用例表格
      DefectEditor.vue                        # 缺陷录入/关联
    report/
      ReportSummary.vue                       # 报告摘要
      ReportCharts.vue                        # 报告图表
  api/requirement-flow.js
```

### 路由设计

V1 只需要一个主页面路由，弹窗/抽屉在页面内部根据状态打开。

```javascript
{
  path: 'requirement-flow/list',
  name: 'requirementFlow',
  component: () => import('@/business/requirement-flow/RequirementFlow.vue')
}
```

---

## 数据模型

### 表命名原则

统一使用 `test_workflow_*` 前缀，表示这是测试工作流主数据，不与现有 `test_plan`、`test_case` 主表混淆。

### 1. test_workflow（主流程表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | UUID |
| project_id | VARCHAR(50) | 项目ID |
| requirement_pool_id | VARCHAR(32) | 关联需求池记录 |
| dmp_num | VARCHAR(64) UNIQUE | 需求编号 |
| requirement_name | VARCHAR(255) | 需求名称 |
| system_name | VARCHAR(255) | 所属系统 |
| system_prefix | VARCHAR(20) | 编号前缀，如 CMS- |
| current_stage | VARCHAR(32) | TEST_PLAN/PREPARATION/REVIEW/EXECUTION/DONE |
| current_status | VARCHAR(32) | 阶段内状态 |
| test_leader | VARCHAR(64) | 测试组长 |
| creator | VARCHAR(64) | 创建人 |
| planned_prep_start_time | BIGINT | 计划准备开始 |
| planned_prep_end_time | BIGINT | 计划准备结束 |
| planned_exec_start_time | BIGINT | 计划执行开始 |
| planned_exec_end_time | BIGINT | 计划执行结束 |
| actual_prep_start_time | BIGINT | 实际准备开始 |
| actual_prep_end_time | BIGINT | 实际准备结束 |
| actual_exec_start_time | BIGINT | 实际执行开始 |
| actual_exec_end_time | BIGINT | 实际执行结束 |
| review_case_count | INT | 评审用例数 |
| actual_case_count | INT | 实际用例数 |
| actual_defect_count | INT | 实际缺陷数 |
| report_id | VARCHAR(32) | 测试报告ID |
| report_url | VARCHAR(500) | 测试报告链接 |
| sync_status | VARCHAR(32) | UNSYNCED/SYNCING/SYNCED/FAILED |
| create_time | BIGINT | 创建时间 |
| update_time | BIGINT | 更新时间 |

### 2. test_workflow_plan（计划阶段数据）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | → test_workflow |
| phase | VARCHAR(32) | PREPARATION / EXECUTION |
| task_type | VARCHAR(32) | REQUIREMENT_ANALYSIS / CASE_WRITING / CASE_REVIEW / CASE_EXECUTION / DEFECT_MANAGEMENT / TEST_REPORT |
| planned_start_time | BIGINT | 计划开始 |
| planned_end_time | BIGINT | 计划结束 |
| work_days | DECIMAL(5,1) | 人天 |
| create_time | BIGINT | |
| update_time | BIGINT | |

### 3. test_workflow_personnel（人员分配）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| plan_task_id | VARCHAR(32) | 关联计划子任务 |
| user_id | VARCHAR(64) | 人员ID |
| role | VARCHAR(32) | LEADER / TESTER / REVIEWER |
| work_days | DECIMAL(5,1) | 分配人天 |
| create_time | BIGINT | |

### 4. test_workflow_preparation（准备材料）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| analysis_content | LONGTEXT | 需求分析内容 |
| case_file_id | VARCHAR(64) | 用例文件ID |
| case_file_name | VARCHAR(255) | 用例文件名 |
| case_file_url | VARCHAR(500) | 用例文件链接 |
| mindmap_file_id | VARCHAR(64) | 思维图文件ID |
| deviation_remark | TEXT | 计划与实际偏差备注 |
| create_time | BIGINT | |
| update_time | BIGINT | |

### 5. test_workflow_review（用例评审）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| actual_start_time | BIGINT | 点击开始测试时记录 |
| actual_end_time | BIGINT | 评审提交时记录 |
| review_case_count | INT | 评审用例数 |
| writer_ids | TEXT | JSON：用例编写人员 |
| reviewer_ids | TEXT | JSON：评审人员 |
| review_result | VARCHAR(32) | PASSED / REJECTED |
| case_status_snapshot | LONGTEXT | 用例状态快照 JSON |
| is_locked | TINYINT(1) | 是否锁定 |
| deviation_remark | TEXT | 偏差备注 |
| create_time | BIGINT | |
| update_time | BIGINT | |

### 6. test_workflow_execution（执行记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| case_id | VARCHAR(50) | 可关联现有 test_case |
| case_name | VARCHAR(255) | 用例名称快照 |
| executor | VARCHAR(64) | 执行人员 |
| exec_result | VARCHAR(32) | PASS/FAIL/BLOCK/SKIP/INVALID |
| defect_id | VARCHAR(50) | 关联 issues |
| defect_title | VARCHAR(255) | 缺陷标题 |
| severity | VARCHAR(32) | 严重级别 |
| defect_reason | VARCHAR(500) | 缺陷原因 |
| dev_handler | VARCHAR(64) | 开发处理人 |
| retest_count | INT | 复测次数 |
| remark | TEXT | 备注 |
| exec_time | BIGINT | 执行时间 |
| create_time | BIGINT | |
| update_time | BIGINT | |

### 7. test_workflow_report（流程报告）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) UNIQUE | |
| total_cases | INT | 总用例数 |
| passed_cases | INT | 通过数 |
| failed_cases | INT | 失败数 |
| blocked_cases | INT | 阻塞数 |
| skipped_cases | INT | 跳过数 |
| invalid_cases | INT | 失效数 |
| pass_rate | DECIMAL(5,2) | 通过率 |
| defect_total | INT | 缺陷总数 |
| defect_by_severity | TEXT | JSON：按严重级别分布 |
| report_content | LONGTEXT | 报告全文 JSON |
| generated_at | BIGINT | 生成时间 |
| is_sent | TINYINT(1) | 是否发送邮件 |

### 8. test_workflow_system_node（系统名称树）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| project_id | VARCHAR(50) | 项目ID |
| name | VARCHAR(255) | 系统名称 |
| prefix | VARCHAR(20) | 需求编号前缀 |
| parent_id | VARCHAR(32) | 父节点 |
| level | INT | 层级 |
| sort | INT | 排序 |
| create_time | BIGINT | |
| update_time | BIGINT | |

### 9. test_workflow_status_log（状态审计）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| from_stage | VARCHAR(32) | 原阶段 |
| to_stage | VARCHAR(32) | 目标阶段 |
| from_status | VARCHAR(32) | 原状态 |
| to_status | VARCHAR(32) | 目标状态 |
| operator | VARCHAR(64) | 操作人 |
| remark | TEXT | 备注 |
| create_time | BIGINT | |

---

## 状态机设计

```
                  ┌────────────┐
                  │ 测试计划中  │ ← 需求规格书创建时初始状态
                  └─────┬──────┘
                        │ 提交计划
                  ┌─────▼──────┐
                  │ 测试准备中  │
                  └─────┬──────┘
                        │ 提交准备材料
                  ┌─────▼──────┐
                  │ 用例评审中  │ ← 驳回可回到测试准备中
                  └─────┬──────┘
                        │ 评审通过并锁定
                  ┌─────▼──────┐
                  │ 测试执行中  │
                  └─────┬──────┘
                        │ 执行完成并生成报告
                  ┌─────▼──────┐
                  │ 测试完成    │
                  └────────────┘
```

状态机使用轻量级实现，不引入额外工作流引擎。

```java
public class RequirementFlowStateMachine {
    static Map<RequirementFlowStage, Set<RequirementFlowStage>> TRANSITIONS = Map.of(
        TEST_PLAN, Set.of(PREPARATION),
        PREPARATION, Set.of(REVIEW),
        REVIEW, Set.of(EXECUTION, PREPARATION),
        EXECUTION, Set.of(DONE)
    );

    public void validate(RequirementFlowStage from, RequirementFlowStage to) {
        if (!TRANSITIONS.get(from).contains(to)) {
            throw new InvalidTransitionException(from, to);
        }
    }
}
```

每次状态变更写入 `test_workflow_status_log`。

---

## API 设计

所有接口前缀 `/requirement-flow/`。

### 主流程

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/requirement-flow/list/{goPage}/{pageSize}` | 查询需求流程列表 |
| GET | `/requirement-flow/{workflowId}` | 查询流程详情 |
| GET | `/requirement-flow/by-dmp/{dmpNum}` | 按需求编号查询 |
| GET | `/requirement-flow/status-log/{workflowId}` | 查询操作历史 |

### 系统树

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/requirement-flow/system-tree/{projectId}` | 获取系统树 |
| POST | `/requirement-flow/system-tree/add` | 新增系统节点 |
| POST | `/requirement-flow/system-tree/update` | 修改系统节点 |
| DELETE | `/requirement-flow/system-tree/{nodeId}` | 删除系统节点 |

### 测试计划弹窗

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/requirement-flow/plan/{workflowId}` | 获取计划阶段数据 |
| POST | `/requirement-flow/plan/save` | 保存计划 |
| POST | `/requirement-flow/plan/submit` | 提交计划，进入测试准备 |

### 测试准备弹窗

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/requirement-flow/preparation/{workflowId}` | 获取准备材料 |
| POST | `/requirement-flow/preparation/save` | 保存准备材料 |
| POST | `/requirement-flow/preparation/upload-file` | 上传用例/脑图文件 |
| POST | `/requirement-flow/preparation/submit` | 提交准备，进入评审 |

### 用例评审弹窗

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/requirement-flow/review/{workflowId}` | 获取评审数据 |
| POST | `/requirement-flow/review/start` | 开始评审，记录实际准备开始时间 |
| POST | `/requirement-flow/review/save` | 保存评审信息 |
| POST | `/requirement-flow/review/submit` | 提交评审，锁定用例材料，进入执行 |
| POST | `/requirement-flow/review/reject` | 驳回，返回准备 |

### 测试执行弹窗

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/requirement-flow/execution/{workflowId}/cases` | 获取执行用例列表 |
| POST | `/requirement-flow/execution/start` | 开始执行，记录实际执行开始时间 |
| POST | `/requirement-flow/execution/execute` | 保存单条执行结果 |
| POST | `/requirement-flow/execution/complete` | 完成执行，生成报告 |

### 测试报告弹窗

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/requirement-flow/report/{workflowId}` | 获取报告 |
| POST | `/requirement-flow/report/regenerate` | 重新生成报告 |
| POST | `/requirement-flow/report/send-email` | 手工发送邮件 |

---

## MQ 同步设计

复用需求池 `RequirementSyncConsumer` / `RequirementCallbackProducer` 的实现模式。

### 入站：全研发 → MeterSphere

需求消息进入现有需求池后，同时创建或更新 `test_workflow` 主记录。

```
RequirementSyncConsumer
  → RequirementPoolService.handleSyncMessage()
  → RequirementFlowSyncService.syncFromRequirementPool()
  → insert/update test_workflow
```

### 出站：MeterSphere → 全研发

新增 `RequirementFlowStatusProducer`，发送流程关键节点状态。

触发时机：

1. 测试计划提交：同步计划准备/执行时间、人天
2. 用例评审提交：同步实际准备起止时间、评审用例数
3. 测试执行完成：同步实际执行起止时间、实际用例数、缺陷数、报告链接

消息体示例：

```json
{
  "dmpNum": "CMS-2024-001",
  "stage": "EXECUTION",
  "plannedPrepStartTime": 1710234567000,
  "plannedPrepEndTime": 1710234568000,
  "actualPrepStartTime": 1710234569000,
  "actualPrepEndTime": 1710234570000,
  "actualExecStartTime": 1710234571000,
  "actualExecEndTime": 1710234572000,
  "reviewCaseCount": 120,
  "actualCaseCount": 118,
  "actualDefectCount": 5,
  "reportUrl": "/#/track/requirement-flow/list?resourceId=xxx",
  "syncTime": 1710234573000,
  "traceId": "trace-001"
}
```

---

## Office/WPS 设计

### V1：文件上传 + 外部链接

V1 不做 Office/WPS 深度嵌入编辑，原因：该能力涉及文档服务器、授权、跨域、回写、多人编辑和保存事件处理，风险高。

V1 实现：

1. 上传 Office/WPS 文件，复用 AttachmentService / MinIO
2. 保存文件 ID、文件名和访问链接到 `test_workflow_preparation`
3. 页面展示文件链接和下载/预览入口
4. 为 V2 预留 `case_file_id`、`case_file_url`、`mindmap_file_id` 等字段

### V2：在线嵌入编辑

后续可选方案：

| 方案 | 说明 |
|------|------|
| WPS Web Office SDK | 如果单位已有 WPS 服务，优先使用 |
| OnlyOffice Document Server | 自建文档服务，支持 Office 在线编辑 |
| Office Online/WOPI | 企业已有 Microsoft 体系时考虑 |

---

## 报告设计

V1 生成独立流程报告 `test_workflow_report`，不强行复用现有 `test_plan_report` 表。原因是新报告统计口径来自 `test_workflow_execution`，与现有测试计划报告结构不同。

报告内容：

1. 用例总数、通过数、失败数、阻塞数、跳过数、失效数
2. 通过率
3. 缺陷总数
4. 缺陷等级分布
5. 执行人员分布
6. 需求编号、系统名称、实际起止时间
7. 报告链接

---

## 权限设计

| 权限 | 测试组长 | 测试工程师 |
|------|----------|------------|
| 查看需求流程 | 是 | 是 |
| 维护系统树 | 是 | 否 |
| 保存测试计划 | 是 | 否 |
| 提交测试计划 | 是 | 否 |
| 填写测试准备 | 是 | 是 |
| 开始/提交评审 | 是 | 是 |
| 执行用例 | 是 | 是 |
| 生成报告 | 是 | 是 |
| 发送邮件 | 是 | 否 |
| 回退状态 | 是 | 否 |

---

## 复用现有能力

| 现有组件 | 用途 |
|----------|------|
| `RequirementPoolService` | 需求入池、需求字段来源 |
| `RequirementSyncConsumer` | 全研发需求消息消费 |
| `RequirementCallbackProducer` 模式 | MQ 回传实现参考 |
| `AttachmentService` | Office/WPS 文件上传 |
| `IssuesService` | 缺陷创建和关联 |
| `TestCaseService` | 如果 V1 需要关联现有功能用例 |
| `@SendNotice` | 邮件通知 |

---

## 涉及文件清单

### 新建文件

| 文件路径 | 说明 |
|----------|------|
| `test-track/backend/src/main/java/io/metersphere/requirement/flow/...` | 需求测试流程后端包 |
| `test-track/backend/src/main/java/io/metersphere/base/domain/TestWorkflow*.java` | 新表实体 |
| `test-track/backend/src/main/java/io/metersphere/base/mapper/TestWorkflow*Mapper.java` | Mapper 接口 |
| `test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtTestWorkflowMapper.java` | 扩展查询 Mapper |
| `test-track/backend/src/main/resources/db/migration/2.10.27/ddl/V20__create_test_workflow_tables.sql` | 建表脚本 |
| `test-track/frontend/src/business/requirement-flow/...` | 需求测试流程前端目录 |
| `test-track/frontend/src/api/requirement-flow.js` | API 模块 |

### 修改文件

| 文件路径 | 说明 |
|----------|------|
| `test-track/frontend/src/router/modules/track.js` | 添加 `requirement-flow/list` 路由 |
| 菜单配置相关文件 | 添加需求测试流程菜单 |
| 权限常量相关文件 | 添加流程模块权限点 |
| `RequirementSyncConsumer` 或 `RequirementPoolService` | 需求同步时创建/更新 `test_workflow` |

---

## 风险与取舍

| 风险 | 等级 | 处理方式 |
|------|------|----------|
| Office/WPS 在线嵌入编辑复杂 | 高 | V1 降级为文件上传+外链，V2 单独专项 |
| 原始需求范围过大 | 高 | V1 做流程闭环，V2 增强自动化和在线编辑 |
| 全研发接口文档不完整 | 高 | 本端先按 DTO 固化关键字段，联调时调整 |
| 单人开发周期压力 | 中 | 拆阶段交付，优先流程闭环 |
| 新流程与现有用例/计划口径冲突 | 中 | 新流程独立建表，现有模块只复用能力不承载主流程 |
