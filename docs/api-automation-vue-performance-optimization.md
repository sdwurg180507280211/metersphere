# 接口自动化页面 Vue 前端性能优化建议

## 背景

接口测试模块的“接口自动化”页面包含左侧模块树、场景表格、场景编辑 Tab、步骤树、调试抽屉、引用弹窗、批量操作弹窗等多个复杂区域。该页面的性能瓶颈主要集中在以下几类：

- 表格列和单元格组件数量较多
- 左侧模块树与表格刷新存在耦合
- 场景步骤树节点较多时组件渲染较重
- 多个编辑 Tab 同时保留完整组件树
- 弹窗、抽屉、批量操作组件默认挂载较多
- 大对象深拷贝和 diff 操作较频繁

本文档整理当前代码中可落地的 Vue 前端性能优化点。

## 相关文件

- `api-test/frontend/src/business/automation/ApiAutomation.vue`
- `api-test/frontend/src/business/automation/scenario/ApiScenarioList.vue`
- `api-test/frontend/src/business/automation/scenario/ApiScenarioModule.vue`
- `api-test/frontend/src/business/automation/scenario/EditApiScenario.vue`
- `api-test/frontend/src/business/automation/scenario/maximize/MaximizeScenario.vue`
- `api-test/frontend/src/business/automation/scenario/component/ComponentConfig.vue`
- `framework/sdk-parent/frontend/src/components/table/MsTable.vue`
- `framework/sdk-parent/frontend/src/components/table/MsTableColumn.vue`

## 已有优化点

当前代码中已经有一些值得保留的优化：

1. 左侧模块树搜索已做防抖，避免每次输入都触发 `el-tree.filter`。
   - 位置：`ApiScenarioModule.vue`

2. 场景步骤数超过 50 时，已切换到 `vue-virtual-tree`。
   - 位置：`EditApiScenario.vue`

3. 环境弹窗收集 `projectId` 已避免使用 `JSON.stringify + 正则` 扫描整棵步骤树。
   - 位置：`EditApiScenario.vue`

这些优化方向是正确的，后续优化应在此基础上继续减少不必要的组件渲染、重复请求和深拷贝。

---

## P0：优先优化项

### 1. 修复 EventBus 解绑问题

#### 现状

`ApiScenarioModule.vue` 中注册事件时使用匿名函数：

```js
this.$EventBus.$on('scenarioConditionBus', (param) => {
  this.param = param;
  if (this.$route.params && this.$route.params.versionId) {
    this.list();
  }
});
```

销毁时又传入另一个匿名函数：

```js
this.$EventBus.$off('scenarioConditionBus', (param) => {
  this.param = param;
});
```

这会导致 `$off` 无法解绑原监听函数。页面反复进入、销毁后，监听器可能累积，最终造成一次事件触发多次刷新。

#### 建议

改为保存函数引用：

```js
created() {
  this.scenarioConditionHandler = (param) => {
    this.param = param;
    if (this.$route.params && this.$route.params.versionId) {
      this.list();
    }
  };
  this.$EventBus.$on('scenarioConditionBus', this.scenarioConditionHandler);
},

beforeDestroy() {
  this.$EventBus.$off('scenarioConditionBus', this.scenarioConditionHandler);
}
```

#### 收益

- 避免监听器泄漏
- 避免重复请求模块树
- 避免页面越用越卡

---

### 2. 优化场景表格列渲染结构

#### 现状

`ApiScenarioList.vue` 中使用如下结构：

```vue
<span v-for="item in fields" :key="item.key">
  <ms-table-column prop="num" ... />
  <ms-table-column prop="name" ... />
  <ms-table-column prop="nodePath" ... />
  <ms-table-column prop="level" ... />
  ...
</span>
```

`MsTableColumn.vue` 内部再通过条件判断是否真正渲染：

```vue
<el-table-column v-if="active && (!field || field.id === prop)" ...>
```

这会导致：

- 每个 `field` 都创建一批 `ms-table-column` 组件实例
- 大量组件最终只是通过 `v-if` 不渲染真实列
- 自定义列越多，组件实例越多

#### 建议

将列配置预处理成真正需要渲染的列表，例如：

```js
computed: {
  visibleColumns() {
    return this.fields
      .filter((field) => field.checked !== false)
      .map((field) => this.columnConfigMap[field.id])
      .filter(Boolean);
  },
}
```

模板只渲染实际列：

```vue
<component
  v-for="column in visibleColumns"
  :key="column.id"
  :is="column.component"
  v-bind="column.props"
/>
```

如果暂时不做动态组件，也可以至少避免“每个 field 下声明所有列”。

#### 收益

- 减少 `MsTableColumn` 实例数量
- 降低表格初次渲染成本
- 降低切换自定义列、刷新表格时的重渲染成本

---

### 3. 合并场景列表和定时任务信息赋值，减少表格二次渲染

#### 现状

`ApiScenarioList.vue` 中列表请求完成后先赋值：

```js
this.tableData = data.listObject;
```

随后再请求定时任务详情：

```js
this.selectSchedule(ids);
```

`selectSchedule` 内部又逐行 `$set`：

```js
this.tableData.forEach((scenario) => {
  let scheduleInfo = this.getScheduleObject(scheduleData[scenario.id], scenario.id);
  this.$set(scenario, 'scheduleObj', scheduleInfo);
});
```

这会导致表格至少渲染两次：

1. 渲染场景列表
2. 定时任务数据返回后，逐行更新 `scheduleObj` 再渲染

#### 建议

优先方案：后端在场景列表接口中直接返回定时任务摘要。

如果后端暂时不改，可以前端合并后一次性赋值：

```js
const list = data.listObject.map((item) => ({
  ...item,
  tags: item.tags ? JSON.parse(item.tags) : [],
}));

const ids = list.map((item) => item.id);
const scheduleResponse = await getScheduleDetail(ids);
const scheduleData = scheduleResponse.data || {};

this.tableData = list.map((scenario) => ({
  ...scenario,
  scheduleObj: this.getScheduleObject(scheduleData[scenario.id], scenario.id),
}));
```

另外，如果用户隐藏了“定时任务”列，可以不请求 `getScheduleDetail`。

#### 收益

- 减少表格二次渲染
- 减少逐行 `$set` 带来的响应式更新
- 大分页条数时收益明显

---

### 4. 降低模块树刷新和表格搜索的耦合

#### 现状

`ApiScenarioList.vue` 的 `nodeChange` 中存在：

```js
if (this.needRefreshModule()) {
  this.$emit('refreshTree');
}
```

并且每次表格条件变化会广播：

```js
this.$EventBus.$emit('scenarioConditionBus', this.condition);
```

左侧模块树收到事件后可能重新请求模块树。

#### 问题

表格搜索、排序、分页、过滤并不总是需要刷新左侧模块树。模块树刷新涉及接口请求、树构建、过滤等操作，和表格查询强耦合会增加额外开销。

#### 建议

将事件拆分得更明确：

- `scenarioQueryChanged`：只表示表格查询条件变化
- `scenarioModuleNeedRefresh`：明确需要刷新模块树

只有以下操作触发模块树刷新：

- 模块新增、编辑、删除
- 场景移动模块
- 垃圾箱状态切换
- 版本切换且左侧模块统计确实依赖版本
- 导入、批量删除、还原等影响模块统计的操作

#### 收益

- 减少模块树重复请求
- 减少树构建和过滤成本
- 降低搜索、分页、排序时的额外开销

---

## P1：步骤编辑器优化

### 5. 步骤树改为轻量摘要行，选中后再渲染完整编辑组件

#### 现状

普通编辑模式中，每个步骤节点都会渲染完整的 `ms-component-config`：

```vue
<ms-component-config
  :scenario-definition="scenarioDefinition"
  :message="message"
  :type="data.type"
  :scenario="data"
  :response="response"
  :currentScenario="currentScenario"
  :node="node"
  :project-list="projectList"
  :env-map="projectEnvMap"
/>
```

全屏模式中也存在类似结构。

#### 问题

- 每个步骤节点都是一个完整表单组件
- 每个节点都接收整个 `scenarioDefinition`
- `message`、`response`、`envMap`、`currentScenario` 等变化可能引发大量子组件更新
- 大场景下即使用虚拟树，单个可见节点的渲染成本仍然偏高

#### 建议

左侧步骤树只渲染轻量摘要，例如：

- 步骤名称
- 步骤类型
- 是否启用
- 调试状态
- 简单操作按钮

完整编辑表单只在右侧详情区域渲染当前选中的步骤：

```vue
<step-summary-row
  v-for="node in visibleNodes"
  :key="node.resourceId"
  :step="node"
  @select="selectStep"
/>

<ms-component-config
  v-if="selectedTreeNode"
  :type="selectedTreeNode.type"
  :scenario="selectedTreeNode"
/>
```

#### 收益

- 大幅降低步骤树渲染成本
- 大场景编辑和滚动更流畅
- 调试消息更新时只影响相关摘要行或详情区

---

### 6. 避免 `ComponentConfig` computed 中产生副作用

#### 现状

`ComponentConfig.vue` 中 `component` 是 computed：

```js
component({ type }) {
  let name;
  switch (type) {
    ...
    name = this.getComponent(...);
  }
  return name;
}
```

但 `getComponent` 会修改组件状态：

```js
this.title = ...;
this.titleColor = ...;
this.backgroundColor = ...;
this.apiId = ...;
this.scenario.document.nodeType = 'scenario';
```

#### 问题

computed 应尽量保持纯函数。computed 中修改响应式数据可能导致额外更新，甚至造成难以排查的重复渲染。

#### 建议

改为返回纯元信息：

```js
computed: {
  componentMeta() {
    switch (this.type) {
      case ELEMENT_TYPE.JSR223PreProcessor:
        return {
          name: 'MsJsr233Processor',
          title: this.$t('api_test.definition.request.pre_script'),
          titleColor: '#b8741a',
          backgroundColor: '#F9F1EA',
        };
      default:
        return {
          name: 'PluginComponent',
          titleColor: '#1483F6',
          backgroundColor: '#F2ECF3',
        };
    }
  },
}
```

模板中使用：

```vue
<component
  :is="componentMeta.name"
  :title="componentMeta.title"
  :color="componentMeta.titleColor"
  :background-color="componentMeta.backgroundColor"
/>
```

历史数据兼容、字段补齐等逻辑应放在数据加载或 `dataProcessing` 阶段。

#### 收益

- 减少无意义响应式更新
- 降低动态组件切换成本
- 组件逻辑更可预测

---

### 7. 减少 `ComponentConfig` 传递重复 props

#### 现状

同一个 `scenario` 被传给多个 prop：

```vue
:scenario="scenario"
:controller="scenario"
:timer="scenario"
:assertions="scenario"
:extract="scenario"
:command="scenario"
:jsr223-processor="scenario"
:request="scenario"
```

#### 建议

统一子组件入参，尽量只使用：

```vue
:scenario="scenario"
```

如果短期无法统一，至少按组件类型只传必要 prop。

#### 收益

- 减少 prop diff 成本
- 降低子组件 watcher 触发概率
- 简化组件接口

---

## P1：调试 / 生成报告链路优化

### 8. 调试前置处理减少整棵步骤树重复遍历

#### 现状

点击“调试”会进入 `EditApiScenario.vue` 的 `runDebug`，主要链路包括：

1. `mergeScenario(this.scenarioDefinition)`
2. `validatePluginData(this.scenarioDefinition)`
3. `clearResult(this.scenarioDefinition)`
4. `clearNodeStatus(this.$refs.stepTree.root.childNodes)`
5. `sort()` / `sort(this.runScenario.hashTree)`
6. `initMessageSocket()`
7. WebSocket 建连成功后触发 `DebugRun.vue` 的执行请求

其中多步都会递归遍历整棵 `scenarioDefinition` 或整棵 `el-tree` 节点。

#### 问题

- 点击调试时存在多次全量递归。
- `runDebug` 中 `mergeScenario(this.scenarioDefinition)` 在 `debugLoading` 判断之前执行，重复点击时即使会 return，也已经产生了一次全量遍历。
- 单步骤调试时也会先处理整棵场景树，成本偏高。

#### 建议

1. 将 `debugLoading` 判断提前到所有重操作之前：

```js
runDebug(runScenario) {
  if (this.debugLoading) {
    return;
  }
  if (!hasPermissions('PROJECT_API_SCENARIO:READ+DEBUG', 'PROJECT_API_SCENARIO:READ+RUN')) {
    return;
  }
  ...
}
```

2. 单步骤调试时只处理当前步骤子树：

```js
const isStepDebug = runScenario && runScenario.stepScenario;
const targetSteps = isStepDebug ? runScenario.hashTree : this.scenarioDefinition;

this.mergeScenario(targetSteps);
this.validatePluginData(targetSteps);
```

3. 合并前置递归逻辑，把 `mergeScenario`、`validatePluginData`、`sort` 中可合并的兼容处理放到一次遍历中完成。

#### 收益

- 降低点击调试时的 CPU 峰值
- 避免重复点击导致额外整树遍历
- 单步骤调试更轻量

---

### 9. WebSocket 调试消息处理避免每条消息全树扫描

#### 现状

调试执行期间，`onDebugMessage` 每收到一条 WebSocket 消息会调用：

```js
this.testing(e.data);
...
this.runningEvaluation(e.data);
```

这些方法内部会递归遍历 `this.$refs.stepTree.root.childNodes`。对于 `result_` 消息，`runningEvaluation`、`runningNodeChild` 中还会在循环内多次执行：

```js
let data = JSON.parse(resultData.substring(7));
```

#### 问题

如果场景有 N 个步骤、执行产生 M 条消息，当前逻辑接近 `O(N * M)`。大场景调试时，WebSocket 消息越多，前端越容易卡顿。

#### 建议

1. 在步骤树加载或排序完成后，建立步骤索引：

```js
buildStepIndex(steps, parent = null) {
  this.stepIndexMap = this.stepIndexMap || new Map();
  this.stepParentMap = this.stepParentMap || new Map();

  steps.forEach((step) => {
    const key1 = `${step.id}_${step.parentIndex}`;
    const key2 = `${step.resourceId}_${step.parentIndex}`;

    if (step.id) this.stepIndexMap.set(key1, step);
    if (step.resourceId) this.stepIndexMap.set(key2, step);
    if (parent) this.stepParentMap.set(step, parent);

    if (step.hashTree && step.hashTree.length > 0) {
      this.buildStepIndex(step.hashTree, step);
    }
  });
}
```

2. WebSocket 消息只解析一次：

```js
let result = null;
if (e.data && e.data.startsWith('result_')) {
  result = JSON.parse(e.data.substring(7));
}
```

3. 根据 `resourceId` 直接定位步骤并更新状态，父级状态通过 `stepParentMap` 向上更新，不再每条消息扫描整棵树。

4. 对高频消息的 UI 更新做批处理，例如用 `requestAnimationFrame` 合并同一帧内的状态刷新。

#### 收益

- 调试期间前端 CPU 占用显著降低
- 大场景执行时步骤状态更新更流畅
- 减少重复 `JSON.parse`

---

### 10. 生成报告链路去掉无效深拷贝和重复排序

#### 现状

点击“生成报告”会进入 `EditApiScenario.vue` 的 `handleCommand`。当前逻辑中有一段深拷贝：

```js
let definition = JSON.parse(JSON.stringify(this.currentScenario));
definition.hashTree = this.scenarioDefinition;
```

但后续并未使用 `definition`。

之后设置 `reportId`，触发 `DebugRun.vue` 的 watcher：

```js
watch: {
  reportId() {
    this.run();
  },
}
```

`DebugRun.vue` 的 `run` 又会构造 `TestPlan`、`ThreadGroup`，并再次执行递归 `sort(testPlan.hashTree)`，最后通过 `saveScenario('/api/automation/run/debug', ...)` 上传执行。

#### 问题

- `definition` 深拷贝是无效成本，可以删除。
- 生成报告前 `handleCommand` 已经做了 `mergeScenario`、`validatePluginData`、`initParameter`，`DebugRun.run` 中又会递归补 `clazzName`、断言文档等，存在重复处理。
- `saveScenario` 会扫描场景文件、body 文件并构造 `FormData`，大场景和多文件场景下成本较高。

#### 建议

1. 删除未使用的深拷贝：

```js
// 删除
let definition = JSON.parse(JSON.stringify(this.currentScenario));
definition.hashTree = this.scenarioDefinition;
```

2. 将生成报告前的数据归一化集中到一个方法中，避免 `EditApiScenario` 和 `DebugRun` 分别递归处理。

3. `DebugRun.run` 优先信任已归一化的 `debugData`，只做必要的 `TestPlan` 包装。

4. 文件扫描可以按需执行：如果场景步骤中没有上传文件相关字段，跳过 `getBodyUploadFiles` / `getScenarioFiles` 的全量扫描；或者维护文件变更索引，避免每次生成报告都扫整棵树。

#### 收益

- 点击生成报告响应更快
- 减少大场景深拷贝和重复递归
- 多文件场景下减少无效文件扫描

---

## P1：Tab 和弹窗懒加载

### 11. 非激活编辑 Tab 不保留完整组件树

#### 现状

`ApiAutomation.vue` 中每个打开的场景 Tab 都保留一个完整 `ms-edit-api-scenario`：

```vue
<el-tab-pane :key="item.name" v-for="item in tabs" ...>
  <ms-edit-api-scenario ... />
</el-tab-pane>
```

#### 问题

用户打开多个复杂场景后，每个 Tab 都保留完整步骤树、表单、环境数据和监听器。即使 Tab 不可见，也会占用内存，并可能参与响应式更新。

#### 建议

只渲染当前激活 Tab 的编辑器：

```vue
<ms-edit-api-scenario
  v-if="activeName === item.name"
  ...
/>
```

如果需要保留编辑状态，可以考虑：

- 将编辑数据保存在父组件或 store 中
- 只缓存最近 1～2 个编辑器实例
- 切换 Tab 时销毁 DOM，重新进入时恢复数据

#### 收益

- 多 Tab 场景下降低内存占用
- 减少非激活页面响应式更新
- 提高整体交互流畅度

---

### 12. 弹窗、抽屉、批量操作组件按需挂载

#### 现状

`ApiScenarioList.vue` 中多个组件默认随页面挂载：

```vue
<batch-edit ... />
<batch-move ... />
<ms-api-run-mode ... />
<ms-run ... />
<ms-task-center ... />
<relationship-graph-drawer ... />
<scenario-delete-confirm ... />
<api-delete-confirm ... />
<ms-show-reference ... />
```

#### 建议

对不常用组件增加首次打开懒挂载：

```vue
<batch-edit
  v-if="batchEditMounted"
  ref="batchEdit"
  ...
/>
```

打开时：

```js
handleBatchEdit() {
  this.batchEditMounted = true;
  this.$nextTick(() => {
    this.$refs.batchEdit.open(...);
  });
}
```

对于内容较重的抽屉，关闭后可以销毁。

#### 收益

- 降低接口自动化列表页首屏组件数量
- 降低初次进入页面耗时
- 减少不常用功能对主页面的影响

---

## P2：表格细节优化

### 13. 标签列和环境列减少 DOM 与 tooltip/popover 实例

#### 现状

标签列每行都创建 tooltip 和多个 tag：

```vue
<el-tooltip>
  <div v-html="getTagToolTips(scope.row.tags)" slot="content"></div>
  <ms-tag v-for="(itemName, index) in scope.row.tags" ... />
</el-tooltip>
```

`getTagToolTips` 每次渲染都会拼接字符串。

环境列每行也可能创建 `el-popover`。

#### 建议

1. 列表数据处理时预计算标签提示：

```js
item.tags = item.tags ? JSON.parse(item.tags) : [];
item.tagTooltip = item.tags.join(',');
```

2. 没有多标签或没有溢出时不挂 tooltip。

3. 环境列 popover 内容点击时再渲染：

```vue
<el-popover @show="row.envPopoverVisible = true">
  <template v-if="row.envPopoverVisible">
    ...
  </template>
</el-popover>
```

#### 收益

- 减少 tooltip/popover 实例
- 降低表格单元格渲染成本
- 表格横向列多时收益更明显

---

### 14. 过滤项数据按列懒加载

#### 现状

页面初始化时会加载负责人、版本、环境等过滤项：

```js
this.getPrincipalOptions([]);
this.getVersionOptions();
this.initEnvironment();
```

但如果用户隐藏了对应列，这些请求和数据处理可能没有必要。

#### 建议

根据当前自定义列决定是否加载：

```js
if (this.hasVisibleField('principalName')) {
  this.getPrincipalOptions([]);
}

if (this.hasVisibleField('versionId')) {
  this.getVersionOptions();
}

if (this.hasVisibleField('environmentMap')) {
  this.initEnvironment();
}
```

或者在用户点击表头过滤器时再懒加载。

#### 收益

- 减少首屏请求数
- 减少初始化数据处理
- 隐藏列不再产生额外成本

---

## P2：大对象深拷贝和 diff 优化

### 15. 关闭 Tab 时避免全量深拷贝和 diff

#### 现状

关闭场景 Tab 时，`ApiAutomation.vue` 会对场景对象做深拷贝和 diff：

```js
let v3 = JSON.parse(JSON.stringify(v2));
...
delta = diff(JSON.parse(JSON.stringify(v1)), JSON.parse(JSON.stringify(v3)));
```

编辑场景加载和保存时也会保存完整深拷贝快照：

```js
scenarioDefinition: JSON.parse(JSON.stringify(this.scenarioDefinition))
```

#### 问题

复杂场景的 `scenarioDefinition` 可能很大，全量深拷贝和 diff 会造成关闭 Tab 或保存后短暂卡顿。

#### 建议

使用 dirty flag 替代关闭时全量 diff：

```js
data() {
  return {
    isDirty: false,
  };
}
```

在以下操作中设置：

- 基础信息修改
- 步骤新增、删除、复制
- 步骤拖拽排序
- 步骤内容修改
- 环境修改
- 参数、Header 修改

```js
markDirty() {
  this.isDirty = true;
}
```

保存成功后：

```js
this.isDirty = false;
```

关闭 Tab 时直接判断 `isDirty`。

如果必须比较内容，可考虑保存轻量 hash，而不是每次关闭都对整棵树做 diff。

#### 收益

- 关闭大场景 Tab 更流畅
- 减少深拷贝内存峰值
- 降低 jsondiffpatch 的 CPU 消耗

---

## 建议实施顺序

### 第一阶段：低风险、收益稳定

1. 修复 `EventBus` 解绑问题
2. 降低模块树和表格搜索耦合
3. 合并列表和定时任务赋值，减少表格二次渲染
4. 标签 tooltip、环境 popover 做轻量化

### 第二阶段：表格结构优化

1. 重构场景表格列渲染方式
2. 隐藏列相关过滤数据懒加载
3. 优化 `MsTable` 中数据变化后的 layout、selection、drag 处理频率

### 第三阶段：大场景编辑优化

1. 步骤树改为轻量摘要行
2. 只有选中步骤渲染完整编辑表单
3. 优化 `ComponentConfig` computed 副作用
4. 减少重复 props

### 第四阶段：调试 / 生成报告链路优化

1. 调试前置处理减少整棵步骤树重复遍历
2. WebSocket 调试消息处理改为索引定位，避免每条消息全树扫描
3. 生成报告链路删除无效深拷贝，减少重复排序和文件扫描

### 第五阶段：内存和交互优化

1. 非激活编辑 Tab 不保留完整组件树
2. 弹窗、抽屉、批量操作组件按需挂载
3. 关闭 Tab 使用 dirty flag 替代全量 deep clone + diff

## 优先级总结

| 优先级 | 优化项 | 预期收益 | 风险 |
| --- | --- | --- | --- |
| P0 | 修复 EventBus 解绑 | 避免重复请求和越用越卡 | 低 |
| P0 | 表格列渲染重构 | 表格首屏和刷新更快 | 中 |
| P0 | 合并 schedule 赋值 | 减少表格二次渲染 | 低/中 |
| P0 | 降低树和表格刷新耦合 | 减少额外请求和树构建 | 中 |
| P1 | 步骤树轻量化 | 大场景编辑明显变快 | 中/高 |
| P1 | ComponentConfig 去副作用 | 减少响应式更新 | 中 |
| P1 | 调试前置处理去重复遍历 | 点击调试响应更快 | 中 |
| P1 | WebSocket 消息索引定位 | 调试期间大场景不卡顿 | 中/高 |
| P1 | 生成报告去无效深拷贝 | 生成报告启动更快 | 低/中 |
| P1 | 非激活 Tab 懒渲染 | 多 Tab 内存下降 | 中 |
| P2 | tooltip/popover 懒渲染 | 表格单元格更轻 | 低 |
| P2 | 过滤项懒加载 | 首屏请求减少 | 低/中 |
| P2 | dirty flag 替代 diff | 关闭大场景不卡顿 | 中 |

## 结论

接口自动化页面当前已经具备部分性能优化基础，例如树搜索防抖和大步骤虚拟树。后续最值得优先处理的是：

1. 修复事件监听泄漏
2. 减少表格列组件数量
3. 避免列表二次渲染
4. 控制模块树刷新频率
5. 将步骤树从“完整表单渲染”改成“摘要行 + 选中详情”
6. 优化调试 / 生成报告链路，尤其是 WebSocket 消息处理时避免每条消息全树扫描

其中步骤树轻量化和 WebSocket 消息索引定位对大场景收益最大，但改动也较大，建议在前几项低风险优化完成后再做。
