# 需求文档：测试跟踪模块嵌入 Grist Core 多维表

## 简介

为 MeterSphere V2.10 测试跟踪模块引入在线多维表能力，用于辅助测试用例编写、评审准备和批量结构化整理。方案以 **Grist Core / grist-oss 离线私有化部署** 为基础，通过 Docker 容器运行在瑞众保险测试内网，与现有 MySQL、Kafka、Redis、MinIO 等容器化中间件保持一致。

本阶段目标是先完成本地 POC 规格设计，不直接替换 MeterSphere 原生用例库。Grist 作为辅助编写区，MeterSphere 仍作为正式用例、评审、执行和报表的主数据系统。

## 背景约束

1. 瑞众保险内网环境无法直接访问外网，所有镜像、依赖、模板和文档资源需要离线导入或内网镜像化。
2. MeterSphere 项目本地开发环境已通过 `docker-compose-dev.yml` 启动 MySQL、Kafka、Redis、MinIO。
3. 测试跟踪前端是 Vue 2 微前端子应用，路由入口位于 `test-track/frontend/src/router/modules/track.js`。
4. 测试跟踪顶部菜单由 `test-track/frontend/src/business/head/TrackHeaderMenus.vue` 管理。
5. 项目二级路由权限映射位于 `framework/sdk-parent/frontend/src/utils/constants.js`。
6. Grist Core 使用 Apache 2.0 许可证，优先使用 `gristlabs/grist-oss` 镜像以降低内网开源合规风险。

## 术语表

| 术语 | 定义 |
|------|------|
| Grist Core | Grist 开源核心版，Apache 2.0 许可证，可自托管 |
| grist-oss | 仅包含开源代码的 Grist Docker 镜像，适合内网合规评审 |
| 多维表 | 具备表格、字段类型、引用、过滤、视图、权限和 API 能力的结构化协作表 |
| 辅助编写区 | 用户在 Grist 中批量编写和整理测试用例的临时工作区 |
| 正式用例库 | MeterSphere `test-track` 模块中的正式测试用例数据 |
| iframe 嵌入 | 在 MeterSphere 页面内通过 iframe 展示 Grist 内网页面 |
| 同步桥接服务 | 后续用于在 Grist 与 MeterSphere 之间同步用例数据的服务能力 |
| 离线部署包 | 在外网环境提前下载后导入内网的镜像、配置和初始化脚本集合 |

## 需求

### 需求 1：Grist 容器化本地部署

**用户故事：** 作为开发人员，我希望能够像启动 MySQL、Kafka、Redis、MinIO 一样启动 Grist，以便在本地复现瑞众内网的多维表嵌入环境。

#### 验收标准

1. THE 系统 SHALL 提供 Grist 本地 Docker Compose 方案。
2. THE Grist 容器 SHALL 与 MeterSphere 开发中间件共享 `metersphere-dev` Docker 网络。
3. THE Grist 服务 SHALL 默认监听本机 `8484` 端口。
4. THE Grist 数据 SHALL 持久化到 Docker volume 或本地目录，容器重启后数据不丢失。
5. THE 方案 SHALL 优先使用 `gristlabs/grist-oss` 镜像。
6. THE 方案 SHALL 支持内网离线镜像导入：外网 `docker pull/save`，内网 `docker load`。

### 需求 2：内网无外联运行

**用户故事：** 作为内网运维人员，我希望 Grist 在瑞众内网运行时不依赖外网，以便符合网络隔离和安全管控要求。

#### 验收标准

1. THE Grist 容器 SHALL 禁用自动版本检查。
2. THE Grist 容器 SHALL 关闭遥测。
3. THE Grist 容器 SHALL 禁用或隐藏外部帮助中心、模板库、计费、Google Drive、外部 Widget 等入口。
4. THE Grist 容器 SHALL 不配置任何公网 API Key 或外部 AI 服务。
5. IF 需要模板 THEN 模板 SHALL 通过内网导入 `.grist` 文件或 CSV 文件实现。
6. IF 需要自定义 Widget THEN Widget 清单和静态资源 SHALL 放在内网地址或直接禁用。

### 需求 3：测试跟踪模块菜单入口

**用户故事：** 作为测试人员，我希望在测试跟踪模块顶部菜单中看到“多维表编写”入口，以便快速进入辅助用例编写区。

#### 验收标准

1. THE 系统 SHALL 在测试跟踪模块新增 `/track/multitable` 路由。
2. THE 系统 SHALL 在 `TrackHeaderMenus.vue` 中新增“多维表编写”菜单项。
3. THE 菜单项 SHALL 复用 `PROJECT_TRACK_CASE:READ` 权限作为 POC 阶段访问控制。
4. WHEN 用户无测试用例读取权限 THEN 系统 SHALL 不展示多维表入口。
5. WHEN 当前路径为 `/track/multitable` THEN 顶部菜单 SHALL 高亮“多维表编写”。

### 需求 4：Grist iframe 嵌入页面

**用户故事：** 作为测试人员，我希望在 MeterSphere 页面内部直接使用 Grist 多维表，以便不离开测试平台即可编写用例。

#### 验收标准

1. THE 系统 SHALL 创建测试跟踪子应用内的 Grist 嵌入组件。
2. THE 嵌入组件 SHALL 使用 iframe 展示配置的 Grist URL。
3. THE iframe SHALL 占满测试跟踪内容区域，避免出现双重滚动和内容遮挡。
4. THE 页面 SHALL 提供加载中、加载失败和配置缺失状态。
5. THE Grist URL SHALL 从环境变量或后端配置获取，不在前端代码中硬编码生产地址。
6. IF 浏览器阻止 iframe 加载 THEN 页面 SHALL 给出可诊断提示。

### 需求 5：配置管理

**用户故事：** 作为系统管理员，我希望能够配置 Grist 内网地址和嵌入行为，以便不同环境使用不同的 Grist 实例。

#### 验收标准

1. THE 系统 SHALL 支持配置 Grist 基础地址，例如 `http://localhost:8484` 或内网域名。
2. THE 系统 SHALL 支持配置默认文档、工作区或视图 URL。
3. THE 系统 SHOULD 支持按项目映射不同 Grist 文档。
4. THE 系统 SHOULD 支持配置 iframe 是否显示外部打开按钮。
5. THE 配置 SHALL 避免泄露 Grist 管理员密钥和 API Key。

### 需求 6：认证与权限策略

**用户故事：** 作为安全管理员，我希望 Grist 访问权限与内网身份体系或 MeterSphere 权限边界一致，以便避免用例数据越权访问。

#### 验收标准

1. POC 阶段 THE 系统 MAY 使用 Grist 本地账号或默认身份运行。
2. 正式阶段 THE 系统 SHALL 优先接入瑞众内网统一身份源，使用 OIDC、SAML 或可信反向代理认证。
3. THE MeterSphere 菜单权限 SHALL 只控制入口可见性，不代替 Grist 内部文档权限。
4. THE Grist 文档 SHALL 按项目或团队设置访问权限。
5. THE 系统 SHALL 禁止在 URL 中明文传递长期管理员 Token。
6. IF 使用反向代理透传身份 THEN 代理 SHALL 防止客户端伪造用户头。

### 需求 7：用例模板

**用户故事：** 作为测试负责人，我希望 Grist 中有一张适合测试用例编写的模板表，以便团队按统一字段批量录入。

#### 验收标准

1. THE Grist 文档 SHALL 包含测试用例基础字段：用例名称、所属模块、优先级、用例类型、前置条件、步骤、预期结果、负责人、状态、标签、需求 ID。
2. THE Grist 文档 SHOULD 包含评审字段：评审状态、评审人、评审意见、通过时间。
3. THE Grist 文档 SHOULD 包含同步字段：MeterSphere 用例 ID、同步状态、同步时间、同步错误信息。
4. THE 模板 SHALL 支持列表视图和按状态过滤视图。
5. THE 模板 SHALL 能通过 `.grist` 文件或 CSV 离线导入。

### 需求 8：用例同步边界

**用户故事：** 作为测试人员，我希望在 Grist 中整理成熟的用例可以同步到 MeterSphere，以便后续评审、执行和报表仍在 MeterSphere 内完成。

#### 验收标准

1. POC 第一阶段 THE 系统 SHALL 只做 iframe 嵌入，不自动同步数据。
2. 第二阶段 THE 系统 SHOULD 提供手动同步入口，将 Grist 记录转换为 MeterSphere 测试用例。
3. 第三阶段 THE 系统 MAY 支持 Grist Webhook 或定时任务自动同步“评审通过”的记录。
4. THE 同步 SHALL 以 MeterSphere 为正式主数据，Grist 为辅助编写数据。
5. THE 同步 SHALL 记录成功、失败和幂等键，避免重复创建用例。
6. IF 同步失败 THEN 系统 SHALL 在 Grist 同步字段或 MeterSphere 同步日志中展示错误原因。

### 需求 9：前端测试验证

**用户故事：** 作为开发人员，我希望有完整的前端测试指南，以便验证本地 Grist 容器和 MeterSphere 嵌入页面是否可用。

#### 验收标准

1. THE 规格 SHALL 提供 `FRONTEND-TEST-GUIDE.md`。
2. THE 测试指南 SHALL 覆盖 Grist 容器启动、MeterSphere 前端启动、路由访问、iframe 加载、权限菜单和异常状态。
3. THE 测试指南 SHALL 包含瑞众内网无外联检查项。
4. THE 测试指南 SHALL 包含常见问题排查。

### 需求 10：合规与审计

**用户故事：** 作为合规人员，我希望方案能够说明许可证、数据留存和审计边界，以便评估是否能进入内网试点。

#### 验收标准

1. THE 文档 SHALL 明确 Grist Core / grist-oss 使用 Apache 2.0 许可证。
2. THE 文档 SHALL 明确 Grist 数据存储在内网本地卷中。
3. THE 文档 SHALL 明确 POC 阶段不存放生产敏感数据。
4. THE 文档 SHOULD 列出正式阶段需要补齐的审计能力和身份集成能力。
5. THE 文档 SHALL 明确 MeterSphere 正式用例库仍为主数据源。

## 非功能性需求

### 性能目标

| 场景 | 目标 |
|------|------|
| Grist 容器启动 | 本地开发机器 1 分钟内可访问登录/初始化页面 |
| iframe 页面加载 | 内网环境下 3 秒内展示 Grist 首屏或明确加载状态 |
| 测试用例模板 | 支持百级到千级用例记录的批量编辑 |
| MeterSphere 前端 | 新增嵌入页不影响原测试用例、评审、计划、缺陷页面加载 |

### 安全目标

1. 禁止公网依赖。
2. 禁止前端硬编码管理员密钥。
3. 禁止跨项目共享同一 Grist 文档且无权限隔离。
4. 禁止绕过 MeterSphere 权限直接暴露入口。
5. 正式阶段必须接入内网身份或可信代理认证。

### 兼容性

1. THE 前端 SHALL 基于 Vue 2、Element UI 和现有测试跟踪子应用实现。
2. THE 部署 SHALL 基于 Docker Compose，与 `docker-compose-dev.yml` 风格一致。
3. THE 方案 SHALL 支持本地开发、测试内网和生产内网三类环境的地址差异。

