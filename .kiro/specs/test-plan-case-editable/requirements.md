# Requirements Document

## Introduction

本需求文档描述了 MeterSphere 测试计划中功能用例编辑页面从只读模式改为可编辑模式的功能需求。当前测试计划中的功能用例详情页面（`FunctionalTestCaseEdit.vue`）大部分字段为只读状态，用户希望能够直接在测试计划执行过程中编辑用例的核心信息，而无需跳转到用例管理模块。

## Glossary

- **Test_Plan_Case_Editor**: 测试计划功能用例编辑器组件，即 `FunctionalTestCaseEdit.vue`
- **Test_Case**: 功能测试用例实体
- **Custom_Field**: 用例模板中定义的自定义字段
- **Step_Result**: 测试步骤的实际执行结果
- **Other_Info**: 用例的其他信息，包括备注、关联测试、关联需求、关联缺陷、附件等

## Requirements

### Requirement 1: 用例基本信息可编辑

**User Story:** As a 测试执行人员, I want 在测试计划执行页面直接编辑用例的基本信息, so that 我可以在执行过程中及时修正用例内容而无需切换页面。

#### Acceptance Criteria

1. WHEN 用户打开测试计划功能用例编辑页面 THEN THE Test_Plan_Case_Editor SHALL 显示用例名称为可编辑状态
2. WHEN 用户修改用例名称并保存 THEN THE Test_Plan_Case_Editor SHALL 同步更新原始用例库中的用例名称
3. WHEN 用例名称为空时尝试保存 THEN THE Test_Plan_Case_Editor SHALL 阻止保存并显示验证错误提示

### Requirement 2: 前置条件可编辑

**User Story:** As a 测试执行人员, I want 编辑用例的前置条件, so that 我可以根据实际测试环境补充或修正前置条件。

#### Acceptance Criteria

1. WHEN 用户打开测试计划功能用例编辑页面 THEN THE Test_Plan_Case_Editor SHALL 显示前置条件富文本编辑器为可编辑状态
2. WHEN 用户修改前置条件并保存 THEN THE Test_Plan_Case_Editor SHALL 同步更新原始用例库中的前置条件

### Requirement 3: 测试步骤可编辑

**User Story:** As a 测试执行人员, I want 编辑用例的测试步骤描述和预期结果, so that 我可以在执行过程中完善测试步骤。

#### Acceptance Criteria

1. WHEN 用例为步骤模式（STEP）THEN THE Test_Plan_Case_Editor SHALL 显示步骤描述和预期结果为可编辑状态
2. WHEN 用例为文本模式（TEXT）THEN THE Test_Plan_Case_Editor SHALL 显示步骤描述和预期结果富文本编辑器为可编辑状态
3. WHEN 用户修改步骤信息并保存 THEN THE Test_Plan_Case_Editor SHALL 同步更新原始用例库中的步骤信息
4. WHEN 用户添加或删除测试步骤 THEN THE Test_Plan_Case_Editor SHALL 同步更新原始用例库中的步骤列表

### Requirement 4: 自定义字段可编辑

**User Story:** As a 测试执行人员, I want 编辑用例的自定义字段, so that 我可以更新用例的分类、优先级等属性。

#### Acceptance Criteria

1. WHEN 用户打开测试计划功能用例编辑页面 THEN THE Test_Plan_Case_Editor SHALL 显示所有自定义字段为可编辑状态
2. WHEN 用户修改自定义字段值并保存 THEN THE Test_Plan_Case_Editor SHALL 同步更新原始用例库中的自定义字段值
3. IF 自定义字段有必填验证规则 THEN THE Test_Plan_Case_Editor SHALL 在保存时执行验证

### Requirement 5: 其他信息可编辑

**User Story:** As a 测试执行人员, I want 编辑用例的备注和关联信息, so that 我可以补充测试相关的备注和关联关系。

#### Acceptance Criteria

1. WHEN 用户打开测试计划功能用例编辑页面 THEN THE Test_Plan_Case_Editor SHALL 显示备注为可编辑状态
2. WHEN 用户打开测试计划功能用例编辑页面 THEN THE Test_Plan_Case_Editor SHALL 显示关联需求为可编辑状态
3. WHEN 用户修改备注或关联需求并保存 THEN THE Test_Plan_Case_Editor SHALL 同步更新原始用例库中的对应信息

### Requirement 6: 数据同步机制

**User Story:** As a 系统管理员, I want 测试计划中的用例编辑能同步到原始用例库, so that 数据保持一致性。

#### Acceptance Criteria

1. WHEN 用户在测试计划中编辑用例并保存 THEN THE Test_Plan_Case_Editor SHALL 调用后端 API 同步更新原始用例
2. IF 原始用例更新失败 THEN THE Test_Plan_Case_Editor SHALL 显示错误提示并回滚本地修改
3. WHEN 保存成功 THEN THE Test_Plan_Case_Editor SHALL 显示成功提示

### Requirement 7: 权限控制

**User Story:** As a 项目管理员, I want 控制用户在测试计划中编辑用例的权限, so that 只有授权用户才能修改用例内容。

#### Acceptance Criteria

1. WHEN 用户没有用例编辑权限 THEN THE Test_Plan_Case_Editor SHALL 保持所有字段为只读状态
2. WHEN 用户拥有用例编辑权限 THEN THE Test_Plan_Case_Editor SHALL 显示字段为可编辑状态
3. THE Test_Plan_Case_Editor SHALL 复用现有的 `PROJECT_TRACK_CASE:READ+EDIT` 权限进行控制

## Implementation Status

### 已实现的需求

| 需求 | 状态 | 说明 |
|-----|------|------|
| Requirement 1: 用例基本信息可编辑 | ✅ 已实现 | 用例名称可编辑，保存时同步到原始用例 |
| Requirement 2: 前置条件可编辑 | ✅ 已实现 | 富文本编辑器可编辑 |
| Requirement 3: 测试步骤可编辑 | ✅ 已实现 | 步骤模式和文本模式均支持 |
| Requirement 4: 自定义字段可编辑 | ✅ 已实现 | 所有自定义字段可编辑 |
| Requirement 5: 其他信息可编辑 | ✅ 已实现 | 备注、关联需求、评论、附件均可编辑 |
| Requirement 6: 数据同步机制 | ✅ 已实现 | 保存时自动同步到原始用例库 |
| Requirement 7: 权限控制 | ✅ 已实现 | 复用 PROJECT_TRACK_CASE:READ+EDIT 权限 |

### 待验证项

- [ ] 用户功能验证（Checkpoint 任务 5）
- [ ] 表单验证（可选任务 4）
- [ ] 单元测试（可选任务 6）

### 已修复的 Bug

1. **评论功能不生效** - 已修复：添加 TestCaseComment 组件和相关事件处理
2. **附件上传不生效** - 已修复：修正 uploadFile 方法的参数传递
