# qiankun 微前端技术细节（MeterSphere 2.x 旧项目）

> 适用范围：本仓库 `metersphere`（Vue2 + Vue CLI + qiankun）。
>
> 目标：用“项目真实代码”总结 qiankun 的主/子应用注册、路由激活、资源加载(publicPath)、通信、构建与部署差异等关键细节，便于后续排障与升级改造。

---

## 1. 整体架构与关键约定

### 1.1 主应用/子应用划分

- 主应用（框架/网关前端）
  - 目录：`framework/sdk-parent/frontend/`
  - 作用：登录、布局、公共组件库、qiankun 注册与运行时容器
- 子应用（业务模块前端）
  - 目录示例：
    - `test-track/frontend/`
    - `project-management/frontend/`
    - `workstation/frontend/`
    - `system-setting/frontend/`
    - `api-test/frontend/` 等

### 1.2 两种运行模式

- **微前端模式（被主应用加载）**
  - 入口：访问主应用（例如本地 `http://localhost:4000`）
  - 主应用通过 qiankun 动态加载子应用
- **独立运行模式（子应用单独启动）**
  - 直接访问子应用 devserver（例如 `http://localhost:4005`）
  - 子应用不依赖主应用运行，但会做一部分“微前端模式下的兼容初始化”（例如端口映射写入 sessionStorage）

（开发操作层面可参考已有文档：`docs/技术文档/开发指南/前端独立开发指南.md`）

---

## 2. 主应用：子应用注册与激活规则

### 2.1 子应用列表来源：后端网关动态发现

- 代码：`framework/sdk-parent/frontend/src/micro-app.js`
- 请求：`framework/sdk-parent/frontend/src/api/apps.js` 调用 `GET /services`

核心点：主应用不是写死子应用清单，而是从网关获取服务列表 `serviceId/port`。

### 2.2 注册逻辑：registerMicroApps + start

- 代码：`framework/sdk-parent/frontend/src/micro-app.js`

实现要点：

1) **activeRule 使用 hash 前缀匹配**

- `activeRule: location.hash.startsWith('#/' + name)`
- 这意味着主应用路由/子应用激活依赖 hash 片段的前缀，例如：
  - `#/test-track/...` 触发 `test-track` 子应用激活

2) **container 统一挂载点**

- `container: '#micro-app'`

3) **入口 entry 在 dev/prod 不同**

- 开发环境：`entry = //127.0.0.1:${svc.port - 4000}`
  - 约定：子应用前端端口 = 后端端口 - 4000
  - 示例：`test-track` 后端 8005 -> 前端 4005
- 生产环境：替换成 `window.location.host + '/' + app.name`
  - 代码：`app.entry = app.entry.replace(/127\.0\.0\.1:\d+/g, window.location.host + '/' + app.name)`
  - 约定：生产环境通过反向代理把子应用静态资源挂在 `/{serviceId}` 路径下

4) **sessionStorage 保存模块信息**

- `micro_apps`：模块是否存在的 map
- `micro_ports`：模块后端端口 map
- `MICRO_MODE`：标记当前处于微前端模式

### 2.3 主应用入口引入

- 代码：`framework/sdk-parent/frontend/src/main.js`
- `import './micro-app';` 在主应用启动时自动注册并启动 qiankun。

---

## 3. 主应用：按需加载模式（loadMicroApp）

除了 `registerMicroApps` 的统一挂载方式，工程里还封装了一个“按需加载”组件：

- 组件：`framework/sdk-parent/frontend/src/components/MicroApp.vue`

### 3.1 典型用途

在某些场景，主应用不通过全局路由激活，而是通过组件参数决定加载哪个子应用、传什么路由参数。

### 3.2 关键实现

- `loadMicroApp(app)` 动态加载
- `microApp.update({...})` 用于在不重建的情况下更新子应用路由（做了 0ms 防抖）
- props 传递：
  - `defaultPath` / `routeParams` / `routeName`
  - `eventBus: this.$EventBus`

### 3.3 全局状态

- 使用 `initGlobalState({ event: null })`
- 当前代码中 `onGlobalStateChange` 监听逻辑留空，更多是“能力预留”。

---

## 4. 子应用：publicPath 与资源加载（关键）

### 4.1 运行时 publicPath 注入

子应用都包含 `src/public-path.js`，用于处理 qiankun 注入的资源路径：

- 示例：`test-track/frontend/src/public-path.js`
- 示例：`workstation/frontend/src/public-path.js`

核心逻辑：

- 当 `window.__POWERED_BY_QIANKUN__ === true`：
  - `__webpack_public_path__ = window.__INJECTED_PUBLIC_PATH_BY_QIANKUN__`

意义：

- 子应用被主应用加载时，子应用的 js/css 等静态资源地址必须从 qiankun 注入的 `publicPath` 计算，否则容易出现资源 404。

### 4.2 独立运行下的端口信息初始化

当子应用独立运行（`!window.__POWERED_BY_QIANKUN__`）时：

- 子应用会请求 `GET /services`（复用主应用的 apps api）
- 写入 sessionStorage：`micro_apps`、`micro_ports`

意义：

- 一些主应用/按需加载逻辑依赖 `micro_ports`，独立运行时也保持数据一致。

---

## 5. 子应用：构建产物必须是 UMD（qiankun 要求）

### 5.1 webpack output 关键配置

以 `test-track/frontend/vue.config.js` 为例：

- `output.library = ${name}-[name]`
- `output.libraryTarget = 'umd'`
- `output.chunkLoadingGlobal = webpackJsonp_${name}`
- 自定义 `filename/chunkFilename`，避免多子应用 chunk 名冲突

意义：

- qiankun 需要子应用以可被加载的 UMD 形式输出
- 多子应用同页加载时，必须规避 webpack 全局变量冲突（`chunkLoadingGlobal`）

### 5.2 devServer 跨域

子应用 devServer 通常加：

- `headers: { 'Access-Control-Allow-Origin': '*' }`

意义：

- 主应用（4000）加载子应用（400x）时避免跨域阻塞。

---

## 6. 子应用：生命周期与独立运行切换

子应用一般在 `src/main.js` 中实现 qiankun 生命周期：

- 示例：`project-management/frontend/src/main.js`
- 示例：`workstation/frontend/src/main.js`

常见模式：

- `if (!window.__POWERED_BY_QIANKUN__) render();`
- `export async function mount(props) { render(props) }`
- `export async function unmount() { instance.$destroy() }`
- 可选：`export async function update(props) { microRouter.push(...) }`

同时：

- props 里通常会传 `eventBus`，子应用会把它挂到 `Vue.prototype.$EventBus`。

---

## 7. 通信机制（项目实现）

### 7.1 Vue EventBus（项目主用）

- 主应用：`framework/sdk-parent/frontend/src/micro-app.js` 创建 `eventBus = new Vue()`
- 通过 qiankun `props` 传给子应用
- 子应用把 `eventBus` 绑定到 `Vue.prototype.$EventBus`

特点：

- 简单、直观
- 适合广播事件（例如刷新、打开弹窗等）
- 需要注意：事件名规范与解注册（避免内存泄漏）

### 7.2 qiankun globalState（能力预留）

- `MicroApp.vue` 里 `initGlobalState` 已接入
- 当前监听/业务使用较少（更多是预留扩展点）

---

## 8. 请求路径与反向代理约定（与微前端强相关）

### 8.1 主框架 request 基础路径

- 代码：`framework/sdk-parent/frontend/src/plugins/request.js`

关键逻辑：

- 如果 `window.__POWERED_BY_QIANKUN__`：`baseURL = '/' + packageJSON.name`
  - 意味着当子应用被加载时，请求会自动带上模块名前缀（例如 `/test-track/...`）
- 如果浏览器路径本身以 `/${packageJSON.name}` 开头：
  - 非分享链接会强制跳转 `/`，防止用户直接访问子应用路径导致路由/资源异常

### 8.2 生产环境静态资源路径

主应用在非 development 时将 entry 替换为：

- `window.location.host + '/' + serviceId`

因此生产环境必须保证：

- `/{serviceId}` 路径能访问到对应子应用的静态资源入口（通常 Nginx/网关反代实现）

---

## 9. 常见问题与排查清单

### 9.1 子应用资源 404

优先检查：

- 子应用是否引入并执行了 `src/public-path.js`
- 是否在 qiankun 模式下正确设置了 `__webpack_public_path__`

### 9.2 多子应用同屏时 js chunk 冲突

优先检查：

- `vue.config.js` 中 `output.chunkLoadingGlobal` 是否为各模块独立值
- `output.library` 是否带模块名前缀

### 9.3 主应用加载子应用跨域失败

优先检查：

- 子应用 devServer 是否设置 `Access-Control-Allow-Origin: *`

### 9.4 子应用独立运行请求路径异常

优先检查：

- `framework/sdk-parent/frontend/src/plugins/request.js` 的 baseURL 规则
- 当前访问路径是否误带 `/${packageJSON.name}` 前缀

---

## 10. 关键文件索引（便于快速定位）

- 主应用注册：`framework/sdk-parent/frontend/src/micro-app.js`
- 子应用清单接口：`framework/sdk-parent/frontend/src/api/apps.js`（`GET /services`）
- 按需加载容器：`framework/sdk-parent/frontend/src/components/MicroApp.vue`
- 子应用 publicPath：`*/frontend/src/public-path.js`
- 子应用构建配置：`*/frontend/vue.config.js`（UMD + chunkLoadingGlobal）
- 请求 baseURL 规则：`framework/sdk-parent/frontend/src/plugins/request.js`

---

## 11. 与新项目（3.6）对比提示

新项目 `metersphere-3.6` 已不再使用 qiankun（详见：`metersphere-3.6/docs/技术文档/开发指南/项目结构分析.md` 中“移除技术”部分）。

因此旧项目继续维护 qiankun 时，建议：

- 明确“保守修复”范围：优先修稳定性与排障能力（publicPath、chunk 冲突、路由回显等）
- 避免扩展复杂度：减少新增跨应用强耦合功能，防止迁移成本上升
