# micro-app 微前端待优化清单

## 背景

当前项目已完成一轮 micro-app 微前端基础稳定化改造，主要包括：

- 统一子应用模块短名、后端 `serviceId`、嵌入式实例名之间的映射关系。
- 集中处理 `/services` 注册信息、`micro_apps`、`micro_ports` 缓存。
- 统一 Webpack 子应用 `public-path.js` 初始化逻辑。
- 修复登录成功后使用不存在的 `login_redirect` 命名路由导致页面空白的问题。
- 兼容 Vue2 + Webpack 子应用和 Vue3 + Vite 的 `analytics-stat` 子应用。

最近一轮低风险优化已完成：

- 主应用和嵌入式 `<micro-app>` 运行策略已集中到 `micro-app-config.js` helper 中。
- 主菜单 API、analytics 入口已改为各自默认页，并保留子应用内部 `/api`、`/analytics` redirect 兼容老地址。
- `analytics-stat` 二级菜单高亮已改为最长路径优先匹配。
- custom fetch 已改为基于 `URL.pathname` 判断根级 `/js/`、`/css/` 静态资源。
- `framework/sdk-parent/frontend/package-lock.json` 已可通过 `npm install --package-lock-only` 生成并锁定 `@micro-zoe/micro-app`，但该文件当前仍被前端 `.gitignore` 忽略，是否纳入版本控制需单独确认。

本文件记录后续仍需治理的 micro-app 微前端技术债和优化方向，供后续专项优化时参考。

## 优先级总览

| 优先级 | 优化项 | 风险类型 | 建议处理方式 |
| --- | --- | --- | --- |
| P0 | 确认登录后是否仍存在跳转系统设置问题 | 运行行为异常 | 若仍可复现，优先修复跳转链路 |
| P0 | `package-lock.json` 与 `package.json` 依赖状态不一致 | 构建稳定性 | 已生成一致 lockfile，但当前被 `.gitignore` 忽略，需确认是否纳入版本控制 |
| P1 | `destroy=false` 与 `:key="currentApp.name"` 策略语义不清 | 生命周期、内存占用 | 已集中默认策略；后续再引入模块级缓存/销毁策略 |
| P1 | `@micro-zoe/micro-app` 使用 rc 预发布版本且版本范围浮动 | 依赖稳定性 | 先锁定当前实际版本，再专项升级正式版 |
| P1 | `inline=true` + with 沙箱隔离弱于 iframe | 全局污染、沙箱风险 | 已集中默认策略；后续按模块显式配置 inline/iframe |
| P1 | `disable-scopecss=true` 全局关闭样式隔离 | 样式污染 | 已集中默认策略；后续按模块逐步启用样式隔离 |
| P1 | 主应用菜单导航和子应用内部路由边界不统一 | 路由一致性 | 已移除 API 手动 click 跳转，菜单直接进入默认页，子应用 redirect 保留老地址兼容 |
| P2 | 自定义 `fetch` 路径判断粗糙 | 静态资源加载边界不清 | 已保持 `Promise<string>` 契约并改为 pathname 判断 |
| P2 | `preFetchApps` 全量预加载且配置未对齐真实挂载 | 首屏后资源压力、预加载收益下降 | 已复用主运行策略的 iframe 判断；后续再按权限/常用模块过滤 |
| P2 | `analytics-stat` 未接入主应用 EventBus/data listener | 跨模块通信能力缺失 | 补充 Vue3 版本 data/event 适配 |
| P2 | `analytics-stat` 独立实现 micro-app 环境判断 | 代码重复 | 复用 SDK 中的 micro-app env 工具 |
| P2 | 多个 Vue2 子应用 `main.js` 存在重复样板代码 | 维护成本 | 抽取 Vue2 micro-app bootstrap 工厂 |
| P2 | `request.js` 重复定义 micro-app 环境判断 | 代码重复 | 复用 `isMicroAppEnv()` |
| P3 | 注释中仍有 qiankun 历史残留 | 代码整洁 | 后续统一替换为“微前端”表述 |
| P3 | `migrated` 字段当前均为 `true` | 配置冗余 | 可保留作为灰度开关 |
| P3 | `test-track` 暂无 `microRouter` | 未来扩展 | 等存在被嵌入需求时再补充 |

## micro-app 源码确认结论

已下载并阅读京东 `micro-app` 核心源码，关键结论如下：

1. `microApp.start({ fetch })` 的自定义 `fetch` 契约是返回 `Promise<string>`，默认实现也是 `window.fetch(...).then(res => res.text())`。因此当前统一 `res.text()` 的方向本身正确，风险主要在路径判断和注释边界不清。
2. `microApp.preFetch(apps, delay)` 内部已经使用 `requestIdleCallback`，之后再按 `delay` 执行，且预加载是串行执行；当前问题不是“立即并发预加载”，而是“全模块预加载”和“预加载配置与真实挂载配置未完全一致”。
3. `<micro-app>` 自定义元素只监听 `name` 和 `url` 属性变化；当前 `:key="currentApp.name"` 会让 Vue 在模块切换时重建 DOM 容器。配合 `destroy=false` 时，更准确的语义是“DOM 会卸载/重建，但资源缓存和 app 实例可能保留”，并不是完整页面状态 keep-alive。
4. `disable-scopecss=true` 会完全关闭 micro-app 的 selector prefix 样式隔离；但直接全局开启可能影响老 Vue2 + Element UI 模块中挂载到 `body` 的弹窗、消息、下拉等组件。
5. `inline=true` 不等于完全绕过沙箱直接污染主 `window`。在 with 沙箱下，micro-app 会通过 Proxy window 执行脚本，但该隔离强度仍弱于 iframe。
6. 源码建议 prefetch options 尽量与真实 `<micro-app>` 配置一致，否则预加载实例可能在真实挂载时被替换，降低预加载收益。

## 本轮已完成低风险优化

### 运行策略集中化

已在 `framework/sdk-parent/frontend/src/micro-app-config.js` 中新增：

- `getMainMicroAppRuntimePolicy(moduleName)`：主应用路由子应用策略，保持 `destroy=false`、`inline=true`、`disableMemoryRouter=true`、`disableScopecss=true`、`fiber=true`，并通过 `isViteApp()` 判断 `iframe`。
- `getEmbedMicroAppRuntimePolicy(moduleName)`：嵌入式子应用策略，保持 `destroy=true`、`clearData=true`、`fiber=true`，并通过 `isViteApp()` 判断 `iframe`。

当前只集中默认策略，不改变主应用和嵌入式场景的生命周期语义。

已接入位置：

- `framework/sdk-parent/frontend/src/business/app-layout/index.vue`
- `framework/sdk-parent/frontend/src/components/MicroAppWrapper.vue`
- `framework/sdk-parent/frontend/src/micro-app-setup.js`

### 菜单默认页和高亮修正

已调整主应用侧菜单：

- API 菜单直接进入 `/api/home`。
- analytics 菜单直接进入 `/analytics/knowledge/chat`。
- 移除 API 菜单历史 `@click` 手动二次跳转。

子应用内部 redirect 仍保留，因此直接访问旧地址仍可进入默认页：

- `/api -> /api/home`
- `/analytics -> /analytics/knowledge/chat`

`analytics-stat` 二级菜单高亮已改为按 path 长度倒序匹配，并使用精确匹配或 slash-bound prefix，避免 `/analytics/knowledge/chat` 被 `/analytics/knowledge` 抢先匹配。

### custom fetch 路径判断收窄

`framework/sdk-parent/frontend/src/micro-app-setup.js` 中的 custom fetch 已调整为：

- 保持 `window.fetch(url, options).then(res => res.text())` 的 `Promise<string>` 契约。
- 使用 `new URL(url, window.location.origin).pathname` 判断路径。
- 仅对根级 `/js/...`、`/css/...` 且尚未带模块前缀的资源补全 `/{moduleName}`。
- URL 解析失败时降级为原始 fetch。

### lockfile 同步状态

已在 `framework/sdk-parent/frontend` 执行：

```bash
npm install --package-lock-only
```

生成的 lockfile 中已包含 `@micro-zoe/micro-app`，且不再把 `qiankun` 作为直接依赖锁定。

注意：`framework/sdk-parent/frontend/.gitignore` 当前忽略 `package-lock.json`，因此该文件不会作为普通 git diff 出现。是否删除该 ignore 规则并将 lockfile 纳入版本控制，需要单独决策。

### 本轮构建验证

已通过：

- `git diff --check`
- `framework/sdk-parent/frontend npm run build`
- `analytics-stat/frontend npm run build`
- `api-test/frontend npm run build`

构建中仍有既有的 browserslist、Sass deprecation、chunk/asset size 警告，未发现本轮改动导致的构建失败。

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

### P0：依赖 lockfile 状态需要优先同步（已生成，待确认是否纳入版本控制）

当前主应用 `package.json` 已声明 micro-app 依赖：

```json
"@micro-zoe/micro-app": "^1.0.0-rc.4"
```

已执行 `npm install --package-lock-only` 后确认：

- lockfile 中已锁定 `@micro-zoe/micro-app`，实际解析版本为 `1.0.0-rc.30`。
- lockfile 中不再出现 `qiankun` 直接依赖。
- 依赖同步后主应用、analytics 子应用、API 子应用构建均已通过。

仍需确认：

1. `framework/sdk-parent/frontend/.gitignore` 当前忽略 `package-lock.json`，需要决定是否删除 ignore 规则并提交 lockfile。
2. 如果继续不提交 lockfile，则新环境仍会按 `package.json` 的版本范围解析依赖，无法完全保证安装结果稳定。
3. 如果决定提交 lockfile，应单独 review 该文件 diff，避免无关依赖漂移。

相关位置：

- `framework/sdk-parent/frontend/package.json`
- `framework/sdk-parent/frontend/package-lock.json`

### P1：`destroy=false` 与 `:key="currentApp.name"` 策略语义不清

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

- `destroy=false` 表示希望子应用卸载时不彻底销毁，以复用资源或提升二次进入速度。
- 但 `:key="currentApp.name"` 会在模块变化时强制 Vue 重建 `<micro-app>` DOM 容器。
- 结合 micro-app 源码看，当前更准确的行为是：DOM 容器会卸载/重建，子应用会 unmount，但资源缓存和 app 实例可能保留。
- 因此当前策略不是完整 DOM 状态 keep-alive，也不是彻底销毁，语义不清。

建议：

- 短期不建议简单去掉 `key`，因为 micro-app 虽然支持 `name/url` 属性变化切换，但内部涉及 attributeChangedCallback、keep-alive、prefetch replacement 等复杂分支。
- 当前已通过 `getMainMicroAppRuntimePolicy()` 集中默认策略，但尚未改变 `destroy=false` 与 `key` 的组合语义。
- 后续建议继续引入模块级运行策略，例如：

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

1. 内存优先：`destroy=true` + 保留 `key`，模块切换时尽量释放资源。
2. 加载速度优先：继续 `destroy=false` + 保留 `key`，明确这只是资源/实例级缓存，不保证 DOM 状态保留。
3. 页面状态优先：多个 `<micro-app>` 使用 `v-for` 渲染，`v-show` 切换，但需要额外评估内存占用。
4. 推荐：按模块配置缓存/销毁策略，例如高频 Vue2 模块可暂保留资源缓存，`analytics` 这类 iframe 模块优先 `destroy=true`。

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

### P1：`inline=true` + with 沙箱隔离弱于 iframe

当前主应用启用了 inline 模式：

```vue
:inline="true"
```

风险与边界：

- `inline=true` 主要影响脚本执行方式，不等于完全禁用沙箱。
- 在默认 with 沙箱下，micro-app 会使用 Proxy window 隔离子应用全局变量。
- with 沙箱隔离强度仍弱于 iframe，部分 escape properties、DOM、事件、定时器等仍需要依赖框架 patch 和卸载清理。
- 子应用中存在 `window.mount`、`window.unmount` 生命周期挂载逻辑，需要避免多个子应用之间生命周期变量互相覆盖。
- 对 Vue2 老模块来说，主要风险更多来自 DOM、CSS 和全局组件库副作用，而不只是普通全局变量。

建议：

- 不建议一次性全部改成 iframe，Vue2 老模块可能出现兼容性问题。
- 当前已将主应用和嵌入式子应用的默认 `iframe` 判断集中到 runtime policy helper 中，但仍主要由 `isViteApp()` 推导。
- 后续建议按模块显式配置运行模式：

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

- Vue2 + Element UI 老模块先保持 inline + with 沙箱，避免一次性切 iframe 引入兼容性问题。
- Vue3/Vite 或新模块优先使用 iframe 或更强隔离策略。
- 不建议只用 `isViteApp` 推导运行模式，后续应沉淀为显式 runtime policy。

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

- micro-app scoped CSS 会把普通选择器改写成类似 `micro-app[name=api] .xxx` 的形式。
- 不能直接全局启用 scoped CSS，否则可能破坏旧模块依赖的全局主题、Element UI 弹窗样式、深层选择器。
- 旧 Vue2 + Element UI 模块中，Message、Notification、Dialog、Dropdown、Popover 等挂载到 `body` 的组件尤其需要单独验证。
- `analytics-stat` 当前按配置走 iframe，天然已有更强隔离，但仍建议保留其自身 CSS 前缀方案。

建议：

- 当前已将主应用默认 `disableScopecss: true` 集中到 `getMainMicroAppRuntimePolicy()`，但未改变任何模块的样式隔离行为。
- 后续引入模块级配置：

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

### P1：主应用菜单导航和子应用内部路由边界不统一（本轮已处理默认入口）

历史问题：

- `analytics` 菜单曾经同时依赖 `<el-menu router>` 和 `@click` 手动跳转子路径，导致 `/workstation -> /analytics` 与 `/workstation -> /analytics/knowledge/chat` 双重导航冲突。
- `api` 菜单曾存在历史兼容逻辑，可能出现“从其它模块首次点击只进入 `/api`，不一定进入 `/api/home`”的行为差异。

本轮处理结果：

- API 菜单 `index` 已改为 `/api/home`。
- analytics 菜单 `index` 已改为 `/analytics/knowledge/chat`。
- API 菜单 `@click="active()"` 手动跳转已删除。
- 子应用内部 redirect 仍保留，因此直接访问 `/api`、`/analytics` 仍可进入各自默认页。

后续建议：

- 继续避免在主应用菜单 click 中手动 push 子应用内部路径。
- 若未来默认页需要调整，应优先在子应用路由 redirect 和主菜单 canonical index 中同步更新。

相关位置：

- `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue`
- `api-test/frontend/src/router/index.js`
- `analytics-stat/frontend/src/router/index.ts`

### P2：自定义 `fetch` 路径判断需要收窄（本轮已处理）

当前逻辑会对 micro-app custom fetch 的响应调用 `res.text()`：

```js
return window.fetch(url, options).then(res => res.text());
```

源码确认后的边界：

- micro-app custom fetch 用于加载子应用 HTML、JS、CSS 等静态资源，默认实现也是 `res.text()`。
- 正常情况下不会拦截子应用运行时 axios/fetch API 请求。
- 因此统一返回 `Promise<string>` 是正确契约，不应改成 `arrayBuffer()` 或直接返回 `Response`。
- 历史主要问题是代码使用 `url.indexOf('/js/')`、`url.indexOf('/css/')` 判断，可能被 query/path 中的非资源片段误判。

本轮处理结果：

1. 已使用 `new URL(url, window.location.origin).pathname` 判断资源路径。
2. 已移除提前的 `url.indexOf('/js/')` / `url.indexOf('/css/')` 判断。
3. 仅对明确的根级 `/js/`、`/css/` 静态资源执行模块路径修正。
4. 保持返回 `Promise<string>`，未改成 `arrayBuffer()` 或直接返回 `Response`。

相关位置：

- `framework/sdk-parent/frontend/src/micro-app-setup.js`

### P2：预加载策略和真实挂载配置未对齐

当前：

```js
microApp.preFetch(apps, 3000);
```

源码确认后的问题：

- micro-app 内部已经使用 `requestIdleCallback`，`microApp.preFetch(apps, 3000)` 的实际含义是“浏览器空闲后再延迟 3 秒串行预加载”。
- 当前不是立即并发预加载，但仍是一次性预加载所有迁移子应用。
- 用户可能只使用其中少数模块，预加载全部模块会浪费网络和内存。
- 当前预加载只传 `name`、`url`、部分 `iframe` 配置，未与真实 `<micro-app>` 上的 `inline`、`disable-scopecss`、`destroy` 等运行策略完全统一。
- micro-app 源码建议 prefetch options 尽量与真实挂载配置一致，否则预加载 app 可能在真实挂载时被替换，降低预加载收益。
- 本轮已复用 `getMainMicroAppRuntimePolicy(moduleName).iframe` 判断预加载 `iframe`，使预加载和真实挂载在 iframe 沙箱策略上同源。

后续建议：

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
- 继续评估是否将 `inline`、`disable-scopecss`、`disable-memory-router` 等字段补齐到 prefetch options；补齐前需要运行验证。
- 对低频模块延后或禁用预加载，避免无意义的资源占用。

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
- 同步 `framework/sdk-parent/frontend/package-lock.json`，确认 lockfile 中实际锁定 `@micro-zoe/micro-app`。
- 锁定 `@micro-zoe/micro-app` 当前实际版本，避免 rc 版本范围浮动。
- `request.js` 复用 `isMicroAppEnv()`。
- 自定义 `fetch` 增加边界注释，改为 pathname 判断。

### 阶段二：运行策略集中化

目标：建立 micro-app runtime policy。

建议在模块配置中集中声明：

```js
{
  name: 'api',
  sandbox: 'with',
  iframe: false,
  inline: true,
  destroy: false,
  disableScopecss: true,
  prefetch: true,
}

{
  name: 'analytics',
  sandbox: 'iframe',
  inline: false,
  destroy: true,
  disableScopecss: false,
  prefetch: false,
}
```

然后由 AppLayout 和 preFetchApps 统一读取配置，不再在模板或预加载逻辑中写死：

- `iframe`
- `inline`
- `destroy`
- `disable-scopecss`
- `prefetch`

注意：prefetch options 应尽量与真实 `<micro-app>` 挂载配置一致，避免预加载实例在真实挂载时被替换。

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
- `/api` 是否正常进入 API 模块默认页。
- `/api/home` 是否正常加载。
- `/performance` 是否正常加载。
- `/report` 是否正常加载。
- `/setting/personsetting` 是否正常加载。
- `/analytics` 是否正常加载并进入分析统计默认页。
- 左侧菜单模块切换是否正常，且无 Vue Router navigation cancelled 警告。
- 子应用间嵌入页面是否正常。
- 项目切换、工作空间切换后子应用上下文是否同步。
- 生产环境 JS/CSS 静态资源路径是否正确。
- 重复切换多个模块后内存是否持续上涨。
