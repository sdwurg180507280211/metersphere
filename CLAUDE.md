# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在本仓库中工作提供指导。

## 项目概览

MeterSphere v2.10 — 一站式开源持续测试平台，涵盖测试跟踪、接口测试、UI 测试和性能测试。

## 构建命令

```bash
# 基础构建 - 安装 parent POM 到本地仓库
./mvnw install -N

# 安装 SDK 依赖（其他子工程依赖这些模块）
./mvnw clean install -pl framework,framework/sdk-parent,framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter

# 完整打包（跳过测试和前端）
mvn clean package -DskipTests -DskipAntRunForJenkins -pl "!framework/sdk-parent/frontend,!api-test/frontend,!performance-test/frontend,!project-management/frontend,!report-stat/frontend,!system-setting/frontend,!test-track/frontend,!workstation/frontend"

# 构建单个模块
./mvnw clean install -pl api-test -am -DskipTests

# 运行单个 Spring Boot 服务
./mvnw spring-boot:run -pl api-test/backend

# 运行特定模块测试
./mvnw -pl api-test -am test

# 前端 - 主 SDK 前端（Vue 2 + Webpack）
cd framework/sdk-parent/frontend && npm install && npm run gateway

# 前端 - 单个子应用（如 api-test）
cd api-test/frontend && npm install && npm run api

# 前端 - Vue 3 + Vite 模块（analytics-stat）
cd analytics-stat/frontend && npm install && npm run analytics
```

## 技术栈

- **后端**：Java 17、Spring Boot 3.2.12、Spring Cloud 2023.0.1、MyBatis、MySQL 8.0
- **前端**：Vue 2（主应用、Element UI）、Vue 3（analytics-stat、Element Plus/Naive UI）
- **前端构建**：Vue CLI 5（Vue 2 模块）、Vite 5（Vue 3 模块）
- **中间件**：Kafka、MinIO、Redis（Redisson）、Eureka、Spring Cloud Gateway
- **测试引擎**：JMeter 5.5、Selenium 4.10
- **Node**：v20.8.1（见 .nvmrc）

## 架构

### 微服务（Spring Cloud）

每个模块都是独立的 Spring Boot 微服务，并拥有各自的数据库职责范围：

| 模块 | 端口 | 用途 |
|------|------|------|
| `framework/eureka` | 8761 | 服务注册中心 |
| `framework/gateway` | 8000 | API 网关（Spring Cloud Gateway） |
| `system-setting` | 8001 | 系统设置与配置 |
| `project-management` | 8002 | 项目管理 |
| `performance-test` | 8003 | 性能/负载测试 |
| `api-test` | 8004 | 接口/API 测试 |
| `test-track` | 8005 | 测试用例管理与跟踪 |
| `report-stat` | 8006 | 报表与统计 |
| `workstation` | 8007 | 工作台/仪表盘 |
| `workflow-service` | 8008 | 工作流编排 |
| `analytics-stat` | 8009 | AI 驱动的数据分析工作台（服务名 `ai`） |

### 模块结构

每个业务模块都遵循类似的目录结构：

```
{module}/
├── backend/                  # Spring Boot 微服务
│   └── src/main/java/io/metersphere/
│       ├── {module}Application.java
│       ├── controller/       # REST 控制器
│       ├── service/          # 业务逻辑
│       ├── base/mapper/      # MyBatis Mapper
│       ├── api/              # 内部 API 客户端 / DTO
│       ├── commons/          # 常量、枚举、工具类
│       ├── listener/         # Kafka/事件监听器
│       └── websocket/        # WebSocket 处理器
├── frontend/                 # Vue 2 微前端子应用
└── pom.xml
```

### Framework / 共享 SDK

- `framework/sdk-parent/sdk/` — 后端共享 SDK：基础服务（用户、项目、工作空间、定时任务、插件）、安全/鉴权拦截器、文件管理、通知、WebSocket 基础设施
- `framework/sdk-parent/domain/` — 领域实体与 MyBatis Mapper（代码生成）
- `framework/sdk-parent/frontend/` — 前端共享 SDK：微前端编排器、公共组件（MsTable、MsDrawer、MsContainer、图表等）、API 客户端、Store（Pinia）、工具函数
- `framework/sdk-parent/jmeter/` — JMeter 集成引擎
- `framework/sdk-parent/xpack-interface/` — 企业版扩展接口
- `framework/sdk-parent/metersphere-plugin-sdk/` — 用于功能扩展的插件 SDK
- `framework/sdk-parent/metersphere-platform-plugin-sdk/` — 平台集成插件 SDK

### 微前端架构（京东 micro-app）

前端使用 `@micro-zoe/micro-app` 进行微前端编排：

- **主应用**：`framework/sdk-parent/frontend/` — 承载所有子应用，管理路由、共享状态和认证
- **子应用**：每个业务模块都有自己的前端，并作为 micro-app 加载
- **Vue 2 子应用**：基于 Webpack，使用默认沙箱（api、performance、track、project、setting、workstation、report）
- **Vue 3 子应用**：基于 Vite（analytics-stat），需要 iframe 沙箱
- **配置**：见 `framework/sdk-parent/frontend/src/micro-app-config.js` — 模块注册表、运行时策略、入口 URL 计算
- **预加载**：通过 `microApp.preFetch()` 在 `micro-app-setup.js` 中进行基于优先级的预加载

### 前端脚本约定

- Vue 2 子应用的开发脚本通常等于模块名，如 `api-test/frontend` 使用 `npm run api`，`project-management/frontend` 使用 `npm run project`
- Vue 2 子应用依赖本地共享包 `metersphere-frontend: file:../../framework/sdk-parent/frontend`
- `analytics-stat/frontend` 是 Vue 3 + Vite 模块，开发使用 `npm run analytics`，类型检查和构建使用 `npm run build:check`

### 代码风格

- Java 17，4 空格缩进，启用 Lombok（`@Data`、`@Builder` 等）
- REST 接口路径使用 kebab-case（如 `/api/test-plan`）
- MyBatis Mapper 位于 `base/mapper/`，XML 位于 `src/main/resources/`
- 数据库迁移使用 Flyway，脚本位于各模块 `backend/src/main/resources/db/migration/`，表结构变更应新增 `Vxxx__*.sql`
- 前端：ESLint（Vue 2：`@babel/eslint-parser`，Vue 3：TypeScript + `vue-tsc`）
- Vue 2 与 Vue 3 模块均使用 Pinia 进行状态管理
- 国际化使用 vue-i18n

### 运行时外部配置

- 各后端服务通过 `@PropertySource("file:/opt/metersphere/conf/metersphere.properties", ignoreResourceNotFound = true)` 读取运行时外部配置
- 数据库、Kafka、Elasticsearch、第三方服务地址和密钥等环境相关配置优先查看 `/opt/metersphere/conf/metersphere.properties`
- MySQL 以 Docker 容器形式启动；连接信息参考 `/opt/metersphere/conf/metersphere.properties`，其中包含数据库 IP、端口、用户名和密码
- 执行 SQL 命令时，先从 `/opt/metersphere/conf/metersphere.properties` 确认连接参数，再使用 MySQL 客户端，例如：

```bash
MYSQL_PWD='<password-from-metersphere.properties>' mysql --protocol=TCP -h <host> -P <port> -u <user> <database> \
    -e "SELECT id, name, email, status, phone FROM \`user\` LIMIT 10;"
```

- Redis 客户端配置使用 Redisson，配置文件为 `/opt/metersphere/conf/redisson.yml`
- 向 `metersphere.properties` 追加新配置时，先检查该文件中是否已有同类服务配置，并以现有运行环境值为准

### 关键中间件集成

- **Kafka**：服务间异步事件通信（执行队列、测试计划事件、清理任务等）
- **MinIO**：文件/附件存储
- **Redis**：缓存、分布式锁（Redisson）
- **MySQL**：业务数据库，连接信息由 `/opt/metersphere/conf/metersphere.properties` 注入
- **Gateway**：路由各子应用静态资源和 API 请求，并处理鉴权/会话过滤
