# 项目经验记录

> 记录在 MeterSphere 二次开发过程中积累的非显而易见的经验，避免重复踩坑。

---

## 2026-02-25: analytics-stat 微前端加载报错 "element head is missing" 排查

### 背景
访问 `http://127.0.0.1:8000/#/analytics` 时，控制台报错 `[micro-app] element head is missing`。

### 关键发现

1. **micro-app 报错 "element head is missing" 的本质**：子应用入口 URL 返回的不是合法 HTML（而是 JSON），micro-app 解析不到 `<head>` 标签。遇到此错误时，优先检查子应用入口 URL 的实际响应内容，而非 micro-app 配置本身。

2. **Gateway 的 401 响应会掩盖真实的 404**：`GET /analytics` 实际返回的 HTTP 状态码是 500，响应体是 `{"success":false,"message":"401 UNAUTHORIZED ..."}` 的 JSON。但真实原因是 analytics-stat 后端根路径返回 404（没有 index.html），Gateway 在处理过程中将其包装为认证错误。排查时不能只看 Gateway 返回的状态码，要直接访问子服务端口验证。

3. **前端产物的部署结构**：MeterSphere 各模块的前端构建产物通过 Maven antrun 插件分两部分复制：
   - `*.html` → `backend/src/main/resources/public/`（Spring Boot 的 public 目录，用于提供首页）
   - 其他文件（css/js/fonts/img）→ `backend/src/main/resources/static/`
   - 如果 `static/` 下有 css/js 但 `public/` 目录不存在，说明前端构建不完整或 antrun 复制步骤被跳过。

4. **快速验证子服务是否正常提供前端资源的方法**：直接 curl 子服务端口的根路径，例如 `curl http://127.0.0.1:8009/`，如果返回 404 JSON 而非 HTML，说明前端资源未正确部署。

### 排查路径（推荐顺序）
```
1. 浏览器 DevTools → Network → 检查子应用入口请求的实际响应内容和状态码
2. 直接 curl 子服务端口（绕过 Gateway）→ 确认是子服务问题还是 Gateway 问题
3. 检查 backend/src/main/resources/public/ 是否有 index.html
4. 检查 frontend/dist/ 是否存在 → 确认前端是否构建过
5. 检查后端 pom.xml 的 antrun 配置 → 确认复制规则是否正确
```

### 标签
`micro-app` `微前端` `analytics-stat` `构建` `排查模式`

---

## 2026-02-25: MeterSphere 前端模块布局组件定位模式

### 背景
需要修改 analytics-stat 模块左上角的项目切换组件。

### 关键发现

1. **各模块顶部导航栏的组件定位路径**：每个业务模块的顶部导航栏（包含项目切换、二级菜单、右上角按钮组）位于 `<模块>/frontend/src/business/head/<模块名>HeaderMenus.vue`。这是项目约定的命名模式，参考了 `report-stat` 的 `ReportStatisticsHeaderMenus.vue`。

2. **二级布局组件的嵌套结构**：路由配置中通过 `router/index.js` 的 `forEach` 循环，将每个子路由的 component 替换为二级布局组件（如 `AnalyticsStat.vue`），原组件下沉为孙子路由。这意味着修改布局时，要改的是 `business/<模块名>.vue` 和 `business/head/<模块名>HeaderMenus.vue`，而不是路由配置。

3. **公共组件来源**：项目切换（`ProjectSwitch`）、右上角按钮组（`MsHeaderRightMenus`）等公共 UI 组件统一来自 `metersphere-frontend/src/components/`，各模块通过 import 引用。移除或替换时只需改模块自己的 HeaderMenus 文件，不影响其他模块。

### 标签
`前端布局` `组件定位` `项目约定` `analytics-stat`

---

## 2026-02-25: micro-app 对 Vite 子应用的支持与 Steering 文档不一致问题

### 背景
评估 analytics-stat 前端能否从 Vue 2 + Webpack 升级到 Vue 3 + Vite + TypeScript，发现选型文档中大量引用 qiankun，但项目实际已切换到 micro-app。

### 关键发现

1. **项目微前端框架已从 qiankun 切换到 micro-app**：`framework/sdk-parent/frontend/package.json` 中依赖的是 `@micro-zoe/micro-app ^1.0.0-rc.4`，所有子应用（api-test、test-track、analytics-stat）的 `main.js` 和 `public-path.js` 都已适配 micro-app 的 UMD 生命周期模式（`window.mount` / `window.unmount`），环境检测使用 `window.__MICRO_APP_ENVIRONMENT__`。但 steering 文档（product.md、structure.md）中仍然写着 qiankun，需要注意以代码为准。

2. **micro-app 对 Vite 构建子应用的支持情况**：
   - 生产环境：完全支持，Vite build 产出普通 JS，micro-app 通过 fetch HTML → 解析 `<script>` 标签正常加载
   - 开发环境：Vite dev server 使用原生 ESM（`<script type="module">`），需要在主应用 `<micro-app>` 标签上配置 `iframe` 沙箱模式或 `esmodule` 属性，且子应用需配置 CORS headers
   - 不需要 `vite-plugin-qiankun`，micro-app 不要求 UMD 打包格式，它直接解析 HTML

3. **micro-app 与 qiankun 的关键差异（影响 Vite 集成）**：
   - qiankun 要求子应用导出 `bootstrap/mount/unmount` 生命周期函数，且需要 UMD 格式打包 → Vite 需要额外插件（`vite-plugin-qiankun`）
   - micro-app 通过 WebComponent 容器加载子应用 HTML，子应用只需暴露 `window.mount/unmount` → Vite 无需特殊插件，天然兼容
   - 这是 micro-app 对 Vite 支持更好的根本原因

4. **现有子应用的 micro-app 适配模式**（供新模块参考）：
   - 环境检测：`import { isMicroAppEnv } from 'metersphere-frontend/src/utils/micro-app-env'`
   - 公共路径：`import { getMicroAppPublicPath } from 'metersphere-frontend/src/utils/micro-app-env'`
   - 事件通信：`import { createEventBusAdapter } from 'metersphere-frontend/src/utils/micro-app-event-bus'`
   - 路由布局：微前端环境下用 PassThrough 透传组件替换 Layout，避免重复侧边栏
   - vue.config.js：去掉 `library` 和 `libraryTarget: 'umd'`，保留 `chunkLoadingGlobal` 和 `globalObject: 'window'`

### 标签
`micro-app` `Vite` `Vue3` `技术选型` `微前端` `analytics-stat`

---

## 2026-02-25: Vue 2 → Vue 3 + Vite + TypeScript 迁移实战经验

### 背景
将 analytics-stat 前端从 Vue 2.7 + Webpack (Vue CLI) + JavaScript 迁移到 Vue 3.4 + Vite 5 + TypeScript 5。

### 关键发现

1. **必须清理 node_modules 再安装**：从 Vue 2 切换到 Vue 3 时，如果不删除 `node_modules` 和 `package-lock.json` 直接 `npm install`，会导致 `vue-demi` 版本冲突。Pinia 内部依赖的 `vue-demi` 会尝试用 `import Vue from 'vue'`（Vue 2 的默认导出方式），而 Vue 3 没有默认导出，构建时报错 `"default" is not exported by vue.runtime.esm-bundler.js`。解决方案：`rm -rf node_modules package-lock.json && npm install`。

2. **metersphere-frontend（Vue 2 SDK）完全不可用**：Vue 3 项目无法引用 Vue 2 的组件库。以下 SDK 功能需要在 Vue 3 中重新实现或替代：
   - 布局组件（MsContainer / MsAsideContainer / MsMainContainer）→ Element Plus 的 el-container / el-aside / el-main
   - 顶部导航（MsHeaderRightMenus）→ 自行实现简化版
   - 图标系统（svg-sprite-loader + SDK icons）→ @element-plus/icons-vue
   - 公共插件/指令/过滤器 → 按需在 Vue 3 中重写
   - 路由权限控制 → 暂不需要（独立模块）
   - 登录页 → 暂不需要（微前端环境由主应用处理）
   - i18n 翻译合并（element-ui / fit2cloud-ui / metersphere-frontend）→ Element Plus 内置 i18n，只保留模块自身翻译

3. **Vue 3 迁移的关键语法变化清单**：
   - `slot-scope="{ row }"` → `#default="{ row }"`（插槽语法）
   - `<div slot="header">` → `<template #header>`（具名插槽）
   - `this.$t()` → `const { t } = useI18n()`（i18n）
   - `this.$route` → `const route = useRoute()`（路由）
   - `this.$router` → `const router = useRouter()`（路由）
   - `this.$message` → `import { ElMessage } from 'element-plus'`（消息提示）
   - `data() { return {} }` → `const xxx = ref()`（响应式数据）
   - `watch: { '$route.path': {} }` → `computed(() => route.path)`（计算属性替代 watch）
   - `mounted()` → `onMounted()`（生命周期）
   - `activated()` → `onActivated()`（keep-alive 激活）
   - Element UI 的 `<i class="el-icon-xxx">` → Element Plus 的 `<el-icon><Xxx /></el-icon>`（图标组件化）
   - `<el-button type="text">` → `<el-button type="primary" link>`（文字按钮）

4. **Vite 构建配置要点**：
   - `cssCodeSplit: false` — 单 CSS 文件输出，micro-app 加载更简单
   - 输出文件名带 `analytics-stat-` 前缀 — 避免多子应用 chunk 冲突
   - `server.cors: true` + `server.origin` — micro-app 跨域加载必需
   - 不需要 `library` / `libraryTarget: 'umd'` — micro-app 不要求 UMD 格式
   - 不需要 `svg-sprite-loader` — 不再使用 SDK 的 SVG 图标系统

5. **package.json 的 build 脚本建议**：
   - 默认 `build` 命令不包含 `vue-tsc --noEmit`，避免 CI 环境因类型错误阻断构建
   - 单独提供 `build:check` 命令用于本地开发时的类型检查
   - 原因：迁移初期可能有类型不完善的地方，不应阻断 Maven 构建流程

6. **依赖包数量对比**：Vue 2 + Webpack + metersphere-frontend 约 1100+ 个包，Vue 3 + Vite 约 110 个包，减少 90%。构建速度从 Webpack 的 30-60 秒降到 Vite 的 12 秒。

7. **pom.xml 无需修改**：Maven 的 frontend-maven-plugin 执行的是 `npm install` + `npm run build`，与 Vite 的 package.json scripts 完全兼容，不需要改 pom.xml。

### 迁移文件清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `vite.config.ts` | 替代 vue.config.js |
| 新增 | `tsconfig.json` / `tsconfig.node.json` | TypeScript 配置 |
| 新增 | `env.d.ts` | Vue SFC + micro-app 类型声明 |
| 新增 | `index.html`（根目录） | Vite 入口 HTML（替代 public/index.html） |
| 新增 | `src/main.ts` | 替代 main.js |
| 新增 | `src/micro-app-env.ts` | 替代从 SDK 导入的 micro-app-env |
| 新增 | `src/router/index.ts` | 替代 router/index.js + modules/analytics.js |
| 新增 | `src/i18n/index.ts` | 替代 i18n/index.js |
| 新增 | `src/i18n/lang/*.ts` | 替代 *.js（去掉 SDK 翻译合并） |
| 重写 | 所有 `.vue` 文件 | Options API → Composition API + `<script setup>` |
| 删除 | `vue.config.js` | Vite 替代 |
| 删除 | `babel.config.js` | Vite 不需要 Babel |
| 删除 | `src/public-path.js` | Vite 不使用 __webpack_public_path__ |
| 删除 | `src/store/index.js` | 不再依赖 SDK 的 user store |
| 删除 | `.npmrc` | 不再需要 SDK 的 npm 配置 |
| 保留 | `pom.xml` | 无需修改 |

### 标签
`Vue3` `Vite` `TypeScript` `迁移` `analytics-stat` `Element Plus` `vue-demi`

---

## 2026-02-25: Maven antrun 前端产物复制规则对 Vite 构建的兼容性

### 背景
analytics-stat 前端从 Webpack (Vue CLI) 迁移到 Vite 后，需要确认后端 pom.xml 的 antrun 复制规则是否仍然有效。

### 关键发现

1. **antrun 复制规则天然兼容 Vite 产物**：后端 pom.xml 的 antrun 规则只区分 `*.html` 和非 `*.html`，不关心具体的子目录名称。Webpack 产出 `dist/css/` + `dist/js/` + `dist/index.html`，Vite 产出 `dist/assets/` + `dist/js/` + `dist/index.html`，两者都能被正确复制。这意味着切换前端构建工具时，**后端 pom.xml 和前端 pom.xml 都不需要修改**。

2. **Spring Boot 静态资源路径映射的隐含约定**：Vite 生成的 `index.html` 中资源引用路径是绝对路径（如 `/js/xxx.js`、`/assets/xxx.css`）。antrun 将这些文件复制到 `static/js/` 和 `static/assets/` 后，Spring Boot 默认将 `classpath:/static/` 映射到根路径 `/`，所以 `/js/xxx.js` 能正确解析到 `static/js/xxx.js`。这个映射关系是隐含的，容易被忽略。

3. **验证打包兼容性的快速方法**：不需要跑完整 Maven 构建，只需 `npx vite build` 后检查 `dist/` 目录结构，对照 antrun 的 `<fileset>` 和 `<include>`/`<exclude>` 规则即可判断。

### 标签
`Maven` `antrun` `Vite` `Webpack` `构建` `Spring Boot` `静态资源`

---

## 2026-02-25: Vite 构建产物在 micro-app 环境下加载失败的两个根因

### 背景
analytics-stat 前端迁移到 Vue 3 + Vite 后，`npm run build` 产物部署到后端 static 目录，通过 `http://127.0.0.1:8000/#/analytics` 访问时页面空白，控制台报 `SyntaxError: Unexpected token 'export'`。

### 关键发现

1. **micro-app 不支持 ESM 格式的子应用脚本**：micro-app 加载子应用的流程是 fetch HTML → 解析 `<script>` 标签 → 通过 `appendChild` 注入到主应用 DOM。`appendChild` 注入的 script 默认以 classic 模式执行，而 Vite 默认输出 ESM 格式（带 `export`），classic 模式下 `export` 是非法语法。解决方案：`rollupOptions.output.format: 'iife'`，将所有代码包裹在立即执行函数中，消除顶层 `export`。

2. **仅设置 `format: 'iife'` 不够，还需移除 `type="module"`**：Vite 在 `index.html` 中注入 `<script type="module" src="...">`，即使 JS 内容是 IIFE 格式，`type="module"` 仍会让浏览器以 ESM 模式解析（严格模式差异、作用域隔离等问题）。需要自定义 Vite 插件通过 `transformIndexHtml` 钩子将 `type="module"` 替换为 `defer`，同时移除 `crossorigin` 属性。

3. **CSS 必须输出到 `css/` 目录前缀，不能用 Vite 默认的 `assets/`**：Gateway 的静态资源路由规则只识别 `/js/` 和 `/css/` 路径前缀。其他 Vue 2 子应用的 Webpack 构建产物天然输出到 `css/` 和 `js/` 目录。Vite 默认将所有资源（包括 CSS）输出到 `assets/` 目录，导致 CSS 404。解决方案：在 `rollupOptions.output.assetFileNames` 中用函数判断文件扩展名，CSS 文件路由到 `css/` 目录。

4. **IIFE 格式不支持代码分割**：设置 `format: 'iife'` 后，Vite/Rollup 会将所有代码打包到单个入口文件，`chunkFileNames` 配置无效。这对 analytics-stat 这种轻量模块没有影响，但如果子应用体积较大，需要考虑使用 `format: 'umd'` 或其他方案。

5. **部署时 `static/index.html` 和 `public/index.html` 都需要更新**：Spring Boot 同时从 `classpath:/static/` 和 `classpath:/public/` 提供静态资源。antrun 将 `*.html` 复制到 `public/`，其他文件复制到 `static/`。但如果手动部署（不走 Maven），容易遗漏其中一个位置，导致 HTML 引用的资源路径与实际文件位置不匹配。

### 完整的 Vite 配置要点（micro-app 子应用）

```typescript
// vite.config.ts 关键配置
build: {
  cssCodeSplit: false,                    // 单 CSS 文件，micro-app 加载更简单
  rollupOptions: {
    output: {
      format: 'iife',                     // 非 ESM，避免 appendChild 注入报错
      entryFileNames: 'js/[name].[hash].js',  // js/ 前缀，匹配 gateway 路由
      assetFileNames: (info) => {
        if (info.name?.endsWith('.css')) return 'css/[name].[hash].[ext]'  // css/ 前缀
        return 'assets/[name].[hash].[ext]'
      },
    },
  },
}
// + 自定义插件移除 type="module" 和 crossorigin
```

### 排查路径（推荐顺序）
```
1. 浏览器 DevTools → Console → 看是否有 SyntaxError（ESM 相关）
2. 检查 dist/index.html 中 <script> 标签是否有 type="module"
3. 检查 dist/ 目录结构，CSS 是否在 css/ 目录下（而非 assets/）
4. 直接 curl 子服务端口的 CSS/JS 路径，确认 404 还是 200
5. 对比其他 Vue 2 子应用的 dist/ 目录结构作为参照
```

### 标签
`micro-app` `Vite` `IIFE` `ESM` `构建` `analytics-stat` `CSS路径` `gateway路由`

---

## 2026-02-25: micro-app 子应用 CSS 污染主应用样式的排查与修复

### 背景
analytics-stat 子应用（Vue 3 + Element Plus）加载后，主应用（Vue 2 + Element UI）左侧导航栏的图标全部下沉，选中任何模块都会出现。

### 根因
Element Plus 和 Element UI 有大量同名 CSS 选择器。micro-app 默认不隔离样式，子应用的 CSS 通过 `<style>` 标签注入到主应用的 `<head>` 中。由于后加载的样式优先级更高（相同选择器权重时，后出现的规则覆盖先出现的），Element Plus 的规则覆盖了 Element UI 的同名规则。

具体到本次问题：
- Element UI（主应用）：`.el-menu-item * { vertical-align: middle }` → 图标垂直居中 ✅
- Element Plus（子应用）：`.el-menu-item * { vertical-align: bottom }` → 图标沉底 ❌

### 关键发现

1. **排查方法**：用 `element.matches(rule.selectorText)` 遍历所有 `document.styleSheets` 的 `cssRules`，找出匹配目标元素的所有规则及其来源（`sheetIndex`、`ownerNode.tagName`、`href`）。这比 DevTools 的 Computed Styles 面板更高效，因为可以一次性看到所有匹配规则的来源。

2. **Element Plus 与 Element UI 的冲突选择器不止一个**：除了 `.el-menu-item *`，还有 `.el-button`、`.el-input`、`.el-table` 等大量同名选择器。只修复单个规则是治标不治本，必须从根本上隔离样式作用域。

3. **`postcss-prefix-selector` 是最佳方案**：在 Vite 的 `css.postcss.plugins` 中配置，给所有 CSS 规则自动加上 `#analytics-app` 前缀（子应用根元素的 ID）。这样 `.el-menu-item *` 变成 `#analytics-app .el-menu-item *`，只在子应用容器内生效，不影响主应用。

4. **prefix transform 函数需要跳过的选择器**：
   - `html`、`body`、`:root` — 全局选择器不应加前缀，否则 CSS 变量（`--el-color-primary` 等）无法生效
   - 已包含 `#analytics-app` 的选择器 — 避免重复添加
   - `from`、`to`、百分比 — `@keyframes` 内的选择器不是 DOM 选择器

5. **micro-app 的样式隔离能力有限**：micro-app 提供了 `shadowDom` 和 `scopecss` 两种样式隔离模式，但 `shadowDom` 会导致 Element Plus 的弹窗（Popover、Dialog 等）挂载到 body 时样式丢失，`scopecss` 只给子应用的 CSS 加前缀但不够精确。`postcss-prefix-selector` 在构建时处理，更可控。

### 配置示例

```typescript
// vite.config.ts
import prefixSelector from 'postcss-prefix-selector'

export default defineConfig({
  css: {
    postcss: {
      plugins: [
        prefixSelector({
          prefix: '#analytics-app',  // 子应用根元素 ID
          transform(prefix, selector, prefixedSelector) {
            if (selector.match(/^(html|body|:root)/)) return selector
            if (selector.includes('#analytics-app')) return selector
            if (selector.match(/^(from|to|\d+%)/)) return selector
            return prefixedSelector
          },
        }),
      ],
    },
  },
})
```

### 安装依赖
```bash
npm install postcss-prefix-selector --save-dev
```

### 标签
`micro-app` `Element Plus` `Element UI` `CSS污染` `样式隔离` `postcss-prefix-selector` `analytics-stat`

## [LRN-20260225-001] best_practice

**Logged**: 2026-02-25T18:00:00Z
**Priority**: medium
**Status**: pending
**Area**: config

### 摘要
Cursor/Claude Code 的 self-improving-agent 技能可以完整移植到 Kiro，通过 steering + hooks 实现等价功能

### 详情
self-improving-agent（v1.0.11）是为 Cursor/Claude Code 设计的技能，核心机制是：SKILL.md 描述行为 + hooks 自动触发 + .learnings/ 存储经验。Kiro 没有 SKILL.md 和 CLAUDE.md 的概念，但有完全对应的机制：

| 原版概念 | Kiro 对应 |
|----------|-----------|
| SKILL.md（行为描述） | `.kiro/steering/self-improvement.md`（auto inclusion） |
| CLAUDE.md（经验晋升目标） | `.kiro/steering/` 目录下新建 steering 文件 |
| `.claude/settings.json` hooks | `.kiro/hooks/` 目录下的 hook JSON |
| `UserPromptSubmit` 事件 | `promptSubmit` 事件 |
| `PostToolUse` 事件 | `postToolUse` 事件 |
| 无直接对应 | `agentStop` 事件（Kiro 独有，更适合做任务后评估） |
| `.learnings/` 目录 | 直接复用，无需改动 |

关键差异：Kiro 的 `agentStop` 事件比原版的 `UserPromptSubmit` 更适合做经验评估，因为它在任务完成后触发，此时上下文最完整。原版在每次 prompt 提交时触发评估，实际上大部分时候任务还没完成，评估时机不理想。

### 建议操作
后续移植其他 Cursor/Claude Code 技能时，参考此映射关系。优先使用 `agentStop` 做任务后评估，`promptSubmit` 做任务前经验回顾。

### 元数据
- Source: conversation
- Related Files: .kiro/steering/self-improvement.md, docs/self-improving-agent-1.0.11/SKILL.md
- Tags: kiro, cursor, 技能移植, hooks, steering

---

## [LRN-20260225-002] best_practice

**Logged**: 2026-02-25T20:00:00Z
**Priority**: high
**Status**: pending
**Area**: frontend

### 摘要
Element UI el-table 在有 fixed 列时会将整个表格 DOM 复制 3 份，列数多时导致 DOM 节点爆炸式增长，是页面渲染慢的首要原因

### 详情
在排查 MeterSphere 缺陷管理页面（`#/track/issue`）加载慢的问题时，发现以下非显而易见的性能陷阱：

1. **Element UI el-table 的 fixed 列 DOM 三倍膨胀**：
   - el-table 在有 `fixed="left"` 和 `fixed="right"` 列时，会将整个表格（包括所有列和所有行）复制 3 份：主体表格 + 左固定区域 + 右固定区域
   - 实测：22 个可见列，仅 8 行数据，实际产生 76 个 `<th>` 元素和 3479 个 DOM 节点（占页面总 DOM 的 76%）
   - 正式环境 50 行 × 22 列 × 3 份 = DOM 节点轻松超过 2 万，直接导致 Recalculate Style 耗时 2734ms（Self Time 占比 24.2%）
   - 这个问题在列数少（< 8 列）时不明显，只有在自定义字段多（20+ 列）时才会爆发

2. **MsTable 的 `doLayout()` 被设计为执行 3 次**：
   ```javascript
   doLayout() {
     for (let i = 1; i <= 3; i++) {
       setTimeout(this.$refs.table.doLayout, 300 * i);
     }
   }
   ```
   - 注释说是"解决表格错位问题"，但每次 doLayout 都会触发整个表格（含 3 份 DOM）的重新布局计算
   - 在 `data` watcher 中也调用了 doLayout，意味着每次数据变化都触发 3 次重排
   - 这是 Total Blocking Time 高达 9594ms 的重要原因之一

3. **MsTableColumn 的 `render-header` 函数**：
   - 每个 MsTableColumn 都通过 `renderHeader` 函数动态渲染表头（判断文字长度 > 7 则加 tooltip）
   - 22 列 × 3 份 = 66 次 render-header 调用，产生 38 个 Element UI 警告
   - Element UI 推荐使用 scoped-slot header 替代 render-header

4. **切换项目时的 API 请求风暴**：
   - `activated()` 中的初始化是串行链式调用：getUserGroupProject → getProjectMember → getIssuePartTemplateWithProject → applyUserGroupFilter → getIssues
   - 同时还有 3 个并行请求
   - 总共 21 个 API 请求，测试环境单个请求耗时 300-1886ms

### 排查方法（推荐复用）
```
1. 注入 XHR 拦截器记录所有 API 请求耗时和响应大小
2. 用 JS 统计 DOM 节点数：document.querySelectorAll('*').length 和 .el-table * 的数量
3. 检查 el-table 的 <th> 数量是否是可见列数的 3 倍（有 fixed 列时）
4. 查看控制台 [Element Warn][TableColumn] render-header 警告数量
5. Performance 面板关注 Recalculate Style 的 Self Time
```

### 优化方向（按优先级）
1. 减少默认显示列数到 8-10 个（最直接有效）
2. 将 doLayout 的 3 次调用改为 1 次，使用 requestAnimationFrame 合并
3. 将串行 API 调用改为 Promise.all 并行
4. 考虑虚拟滚动（数据量大时）
5. 缓存不变数据（所属系统列表、平台状态等）

### 建议操作
排查 MeterSphere 任何页面的渲染性能问题时，首先检查 el-table 的列数和是否有 fixed 列。列数 × 行数 × 3（fixed 倍数）是 DOM 节点的主要来源。

### 元数据
- Source: conversation
- Related Files: test-track/frontend/src/business/issue/IssueList.vue, framework/sdk-parent/frontend/src/components/table/MsTable.vue, framework/sdk-parent/frontend/src/components/table/MsTableColumn.vue
- Tags: 性能优化, el-table, fixed列, DOM膨胀, doLayout, render-header, 缺陷管理
- See Also: LRN-20260225-001

---

## [LRN-20260225-003] best_practice

**Logged**: 2026-02-25T22:00:00Z
**Priority**: high
**Status**: pending
**Area**: backend

### 摘要
MeterSphere 第三方平台集成接口（Jira/禅道/Tapd）缺少容错机制，平台不可用时会导致整个页面 500 并长时间卡死

### 详情
在排查缺陷列表页 `/issues/platform/status` 返回 500 的问题时，发现以下非显而易见的设计缺陷：

1. **后端 `IssuesService.getPlatformStatus()` 没有 try-catch**：
   - 当项目绑定的是第三方平台（如 Jira），方法会调用 `platformPluginService.getPlatform(platform).getStatusList(projectConfig)`
   - `getStatusList()` 内部发 HTTP 请求到第三方平台获取状态列表
   - 如果第三方平台停服/网络不通，HTTP 连接超时（默认 30-60 秒）后抛出 `ConnectTimeoutException`
   - 异常未被捕获，冒泡到 Controller 层，Spring Boot 返回 500
   - 方法中只有 `platform == Local` 时的短路返回，没有任何对外部调用的防御性处理

2. **前端重复调用加剧问题**：
   - `IssueList.vue` 的 `activated()` 中调用一次 `getPlatformStatus()`（获取状态映射）
   - `getPlatformStatusFiltes()` 方法中又调用一次（获取表头过滤选项）
   - 两个请求同时卡在等第三方平台超时，页面要等两个都超时完才能恢复
   - 且两次调用的参数完全相同，属于不必要的重复请求

3. **这个模式在项目中可能是普遍的**：
   - `getPlatformTransitions()` 也有同样的问题（无 try-catch）
   - `syncIssues()`、`syncAllIssues()` 等同步接口也直接调用第三方平台
   - 项目中所有 `platformPluginService.getPlatform(platform).xxx()` 的调用点都可能存在同样的风险

4. **判断平台类型的关键方法**：
   - `PlatformPluginService.isPluginPlatform(platform)` — 排除 Tapd、AzureDevops、Local，其余都是插件平台
   - 插件平台走 `platformPluginService.getPlatform().getStatusList()` 路径
   - 非插件平台（Tapd/AzureDevops）走 `IssueFactory.createPlatform().getTransitions()` 路径
   - 两条路径都会请求外部 API，都没有容错

### 建议操作
1. 后端：在 `getPlatformStatus()` 中对 `getStatusList()` 调用加 try-catch，异常时返回空列表而非 500
2. 后端：考虑加缓存（Redis/本地缓存），平台状态列表不需要每次实时获取
3. 前端：合并两次 `getPlatformStatus` 调用为一次，结果共享给 `activated()` 和 `getPlatformStatusFiltes()`
4. 前端：所有第三方平台相关的 API 调用都加 `.catch()` 容错（已对 `getPlatformStatus` 完成）
5. 排查项目中所有 `platformPluginService.getPlatform(xxx).` 的调用点，统一加容错

### 元数据
- Source: conversation
- Related Files: test-track/backend/src/main/java/io/metersphere/service/IssuesService.java, test-track/frontend/src/business/issue/IssueList.vue, test-track/backend/src/main/java/io/metersphere/service/PlatformPluginService.java
- Tags: 第三方平台, Jira, 容错, 超时, 500错误, 缺陷管理, getPlatformStatus
- See Also: LRN-20260225-002

---

## [LRN-20260225-004] best_practice

**Logged**: 2026-02-25T23:00:00Z
**Priority**: high
**Status**: pending
**Area**: infra

### 摘要
MeterSphere Docker 部署的容器内存分配策略和关键风险点

### 详情
通过 `docker stats` 分析正式环境 12 个容器的资源使用情况，发现以下非显而易见的部署特征：

1. **各容器内存限制不一致，有明确的分级策略**：
   - test-track: 4G（最高，其他业务模块的 2 倍）
   - 大部分业务模块（api-test, report-stat, workstation, performance-test, system-setting, project-management, gateway）: 2G
   - ms-node-controller, ms-data-streaming: 1G
   - eureka: 512M（最低）
   - ms-prometheus: 未设限制（默认 31.24G，即宿主机内存）

2. **test-track 被特殊对待分配 4G**：说明团队已经意识到缺陷管理模块内存消耗大。结合 LRN-20260225-002 中发现的 el-table DOM 三倍膨胀问题，后端处理缺陷数据（自定义字段解析、平台状态映射等）确实比其他模块更吃内存。

3. **eureka 512M 限制是高危隐患**：实际使用 66%（337.9M/512M），只剩 ~174M。Eureka 是所有微服务的注册中心，一旦 OOM 被 kill，所有服务间调用全部中断。建议提到 1G。

4. **ms-prometheus 没有内存限制**：Docker 默认给了宿主机几乎全部内存（31.24G）。Prometheus 的内存使用与监控数据量成正比，长期运行可能逐渐增长。应显式设置限制（建议 2G）。

5. **总内存使用约 10.3G**：12 个容器的总内存占用。宿主机至少需要 16G 内存才能稳定运行，32G 更理想。

6. **NET I/O 可以反映服务活跃度**：
   - test-track: 6.92G/4.5G（最高，说明缺陷管理是使用最频繁的模块）
   - api-test: 5.04G/4.05G（第二高）
   - gateway: 3.72G/2.78G（作为入口网关，所有流量经过）

### 建议操作
- eureka 内存限制从 512M 提升到 1G（最紧急）
- ms-prometheus 显式设置内存限制为 2G
- 监控 api-test 内存（64% 使用率，接口并发高时可能触顶）
- 后续排查性能问题时，先看 docker stats 确认是否有容器内存/CPU 异常

### 元数据
- Source: conversation
- Related Files: docker-compose.yml（部署时使用，不在仓库中）
- Tags: Docker, 内存, 部署, eureka, prometheus, 容器资源, 运维
- See Also: LRN-20260225-002, LRN-20260225-003

---

## [LRN-20260225-005] best_practice

**Logged**: 2026-02-25T23:30:00Z
**Priority**: medium
**Status**: pending
**Area**: infra

### 摘要
MeterSphere 的 Eureka 配置关闭了自我保护模式，Gateway 完全依赖 Eureka 服务发现做路由，重启 Eureka 影响窗口约 30-90 秒

### 详情
分析 Eureka 重启对整体应用的影响时，发现以下项目特有的配置：

1. **Eureka Server 关闭了自我保护模式**（`enable-self-preservation=false`）：
   - 默认 Eureka 在网络分区时会保留所有注册实例（自我保护），防止误剔除
   - 项目关闭了这个机制，意味着 Eureka 重启后，如果某个服务还没来得及重新注册，会在 10 秒内被剔除（`eviction-interval-timer-in-ms=10000`）
   - 这个配置适合开发/测试环境（快速感知服务下线），但在生产环境有风险

2. **Gateway 完全依赖 Eureka 做服务发现路由**：
   - `spring.cloud.gateway.discovery.locator.enabled=true` — 所有路由通过 Eureka 服务列表动态生成
   - Gateway 的 `SessionFilter` 和 `LoginController` 直接注入 `DiscoveryClient`，用于会话路由和服务列表展示
   - 这意味着 Eureka 不可用时，Gateway 无法发现新服务，但已缓存的路由仍可用

3. **Eureka Server 关闭了只读响应缓存**（`use-read-only-response-cache=false`）：
   - 默认 Eureka 有三级缓存（registry → readWriteCache → readOnlyCache），客户端从 readOnlyCache 读取
   - 关闭后客户端直接从 readWriteCache 读取，服务注册/下线的感知更快
   - 但也意味着 Eureka 重启后，缓存需要重新构建，恢复时间略长

4. **各微服务的 Eureka Client 使用默认配置**：
   - 没有自定义 `registry-fetch-interval-seconds`（默认 30 秒）
   - 没有自定义 `lease-renewal-interval-in-seconds`（默认 30 秒）
   - 这意味着 Eureka 重启后，最多 30 秒各服务才会重新注册

5. **重启影响时间线**：
   - 0-30 秒：各服务本地缓存有效，用户无感知
   - 30-60 秒：如果 Eureka 还没恢复，Gateway 开始打 WARN 日志但仍可路由
   - 60-90 秒：Eureka 恢复后各服务重新注册，全部恢复
   - 如果 Eureka 重启超过 90 秒，可能出现 503

### 建议操作
- 扩容内存用 `docker update`（热更新，不需要重启）
- 如果必须重启 Eureka，选在用户少的时段
- 考虑在生产环境开启自我保护模式（`enable-self-preservation=true`）

### 元数据
- Source: conversation
- Related Files: framework/eureka/src/main/resources/application.properties, framework/gateway/src/main/resources/application.properties, framework/gateway/src/main/java/io/metersphere/gateway/filter/SessionFilter.java
- Tags: Eureka, Gateway, 服务发现, 运维, 重启影响, 自我保护
- See Also: LRN-20260225-004

---

## [LRN-20260225-006] best_practice

**Logged**: 2026-02-25T23:45:00Z
**Priority**: medium
**Status**: pending
**Area**: frontend

### 摘要
MeterSphere 的 `stopFullScreenLoading` 工具函数内藏 2 秒硬编码延迟，是切换项目/工作空间时用户感知卡顿的隐藏来源之一

### 详情
排查"资源切换中"loading 遮罩为何持续过久时，发现 `framework/sdk-parent/frontend/src/utils/index.js` 中的 `stopFullScreenLoading` 函数默认 `setTimeout(close, 2000)`。这意味着即使所有 API 请求和页面渲染都已完成，用户仍需额外等待 2 秒才能操作页面。

这个延迟的非显而易见之处在于：
1. **Performance 面板不会标记它**：setTimeout 不消耗 CPU，不会出现在 Scripting/Rendering 的火焰图中，只会体现为 Idle 时间
2. **藏在工具函数中**：调用方（`ProjectSearchList.vue`、`HeaderWs.vue` 等）只写 `stopFullScreenLoading(loading)`，看不出有延迟
3. **被 6+ 个组件共享**：切换项目、切换工作空间、切换语言、项目列表跳转、工作台切换项目都受影响
4. **与真实性能问题叠加**：14.1 秒的总耗时中，有 2 秒是这个人为延迟，但在 Performance 录制中它混在 Idle 时间里，容易被忽略

### 建议操作
排查 MeterSphere 前端性能问题时，除了关注 Performance 面板的 Scripting/Rendering 指标，还要检查共享工具函数中是否有人为的 setTimeout 延迟。特别是 `fullScreenLoading` / `stopFullScreenLoading` 这类 loading 管理函数。

### 元数据
- Source: conversation
- Related Files: framework/sdk-parent/frontend/src/utils/index.js, framework/sdk-parent/frontend/src/components/head/ProjectSearchList.vue, framework/sdk-parent/frontend/src/components/head/HeaderWs.vue
- Tags: 性能优化, loading, setTimeout, 隐藏延迟, 工具函数, 切换项目
- See Also: LRN-20260225-002

---

## [LRN-20260225-007] best_practice

**Logged**: 2026-02-25T23:55:00Z
**Priority**: high
**Status**: pending
**Area**: frontend

### 摘要
MeterSphere 使用 MsTable + 自定义字段的页面存在"三重遍历 + resetHeader 重建 + doLayout × 3"的 Scripting 性能陷阱，用例列表页 7954ms Scripting 的完整构成链路

### 详情
在分析用例列表页（`/track/case`，1846 条数据）的 Performance 录制时，拆解出 7954ms Scripting 的完整构成：

1. **API 返回后对 1846 条数据做了 3 次独立的全量遍历**：
   ```javascript
   // TestCaseList.vue 的 getData() 回调中，连续 3 次遍历
   parseCustomFilesForList(this.page.data);  // 遍历 1：每条记录的 fields 数组 → JSON.parse
   parseTag(this.page.data);                  // 遍历 2：每条记录的 tags → JSON.parse
   this.page.data.forEach((item) => {         // 遍历 3：每条记录的 customFields → JSON.parse
     item.customFields = JSON.parse(item.customFields);
   });
   ```
   - 1846 条 × 3 次遍历 = 5538+ 次 JSON.parse（parseCustomFilesForList 内部还有嵌套 forEach）
   - 这 3 次遍历完全可以合并为 1 次，但代码中是分开写的（可能是不同时期不同人加的）
   - 估算耗时：~2s

2. **MsTable 的 `resetHeader()` 会销毁重建整个表格**：
   - `getTemplateField()` 的 Promise.all 完成后调用 `this.$refs.table.resetHeader()`
   - resetHeader 内部将 `tableActive` 设为 false（v-if 销毁表格），$nextTick 后设为 true（重建表格）
   - 重建后又触发一轮 doLayout（3 次 setTimeout）
   - 这和 data watcher 中的 doLayout 是叠加关系：data 变化触发 3 次 doLayout + resetHeader 再触发 3 次 = 最多 6 次 doLayout
   - 估算耗时：~1s

3. **完整的 Scripting 时间线**：
   ```
   created() → getTemplateField() [Promise.all: 3 个 API]     ~2s（含网络等待）
   ↓
   initTableData() → testCaseList() API                        ~1s（网络等待）
   ↓
   API 返回 → 3 次全量遍历 JSON.parse                          ~2s（CPU 密集）
   ↓
   page.data 赋值 → MsTable data watcher → doLayout × 3       ~2s（DOM 重排）
   ↓
   Promise.all 完成 → resetHeader → 表格销毁重建 → doLayout × 3  ~1s（DOM 重排）
   ```

4. **这个模式不只存在于用例列表**：所有使用 MsTable + 自定义字段的页面（缺陷列表、测试计划、用例评审等）都有类似的 parseCustomFilesForList + parseTag + customFields JSON.parse 三重遍历模式，以及 resetHeader 触发的额外 doLayout。

### 建议操作
1. 将 3 次数据遍历合并为 1 次（在 `tableUtils.js` 中新增合并函数，或在 getData 回调中用单次 forEach 处理所有字段）
2. resetHeader 中避免 tableActive 的 false→true 切换，改为直接调用 doLayout
3. doLayout 从 3 次改为 1 次（requestAnimationFrame 合并）
4. 排查其他使用 MsTable 的页面是否有同样的三重遍历模式

### 元数据
- Source: conversation
- Related Files: test-track/frontend/src/business/case/components/TestCaseList.vue, framework/sdk-parent/frontend/src/components/table/MsTable.vue, framework/sdk-parent/frontend/src/utils/tableUtils.js, framework/sdk-parent/frontend/src/utils/index.js
- Tags: 性能优化, Scripting, JSON.parse, 数据遍历, resetHeader, doLayout, MsTable, 用例列表
- See Also: LRN-20260225-002, LRN-20260225-006
