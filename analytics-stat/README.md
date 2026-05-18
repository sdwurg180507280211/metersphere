# 分析统计微服务

## 简介
分析统计微服务是MeterSphere的独立微服务模块，提供综合查询、SQL查询台、统计图表、数据字典等功能。

## 技术栈
- **后端**: Spring Boot 3.2.12 + Java 17
- **前端**: Vue 3.4.0 + Vite 5.0.0 + TypeScript 5.3.0
- **UI组件**: Element Plus 2.5.0
- **数据库**: MySQL 8.0+
- **服务注册**: Eureka Client

## 端口配置
- HTTP端口: 8009
- 管理端口: 7009
- 前端开发端口: 4009

## 快速开始

### 后端开发

#### 1. 编译项目
```bash
# 在项目根目录执行
./mvnw clean compile -pl analytics-stat/backend -am -DskipAntRunForJenkins=true
```

#### 2. 打包项目
```bash
./mvnw clean package -pl analytics-stat/backend -am -DskipTests -DskipAntRunForJenkins=true
```

#### 3. 启动服务

**方式一：使用Maven插件**
```bash
cd analytics-stat/backend
mvn spring-boot:run
```

**方式二：直接运行JAR**
```bash
cd analytics-stat/backend/target
java -jar analytics-stat-2.10.jar
```

**方式三：使用IDE**
- 运行主类：`io.metersphere.analyticsstat.AnalyticsStatApplication`

#### 4. 验证服务
访问健康检查接口：
```bash
curl http://localhost:8009/analytics-stat/health
```

预期响应：
```json
{
  "status": "UP",
  "service": "analytics-stat",
  "version": "1.0.0",
  "description": "分析统计微服务"
}
```

### 前端开发

#### 1. 安装依赖
```bash
cd analytics-stat/frontend
npm install
```

#### 2. 启动开发服务器
```bash
npm run dev
```

访问地址：http://localhost:4009

#### 3. 构建生产版本
```bash
npm run build
```

构建产物位于 `dist/` 目录。

## 项目结构

```
analytics-stat/
├── backend/                          # 后端模块
│   ├── src/main/
│   │   ├── java/
│   │   │   └── io/metersphere/analyticsstat/
│   │   │       ├── AnalyticsStatApplication.java    # 启动类
│   │   │       └── controller/
│   │   │           └── HealthController.java        # 健康检查
│   │   └── resources/
│   │       ├── application.properties               # 配置文件
│   │       └── db/migration/
│   │           └── V1__init_analytics_stat.sql      # 数据库初始化
│   ├── pom.xml
│   └── .gitignore
├── frontend/                         # 前端模块
│   ├── src/
│   │   ├── views/                    # 页面组件
│   │   │   ├── Layout.vue            # 布局
│   │   │   ├── Dashboard.vue         # 数据概览
│   │   │   ├── SqlConsole.vue        # SQL查询台
│   │   │   └── DataDictionary.vue    # 数据字典
│   │   ├── router/                   # 路由配置
│   │   ├── App.vue                   # 根组件
│   │   └── main.ts                   # 入口文件
│   ├── index.html
│   ├── vite.config.ts                # Vite配置
│   ├── tsconfig.json                 # TypeScript配置
│   ├── package.json
│   └── .gitignore
├── pom.xml                           # 父POM
├── Dockerfile                        # Docker镜像
└── README.md                         # 本文件
```

## 功能模块

### 1. 数据概览（Dashboard）
- 功能模块快速入口
- 系统状态展示
- 常用功能导航

### 2. SQL查询台（SqlConsole）
- SQL编辑器
- 查询结果展示
- 查询历史记录
- 结果导出（CSV、Excel）

### 3. 数据字典（DataDictionary）
- 字典类型管理
- 字典项CRUD
- 状态启用/禁用
- 排序管理

### 4. 统计图表（即将上线）
- 多维度数据统计
- 图表可视化
- 报表生成

## 数据库

### 表结构

#### sql_query_history（SQL查询历史）
- 记录用户的SQL查询历史
- 包含查询SQL、执行时间、结果行数、状态等

#### data_dictionary（数据字典）
- 系统数据字典配置
- 支持字典类型和字典项管理

### 初始化
数据库表会在服务启动时通过Flyway自动创建。

## 配置说明

### application.properties
```properties
# 服务名称
spring.application.name=analytics-stat

# 端口配置
server.port=8009
management.server.port=7009

# Flyway配置
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.table=metersphere_version

# 日志配置
logging.file.path=/opt/metersphere/logs/analytics-stat
```

### 外部配置
服务会加载以下外部配置文件：
- `classpath:commons.properties`
- `file:/opt/metersphere/conf/metersphere.properties`

## Docker部署

### 构建镜像
```bash
cd analytics-stat
docker build -t analytics-stat:2.10 .
```

### 运行容器
```bash
docker run -d \
  --name analytics-stat \
  -p 8009:8009 \
  -p 7009:7009 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/metersphere \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=password \
  analytics-stat:2.10
```

## 开发指南

### 添加新功能
1. 在 `backend/src/main/java/io/metersphere/analyticsstat/` 下创建相应的包
2. 创建Controller、Service、Mapper等类
3. 在 `frontend/src/views/` 下创建Vue组件
4. 在 `frontend/src/router/index.ts` 中添加路由

### 数据库迁移
1. 在 `backend/src/main/resources/db/migration/` 下创建新的SQL文件
2. 文件命名规则：`V{版本号}__{描述}.sql`
3. 例如：`V2__add_user_table.sql`

### API开发规范
- 所有API路径以 `/analytics-stat/` 开头
- 使用RESTful风格
- 返回统一的响应格式

## 常见问题

### Q: 服务启动失败，提示端口被占用
A: 检查8009和7009端口是否被其他服务占用，可以在application.properties中修改端口。

### Q: 前端开发服务器启动失败
A: 确保Node.js版本 >= 16，执行 `npm install` 重新安装依赖。

### Q: 数据库连接失败
A: 检查数据库配置，确保MySQL服务正常运行，数据库已创建。

### Q: Flyway迁移失败
A: 检查数据库用户权限，确保有创建表的权限。

## 相关文档
- [模块命名规范](../docs/03-功能模块/分析统计/模块命名规范.md)
- [前端技术栈选型](../docs/03-功能模块/分析统计/前端技术栈选型.md)
- [后端技术栈选型](../docs/03-功能模块/分析统计/后端技术栈选型.md)
- [资源占用评估](../docs/03-功能模块/分析统计/资源占用评估.md)
- [实施记录](../docs/03-功能模块/分析统计/分析统计微服务实施记录.md)

## 许可证
与MeterSphere主项目保持一致
