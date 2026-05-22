# 实施计划：micro-app 运行策略低风险优化

## 概述

本任务清单用于记录本轮 micro-app 低风险优化的 Kiro 格式实施步骤。目标是在不改变现有生命周期语义的前提下，集中主应用与嵌入式 `<micro-app>` 运行策略，修正菜单默认入口和 analytics 二级菜单高亮，收窄 custom fetch 静态资源路径修正逻辑，并同步 parent frontend lockfile。

## 任务

- [x] 1. 集中 micro-app 运行策略 helper
  - [x] 1.1 在 `micro-app-config.js` 新增主应用运行策略 helper
    - 新增并导出 `getMainMicroAppRuntimePolicy(moduleName)`
    - 保持主应用默认策略不变：`iframe: isViteApp(moduleName)`、`fiber: true`、`destroy: false`、`inline: true`、`disableMemoryRouter: true`、`disableScopecss: true`
    - 复用现有 `isViteApp()` 判断 iframe 沙箱
    - 文件：`framework/sdk-parent/frontend/src/micro-app-config.js`
    - _Requirements: Runtime policy centralization, Preserve main app behavior_
  - [x] 1.2 在 `micro-app-config.js` 新增嵌入式运行策略 helper
    - 新增并导出 `getEmbedMicroAppRuntimePolicy(moduleName)`
    - 保持嵌入式默认策略不变：`iframe: isViteApp(moduleName)`、`fiber: true`、`destroy: true`、`clearData: true`
    - 不引入主应用场景的 `inline`、`disableMemoryRouter`、`disableScopecss`
    - 文件：`framework/sdk-parent/frontend/src/micro-app-config.js`
    - _Requirements: Runtime policy centralization, Preserve embedded app behavior_

- [x] 2. 主应用和嵌入式容器接入运行策略
  - [x] 2.1 修改 AppLayout 使用主应用运行策略
    - 从 `micro-app-config.js` 引入 `getMainMicroAppRuntimePolicy`
    - 新增 `microAppPolicy` computed，按 `currentApp.name` 获取策略
    - `<micro-app>` 模板继续保留显式 kebab-case 绑定，分别绑定 `destroy`、`fiber`、`iframe`、`inline`、`disable-memory-router`、`disable-scopecss`
    - 不改变 `destroy=false` 与 `:key="currentApp.name"` 的现有语义
    - 文件：`framework/sdk-parent/frontend/src/business/app-layout/index.vue`
    - _Requirements: Runtime policy centralization, Preserve main app behavior_
  - [x] 2.2 修改 MicroAppWrapper 使用嵌入式运行策略
    - 从 `micro-app-config.js` 引入 `getEmbedMicroAppRuntimePolicy`
    - 新增 `microAppPolicy` computed，按 `this.service` 获取策略
    - `<micro-app>` 显式绑定 `iframe`、`destroy`、`clear-data`、`fiber`
    - 保持嵌入式 disposable 行为，不引入主应用运行字段
    - 文件：`framework/sdk-parent/frontend/src/components/MicroAppWrapper.vue`
    - _Requirements: Runtime policy centralization, Preserve embedded app behavior_

- [x] 3. 预加载复用主运行策略中的 iframe 判断
  - [x] 3.1 修改 preFetchApps 的 iframe 配置来源
    - 从 `micro-app-config.js` 引入 `getMainMicroAppRuntimePolicy`
    - 在 `preFetchApps()` 中使用 `getMainMicroAppRuntimePolicy(moduleName).iframe` 决定是否传入 `{ iframe: true }`
    - 本轮不补齐未验证的 `inline`、`disable-scopecss`、`disable-memory-router` 等 prefetch 字段
    - 文件：`framework/sdk-parent/frontend/src/micro-app-setup.js`
    - _Requirements: Prefetch policy alignment, Low-risk incremental change_

- [x] 4. 收窄 custom fetch 静态资源路径修正逻辑
  - [x] 4.1 移除粗糙的字符串提前判断
    - 删除 `url.indexOf('/js/') === -1 && url.indexOf('/css/') === -1` 的提前返回逻辑
    - 保持默认返回 `window.fetch(url, options).then(res => res.text())`
    - 文件：`framework/sdk-parent/frontend/src/micro-app-setup.js`
    - _Requirements: Custom fetch boundary, Promise<string> contract_
  - [x] 4.2 使用 URL pathname 判断根级 JS/CSS 静态资源
    - 使用 `new URL(url, window.location.origin).pathname` 获取 pathname
    - 仅当 pathname 命中根级 `/js/...` 或 `/css/...` 且尚未带 `/{moduleName}/` 前缀时补全模块前缀
    - URL 解析失败时记录 warning 并降级为原始 fetch
    - 不改变 custom fetch 的 `Promise<string>` 返回契约
    - 文件：`framework/sdk-parent/frontend/src/micro-app-setup.js`
    - _Requirements: Custom fetch boundary, Static resource path correctness_

- [x] 5. 修正主菜单默认入口和 analytics 二级菜单高亮
  - [x] 5.1 修改 API 菜单直接进入默认页
    - 将 API 菜单 `index` 从 `/api` 改为 `/api/home`
    - 删除 API 菜单 `@click="active()"` 绑定
    - 删除 `active()` 方法
    - 保留 API 子应用内部 `/api -> /api/home` redirect 以兼容老地址
    - 文件：`framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue`
    - _Requirements: Menu canonical route, Avoid double navigation_
  - [x] 5.2 修改 analytics 菜单直接进入默认页
    - 将 analytics 菜单 `index` 从 `/analytics` 改为 `/analytics/knowledge/chat`
    - 保留 analytics 子应用内部 `/analytics -> /analytics/knowledge/chat` redirect 以兼容老地址
    - 文件：`framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue`
    - _Requirements: Menu canonical route, Avoid double navigation_
  - [x] 5.3 修复 analytics 二级菜单最长前缀匹配
    - 将 `menus.find((m) => path.startsWith(m.path))` 改为按 path 长度倒序匹配
    - 使用 `path === m.path || path.startsWith(`${m.path}/`)` 避免非 slash-bound 前缀误匹配
    - 确保 `/analytics/knowledge/chat` 高亮知识问答菜单，而不是知识库菜单
    - 文件：`analytics-stat/frontend/src/business/head/KnowledgeBaseHeaderMenus.vue`
    - _Requirements: Analytics submenu highlight correctness_

- [x] 6. 同步 parent frontend lockfile
  - [x] 6.1 执行 package-lock-only 依赖同步
    - 在 `framework/sdk-parent/frontend` 执行 `npm install --package-lock-only`
    - 访问 npm 时使用检测到的代理 `http://127.0.0.1:7890`
    - 不执行 `npm update`，不主动升级业务依赖
    - 文件：`framework/sdk-parent/frontend/package-lock.json`
    - _Requirements: Dependency lock consistency_
  - [x] 6.2 检查 lockfile 依赖结果
    - 确认生成的 lockfile root dependencies 中包含 `@micro-zoe/micro-app`
    - 确认不再出现 `qiankun` 直接依赖
    - 记录当前 `framework/sdk-parent/frontend/.gitignore` 仍忽略 `package-lock.json`，是否纳入版本控制需单独决策
    - _Requirements: Dependency lock consistency, Review unexpected drift_

- [x] 7. 验证本轮改动
  - [x] 7.1 执行静态 diff 检查
    - 执行 `git diff --check`
    - 确认本轮代码和文档改动无 whitespace error
    - _Requirements: Static validation_
  - [x] 7.2 构建主应用前端
    - 在 `framework/sdk-parent/frontend` 执行 `npm run build`
    - 构建通过；存在既有 browserslist、Sass deprecation、asset size warning
    - _Requirements: Build validation_
  - [x] 7.3 构建 analytics 子应用
    - 在 `analytics-stat/frontend` 执行 `npm run build`
    - 构建通过；存在既有 chunk size warning
    - _Requirements: Build validation_
  - [x] 7.4 构建 API 子应用
    - 在 `api-test/frontend` 执行 `npm run build`
    - 构建通过；存在既有 browserslist、Sass deprecation、asset size warning
    - _Requirements: Build validation_

## 备注

- 本轮只做低风险集中化和边界修正，不升级 `@micro-zoe/micro-app` 到正式版。
- 本轮不改变主应用 `destroy=false` 的生命周期语义。
- 本轮不全局开启 scoped CSS，不引入 keep-alive，不重写所有子应用 bootstrap。
- `package-lock.json` 当前被 `framework/sdk-parent/frontend/.gitignore` 忽略，若要真正提交 lockfile，需要单独调整 ignore 规则并 review lockfile diff。
- 浏览器回归仍需在集成环境或本地联调环境验证菜单跳转、子应用加载、嵌入式 MicroAppWrapper、生产 JS/CSS 资源路径。
