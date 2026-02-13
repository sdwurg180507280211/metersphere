# 需求文档

## 简介

在 MeterSphere 系统设置的公告设置页面中增加"需求上线内容"管理功能，允许管理员录入每次需求上线的内容记录（标题、内容详情），并在测试跟踪首页以列表形式展示最近5条上线记录。点击记录弹出对话框查看完整内容。本质上是一个简单的内容发布/展示系统：后台录入，前台展示。

## 术语表

- **Release_Note**：需求上线记录实体，存储在独立数据库表 `release_note` 中，包含标题、内容、创建人、创建时间等字段
- **Release_Notes_Manager**：需求上线内容管理组件，位于系统设置公告设置 Tab 页内，用于 CRUD 操作
- **Release_Notes_Board**：需求上线内容展示面板，位于测试跟踪首页，以列表形式展示最近的上线记录
- **System_Setting_Page**：系统参数设置页面，位于 `系统设置 -> 系统 -> 系统参数设置`
- **Track_Home**：测试跟踪首页，位于 `测试跟踪 -> 首页`

## 需求

### 需求 1：管理入口集成

**用户故事：** 作为系统管理员，我希望在系统参数设置的公告设置标签页中看到需求上线内容管理入口，以便在统一的位置管理公告和上线记录。

#### 验收标准

1. WHEN 管理员访问系统参数设置页面并切换到"公告设置"Tab THEN System_Setting_Page SHALL 在现有公告配置下方显示 Release_Notes_Manager 区域
2. THE Release_Notes_Manager SHALL 仅对具有 `SYSTEM_SETTING:READ` 权限的用户可见
3. WHILE 用户不具有 `SYSTEM_SETTING:READ+EDIT` 权限 THEN Release_Notes_Manager SHALL 隐藏新增、编辑、删除操作按钮

### 需求 2：上线内容录入

**用户故事：** 作为系统管理员，我希望能够录入每次需求上线的内容记录，以便团队成员了解最新的上线变更。

#### 验收标准

1. WHEN 管理员点击"新增上线记录"按钮 THEN Release_Notes_Manager SHALL 弹出录入对话框，包含以下字段：标题（必填，最长100字符）、内容详情（必填，支持多行文本，最长2000字符）
2. WHEN 管理员填写完整信息并点击确认 THEN THE System SHALL 将 Release_Note 保存到数据库并刷新列表
3. IF 必填字段为空 THEN THE System SHALL 阻止提交并在对应字段显示校验错误提示
4. WHEN 保存成功 THEN THE System SHALL 显示成功提示信息
5. IF 保存操作失败 THEN THE System SHALL 显示错误提示信息

### 需求 3：上线内容管理

**用户故事：** 作为系统管理员，我希望能够查看、编辑和删除已有的上线记录，以便维护上线内容的准确性。

#### 验收标准

1. WHEN Release_Notes_Manager 加载时 THEN THE System SHALL 以表格形式展示所有 Release_Note 记录，按创建时间倒序排列
2. THE 表格 SHALL 显示以下列：标题、创建人、创建时间、操作
3. WHEN 管理员点击编辑按钮 THEN Release_Notes_Manager SHALL 弹出编辑对话框，预填充当前记录数据
4. WHEN 管理员点击删除按钮 THEN Release_Notes_Manager SHALL 弹出确认对话框，确认后删除该记录并刷新列表
5. WHEN 记录列表超过10条 THEN Release_Notes_Manager SHALL 提供分页功能

### 需求 4：上线内容展示

**用户故事：** 作为测试团队成员，我希望在测试跟踪首页看到最近的需求上线内容，以便快速了解最新的系统变更。

#### 验收标准

1. WHEN 用户访问测试跟踪首页 THEN Track_Home SHALL 在"过去7天测试计划失败用例 TOP 10"区域下方显示 Release_Notes_Board
2. WHEN Release_Notes_Board 加载时 THEN THE System SHALL 获取最近5条 Release_Note 记录（按创建时间倒序）并以列表形式展示
3. THE Release_Notes_Board SHALL 每条记录显示为如下格式：标题行为 `{create_time 格式化为 YYYY年MM月DD日}上线公告`，下方显示 `创建时间: YYYY-MM-DD` 和 `创建者: {creator_name}`
4. WHEN 用户点击某条记录 THEN Release_Notes_Board SHALL 弹出对话框显示完整的上线内容详情
5. WHEN 无上线记录时 THEN Release_Notes_Board SHALL 显示"暂无上线记录"空状态提示

### 需求 5：后端 API

**用户故事：** 作为开发者，我希望有完整的 RESTful API 支持上线记录的增删改查，以便前端组件能够正常交互。

#### 验收标准

1. THE System SHALL 提供 `POST /release-note/add` 接口用于创建 Release_Note，接口需要 `SYSTEM_SETTING:READ+EDIT` 权限
2. THE System SHALL 提供 `POST /release-note/update` 接口用于更新 Release_Note，接口需要 `SYSTEM_SETTING:READ+EDIT` 权限
3. THE System SHALL 提供 `GET /release-note/delete/{id}` 接口用于删除 Release_Note，接口需要 `SYSTEM_SETTING:READ+EDIT` 权限
4. THE System SHALL 提供 `POST /release-note/list/{goPage}/{pageSize}` 接口用于分页查询 Release_Note 列表，按创建时间倒序排列
5. THE System SHALL 提供 `GET /release-note/recent/{limit}` 接口用于获取最近 N 条 Release_Note 记录（供 Track_Home 使用，无需管理权限）
6. THE System SHALL 提供 `GET /release-note/get/{id}` 接口用于获取单条 Release_Note 详情
7. WHEN 创建或更新 Release_Note 时 THEN THE System SHALL 自动记录创建人和创建时间
8. IF 请求的 Release_Note 不存在 THEN THE System SHALL 返回 HTTP 404 状态码

### 需求 6：数据库设计

**用户故事：** 作为开发者，我希望有合理的数据库表结构存储上线记录，以便数据持久化和高效查询。

#### 验收标准

1. THE System SHALL 创建 `release_note` 表，包含以下字段：id（VARCHAR(50) 主键）、title（VARCHAR(100) 公告标题）、content（TEXT 上线内容）、creator（VARCHAR(50) 创建人ID）、create_time（BIGINT 创建时间戳）、update_time（BIGINT 更新时间戳）
2. THE `release_note` 表 SHALL 在 `create_time` 字段上建立索引以支持按时间排序查询
3. THE System SHALL 通过 Flyway 数据库迁移脚本创建该表
