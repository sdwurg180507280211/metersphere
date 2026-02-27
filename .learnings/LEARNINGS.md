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

## [LRN-20260226-001] best_practice

**Logged**: 2026-02-26T10:00:00Z
**Priority**: medium
**Status**: pending
**Area**: docs

### 摘要
跨项目功能迁移评估的系统化方法：先拆组件清单做可复用性对比，再逐项评估适配成本，最后给出分期实施路径

### 详情
将 PaiSmart 知识库检索功能迁移到 MeterSphere analytics-stat 模块时，总结出一套评估方法论：

1. **组件清单对比法**：将源项目的功能拆解为独立组件（文件存储、消息队列、文件解析、向量生成、向量存储、混合检索等），逐一标注目标项目是否已有对应基础设施。这一步能快速识别"需要新增的依赖"和"可直接复用的组件"，是评估的核心。

2. **技术栈差异矩阵**：列出两个项目在 ORM（JPA vs MyBatis）、认证（Spring Security vs Shiro/Gateway）、前端组件库（Naive UI vs Element Plus）、构建工具等维度的差异，每个差异点给出具体的适配方案和工时估算。

3. **分期实施策略**：对于有"入库"和"检索"两个方向的功能，优先实现检索（可手动导入测试数据验证效果），再补全入库链路。这样可以最快验证核心价值，降低一次性投入的风险。

4. **文件清单法**：列出需要迁移的每个源文件、目标路径、改动说明，以及需要新建的文件。这比笼统的"迁移 Service 层"更具可操作性。

5. **权限模型映射**：两个项目的用户/组织模型不同时，需要明确建立映射关系（如 PaiSmart 的 orgTag → MeterSphere 的 workspace_id），而不是试图统一模型。

### 建议操作
后续做跨项目功能迁移评估时，按以下顺序执行：
1. 源项目功能拆解 → 组件清单
2. 目标项目现状分析 → 已有/缺失对比
3. 技术栈差异矩阵 → 逐项适配方案
4. 迁移文件清单 → 源文件 → 目标路径 → 改动说明
5. 基础设施变更 → Docker/配置/依赖
6. 风险评估 + 工时估算
7. 分期实施路径

### 元数据
- Source: conversation
- Related Files: metersphere/docs/知识库检索迁移评估.md, PaiSmart/src/main/java/com/yizhaoqi/smartpai/service/HybridSearchService.java
- Tags: 迁移评估, 方法论, 跨项目, 知识库, analytics-stat
- See Also: LRN-20260225-003

---

## [LRN-20260226-002] best_practice

**Logged**: 2026-02-26T14:00:00Z
**Priority**: high
**Status**: pending
**Area**: docs

### 摘要
编辑大型 Markdown 文档时，`fsWrite` 整体重写比 `strReplace` 大范围替换更安全可靠；`strReplace` 应仅用于小范围精确修改

### 详情
在优化 `docs/知识库检索迁移评估.md`（约 500 行）的格式时，尝试用 `strReplace` 替换从第三章到文件末尾的全部内容（约 300 行），结果出现旧内容残留 + 新内容追加的重复问题，导致文件"面目全非"。

根因分析：
1. `strReplace` 的 `oldStr` 需要精确匹配文件中的连续文本，当匹配范围过大时，可能因为空白字符、换行符等细微差异导致匹配不完整
2. 工具报告"成功"但实际替换不完整时，后续的 `fsAppend` 会在残留内容后追加，产生重复
3. 没有在 `strReplace` 后验证文件状态就继续操作，错过了发现问题的时机

正确做法：
- 需要修改文件 50%+ 内容时 → 用 `fsWrite` 重写整个文件（分批 write + append）
- 需要修改几行到几十行时 → 用 `strReplace`，确保 `oldStr` 足够短且唯一
- 任何大范围修改后 → 用 `readFile` 验证实际结果再继续

### 建议操作
建立工具选择规则：
- 修改范围 < 30% 文件内容 → `strReplace`
- 修改范围 >= 30% 文件内容 → `fsWrite` + `fsAppend` 重写
- 任何替换操作后如果要继续追加 → 先 `readFile` 验证

### 元数据
- Source: error
- Related Files: docs/知识库检索迁移评估.md
- Tags: strReplace, fsWrite, 文件编辑, 大文件, Markdown, 工具选择
- See Also: ERR-20260226-002

---

## [LRN-20260226-003] best_practice

**Logged**: 2026-02-26T16:00:00Z
**Priority**: high
**Status**: pending
**Area**: backend

### 摘要
跨项目 Service 层迁移时，权限模型的适配策略：用调用方传参替代 Service 内部查询用户信息，保持 Service 层无状态

### 详情
将 PaiSmart 的 `HybridSearchService` 迁移到 MeterSphere 的 `KnowledgeSearchService` 时，发现两个项目的用户信息获取方式完全不同：

1. **PaiSmart 的做法（反面教材）**：Service 层内部通过 `UserRepository` 和 `OrgTagCacheService` 查询用户信息。`getUserEffectiveOrgTags()` 和 `getUserDbId()` 两个私有方法各自独立查询数据库，且 `searchWithPermission()` 的异常处理中又重复调用这两个方法（降级路径），导致一次检索请求最多触发 4 次用户查询。

2. **MeterSphere 的做法（推荐）**：Controller 层通过 `SessionUtils.getUserId()` 和 `SessionUtils.getCurrentWorkspaceId()` 获取用户信息，作为参数传入 Service 方法。Service 层不依赖任何用户查询组件，保持无状态。

3. **关键适配点**：
   - PaiSmart 的 `orgTag`（组织标签，支持层级关系，一个用户可能有多个有效标签）→ MeterSphere 的 `workspaceId`（工作空间ID，单值）。这个简化是合理的，因为 MeterSphere 的工作空间是扁平结构，不存在层级关系。
   - PaiSmart 的 `userId` 可能是 Long 类型的数据库 ID 或 String 类型的用户名 → MeterSphere 的 `userId` 统一是 String 类型的 UUID。去掉了 `NumberFormatException` 的分支处理。
   - ES 查询中权限过滤的 `should` 子句从"多个 orgTag 的 bool should 组合"简化为"单个 workspaceId 的 term 查询"。

4. **这个模式的通用价值**：跨项目迁移 Service 层时，如果两个项目的认证/授权体系不同，最佳策略是：
   - 将用户信息获取逻辑上移到 Controller 层
   - Service 方法签名中显式声明需要的用户信息参数（userId、workspaceId 等）
   - Service 层不引入任何认证框架的依赖（不 import Shiro、不 import Spring Security）
   - 这样 Service 层可以在不同认证体系间复用，也更容易写单元测试

### 建议操作
后续迁移其他 PaiSmart Service 到 MeterSphere 时（如 Phase 2 的文件解析、向量化服务），遵循同样的模式：Controller 获取用户信息 → 参数传入 Service → Service 保持无状态。

### 元数据
- Source: conversation
- Related Files: metersphere/analytics-stat/backend/src/main/java/io/metersphere/knowledge/service/KnowledgeSearchService.java, metersphere/analytics-stat/backend/src/main/java/io/metersphere/knowledge/controller/KnowledgeSearchController.java, PaiSmart/src/main/java/com/yizhaoqi/smartpai/service/HybridSearchService.java
- Tags: 迁移, Service层, 权限模型, 无状态, SessionUtils, 跨项目
- See Also: LRN-20260226-001

---

## [LRN-20260226-004] best_practice

**Logged**: 2026-02-26T18:00:00Z
**Priority**: high
**Status**: pending
**Area**: backend

### 摘要
MeterSphere 所有模块的 MyBatis Mapper 扫描由 SDK 的 `CommonsDatabaseConfig` 统一配置，只扫描 `io.metersphere.base.mapper` 包，新增 Mapper 必须放在此包路径下

### 详情
在知识库模块（analytics-stat）启动时遇到 `KbFileUploadMapper` bean not found 错误，排查过程中发现以下项目特有的隐含约定：

1. **Mapper 扫描的唯一配置点**：`framework/sdk-parent/sdk/src/main/java/io/metersphere/autoconfigure/CommonsDatabaseConfig.java` 中的 `@MapperScan(basePackages = {"io.metersphere.base.mapper", "io.metersphere.xpack.mapper"})` 是全项目唯一的 Mapper 扫描配置。所有业务模块（test-track、api-test、report-stat 等）都没有自己的 `@MapperScan`，完全依赖 SDK 的这个统一配置。

2. **所有模块的 Mapper 都放在 `io.metersphere.base.mapper` 下**：这是项目约定，不是 MyBatis 的默认行为。test-track 有 40+ 个 Mapper 接口全部在 `io.metersphere.base.mapper/` 下，扩展 Mapper 在 `io.metersphere.base.mapper.ext/` 下。Mapper XML 文件和 Java 接口放在同一目录（不是 resources 下的独立目录）。

3. **错误做法**：将 Mapper 放在自定义子包下（如 `io.metersphere.knowledge.base.mapper`），虽然包名包含 `base.mapper`，但 `@MapperScan` 的 `basePackages` 不支持通配符匹配，`io.metersphere.base.mapper` 不会扫描到 `io.metersphere.knowledge.base.mapper`。

4. **正确做法**：新增模块的 Mapper 接口必须放在 `io.metersphere.base.mapper/` 包下。可以用文件名前缀区分模块（如 `KbFileUploadMapper`、`KbDocumentVectorMapper`）。其他类（Controller、Service、DTO、Config）可以放在任意 `io.metersphere.*` 子包下，因为 Spring 的 `@ComponentScan` 默认扫描启动类所在包及其所有子包。

5. **test-track 的标准包结构**（供新模块参考）：
   ```
   io.metersphere/
   ├── XxxApplication.java        # 启动类
   ├── base/
   │   ├── domain/                # 模块私有实体类
   │   └── mapper/                # MyBatis Mapper（接口 + XML 同目录）
   │       └── ext/               # 扩展 Mapper（自定义复杂 SQL）
   ├── config/                    # 配置类
   ├── constants/                 # 常量枚举
   ├── controller/                # REST 控制器
   ├── dto/                       # 数据传输对象
   ├── listener/                  # 事件监听器 / Kafka 消费者
   ├── request/                   # 请求参数对象
   ├── service/                   # 业务逻辑
   └── utils/                     # 工具类
   ```

### 建议操作
- 新增 MyBatis Mapper 时，始终放在 `io.metersphere.base.mapper` 包下
- 如果需要按功能域隔离，用文件名前缀（如 `Kb*Mapper`）而非子包
- 遇到 Mapper bean not found 错误时，首先检查 Mapper 接口的包路径是否在 `@MapperScan` 的 basePackages 范围内
- 不要在业务模块的 Application 类上添加额外的 `@MapperScan`，保持 SDK 统一管理

### 元数据
- Source: error
- Related Files: framework/sdk-parent/sdk/src/main/java/io/metersphere/autoconfigure/CommonsDatabaseConfig.java, metersphere/analytics-stat/backend/src/main/java/io/metersphere/knowledge/base/mapper/KbFileUploadMapper.java, metersphere/test-track/backend/src/main/java/io/metersphere/base/mapper/
- Tags: MyBatis, MapperScan, 包结构, SDK, 项目约定, analytics-stat, bean not found
- See Also: LRN-20260226-003

---

## [LRN-20260226-005] best_practice

**Logged**: 2026-02-26T20:00:00Z
**Priority**: medium
**Status**: pending
**Area**: config

### 摘要
MeterSphere 所有模块通过 `@PropertySource("file:/opt/metersphere/conf/metersphere.properties")` 加载外部配置，新增配置项可直接写入该文件而无需环境变量

### 详情
将 analytics-stat 知识库检索配置（ES、Embedding API、文件解析、Kafka Topic）从 `application.properties` 外移到 `/opt/metersphere/conf/metersphere.properties` 时，确认了以下项目约定：

1. **所有 11 个微服务都配置了同一个外部文件**：`@PropertySource("file:/opt/metersphere/conf/metersphere.properties", ignoreResourceNotFound = true)`。这意味着任何模块的自定义配置都可以集中到这一个文件中管理，不需要每个模块单独维护外部配置。

2. **属性覆盖优先级**：Spring 的 `@PropertySource` 加载顺序是声明顺序，后加载的覆盖先加载的。项目中的声明顺序是 `classpath:commons.properties` → `file:/opt/metersphere/conf/metersphere.properties`，所以外部文件的值会覆盖 classpath 中的同名 key。但 `application.properties` 的优先级高于 `@PropertySource`（Spring Boot 特性），所以如果 `application.properties` 中写了固定值（非 `${ENV:default}` 占位符），外部文件无法覆盖。

3. **外移配置时的关键操作**：必须从 `application.properties` 中删除对应的配置行（或改为注释），否则 `application.properties` 的优先级更高，外部文件的值不会生效。原来用 `${ENV_VAR:default}` 占位符的写法虽然也能被环境变量覆盖，但如果同时存在外部文件和环境变量，环境变量优先级更高（Spring Boot: 环境变量 > application.properties > @PropertySource）。

4. **该文件已有的配置项**：eureka、数据库、kafka、Redis、MinIO、CAS 等基础设施配置都在这个文件中。新增知识库配置时应加注释区分模块归属（如 `# analytics-stat 模块`），避免配置项混乱。

### 建议操作
后续为 analytics-stat 或其他模块新增需要运维调整的配置项时（特别是密码、API Key、外部服务地址），直接写入 `/opt/metersphere/conf/metersphere.properties`，同时从 `application.properties` 中移除对应行。不需要用环境变量中转。

### 元数据
- Source: conversation
- Related Files: metersphere/analytics-stat/backend/src/main/java/io/metersphere/AnalyticsStatApplication.java, metersphere/analytics-stat/backend/src/main/resources/application.properties, /opt/metersphere/conf/metersphere.properties
- Tags: PropertySource, 外部配置, 属性覆盖, metersphere.properties, 运维
- See Also: LRN-20260226-003

## [LRN-20260226-006] best_practice

**Logged**: 2026-02-26T20:10:00Z
**Priority**: medium
**Status**: pending
**Area**: config

### 摘要
外移配置到 metersphere.properties 时，必须交叉检查该文件中已有的同类配置项，确保值一致

### 详情
将知识库 ES 配置从 `application.properties` 外移到 `/opt/metersphere/conf/metersphere.properties` 时，`elasticsearch.scheme` 写成了 `https`（沿用了 application.properties 中的默认值），但同一文件中已有的 Spring Data ES 配置是 `spring.elasticsearch.uris=http://localhost:9200`（明文 http）。两组配置指向同一个 ES 实例，scheme 不一致导致启动时报 `Unrecognized SSL message, plaintext connection?`。

根因：外移时只关注了源文件（application.properties）中的默认值，没有检查目标文件（metersphere.properties）中已有的同类配置来推断实际环境。

### 建议操作
向 metersphere.properties 追加新配置时，先 grep 该文件中是否已有同一服务（ES、Redis、Kafka 等）的配置项，以已有配置的值为准，而非 application.properties 中的默认值。

### 元数据
- Source: error
- Related Files: /opt/metersphere/conf/metersphere.properties, metersphere/analytics-stat/backend/src/main/resources/application.properties
- Tags: 配置外移, elasticsearch, scheme, 交叉检查, metersphere.properties
- See Also: LRN-20260226-005

## [LRN-20260226-007] best_practice

**Logged**: 2026-02-26T20:30:00Z
**Priority**: medium
**Status**: pending
**Area**: infra

### 摘要
在 Docker 容器内的 ES 8.x 安装 IK 分词插件的完整步骤和注意事项

### 详情
知识库检索功能的 ES mapping 使用了 `ik_max_word`（索引分词器）和 `ik_smart`（搜索分词器），需要在 ES 中安装 analysis-ik 插件。

1. **IK 插件下载源**：官方 GitHub release 在国内可能较慢，使用 `https://get.infini.cloud/elasticsearch/analysis-ik/{version}` 镜像源更快。URL 格式固定，只需替换版本号，版本号必须与 ES 版本完全一致（如 8.10.4）。

2. **`--batch` 参数是必须的**：`elasticsearch-plugin install` 在安装需要额外权限的插件时会弹出交互式确认（`Continue with installation? [y/N]`）。在 `docker exec` 环境下没有 TTY，会报 `unable to read from standard input; is standard input open and a tty attached?` 并回滚安装。加 `--batch` 参数跳过确认。

3. **安装后必须重启 ES 容器**：`docker restart elasticsearch`。ES 8.x 重启到 healthy 状态需要约 2 分钟（在 macOS Docker Desktop 环境下），期间 curl 请求会返回 exit code 56（接收数据失败）。不要在 health: starting 阶段就认为启动失败。

4. **完整命令**：
   ```bash
   # 查看 ES 版本
   curl -s -u elastic:<password> http://localhost:9200/ | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['version']['number'])"
   
   # 安装 IK 插件（--batch 跳过交互确认）
   docker exec elasticsearch elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/8.10.4
   
   # 重启 ES
   docker restart elasticsearch
   
   # 等待 healthy（约 2 分钟）
   docker ps -a --filter name=elasticsearch --format '{{.Status}}'
   
   # 验证插件
   curl -s -u elastic:<password> http://localhost:9200/_cat/plugins
   ```

### 建议操作
后续如果需要在 Docker ES 中安装其他插件（如 analysis-pinyin），遵循同样的流程：`--batch` + 重启 + 等待 healthy。

### 元数据
- Source: conversation
- Related Files: metersphere/analytics-stat/backend/src/main/resources/es-mappings/knowledge_base.json
- Tags: Elasticsearch, IK分词, Docker, 插件安装, analysis-ik, infini.cloud
- See Also: LRN-20260226-006

## [LRN-20260226-008] best_practice

**Logged**: 2026-02-26T21:30:00Z
**Priority**: high
**Status**: pending
**Area**: backend

### 摘要
MeterSphere 微服务的认证机制：Gateway 用 X-AUTH-TOKEN header + Redis session，子服务直连无法通过认证，API 测试必须走 Gateway

### 详情
在测试 analytics-stat 知识库检索接口时，发现以下非显而易见的认证机制：

1. **MeterSphere 不使用 Cookie 认证**：登录接口 `POST /signin` 的响应不包含 `Set-Cookie` header，而是在响应头中返回 `X-AUTH-TOKEN: <sessionId>`，同时在响应 body 的 `data.sessionId` 和 `data.csrfToken` 中也返回。后续请求需要在 header 中携带 `X-AUTH-TOKEN` 和 `CSRF-TOKEN`。用 `curl -c cookie.txt` 保存 cookie 的方式无效。

2. **直连子服务端口无法通过认证**：每个子服务（如 analytics-stat:8009）使用 SDK 的 `ShiroConfig` 配置了 `ServletContainerSessionManager`，这是基于 Servlet 容器的本地 session 管理。而 Gateway 使用 Redis 存储 session（Spring Session），两者的 session 存储不共享。直连子服务时，即使携带了从 Gateway 获取的 `X-AUTH-TOKEN`，子服务的 Shiro 也无法识别（因为 session 不在本地 Servlet 容器中），返回 `302 + Authentication-Status: invalid`。

3. **正确的 API 测试路径是通过 Gateway**：`http://localhost:8000/<服务名>/<接口路径>`。Gateway 的 `SessionFilter` 会从 Redis 中查找 session，验证通过后将用户信息注入到转发请求的 header 中，子服务的 Shiro 通过这些 header 完成认证。

4. **Gateway 对 URL 中的非 ASCII 字符敏感**：直接在 URL 中放中文（如 `?query=测试平台`）会返回 400 Bad Request（空 body）。必须 URL encode（`?query=%E6%B5%8B%E8%AF%95%E5%B9%B3%E5%8F%B0`）。这是 Spring Cloud Gateway（基于 Netty）的默认行为，比 Tomcat 更严格。

5. **Gateway 500 + "Cannot deserialize" 错误**：URL encode 后请求能到达 Gateway 的 SessionFilter，但返回 `{"success":false,"message":"Cannot deserialize","data":null}`。这说明 Gateway 在反序列化 Redis 中的 session 时出错，可能是 session 中存储的对象类在 Gateway 的 classpath 中不存在（Gateway 是 WebFlux 应用，不包含业务模块的 DTO 类）。这个问题需要进一步排查。

### curl 测试 MeterSphere API 的正确姿势
```bash
# 1. 登录获取 token
TOKEN=$(curl -s -X POST 'http://localhost:8000/signin' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"metersphere"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['sessionId'])")

CSRF=$(curl -s -X POST 'http://localhost:8000/signin' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"metersphere"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['csrfToken'])")

# 2. 带 token 调接口（通过 Gateway，URL encode 中文参数）
curl -s -G \
  -H "X-AUTH-TOKEN: $TOKEN" \
  -H "CSRF-TOKEN: $CSRF" \
  --data-urlencode 'query=测试平台' \
  'http://localhost:8000/analytics/knowledge/search/hybrid?topK=5'

# 3. 不要直连子服务端口（会被 Shiro 拦截）
# ❌ curl http://localhost:8009/knowledge/search/hybrid  → 302
```

### 建议操作
- 测试 MeterSphere 任何模块的 API 时，始终通过 Gateway（8000 端口）
- 使用 `X-AUTH-TOKEN` header 而非 cookie
- 中文参数必须 URL encode
- 如果遇到 "Cannot deserialize" 错误，检查 Gateway 的 classpath 是否包含 session 中存储的对象类

### 元数据
- Source: conversation
- Related Files: framework/sdk-parent/sdk/src/main/java/io/metersphere/autoconfigure/ShiroConfig.java, framework/sdk-parent/sdk/src/main/java/io/metersphere/commons/utils/FilterChainUtils.java, framework/gateway/src/main/java/io/metersphere/gateway/filter/SessionFilter.java
- Tags: 认证, X-AUTH-TOKEN, Shiro, Gateway, session, Redis, curl, API测试
- See Also: LRN-20260225-005, LRN-20260226-005

## [LRN-20260226-009] best_practice

**Logged**: 2026-02-26T22:00:00Z
**Priority**: high
**Status**: pending
**Area**: frontend

### 摘要
Vue 3 独立子应用在 MeterSphere 微前端环境下必须手动注入认证 header，否则所有 API 请求都会被 Gateway 返回 401

### 详情
analytics-stat 前端从 Vue 2 迁移到 Vue 3 后，使用独立的 `axios.create()` 发送 API 请求。搜索接口 `GET /analytics/knowledge/search/hybrid` 始终返回 `401 UNAUTHORIZED "Not found session"`，但页面本身能正常加载（因为页面加载走的是主应用的请求链路）。

根因分析：
1. **Vue 2 模块的认证是 SDK 自动处理的**：所有 Vue 2 模块通过 `import { request } from 'metersphere-frontend/src/plugins/request'` 使用 SDK 提供的 axios 实例，该实例的请求拦截器会自动从 `localStorage["Admin-Token"]` 读取 `sessionId` 和 `csrfToken`，注入到 `X-AUTH-TOKEN` 和 `CSRF-TOKEN` header 中，同时从 `sessionStorage` 读取 `workspace_id` 和 `project_id` 注入到 `WORKSPACE` 和 `PROJECT` header 中。

2. **Vue 3 模块脱离了 SDK 体系**：Vue 3 无法引用 Vue 2 的 SDK 组件（参见 LRN-20260225-004），因此使用独立的 `axios.create()`。但最初只设置了 `baseURL: '/analytics'` 和 `withCredentials: true`，没有注入认证 header。`withCredentials: true` 只确保发送 cookie，而 MeterSphere 不使用 cookie 认证（参见 LRN-20260226-008），所以完全无效。

3. **对比正常请求和失败请求的 header 差异**是定位此问题的关键方法：通过 DevTools Network 面板对比 setting 模块（正常 200）和 analytics 模块（401）的请求 header，立即发现 analytics 的请求缺少 `x-auth-token`、`csrf-token`、`workspace`、`project` 四个 header。

4. **修复方案**：在独立 axios 实例上添加请求拦截器，复刻 SDK `request.js` 的 token 注入逻辑：
   ```typescript
   http.interceptors.request.use((config) => {
     const tokenStr = localStorage.getItem('Admin-Token')
     if (tokenStr) {
       const user = JSON.parse(tokenStr)
       if (user?.sessionId) config.headers['X-AUTH-TOKEN'] = user.sessionId
       if (user?.csrfToken) config.headers['CSRF-TOKEN'] = user.csrfToken
     }
     const workspaceId = sessionStorage.getItem('workspace_id')
     if (workspaceId) config.headers['WORKSPACE'] = workspaceId
     const projectId = sessionStorage.getItem('project_id')
     if (projectId) config.headers['PROJECT'] = projectId
     return config
   })
   ```

5. **关键常量值**（与 SDK 保持一致）：
   - `localStorage` key: `"Admin-Token"`（SDK 的 `TokenKey`）
   - `sessionStorage` keys: `"workspace_id"`（SDK 的 `WORKSPACE_ID`）、`"project_id"`（SDK 的 `PROJECT_ID`）
   - Header names: `X-AUTH-TOKEN`、`CSRF-TOKEN`、`WORKSPACE`、`PROJECT`

### 建议操作
后续在 MeterSphere 中新增任何 Vue 3 子应用（或其他脱离 SDK 的前端模块），创建 axios 实例时必须同步添加认证 header 注入拦截器。可以将此拦截器抽取为独立的 `auth-interceptor.ts` 工具文件，供多个 API 模块复用。

### 元数据
- Source: conversation
- Related Files: metersphere/analytics-stat/frontend/src/api/knowledge.ts, metersphere/framework/sdk-parent/frontend/src/plugins/request.js, metersphere/framework/sdk-parent/frontend/src/utils/constants.js
- Tags: 认证, X-AUTH-TOKEN, axios, 拦截器, Vue3, 微前端, Gateway, 401, analytics-stat
- See Also: LRN-20260226-008, LRN-20260225-004

---

## [LRN-20260226-010] gotcha

**Logged**: 2026-02-26T23:00:00Z
**Priority**: high
**Status**: pending
**Area**: backend

### 摘要
Lombok `@Data` + Jackson 对 `boolean isXxx` 字段的命名陷阱：getter `isXxx()` 被 Jackson 映射为 JSON 属性 `xxx`（去掉 `is` 前缀），与 ES/数据库中的 `isXxx` 字段名不匹配

### 详情
知识库检索接口 `GET /analytics/knowledge/search/hybrid` 返回 200 但 Jackson 反序列化 ES 响应时抛出 `UnrecognizedPropertyException: Unrecognized field "isPublic"`。

根因链路：
1. `EsDocument.java` 定义了 `private boolean isPublic`
2. Lombok `@Data` 生成 getter `isPublic()` 和 setter `setPublic(boolean)`
3. Jackson 根据 JavaBean 规范推断 JSON 属性名：对于 `isXxx()` getter，属性名是 `xxx`（去掉 `is` 前缀）→ 映射为 `"public"`
4. ES 中存储的字段名是 `"isPublic"`（PaiSmart 原始字段名）
5. Jackson 在 ES 响应中看到 `"isPublic"` 字段，但只认识 `"public"`，抛出 `UnrecognizedPropertyException`
6. 错误信息中的 `9 known properties` 列表里确实只有 `"public"` 而没有 `"isPublic"`

修复方案：
- 在 `isPublic` 字段上添加 `@JsonProperty("isPublic")`，显式告诉 Jackson 这个字段对应 JSON 中的 `"isPublic"`
- 在类上添加 `@JsonIgnoreProperties(ignoreUnknown = true)`，防止 ES 返回的其他未知字段导致反序列化失败

这个问题的非显而易见之处：
- `boolean isPublic` 在 Java 中看起来很自然，但 Lombok + Jackson 的组合会产生意外的属性名映射
- 如果字段类型是 `Boolean`（包装类型）而非 `boolean`（基本类型），Lombok 生成的 getter 是 `getIsPublic()`，Jackson 映射为 `"isPublic"`，反而不会有这个问题
- PaiSmart 原项目使用 JPA + Spring Data，JPA 不依赖 JavaBean getter 命名规范做 JSON 序列化，所以同样的字段定义在 PaiSmart 中没有问题

### 通用规则
在使用 Lombok `@Data` + Jackson 的项目中，避免用 `boolean isXxx` 命名字段。推荐做法：
1. 改用 `Boolean isXxx`（包装类型，getter 为 `getIsXxx()`）
2. 或加 `@JsonProperty("isXxx")` 显式指定
3. 或改名为 `boolean xxxFlag` / `boolean publicFlag` 避免 `is` 前缀

### 元数据
- Source: error
- Related Files: metersphere/analytics-stat/backend/src/main/java/io/metersphere/knowledge/dto/EsDocument.java, PaiSmart/src/main/java/com/yizhaoqi/smartpai/entity/EsDocument.java
- Tags: Lombok, Jackson, boolean, isPublic, JsonProperty, 反序列化, ES, 命名陷阱
- See Also: LRN-20260226-009

---

## [LRN-20260226-011] gotcha

**Logged**: 2026-02-26T16:45:00Z
**Priority**: medium
**Status**: pending
**Area**: infra

### 摘要
steering 文档中的 MySQL 连接命令 `mysql -h localhost` 在本地开发环境不可用，MySQL 运行在 Docker 容器中，必须用 `docker exec mysql mysql` 连接

### 详情
steering 的 `chinese-rules.md` 中数据库连接信息写的是：
```bash
mysql -h localhost -u root -p'Password123@mysql' -e "USE metersphere_dev;"
```

但实际环境中 MySQL 运行在名为 `mysql` 的 Docker 容器内，宿主机没有安装 mysql client，也没有暴露 3306 端口到 localhost（或者暴露了但没有本地 mysql 命令）。正确的连接方式是：
```bash
docker exec mysql mysql -u root -p'Password123@mysql' -e "USE metersphere_dev;"
```

如果需要执行 SQL 文件：
```bash
docker exec -i mysql mysql -u root -p'Password123@mysql' metersphere_dev < path/to/script.sql
```

注意 `-i` 参数是必须的（允许 stdin 重定向），不加 `-i` 会报 `the input device is not a TTY`。

### 建议操作
后续所有数据库操作都用 `docker exec mysql mysql` 而非 `mysql -h localhost`。如果 steering 文档有更新机会，应修正连接命令。

### 元数据
- Source: error
- Related Files: .kiro/steering/chinese-rules.md
- Tags: MySQL, Docker, 数据库连接, steering文档, 环境差异
- See Also: LRN-20260226-005

---

## [LRN-20260226-012] gotcha

**Logged**: 2026-02-26T16:50:00Z
**Priority**: medium
**Status**: pending
**Area**: backend

### 摘要
analytics-stat 模块的 Flyway 迁移脚本不会自动执行，`db/migration/` 下的 SQL 文件需要手动在 MySQL 中执行

### 详情
analytics-stat 的 `backend/src/main/resources/db/migration/V2__add_knowledge_base_tables.sql` 包含了 `kb_file_upload`、`kb_document_vectors`、`kb_chunk_info` 三张表的建表语句，但启动 analytics-stat 后端时这些表并没有被自动创建。

可能的原因：
1. analytics-stat 模块可能没有配置 Flyway 自动迁移（`spring.flyway.enabled=false` 或未引入 flyway 依赖）
2. MeterSphere 其他模块（如 system-setting）可能有 Flyway 配置，但 analytics-stat 作为新增模块可能遗漏了
3. 迁移脚本放在 `db/migration/` 目录下只是作为文档/参考，实际建表需要手动执行

手动建表命令：
```bash
docker exec -i mysql mysql -u root -p'Password123@mysql' metersphere_dev < analytics-stat/backend/src/main/resources/db/migration/V2__add_knowledge_base_tables.sql
```

### 建议操作
- 新增数据库表时，不要依赖 Flyway 自动迁移，手动执行 SQL 脚本
- 或者检查 analytics-stat 的 pom.xml 和 application.properties 是否需要添加 Flyway 配置
- 迁移脚本仍然保留在 `db/migration/` 目录下作为版本化的 DDL 记录

### 元数据
- Source: error
- Related Files: metersphere/analytics-stat/backend/src/main/resources/db/migration/V2__add_knowledge_base_tables.sql, metersphere/analytics-stat/backend/src/main/resources/application.properties
- Tags: Flyway, 数据库迁移, 建表, analytics-stat, 手动执行
- See Also: LRN-20260226-004

---

## [LRN-20260226-013] gotcha

**Logged**: 2026-02-26T17:00:00Z
**Priority**: high
**Status**: active
**Area**: backend

### 摘要
MeterSphere 每个模块必须使用独立的 `spring.flyway.table` 名，共用版本表会导致迁移脚本被静默跳过

### 详情
analytics-stat 的 Flyway 迁移脚本（V1、V2）没有自动执行，最初误判为"Flyway 未配置"或"不会自动执行"（LRN-20260226-012 的结论是错误的）。

实际根因：`application.properties` 中 `spring.flyway.table=metersphere_version`，与 system-setting 模块共用同一张版本记录表。该表已有 149 条迁移记录（V1 到 V149），Flyway 启动时发现 V1、V2 的版本号低于已有最高版本 149，判定为"已执行过"，静默跳过。

这个问题的非显而易见之处：
- Flyway 不会报错或警告，只是在 DEBUG 日志中记录 "Resolved migration V1 was ignored because it is below the baseline"
- `spring.flyway.baseline-on-migrate=true` + `baseline-version=0` 的组合意味着 Flyway 会自动创建 baseline，但如果版本表已存在且有更高版本的记录，baseline 不会重新创建
- 从 system-setting 复制 application.properties 模板时，很容易忘记改 `flyway.table` 的值

项目中各模块的 Flyway 版本表命名约定：

| 模块 | flyway.table |
|------|-------------|
| system-setting | `metersphere_version` |
| api-test | `api_version` |
| test-track | `track_version` |
| performance-test | `performance_version` |
| project-management | `project_management_version` |
| report-stat | `report_version`（推测） |
| analytics-stat | `analytics_version`（已修复） |

### 建议操作
- 新增模块时，`spring.flyway.table` 必须设置为模块独有的表名（如 `<模块名>_version`）
- 如果从其他模块复制 application.properties 模板，第一时间检查 `flyway.table` 是否改了
- 遇到 Flyway 迁移脚本"没执行"的问题，先查版本表内容（`SELECT * FROM <flyway.table>`），而不是假设 Flyway 未配置
- LRN-20260226-012 的结论"Flyway 不会自动执行"是错误的，已被本条纠正

### 元数据
- Source: error
- Related Files: metersphere/analytics-stat/backend/src/main/resources/application.properties
- Tags: Flyway, 版本表, 迁移脚本, 静默跳过, analytics-stat, 项目约定
- Supersedes: LRN-20260226-012
- See Also: LRN-20260226-004

---

## [LRN-20260226-014] gotcha

**Logged**: 2026-02-26T17:30:00Z
**Priority**: high
**Status**: active
**Area**: backend

### 摘要
MeterSphere SDK 的 `ResultResponseBodyAdvice` 会自动将所有 `io.metersphere` 包下 Controller 的返回值包装为 `ResultHolder { success, message, data }`，新增 Controller 不要手动构造响应格式

### 详情
知识库检索接口后端返回 1 条结果且无报错，但前端弹窗"检索失败"。排查发现是响应格式被二次包装导致前端解析失败。

根因链路：
1. `ResultResponseBodyAdvice`（`@RestControllerAdvice(value = {"io.metersphere"})`）拦截所有 `io.metersphere` 包下 Controller 的返回值
2. 如果返回值不是 `ResultHolder` 类型，自动调用 `ResultHolder.success(o)` 包装
3. 我们的 Controller 返回 `Map<String, Object> { code: 200, message: "success", data: [...] }`
4. 被包装后变成 `{ success: true, message: null, data: { code: 200, message: "success", data: [...] } }`
5. 前端检查 `res.data.code === 200`，但 `res.data` 是外层 `ResultHolder`，`res.data.code` 是 `undefined`
6. 走到 `throw new Error('检索失败')`

这个问题的非显而易见之处：
- 后端日志完全正常（"检索完成，返回 1 条结果"），HTTP 状态码 200，没有任何错误
- 前端 Network 面板看到的响应也是 200，body 里有数据，但嵌套了两层
- 从 PaiSmart 迁移 Controller 时，PaiSmart 没有全局响应包装，所以手动构造 `{ code, message, data }` 是正确的；但 MeterSphere 有 `ResultResponseBodyAdvice`，手动构造反而导致二次包装
- 如果 Controller 返回的就是 `ResultHolder`，`ResultResponseBodyAdvice` 会直接透传不包装（`if (!(o instanceof ResultHolder))` 判断）

正确做法：
- Controller 直接返回业务数据（如 `List<KnowledgeSearchResult>`），让 SDK 自动包装
- 前端统一用 `res.data.success` 判断成功，`res.data.data` 获取业务数据
- 如果需要跳过自动包装（如返回文件流），在方法上加 `@NoResultHolder` 注解

MeterSphere 标准响应格式（`ResultHolder`）：
```json
{
  "success": true,       // boolean，不是 code: 200
  "message": null,       // 错误时有值
  "data": [...]          // 业务数据
}
```

### 建议操作
- 从其他项目迁移 Controller 到 MeterSphere 时，去掉手动构造的响应格式（Map/自定义 Response 类），直接返回业务对象
- 前端 API 客户端统一用 `success` 字段判断，不要用 `code`
- 遇到"后端正常但前端报错"的情况，先在 DevTools Network 面板检查实际响应 JSON 结构

### 元数据
- Source: error
- Related Files: framework/sdk-parent/sdk/src/main/java/io/metersphere/controller/handler/ResultResponseBodyAdvice.java, framework/sdk-parent/sdk/src/main/java/io/metersphere/controller/handler/ResultHolder.java, metersphere/analytics-stat/backend/src/main/java/io/metersphere/knowledge/controller/KnowledgeSearchController.java, metersphere/analytics-stat/frontend/src/api/knowledge.ts
- Tags: ResultHolder, ResultResponseBodyAdvice, 响应包装, 二次包装, Controller, 迁移陷阱, analytics-stat
- See Also: LRN-20260226-003, LRN-20260226-009

---


## [LRN-20260226-015] best_practice

**Logged**: 2026-02-26T23:30:00Z
**Priority**: high
**Status**: pending
**Area**: backend

### 摘要
Lombok `@Data` 的 `boolean isXxx` Jackson 命名陷阱不仅影响 ES 反序列化，还会影响 Kafka JSON 序列化——项目中所有使用 `boolean isPublic` 的 DTO 都需要统一修复

### 详情
在评估第二期代码时发现，LRN-20260226-010 记录的 `boolean isPublic` + Lombok + Jackson 命名陷阱只在 `EsDocument.java` 上修复了（加了 `@JsonProperty("isPublic")`），但同样的字段定义还存在于以下 3 个 DTO 中：

1. `KbFileUpload.java` — `private boolean isPublic`（数据库实体，参与 API 响应序列化）
2. `KbDocumentVector.java` — `private boolean isPublic`（数据库实体）
3. `FileProcessingTask.java` — `private boolean isPublic`（Kafka 消息体，参与 JSON 序列化/反序列化）

其中 `FileProcessingTask` 的风险最高：它通过 `KafkaConfig` 配置的 `JsonSerializer` / `JsonDeserializer` 进行 Kafka 消息传递。Producer 端 Jackson 序列化时 `isPublic` 会被映射为 `"public"`，Consumer 端反序列化时期望字段名 `"public"` 也能正常工作（因为 getter/setter 一致），所以 Kafka 内部传递不会报错。但如果有外部系统消费这个 Topic，或者用 Kafka 控制台查看消息内容，会看到字段名是 `"public"` 而非 `"isPublic"`，造成混淆。

`KbFileUpload` 的风险更直接：它作为 `KnowledgeFileController.listFiles()` 的返回值，经 `ResultResponseBodyAdvice` 包装后返回给前端。前端 `FileList.vue` 中用 `row.isPublic` 访问，但 Jackson 序列化后 JSON 中的字段名是 `"public"`，导致前端拿到的值始终是 `undefined`，可见性标签永远显示"私有"。

### 建议操作
在项目中建立规则：所有 Lombok `@Data` 类中，`boolean` 类型字段禁止使用 `is` 前缀。统一使用以下方案之一：
1. 改为 `Boolean isPublic`（包装类型，getter 为 `getIsPublic()`，Jackson 映射为 `"isPublic"`）
2. 改为 `boolean publicFlag`（避免 `is` 前缀）
3. 加 `@JsonProperty("isPublic")` 显式指定

当前需要修复的文件：`KbFileUpload.java`、`KbDocumentVector.java`、`FileProcessingTask.java`

### 元数据
- Source: conversation
- Related Files: metersphere/analytics-stat/backend/src/main/java/io/metersphere/knowledge/dto/KbFileUpload.java, metersphere/analytics-stat/backend/src/main/java/io/metersphere/knowledge/dto/KbDocumentVector.java, metersphere/analytics-stat/backend/src/main/java/io/metersphere/knowledge/dto/FileProcessingTask.java
- Tags: Lombok, Jackson, boolean, isPublic, Kafka, 序列化, 前端字段映射
- See Also: LRN-20260226-010

---

## [LRN-20260226-016] best_practice

**Logged**: 2026-02-26T23:35:00Z
**Priority**: medium
**Status**: pending
**Area**: backend

### 摘要
跨项目迁移代码审查时，"死代码"（已定义但未被引用的 Mapper/表/DTO）是常见遗留问题，应在审查清单中专门检查

### 详情
第二期代码中 `KbChunkInfoMapper`（接口 + XML）和 `kb_chunk_info` 表已经完整定义，但在整个后端代码中没有任何 Service 引用它。`KnowledgeParseService` 直接将分块写入 `kb_document_vectors` 表，`KnowledgeFileConsumer` 的处理链路也不涉及 `kb_chunk_info`。

这个问题的产生原因：迁移评估文档（第五章）列出了 3 个需要新建的 Mapper（`KbFileUploadMapper`、`KbDocumentVectorMapper`、`KbChunkInfoMapper`），开发时按清单创建了所有 Mapper 和表，但实际编码时发现 `kb_chunk_info` 的功能被 `kb_document_vectors` 覆盖了（PaiSmart 原始设计中两张表有不同用途，但迁移简化后合并了），没有回头清理。

这类"死代码"的危害：
1. 增加维护成本——后续开发者会困惑这张表的用途
2. Flyway 迁移脚本中包含了建表语句，占用数据库空间
3. 如果未来有人误以为应该使用这个 Mapper，可能引入 Bug

### 建议操作
迁移代码审查清单中增加一项："检查所有新建的 Mapper/DTO/表是否被至少一个 Service 引用"。可以用 IDE 的 Find Usages 或 grep 快速验证。

### 元数据
- Source: conversation
- Related Files: metersphere/analytics-stat/backend/src/main/java/io/metersphere/base/mapper/KbChunkInfoMapper.java, metersphere/analytics-stat/backend/src/main/java/io/metersphere/base/mapper/KbChunkInfoMapper.xml
- Tags: 死代码, 迁移审查, Mapper, 代码清理
- See Also: LRN-20260226-001

---

## [LRN-20260226-017] best_practice

**Logged**: 2026-02-26T23:40:00Z
**Priority**: medium
**Status**: pending
**Area**: backend

### 摘要
文件上传接口中 `MultipartFile.getBytes()` 计算 MD5 会将整个文件读入堆内存，大文件场景下应改用流式 `DigestInputStream`

### 详情
`KnowledgeFileService.uploadFile()` 中的 MD5 计算方式：
```java
String fileMd5 = calculateMD5(file.getBytes());
```

`file.getBytes()` 会将整个文件内容读入 `byte[]`。对于知识库场景，用户可能上传 50MB 的 PDF 或 Word 文档，这意味着瞬间分配 50MB+ 的堆内存。如果多个用户同时上传，内存压力会叠加。

更好的做法是流式计算：
```java
private String calculateMD5(InputStream inputStream) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");
    try (DigestInputStream dis = new DigestInputStream(inputStream, md)) {
        byte[] buffer = new byte[8192];
        while (dis.read(buffer) != -1) { /* 消费流 */ }
    }
    byte[] digest = md.digest();
    // ... 转 hex
}
```

但注意：流式计算 MD5 后，`InputStream` 已经被消费完毕，后续上传到 MinIO 需要重新获取流（`file.getInputStream()` 可以多次调用，Spring 的 `MultipartFile` 会缓存到临时文件）。所以需要调用两次 `file.getInputStream()`：一次算 MD5，一次传 MinIO。

### 建议操作
将 `calculateMD5(file.getBytes())` 改为 `calculateMD5(file.getInputStream())`，并确保 MinIO 上传使用独立的 `file.getInputStream()` 调用。

### 元数据
- Source: conversation
- Related Files: metersphere/analytics-stat/backend/src/main/java/io/metersphere/knowledge/service/KnowledgeFileService.java
- Tags: 内存优化, 文件上传, MD5, DigestInputStream, MultipartFile
- See Also: LRN-20260226-015

---
