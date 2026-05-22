# micro-app 微前端待优化清单

## 背景

当前项目已完成一轮 micro-app 微前端基础稳定化改造，主要包括：

- 统一子应用模块短名、后端 `serviceId`、嵌入式实例名之间的映射关系。
- 集中处理 `/services` 注册信息、`micro_apps`、`micro_ports` 缓存。
- 统一 Webpack 子应用 `public-path.js` 初始化逻辑。
- 修复登录成功后使用不存在的 `login_redirect` 命名路由导致页面空白的问题。
- 兼容 Vue2 + Webpack 子应用和 Vue3 + Vite 的 `analytics-stat` 子应用。

本文件记录后续仍需治理的 micro-app 微前端技术债和优化方向，供后续专项优化时参考。

## 优先级总览

| 优先级 | 优化项 | 风险类型 | 建议处理方式 |
| --- | --- | --- | --- |
| P0 | 确认登录后是否仍存在跳转系统设置问题 | 运行行为异常 | 若仍可复现，优先修复跳转链路 |
| P1 | `destroy=false` 与 `:key="currentApp.name"` 策略冲突 | 生命周期、内存占用 | 引入模块级缓存/销毁策略 |
| P1 | `@micro-zoe/micro-app` 使用 rc 预发布版本且版本范围浮动 | 依赖稳定性 | 先锁定当前实际版本，再专项升级正式版 |
| P1 | `inline=true` + with 沙箱隔离强度不足 | 全局污染、沙箱风险 | 按模块配置 inline/iframe 策略 |
| P1 | `disable-scopecss=true` 全局关闭样式隔离 | 样式污染 | 按模块逐步启用样式隔离 |
| P2 | 自定义 `fetch` 对响应统一 `text()` 且路径判断粗糙 | 静态资源加载边界不清 | 收窄处理范围，补充边界注释 |
| P2 | `preFetchApps` 全量预加载且固定 3 秒延迟 | 首屏后资源压力、内存占用 | 按权限、常用模块、空闲时间分批预加载 |
| P2 | `analytics-stat` 未接入主应用 EventBus/data listener | 跨模块通信能力缺失 | 补充 Vue3 版本 data/event 适配 |
| P2 | `analytics-stat` 独立实现 micro-app 环境判断 | 代码重复 | 复用 SDK 中的 micro-app env 工具 |
| P2 | 多个 Vue2 子应用 `main.js` 存在重复样板代码 | 维护成本 | 抽取 Vue2 micro-app bootstrap 工厂 |
| P2 | `request.js` 重复定义 micro-app 环境判断 | 代码重复 | 复用 `isMicroAppEnv()` |
| P3 | 注释中仍有 qiankun 历史残留 | 代码整洁 | 后续统一替换为“微前端”表述 |
| P3 | `migrated` 字段当前均为 `true` | 配置冗余 | 可保留作为灰度开关 |
| P3 | `test-track` 暂无 `microRouter` | 未来扩展 | 等存在被嵌入需求时再补充 |

## 详细说明

### P0：登录后跳转系统设置问题需最终确认

现象：登录后本应进入 `/project/home`，但最终可能进入 `#/setting/personsetting`。

已知排查结论：

- 登录权限判断本身可以命中项目模块，目标路径为 `/project/home`。
- 浏览器网络记录中能看到登录后先请求了 `/project` 子应用入口。
- 后续可能由于主应用或菜单逻辑触发 `router.replace('/')`，再被主路由 `/` 重定向到 `/setting/personsetting`。

建议后续处理：

1. 先确认当前版本是否仍能稳定复现。
2. 如果仍复现，优先从主应用路由、左侧菜单初始化、`micro_apps` 注册信息写入时序三处排查。
3. 不建议大范围重写菜单 `check()` 逻辑，优先采用小范围时序保护或初始化保障方案。

相关位置：

- `framework/sdk-parent/frontend/src/router/index.js`
- `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue`
- `framework/sdk-parent/frontend/src/business/app-layout/index.vue`
- `framework/sdk-parent/frontend/src/utils/micro-app-registry.js`

### P1：`destroy=false` 与 `:key="currentApp.name"` 策略冲突

当前主应用只渲染一个 `<micro-app>` 容器：

```vue
<micro-app
  v-if="currentApp"
  :key="currentApp.name"
  :name="currentApp.name"
  :url="currentApp.entry"
  :destroy="false"
/>
```

问题：

- `destroy=false` 表示希望子应用切换时不销毁，以保留状态或提升二次进入速度。
- 但 `:key="currentApp.name"` 会在模块变化时强制 Vue 重建 `<micro-app>` DOM 容器。
- 当前策略介于“缓存”和“销毁”之间，语义不清，可能导致内存常驻和 DOM 重建同时存在。

建议：

- 不建议简单去掉 `key`，因为 micro-app 对动态修改 `name/url` 的切换行为需要验证。
- 建议引入模块级运行策略，例如：

```js
{
  name: 'api',
  destroy: false,
  cache: true,
}

{
  name: 'report',
  destroy: true,
  cache: false,
}
```

可选方案：

1. 内存优先：`destroy=true` + 保留 `key`。
2. 缓存优先：多个 `<micro-app>` 使用 `v-for` 渲染，`v-show` 切换。
3. 推荐：按模块配置缓存/销毁策略。

相关位置：

- `framework/sdk-parent/frontend/src/business/app-layout/index.vue`

### P1：micro-app 依赖仍为 rc 预发布版本

当前声明类似：

```json
"@micro-zoe/micro-app": "^1.0.0-rc.4"
```

风险：

- `^1.0.0-rc.4` 会允许安装更新的 rc 版本。
- 实际 lockfile 中可能解析到 `1.0.0-rc.30`。
- rc 版本之间可能存在 API 行为差异。
- 新环境重新安装依赖时，版本行为可能不可控。

建议：

1. 短期先锁定当前实际可运行版本，例如：

```json
"@micro-zoe/micro-app": "1.0.0-rc.30"
```

2. 后续单独开专项升级到正式稳定版。
3. 升级时必须完整回归：
   - 主路由子应用加载。
   - `MicroAppWrapper` 嵌入式加载。
   - `preFetch`。
   - `iframe` 模式。
   - `inline` 模式。
   - `disable-memory-router`。
   - `destroy` 行为。
   - 自定义 `fetch`。

相关位置：

- `framework/sdk-parent/frontend/package.json`
- lockfile 文件

### P1：`inline=true` + with 沙箱隔离风险

当前主应用启用了 inline 模式：

```vue
:inline="true"
```

风险：

- Vue2 子应用 JS 在同一页面上下文中执行，隔离强度弱于 iframe。
- 依赖 with 沙箱隔离全局变量，仍可能出现全局污染。
- 子应用中存在 `window.mount`、`window.unmount` 生命周期挂载逻辑，理论上存在互相覆盖风险。
- 全局 Vue 插件、`Vue.prototype`、事件监听、定时器等需要严格清理。

建议：

- 不建议一次性全部改成 iframe，Vue2 老模块可能出现兼容性问题。
- 建议按模块配置运行模式：

```js
{
  name: 'api',
  inline: true,
  iframe: false,
}

{
  name: 'analytics',
  inline: false,
  iframe: true,
}
```

- Vue2 + Element UI 老模块先保持 inline。
- Vue3/Vite 或新模块优先使用 iframe 或更强隔离策略。

相关位置：

- `framework/sdk-parent/frontend/src/business/app-layout/index.vue`
- 各子应用 `frontend/src/main.js`

### P1：`disable-scopecss=true` 全局关闭样式隔离

当前：

```vue
:disable-scopecss="true"
```

风险：

- 所有 inline 子应用的 CSS 选择器都可能影响主应用或其它子应用。
- Element UI、全局 `.ms-*`、`body/html`、布局类样式容易互相污染。
- `analytics-stat` 使用 Naive UI，与 Vue2 + Element UI 风格不同，尤其需要关注隔离。

注意：

- 不能直接全局启用 scoped CSS，否则可能破坏旧模块依赖的全局主题、Element UI 弹窗样式、深层选择器。
- `analytics-stat` 当前按配置走 iframe，天然已有更强隔离，但仍建议保留其自身 CSS 前缀方案。

建议：

- 引入模块级配置：

```js
{
  name: 'api',
  disableScopecss: true,
}

{
  name: 'analytics',
  disableScopecss: false,
}
```

- 先对新模块或样式独立模块开启隔离。
- 老 Vue2 模块逐个验证后再调整。

相关位置：

- `framework/sdk-parent/frontend/src/business/app-layout/index.vue`

### P2：自定义 `fetch` 处理边界需要收窄

当前逻辑会对 micro-app custom fetch 的响应调用 `res.text()`：

```js
return window.fetch(url, options).then(res => res.text());
```

风险与边界：

- micro-app custom fetch 通常只处理子应用 HTML、JS、CSS 等静态资源。
- 正常情况下不会拦截子应用运行时 axios/fetch API 请求。
- 但当前代码没有说明这个边界，且对非 `/js/`、`/css/` 的资源也统一 `text()`，未来维护时容易误解。
- 当前使用 `url.indexOf('/js/')`、`url.indexOf('/css/')` 判断，可能被 query/path 中的非资源片段误判。

建议：

1. 增加注释说明 custom fetch 只处理 micro-app 静态资源加载，不处理子应用业务 API 请求。
2. 使用 `new URL(url, window.location.origin).pathname` 判断资源路径。
3. 仅对明确的 HTML/JS/CSS 文本资源执行路径修正和 `text()`。
4. 不要未经验证改成 `arrayBuffer()` 或直接返回 `Response`，需确认 micro-app fetch hook 的返回契约。

相关位置：

- `framework/sdk-parent/frontend/src/micro-app-setup.js`

### P2：预加载策略过粗

当前：

```js
microApp.preFetch(apps, 3000);
```

问题：

- 一次性预加载所有迁移子应用。
- 固定延迟 3 秒，不考虑浏览器空闲状态。
- 用户可能只使用其中少数模块，预加载全部模块会浪费网络和内存。

建议：

- 根据用户权限、菜单可见性、项目启用模块过滤预加载列表。
- 优先预加载常用模块，例如：
  - `workstation`
  - `track`
  - `api`
  - `project`
- 低频模块延后，例如：
  - `report`
  - `performance`
  - `analytics`
- 使用 `requestIdleCallback` 或分批 `setTimeout` 替代固定一次性预加载。

相关位置：

- `framework/sdk-parent/frontend/src/micro-app-setup.js`
- `framework/sdk-parent/frontend/src/app-init.js`

### P2：`analytics-stat` 需要补齐通信适配

当前 `analytics-stat` 是 Vue3 + Vite 子应用，已具备独立 micro-app 生命周期，但与 Vue2 子应用相比，仍有通信适配缺口。

待确认/优化：

- 是否接入主应用 `microApp.addDataListener`。
- 是否能接收项目切换、工作空间切换、语言切换、主题切换等主应用事件。
- 是否需要 Vue3 版本的 EventBus/data adapter。

建议：

- 如果分析统计模块需要响应主应用上下文变化，应补充 data listener。
- 与 Vue2 子应用的 `createEventBusAdapter()` 保持语义一致。

相关位置：

- `analytics-stat/frontend/src/main.ts`
- `framework/sdk-parent/frontend/src/utils/micro-app-event-bus.js`

### P2：`analytics-stat` 独立实现 micro-app 环境判断

问题：

- `analytics-stat` 作为 Vue3/Vite 子应用，可能有自己的 micro-app 环境判断逻辑。
- 如果判断逻辑与 SDK 中的 `micro-app-env.js` 不一致，后续维护成本会上升。

建议：

- 评估是否可以复用或对齐 SDK 中的环境判断逻辑。
- 如 Vue3/Vite 无法直接复用 Vue2 SDK 代码，也应保持判断规则一致。

相关位置：

- `analytics-stat/frontend/src/main.ts`
- `framework/sdk-parent/frontend/src/utils/micro-app-env.js`

### P2：Vue2 子应用 `main.js` 样板代码重复

多个 Vue2 子应用中存在大量重复逻辑：

- `public-path.js` 顶部导入。
- `isMicroAppEnv()` 判断。
- EventBus 适配器创建。
- `#app` 挂载点检测和创建。
- `window.mount` / `window.unmount` UMD 生命周期。
- 非微前端环境直接 `mount()`。

建议：

- 抽取 SDK 工厂函数，例如：

```js
createMicroAppBootstrap({
  Vue,
  App,
  router,
  i18n,
  pinia,
  createEventBus: true,
});
```

注意：

- `public-path.js` 必须继续保持最顶部导入。
- Vue2 与 Vue3 bootstrap 应分开设计。
- 不要破坏各子应用当前插件注册顺序。

相关位置：

- `api-test/frontend/src/main.js`
- `performance-test/frontend/src/main.js`
- `project-management/frontend/src/main.js`
- `report-stat/frontend/src/main.js`
- `system-setting/frontend/src/main.js`
- `test-track/frontend/src/main.js`
- `workstation/frontend/src/main.js`

### P2：`request.js` micro-app 环境判断重复

当前 `request.js` 内部重复定义 micro-app 环境判断：

```js
const isMicroEnv = !!window.__MICRO_APP_ENVIRONMENT__
  || !!(window.__MICRO_APP_PROXY_WINDOW__ && window.__MICRO_APP_PROXY_WINDOW__.__MICRO_APP_ENVIRONMENT__);
```

建议改为复用 SDK 工具函数：

```js
import { isMicroAppEnv } from '../utils/micro-app-env';
```

相关位置：

- `framework/sdk-parent/frontend/src/plugins/request.js`
- `framework/sdk-parent/frontend/src/utils/micro-app-env.js`

### P3：qiankun 注释残留

部分文件仍保留 qiankun 迁移历史注释，例如：

- 替代 qiankun props 接收 eventBus。
- 从 qiankun 环境变量迁移到 micro-app 环境变量。

影响：

- 不影响功能。
- 有助于理解迁移历史。
- 但长期看会让代码语义不够统一。

建议：

- 后续代码整理时统一改成“微前端”或“micro-app”表述。
- 迁移历史如有必要，可沉淀到文档中，不长期保留在运行时代码注释里。

### P3：`migrated` 字段当前冗余

当前 `micro-app-config.js` 中模块配置包含：

```js
migrated: true
```

现状：

- 目前所有模块都是 `true`。
- 从当前代码看，短期内像冗余字段。

建议：

- 不急于删除。
- 可作为后续灰度开关使用，例如临时关闭某模块 micro-app 加载。
- 如果长期没有灰度需求，再考虑清理。

相关位置：

- `framework/sdk-parent/frontend/src/micro-app-config.js`

### P3：`test-track` 暂无 microRouter

现状：

- 当前跨模块嵌入主要方向是：
  - `test-track` 嵌入 `api-test`
  - `test-track` 嵌入 `performance-test`
- `test-track` 自身目前不是主要被嵌入方。

建议：

- 暂不需要补 `test-track` 的 `microRouter`。
- 如果未来出现其它模块嵌入测试跟踪页面的需求，再补充对应 microRouter。

## analytics-stat 当前方案亮点

`analytics-stat` 作为 Vue3 + Vite 子应用，其兼容方案值得保留记录：

1. 使用 IIFE 输出，绕过 micro-app 对 ESM / `<script type="module">` 的兼容限制。
2. 构建后移除 `type="module"` 属性，保证 micro-app 能按普通脚本加载。
3. 使用 PostCSS selector prefix，为 CSS 增加 `#analytics-app` 前缀，提供额外样式保护。
4. 当前配置中 `analytics-stat` 仍按 `isViteApp: true` 走 iframe 隔离，因此它同时具备 iframe 隔离和 CSS 前缀保护。

需要注意：

- 如果未来取消 iframe 模式，必须重新验证 Vite 产物、CSS 前缀、全局变量、事件通信和路由行为。
- 如果继续保留 iframe，需重点优化主子应用通信和上下文同步。

## 建议后续实施顺序

### 阶段一：稳定性和低风险治理

目标：不大幅改变运行行为，先降低不确定性。

- 确认登录后跳转系统设置问题是否仍存在。
- 锁定 `@micro-zoe/micro-app` 当前实际版本。
- `request.js` 复用 `isMicroAppEnv()`。
- 自定义 `fetch` 增加边界注释，改为 pathname 判断。

### 阶段二：运行策略集中化

目标：建立 micro-app runtime policy。

建议在模块配置中集中声明：

```js
{
  name: 'api',
  iframe: false,
  inline: true,
  destroy: false,
  disableScopecss: true,
  prefetch: true,
}

{
  name: 'analytics',
  iframe: true,
  inline: false,
  destroy: true,
  disableScopecss: false,
  prefetch: false,
}
```

然后由 AppLayout 统一读取配置，不再在模板中写死：

- `iframe`
- `inline`
- `destroy`
- `disable-scopecss`
- `prefetch`

### 阶段三：性能和内存治理

目标：降低长时间使用的资源占用。

- 按模块配置缓存/销毁策略。
- 分批预加载子应用。
- 按用户权限和模块启用状态过滤预加载列表。
- 增加多模块切换后的内存观察和验证。

### 阶段四：专项升级和深度治理

目标：完成生产级微前端治理。

- micro-app 升级到正式稳定版。
- 按模块逐步开启样式隔离。
- 抽取 Vue2 子应用 bootstrap 工厂。
- 补齐 `analytics-stat` Vue3 通信适配。

## 回归验证清单

后续每次调整 micro-app 运行策略时，至少验证：

- 登录后默认落点是否正确。
- `/workstation` 是否正常加载。
- `/project/home` 是否正常加载。
- `/track` 是否正常加载。
- `/api/home` 是否正常加载。
- `/performance` 是否正常加载。
- `/report` 是否正常加载。
- `/setting/personsetting` 是否正常加载。
- `/analytics` 是否正常加载。
- 左侧菜单模块切换是否正常。
- 子应用间嵌入页面是否正常。
- 项目切换、工作空间切换后子应用上下文是否同步。
- 生产环境 JS/CSS 静态资源路径是否正确。
- 重复切换多个模块后内存是否持续上涨。
