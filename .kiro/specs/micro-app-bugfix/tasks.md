# 实施计划：micro-app-bugfix

## 概述

按优先级修复 micro-app 迁移后的 6 个 Bug。Bug 6（侧边栏菜单点击无法触发子应用生命周期）和 Bug 2（inline 模式双重挂载）为高优先级。所有修改集中在前端 JavaScript/Vue 文件，以正确实现的模块（test-track）为参考模板。

## 任务

- [ ] 1. 修复侧边栏菜单点击无法触发子应用生命周期（Bug 6 - 最高优先级）
  - [ ] 1.1 在 App.vue 的 `<micro-app>` 标签添加 `:key="currentApp.name"` 绑定
    - 在 `<micro-app v-if="currentApp"` 行之后添加 `:key="currentApp.name"`
    - 目的：当模块名变化时，Vue 销毁旧 micro-app 实例并创建新实例，触发子应用完整生命周期
    - 文件：`framework/sdk-parent/frontend/src/App.vue`
    - _Requirements: 6.1, 6.4, 6.5_
  - [ ] 1.2 移除 AsideMenus.vue 中的 `window.location.href` 强制刷新逻辑
    - 删除 `active()` 方法体中的 `window.location.href = "/#/api/home"` 逻辑，保留空方法或直接移除方法和对应的 `@click="active()"` 绑定
    - 删除 `activeAnalyticsStat()` 方法体中的 `window.location.href = "/#/analytics"` 逻辑，移除方法和对应的 `@click="activeAnalyticsStat()"` 绑定
    - 文件：`framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue`
    - _Requirements: 6.2, 6.3_

- [ ] 2. 修复跨应用事件广播（Bug 1）
  - [ ] 2.1 在 ProjectSearchList.vue 的 change() 方法中添加 broadcastEvent 调用
    - 在 `this.$EventBus.$emit('projectChange')` 之后，添加 `broadcastEvent({ type: 'projectChange' })`
    - 需要 import broadcastEvent from `metersphere-frontend/src/utils/micro-app-event-bus`
    - _Requirements: 1.1_
  - [ ] 2.2 在 HeaderWs.vue 的 _changeWs() 方法中添加 broadcastEvent 调用
    - 在 `this.$EventBus.$emit('projectChange')` 之后，添加 `broadcastEvent({ type: 'projectChange' })` 和 `broadcastEvent({ type: 'changeWs' })`
    - 需要 import broadcastEvent from `metersphere-frontend/src/utils/micro-app-event-bus`
    - _Requirements: 1.2, 1.3_

- [ ] 3. 修复 EventBus 适配器 inline 兼容性（Bug 3）
  - [ ] 3.1 修改 micro-app-event-bus.js 中 createEventBusAdapter() 的环境检测
    - 将 `if (window.__MICRO_APP_ENVIRONMENT__)` 替换为 `if (isMicroAppEnv())`
    - 添加 `import { isMicroAppEnv } from './micro-app-env'`
    - _Requirements: 3.1, 3.2_
  - [ ]* 3.2 编写 createEventBusAdapter 的属性测试
    - **Property 1: EventBus 事件转发一致性**
    - **Validates: Requirements 1.4, 3.2**

- [ ] 4. 修复子应用 inline 模式环境检测（Bug 2）
  - [ ] 4.1 修复 performance-test/frontend/src/main.js
    - 添加 `import { isMicroAppEnv } from 'metersphere-frontend/src/utils/micro-app-env'`
    - 将所有 `window.__MICRO_APP_ENVIRONMENT__` 替换为 `isMicroAppEnv()`
    - 参考 test-track/frontend/src/main.js 的实现
    - _Requirements: 2.1, 2.4, 2.5, 2.6_
  - [ ] 4.2 修复 performance-test/frontend/src/public-path.js
    - 使用 isMicroAppEnv() 和 getMicroAppPublicPath() 替换直接访问 window 属性
    - 参考 test-track/frontend/src/public-path.js 的实现
    - _Requirements: 2.7_
  - [ ] 4.3 修复 report-stat/frontend/src/main.js
    - 同 4.1 的修改模式（无 addDataListener 部分）
    - _Requirements: 2.2, 2.4, 2.5_
  - [ ] 4.4 修复 report-stat/frontend/src/public-path.js
    - 同 4.2 的修改模式
    - _Requirements: 2.7_
  - [ ] 4.5 修复 workstation/frontend/src/main.js
    - 同 4.3 的修改模式
    - _Requirements: 2.3, 2.4, 2.5_
  - [ ] 4.6 修复 workstation/frontend/src/public-path.js
    - 同 4.2 的修改模式
    - _Requirements: 2.7_

- [ ] 5. 检查点 - 确认高优先级修复
  - 确保所有修改的文件语法正确，运行 lint 检查
  - 确认 App.vue 中 `<micro-app>` 标签包含 `:key="currentApp.name"`
  - 确认 AsideMenus.vue 中不再有 `window.location.href` 调用
  - 确认 performance-test、report-stat、workstation 的 main.js 和 public-path.js 中不再有 `window.__MICRO_APP_ENVIRONMENT__` 直接引用
  - 确保所有测试通过，如有问题请询问用户

- [ ] 6. 清理 qiankun 残留代码并修复预加载（Bug 4 + Bug 5）
  - [ ] 6.1 创建 app-init.js 迁移服务列表获取和预加载逻辑
    - 从 micro-app.js 提取 getApps() 调用、sessionStorage 写入、preFetchApps() 调用
    - 移除 qiankun 相关代码（registerMicroApps、start、isMigrated 过滤）
    - 包含 .catch() 错误处理
    - _Requirements: 4.3, 5.1, 5.2, 5.3_
  - [ ] 6.2 修改 Main_App 的 main.js
    - 将 `import './micro-app'` 替换为 `import './app-init'`
    - _Requirements: 4.1_
  - [ ] 6.3 删除 micro-app.js 文件
    - 删除 `framework/sdk-parent/frontend/src/micro-app.js`
    - _Requirements: 4.2_
  - [ ]* 6.4 编写 app-init.js 的属性测试
    - **Property 2: 服务列表 sessionStorage 写入正确性**
    - **Validates: Requirements 4.3, 5.3**
  - [ ]* 6.5 编写 preFetchApps 的属性测试
    - **Property 3: preFetchApps 网关过滤**
    - **Validates: Requirements 5.2**

- [ ] 7. 最终检查点
  - 确保所有修改的文件语法正确
  - 确认 micro-app.js 已删除
  - 确认 main.js 中不再有 qiankun 相关 import
  - 确认 App.vue 的 micro-app 标签有 :key 绑定
  - 确认 AsideMenus.vue 无 window.location.href
  - 确保所有测试通过，如有问题请询问用户

## 备注

- 标记 `*` 的任务为可选任务，可跳过以加快 MVP 进度
- 每个任务引用了具体的需求编号，便于追溯
- 检查点确保增量验证
- 属性测试验证通用正确性属性
- 单元测试验证具体示例和边界情况
- 所有修改以 test-track 模块的正确实现为参考模板
- Bug 6（侧边栏菜单生命周期）提升为最高优先级，排在任务 1
