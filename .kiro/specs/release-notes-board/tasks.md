# Implementation Plan: 需求上线内容管理（Release Notes Board）

## Overview

基于 MeterSphere 现有微服务架构，在 SDK 层新增 ReleaseNote 的后端 CRUD 能力，在 system-setting 前端新增管理组件，在 test-track 前端新增展示组件。按照"数据层 → 后端 API → 前端管理 → 前端展示"的顺序递增实现。

## Tasks

- [ ] 1. 数据库表和实体类
  - [ ] 1.1 创建 Flyway 迁移脚本 `system-setting/backend/src/main/resources/db/migration/V149__release_note.sql`
    - 创建 `release_note` 表：id(VARCHAR(50) PK)、title(VARCHAR(100))、content(TEXT)、creator(VARCHAR(50))、create_time(BIGINT)、update_time(BIGINT)
    - 创建索引 `idx_create_time` ON `create_time` DESC
    - _Requirements: 6.1, 6.2, 6.3_
  - [ ] 1.2 创建实体类 `framework/sdk-parent/domain/src/main/java/io/metersphere/base/domain/ReleaseNote.java`
    - 使用 @Data 注解，字段：id, title, content, creator, createTime, updateTime
    - _Requirements: 6.1_
  - [ ] 1.3 创建 MyBatis Mapper 接口和 XML
    - `framework/sdk-parent/sdk/src/main/java/io/metersphere/base/mapper/ReleaseNoteMapper.java`
    - `framework/sdk-parent/sdk/src/main/resources/mapper/ReleaseNoteMapper.xml`
    - 包含标准 CRUD 方法 + `selectRecent(int limit)` 自定义查询
    - _Requirements: 5.4, 5.5, 5.6_

- [ ] 2. 后端 Service 和 Controller
  - [ ] 2.1 创建 `framework/sdk-parent/sdk/src/main/java/io/metersphere/service/ReleaseNoteService.java`
    - 实现 add（自动填充 id/creator/createTime/updateTime）、update、delete、list（PageHelper 分页）、recent（limit 查询）、get 方法
    - 必填字段校验（title、content 非空）
    - 记录不存在时抛出 MSException
    - _Requirements: 2.2, 2.3, 3.1, 5.7, 5.8_
  - [ ] 2.2 创建 `framework/sdk-parent/sdk/src/main/java/io/metersphere/controller/ReleaseNoteController.java`
    - 6 个端点：add、update、delete、list、recent、get
    - add/update/delete 需要 @RequiresPermissions(SYSTEM_SETTING_READ_EDIT)
    - list/recent/get 无需特殊权限
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_
  - [ ]* 2.3 编写 ReleaseNoteService 属性测试：CRUD 往返一致性
    - **Property 1: CRUD round-trip consistency**
    - **Validates: Requirements 2.2, 5.6, 5.7**
  - [ ]* 2.4 编写 ReleaseNoteService 属性测试：必填字段校验
    - **Property 2: Required field validation**
    - **Validates: Requirements 2.3**
  - [ ]* 2.5 编写 ReleaseNoteService 属性测试：列表按创建时间倒序排列
    - **Property 3: List ordering by create_time desc**
    - **Validates: Requirements 3.1, 4.2, 5.4, 5.5**
  - [ ]* 2.6 编写 ReleaseNoteService 属性测试：分页正确性
    - **Property 4: Pagination correctness**
    - **Validates: Requirements 3.5**
  - [ ]* 2.7 编写 ReleaseNoteService 属性测试：删除后不可查
    - **Property 5: Delete then not found**
    - **Validates: Requirements 3.4, 5.3, 5.8**
  - [ ]* 2.8 编写 ReleaseNoteService 属性测试：更新正确性
    - **Property 6: Update correctness**
    - **Validates: Requirements 5.2**

- [ ] 3. Checkpoint - 后端验证
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. system-setting 前端管理组件
  - [ ] 4.1 创建 API 文件 `system-setting/frontend/src/api/release-note.js`
    - 导出 addReleaseNote、updateReleaseNote、deleteReleaseNote、listReleaseNotes 函数
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  - [ ] 4.2 创建 `system-setting/frontend/src/business/system/setting/ReleaseNoteDialog.vue`
    - el-dialog 弹窗，表单包含 title（el-input, maxlength=100）和 content（el-input textarea, maxlength=2000）
    - 表单校验：title 和 content 必填
    - 支持新增和编辑模式
    - _Requirements: 2.1, 2.3, 3.3_
  - [ ] 4.3 创建 `system-setting/frontend/src/business/system/setting/ReleaseNoteManager.vue`
    - el-table 展示记录（列：标题、创建人、创建时间、操作）
    - 按 create_time 倒序排列
    - 分页（每页10条）
    - 新增/编辑/删除操作，删除前确认
    - 权限控制：v-permission 控制操作按钮
    - _Requirements: 1.3, 2.1, 2.2, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_
  - [ ] 4.4 修改 `system-setting/frontend/src/business/system/setting/AnnouncementSetting.vue`
    - 在底部引入 ReleaseNoteManager 组件，用 el-divider 分隔
    - _Requirements: 1.1, 1.2_

- [ ] 5. test-track 前端展示组件
  - [ ] 5.1 创建 API 文件 `test-track/frontend/src/api/release-note.js`
    - 导出 getRecentReleaseNotes、getReleaseNote 函数
    - _Requirements: 5.5, 5.6_
  - [ ] 5.2 创建 `test-track/frontend/src/business/home/components/ReleaseNotesBoard.vue`
    - 调用 getRecentReleaseNotes(5) 获取最近5条记录
    - 每条记录显示：标题行 `{YYYY年MM月DD日}上线公告`，下方 `创建时间: YYYY-MM-DD` 和 `创建者: {creator_name}`
    - 点击记录弹出 el-dialog 显示完整 content
    - 空状态显示"暂无上线记录"
    - 不分页
    - _Requirements: 4.2, 4.3, 4.4, 4.5_
  - [ ] 5.3 修改 `test-track/frontend/src/business/home/TrackHome.vue`
    - 在 FailureTestCaseList 的 el-row 下方新增 el-row，引入 ReleaseNotesBoard 组件
    - _Requirements: 4.1_
  - [ ]* 5.4 编写日期格式化属性测试
    - **Property 7: Date formatting**
    - **Validates: Requirements 4.3**

- [ ] 6. Final checkpoint - 全部验证
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 后端代码使用 Java 17，前端使用 Vue.js 2.7 + Element UI
- Controller 放在 SDK 层以支持跨模块访问
- 属性测试使用 jqwik 库，每个属性最少 100 次迭代
