# 需求文档：接口测试前端性能优化

## 简介

接口测试自动化场景在大数据量、大步骤树、版本对比、保存/调试和复杂步骤组件渲染时存在卡顿风险。已完成一轮低风险优化后，普通编辑器和最大化编辑器的主步骤树已具备大场景虚拟滚动能力，但版本对比页、强制刷新链路、保存/调试深拷贝、单步骤组件内部渲染和按需挂载仍是后续主要优化方向。

本需求文档聚焦 `api-test/frontend/src/business/automation/` 下接口测试模块的前端性能优化。

## 术语表

| 术语 | 定义 |
|------|------|
| `scenarioDefinition` | 接口自动化场景定义大对象，包含场景树、请求、控制器、断言、变量等数据 |
| 步骤树 | 场景步骤的树形结构，字段通常为 `hashTree` |
| 虚拟滚动 | 只渲染可视区域附近节点，降低 DOM 数量和布局计算 |
| 强制刷新链路 | 通过全局 key、临时 `push + splice` 等方式触发 Vue 重新渲染的逻辑 |
| 按需挂载 | 节点展开或进入可视区域后再挂载重组件 |
| 结构化深拷贝优化 | 按保存/调试所需字段构造 payload，减少整棵树 JSON 深拷贝 |

## 当前结论

大场景下的主要性能瓶颈不是单点问题，而是以下成本叠加：

1. `scenarioDefinition` 大对象在保存、调试、版本对比时被深拷贝或完整遍历。
2. `el-tree` 在大步骤树下会一次性创建大量 DOM 和步骤组件。
3. 每个可见步骤节点挂载的 `MsComponentConfig` 可能包含断言、脚本编辑器、请求体等重组件。
4. `forceRerenderIndex`、`push + splice` 等刷新方式会扩大重渲染范围。
5. 版本对比页左右两棵树同时渲染，成本天然翻倍。

## 已完成优化基线

| 优化项 | 当前状态 |
|--------|----------|
| 核心递归数组遍历优化 | 已完成重点路径，将高频 `for...in` 改为数组遍历 |
| 批量删除 `splice` 修复 | 已完成，改为倒序删除并命中返回 |
| 版本对比等待时间 | 已完成，从按子组件数量乘 5 秒改为固定约 1 秒 |
| 步骤组件懒加载 | 已完成，`ComponentConfig.vue` 中多个重组件改为异步组件 |
| timer / RAF 清理 | 已完成，降低组件销毁后的残留风险 |
| 稳定 `v-for key` | 已完成重点增删场景 |
| 普通编辑器步骤树虚拟滚动 | 已存在，超过阈值切换 `vue-virtual-tree` |
| 最大化编辑器步骤树虚拟滚动 | 已补齐，超过 50 个可展示步骤切换 `vue-virtual-tree` |

## 需求

### 需求 1：版本对比页左右步骤树虚拟化

**用户故事：** 作为测试人员，我希望打开大场景版本对比页时页面能快速展示和滚动，以便对比历史版本时不会因为左右步骤树过大而卡顿。

#### 验收标准

1. WHEN 版本对比页任一侧可展示步骤数超过阈值 THEN 系统 SHALL 将该侧步骤树切换为虚拟滚动树。
2. THE 阈值 SHALL 递归统计可展示节点数，而不是只统计顶层 `scenarioDefinition.length`。
3. THE 小场景 SHALL 继续使用原 `el-tree`，避免引入不必要复杂度。
4. THE 左右树 SHALL 保持展开、收起、节点点击、弹出 `ScenarioChildDiff` 的行为一致。
5. THE diff 高亮 SHALL 在虚拟树模式下不出现阻塞主流程的异常。

**影响文件**：`api-test/frontend/src/business/automation/version/ScenarioDiff.vue`

### 需求 2：强制刷新链路重构

**用户故事：** 作为测试人员，我希望新增步骤、批量处理和树状态变化时只刷新必要区域，以便大场景下操作不会触发整棵树或所有步骤组件重渲染。

#### 验收标准

1. THE 系统 SHALL 梳理 `forceRerenderIndex` 的所有触发点和消费点。
2. THE 系统 SHALL 将全局 key 刷新逐步替换为局部响应式状态或明确的树刷新 API。
3. THE 系统 SHALL 移除通过临时 `push + splice` 触发树刷新的逻辑。
4. WHEN 新增断言、控制器或请求步骤 THEN 系统 SHALL 只刷新受影响节点及必要父节点。
5. THE 改造 SHALL 保持普通编辑器、最大化编辑器和批量处理行为一致。

**影响文件**：

- `api-test/frontend/src/business/automation/scenario/common/ApiBaseComponent.vue`
- `api-test/frontend/src/business/automation/scenario/menu/Menu.js`
- `api-test/frontend/src/business/automation/scenario/maximize/MaximizeScenario.vue`
- `api-test/frontend/src/business/automation/scenario/EditApiScenario.vue`

### 需求 3：保存/调试大对象深拷贝结构化优化

**用户故事：** 作为测试人员，我希望保存或调试大场景时页面响应更快，以便不会因为整棵 `scenarioDefinition` 深拷贝造成明显卡顿。

#### 验收标准

1. THE 系统 SHALL 梳理保存、调试、版本对比中所有 `JSON.parse(JSON.stringify())` 使用点。
2. THE 系统 SHALL 区分 UI 状态字段和提交必需字段。
3. WHEN 保存场景 THEN 系统 SHALL 构造最小保存 payload，避免不必要地复制 UI 临时状态。
4. WHEN 调试场景 THEN 系统 SHALL 只复制运行所需的最小 testPlan/threadGroup/step 结构。
5. THE 优化 SHALL 不污染原始 `scenarioDefinition`，不影响保存结果、调试结果和版本对比结果。

**影响文件**：

- `api-test/frontend/src/business/automation/api-automation.js`
- `api-test/frontend/src/business/automation/ApiAutomation.vue`
- `api-test/frontend/src/business/automation/scenario/EditApiScenario.vue`
- `api-test/frontend/src/business/automation/scenario/maximize/MaximizeScenario.vue`
- `api-test/frontend/src/business/automation/scenario/component/ApiScenarioComponent.vue`

### 需求 4：单个步骤组件内部渲染成本优化

**用户故事：** 作为测试人员，我希望展开复杂断言、脚本编辑器、请求体等步骤时页面仍能保持流畅，以便大场景编辑不会被少数重组件拖慢。

#### 验收标准

1. THE 系统 SHALL 识别渲染成本较高的步骤组件类型。
2. THE 脚本编辑器类组件 SHALL 在节点折叠或未进入编辑态时避免挂载完整编辑器实例。
3. THE 复杂断言列表 SHALL 使用稳定 key、局部 DOM 查询和必要的延迟测量。
4. THE 请求体、参数表、文件列表等大列表 SHALL 避免无意义深度 watch 和全量重算。
5. THE 单组件优化 SHALL 不改变已有表单字段、校验和保存结构。

**重点组件**：

- `PluginContentAssertions.vue`
- 脚本处理器组件
- 请求体/参数/文件上传相关组件
- `ComponentConfig.vue`

### 需求 5：大场景按需展开和按需挂载子步骤组件

**用户故事：** 作为测试人员，我希望大场景默认只渲染当前可见和已展开的步骤，以便几百个步骤的场景也能保持可编辑。

#### 验收标准

1. WHEN 节点处于折叠状态 THEN 系统 SHALL 不挂载其子步骤重组件。
2. WHEN 用户展开节点 THEN 系统 SHALL 再挂载该节点下必要的子步骤组件。
3. WHEN 批量展开超过安全阈值 THEN 系统 SHALL 控制挂载节奏，避免一次性挂载全部重组件。
4. THE 系统 SHALL 保持场景引用、循环、事务、If 控制器等容器节点行为一致。
5. THE 优化 SHALL 与虚拟滚动兼容，不造成选中、拖拽、展开状态错乱。

## 非功能性需求

### 性能目标

| 场景 | 目标 |
|------|------|
| 版本对比页大场景打开 | DOM 节点数量明显下降，滚动不卡顿 |
| 大场景保存/调试 | 主线程长任务减少，内存峰值下降 |
| 新增步骤/断言 | 避免整树或所有步骤组件重渲染 |
| 展开复杂步骤 | 只挂载必要组件，折叠态成本降低 |
| 100+ 可展示步骤 | 普通/最大化/版本对比主路径均可操作 |

### 兼容性

1. THE 优化 SHALL 基于 Vue 2 和现有 Element UI / `vue-virtual-tree` 能力实现。
2. THE 优化 SHALL 不引入新的大型依赖。
3. THE 优化 SHALL 保持小场景现有交互稳定。

### 可验证性

1. THE 系统 SHALL 使用 `npm run lint` 和 `npm run build` 验证前端构建。
2. THE 系统 SHOULD 使用 Chrome Performance 对比优化前后长任务、DOM 数量和内存峰值。
3. THE 系统 SHOULD 准备 20、50、100、200 步场景作为性能回归样本。
