# 实现计划：高级搜索条件记忆功能

## 概述

基于设计文档，将搜索条件记忆功能拆分为工具函数实现、两个组件改造、测试三个阶段。工具函数先行，组件改造复用同一套函数，最后通过属性测试和单元测试验证正确性。

## 任务

- [x] 1. 在 tableUtils.js 中实现搜索记忆工具函数
  - [x] 1.1 实现 `_buildAdvSearchStorageKey`、`saveAdvSearchCondition`、`getAdvSearchCondition`、`clearAdvSearchCondition` 四个函数
    - 在 `framework/sdk-parent/frontend/src/utils/tableUtils.js` 末尾添加
    - `saveAdvSearchCondition`：从 components 数组提取 key、operator.value、value，序列化为 JSON 存入 localStorage，try-catch 包裹
    - `getAdvSearchCondition`：从 localStorage 读取并解析 JSON，解析失败返回 null，try-catch 包裹
    - `clearAdvSearchCondition`：删除对应 localStorage 记录，try-catch 包裹
    - 添加详细中文注释
    - _Requirements: 1.1, 1.2, 1.3, 6.1, 6.2, 6.3, 6.4_

  - [ ]* 1.2 编写工具函数的属性测试
    - 在 `framework/sdk-parent/frontend/src/utils/__tests__/advSearchMemory.test.js` 中编写
    - 使用 fast-check 库
    - **Property 1: 保存-读取往返一致性**
    - **Validates: Requirements 1.1, 1.2, 2.1, 2.2**
    - **Property 2: 存储隔离性**
    - **Validates: Requirements 3.1, 3.2**
    - **Property 3: 清除有效性**
    - **Validates: Requirements 1.3**

  - [ ]* 1.3 编写工具函数的单元测试
    - 在同一测试文件中编写
    - 测试 localStorage JSON 损坏时返回 null（需求 5.1）
    - 测试 localStorage 写入异常时静默忽略（需求 5.2）
    - 测试空组件数组的保存和读取
    - _Requirements: 5.1, 5.2_

- [x] 2. 改造 MsTableAdvSearchBar 组件（旧版）
  - [x] 2.1 添加 moduleKey prop 并改造 search/reset/init 方法
    - 修改 `framework/sdk-parent/frontend/src/components/search/MsTableAdvSearchBar.vue`
    - 添加 `moduleKey` 可选 prop，默认值为空字符串
    - 在 `search()` 方法末尾添加 `saveAdvSearchCondition` 调用（仅当 moduleKey 非空且 userId 有效时）
    - 在 `reset()` 方法末尾添加 `clearAdvSearchCondition` 调用
    - 在 `init()` 方法中 slice 截取后添加回填逻辑，调用 `getAdvSearchCondition` 和 `_restoreSearchConditions`
    - 实现 `_restoreSearchConditions` 方法：遍历已保存条件，匹配 optional.components 中的 key，恢复 operator.value 和 value，跳过不存在的字段
    - 导入 `getCurrentUserId` 和三个工具函数
    - 添加详细中文注释
    - _Requirements: 1.1, 1.3, 2.1, 2.2, 2.3, 3.3, 4.1, 4.2, 4.3, 5.3_

- [x] 3. 改造 MsTableAdvSearch 组件（新版）
  - [x] 3.1 添加 moduleKey prop 并改造 search/reset/init 方法
    - 修改 `framework/sdk-parent/frontend/src/components/new-ui/MsTableAdvSearch.vue`
    - 与 2.1 完全相同的改动逻辑
    - 添加 `moduleKey` 可选 prop，默认值为空字符串
    - 在 `search()` 方法末尾添加 `saveAdvSearchCondition` 调用
    - 在 `reset()` 方法末尾添加 `clearAdvSearchCondition` 调用
    - 在 `init()` 方法中添加回填逻辑
    - 实现 `_restoreSearchConditions` 方法
    - 导入 `getCurrentUserId` 和三个工具函数
    - 添加详细中文注释
    - _Requirements: 1.1, 1.3, 2.1, 2.2, 2.3, 3.3, 4.1, 4.2, 4.3, 5.3_

  - [ ]* 3.2 编写回填逻辑的属性测试
    - 在测试文件中添加
    - **Property 4: 回填仅恢复已知字段**
    - **Validates: Requirements 2.3, 5.3**

- [x] 4. 检查点 - 确保所有测试通过
  - 确保所有测试通过，如有问题请向用户确认。

## 备注

- 标记 `*` 的子任务为可选，可跳过以加速 MVP 交付
- 每个任务引用了具体的需求编号，确保可追溯性
- 工具函数优先实现，两个组件复用同一套函数
- 属性测试验证通用正确性，单元测试覆盖边界情况
