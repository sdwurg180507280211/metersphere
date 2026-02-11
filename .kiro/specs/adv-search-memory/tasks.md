# 实施计划：高级搜索记忆功能

## 概述

基于纯前端 localStorage 方案，为 MsTableAdvSearchBar 组件增加搜索条件记忆能力。改动集中在 4 个文件（tableUtils.js、MsTableAdvSearchBar.vue、MsSearch.vue、MsTableHeader.vue），加上 Consumer 页面传入 moduleKey。

## 任务

- [ ] 1. 实现 localStorage 工具函数
  - [ ] 1.1 在 `framework/sdk-parent/frontend/src/utils/tableUtils.js` 中新增三个工具函数
    - 新增 `saveAdvSearchCondition(userId, moduleKey, conditions)` — 将搜索条件序列化为 JSON 并存入 localStorage，key 格式为 `ADV_SEARCH_{userId}_{moduleKey}`，用 try-catch 包裹 setItem 防止存储失败
    - 新增 `getAdvSearchCondition(userId, moduleKey)` — 从 localStorage 读取并解析 JSON，解析失败返回 null
    - 新增 `clearAdvSearchCondition(userId, moduleKey)` — 从 localStorage 删除对应 key
    - 添加详细中文注释，保持与现有 `saveLastTableSortField`、`saveCustomTableWidth` 一致的代码风格
    - _Requirements: 1.1, 1.3, 1.4, 5.1, 5.2, 6.1, 6.2, 6.3, 6.4_

  - [ ]* 1.2 为工具函数编写属性测试
    - **Property 1: 搜索条件存取往返一致性**
    - **Validates: Requirements 1.1, 1.2, 1.4, 2.1, 2.2**
    - **Property 2: 重置操作清除存储数据**
    - **Validates: Requirements 1.3**
    - **Property 4: 存储键隔离性**
    - **Validates: Requirements 3.1, 3.2**
    - **Property 5: 损坏的 JSON 数据安全降级**
    - **Validates: Requirements 5.1**

- [ ] 2. 修改 MsTableAdvSearchBar 组件核心逻辑
  - [ ] 2.1 在 `MsTableAdvSearchBar.vue` 中新增 `moduleKey` prop 和记忆相关方法
    - 新增 `moduleKey` prop（String 类型，默认空字符串）
    - 新增 `isMemoryEnabled()` 方法 — 判断 moduleKey 是否非空
    - 新增 `serializeConditions()` 方法 — 遍历 `optional.components`，提取每项的 key、operator.value、value、custom 标记，返回 `{ version: 1, items: [...] }` 结构
    - 新增 `restoreSearchCondition(savedData)` 方法 — 遍历 savedData.items，在 `condition.components` 中查找匹配 key 的组件，找到则克隆并设置 operator.value 和 value 加入 optional.components，未找到则跳过
    - 在文件顶部 import `saveAdvSearchCondition`、`getAdvSearchCondition`、`clearAdvSearchCondition` 和 `getCurrentUserId`
    - _Requirements: 2.1, 2.2, 2.3, 3.3, 4.1, 4.2, 4.3, 5.3_

  - [ ] 2.2 修改 `search()` 方法，在搜索执行后保存条件
    - 在 `this.visible = false` 之前，判断 `isMemoryEnabled()`，若启用则调用 `saveAdvSearchCondition(getCurrentUserId(), this.moduleKey, this.serializeConditions())`
    - _Requirements: 1.1, 1.2_

  - [ ] 2.3 修改 `reset()` 方法，在重置后清除存储
    - 在 `this.$emit('search')` 之前，判断 `isMemoryEnabled()`，若启用则调用 `clearAdvSearchCondition(getCurrentUserId(), this.moduleKey)`
    - _Requirements: 1.3_

  - [ ] 2.4 修改 `init()` 方法，在初始化后回填条件
    - 在现有 init 逻辑末尾（slice 和 disable 设置之后），判断 `isMemoryEnabled()`，若启用则调用 `getAdvSearchCondition(getCurrentUserId(), this.moduleKey)` 获取存储数据
    - 若返回非 null，调用 `restoreSearchCondition()` 回填
    - _Requirements: 2.1, 2.4_

  - [ ]* 2.5 为回填逻辑编写属性测试
    - **Property 3: 不存在的字段在回填时被跳过**
    - **Validates: Requirements 2.3, 5.3**

- [ ] 3. Checkpoint — 确保核心逻辑测试通过
  - 确保所有测试通过，如有问题请向用户确认。

- [ ] 4. 透传 moduleKey 属性到组件链
  - [ ] 4.1 修改 `MsSearch.vue`，新增 `moduleKey` prop 并透传给 `MsTableAdvSearchBar`
    - 新增 `moduleKey` prop（String 类型，默认空字符串）
    - 在模板中 `<ms-table-adv-search-bar>` 上添加 `:module-key="moduleKey"`
    - _Requirements: 4.1, 4.2_

  - [ ] 4.2 修改 `MsTableHeader.vue`，新增 `moduleKey` prop 并透传给 `MsSearch`
    - 新增 `moduleKey` prop（String 类型，默认空字符串）
    - 在模板中 `<ms-search>` 上添加 `:module-key="moduleKey"`
    - _Requirements: 4.1, 4.2_

- [ ] 5. 在 Consumer 页面接入搜索记忆
  - [ ] 5.1 修改 `IssueList.vue`（缺陷列表）作为首个接入页面
    - 在 `<ms-table-header>` 上添加 `:module-key="tableHeaderKey"`，复用已有的 `tableHeaderKey: "ISSUE_LIST"`
    - _Requirements: 4.3_

- [ ] 6. Final checkpoint — 确保所有测试通过
  - 确保所有测试通过，如有问题请向用户确认。

## 备注

- 标记 `*` 的任务为可选任务，可跳过以加快 MVP 进度
- 每个任务引用了具体的需求编号以便追溯
- Consumer 页面（任务 5）仅以 IssueList 为示例，其他页面可按相同模式逐步接入
- 所有代码修改需添加详细中文注释
