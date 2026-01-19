# 项目结构

## 根目录布局

```
metersphere/
├── framework/              # 框架层（gateway、eureka、SDK）
├── api-test/              # 接口测试模块
├── performance-test/      # 性能测试模块
├── test-track/            # 测试跟踪模块
├── project-management/    # 项目管理模块
├── system-setting/        # 系统设置模块
├── report-stat/           # 报告统计模块
├── workstation/           # 工作台模块
├── workflow-service/      # 工作流服务（仅后端）
├── xpack-lib/             # 企业扩展（JAR）
├── scripts/               # 构建和部署脚本
├── docs/                  # 文档（中文）
├── pom.xml                # 根 Maven POM
└── Jenkinsfile            # CI/CD 流水线
```

## 模块结构模式

每个业务模块遵循一致的结构：

```
<模块名>/
├── backend/
│   ├── src/main/
│   │   ├── java/io/metersphere/
│   │   │   ├── controller/      # REST API 端点
│   │   │   ├── service/         # 业务逻辑
│   │   │   ├── base/mapper/     # MyBatis 数据访问
│   │   │   ├── dto/             # 数据传输对象
│   │   │   └── <模块Application>.java
│   │   └── resources/
│   │       ├── application.yml  # Spring Boot 配置
│   │       └── db/migration/    # Flyway 数据库迁移
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── api/                 # API 客户端定义
│   │   ├── business/            # 业务组件
│   │   ├── router/              # Vue Router 配置
│   │   ├── store/               # Pinia 状态管理
│   │   ├── i18n/                # 国际化
│   │   ├── main.js              # 入口文件
│   │   └── App.vue              # 根组件
│   ├── package.json
│   ├── pom.xml                  # Maven 前端插件
│   ├── vue.config.js
│   └── babel.config.js
└── pom.xml                      # 模块父 POM
```

## 框架层

### framework/gateway
- **用途**：API 网关和路由
- **技术**：Spring Cloud Gateway（WebFlux）
- **端口**：所有外部请求的入口
- **包含**：前端静态资源从这里提供服务

### framework/eureka
- **用途**：服务注册与发现
- **技术**：Spring Cloud Netflix Eureka
- **功能**：所有微服务在此注册

### framework/sdk-parent
共享类库和公共代码：

```
sdk-parent/
├── sdk/                   # 公共工具、安全、日志
├── domain/                # 共享数据模型和实体
├── jmeter/                # JMeter 引擎集成
├── frontend/              # 共享 Vue 组件
└── xpack-interface/       # 扩展点接口
```

## 业务模块

### api-test（端口：8004）
- API 定义和管理
- Mock 服务
- 测试场景编排
- 协议支持：HTTP、WebSocket、Dubbo、TCP
- 支持 Postman、Swagger、HAR 格式解析

### performance-test（端口：8005）
- JMeter 脚本管理
- 分布式压测
- 实时监控
- 性能报告

### test-track（端口：8003）
- 测试用例管理（列表和脑图视图）
- 测试计划执行
- 缺陷跟踪
- 与外部缺陷跟踪系统集成

### project-management（端口：8002）
- 项目 CRUD 操作
- 成员和权限管理
- 项目模板

### system-setting（端口：8001）
- 用户和组织管理
- 系统配置
- 工作空间管理
- 认证设置

### report-stat（端口：8006）
- 多维度统计
- 报告生成和导出
- 仪表盘可视化

### workstation（端口：8007）
- 个人工作台
- 任务聚合
- 常用功能快速访问

## 关键目录

### /scripts
构建自动化和部署脚本：
- `metersphere-build.sh` - 模块化构建系统
- `Makefile` - 构建快捷命令
- `build.config.example` - 配置模板

### /docs
完整的中文文档：
- `功能开发/` - 功能开发指南
- `技术文档/` - 技术文档
- `部署运维/` - 部署和运维
- `版本记录/` - 版本历史和变更日志

### /xpack-lib
构建时注入的企业扩展 JAR 文件：
- `metersphere-xpack-2.10.26-lts.jar`

## 配置文件

### Maven
- `pom.xml` - 根 POM，通过 `${revision}` 管理版本
- `lombok.config` - Lombok 配置
- `.flattened-pom.xml` - 由 flatten-maven-plugin 生成

### 前端
- `package.json` - 每个模块的 npm 依赖
- `vue.config.js` - Vue CLI 和 webpack 配置
- `babel.config.js` - JavaScript 转译
- `.eslintrc` - 代码检查规则（嵌入在 package.json 中）

### Docker
- `Dockerfile` - 每个模块一个，使用分层 JAR 方式
- Docker Compose 文件在部署时使用（不在仓库中）

### CI/CD
- `Jenkinsfile` - 流水线定义，支持多架构构建
- `Jenkinsfile.enhanced` - 增强版本，包含额外功能

## 命名约定

### Java 包
- `io.metersphere.<模块>` - 基础包
- `controller` - REST 端点
- `service` - 业务逻辑
- `base.mapper` - MyBatis 接口
- `dto` - 数据传输对象
- `domain` - 实体模型（在 sdk-parent/domain 中）

### 前端
- `kebab-case` 用于文件名
- `PascalCase` 用于 Vue 组件
- `camelCase` 用于 JavaScript 变量/函数

### 数据库
- Flyway 迁移脚本在 `src/main/resources/db/migration/`
- 命名规则：`V{版本}__{描述}.sql`

## 构建产物

### Maven 输出
- 后端 JAR：`<模块>/backend/target/*.jar`
- 解压的 JAR：`<模块>/backend/target/dependency/`（用于 Docker 分层）
- 前端构建：`<模块>/frontend/dist/`

### Docker 镜像
- 镜像仓库：`registry.fit2cloud.com/north/` 或 `registry.cn-qingdao.aliyuncs.com/metersphere/`
- 命名规则：`<模块名>:<版本>`
- 多架构：`linux/amd64`、`linux/arm64`

## 重要说明

- **SDK 优先**：始终先构建 `framework/sdk-parent`，再构建业务模块
- **Maven 中的前端**：前端构建通过 `frontend-maven-plugin` 集成到 Maven 生命周期
- **微前端**：每个前端模块独立构建，但在运行时通过 qiankun 加载
- **XPack 注入**：企业功能在 Docker 构建时从 `xpack-lib/` 注入
- **版本同步**：所有模块通过 Maven `${revision}` 属性共享相同版本
