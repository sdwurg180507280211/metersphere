# 快速开始

欢迎来到 MeterSphere 二次开发！本指南帮助你快速上手项目。

## 🎯 前置要求

### 必需环境
- **Java 17**（必须）
- **Maven 3.6+**（项目自带 Maven Wrapper）
- **Node.js v20.8.1**（前端构建）
- **MySQL 8.0+**
- **Redis**

### 可选环境
- **Docker 20.x**（容器化部署）
- **Kafka 3.6.1**（消息队列功能）
- **MinIO**（对象存储）

## 🚀 5分钟快速启动

### 1. 克隆项目
```bash
git clone <your-repo-url>
cd metersphere
```

### 2. 构建 SDK（必须先执行）
```bash
# 安装父 POM
./mvnw install -N

# 构建共享 SDK
./mvnw clean install -pl framework,framework/sdk-parent,framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter
```

### 3. 启动数据库
```bash
# 使用 Docker 快速启动（推荐）
docker run -d \
  --name metersphere-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=Password123@mysql \
  -e MYSQL_DATABASE=metersphere_dev \
  mysql:8.0
```

### 4. 启动服务
```bash
# 启动 Eureka（服务注册中心）
cd framework/eureka
./mvnw spring-boot:run

# 启动 Gateway（API 网关）
cd framework/gateway
./mvnw spring-boot:run

# 启动业务模块（以 system-setting 为例）
cd system-setting/backend
./mvnw spring-boot:run
```

### 5. 启动前端（可选）
```bash
cd system-setting/frontend
npm install
npm run serve
```

访问：http://localhost:8080

## 📖 下一步

### 了解项目结构
阅读 `02-开发指南/核心机制/项目结构分析.md`

### 学习核心机制
- page.condition 分页查询机制
- 自定义字段系统
- 权限管理模型

### 开始开发
1. 选择要开发的模块（03-功能模块）
2. 查看对应的功能文档
3. 参考数据库表结构（06-数据库）

## 🔧 常见问题

### Maven 构建失败
参考：`02-开发指南/环境搭建/Maven-在MeterSphere项目中的使用与常见问题.md`

### 前端启动失败
参考：`02-开发指南/核心机制/NPM镜像源配置说明.md`

### Docker 相关问题
参考：`05-部署运维/故障排查/Docker服务启动指南.md`

## 📚 推荐阅读顺序

1. **新手必读**
   - 本文档（快速开始）
   - 02-开发指南/核心机制/项目结构分析.md
   - 04-技术架构/微服务架构

2. **功能开发**
   - 02-开发指南/核心机制/page.condition核心机制系列
   - 03-功能模块（对应模块文档）
   - 06-数据库/表结构文档

3. **部署运维**
   - 05-部署运维/CI-CD
   - 05-部署运维/Docker部署

## 💡 开发建议

### 二次开发原则
1. **改动面小**：尽量少改官方核心链路
2. **边界清晰**：集中在少数模块/文件
3. **可回滚**：升级时能快速对照、快速搬运

### 代码规范
- 遵循项目现有代码风格
- 添加详细中文注释
- 高内聚、低耦合
- 优先使用现有工具类

### 调试技巧
- 使用 Spring Boot DevTools 热重载
- 善用断点调试
- 查看日志定位问题
- 使用 Postman 测试 API

## 🆘 获取帮助

- 查看文档中心：`docs/README.md`
- 搜索相关功能文档：`03-功能模块`
- 查看故障排查：`05-部署运维/故障排查`
- 参考数据库脚本：`06-数据库/SQL脚本`

---

**祝你开发顺利！** 🎉
