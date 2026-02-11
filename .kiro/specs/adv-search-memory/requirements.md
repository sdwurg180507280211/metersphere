# 需求文档

## 简介

在 MeterSphere 高级搜索弹窗中增加搜索条件记忆功能。每个用户在每个业务模块下，自动保存上一次的搜索条件。用户下次打开高级搜索时，自动回填上次的搜索条件，提升重复搜索场景下的操作效率。该功能采用纯前端 localStorage 方案，无需后端改动。

## 术语表

- **AdvSearchBar**：高级搜索组件（`MsTableAdvSearchBar.vue`），被 20+ 页面共享使用的弹窗式搜索组件
- **SearchCondition**：搜索条件对象，包含内置字段（如 name、createTime）和自定义字段（customs 数组）
- **ModuleKey**：模块标识符，用于区分不同业务页面的搜索上下文（如 `ISSUE_LIST`、`TEST_CASE_LIST`）
- **StorageKey**：localStorage 存储键，格式为 `ADV_SEARCH_{userId}_{moduleKey}`
- **SearchMemoryData**：序列化后存储在 localStorage 中的搜索条件数据结构，包含各搜索项的 key、operator、value 信息
- **Consumer**：使用 AdvSearchBar 的业务页面（如缺陷列表、用例列表等）

## 需求

### 需求 1：搜索条件持久化存储

**用户故事：** 作为测试人员，我希望在点击"查询"时自动保存当前搜索条件，以便下次打开高级搜索时能快速恢复。

#### 验收标准

1. WHEN 用户在 AdvSearchBar 中点击"查询"按钮，THE AdvSearchBar SHALL 将当前所有搜索条件序列化并存储到 localStorage 中，存储键格式为 `ADV_SEARCH_{userId}_{moduleKey}`
2. THE SearchMemoryData SHALL 包含每个搜索项的 key、operator 值和 value 值
3. WHEN 用户在 AdvSearchBar 中点击"重置"按钮，THE AdvSearchBar SHALL 删除 localStorage 中对应 StorageKey 的记录
4. THE AdvSearchBar SHALL 使用 JSON 格式序列化 SearchMemoryData

### 需求 2：搜索条件自动回填

**用户故事：** 作为测试人员，我希望打开高级搜索时自动回填上次的搜索条件，以便减少重复输入。

#### 验收标准

1. WHEN AdvSearchBar 首次初始化（`init()` 方法执行）且 localStorage 中存在对应 StorageKey 的记录，THE AdvSearchBar SHALL 自动将存储的搜索条件回填到各搜索项中
2. WHEN 回填搜索条件时，THE AdvSearchBar SHALL 恢复每个搜索项的 key、operator 和 value
3. WHEN 存储的搜索条件中包含当前模板中不存在的字段 key，THE AdvSearchBar SHALL 跳过该字段并继续回填其余字段
4. WHEN localStorage 中不存在对应 StorageKey 的记录，THE AdvSearchBar SHALL 按原有逻辑展示默认搜索条件

### 需求 3：模块隔离与用户隔离

**用户故事：** 作为测试人员，我希望不同模块和不同用户的搜索记忆互不干扰，以便每个场景都能独立记忆。

#### 验收标准

1. THE StorageKey SHALL 同时包含用户 ID 和模块标识符，确保不同用户在同一模块下的搜索记忆互相隔离
2. THE StorageKey SHALL 确保同一用户在不同模块下的搜索记忆互相隔离
3. WHEN Consumer 页面未传入 moduleKey 属性，THE AdvSearchBar SHALL 不启用搜索记忆功能，保持原有行为不变

### 需求 4：向后兼容性

**用户故事：** 作为开发人员，我希望搜索记忆功能对现有页面完全向后兼容，以便不影响未适配的页面。

#### 验收标准

1. THE AdvSearchBar SHALL 将 moduleKey 作为可选属性（prop），默认值为空
2. WHEN moduleKey 属性为空或未传入，THE AdvSearchBar SHALL 完全保持原有行为，不执行任何存储或回填操作
3. WHEN Consumer 页面传入 moduleKey 属性，THE AdvSearchBar SHALL 启用搜索记忆功能

### 需求 5：数据健壮性

**用户故事：** 作为测试人员，我希望搜索记忆功能在各种异常情况下都能正常工作，以便不影响正常使用。

#### 验收标准

1. IF localStorage 中的 SearchMemoryData 解析失败（JSON 格式损坏），THEN THE AdvSearchBar SHALL 忽略该记录并按默认条件展示，不影响正常使用
2. IF localStorage 存储操作失败（如存储空间已满），THEN THE AdvSearchBar SHALL 静默忽略错误，不影响搜索功能的正常执行
3. WHEN 模板字段发生变更（如自定义字段被删除或新增），THE AdvSearchBar SHALL 仅回填当前模板中仍然存在的字段，跳过已不存在的字段

### 需求 6：存储工具函数

**用户故事：** 作为开发人员，我希望搜索记忆的存储和读取逻辑封装为独立的工具函数，以便复用和维护。

#### 验收标准

1. THE 工具函数 SHALL 提供 `saveAdvSearchCondition(userId, moduleKey, conditions)` 方法用于保存搜索条件
2. THE 工具函数 SHALL 提供 `getAdvSearchCondition(userId, moduleKey)` 方法用于读取搜索条件
3. THE 工具函数 SHALL 提供 `clearAdvSearchCondition(userId, moduleKey)` 方法用于清除搜索条件
4. THE 工具函数 SHALL 放置在 `tableUtils.js` 中，与现有 localStorage 工具函数保持一致的代码风格
