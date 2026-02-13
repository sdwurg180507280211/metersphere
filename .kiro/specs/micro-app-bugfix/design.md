# 设计文档

## 概述

本设计文档描述了 MeterSphere 从 qiankun 迁移到 micro-app 后发现的 5 个 Bug 的修复方案。修复工作集中在前端 JavaScript/Vue 文件，涉及跨应用事件广播、inline 模式环境检测、EventBus 适配器兼容性、qiankun 残留代码清理和预加载调用链修复。

所有修复遵循"改动面小、边界清晰、可回滚"的二次开发原则，优先复用已有的工具函数（`isMicroAppEnv()`、`getMicroAppPublicPath()`、`broadcastEvent()`），以正确实现的模块（test-track、api-test、system-setting）为参考模板。

## 架构

### 当前架构（存在问题）

```mermaid
graph TB
    subgraph "主应用 Main_App"
        PSL[ProjectSearchList.vue]
        HWS[HeaderWs.vue]
        MAJ[micro-app.js<br/>qiankun 残留]
        MAS[micro-app-setup.js]
        EB[micro-app-event-bus.js]
        ENV[micro-app-env.js]
    end

    subgraph "正确实现的子应用"
        TT[test-track ✅]
        API[api-test ✅]
        SS[system-setting ✅]
    end

    subgraph "有问题的子应用"
        PT[performance-test ❌]
        RS[report-stat ❌]
        WS[workstation ❌]
    end

    PSL -->|"$EventBus.$emit 仅本地"| EB
    HWS -->|"$EventBus.$emit 仅本地"| EB
    PSL -.->|"broadcastEvent 未调用"| TT
    MAJ -->|"qiankun start() 仍执行"| PT
    MAJ -->|"调用 preFetchApps"| MAS

    PT -->|"window.__MICRO_APP_ENVIRONMENT__<br/>inline 模式下为 undefined"| PT
    RS -->|"window.__MICRO_APP_ENVIRONMENT__<br/>inline 模式下为 undefined"| RS
    WS -->|"window.__MICRO_APP_ENVIRONMENT__<br/>inline 模式下为 undefined"| WS

    TT -->|"isMicroAppEnv() ✅"| ENV
    API -->|"isMicroAppEnv() ✅"| ENV
    SS -->|"isMicroAppEnv() ✅"| ENV
```

### 修复后架构

```mermaid
graph TB
    subgraph "主应用 Main_App"
        PSL2[ProjectSearchList.vue]
        HWS2[HeaderWs.vue]
        MAS2[micro-app-setup.js]
        INIT[app-init.js<br/>服务列表 + 预加载]
        EB2[micro-app-event-bus.js]
        ENV2[micro-app-env.js]
    end

    subgraph "所有子应用（统一使用 isMicroAppEnv）"
        TT2[test-track ✅]
        API2[api-test ✅]
        SS2[system-setting ✅]
        PT2[performance-test ✅]
        RS2[report-stat ✅]
        WS2[workstation ✅]
    end

    PSL2 -->|"$EventBus.$emit + broadcastEvent"| EB2
    HWS2 -->|"$EventBus.$emit + broadcastEvent"| EB2
    EB2 -->|"setGlobalData 广播"| TT2
    EB2 -->|"setGlobalData 广播"| API2
    EB2 -->|"setGlobalData 广播"| PT2
    EB2 -->|"setGlobalData 广播"| RS2
    EB2 -->|"setGlobalData 广播"| WS2
    EB2 -->|"setGlobalData 广播"| SS2

    INIT -->|"getApps → preFetchApps"| MAS2
    INIT -->|"sessionStorage 写入"| INIT

    PT2 -->|"isMicroAppEnv()"| ENV2
    RS2 -->|"isMicroAppEnv()"| ENV2
    WS2 -->|"isMicroAppEnv()"| ENV2
```

## 组件与接口

### 修改的组件

| 组件 | 文件路径 | 修改内容 |
|------|----------|----------|
| ProjectSearchList | `framework/sdk-parent/frontend/src/components/head/ProjectSearchList.vue` | change() 方法中添加 broadcastEvent 调用 |
| HeaderWs | `framework/sdk-parent/frontend/src/components/head/HeaderWs.vue` | _changeWs() 方法中添加 broadcastEvent 调用 |
| micro-app-event-bus | `framework/sdk-parent/frontend/src/utils/micro-app-event-bus.js` | createEventBusAdapter() 内部使用 isMicroAppEnv() |
| performance-test main | `performance-test/frontend/src/main.js` | 替换所有 window.__MICRO_APP_ENVIRONMENT__ |
| performance-test public-path | `performance-test/frontend/src/public-path.js` | 使用 isMicroAppEnv() 和 getMicroAppPublicPath() |
| report-stat main | `report-stat/frontend/src/main.js` | 替换所有 window.__MICRO_APP_ENVIRONMENT__ |
| report-stat public-path | `report-stat/frontend/src/public-path.js` | 使用 isMicroAppEnv() 和 getMicroAppPublicPath() |
| workstation main | `workstation/frontend/src/main.js` | 替换所有 window.__MICRO_APP_ENVIRONMENT__ |
| workstation public-path | `workstation/frontend/src/public-path.js` | 使用 isMicroAppEnv() 和 getMicroAppPublicPath() |
| Main_App main | `framework/sdk-parent/frontend/src/main.js` | 移除 `import './micro-app'` |

### 新增的组件

| 组件 | 文件路径 | 用途 |
|------|----------|------|
| app-init | `framework/sdk-parent/frontend/src/app-init.js` | 从 micro-app.js 迁移服务列表获取和预加载逻辑 |

### 删除的组件

| 组件 | 文件路径 | 原因 |
|------|----------|------|
| micro-app.js | `framework/sdk-parent/frontend/src/micro-app.js` | qiankun 残留代码，registerMicroApps/start 不再需要 |

### 接口定义

#### broadcastEvent（已存在，需被调用）

```javascript
/**
 * 全局广播事件到所有子应用
 * @param {Object} eventData - 事件数据
 * @param {string} eventData.type - 事件类型，如 'projectChange'、'changeWs'
 */
broadcastEvent({ type: 'projectChange' })
broadcastEvent({ type: 'changeWs' })
```

#### isMicroAppEnv（已存在，需在更多位置使用）

```javascript
/**
 * 判断当前是否运行在 micro-app 子应用环境中
 * 兼容 inline 模式和非 inline 模式
 * @returns {boolean}
 */
isMicroAppEnv()
```

#### getMicroAppPublicPath（已存在，需在更多位置使用）

```javascript
/**
 * 获取 micro-app 注入的公共路径
 * inline 模式下从 __MICRO_APP_PROXY_WINDOW__ 中获取
 * @returns {string|undefined}
 */
getMicroAppPublicPath()
```

## 数据模型

本次修复不涉及数据模型变更。所有修改均为前端 JavaScript 逻辑层面的修复。

### 事件数据格式（EventBusData）

broadcastEvent 和 createEventBusAdapter 之间通过以下数据格式通信：

```javascript
// broadcastEvent 发送的数据格式
{
  eventType: 'EventBus',        // 固定标识
  eventName: 'projectChange',   // 事件名称
  payload: { type: 'projectChange', ...extraData }  // 完整事件数据
}
```

### sessionStorage 数据

从 micro-app.js 迁移到 app-init.js 的数据写入：

```javascript
sessionStorage.setItem('micro_apps', JSON.stringify(modules));    // { "api-test": true, ... }
sessionStorage.setItem('micro_ports', JSON.stringify(microPorts)); // { "api-test": 8004, ... }
```


## 正确性属性

*正确性属性是一种在系统所有有效执行中都应成立的特征或行为——本质上是关于系统应该做什么的形式化陈述。属性是人类可读规范与机器可验证正确性保证之间的桥梁。*

### Property 1: EventBus 事件转发一致性

*For any* 事件名称和任意 payload 数据，当通过 micro-app 的 globalDataListener 接收到符合 EventBusData 格式的数据时，createEventBusAdapter() 创建的本地 EventBus 应触发与 eventName 字段完全一致的事件，且 payload 数据完整传递。

**Validates: Requirements 1.4, 3.2**

### Property 2: 服务列表 sessionStorage 写入正确性

*For any* 从 getApps() 返回的服务列表，app-init.js 写入 sessionStorage 的 `micro_apps` 应包含除 gateway 外的所有服务（值为 true），`micro_ports` 应包含对应的端口映射，且与原始数据一致。

**Validates: Requirements 4.3, 5.3**

### Property 3: preFetchApps 网关过滤

*For any* 服务列表（包含或不包含 gateway 服务），preFetchApps() 生成的预加载应用列表应排除 serviceId 为 'gateway' 的条目，且包含所有其他服务。

**Validates: Requirements 5.2**

## 错误处理

### Bug 1 修复的错误处理

- broadcastEvent 内部调用 microApp.setGlobalData()，如果 microApp 未初始化，不应阻塞主应用的正常流程
- broadcastEvent 调用应放在 `$EventBus.$emit` 之后，确保本地事件不受影响

### Bug 2 修复的错误处理

- isMicroAppEnv() 已内置容错逻辑，检查 `window.__MICRO_APP_PROXY_WINDOW__` 前先判断其是否存在
- getMicroAppPublicPath() 返回 undefined 时，webpack 会使用默认的 publicPath

### Bug 3 修复的错误处理

- createEventBusAdapter() 中 `window.microApp?.addDataListener` 使用可选链操作符，microApp 对象不存在时不会报错

### Bug 4 修复的错误处理

- 删除 micro-app.js 前需确认服务列表获取逻辑已迁移到 app-init.js
- app-init.js 中 getApps() 调用应包含 .catch() 错误处理，与原 micro-app.js 保持一致

### Bug 5 修复的错误处理

- preFetchApps() 调用失败不应影响主应用启动，应在 catch 中记录错误日志

## 测试策略

### 测试框架

- 单元测试：Jest（Vue CLI 默认集成）
- 属性测试：fast-check（JavaScript 属性测试库）
- 组件测试：@vue/test-utils

### 单元测试

针对具体的修复点编写单元测试：

1. **broadcastEvent 调用验证**：mock broadcastEvent，验证 ProjectSearchList.change() 和 HeaderWs._changeWs() 中调用了 broadcastEvent
2. **isMicroAppEnv() inline 模式检测**：模拟 inline 模式环境（设置 `window.__MICRO_APP_PROXY_WINDOW__.__MICRO_APP_ENVIRONMENT__ = true`），验证返回 true
3. **createEventBusAdapter() inline 模式注册**：模拟 inline 模式环境，验证 addDataListener 和 addGlobalDataListener 被调用
4. **app-init.js 服务列表处理**：mock getApps()，验证 sessionStorage 写入和 preFetchApps 调用

### 属性测试

使用 fast-check 库，每个属性测试运行至少 100 次迭代：

- **Property 1**：生成随机事件名和 payload，验证 EventBus 转发一致性
  - Tag: **Feature: micro-app-bugfix, Property 1: EventBus 事件转发一致性**
- **Property 2**：生成随机服务列表，验证 sessionStorage 写入正确性
  - Tag: **Feature: micro-app-bugfix, Property 2: 服务列表 sessionStorage 写入正确性**
- **Property 3**：生成随机服务列表（含/不含 gateway），验证过滤逻辑
  - Tag: **Feature: micro-app-bugfix, Property 3: preFetchApps 网关过滤**

### 手动验证

由于涉及微前端运行时行为，以下场景需要手动验证：

1. 在浏览器中切换项目，观察子应用是否刷新数据
2. 在浏览器中切换工作空间，观察子应用是否刷新数据
3. 验证 performance-test、report-stat、workstation 在 inline 模式下不会双重挂载
4. 验证 qiankun 的路由劫持不再生效
