# 技术栈

## 构建系统

**Maven** 是主要构建工具，项目包含 Maven Wrapper（`./mvnw`）。

### 核心构建命令

```bash
# 安装父 POM 到本地仓库
./mvnw install -N

# 构建 SDK 和共享类库
./mvnw clean install -pl framework,framework/sdk-parent,framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter

# 完整构建（所有模块）
./mvnw clean package

# 跳过测试和前端的构建
mvn clean package -DskipTests -DskipAntRunForJenkins -pl "!framework/sdk-parent/frontend,!api-test/frontend,!performance-test/frontend,!project-management/frontend,!report-stat/frontend,!system-setting/frontend,!test-track/frontend,!workstation/frontend"
```

### 前端构建

每个模块都有独立的前端，使用 **npm** 和 **Vue CLI** 构建：

```bash
# 进入任意前端目录
cd api-test/frontend

# 安装依赖
npm install

# 开发服务器
npm run api  # 或 npm run serve

# 生产构建
npm run build

# 代码检查
npm run lint
```

## 后端技术栈

- **Java 17**（必需）
- **Spring Boot 3.2.12** - 核心框架
- **Spring Cloud 2023.0.1** - 微服务基础设施
- **MyBatis 3.0.3** - ORM 框架，配合 PageHelper 6.0.0 分页插件
- **Eureka** - 服务发现
- **Spring Cloud Gateway** - API 网关（基于 WebFlux）
- **Apache Shiro 2.0.1** - 安全认证
- **Kafka 3.6.1** - 消息队列
- **Redisson 3.25.0** - 分布式锁和缓存
- **Quartz 1.0.8** - 任务调度
- **JMeter 5.5** - 性能测试引擎
- **Selenium 4.10.0** - UI 自动化

### 核心类库

- Lombok - 代码生成
- Guava 32.0.1 - 工具类
- EasyExcel 3.1.1 - Excel 处理
- Jsoup 1.15.3 - HTML 解析
- Jackson - JSON 处理
- Commons IO 2.11.0 - 文件操作

## 前端技术栈

- **Vue.js 2.7.3** - 核心框架
- **Vue Router 3.1.3** - 路由管理
- **Pinia 2.0.14** - 状态管理
- **Element UI 2.15.13** - 组件库
- **qiankun** - 微前端框架
- **Axios 1.6.0** - HTTP 客户端
- **ECharts 5.0.2** - 图表可视化
- **Vue I18n 8.15.3** - 国际化
- **Mavon Editor 2.10.4** - Markdown 编辑器

### 构建工具

- **Vue CLI 5.0.7** - 构建工具
- **Webpack** - 模块打包器（通过 Vue CLI 集成）
- **Babel 7.12.16** - JavaScript 编译器
- **Sass 1.43.4** - CSS 预处理器
- **ESLint 7.32.0** - 代码检查

## 基础设施

- **MySQL 8.0.x** - 主数据库
- **Redis** - 缓存和会话存储
- **Kafka 3.6.1** - 事件流
- **MinIO** - 对象存储
- **Docker 20.x** - 容器化
- **Kubernetes 1.20+** - 容器编排（可选）

## 开发工具

- **Maven Wrapper** - 确保 Maven 版本一致
- **Node v20.8.1** - 前端运行时（在 pom.xml 中指定）
- **npm 8.4.0** - 包管理器
- **frontend-maven-plugin 1.12.1** - 将 npm 构建集成到 Maven

## 版本管理

项目使用 Maven 的 `revision` 属性统一管理所有模块的版本。版本在根 `pom.xml` 中设置，并传播到所有子模块。

## 代码质量

- **ESLint** 为 Vue.js 配置，采用宽松规则以支持快速开发
- **Lombok** 减少 Java 样板代码
- **Spring Boot DevTools** 支持开发时热重载

## 测试

- **JUnit** - 单元测试（Spring Boot Starter Test）
- **Mockito** - Mock 框架
- **Spring Kafka Test** - Kafka 集成测试
- **Reactor Test** - 响应式流测试

## CI/CD

- **Jenkins** - 主要 CI/CD 工具（参见 `Jenkinsfile`）
- **Docker Buildx** - 多架构镜像构建（amd64、arm64）
- **pollSCM** - Git 轮询自动构建（每天凌晨 1 点）
- 分支到版本映射：`v2.10.23` → `2.10.23`

## 常用模式

- **微服务**：每个模块可独立部署
- **微前端**：前端模块通过 qiankun 动态加载
- **分层架构**：Controller → Service → Mapper（MyBatis）
- **DTO 模式**：数据传输对象与领域模型分离
- **共享 SDK**：公共工具类在 `framework/sdk-parent/sdk` 中
