# 设计文档：测试平台流程改造

## 概述

本文档对测试平台流程改造功能的详细设计进行全面准确的描述。核心目标是在 MeterSphere 测试跟踪模块中新增一个以**需求规格书驱动**的“需求测试流程”模块。

本次改造不应把新流程硬塞进现有测试计划或测试用例页面，而应采用**新模块为主、少量集成点改造**的方式落地。现有 `test_plan`、`test_case`、`issues`、`test_plan_report`、附件、通知等能力可以被复用，但新的流程主数据应独立建模。

设计边界是**测试平台侧实现**：负责接收全研发同步来的系统名称、需求编号和需求规格书信息；负责采集测试计划、测试准备、用例评审、测试执行、测试报告数据；负责沉淀流程、质量、人员工作量指标；负责将关键节点回传全研发。全研发侧计划审批、业务优先级排序、邮件系统内部实现不在本模块内重做。

**文档版本**：V1.2
**修订日期**：2026年6月2日

---

## 核心设计目标

1. **需求驱动**：流程主线围绕一条带编号需求规格书展开
2. **交互收敛**：主界面为系统名称树 + 需求列表，点击需求后按状态打开弹窗/抽屉
3. **V1 闭环**：优先打通计划、准备、评审、待测、执行、报告的测试平台侧闭环
4. **状态可对齐**：测试平台内部状态与全研发流程节点保持可映射，支持等待测试、待测、计划审批等外部状态记录
5. **指标可沉淀**：提前沉淀流程维度、测试质量维度、人员工作量维度所需字段
6. **表格数据可入库**：V1 优先使用 Univer/Luckysheet 等平台内类 Excel 表格组件，支持增删改查、合并单元格/分组展示，并将表格数据结构化入库
7. **共享文档可入口化**：Office/WPS/金山/飞书在 V1 作为材料入口或辅助查看，V2 再做深度在线编辑和回写
8. **低侵入**：不重写现有 test-track 核心模块，只改必要入口和集成点
9. **可扩展**：为 V2 在线嵌入编辑、思维图深度编辑、自动通知和全量同步预留字段

---

## 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3.x | 业务逻辑实现 |
| 数据持久化 | MyBatis + MySQL | 流程主数据、指标数据存储 |
| 消息队列 | RocketMQ 4.9.2 | 与全研发双向同步 |
| 前端框架 | Vue 2.7 + qiankun/micro-app 适配现有模块 | 以当前分支实际微前端架构为准 |
| UI 组件库 | Element UI 2.15 | 表格、树、弹窗、抽屉 |
| 类 Excel 表格 | Univer / Luckysheet | V1 核心用例编辑器，支持增删改查、合并单元格/分组展示、结构化入库 |
| Excel 导入导出 | EasyExcel / Apache POI | 后端解析 xlsx、处理合并单元格、导入导出 |
| 文件存储 | MinIO/AttachmentService | V1 上传 Office/WPS 文件和附件 |
| 共享文档入口 | 外部链接/iframe/新窗口打开 | V1 支持金山、飞书、WPS、Office 等链接型接入，但不作为数据库唯一数据源 |
| 文档深度嵌入 | Office/WPS Web SDK、OnlyOffice、WOPI | V2 在线编辑、协同、保存回写 |
| 状态管理 | Pinia | 前端状态 |

---

## 架构设计

### 总体架构

```
┌─────────────────┐         RocketMQ          ┌──────────────────────────┐
│   全研发平台    │ ──────────────────────────>│  MeterSphere test-track  │
│                 │  需求/系统/状态同步       │                          │
│  计划审批       │                            │  ┌────────────────────┐  │
│  邮件通知       │                            │  │ 需求测试流程模块    │  │
│  上线评估       │                            │  │ requirement-flow    │  │
│                 │                            │  └────────────────────┘  │
│                 │  关键节点/指标回传        │  ┌────────────────────┐  │
│                 │ <──────────────────────────│  │ 用例/缺陷/附件/通知 │  │
└─────────────────┘         RocketMQ          └──────────────────────────┘
```

### 交互架构

```
┌────────────────────────────────────────────────────────────────────┐
│ 需求测试流程                                                        │
├───────────────┬────────────────────────────────────────────────────┤
│ 系统名称树     │ 需求列表                                            │
│ - CMS-        │ 需求编号 | 名称 | 当前状态 | 计划时间 | 指标 | 报告 | 操作 │
│ - CMS2.0-     │                                                    │
│ - SMGS-       │ 点击某条需求，根据状态打开对应弹窗/抽屉               │
└───────────────┴────────────────────────────────────────────────────┘

状态弹窗/抽屉：
- 测试计划弹窗
- 测试准备/用例评审弹窗
- 待测信息抽屉
- 测试执行弹窗
- 测试报告弹窗
- 操作回溯抽屉
```

### 职责边界

| 范围 | 测试平台侧处理 | 不在本模块重做 |
|------|----------------|----------------|
| 需求起点 | 接收带编号需求、系统名称、需求规格书信息 | 全研发需求创建页面 |
| 计划审批 | 记录/展示审批相关状态和计划上线时间 | 全研发审批规则、业务优先级排序 |
| 邮件通知 | 调用现有通知能力或接收提测通知状态 | 邮件系统内部实现 |
| 平台内类 Excel 表格 | 作为用例准备、评审、执行的主要编辑器，保存结构化 JSON 和业务表数据 | 不依赖原生 Office/WPS 作为唯一数据源 |
| 共享文档 | 保存链接、文件、打开/预览/嵌入入口 | V1 不做协同编辑保存回写 |
| 缺陷 | 复用现有缺陷能力并记录指标字段 | 重构缺陷管理模块 |
| 报告 | 生成新流程报告和指标数据 | 重写现有测试计划报告 |

### 模块命名建议

建议使用 `requirement/flow` 包。为了和已有 `requirement/pool` 保持语义一致，推荐：

```
io.metersphere.requirement.flow
```

本文档后续统一以 `requirement/flow` 描述。

---

## 后端模块设计

### 包结构

```
test-track/backend/src/main/java/io/metersphere/requirement/flow/
  controller/
    RequirementFlowController.java
    RequirementFlowPlanController.java
    RequirementFlowPreparationController.java
    RequirementFlowReviewController.java
    RequirementFlowExecutionController.java
    RequirementFlowReportController.java
    RequirementFlowMetricController.java
    RequirementFlowSystemController.java
  service/
    RequirementFlowService.java
    RequirementFlowPlanService.java
    RequirementFlowPreparationService.java
    RequirementFlowReviewService.java
    RequirementFlowExecutionService.java
    RequirementFlowReportService.java
    RequirementFlowMetricService.java
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
    RequirementFlowMetricDTO.java
    RequirementFlowSystemNodeDTO.java
  request/
    QueryRequirementFlowRequest.java
    SaveFlowPlanRequest.java
    SaveFlowPreparationRequest.java
    StartFlowPreparationRequest.java
    SubmitFlowReviewRequest.java
    StartFlowExecutionRequest.java
    ExecuteFlowCaseRequest.java
    CompleteFlowExecutionRequest.java
    QueryFlowMetricRequest.java
  producer/
    RequirementFlowStatusProducer.java
  enums/
    RequirementFlowStage.java
    RequirementFlowStatus.java
    FlowExecutionResult.java
    SharedDocumentType.java
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
| 附件/文件服务 | 复用上传、预览、下载能力 |
| 缺陷服务 | 复用缺陷创建和关联能力 |
| 通知服务 | 复用手工邮件/站内通知能力 |

---

## 前端模块设计

### 目录结构

```
test-track/frontend/src/business/requirement-flow/
  RequirementFlow.vue
  components/
    SystemTreePanel.vue
    RequirementFlowTable.vue
    FlowStageTag.vue
    FlowProgress.vue
    OperationHistoryDrawer.vue
    SharedDocumentEntry.vue
    dialogs/
      FlowPlanDialog.vue
      FlowPreparationDialog.vue
      FlowReviewDialog.vue
      FlowWaitingTestDrawer.vue
      FlowExecutionDialog.vue
      FlowReportDialog.vue
    plan/
      PlanTaskTable.vue
      PersonnelAllocation.vue
    preparation/
      PreparationMaterial.vue
      CaseSpreadsheetEditor.vue             # Univer/Luckysheet 类 Excel 用例编辑器
      OfficeFilePanel.vue
      MindMapEntry.vue
    review/
      ReviewCaseList.vue
      ReviewLockBanner.vue
    execution/
      ExecutionCaseTable.vue
      CaseExecutionSpreadsheet.vue          # 基于锁定快照的执行表格
      DefectEditor.vue
      ExecutionMetricPanel.vue
    report/
      ReportSummary.vue
      ReportCharts.vue
      QualityMetricPanel.vue
      WorkloadMetricPanel.vue
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
| current_stage | VARCHAR(32) | PLAN / PREPARATION / WAITING_TEST / EXECUTION / DONE |
| current_status | VARCHAR(32) | TEST_PLAN / PLAN_APPROVAL / WAITING_TEST / PREPARATION / WAITING_DELIVERY / TESTING / DONE |
| external_status | VARCHAR(64) | 全研发侧状态 |
| test_leader | VARCHAR(64) | 测试组长 |
| creator | VARCHAR(64) | 创建人 |
| requirement_created_time | BIGINT | 需求规格书上传/创建时间 |
| dev_plan_submit_time | BIGINT | 开发计划提交时间，来自全研发 |
| plan_approval_time | BIGINT | 计划审批完成时间，来自全研发 |
| planned_online_time | BIGINT | 计划上线时间，来自全研发或计算结果 |
| actual_online_time | BIGINT | 实际上线时间，来自全研发 |
| planned_prep_start_time | BIGINT | 计划准备开始 |
| planned_prep_end_time | BIGINT | 计划准备完成 |
| planned_exec_start_time | BIGINT | 计划执行开始/计划提测时间 |
| planned_exec_end_time | BIGINT | 计划执行完成/测试报告计划完成 |
| actual_prep_start_time | BIGINT | 实际准备开始 |
| actual_prep_end_time | BIGINT | 实际准备结束/评审完成 |
| actual_exec_start_time | BIGINT | 实际执行开始 |
| actual_exec_end_time | BIGINT | 实际执行结束 |
| planned_prep_work_days | DECIMAL(6,2) | 计划准备人天 |
| planned_exec_work_days | DECIMAL(6,2) | 计划执行人天 |
| actual_work_days | DECIMAL(6,2) | 实际测试人天 |
| review_case_count | INT | 评审用例数 |
| actual_case_count | INT | 实际执行用例数 |
| invalid_case_count | INT | 失效用例数 |
| added_case_count | INT | 新增用例数 |
| actual_defect_count | INT | 实际缺陷数 |
| retest_count | INT | 复测数 |
| report_id | VARCHAR(32) | 测试报告ID |
| report_url | VARCHAR(500) | 测试报告链接 |
| pause_reason | VARCHAR(500) | 流程关闭/暂停说明 |
| sync_status | VARCHAR(32) | UNSYNCED/SYNCING/SYNCED/FAILED |
| create_time | BIGINT | 创建时间 |
| update_time | BIGINT | 更新时间 |

### 2. test_workflow_plan（计划阶段数据）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | -> test_workflow |
| phase | VARCHAR(32) | PREPARATION / EXECUTION |
| task_type | VARCHAR(32) | REQUIREMENT_ANALYSIS / CASE_WRITING / CASE_REVIEW / CASE_EXECUTION / DEFECT_MANAGEMENT / TEST_REPORT |
| planned_start_time | BIGINT | 计划开始 |
| planned_end_time | BIGINT | 计划结束 |
| work_days | DECIMAL(6,2) | 人天 |
| create_time | BIGINT | |
| update_time | BIGINT | |

### 3. test_workflow_personnel（人员分配）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| plan_task_id | VARCHAR(32) | 关联计划子任务 |
| user_id | VARCHAR(64) | 人员ID |
| user_name | VARCHAR(128) | 人员名称 |
| role | VARCHAR(32) | LEADER / TESTER / REVIEWER / DEVELOPER |
| work_days | DECIMAL(6,2) | 分配人天 |
| create_time | BIGINT | |

### 4. test_workflow_preparation（准备材料和共享文档）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| analysis_content | LONGTEXT | 需求分析内容 |
| case_file_id | VARCHAR(64) | 用例文件ID |
| case_file_name | VARCHAR(255) | 用例文件名 |
| case_file_url | VARCHAR(500) | 用例文件链接 |
| shared_doc_url | VARCHAR(1000) | 共享文档链接 |
| shared_doc_type | VARCHAR(32) | WPS / OFFICE / KINGSOFT / FEISHU / OTHER |
| shared_doc_open_mode | VARCHAR(32) | LINK / IFRAME / NEW_WINDOW |
| mindmap_file_id | VARCHAR(64) | 思维图文件ID |
| deviation_remark | TEXT | 计划与实际偏差备注 |
| create_time | BIGINT | |
| update_time | BIGINT | |

### 5. test_workflow_review（用例评审）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| actual_start_time | BIGINT | 点击开始准备工作时记录 |
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

### 6. test_workflow_case（用例结构化数据）

该表承载平台内类 Excel 表格解析后的业务用例数据。数据库是最终数据真相来源，表格组件负责编辑体验。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| row_index | INT | 表格行号 |
| requirement_no | VARCHAR(64) | 需求编号 |
| split_category | VARCHAR(255) | 需求拆分分类，合并单元格展开后的值 |
| manual_category | VARCHAR(255) | 人工用例分类，合并单元格展开后的值 |
| case_name | VARCHAR(500) | 用例 |
| expected_result | TEXT | 预期 |
| steps | LONGTEXT | 步骤 |
| writer | VARCHAR(64) | 用例制定人员 |
| case_status | VARCHAR(32) | DRAFT / PASSED / DEPRECATED / INVALID / LOCKED |
| merge_group_key | VARCHAR(64) | 合并单元格/分组标识 |
| source_type | VARCHAR(32) | SPREADSHEET / IMPORTED_XLSX / TEST_CASE |
| create_time | BIGINT | |
| update_time | BIGINT | |

### 7. test_workflow_sheet_snapshot（表格快照）

保存类 Excel 表格组件的原始数据结构，便于恢复合并单元格、列宽、分组等编辑状态。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| stage | VARCHAR(32) | PREPARATION / REVIEW / EXECUTION |
| sheet_data | LONGTEXT | Univer/Luckysheet 原始 JSON |
| merge_config | LONGTEXT | 合并单元格配置 JSON |
| version | INT | 快照版本 |
| is_locked | TINYINT(1) | 是否锁定 |
| create_time | BIGINT | |
| update_time | BIGINT | |

### 8. test_workflow_execution（执行记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| workflow_case_id | VARCHAR(32) | 关联 test_workflow_case |
| case_id | VARCHAR(50) | 可关联现有 test_case |
| case_name | VARCHAR(255) | 用例名称快照 |
| is_added_case | TINYINT(1) | 是否执行阶段新增用例 |
| executor | VARCHAR(64) | 执行人员 |
| exec_result | VARCHAR(32) | PASS / FAIL / BLOCK / SKIP / INVALID |
| defect_id | VARCHAR(50) | 关联 issues |
| defect_title | VARCHAR(255) | 缺陷标题 |
| severity | VARCHAR(32) | 严重级别 |
| defect_reason | VARCHAR(500) | 缺陷原因 |
| dev_handler | VARCHAR(64) | 开发处理人 |
| retest_count | INT | 复测次数 |
| is_fixed | TINYINT(1) | 是否修复 |
| defect_operator | VARCHAR(64) | 缺陷操作人员 |
| defect_fix_user | VARCHAR(64) | 缺陷修复人员 |
| remark | TEXT | 备注 |
| exec_time | BIGINT | 执行时间 |
| create_time | BIGINT | |
| update_time | BIGINT | |

### 9. test_workflow_report（流程报告）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) UNIQUE | |
| total_cases | INT | 总用例数 |
| review_cases | INT | 评审用例数 |
| actual_cases | INT | 实际执行用例数 |
| added_cases | INT | 新增用例数 |
| invalid_cases | INT | 失效用例数 |
| passed_cases | INT | 通过数 |
| failed_cases | INT | 失败数 |
| blocked_cases | INT | 阻塞数 |
| skipped_cases | INT | 跳过数 |
| pass_rate | DECIMAL(6,2) | 通过率 |
| case_health_rate | DECIMAL(6,2) | 用例整体健康度，实际执行用例数 / 评审用例数 |
| invalid_rate | DECIMAL(6,2) | 失效率，失效用例数 / 评审用例数 |
| defect_total | INT | 缺陷总数 |
| retest_total | INT | 复测总数 |
| defect_density | DECIMAL(8,4) | 用例缺陷密度，缺陷数 / 实际执行用例数 |
| defect_by_severity | TEXT | JSON：按严重级别分布 |
| report_content | LONGTEXT | 报告全文 JSON |
| generated_at | BIGINT | 生成时间 |
| is_sent | TINYINT(1) | 是否发送邮件 |

### 10. test_workflow_system_node（系统名称树）

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

### 11. test_workflow_status_log（状态审计）

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
| external_event | VARCHAR(64) | 外部事件，如 PLAN_APPROVED / DEV_DELIVERED |
| create_time | BIGINT | |

### 12. test_workflow_metric_snapshot（指标快照）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(32) PK | |
| workflow_id | VARCHAR(32) FK | |
| metric_type | VARCHAR(32) | FLOW / QUALITY / WORKLOAD |
| metric_content | LONGTEXT | JSON 快照 |
| generated_at | BIGINT | 生成时间 |

---

## 状态机设计

### 状态映射

| 测试平台状态 | 全研发/流程含义 | 是否测试平台可操作 |
|--------------|----------------|--------------------|
| TEST_PLAN | 测试计划中 | 是 |
| PLAN_APPROVAL | 计划时间审批 | 否，仅记录/展示 |
| WAITING_TEST | 等待测试 | 是，可点击开始准备 |
| PREPARATION | 测试准备中 | 是 |
| WAITING_DELIVERY | 待测 | 是，可点击开始执行 |
| TESTING | 测试中 | 是 |
| DONE | 测试完成 | 是，查看报告 |

### 状态流转

```
┌────────────┐
│ 测试计划中 │ ← 需求规格书创建时初始状态
└─────┬──────┘
      │ 提交测试计划
┌─────▼──────┐
│ 计划时间审批 │ ← 全研发侧节点，测试平台记录/展示
└─────┬──────┘
      │ 审批完成且尚未开始准备
┌─────▼──────┐
│ 等待测试   │
└─────┬──────┘
      │ 点击开始准备工作
┌─────▼──────┐
│ 测试准备中 │
└─────┬──────┘
      │ 提交用例评审并锁定
┌─────▼──────┐
│ 待测       │ ← 等待开发提测或等待点击开始执行
└─────┬──────┘
      │ 点击开始测试执行
┌─────▼──────┐
│ 测试中     │
└─────┬──────┘
      │ 执行完成并生成报告
┌─────▼──────┐
│ 测试完成   │
└────────────┘
```

特殊规则：

1. 如果测试人员在计划审批完成前已经点击开始准备，则审批完成后可跳过 `WAITING_TEST`。
2. 如果用例评审完成时开发已完成提测，可直接从 `PREPARATION` 进入 `TESTING` 或展示为可开始执行状态。
3. 回退仅允许测试组长操作，并写入 `test_workflow_status_log`。
4. 每次状态变更都要记录操作人、时间、来源事件和备注。

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
| POST | `/requirement-flow/status/rollback` | 测试组长回退状态 |

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
| POST | `/requirement-flow/plan/submit` | 提交计划并回传计划准备/执行时间和人天 |

### 测试准备/评审弹窗

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/requirement-flow/preparation/{workflowId}` | 获取准备材料 |
| POST | `/requirement-flow/preparation/start` | 开始准备，记录实际准备开始时间 |
| POST | `/requirement-flow/preparation/save` | 保存准备材料和共享文档入口 |
| POST | `/requirement-flow/preparation/sheet/save` | 保存平台内类 Excel 表格 JSON，并同步结构化用例数据入库 |
| POST | `/requirement-flow/preparation/sheet/import` | 导入 xlsx，解析合并单元格并生成表格数据和用例数据 |
| GET | `/requirement-flow/preparation/sheet/export/{workflowId}` | 导出用例表格 xlsx |
| POST | `/requirement-flow/preparation/upload-file` | 上传用例/脑图文件 |
| GET | `/requirement-flow/review/{workflowId}` | 获取评审数据 |
| POST | `/requirement-flow/review/save` | 保存评审信息 |
| POST | `/requirement-flow/review/submit` | 提交评审，锁定用例材料，进入待测 |
| POST | `/requirement-flow/review/reject` | 驳回，返回准备 |

### 测试执行弹窗

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/requirement-flow/execution/{workflowId}/cases` | 获取执行用例列表 |
| POST | `/requirement-flow/execution/start` | 开始执行，记录实际执行开始时间 |
| POST | `/requirement-flow/execution/execute` | 保存单条执行结果 |
| POST | `/requirement-flow/execution/complete` | 完成执行，生成报告和指标 |

### 测试报告和指标

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/requirement-flow/report/{workflowId}` | 获取报告 |
| POST | `/requirement-flow/report/regenerate` | 重新生成报告 |
| POST | `/requirement-flow/report/send-email` | 手工发送邮件 |
| POST | `/requirement-flow/metrics/flow` | 查询流程维度指标 |
| POST | `/requirement-flow/metrics/quality` | 查询测试质量维度指标 |
| POST | `/requirement-flow/metrics/workload` | 查询人员工作量维度指标 |

---

## MQ 同步设计

复用需求池 `RequirementSyncConsumer` / `RequirementCallbackProducer` 的实现模式。

### 入站：全研发 -> MeterSphere

需求消息进入现有需求池后，同时创建或更新 `test_workflow` 主记录。

```
RequirementSyncConsumer
  -> RequirementPoolService.handleSyncMessage()
  -> RequirementFlowSyncService.syncFromRequirementPool()
  -> insert/update test_workflow
```

入站关键字段：

1. 系统名称、需求编号前缀、需求编号、需求名称
2. 需求规格书上传/创建时间
3. 开发计划提交时间、计划提测时间
4. 计划审批完成状态、计划上线时间
5. 开发完成/提测状态
6. 实际上线时间

### 出站：MeterSphere -> 全研发

新增 `RequirementFlowStatusProducer`，发送流程关键节点状态。

触发时机：

1. 测试计划提交：同步计划准备/执行时间、人天
2. 开始准备工作：同步实际准备开始时间
3. 用例评审提交：同步实际准备结束时间、评审用例数
4. 开始测试执行：同步实际执行开始时间
5. 测试执行完成：同步实际执行结束时间、实际用例数、缺陷数、实际测试人天、报告链接

消息体示例：

```json
{
  "dmpNum": "CMS-2024-001",
  "stage": "TESTING",
  "plannedPrepStartTime": 1710234567000,
  "plannedPrepEndTime": 1710234568000,
  "plannedExecStartTime": 1710234569000,
  "plannedExecEndTime": 1710234570000,
  "plannedPrepWorkDays": 3.5,
  "plannedExecWorkDays": 4.0,
  "actualPrepStartTime": 1710234571000,
  "actualPrepEndTime": 1710234572000,
  "actualExecStartTime": 1710234573000,
  "actualExecEndTime": 1710234574000,
  "actualWorkDays": 7.0,
  "reviewCaseCount": 120,
  "actualCaseCount": 118,
  "invalidCaseCount": 2,
  "addedCaseCount": 5,
  "actualDefectCount": 5,
  "retestCount": 3,
  "reportUrl": "/#/track/requirement-flow/list?resourceId=xxx",
  "syncTime": 1710234575000,
  "traceId": "trace-001"
}
```

---

## 表格与共享文档设计

### V1：平台内类 Excel 表格

V1 将平台内类 Excel 表格作为用例数据的主要编辑器，数据库作为最终数据真相来源。推荐优先评估 Univer，其次 Luckysheet；两者都比原生 Office/WPS 更适合把增删改查、合并单元格和执行结果稳定落库。

V1 实现：

1. 前端使用 Univer/Luckysheet 类组件实现用例表格编辑体验
2. 支持新增行、删除行、修改单元格、复制粘贴、批量编辑
3. 支持合并单元格或分组展示，用于需求拆分分类、人工用例分类等层级信息
4. 保存时同时写入 `test_workflow_sheet_snapshot.sheet_data` 和结构化 `test_workflow_case` 数据
5. 合并单元格作为展示和分组信息保存，后端同时将分类值展开到每条用例记录
6. 评审提交后锁定表格快照和结构化用例数据，执行阶段基于锁定快照操作
7. Excel 导入导出使用 EasyExcel 或 Apache POI，导入时解析合并单元格并转换为结构化数据

### V1：共享文档入口

V1 不做 Office/WPS/金山/飞书深度在线编辑和保存回写，原因是该能力涉及文档服务、授权、跨域、协同编辑、保存事件处理和内容解析，风险高。

共享文档在 V1 的定位是材料入口或辅助查看，不作为数据库唯一数据源。

V1 实现：

1. 上传 Office/WPS 文件，复用 AttachmentService / MinIO
2. 支持维护外部共享文档链接，如金山、飞书、WPS、Office 在线表格
3. 保存文件 ID、文件名、访问链接、共享文档类型、打开方式到 `test_workflow_preparation`
4. 在准备、评审、执行阶段展示打开/预览/嵌入入口
5. 评审提交后锁定共享文档入口和评审用例材料，不允许修改删除
6. 为 V2 预留 `shared_doc_url`、`shared_doc_type`、`shared_doc_open_mode` 等字段

### V2：在线嵌入编辑和回写

后续可选方案：

| 方案 | 说明 |
|------|------|
| WPS Web Office SDK | 如果单位已有 WPS 服务，优先使用 |
| 金山/飞书开放平台 | 如果共享表格来自对应平台，按其开放接口接入 |
| OnlyOffice Document Server | 自建文档服务，支持 Office 在线编辑 |
| Office Online/WOPI | 企业已有 Microsoft 体系时考虑 |

---

## 报表与指标设计

### 流程维度

| 指标 | 口径 |
|------|------|
| 开发计划时效 | 开发计划提交时间 - 需求规格上传时间，仅统计工作日 |
| 测试计划时效 | 测试计划提交时间 - 需求规格上传时间，仅统计工作日 |
| 计划测试天数 | 计划测试准备完成时间 - 子需求创建时间 + 计划测试完成时间 - 计划提测时间 |
| 实际测试天数 | 实际用例评审完成时间 - 子需求创建时间 + 实际测试完成时间 - 实际提测时间 |
| 人天工作量差异 | 计划人天 - 实际人天 |
| 上线预期偏离 | 计划上线时间 - 实际上线时间 |

### 测试质量维度

| 指标 | 口径 |
|------|------|
| 实际执行用例数 | 计划用例数 - 失效用例数 + 新增用例数 |
| 用例整体健康度 | 实际执行用例数 / 评审用例数 |
| 失效率 | 失效用例数 / 评审用例数 |
| 缺陷总数 | 缺陷数 + 复测数 |
| 用例缺陷密度 | 缺陷数 / 实际执行用例数 |

### 人员工作量维度

| 指标 | 口径 |
|------|------|
| 涉及需求数量 | 人员参与的流程需求数量 |
| 测试完成数量 | 人员参与并完成测试的需求数量 |
| 用例编写数量 | 评审阶段记录的编写人员关联用例数量 |
| 用例执行数量 | 执行阶段记录的执行人员关联用例数量 |
| 发现缺陷数量 | 缺陷操作人员关联缺陷数量 |
| 修复缺陷数量 | 缺陷修复人员关联缺陷数量，含复测 |
| 日工作量 | (用例编写数量 + 用例执行数量 + 修复缺陷数量) / 出勤天数 |

---

## 权限设计

| 权限 | 测试组长 | 测试工程师 |
|------|----------|------------|
| 查看需求流程 | 是 | 是 |
| 维护系统树 | 是 | 否 |
| 保存测试计划 | 是 | 否 |
| 提交测试计划 | 是 | 否 |
| 开始准备工作 | 是 | 是 |
| 填写测试准备 | 是 | 是 |
| 提交评审 | 是 | 是 |
| 开始测试执行 | 是 | 是 |
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
| Univer / Luckysheet | 平台内类 Excel 表格编辑器候选 |
| EasyExcel / Apache POI | xlsx 导入导出和合并单元格解析 |
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
| 附件/预览相关组件 | 复用共享文档入口能力 |
| 通知相关服务 | 手工发送报告通知 |

---

## 风险与取舍

| 风险 | 等级 | 处理方式 |
|------|------|----------|
| 类 Excel 表格组件选型和集成复杂 | 中 | 优先评估 Univer，其次 Luckysheet；先实现核心编辑、保存、导入导出 |
| Office/WPS 在线嵌入编辑复杂 | 高 | V1 做共享文档入口，V2 单独专项做深度嵌入和回写 |
| 共享文档来自多个平台 | 高 | V1 统一按链接/iframe/新窗口入口处理，不解析文档内容 |
| 全研发接口文档不完整 | 高 | 本端先按 DTO 固化关键字段，联调时调整 |
| 报表指标口径后续变化 | 中 | 指标采用快照和可重算字段，避免写死单一口径 |
| 新流程与现有用例/计划口径冲突 | 中 | 新流程独立建表，现有模块只复用能力不承载主流程 |
| 用例锁定并发 | 中 | 锁定状态 + 事务校验，必要时加悲观锁 |
| 单人开发周期压力 | 中 | 拆阶段交付，优先流程闭环和关键指标 |
