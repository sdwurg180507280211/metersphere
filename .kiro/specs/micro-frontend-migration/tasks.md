# 实施计划：微前端框架迁移（qiankun → micro-app）

## 概述

采用渐进式迁移策略，先搭建 micro-app 基础设施和双模式并行能力，然后逐模块迁移子应用，最后清理 qiankun 残留代码。迁移顺序：先迁移简单模块（workstation、report-stat），再迁移复杂模块（api-test、test-track）。

 q子应用统一采用 micro-app 的 UMD 生命周期模式（`window.mount` / `window.unmount`），适合 MeterSphere 频繁切换模块的场景。注意：这里的「UMD 模式」是 micro-app 的生命周期管理概念，与 webpack 的 `libraryTarget: 'umd'` 打包格式无关，后者必须移除。

## 任务

- [x] 1. 主应用 micro-app 基础设施搭建
  - [x] 1.1 安装 micro-app 依赖并创建初始化配置
    - 在 `framework/sdk-parent/frontend/package.json` 中添加 `@micro-zoe/micro-app` 依赖
    - 创建 `framework/sdk-parent/frontend/src/micro-app-setup.js`：
      - 添加 `Vue.config.ignoredElements = ['micro-app']`（Vue 2 必须，否则报 Unknown custom element 警告）
      - 调用 `microApp.start()` 并配置 `fiber: true`（异步执行 JS，减少 8 个子应用对主线程的阻塞）和全局生命周期
      - 【注意】不在全局设置 `iframe: true`，只有 Vue 3 + Vite 子应用需要 iframe 沙箱，通过 `<micro-app iframe>` 标签属性单独开启
      - 实现 `preFetchApps()` 函数，在获取服务列表后调用 `microApp.preFetch()` 预加载子应用资源，Vite 子应用预加载时需设置 `iframe: true`
    - 在 `framework/sdk-parent/frontend/src/main.js` 中引入 `micro-app-setup.js`（与现有 `micro-app.js` 并行）
    - _Requirements: 1.1, 1.5, 8.1_

  - [x] 1.2 创建模块配置表和双模式加载逻辑
    - 创建 `framework/sdk-parent/frontend/src/micro-app-config.js`，定义 `MIGRATED_MODULES` 配置表
    - 配置表结构：`{ migrated: boolean, isViteApp: boolean }`，`isViteApp` 用于决定是否开启 iframe 沙箱
    - 初始状态所有模块标记为 `migrated: false, isViteApp: false`
    - 修改 `framework/sdk-parent/frontend/src/micro-app.js`，在 `registerMicroApps` 前过滤已迁移模块（已迁移的不注册到 qiankun）
    - _Requirements: 6.1, 6.4, 8.2_

  - [x] 1.3 改造 App.vue 支持 micro-app 子应用容器
    - 在 `framework/sdk-parent/frontend/src/App.vue` 中添加 `<micro-app>` 标签（与 `#micro-app` div 并存）
    - 根据当前路由和模块配置表决定使用哪种容器
    - 添加 `:iframe="currentApp.isViteApp || false"` 属性，Vue 3 + Vite 子应用自动开启 iframe 沙箱
    - 全局路由激活的子应用不设置 `destroy`（利用缓存加速重复加载）
    - 实现 `@datachange` 和 `@error` 事件监听
    - _Requirements: 1.2, 1.3, 8.1_

  - [ ]* 1.4 编写属性测试：服务列表到 micro-app 标签映射
    - **Property 1: 服务列表到 micro-app 标签映射**
    - 使用 fast-check 生成随机服务列表，验证配置生成逻辑（数量、name、url 计算）
    - **Validates: Requirements 1.2**

  - [ ]* 1.5 编写属性测试：双模式并行加载与回退
    - **Property 6: 双模式并行加载与回退**
    - 使用 fast-check 生成随机模块配置表，验证加载方式选择逻辑
    - **Validates: Requirements 6.1, 6.4**

  - [ ]* 1.6 编写属性测试：iframe 沙箱模式选择正确性
    - **Property 9: iframe 沙箱模式选择正确性**
    - 使用 fast-check 生成随机配置表（含 isViteApp 标记），验证 iframe 属性设置正确
    - **Validates: Requirements 8.1, 8.2**

- [x] 2. 跨应用通信机制改造
  - [x] 2.1 创建 EventBus 兼容适配器
    - 创建 `framework/sdk-parent/frontend/src/utils/micro-app-event-bus.js`
    - 实现 `createEventBusAdapter()` 函数：
      - 桥接 micro-app `addDataListener` → 本地 Vue EventBus（跨应用 → 本地）
      - 桥接 micro-app `addGlobalDataListener` → 本地 Vue EventBus（全局广播 → 本地）
    - _Requirements: 4.1, 4.2_

  - [x] 2.2 实现全局广播机制
    - 在主应用中实现 `broadcastEvent(eventData)` 函数
    - 使用 `microApp.setGlobalData()` 一次性广播到所有子应用（替代遍历 EventBus 逐个通知）
    - 集成到项目切换（projectChange）和工作空间切换（changeWs）事件处理中
    - _Requirements: 4.3_

  - [ ]* 2.3 编写属性测试：数据传递完整性
    - **Property 2: 主应用到子应用数据传递**
    - **Property 3: 子应用到主应用数据传递**
    - 使用 fast-check 生成随机数据对象，验证传递后等价
    - **Validates: Requirements 3.3, 4.1, 4.2**

  - [ ]* 2.4 编写属性测试：全局广播覆盖率
    - **Property 4: 全局广播到所有子应用**
    - 使用 fast-check 生成随机子应用集合，验证全部收到广播
    - **Validates: Requirements 4.3**

- [x] 3. MicroAppWrapper 按需加载组件
  - [x] 3.1 创建 MicroAppWrapper.vue 组件
    - 创建 `framework/sdk-parent/frontend/src/components/MicroAppWrapper.vue`
    - 实现与 `MicroApp.vue` 相同的 props 接口（to、service、routeParams、routeName）
    - 内部使用 `<micro-app>` 标签替代 `loadMicroApp`
    - 添加 `:iframe="isViteApp"` 属性，根据模块配置表中的 `isViteApp` 字段决定是否开启 iframe 沙箱
    - 设置 `destroy` 和 `clear-data` 属性（按需加载场景需要强制清除缓存和通讯数据）
    - 使用 `embed-` 前缀的 appName 避免与全局路由激活的子应用 name 冲突
    - 实现 `@datachange`、`@error` 监听
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 8.1_

  - [ ]* 3.2 编写属性测试：模块切换正确性
    - **Property 7: 模块切换正确性与资源释放**
    - 使用 fast-check 生成随机切换序列，验证资源释放
    - **Validates: Requirements 3.2, 7.4**

- [x] 4. Checkpoint - 基础设施验证
  - 确保所有测试通过，验证 micro-app 基础设施在双模式下正常工作
  - 确认 qiankun 原有功能未受影响
  - 如有问题请告知

- [x] 5. 子应用改造模板和工具
  - [x] 5.1 创建子应用 public-path.js 改造模板
    - 将 `__POWERED_BY_QIANKUN__` 替换为 `__MICRO_APP_ENVIRONMENT__`
    - 将 `__INJECTED_PUBLIC_PATH_BY_QIANKUN__` 替换为 `__MICRO_APP_PUBLIC_PATH__`
    - 保留独立运行时的服务列表获取逻辑
    - _Requirements: 2.2, 5.4_

  - [x] 5.2 创建子应用 main.js 改造模板（UMD 生命周期模式）
    - 移除 qiankun 生命周期导出（`export async function bootstrap/mount/unmount/update`）
    - 采用 micro-app 的 UMD 生命周期模式：将渲染逻辑放入 `window.mount = (data) => { ... }`
    - 【关键】`window.mount(data)` 的 `data` 参数由 micro-app 自动传入，来源于 `<micro-app :data="appData">` 的 data 属性
    - 将卸载逻辑放入 `window.unmount = () => { ... }`
    - Vue 插件注册（`Vue.use()`）放在 mount 外部，只执行一次（UMD 生命周期模式的优势）
    - 非微前端环境直接调用 `window.mount()`
    - 将 `!window.__POWERED_BY_QIANKUN__` 替换为 `!window.__MICRO_APP_ENVIRONMENT__`
    - 对于含按需加载场景的子应用（api-test），在 mount 中处理 defaultPath/routeParams/routeName，并添加 `addDataListener` 监听后续路由更新（补充 mount 初始数据之外的运行时动态更新）
    - 集成 EventBus 兼容适配器（`createEventBusAdapter()` 替代从 props 接收 eventBus）
    - 【注意】这里的「UMD 模式」是 micro-app 的生命周期管理概念，与 webpack 的 `libraryTarget: 'umd'` 打包格式无关
    - _Requirements: 2.1, 2.3, 2.5_

  - [x] 5.3 创建子应用 vue.config.js 改造模板
    - 移除 `library` 和 `libraryTarget: 'umd'` 配置（micro-app 不需要 UMD 打包格式）
    - 保留 `chunkLoadingGlobal` 避免多应用 chunk 冲突
    - 新增 `globalObject: 'window'`（micro-app 指南推荐）
    - 保留 CORS 头配置
    - 【注意】移除的是 webpack 的 UMD 打包配置，保留的是 main.js 中的 UMD 生命周期模式，两者完全不同
    - _Requirements: 2.4, 5.1, 5.3_

  - [ ]* 5.4 编写属性测试：publicPath 正确性
    - **Property 5: publicPath 资源路径正确性**
    - 使用 fast-check 生成随机路径，验证 publicPath 设置
    - **Validates: Requirements 5.4**

- [x] 6. 第一批子应用迁移（简单模块）
  - [x] 6.1 迁移 workstation 子应用
    - 按模板改造 `workstation/frontend/src/public-path.js`
    - 按模板改造 `workstation/frontend/src/main.js`（UMD 生命周期模式：window.mount/unmount，移除 qiankun 生命周期导出）
    - 按模板改造 `workstation/frontend/vue.config.js`（移除 libraryTarget: 'umd' 打包配置，添加 globalObject）
    - 在模块配置表中将 workstation 标记为 `{ migrated: true, isViteApp: false }`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 6.2 迁移 report-stat 子应用
    - 按模板改造 `report-stat/frontend/src/public-path.js`
    - 按模板改造 `report-stat/frontend/src/main.js`（UMD 生命周期模式）
    - 按模板改造 `report-stat/frontend/vue.config.js`（移除 UMD 打包配置）
    - 在模块配置表中将 report-stat 标记为 `{ migrated: true, isViteApp: false }`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 6.3 迁移 analytics-stat 子应用
    - 按模板改造 `analytics-stat/frontend/src/public-path.js`
    - 按模板改造 `analytics-stat/frontend/src/main.js`（UMD 生命周期模式）
    - 按模板改造 `analytics-stat/frontend/vue.config.js`（移除 UMD 打包配置）
    - 在模块配置表中将 analytics-stat 标记为 `{ migrated: true, isViteApp: false }`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 7. Checkpoint - 第一批迁移验证
  - 验证 workstation、report-stat、analytics-stat 在 micro-app 下正常加载
  - 验证未迁移模块仍通过 qiankun 正常工作
  - 验证模块间切换正常
  - 如有问题请告知

- [x] 8. 第二批子应用迁移（中等复杂度）
  - [x] 8.1 迁移 project-management 子应用
    - 按模板改造 public-path.js、main.js（UMD 生命周期模式）、vue.config.js（移除 UMD 打包配置）
    - 在模块配置表中标记为 `{ migrated: true, isViteApp: false }`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 8.2 迁移 system-setting 子应用
    - 按模板改造 public-path.js、main.js（UMD 生命周期模式）、vue.config.js（移除 UMD 打包配置）
    - 在模块配置表中标记为 `{ migrated: true, isViteApp: false }`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 8.3 迁移 performance-test 子应用
    - 按模板改造 public-path.js、main.js（UMD 生命周期模式）、vue.config.js（移除 UMD 打包配置）
    - 在模块配置表中标记为 `{ migrated: true, isViteApp: false }`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 9. 第三批子应用迁移（高复杂度 - 含按需加载）
  - [x] 9.1 迁移 test-track 子应用
    - 按模板改造 public-path.js、main.js（UMD 生命周期模式）、vue.config.js（移除 UMD 打包配置）
    - 将所有 `<micro-app>` 组件引用（约 10 处）从 `MicroApp` 替换为 `MicroAppWrapper`
    - 涉及文件：TestPlanApiCaseResult.vue、TestPlanApiScenarioList.vue、TestPlanUiScenarioList.vue、TestPlanLoadCaseList.vue、ApiScenarioFailureResult.vue、UiScenarioResult.vue、LoadAllResult.vue
    - 在模块配置表中标记为 `{ migrated: true, isViteApp: false }`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 3.1_

  - [x] 9.2 迁移 api-test 子应用
    - 按模板改造 public-path.js、vue.config.js（移除 UMD 打包配置）
    - main.js 改造需特别处理：
      - UMD 生命周期模式的 `window.mount(data)` 中根据 data 参数决定使用 router 还是 microRouter
      - `data` 参数由 micro-app 自动传入，包含 defaultPath、routeParams、routeName
      - 添加 `addDataListener` 监听后续主应用路由更新（补充 mount 初始数据之外的运行时动态更新）
    - 注意 api-test 有多个 pages 入口（shareApiReport、shareDocument、apiDocument），非主入口不受影响
    - 在模块配置表中标记为 `{ migrated: true, isViteApp: false }`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 9.3 迁移 TaskCenter 组件中的 MicroApp 引用
    - 将 `framework/sdk-parent/frontend/src/components/task/TaskCenter.vue` 中的 `MicroApp` 替换为 `MicroAppWrapper`
    - 更新 import 路径和组件注册
    - _Requirements: 3.2_

- [x] 10. Checkpoint - 全量迁移验证
  - 验证所有 8 个子应用在 micro-app 下正常加载
  - 验证所有跨模块嵌入场景正常（test-track 中的 API/性能/UI 报告）
  - 验证 TaskCenter 报告查看正常
  - 验证模块间快速切换无白屏和内存泄漏
  - 验证页面刷新后状态恢复正常
  - 如有问题请告知

- [x] 11. 清理 qiankun 残留代码
  - [x] 11.1 移除主应用 qiankun 相关代码
    - 删除 `framework/sdk-parent/frontend/src/micro-app.js`（qiankun 注册逻辑）
    - 删除 `framework/sdk-parent/frontend/src/components/MicroApp.vue`（旧按需加载组件）
    - 从 `framework/sdk-parent/frontend/src/main.js` 中移除 `import './micro-app'`
    - 移除模块配置表中的 `migrated` 标记（所有模块统一使用 micro-app）
    - 删除 `framework/sdk-parent/frontend/public/js/dev/qiankun.umd.js` 和 `prd/qiankun.umd.min.js`
    - _Requirements: 1.4, 3.5, 4.4_

  - [x] 11.2 移除 qiankun npm 依赖
    - 从 `framework/sdk-parent/frontend/package.json` 中移除 `qiankun: "2.9.3"` 依赖
    - 运行 `npm install` 更新 lock 文件
    - _Requirements: 1.4_

  - [ ]* 11.3 编写属性测试：页面刷新后状态恢复
    - **Property 8: 页面刷新后状态恢复**
    - 使用 fast-check 生成随机模块+路由组合，验证恢复逻辑
    - **Validates: Requirements 7.5**

- [x] 12. Final checkpoint - 最终验证
  - 确保所有测试通过
  - 确认项目中无 qiankun 残留引用（搜索 `__POWERED_BY_QIANKUN__`、`registerMicroApps`、`loadMicroApp` 等关键词）
  - 确认构建产物正常（`npm run build` 各模块）
  - 如有问题请告知

## 备注

- 标记 `*` 的任务为可选任务，可跳过以加速 MVP
- 每个任务引用了具体的需求编号，确保可追溯
- Checkpoint 任务确保增量验证
- 迁移顺序按复杂度递增：简单模块 → 中等模块 → 含按需加载的复杂模块
- 属性测试使用 fast-check 库，每个测试至少运行 100 次迭代
- 子应用迁移遵循二次开发原则：改动面小、边界清晰、可回滚
- 子应用统一采用 micro-app 的 UMD 生命周期模式（window.mount / window.unmount），这是 micro-app 的生命周期管理概念
- 必须移除 webpack 的 `libraryTarget: 'umd'` 打包配置，micro-app 不需要 UMD 打包格式
- Vue 2 + Webpack 子应用使用默认 with 沙箱，Vue 3 + Vite 子应用通过 `<micro-app iframe>` 开启 iframe 沙箱
- 两种技术栈统一使用 `window.mount` / `window.unmount` 生命周期管理，差异仅在沙箱模式
