# MeterSphere 接口自动化模块逻辑文档

## 1. 模块概述

接口自动化是 MeterSphere API 测试的核心模块之一，用于编排和执行多步骤的 API 测试场景。场景（Scenario）由一系列有序步骤组成，支持请求、控制器、前后置脚本、断言、提取等步骤类型，并可嵌套引用其他场景。

---

## 2. 页面结构

### 2.1 主页面：ApiAutomation.vue

入口文件：`api-test/frontend/src/business/automation/ApiAutomation.vue`

页面采用**左右布局 + Tab 标签页**的结构：

```
+------------------+------------------------------------------------------+
|                  |  [场景列表]  [回收站]  [场景A]  [场景B]  [+]         |
|   模块树         |------------------------------------------------------|
|   (左侧边栏)     |                                                      |
|                  |  当前 Tab 内容：                                      |
|   - 全部场景     |    - default Tab → 场景列表 (MsApiScenarioList)       |
|   - 未规划场景   |    - trash Tab  → 回收站列表                          |
|   - 自定义模块   |    - 动态 Tab   → 场景编辑页 (EditApiScenario)         |
|   - ...          |                                                      |
|                  |                                                      |
+------------------+------------------------------------------------------+
```

**核心数据流：**

- `moduleOptions` / `nodeTree`：模块树数据，从 `ApiAutomation` 传递到子组件
- `selectNodeIds`：当前选中的模块节点 ID，驱动场景列表过滤
- `tabs`：打开的 Tab 数组，每项格式为 `{ label, name, currentScenario }`
- `trashEnable`：是否显示回收站 Tab

### 2.2 模块树：MsApiScenarioModule

文件：`api-test/frontend/src/business/automation/scenario/ApiScenarioModule.vue`

- 支持增删改、拖拽排序
- API：`GET /api/automation/module/list/{projectId}`
- 拖拽排序：`POST /api/automation/module/drag`

### 2.3 场景列表：MsApiScenarioList

文件：`api-test/frontend/src/business/automation/scenario/ApiScenarioList.vue`

- 分页查询场景：`POST /api/automation/list/{page}/{size}`
- 支持批量操作：批量删除、批量编辑、批量复制、批量执行、批量更新环境
- 行操作：编辑、复制、删除（移至回收站）、执行、生成性能测试 JMX、定时任务设置

---

## 3. 场景编辑器：EditApiScenario.vue

文件：`api-test/frontend/src/business/automation/scenario/EditApiScenario.vue`

### 3.1 左侧面板：基础信息

| 字段 | 说明 |
|------|------|
| name | 场景名称，最长 100 字符 |
| apiScenarioModuleId | 所属模块 |
| status | 状态（进行中/已完成等） |
| principal | 负责人 |
| level | 优先级（P0/P1/P2/P3） |
| tags | 标签 |
| description | 描述 |
| customNum | 自定义 ID |

### 3.2 右侧面板：场景步骤编排

步骤以树形结构展示在右侧主区域，核心数据结构为 `scenarioDefinition` 数组，每个元素是一个步骤对象，包含 `type`、`hashTree`（子步骤）、`index` 等字段。

**步骤操作栏按钮（从 Menu.js 渲染）：**

| 按钮 | 类型 | 说明 |
|------|------|------|
| 提取参数 | Extract | 从响应中提取变量 |
| 后置脚本 | JSR223PostProcessor | 请求后执行的脚本 |
| 前置脚本 | JSR223PreProcessor | 请求前执行的脚本 |
| 后置SQL | JDBCPostProcessor | 请求后执行的 SQL |
| 前置SQL | JDBCPreProcessor | 请求前执行的 SQL |
| 自定义脚本 | JSR223Processor | 独立执行的自定义脚本 |
| 条件控制器 | IfController | 条件分支 |
| 循环控制器 | LoopController | 循环执行 |
| 事务控制器 | TransactionController | 事务聚合 |
| 等待控制器 | ConstantTimer | 固定延时 |
| 场景断言 | Assertions | 断言校验 |
| 自定义请求 | CustomizeReq | 手动配置的请求 |
| 场景引用 | scenario | 引用其他场景 |
| 接口列表导入 | — | 从接口定义中选择请求 |

**步骤添加逻辑（setNode / setComponent）：**

1. 若当前选中了步骤节点（`selectedTreeNode`）：
   - 选中步骤属于"特殊步骤"（HTTP/Dubbo/JDBC/TCP/JSR223 等）：新步骤添加到**父级 hashTree** 的同级位置（断言类型特殊处理，会查找到已有的断言步骤合并）
   - 选中步骤不属于特殊步骤：新步骤添加到**当前步骤的 hashTree** 子级
2. 若未选中步骤：新步骤追加到 `scenarioDefinition` 根数组
3. 断言类型（Assertions）有去重逻辑：同级已有断言时激活它，而非新增

### 3.3 场景变量

文件：`api-test/frontend/src/business/automation/scenario/variable/`

支持 5 种变量类型：

| 类型 | 组件 | 说明 |
|------|------|------|
| 常量 | EditConstant | 固定值变量 |
| 列表 | EditListValue | 多值列表变量 |
| CSV | EditCsv | CSV 文件驱动变量 |
| 计数器 | EditCounter | 自增计数变量 |
| 随机数 | EditRandom | 随机数变量 |

### 3.4 环境配置

场景支持三种环境选择方式：

| 配置 | 说明 |
|------|------|
| environmentType | 环境类型 |
| environmentJson | 单项目-环境映射（JSON） |
| environmentGroupId | 环境组 ID |

相关组件：`EnvSelect.vue`、`EnvPopover.vue`、`EnvGroup.vue`

---

## 4. 步骤类型体系

### 4.1 ELEMENT_TYPE 枚举

定义文件：`api-test/frontend/src/business/automation/scenario/Setting.js`

| 类型 | 后端 Java 类 | 分类 |
|------|-------------|------|
| scenario | MsScenario | 场景引用 |
| HTTPSamplerProxy | MsHTTPSamplerProxy | 采样器 |
| DubboSampler | MsDubboSampler | 采样器 |
| JDBCSampler | MsJDBCSampler | 采样器 |
| TCPSampler | MsTCPSampler | 采样器 |
| IfController | MsIfController | 控制器 |
| TransactionController | MsTransactionController | 控制器 |
| LoopController | MsLoopController | 控制器 |
| ConstantTimer | MsConstantTimer | 定时器 |
| JSR223Processor | MsJSR223Processor | 脚本处理器 |
| JSR223PreProcessor | MsJSR223PreProcessor | 前置脚本 |
| JSR223PostProcessor | MsJSR223PostProcessor | 后置脚本 |
| JDBCPreProcessor | MsJDBCPreProcessor | 前置 SQL |
| JDBCPostProcessor | MsJDBCPostProcessor | 后置 SQL |
| Assertions | MsAssertions | 断言 |
| Extract | MsExtract | 提取器 |
| CustomizeReq | — | 自定义请求 |
| Plugin | — | 插件步骤 |

### 4.2 前端步骤过滤器（STEP Map）

不同步骤类型可添加的子步骤不同：

| 父步骤类型 | 允许的子步骤 |
|-----------|------------|
| HTTPSamplerProxy / Dubbo / JDBC / TCP | 断言 + 前后置处理器 + 配置元件 + 监听器 |
| IfController / LoopController / TransactionController | 全部类型 |
| ConstantTimer | 无（叶子节点） |
| Assertions / Extract | 无（叶子节点） |
| JSR223PreProcessor / JSR223PostProcessor | 无（叶子节点） |
| scenario | 全部类型 |

### 4.3 组件渲染映射

文件：`api-test/frontend/src/business/automation/scenario/component/ComponentConfig.vue`

`ComponentConfig.vue` 根据步骤的 `type` 字段动态渲染对应的 Vue 组件：

| type | 渲染组件 |
|------|---------|
| HTTPSamplerProxy / DubboSampler / JDBCSampler / TCPSampler | ApiComponent |
| scenario | ApiScenarioComponent |
| IfController | IfController |
| LoopController | LoopController |
| TransactionController | TransactionController |
| ConstantTimer | ConstantTimer |
| JSR223Processor / JSR223PreProcessor / JSR223PostProcessor | Jsr233Processor |
| JDBCPreProcessor / JDBCPostProcessor | JDBCProcessor |
| Assertions | ApiComponent（内含断言编辑） |
| Extract | ApiComponent（内含提取编辑） |
| Plugin | PluginComponent |

---

## 5. 数据模型

### 5.1 api_scenario 表（主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR | 主键 |
| project_id | VARCHAR | 所属项目 |
| name | VARCHAR | 场景名称 |
| api_scenario_module_id | VARCHAR | 所属模块 ID |
| module_path | VARCHAR | 模块路径（显示用） |
| level | VARCHAR | 优先级 P0/P1/P2/P3 |
| status | VARCHAR | 状态 |
| principal | VARCHAR | 负责人 |
| step_total | INTEGER | 步骤总数 |
| user_id | VARCHAR | 用户 ID |
| create_time | BIGINT | 创建时间 |
| update_time | BIGINT | 更新时间 |
| tags | VARCHAR | 标签（JSON） |
| pass_rate | VARCHAR | 通过率 |
| last_result | VARCHAR | 最近执行结果 |
| report_id | VARCHAR | 最近报告 ID |
| num | INTEGER | 自增编号 |
| custom_num | VARCHAR | 自定义编号 |
| create_user | VARCHAR | 创建人 |
| version | INTEGER | 版本号 |
| version_id | VARCHAR | 版本 ID |
| ref_id | VARCHAR | 版本引用 ID |
| latest | BIT | 是否最新版本 |
| order | BIGINT | 排序值 |
| environment_type | VARCHAR | 环境类型 |
| environment_group_id | VARCHAR | 环境组 ID |
| execute_times | INTEGER | 执行次数 |
| delete_time | BIGINT | 删除时间 |
| delete_user_id | VARCHAR | 删除人 |
| **scenario_definition** | LONGVARCHAR | 场景定义（JSON BLOB，核心字段） |
| **description** | LONGVARCHAR | 描述 |
| **environment_json** | LONGVARCHAR | 环境配置（JSON BLOB） |

**scenarioDefinition JSON 结构示例：**

```json
[
  {
    "type": "HTTPSamplerProxy",
    "name": "登录接口",
    "index": 1,
    "hashTree": [
      { "type": "Assertions", "hashTree": [] },
      { "type": "JSR223PostProcessor", "hashTree": [] }
    ]
  },
  {
    "type": "IfController",
    "name": "条件判断",
    "hashTree": [
      { "type": "HTTPSamplerProxy", "hashTree": [] }
    ]
  }
]
```

### 5.2 api_scenario_report 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR | 主键 |
| project_id | VARCHAR | 项目 ID |
| name | VARCHAR | 报告名称 |
| scenario_id | VARCHAR | 场景 ID |
| scenario_name | VARCHAR | 场景名称 |
| status | VARCHAR | 执行状态 |
| trigger_mode | VARCHAR | 触发方式（MANUAL/API/SCHEDULE） |
| execute_type | VARCHAR | 执行类型（Debug/Saved/Completed） |
| user_id | VARCHAR | 用户 |
| create_user | VARCHAR | 创建人 |
| create_time | BIGINT | 创建时间 |
| end_time | BIGINT | 结束时间 |
| report_type | VARCHAR | 报告类型 |
| version_id | VARCHAR | 版本 ID |

### 5.3 关联表

| 表 | 说明 |
|----|------|
| api_scenario_module | 模块树节点（id, project_id, name, parent_id, level, pos） |
| api_scenario_follow | 场景关注者（scenario_id, follow_id） |
| api_scenario_reference_id | 场景间交叉引用 |
| api_scenario_report_detail | 报告详细内容 |
| api_scenario_report_result | 单步骤执行结果 |
| api_scenario_report_structure | 报告步骤层级结构 |
| scenario_execution_info | 执行记录追踪 |
| test_plan_api_scenario | 测试计划-场景关联 |

---

## 6. 后端架构

### 6.1 Controller 层

| Controller | 路径前缀 | 职责 |
|-----------|---------|------|
| ApiScenarioController | `/api/automation` | 场景 CRUD、执行、批量操作、导入导出 |
| ApiScenarioModuleController | `/api/automation/module` | 模块树 CRUD、拖拽排序 |
| ApiScenarioReportController | `/api/scenario/report` | 报告查询、删除、重命名 |
| ApiScenarioRerunController | `/api/test/exec` | 失败重跑 |

### 6.2 Service 层

| Service | 职责 |
|---------|------|
| ApiScenarioService | 核心 CRUD、创建/更新/删除、批量操作、导入/导出、执行编排、定时任务、JMX 生成 |
| ApiScenarioModuleService | 模块树 CRUD、拖拽排序 |
| ApiScenarioReportService | 报告 CRUD、重命名、批量删除 |
| ApiScenarioReportStructureService | 报告步骤层级结构 |
| ApiScenarioReportResultService | 单步骤执行结果 |
| ApiScenarioExecutionInfoService | 执行信息记录 |
| ApiScenarioReferenceIdService | 场景间引用关系追踪 |
| ApiScenarioRerunService | 失败场景重跑 |
| ApiAutomationRelationshipEdgeService | 场景依赖关系图 |

### 6.3 执行引擎

| Service | 职责 |
|---------|------|
| ApiScenarioExecuteService | 核心执行编排器：构建 JMeter HashTree、管理串行/并行执行、创建报告 |
| ApiScenarioEnvService | 解析场景运行环境配置 |
| ApiScenarioParallelService | 并行执行模式 |
| ApiScenarioSerialService | 串行执行模式 |

### 6.4 导入解析器

| 解析器 | 职责 |
|--------|------|
| MsScenarioParser | MeterSphere 原生格式 |
| HarScenarioParser | HAR 文件格式 |
| PostmanScenarioParser | Postman Collection 格式 |
| ScenarioImportParserFactory | 解析器工厂，根据格式选择对应解析器 |

---

## 7. 核心流程

### 7.1 场景 CRUD 流程

```
用户打开页面
    │
    ├─→ 加载模块树: GET /api/automation/module/list/{projectId}
    │
    ├─→ 加载场景列表: POST /api/automation/list/{page}/{size}
    │
    ├─→ 点击"新增"或"编辑" → 打开新 Tab (EditApiScenario)
    │       │
    │       ├─→ 编辑基础信息（左侧面板）
    │       ├─→ 编排步骤（右侧面板）
    │       │       ├─→ 点击步骤按钮 → setComponent() → setNode() → 插入步骤
    │       │       ├─→ 拖拽排序步骤
    │       │       └─→ 配置步骤参数
    │       │
    │       └─→ 保存: api-automation.js saveScenario()
    │               ├─→ 收集 bodyFileRequestIds / scenarioFileIds
    │               ├─→ 构建 FormData (request JSON + 文件)
    │               └─→ POST /api/automation/create 或 /update
    │
    └─→ 删除 → 移至回收站 (move-gc) → 恢复 (reduction) 或彻底删除 (delete)
```

### 7.2 场景执行流程

```
用户点击"执行"
    │
    ├─→ 前端: scenarioRun() → POST /api/automation/run
    │        （调试模式: POST /api/automation/run/debug）
    │        （批量执行: POST /api/automation/run/batch）
    │
    ├─→ 后端: ApiScenarioController.run()
    │       → ApiScenarioService.run()
    │       → ApiScenarioExecuteService.run()
    │           │
    │           ├─→ 创建 ApiScenarioReport 记录
    │           ├─→ 解析 scenarioDefinition JSON → 构建 JMeter HashTree
    │           ├─→ ApiScenarioEnvService 解析环境配置
    │           │
    │           ├─→ 并行模式 → ApiScenarioParallelService
    │           └─→ 串行模式 → ApiScenarioSerialService
    │                   │
    │                   └─→ JMeterService 执行 HashTree
    │
    ├─→ 执行结果通过 WebSocket (baseSocket) 实时推送
    │
    └─→ 报告保存 → 查看: GET /api/scenario/report/get/{reportId}
```

### 7.3 回收站流程

```
移至回收站: POST /api/automation/move-gc-ids （软删除，status 改为 "Trash"）
           POST /api/automation/move-gc-batch （批量软删除）

恢复: POST /api/automation/reduction

彻底删除: GET /api/automation/delete/{id} （硬删除）
         POST /api/automation/del-ids （批量硬删除）
         POST /api/automation/del-batch （按条件批量硬删除）
```

### 7.4 场景导入导出流程

**导入：**
```
用户上传文件 → ScenarioImportParserFactory 选择解析器
    ├─→ MsScenarioParser（.json，MeterSphere 格式）
    ├─→ HarScenarioParser（.har，HAR 格式）
    └─→ PostmanScenarioParser（.json，Postman 格式）
→ 解析为 ApiScenario 对象列表
→ ApiScenarioService.importScenario() 批量创建
```

**导出：**
```
POST /api/automation/export → 导出为 MeterSphere JSON 格式
POST /api/automation/export/zip → 导出为 ZIP 包
POST /api/automation/export/jmx → 导出为 JMX 格式
```

---

## 8. 状态管理

文件：`api-test/frontend/src/store/state.js`

Pinia Store（id: `'API'`）管理以下状态：

| 状态 | 类型 | 说明 |
|------|------|------|
| useEnvironment | String | 当前选择的环境 |
| selectStep | Object | 当前选中的步骤 |
| scenarioEnvMap | Map | 场景-环境映射 |
| pluginFiles | Map | 插件上传文件 |
| forceRerenderIndex | String | 强制重渲染触发器 |
| apiMap | Map | 接口定义缓存 |
| apiStatus | Map | 接口状态缓存 |
| apiCaseMap | Map | 接口用例缓存 |
| saveMap | Map | 保存状态追踪 |

---

## 9. 前端 API 层

### 9.1 场景 API（scenario.js）

| 函数 | HTTP | 路径 | 说明 |
|------|------|------|------|
| getScenarioById | GET | `/api/automation/get/{id}` | 获取场景 |
| getScenarioWithBLOBsById | GET | `/api/automation/scenario-details/{id}` | 获取完整定义 |
| getScenarioList | POST | `/api/automation/list/{page}/{size}` | 分页列表 |
| scenarioRun | POST | `/api/automation/run` | 执行场景 |
| runBatch | POST | `/api/automation/run/batch` | 批量执行 |
| batchEditScenario | POST | `/api/automation/batch/edit` | 批量编辑 |
| batchCopyScenario | POST | `/api/automation/batch/copy` | 批量复制 |
| updateScenarioEnv | POST | `/api/automation/batch/update/env` | 批量更新环境 |
| removeScenarioToGcByBatch | POST | `/api/automation/move-gc-batch` | 批量移至回收站 |
| scenarioReduction | POST | `/api/automation/reduction` | 从回收站恢复 |
| exportScenario | POST | `/api/automation/export` | 导出 |
| importScenario | POST | `/api/automation/import` | 导入 |
| genPerformanceTestJmx | POST | `/api/automation/gen-jmx` | 生成 JMX |
| createSchedule | POST | `/api/automation/schedule/create` | 创建定时任务 |
| updateSchedule | POST | `/api/automation/schedule/update` | 更新定时任务 |

### 9.2 模块 API（scenario-module.js）

| 函数 | HTTP | 路径 | 说明 |
|------|------|------|------|
| getModuleTree | GET | `/api/automation/module/list/{projectId}` | 获取模块树 |
| getTrashModuleTree | GET | `/api/automation/module/trash/list/{projectId}` | 获取回收站模块树 |
| addModule | POST | `/api/automation/module/add` | 新增模块 |
| editModule | POST | `/api/automation/module/edit` | 编辑模块 |
| deleteModule | POST | `/api/automation/module/delete` | 删除模块 |
| dragModule | POST | `/api/automation/module/drag` | 拖拽排序 |

### 9.3 报告 API（scenario-report.js）

| 函数 | HTTP | 路径 | 说明 |
|------|------|------|------|
| getReportById | GET | `/api/scenario/report/get/{id}` | 获取报告 |
| getReportDetail | GET | `/api/scenario/report/get/detail/{id}` | 获取完整报告详情 |
| getReportList | POST | `/api/scenario/report/list/{page}/{size}` | 分页报告列表 |
| deleteReport | POST | `/api/scenario/report/delete` | 删除报告 |
| batchDeleteReport | POST | `/api/scenario/report/batch/delete` | 批量删除报告 |
| renameReport | POST | `/api/scenario/report/rename` | 重命名报告 |

---

## 10. 版本管理

场景支持版本管理（xpack 功能）：

- 版本列表：`GET /api/automation/versions/{scenarioId}`
- 按版本获取：`GET /api/automation/get/{version}/{refId}`
- 按版本删除：`GET /api/automation/delete/{version}/{refId}`
- 版本对比：`ScenarioDiff.vue` / `ScenarioChildDiff.vue`

---

## 11. 报告模块

### 11.1 报告列表页

文件：`api-test/frontend/src/business/automation/report/ApiReportList.vue`

### 11.2 报告详情页

文件：`api-test/frontend/src/business/automation/report/ApiReportDetail.vue`

报告详情展示结构：

```
报告详情
├── 请求指标 (RequestMetric)
├── 请求结果树 (InfiniteScrollTree)
│   └── 每个节点 → 请求结果 (RequestResult)
│       ├── 请求内容 (RequestText)
│       ├── 响应内容 (ResponseText)
│       ├── 断言结果 (AssertionResults)
│       └── 子请求 (RequestSubResult)
├── 场景结果 (ScenarioResults)
│   └── 单场景结果 (ScenarioResult)
└── SQL 结果表 (SqlResultTable)
```

---

## 12. 定时任务

场景支持定时执行：

- 创建定时任务：`POST /api/automation/schedule/create`
- 更新定时任务：`POST /api/automation/schedule/update`
- 触发方式：SCHEDULE
- 相关组件：
  - `ScheduleMaintain.vue`：定时任务维护
  - `ScheduleSwitch.vue`：开关控制
  - `ScheduleNotification.vue`：通知配置
  - `ScheduleInfoInTable.vue`：列表内展示

---

## 13. 关键目录索引

```
api-test/
├── frontend/src/business/automation/
│   ├── ApiAutomation.vue                    # 主页面入口
│   ├── api-automation.js                    # 场景保存、快捷键等工具函数
│   ├── scenario/
│   │   ├── EditApiScenario.vue              # 场景编辑器（核心页面）
│   │   ├── ApiScenarioList.vue              # 场景列表
│   │   ├── ApiScenarioModule.vue            # 模块树
│   │   ├── Setting.js                       # 步骤类型枚举、过滤器
│   │   ├── menu/Menu.js                     # 步骤按钮、添加逻辑
│   │   ├── component/                       # 步骤渲染组件
│   │   ├── common/                          # 公共组件（脚本编辑、运行模式等）
│   │   ├── api/                             # 接口/用例关联对话框
│   │   ├── variable/                        # 场景变量管理
│   │   └── maximize/                        # 全屏编辑
│   ├── report/                              # 报告页面
│   ├── version/                             # 版本对比
│   └── schedule/                            # 定时任务
├── backend/src/main/java/io/metersphere/
│   ├── controller/scenario/
│   │   ├── ApiScenarioController.java       # 场景 API
│   │   ├── ApiScenarioModuleController.java # 模块 API
│   │   └── ApiScenarioReportController.java # 报告 API
│   ├── service/scenario/
│   │   ├── ApiScenarioService.java          # 核心业务逻辑
│   │   ├── ApiScenarioModuleService.java    # 模块服务
│   │   └── ApiScenarioReportService.java    # 报告服务
│   └── api/exec/scenario/
│       ├── ApiScenarioExecuteService.java   # 执行编排
│       ├── ApiScenarioParallelService.java  # 并行执行
│       └── ApiScenarioSerialService.java    # 串行执行
```
