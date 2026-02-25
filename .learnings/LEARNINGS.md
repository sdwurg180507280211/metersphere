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
