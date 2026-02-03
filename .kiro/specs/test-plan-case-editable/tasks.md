# Implementation Plan: 测试计划功能用例可编辑

## Overview

将测试计划中功能用例编辑页面从只读模式改为可编辑模式，通过修改前端组件的 props 实现，复用现有权限和 API。

## Tasks

- [x] 1. 修改 CustomFieldFormItems.vue 支持动态 disabled
  - 新增 `disabled` prop，默认值为 `true`
  - 将内部 `custom-filed-component` 的 `:disabled="true"` 改为 `:disabled="disabled"`
  - _Requirements: 4.1, 4.2_

- [x] 2. 修改 FunctionalTestCaseEdit.vue 主组件
  - [x] 2.1 新增权限计算属性 `canEditCase`
    - 引入 `hasPermission` 工具函数
    - 计算逻辑：`hasPermission('PROJECT_TRACK_CASE:READ+EDIT') && hasProjectPermission && !isReadOnly`
    - _Requirements: 7.1, 7.2, 7.3_
  
  - [x] 2.2 修改前置条件 FormRichTextItem 的 disabled 属性
    - 将 `:disabled="true"` 改为 `:disabled="!canEditCase"`
    - _Requirements: 2.1, 2.2_
  
  - [x] 2.3 修改步骤描述和预期结果 FormRichTextItem 的 disabled 属性
    - 将文本模式下的 `:disabled="true"` 改为 `:disabled="!canEditCase"`
    - _Requirements: 3.1, 3.2_
  
  - [x] 2.4 修改 StepChangeItem 的 disable 属性
    - 将 `:disable="true"` 改为 `:disable="!canEditCase"`
    - _Requirements: 3.1_
  
  - [x] 2.5 修改 CustomFieldFormItems 传入 disabled prop
    - 添加 `:disabled="!canEditCase"` prop
    - _Requirements: 4.1_
  
  - [x] 2.6 修改 TestCaseEditOtherInfo 的 read-only 属性
    - 将 `:read-only="true"` 改为 `:read-only="!canEditCase"`
    - _Requirements: 5.1, 5.2_

- [x] 3. 修改 saveCase 方法支持同步原始用例
  - [x] 3.1 扩展保存参数，包含用例基本信息
    - 添加 name、prerequisite、steps、stepDescription、expectedResult 等字段
    - 添加自定义字段值（使用 buildCustomFields 函数）
    - _Requirements: 1.2, 2.2, 3.3, 4.2_
  
  - [x] 3.2 新增调用原始用例编辑 API
    - 在 `testPlanTestCaseEdit` 成功后调用 `/test/case/edit` API
    - 使用 FormData 格式发送请求
    - 新增 `syncOriginalTestCase()` 方法
    - _Requirements: 6.1_
  
  - [x] 3.3 添加错误处理逻辑
    - 捕获 API 错误并显示提示
    - 保存失败时不关闭页面
    - _Requirements: 6.2, 6.3_

- [x] 3.4 修复评论功能
  - [x] 添加 `@openComment="openComment"` 事件监听到 TestCaseEditOtherInfo
  - [x] 导入并注册 TestCaseComment 组件
  - [x] 添加 TestCaseComment 组件到模板
  - [x] 新增 `openComment()` 和 `getComments()` 方法
  - _Requirements: 5.1_

- [x] 3.5 修复附件上传 bug
  - [x] 修复 TestCaseEditOtherInfo.vue 中 uploadFile 方法参数错误
  - [x] 将 `data` 对象改为 `sourceId` 字符串
  - _Requirements: 5.1_

- [ ] 4. 添加表单验证（可选）
  - [ ] 4.1 添加用例名称必填验证
    - 在保存前检查 name 是否为空或纯空白
    - 显示验证错误提示
    - _Requirements: 1.3_
  
  - [ ] 4.2 添加必填自定义字段验证
    - 遍历自定义字段检查必填项
    - 显示验证错误提示
    - _Requirements: 4.3_

- [ ] 5. Checkpoint - 功能验证
  - 启动前端开发服务器
  - 测试有权限用户可以编辑各字段
  - 测试无权限用户看到只读字段
  - 测试保存后原始用例同步更新
  - 测试评论功能正常
  - 测试附件上传功能正常
  - 确保所有功能正常，如有问题请反馈

- [ ]* 6. 编写单元测试
  - [ ]* 6.1 测试权限控制逻辑
    - 测试 canEditCase 计算属性在不同权限下的返回值
    - _Requirements: 7.1, 7.2_
  
  - [ ]* 6.2 测试表单验证逻辑
    - 测试空用例名称被拒绝
    - 测试必填自定义字段验证
    - _Requirements: 1.3, 4.3_

## Notes

- 任务 6 标记为可选（*），可根据项目测试要求决定是否实现
- 改动集中在 `test-track/frontend/src/business/plan/view/comonents/functional/` 目录
- 复用现有的 `PROJECT_TRACK_CASE:READ+EDIT` 权限，无需后端改动
- 保存时调用现有的 `/test/case/edit` API，无需新增后端接口

## 已完成的代码修改

### FunctionalTestCaseEdit.vue 主要修改：

1. **新增导入**：
   - `buildCustomFields` 从 `metersphere-frontend/src/utils/custom_field`
   - `post` 从 `metersphere-frontend/src/plugins/request`
   - `hasPermission` 从 `metersphere-frontend/src/utils/permission`
   - `TestCaseComment` 组件

2. **新增计算属性 `canEditCase`**：
   ```javascript
   canEditCase() {
     return hasPermission('PROJECT_TRACK_CASE:READ+EDIT') &&
            this.hasProjectPermission &&
            !this.isReadOnly;
   }
   ```

3. **TestCaseEditOtherInfo 组件修改**：
   - `:read-only="true"` → `:read-only="!canEditCase"`
   - 添加 `@openComment="openComment"` 事件监听
   - 添加 `type="edit"` 属性

4. **新增 syncOriginalTestCase() 方法**：
   - 构建完整的用例编辑参数
   - 设置所有自定义字段的 `isEdit: true`
   - 使用 `buildCustomFields` 构建 `addFields`、`editFields`、`requestFields`
   - 解析系统字段（用例等级、责任人、用例状态）
   - 使用 FormData 格式调用 `/test/case/edit` API

5. **saveCase() 方法修改**：
   - 在 `testPlanTestCaseEdit` 成功后，如果有编辑权限则调用 `syncOriginalTestCase()`

6. **新增评论功能**：
   - 添加 `TestCaseComment` 组件到模板
   - 新增 `openComment()` 方法打开评论对话框
   - 新增 `getComments()` 方法刷新评论列表

### TestCaseEditOtherInfo.vue Bug 修复：

1. **修复附件上传 bug**：
   - `uploadFile` 方法中，将 `data` 对象改为 `sourceId` 字符串
   - 原代码：`let data = {"belongId": this.caseId, "belongType": "testcase"};`
   - 修复后：`let sourceId = this.caseId;`
   - 原因：`uploadTestCaseAttachment` 函数期望第二个参数是 `sourceId`（字符串），而不是对象

## 修改文件清单

| 文件路径 | 修改类型 | 说明 |
|---------|---------|------|
| `test-track/frontend/src/business/plan/view/comonents/functional/FunctionalTestCaseEdit.vue` | 修改 | 主组件，添加编辑功能和同步逻辑 |
| `test-track/frontend/src/business/case/components/TestCaseEditOtherInfo.vue` | 修复 | 修复附件上传参数错误 |
| `test-track/frontend/src/business/common/CustomFieldFormItems.vue` | 修改 | 添加 disabled prop 支持 |
