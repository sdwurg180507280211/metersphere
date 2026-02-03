# Design Document: 测试计划功能用例可编辑

## Overview

本设计文档描述了将测试计划中功能用例编辑页面从只读模式改为可编辑模式的技术方案。核心思路是复用现有的用例编辑组件和 API，在测试计划执行页面中启用编辑功能，并将修改同步到原始用例库。

### 设计原则

遵循 MeterSphere 二次开发原则：
- **改动面小**：仅修改 `FunctionalTestCaseEdit.vue` 及相关子组件的 props
- **边界清晰**：改动集中在 `test-track/frontend/src/business/plan/view/comonents/functional/` 目录
- **可回滚**：通过 props 控制编辑状态，不影响原有只读逻辑

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    FunctionalTestCaseEdit.vue                    │
│  (测试计划功能用例编辑页面 - 主组件)                              │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │ FormRichTextItem│  │ StepChangeItem  │  │CustomFieldForm  │  │
│  │ (前置条件)      │  │ (步骤切换)      │  │Items (自定义字段)│  │
│  │ disabled→false  │  │ disable→false   │  │ disabled→false  │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
│  ┌─────────────────┐  ┌─────────────────┐                       │
│  │TestCaseEditOther│  │TestPlanCaseStep │                       │
│  │Info (其他信息)  │  │ResultsItem(步骤)│                       │
│  │ readOnly→false  │  │ isReadOnly→false│                       │
│  └─────────────────┘  └─────────────────┘                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         API Layer                                │
├─────────────────────────────────────────────────────────────────┤
│  现有 API:                                                       │
│  - POST /test/plan/case/edit  (保存执行结果)                     │
│  新增调用:                                                       │
│  - POST /test/case/edit       (同步更新原始用例)                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Backend Services                           │
├─────────────────────────────────────────────────────────────────┤
│  TestPlanTestCaseService.editTestCase()                          │
│       │                                                          │
│       ├──► 保存测试计划用例执行结果                              │
│       │                                                          │
│       └──► TestCaseService.edit() (新增调用，同步原始用例)       │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. FunctionalTestCaseEdit.vue (主组件修改)

**当前状态**：大部分字段通过 `:disabled="true"` 或 `:read-only="true"` 设置为只读

**修改方案**：
```javascript
// 新增计算属性，根据权限控制编辑状态
computed: {
  // 是否允许编辑用例内容（复用现有权限）
  canEditCase() {
    return hasPermission('PROJECT_TRACK_CASE:READ+EDIT') && 
           this.hasProjectPermission && 
           !this.isReadOnly;
  }
}
```

**子组件 props 修改**：

| 组件 | 当前 prop | 修改后 |
|------|-----------|--------|
| FormRichTextItem (前置条件) | `:disabled="true"` | `:disabled="!canEditCase"` |
| FormRichTextItem (步骤描述) | `:disabled="true"` | `:disabled="!canEditCase"` |
| FormRichTextItem (预期结果) | `:disabled="true"` | `:disabled="!canEditCase"` |
| StepChangeItem | `:disable="true"` | `:disable="!canEditCase"` |
| CustomFieldFormItems | 内部 `:disabled="true"` | 传入 `:disabled="!canEditCase"` |
| TestCaseEditOtherInfo | `:read-only="true"` | `:read-only="!canEditCase"` |

### 2. CustomFieldFormItems.vue (自定义字段组件修改)

**当前状态**：内部硬编码 `:disabled="true"`

**修改方案**：
```vue
<template>
  <custom-filed-component
    :disabled="disabled"  <!-- 改为接收 props -->
    :data="item"
    :form="{}"
    prop="defaultValue"
  />
</template>

<script>
props: {
  // 新增 disabled prop
  disabled: {
    type: Boolean,
    default: true
  }
}
</script>
```

### 3. TestPlanCaseStepResultsItem.vue (步骤结果组件)

**当前状态**：步骤描述和预期结果只读，仅实际结果可编辑

**修改方案**：新增 `canEditStep` prop 控制步骤描述和预期结果的编辑状态

### 4. 新增前端 API 方法

```javascript
// test-track/frontend/src/api/testCase.js
export function editTestCaseFromPlan(param) {
  // 使用 FormData 格式，与原有 /test/case/edit 接口保持一致
  const formData = new FormData();
  formData.append('request', new Blob([JSON.stringify(param)], { type: 'application/json' }));
  return post('/test/case/edit', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}
```

## Data Models

### 保存请求数据结构

```typescript
interface TestPlanCaseEditRequest {
  // 测试计划用例 ID
  id: string;
  // 原始用例 ID
  caseId: string;
  // 执行状态
  status: string;
  // 步骤执行结果
  results: StepResult[];
  // 备注
  remark: string;
  // 评论
  comment?: string;
  
  // === 新增：同步到原始用例的字段 ===
  // 用例名称
  name?: string;
  // 前置条件
  prerequisite?: string;
  // 步骤（JSON 字符串）
  steps?: string;
  // 步骤描述（文本模式）
  stepDescription?: string;
  // 预期结果（文本模式）
  expectedResult?: string;
  // 自定义字段
  customFields?: CustomFieldValue[];
  // 需求 ID
  demandId?: string;
  // 需求名称
  demandName?: string;
  
  // 标记是否需要同步原始用例
  syncToOriginal?: boolean;
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: 权限控制一致性
*For any* 用户访问测试计划功能用例编辑页面，如果用户没有 `PROJECT_TRACK_CASE:READ+EDIT` 权限，则所有字段应保持只读状态。
**Validates: Requirements 7.1, 7.2**

### Property 2: 数据同步完整性
*For any* 在测试计划中编辑并保存的用例，原始用例库中对应用例的所有被修改字段值应与编辑后的值一致。
**Validates: Requirements 6.1**

### Property 3: 保存失败回滚
*For any* 保存操作，如果后端返回错误，前端应显示错误提示且本地数据应恢复到编辑前的状态。
**Validates: Requirements 6.2**

### Property 4: 必填字段验证
*For any* 保存操作，如果用例名称为空或必填自定义字段为空，系统应阻止保存并显示验证错误。
**Validates: Requirements 1.3, 4.3**

## Error Handling

### 前端错误处理

1. **权限不足**：检测到无编辑权限时，保持只读状态，不显示编辑控件
2. **保存失败**：
   - 显示错误提示（使用 `this.$error()`）
   - 不关闭编辑页面，允许用户重试
3. **验证失败**：
   - 高亮显示验证失败的字段
   - 显示具体的验证错误信息

### 后端错误处理

1. **原始用例不存在**：返回 404，前端提示"原始用例已被删除"
2. **并发修改冲突**：返回 409，前端提示"用例已被其他用户修改，请刷新后重试"
3. **权限验证失败**：返回 403，前端提示"无权限编辑此用例"

## Testing Strategy

### 单元测试

1. **权限控制测试**
   - 测试有权限用户看到可编辑字段
   - 测试无权限用户看到只读字段

2. **数据验证测试**
   - 测试空用例名称被拒绝
   - 测试必填自定义字段验证

### 集成测试

1. **保存流程测试**
   - 测试修改用例名称后保存，验证原始用例同步更新
   - 测试修改步骤后保存，验证原始用例同步更新

2. **错误处理测试**
   - 测试网络错误时的错误提示
   - 测试并发修改时的冲突处理

### 手动测试场景

1. 打开测试计划 → 功能用例 → 点击用例进入编辑页面
2. 验证各字段可编辑状态
3. 修改用例名称、前置条件、步骤等
4. 保存后验证原始用例库中的用例已同步更新

## Implementation Summary

### 已完成的核心修改

#### 1. FunctionalTestCaseEdit.vue

**新增导入**：
```javascript
import {buildCustomFields} from "metersphere-frontend/src/utils/custom_field";
import {hasPermission} from "metersphere-frontend/src/utils/permission";
import {post} from "metersphere-frontend/src/plugins/request";
import TestCaseComment from "@/business/case/components/TestCaseComment";
```

**新增计算属性**：
```javascript
canEditCase() {
  return hasPermission('PROJECT_TRACK_CASE:READ+EDIT') &&
         this.hasProjectPermission &&
         !this.isReadOnly;
}
```

**组件 props 修改**：
- `CustomFieldFormItems`: `:disabled="!canEditCase"`
- `FormRichTextItem` (前置条件): `:disabled="!canEditCase"`
- `StepChangeItem`: `:disable="!canEditCase"`
- `FormRichTextItem` (步骤描述/预期结果): `:disabled="!canEditCase"`
- `TestCaseEditOtherInfo`: `:read-only="!canEditCase"`, `type="edit"`

**新增 syncOriginalTestCase() 方法**：
- 构建用例编辑参数（name, prerequisite, steps, stepDescription, expectedResult 等）
- 处理自定义字段（设置 isEdit: true，使用 buildCustomFields）
- 解析系统字段（用例等级、责任人、用例状态）
- 使用 FormData 格式调用 `/test/case/edit` API

**新增评论功能**：
- 添加 `TestCaseComment` 组件
- 新增 `openComment()` 和 `getComments()` 方法

#### 2. TestCaseEditOtherInfo.vue Bug 修复

**修复附件上传参数错误**：
```javascript
// 修复前（错误）
let data = {"belongId": this.caseId, "belongType": "testcase"};
uploadTestCaseAttachment(file, data, ...);

// 修复后（正确）
let sourceId = this.caseId;
uploadTestCaseAttachment(file, sourceId, ...);
```

### 修改文件清单

| 文件 | 修改类型 | 说明 |
|-----|---------|------|
| `FunctionalTestCaseEdit.vue` | 功能增强 | 添加编辑功能、同步逻辑、评论功能 |
| `TestCaseEditOtherInfo.vue` | Bug 修复 | 修复附件上传参数错误 |
| `CustomFieldFormItems.vue` | 功能增强 | 添加 disabled prop 支持 |
