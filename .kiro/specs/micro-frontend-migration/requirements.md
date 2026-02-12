# 需求文档：微前端框架迁移（qiankun → micro-app）

## 引言

MeterSphere 当前使用 qiankun 2.9.3 作为微前端框架，将 8 个业务模块（api-test、test-track、performance-test、project-management、system-setting、report-stat、workstation、analytics-stat）作为子应用加载到主应用（framework/sdk-parent/frontend）中。

qiankun 基于 JS 沙箱 + HTML Entry 方案，要求子应用以 UMD 格式打包输出。这一限制导致子应用无法使用 Vite 构建（Vite 原生输出 ES Module），阻碍了 Vue 2 → Vue 3 + Vite 的渐进式升级路径。

### 当前 qiankun 使用现状

**主应用侧（framework/sdk-parent/frontend）**：
- `micro-app.js`：通过 `GET /services` 从网关动态获取服务列表，调用 `registerMicroApps()` + `start()` 注册并启动所有子应用
- `MicroApp.vue`：封装 `loadMicroApp()` 实现按需加载，用于跨模块嵌入场景（如测试计划中嵌入 API 报告）
- `App.vue`：包含 `<div id="micro-app">` 作为子应用统一挂载容器
- 路由激活规则：基于 hash 前缀匹配（`location.hash.startsWith('#/' + name)`）

**子应用侧（8 个业务模块）**：
- 每个模块的 `main.js` 导出 qiankun 生命周期函数：`bootstrap`、`mount`、`unmount`、`update`
- 每个模块的 `public-path.js` 处理 `__POWERED_BY_QIANKUN__` 和 `__INJECTED_PUBLIC_PATH_BY_QIANKUN__`
- 每个模块的 `vue.config.js` 配置 `libraryTarget: 'umd'` 和独立的 `chunkLoadingGlobal`

**跨应用通信**：
- Vue EventBus：主应用创建 `eventBus = new Vue()`，通过 props 传递给子应用，子应用挂载到 `Vue.prototype.$EventBus`
- qiankun globalState：`initGlobalState` / `onGlobalStateChange` / `setGlobalState`（当前主要为能力预留，实际业务使用较少）

**按需加载场景（MicroApp.vue）**：
- test-track 模块中大量使用：嵌入 API 场景报告、API 用例结果、性能测试报告、UI 场景报告
- TaskCenter 组件中使用：动态加载不同模块的报告视图
- 共约 10+ 处跨模块嵌入使用

### 迁移动机

1. qiankun 强制要求 UMD 打包，不支持 Vite 原生 ES Module 输出
2. qiankun 官方已停止活跃维护（最后一个版本 2.10.16 发布于 2023 年）
3. MeterSphere 3.6 新版本已移除 qiankun，验证了去 qiankun 的可行性
4. 需要为 Vue 2 → Vue 3 + Vite 的渐进式升级铺平道路

### 微前端框架对比与推荐

| 特性 | qiankun 2.x | micro-app（京东） | wujie（腾讯） | 无框架（路由分发） |
|------|-------------|------------------|--------------|-------------------|
| Vue 3 + Vite 支持 | ❌ 需要额外插件 | ✅ 原生支持 | ✅ 原生支持 | ✅ 不限制 |
| 接入成本 | 高（UMD + 生命周期） | 低（WebComponent 风格） | 中（iframe + WebComponent） | 低（无框架依赖） |
| JS 沙箱 | ✅ Proxy 沙箱 | ✅ Proxy 沙箱 | ✅ iframe 天然隔离 | ❌ 无隔离 |
| CSS 隔离 | ✅ Shadow DOM / Scoped | ✅ Scoped CSS | ✅ iframe 天然隔离 | ❌ 需手动处理 |
| 子应用改造量 | 大（UMD + 生命周期导出） | 小（几乎零改造） | 中（需适配通信） | 大（需自行实现） |
| 按需加载（嵌入式） | ✅ loadMicroApp | ✅ `<micro-app>` 标签 | ✅ `<wujie-vue>` 组件 | ❌ 需自行实现 |
| Vue 2 兼容 | ✅ | ✅ | ✅ | ✅ |
| 社区活跃度 | 低（停止维护） | 高（京东持续维护） | 中 | N/A |
| 渐进式迁移 | N/A | ✅ 可逐模块迁移 | ✅ 可逐模块迁移 | ✅ |

**推荐方案：micro-app（京东）**

理由：
1. WebComponent 风格的 `<micro-app>` 标签，接入成本最低，子应用几乎零改造
2. 原生支持 Vite + ES Module，无需 UMD 打包
3. 支持 Vue 2 和 Vue 3 混合运行，适合渐进式升级
4. 提供完善的 JS 沙箱和 CSS 隔离
5. 京东大规模生产验证，社区活跃
6. API 设计与 qiankun 的 `loadMicroApp` 模式相似，迁移路径清晰

## 术语表

- **主应用（Main_App）**：framework/sdk-parent/frontend，负责布局、路由、子应用注册和加载
- **子应用（Sub_App）**：各业务模块的 frontend 目录，被主应用动态加载
- **micro-app**：京东开源的微前端框架，基于 WebComponent 实现
- **按需加载（On_Demand_Loading）**：通过组件方式在页面中嵌入其他模块的视图，而非全局路由激活
- **生命周期钩子（Lifecycle_Hooks）**：子应用的 bootstrap、mount、unmount、update 函数
- **EventBus**：基于 Vue 实例的事件总线，用于跨模块通信
- **UMD**：Universal Module Definition，一种兼容多种模块系统的打包格式
- **ES Module**：JavaScript 原生模块系统，Vite 默认输出格式

## 需求

### 需求 1：主应用微前端框架替换

**用户故事：** 作为前端开发者，我希望将主应用的微前端框架从 qiankun 替换为 micro-app，以便支持 Vite 构建的子应用接入。

#### 验收标准

1. WHEN 主应用启动时，THE Main_App SHALL 使用 micro-app 替代 qiankun 注册和加载所有子应用
2. WHEN 主应用从网关获取服务列表后，THE Main_App SHALL 通过 `<micro-app>` 标签动态创建子应用容器
3. WHEN 用户点击导航菜单切换模块时，THE Main_App SHALL 通过 hash 路由激活对应的 micro-app 子应用实例
4. WHEN 主应用完成框架替换后，THE Main_App SHALL 移除 qiankun 依赖（`qiankun` npm 包及 `micro-app.js` 注册逻辑）
5. WHEN micro-app 初始化时，THE Main_App SHALL 配置 JS 沙箱和 CSS 隔离以保持与 qiankun 同等的隔离能力

### 需求 2：子应用适配改造

**用户故事：** 作为前端开发者，我希望各子应用能以最小改动适配 micro-app 框架，以便降低迁移风险和工作量。

#### 验收标准

1. WHEN 子应用被 micro-app 加载时，THE Sub_App SHALL 正确渲染页面内容到指定容器中
2. WHEN 子应用从 qiankun 模式迁移到 micro-app 模式后，THE Sub_App SHALL 移除 `public-path.js` 中的 `__POWERED_BY_QIANKUN__` 判断逻辑
3. WHEN 子应用的 `main.js` 完成适配后，THE Sub_App SHALL 将 qiankun 生命周期导出替换为 micro-app 的生命周期监听方式
4. WHEN 子应用的 `vue.config.js` 完成适配后，THE Sub_App SHALL 移除 `libraryTarget: 'umd'` 配置（micro-app 不要求 UMD 格式）
5. WHEN 子应用独立运行时（开发模式），THE Sub_App SHALL 保持与迁移前相同的独立运行能力
6. WHEN 所有 8 个子应用完成适配后，THE Sub_App SHALL 在 micro-app 环境下正常加载和运行

### 需求 3：按需加载组件迁移

**用户故事：** 作为前端开发者，我希望将 MicroApp.vue 按需加载组件迁移到 micro-app 方案，以便跨模块嵌入场景继续正常工作。

#### 验收标准

1. WHEN test-track 模块需要嵌入 API 报告视图时，THE Main_App SHALL 通过 micro-app 的组件方式加载 api-test 子应用的指定页面
2. WHEN TaskCenter 组件需要展示不同模块的报告时，THE Main_App SHALL 通过 micro-app 动态切换加载目标子应用和路由
3. WHEN 按需加载的子应用接收到路由参数更新时，THE Sub_App SHALL 响应参数变化并更新视图内容
4. WHEN 按需加载的子应用被销毁时，THE Main_App SHALL 正确卸载子应用实例并释放资源
5. WHEN 迁移完成后，THE Main_App SHALL 移除旧的 `MicroApp.vue` 组件及其对 qiankun `loadMicroApp` 的依赖

### 需求 4：跨应用通信机制迁移

**用户故事：** 作为前端开发者，我希望跨应用通信机制平滑迁移到 micro-app 方案，以便模块间的事件传递和状态共享继续正常工作。

#### 验收标准

1. WHEN 主应用需要向子应用传递事件时，THE Main_App SHALL 通过 micro-app 的数据通信机制（`data` 属性或 `dispatch`）替代 qiankun 的 EventBus props 传递
2. WHEN 子应用需要向主应用发送事件时，THE Sub_App SHALL 通过 micro-app 的 `dispatch` 机制替代 `$EventBus.$emit`
3. WHEN 项目切换或工作空间切换事件发生时，THE Main_App SHALL 将事件正确广播到所有已加载的子应用
4. WHEN 迁移完成后，THE Main_App SHALL 移除 qiankun 的 `initGlobalState` / `onGlobalStateChange` / `setGlobalState` 相关代码

### 需求 5：构建配置和部署适配

**用户故事：** 作为 DevOps 工程师，我希望构建和部署流程适配 micro-app 方案，以便 CI/CD 流水线和生产环境正常运行。

#### 验收标准

1. WHEN 子应用通过 Vue CLI 构建后，THE Sub_App SHALL 输出 micro-app 兼容的构建产物（无需 UMD 格式）
2. WHEN 生产环境部署时，THE Main_App SHALL 通过网关反向代理正确访问各子应用的静态资源（路径规则：`/{serviceId}/`）
3. WHEN 开发环境启动时，THE Sub_App SHALL 配置正确的 CORS 头（`Access-Control-Allow-Origin: *`）以支持跨域加载
4. WHEN 子应用的静态资源路径需要动态调整时，THE Sub_App SHALL 通过 micro-app 提供的 publicPath 机制正确设置资源基础路径
5. IF 构建产物格式变更导致 Nginx/Gateway 路由规则需要调整，THEN THE Main_App SHALL 更新对应的反向代理配置

### 需求 6：渐进式迁移支持

**用户故事：** 作为技术负责人，我希望迁移过程支持渐进式推进，以便降低一次性全量迁移的风险。

#### 验收标准

1. WHILE 迁移过程中存在未迁移的子应用时，THE Main_App SHALL 同时支持 qiankun 和 micro-app 两种加载方式
2. WHEN 单个子应用完成迁移后，THE Main_App SHALL 能够独立验证该子应用在 micro-app 下的功能完整性
3. WHEN 所有子应用迁移完成后，THE Main_App SHALL 完全移除 qiankun 相关代码和依赖
4. IF 某个子应用迁移后出现严重问题，THEN THE Main_App SHALL 能够将该子应用回退到 qiankun 加载方式

### 需求 7：功能回归验证

**用户故事：** 作为测试工程师，我希望迁移后所有现有功能保持正常，以便确保迁移不引入回归缺陷。

#### 验收标准

1. WHEN 迁移完成后，THE Main_App SHALL 保持与迁移前相同的模块导航和页面加载行为
2. WHEN 迁移完成后，THE Sub_App SHALL 保持与迁移前相同的业务功能和交互体验
3. WHEN 跨模块嵌入场景（如测试计划中查看 API 报告）执行时，THE Main_App SHALL 正确加载和展示嵌入内容
4. WHEN 用户在不同模块间快速切换时，THE Main_App SHALL 正确卸载旧模块并加载新模块，无内存泄漏
5. WHEN 浏览器刷新页面后，THE Main_App SHALL 正确恢复到刷新前的模块和路由状态

### 需求 8：Vue 3 + Vite 升级路径支持

**用户故事：** 作为前端架构师，我希望迁移后的微前端框架能支持 Vue 3 + Vite 子应用接入，以便后续渐进式技术栈升级。

#### 验收标准

1. WHEN 新模块使用 Vue 3 + Vite 构建时，THE Main_App SHALL 能够通过 micro-app 正确加载该模块
2. WHEN Vue 2 子应用和 Vue 3 子应用同时运行时，THE Main_App SHALL 保持两者的隔离性，互不干扰
3. WHEN Vue 3 + Vite 子应用输出 ES Module 格式时，THE Main_App SHALL 能够正确解析和加载该格式的构建产物
