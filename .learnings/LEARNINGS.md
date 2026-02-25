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
