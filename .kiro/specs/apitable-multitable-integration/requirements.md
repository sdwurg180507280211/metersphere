# 需求文档：测试跟踪模块嵌入 APITable 多维表

## 简介

为 MeterSphere V2.10 测试跟踪模块评估 APITable 作为在线多维表能力的备选方案，用于辅助测试用例编写、评审准备、批量结构化整理和后续同步。

本规格定位为 **备选 POC 方案**。APITable 功能形态更接近 Airtable，具备多视图、字段类型、权限、API、分享和嵌入能力；但它相对 Grist 更重，开源版采用 AGPL-3.0，正式进入瑞众内网前需要完成资源评估和开源合规评审。

本阶段目标是先完成本地和内网 POC 规格设计，不替换 MeterSphere 原生用例库。APITable 作为辅助编写区，MeterSphere 仍作为正式用例、评审、执行和报表的主数据系统。

## 背景约束

1. 瑞众保险内网环境无法直接访问外网，所有镜像、Compose 包、依赖、模板和文档资源需要离线导入或内网镜像化。
2. 本仓库保留 `docker-compose-dev.yml` 作为开发中间件示例；当前本机实际通过 `/opt/metersphere/docker-compose-*.yml` 启动 MySQL、Kafka、Redis、MinIO，并创建 `metersphere_ms-network` Docker 网络。
3. 测试跟踪前端是 Vue 2 微前端子应用，路由入口位于 `test-track/frontend/src/router/modules/track.js`。
4. 测试跟踪顶部菜单由 `test-track/frontend/src/business/head/TrackHeaderMenus.vue` 管理。
5. 项目二级路由权限映射位于 `framework/sdk-parent/frontend/src/utils/constants.js`。
6. APITable 官方 README 建议 Docker Compose 部署环境至少 4 CPU / 8GB RAM。
7. APITable 官方 README 说明 native arm64 / Apple Silicon 镜像尚未准备好，arm64 或 Apple Silicon 可能性能较差。
8. APITable all-in-one 镜像仅适合 demo 或测试，不建议用于企业或生产环境。
9. APITable 开源版使用 AGPL-3.0，嵌入页去除 “Powered by APITable” 标识会触发额外许可风险，需要合规确认。

## 术语表

| 术语 | 定义 |
|------|------|
| APITable | 开源 API-oriented 多维表产品，提供表格、视图、权限、API、分享和嵌入能力 |
| AITable.ai | APITable 官方商业云服务和企业能力入口，不等同于本规格的开源自托管 POC |
| all-in-one 镜像 | `apitable/all-in-one`，官方说明仅用于 demo 或测试 |
| 多维表 | 具备字段类型、视图、过滤、分组、权限、API 和协作能力的结构化表 |
| Datasheet | APITable 中承载多维表数据的核心对象 |
| Space | APITable 的空间/工作区概念，可用于团队或项目隔离 |
| 辅助编写区 | 用户在 APITable 中批量编写和整理测试用例的临时工作区 |
| 正式用例库 | MeterSphere `test-track` 模块中的正式测试用例数据 |
| iframe 嵌入 | 在 MeterSphere 页面内通过 iframe 展示 APITable 内网页面 |
| 同步桥接服务 | 后续用于在 APITable 与 MeterSphere 之间同步用例数据的服务能力 |
| 离线部署包 | 在外网环境提前下载后导入内网的镜像、配置、Compose、初始化模板集合 |

## 需求

### 需求 1：APITable 容器化本地 POC

**用户故事：** 作为开发人员，我希望能够在本地用 Docker 启动 APITable，以便验证其是否适合作为 MeterSphere 测试用例辅助编写区。

#### 验收标准

1. THE 系统 SHALL 提供 APITable 本地 Docker Compose POC 方案。
2. THE APITable POC SHALL 与当前 MeterSphere 中间件 Docker 网络连通，默认使用 `/opt/metersphere` 部署创建的 `metersphere_ms-network`，并可通过环境变量覆盖为 `metersphere-dev` 等其他本地网络。
3. THE APITable POC SHALL 默认映射到本机 `8088` 端口，避免占用 80 端口。
4. THE APITable 数据 SHALL 持久化到 Docker volume 或本地目录，容器重启后数据不丢失。
5. THE POC 方案 MAY 使用 `apitable/all-in-one:latest` 镜像。
6. THE 文档 SHALL 明确 all-in-one 镜像仅用于 demo 或测试，不作为正式内网部署方案。
7. THE 方案 SHALL 支持内网离线镜像导入：外网 `docker pull/save`，内网 `docker load`。

### 需求 2：正式内网部署准备

**用户故事：** 作为内网运维人员，我希望 APITable 能在瑞众内网无外网环境部署，以便满足网络隔离和安全管控要求。

#### 验收标准

1. THE 方案 SHALL 说明官方 `curl https://apitable.github.io/install.sh | bash` 不适合内网直接执行。
2. THE 方案 SHALL 要求外网环境提前下载官方 Compose 包和相关镜像。
3. THE 方案 SHALL 要求内网通过 `docker load` 或内网镜像仓库导入镜像。
4. THE 方案 SHALL 要求固定镜像 tag 或 digest，避免正式环境使用不可追溯的 `latest`。
5. THE 方案 SHALL 要求确认 APITable 是否有外部模板、帮助、AI、统计、更新检查等外联行为，并在内网禁用或替换为内网地址。
6. THE 正式部署 SHALL 不复用 MeterSphere 业务 MySQL、Redis、MinIO，除非完成兼容性评估和数据边界设计。

### 需求 3：测试跟踪模块菜单入口

**用户故事：** 作为测试人员，我希望在测试跟踪模块顶部菜单中看到 APITable POC 入口，以便快速进入辅助用例编写区。

#### 验收标准

1. THE 系统 SHALL 在测试跟踪模块新增 APITable POC 路由，建议为 `/track/apitable`。
2. THE 系统 SHALL 在 `TrackHeaderMenus.vue` 中新增“APITable 编写”或“多维表编写（APITable）”菜单项。
3. THE 菜单项 SHALL 复用 `PROJECT_TRACK_CASE:READ` 权限作为 POC 阶段访问控制。
4. WHEN 用户无测试用例读取权限 THEN 系统 SHALL 不展示 APITable 入口。
5. WHEN 当前路径为 `/track/apitable` THEN 顶部菜单 SHALL 高亮 APITable 菜单项。
6. THE 正式阶段 SHOULD 抽象为 `/track/multitable` 统一入口，通过配置选择 Grist 或 APITable provider。

### 需求 4：APITable iframe 嵌入页面

**用户故事：** 作为测试人员，我希望在 MeterSphere 页面内部直接使用 APITable 多维表，以便不离开测试平台即可编写用例。

#### 验收标准

1. THE 系统 SHALL 创建测试跟踪子应用内的 APITable 嵌入组件。
2. THE 嵌入组件 SHALL 使用 iframe 展示配置的 APITable URL。
3. THE iframe SHALL 占满测试跟踪内容区域，避免出现双重滚动和内容遮挡。
4. THE 页面 SHALL 提供加载中、加载失败和配置缺失状态。
5. THE APITable URL SHALL 从环境变量或后端配置获取，不在前端代码中硬编码生产地址。
6. IF 浏览器阻止 iframe 加载 THEN 页面 SHALL 给出可诊断提示，并提供“新窗口打开”按钮。
7. THE 嵌入 SHALL 保留 APITable 开源版要求的品牌标识，除非采购或确认高级嵌入许可。

### 需求 5：配置管理

**用户故事：** 作为系统管理员，我希望能够配置 APITable 内网地址和嵌入行为，以便不同环境使用不同的 APITable 实例。

#### 验收标准

1. THE 系统 SHALL 支持配置 APITable 基础地址，例如 `http://localhost:8088` 或内网域名。
2. THE 系统 SHALL 支持配置默认 Space、Datasheet 或 Share URL。
3. THE 系统 SHOULD 支持按项目映射不同 APITable Datasheet。
4. THE 系统 SHOULD 支持配置 iframe 是否显示外部打开按钮。
5. THE 配置 SHALL 避免泄露 APITable 管理员密钥和 API Token。
6. THE 正式阶段 SHALL 由后端返回白名单内的 embed URL，避免前端加载任意外部地址。

### 需求 6：认证与权限策略

**用户故事：** 作为安全管理员，我希望 APITable 访问权限与内网身份体系或 MeterSphere 权限边界一致，以便避免用例数据越权访问。

#### 验收标准

1. POC 阶段 THE 系统 MAY 使用 APITable 本地账号或手工创建用户运行。
2. 正式阶段 THE 系统 SHALL 优先评估瑞众统一身份源集成能力。
3. THE MeterSphere 菜单权限 SHALL 只控制入口可见性，不代替 APITable 内部 Space、Folder、Datasheet 权限。
4. THE APITable Space 或 Datasheet SHALL 按项目或团队设置访问权限。
5. THE 系统 SHALL 禁止在 URL 中明文传递长期管理员 Token。
6. IF 使用反向代理透传身份 THEN 代理 SHALL 防止客户端伪造用户头。

### 需求 7：用例模板

**用户故事：** 作为测试负责人，我希望 APITable 中有一张适合测试用例编写的模板表，以便团队按统一字段批量录入。

#### 验收标准

1. THE APITable Datasheet SHALL 包含测试用例基础字段：用例名称、所属模块、优先级、用例类型、前置条件、步骤、预期结果、负责人、状态、标签、需求 ID。
2. THE APITable Datasheet SHOULD 包含评审字段：评审状态、评审人、评审意见、通过时间。
3. THE APITable Datasheet SHOULD 包含同步字段：MeterSphere 用例 ID、同步状态、同步时间、同步错误信息。
4. THE 模板 SHALL 支持表格视图、看板视图和按状态过滤视图。
5. THE 模板 SHALL 能通过 CSV、Excel 或 APITable 自身导入能力在内网初始化。

### 需求 8：用例同步边界

**用户故事：** 作为测试人员，我希望在 APITable 中整理成熟的用例可以同步到 MeterSphere，以便后续评审、执行和报表仍在 MeterSphere 内完成。

#### 验收标准

1. POC 第一阶段 THE 系统 SHALL 只做 iframe 嵌入，不自动同步数据。
2. 第二阶段 THE 系统 SHOULD 提供手动同步入口，将 APITable 记录转换为 MeterSphere 测试用例。
3. 第三阶段 THE 系统 MAY 支持 APITable API、Webhook 或定时任务同步“评审通过”的记录。
4. THE 同步 SHALL 以 MeterSphere 为正式主数据，APITable 为辅助编写数据。
5. THE 同步 SHALL 记录成功、失败和幂等键，避免重复创建用例。
6. IF 同步失败 THEN 系统 SHALL 在 APITable 同步字段或 MeterSphere 同步日志中展示错误原因。

### 需求 9：前端测试验证

**用户故事：** 作为开发人员，我希望有完整的前端测试指南，以便验证本地 APITable 容器和 MeterSphere 嵌入页面是否可用。

#### 验收标准

1. THE 规格 SHALL 提供 `FRONTEND-TEST-GUIDE.md`。
2. THE 测试指南 SHALL 覆盖 APITable 容器启动、MeterSphere 前端启动、路由访问、iframe 加载、权限菜单和异常状态。
3. THE 测试指南 SHALL 包含瑞众内网无外联检查项。
4. THE 测试指南 SHALL 包含资源消耗和性能排查项。
5. THE 测试指南 SHALL 包含常见问题排查。

### 需求 10：合规与审计

**用户故事：** 作为合规人员，我希望方案能够说明 APITable 许可证、嵌入许可、数据留存和审计边界，以便评估是否能进入内网试点。

#### 验收标准

1. THE 文档 SHALL 明确 APITable 开源版使用 AGPL-3.0。
2. THE 文档 SHALL 明确 APITable 数据存储在内网本地卷或内网数据库中。
3. THE 文档 SHALL 明确 POC 阶段不存放生产敏感数据。
4. THE 文档 SHALL 明确嵌入页不得移除 “Powered by APITable” 标识，除非完成商业许可或法务确认。
5. THE 文档 SHOULD 列出正式阶段需要补齐的审计能力、身份集成能力和备份能力。
6. THE 文档 SHALL 明确 MeterSphere 正式用例库仍为主数据源。

## 非功能性需求

### 性能目标

| 场景 | 目标 |
|------|------|
| APITable POC 容器启动 | 本地开发机器数分钟内可访问初始化页面 |
| iframe 页面加载 | 内网环境下 5 秒内展示 APITable 首屏或明确加载状态 |
| 测试用例模板 | 支持千级用例记录的批量编辑 POC |
| MeterSphere 前端 | 新增嵌入页不影响原测试用例、评审、计划、缺陷页面加载 |

### 安全目标

1. 禁止公网依赖。
2. 禁止前端硬编码管理员密钥。
3. 禁止跨项目共享同一 Datasheet 且无权限隔离。
4. 禁止绕过 MeterSphere 权限直接暴露入口。
5. 正式阶段必须接入内网身份或可信代理认证。
6. 正式阶段必须完成 AGPL 和嵌入许可评审。

### 兼容性

1. THE 前端 SHALL 基于 Vue 2、Element UI 和现有测试跟踪子应用实现。
2. THE 部署 SHALL 基于 Docker Compose，与 `docker-compose-dev.yml` 风格一致。
3. THE 方案 SHALL 支持本地开发、测试内网和生产内网三类环境的地址差异。
4. THE POC SHALL 避免占用 MeterSphere 已有端口。

## 参考资料

1. APITable GitHub: https://github.com/apitable/apitable
2. APITable README Installation: https://github.com/apitable/apitable#installation
3. APITable Licensing: https://github.com/apitable/apitable/blob/develop/LICENSING.md
4. APITable Developer Center: https://developers.aitable.ai/
