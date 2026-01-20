# 实现计划：公告栏配置功能（含样式选项）

## 概述

在 MeterSphere 系统参数设置页面添加公告栏配置功能，使管理员能够通过可视化界面配置页面顶部公告栏内容和样式。

## 任务

- [x] 1. 添加国际化文本
  - 在 `framework/sdk-parent/frontend/src/i18n/lang/` 下的 zh-CN.js、zh-TW.js、en-US.js 中添加公告相关文本
  - 包含：announcement.setting、announcement.content、announcement.preview、announcement.content_placeholder、announcement.no_content
  - _Requirements: 1.1, 3.3_

- [x] 2. 创建公告配置组件（基础版）
  - [x] 2.1 创建 AnnouncementSetting.vue 组件
    - 在 `system-setting/frontend/src/business/system/setting/` 目录下创建
    - 实现公告内容输入框（el-input textarea）
    - 实现实时预览区域
    - 实现编辑/保存/取消按钮
    - 实现权限控制（v-permission 指令）
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 5.1, 5.2, 5.3_
  
  - [x] 2.2 添加前端 API 封装
    - 在 `system-setting/frontend/src/api/system.js` 中添加 getAnnouncementContent 和 saveAnnouncementContent 函数
    - _Requirements: 2.1, 2.2_

- [x] 3. 集成到系统参数设置页面
  - 修改 `system-setting/frontend/src/business/system/setting/SystemParameterSetting.vue`
  - 添加"公告设置"Tab 页
  - 引入 AnnouncementSetting 组件
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 4. 实现公告即时生效（基础版）
  - [x] 4.1 在 AnnouncementSetting 中添加 EventBus 事件发送
    - 保存成功后发送 `announcement-updated` 事件
    - _Requirements: 4.1_
  
  - [x] 4.2 修改 AppLayout 监听事件
    - 在 `framework/sdk-parent/frontend/src/business/app-layout/index.vue` 中监听 `announcement-updated` 事件
    - 收到事件后重新加载公告内容
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 5. Checkpoint - 基础功能验证
  - 确保基础功能正常工作
  - 验证公告配置、预览、保存、即时生效
  - 如有问题请提出

- [x] 6. 添加样式选项国际化文本
  - 在 `framework/sdk-parent/frontend/src/i18n/lang/` 下添加样式相关文本
  - 包含：enabled、disabled_hint、style、style_info、style_warning、style_danger、style_success、style_custom、custom_colors、background_color、text_color
  - _Requirements: 6.1, 7.1_

- [x] 7. 实现样式配置功能
  - [x] 7.1 添加样式相关 API 封装
    - 在 `system-setting/frontend/src/api/system.js` 中添加 getAnnouncementEnabled、getAnnouncementStyle、saveAnnouncementEnabled、saveAnnouncementStyle 函数
    - _Requirements: 6.5, 6.6, 7.4_
  
  - [x] 7.2 更新 AnnouncementSetting.vue 组件
    - 添加公告开关（el-switch）
    - 添加预设样式选择器（el-radio-group）
    - 添加自定义颜色选择器（el-color-picker）
    - 更新预览区域支持动态样式
    - 更新保存逻辑保存所有配置
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3_

- [x] 8. 更新 AppLayout 支持样式
  - [x] 8.1 修改 AppLayout 加载样式配置
    - 加载 announcement.enabled 和 announcement.style
    - 应用动态样式到公告栏
    - _Requirements: 4.4, 6.5, 6.6, 7.2, 7.3_
  
  - [x] 8.2 更新 EventBus 事件处理
    - 收到 announcement-updated 事件后重新加载所有配置
    - _Requirements: 4.1, 4.4_

- [x] 9. Checkpoint - 样式功能验证
  - 确保样式功能正常工作
  - 验证预设样式切换、自定义颜色、开关控制
  - 验证公告栏样式即时生效
  - 如有问题请提出

- [ ]* 10. 编写单元测试
  - [ ]* 10.1 组件渲染测试
    - 测试 AnnouncementSetting 组件正确渲染
    - 测试样式选择器正确渲染
    - _Requirements: 1.1, 1.2, 6.1_
  
  - [ ]* 10.2 权限控制测试
    - **Property 3: 权限控制一致性**
    - **Validates: Requirements 1.3, 5.1, 5.2, 5.3**
  
  - [ ]* 10.3 样式切换测试
    - **Property 5: 样式配置一致性**
    - **Validates: Requirements 3.4, 6.2, 6.5, 6.6**

- [ ]* 11. 编写属性测试
  - [ ]* 11.1 保存-读取 Round-Trip 测试
    - **Property 1: 公告内容保存 Round-Trip**
    - **Validates: Requirements 2.2, 2.3**
  
  - [ ]* 11.2 预览同步测试
    - **Property 2: 预览内容同步**
    - **Validates: Requirements 3.1**
  
  - [ ]* 11.3 开关状态测试
    - **Property 6: 开关状态一致性**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

- [x] 12. Final Checkpoint - 确保所有功能正常
  - 确保所有功能正常，如有问题请提出

## 备注

- 标记 `*` 的任务为可选任务，可跳过以加快 MVP 开发
- 每个任务都引用了具体的需求条款以便追溯
- Checkpoint 用于增量验证功能
- 属性测试验证通用正确性属性
- 任务 1-5 为已完成的基础功能
- 任务 6-9 为新增的样式配置功能
