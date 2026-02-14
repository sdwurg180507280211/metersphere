# 需求文档

## 简介

MeterSphere 从 qiankun 迁移到 micro-app 微前端框架后，代码评审发现了 6 个 Bug。这些 Bug 涉及跨应用事件通信断裂、inline 模式下环境检测失败导致双重挂载、EventBus 适配器内部兼容性问题、qiankun 残留代码仍在执行、预加载调用链断裂、以及侧边栏菜单点击无法触发子应用生命周期切换。本需求文档定义了修复这些 Bug 所需的全部验收标准。

## 术语表

- **主应用（Main_App）**：framework/sdk-parent/frontend，承载所有子应用的宿主应用
- **子应用（Sub_App）**：各业务模块前端（api-test、test-track、performance-test、report-stat、workstation、system-setting、project-management）
- **EventBus**：基于 Vue 实例的事件总线，用于组件间通信
- **broadcastEvent**：micro-app-event-bus.js 中定义的全局广播函数，通过 microApp.setGlobalData() 向所有子应用发送事件
- **isMicroAppEnv()**：micro-app-env.js 中定义的环境检测函数，兼容 inline 模式和非 inline 模式
- **getMicroAppPublicPath()**：micro-app-env.js 中定义的公共路径获取函数，兼容 inline 模式
- **inline 模式**：micro-app 的一种运行模式，子应用 JS 在主应用 window 上下文中执行，`window.__MICRO_APP_ENVIRONMENT__` 不会注入到 window 对象
- **createEventBusAdapter()**：micro-app-event-bus.js 中定义的 EventBus 适配器工厂函数，创建兼容 micro-app 数据通信的本地 EventBus
- **preFetchApps()**：micro-app-setup.js 中定义的预加载函数，利用 micro-app 的 preFetch API 预加载子应用资源
- **UMD 生命周期**：micro-app 的子应用挂载模式，通过 window.mount() 和 window.unmount() 控制生命周期
- **micro-app 标签**：`<micro-app>` 自定义元素，micro-app 框架用于加载和渲染子应用的容器标签
- **Vue key 属性**：Vue 的特殊属性，用于标识 VNode 的唯一性，当 key 变化时 Vue 会销毁旧元素并创建新元素
- **SPA 导航**：单页应用内部通过 Vue Router 进行的路由跳转，不触发浏览器页面刷新

## 需求

### 需求 1：跨应用事件广播修复

**用户故事：** 作为 MeterSphere 用户，我希望在主应用切换项目或工作空间时，所有子应用能同步刷新数据，以便我看到最新的项目上下文。

#### 验收标准

1. WHEN 用户在 ProjectSearchList 组件中切换项目, THE Main_App SHALL 在触发本地 EventBus 的 `projectChange` 事件后，同时调用 broadcastEvent 将 `projectChange` 事件广播到所有 Sub_App
2. WHEN 用户在 HeaderWs 组件中切换工作空间, THE Main_App SHALL 在触发本地 EventBus 的 `projectChange` 事件后，同时调用 broadcastEvent 将 `projectChange` 事件广播到所有 Sub_App
3. WHEN 用户在 HeaderWs 组件中切换工作空间, THE Main_App SHALL 调用 broadcastEvent 将 `changeWs` 事件广播到所有 Sub_App
4. WHEN Sub_App 接收到 broadcastEvent 广播的事件, THE Sub_App 的本地 EventBus SHALL 触发对应的事件名称，使已注册的监听器正常执行

### 需求 2：子应用 inline 模式环境检测修复

**用户故事：** 作为 MeterSphere 用户，我希望所有子应用在 micro-app inline 模式下能正确检测运行环境，避免双重挂载和 EventBus 功能降级。

#### 验收标准

1. WHEN performance-test Sub_App 在 inline 模式下被 Main_App 加载, THE performance-test SHALL 使用 isMicroAppEnv() 替代 `window.__MICRO_APP_ENVIRONMENT__` 进行环境检测，确保正确识别微前端环境
2. WHEN report-stat Sub_App 在 inline 模式下被 Main_App 加载, THE report-stat SHALL 使用 isMicroAppEnv() 替代 `window.__MICRO_APP_ENVIRONMENT__` 进行环境检测，确保正确识别微前端环境
3. WHEN workstation Sub_App 在 inline 模式下被 Main_App 加载, THE workstation SHALL 使用 isMicroAppEnv() 替代 `window.__MICRO_APP_ENVIRONMENT__` 进行环境检测，确保正确识别微前端环境
4. WHEN Sub_App 在 inline 模式下正确检测到微前端环境, THE Sub_App SHALL 调用 createEventBusAdapter() 创建带有 micro-app 数据监听器的 EventBus，而非降级为普通 Vue 实例
5. WHEN Sub_App 在 inline 模式下正确检测到微前端环境, THE Sub_App SHALL 跳过底部的独立挂载逻辑，仅通过 window.mount() 由 micro-app 框架触发挂载，避免双重挂载
6. WHEN performance-test Sub_App 在 inline 模式下正确检测到微前端环境, THE performance-test SHALL 在 window.mount() 中注册 addDataListener 用于接收路由数据更新
7. WHEN Sub_App 的 public-path.js 在 inline 模式下执行, THE Sub_App SHALL 使用 isMicroAppEnv() 和 getMicroAppPublicPath() 替代直接访问 `window.__MICRO_APP_ENVIRONMENT__` 和 `window.__MICRO_APP_PUBLIC_PATH__`，确保正确设置 webpack 公共路径

### 需求 3：EventBus 适配器内部 inline 兼容修复

**用户故事：** 作为 MeterSphere 开发者，我希望 createEventBusAdapter() 在 inline 模式下也能正确注册 micro-app 数据监听器，以便跨应用事件能被正确转发到本地 EventBus。

#### 验收标准

1. WHEN createEventBusAdapter() 在 inline 模式的 Sub_App 中被调用, THE createEventBusAdapter SHALL 使用 isMicroAppEnv() 替代 `window.__MICRO_APP_ENVIRONMENT__` 进行环境检测
2. WHEN createEventBusAdapter() 正确检测到微前端环境, THE createEventBusAdapter SHALL 注册 addDataListener 和 addGlobalDataListener，将 micro-app 传来的事件数据转发到本地 EventBus

### 需求 4：qiankun 残留代码清理

**用户故事：** 作为 MeterSphere 开发者，我希望移除所有 qiankun 残留代码，避免旧框架的路由劫持和不必要的代码执行影响 micro-app 的正常运行。

#### 验收标准

1. WHEN Main_App 启动时, THE Main_App SHALL 移除对 micro-app.js（qiankun 代码文件）的 import，停止 qiankun 的 registerMicroApps 和 start 执行
2. WHEN micro-app.js 的 import 被移除后, THE Main_App SHALL 删除 framework/sdk-parent/frontend/src/micro-app.js 文件
3. WHEN qiankun 代码被移除后, THE Main_App SHALL 保留服务列表获取和 sessionStorage 写入逻辑（micro_apps、micro_ports），将其迁移到合适的位置

### 需求 5：preFetchApps 调用链修复

**用户故事：** 作为 MeterSphere 用户，我希望子应用资源能在浏览器空闲时被预加载，以便模块切换时获得更快的加载体验。

#### 验收标准

1. WHEN qiankun 代码（micro-app.js）被移除后, THE Main_App SHALL 在获取服务列表后调用 preFetchApps() 进行子应用资源预加载
2. WHEN preFetchApps() 被调用时, THE preFetchApps SHALL 接收完整的服务列表数据，排除网关服务后对所有子应用执行预加载
3. WHEN Main_App 启动完成后, THE Main_App SHALL 将服务列表信息（micro_apps、micro_ports）写入 sessionStorage，供子应用间跨模块嵌入和端口映射使用

### 需求 6：侧边栏菜单点击触发子应用生命周期切换

**用户故事：** 作为 MeterSphere 用户，我希望点击侧边栏菜单在不同模块间切换时，子应用能正确触发生命周期事件（created → beforemount → mounted），以便每个模块都能正常初始化和渲染，而无需手动刷新页面。

#### 验收标准

1. WHEN 用户点击侧边栏菜单从一个模块切换到另一个模块（例如从 /track 切换到 /api）, THE Main_App 的 `<micro-app>` 标签 SHALL 通过 Vue key 属性绑定 `currentApp.name`，使 Vue 在模块名变化时销毁旧的 micro-app 实例并创建新实例，从而触发子应用完整的生命周期
2. WHEN 用户点击侧边栏的接口测试菜单（/api）且当前已在 /api 路由, THE AsideMenus 组件 SHALL 使用 Vue Router 进行 SPA 导航，移除 active() 方法中的 `window.location.href` 强制刷新逻辑
3. WHEN 用户点击侧边栏的统计分析菜单（/analytics）, THE AsideMenus 组件 SHALL 使用 Vue Router 进行 SPA 导航，移除 activeAnalyticsStat() 方法中的 `window.location.href` 强制刷新逻辑
4. WHEN `<micro-app>` 标签因 key 变化被 Vue 销毁时, THE micro-app 框架 SHALL 触发旧子应用的 unmount 生命周期，完成资源清理
5. WHEN `<micro-app>` 标签因 key 变化被 Vue 重新创建时, THE micro-app 框架 SHALL 触发新子应用的完整生命周期（created → beforemount → mounted），确保子应用正常初始化
