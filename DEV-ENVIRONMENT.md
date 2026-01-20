# MeterSphere 开发环境

本文档描述如何快速启动 MeterSphere 开发所需的中间件环境。

## 🚀 快速启动

### 方式一：使用管理脚本（推荐）

```bash
# 启动所有服务
./scripts/dev-env.sh start

# 查看服务状态
./scripts/dev-env.sh status

# 测试连接
./scripts/dev-env.sh test

# 停止服务
./scripts/dev-env.sh stop
```

### 方式二：使用 Docker Compose

```bash
# 启动服务
docker-compose -f docker-compose-dev.yml up -d

# 查看状态
docker-compose -f docker-compose-dev.yml ps

# 停止服务
docker-compose -f docker-compose-dev.yml down
```

## 📋 服务列表

| 服务 | 端口 | 用户名 | 密码 | 说明 |
|------|------|--------|------|------|
| MySQL 8.0.36 | 3306 | root | Password123@mysql | 主数据库 |
| MySQL 8.0.36 | 3306 | metersphere | Password123@mysql | 应用用户 |
| Redis 7.2.6 | 6379 | - | Password123@redis | 缓存服务 |
| Kafka 3.9.0 | 9092 | - | - | 消息队列 |
| MinIO | 9000/9001 | minioadmin | minioadmin123 | 对象存储 |

## 🗄️ 数据库信息

### 自动创建的数据库

- `metersphere_test` - 主业务数据库
- `metersphere_workflow` - 工作流数据库  
- `metersphere_audit` - 审计日志数据库

### 连接配置

```yaml
# application.yml 配置示例
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/metersphere_test?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: metersphere
    password: Password123@mysql
    
  redis:
    host: localhost
    port: 6379
    password: Password123@redis
    
  kafka:
    bootstrap-servers: localhost:9092
```

## 📁 统一配置管理

### 配置目录结构

所有 MySQL 相关配置已统一管理在 `/opt/metersphere/conf` 目录：

```
/opt/metersphere/conf/
└── mysql/
    ├── my.cnf                    # MySQL 主配置文件
    └── init/                     # 数据库初始化脚本目录
        └── 01-init-databases.sql # 数据库初始化脚本
```

### 配置管理命令

```bash
# 初始化配置目录（首次使用）
./scripts/dev-env.sh config init

# 编辑 MySQL 配置文件
./scripts/dev-env.sh config edit

# 查看当前配置
./scripts/dev-env.sh config show

# 备份当前配置
./scripts/dev-env.sh config backup

# 检查配置文件权限
./scripts/dev-env.sh config check
```

### 配置修改流程

1. **编辑配置**: `./scripts/dev-env.sh config edit`
2. **验证配置**: `./scripts/dev-env.sh verify`
3. **重启服务**: `./scripts/dev-env.sh restart`

### 配置文件说明

- **主配置文件**: `/opt/metersphere/conf/mysql/my.cnf`
  - 包含所有 MySQL 优化参数
  - 字符集、性能、InnoDB、二进制日志等配置

- **初始化脚本**: `/opt/metersphere/conf/mysql/init/`
  - 容器首次启动时自动执行
  - 创建必需的数据库和用户权限

### MySQL 优化配置

当前 MySQL 容器已应用以下优化配置：
- 字符集: `utf8mb4`
- 排序规则: `utf8mb4_general_ci`
- 表名大小写不敏感: `lower_case_table_names=1`
- 默认存储引擎: `InnoDB`

**性能优化**
- 关闭性能模式: `performance_schema=OFF`
- 表缓存: `table_open_cache=128`
- 最大连接数: `max_connections=1000`
- 最大连接错误数: `max_connect_errors=6000`
- 最大数据包: `max_allowed_packet=64M`

**事务配置**
- 事务隔离级别: `READ-COMMITTED`

**InnoDB 优化**
- 每表一个文件: `innodb_file_per_table=1`
- 缓冲池大小: `innodb_buffer_pool_size=512M`
- 刷新方法: `innodb_flush_method=O_DIRECT`
- 锁等待超时: `innodb_lock_wait_timeout=1800`

**二进制日志**
- 启用二进制日志: `log_bin=ON`
- 日志格式: `binlog_format=MIXED`
- 日志过期天数: `expire_logs_days=2`

**其他优化**
- 跳过域名解析: `skip_name_resolve=ON`
- SQL 模式: `STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION`

## 🔧 管理命令

```bash
# 查看帮助
./scripts/dev-env.sh help

# 启动服务
./scripts/dev-env.sh start

# 停止服务  
./scripts/dev-env.sh stop

# 重启服务
./scripts/dev-env.sh restart

# 查看状态
./scripts/dev-env.sh status

# 查看日志（所有服务）
./scripts/dev-env.sh logs

# 查看特定服务日志
./scripts/dev-env.sh logs mysql
./scripts/dev-env.sh logs redis
./scripts/dev-env.sh logs kafka
./scripts/dev-env.sh logs minio

# 测试连接
./scripts/dev-env.sh test

# 验证 MySQL 配置
./scripts/dev-env.sh verify

# MySQL 配置管理
./scripts/dev-env.sh config init      # 初始化配置目录
./scripts/dev-env.sh config edit      # 编辑配置文件
./scripts/dev-env.sh config show      # 显示当前配置
./scripts/dev-env.sh config check     # 检查配置权限
./scripts/dev-env.sh config backup    # 备份当前配置

# Docker 镜像仓库管理
./scripts/dev-env.sh registry login   # 登录阿里云镜像仓库
./scripts/dev-env.sh registry test    # 测试仓库连接
./scripts/dev-env.sh registry pull mysql:8.0.36  # 拉取指定镜像
./scripts/dev-env.sh registry list    # 列出本地镜像
./scripts/dev-env.sh registry clean   # 清理无用镜像

# 清理环境（删除所有数据）
./scripts/dev-env.sh clean
```

## 🌐 Web 界面

- **MinIO Console**: http://localhost:9001
  - 用户名: `minioadmin`
  - 密码: `minioadmin123`

## 📊 连接测试

### MySQL 连接测试

```bash
# 使用 MySQL 客户端连接
mysql -h localhost -P 3306 -u metersphere -pPassword123@mysql metersphere_test

# 或使用 Docker 容器内的客户端
docker exec -it metersphere-mysql mysql -u metersphere -pPassword123@mysql metersphere_test
```

### Redis 连接测试

```bash
# 使用 Redis 客户端连接
redis-cli -h localhost -p 6379 -a Password123@redis

# 或使用 Docker 容器内的客户端
docker exec -it metersphere-redis redis-cli -a Password123@redis
```

### Kafka 连接测试

```bash
# 列出 Topic
docker exec -it metersphere-kafka kafka-topics --bootstrap-server localhost:9092 --list

# 创建测试 Topic
docker exec -it metersphere-kafka kafka-topics --bootstrap-server localhost:9092 --create --topic test-topic --partitions 1 --replication-factor 1
```

## 🔍 故障排除

### Docker 服务问题

如果遇到 Docker 连接错误：

```bash
# 检查 Docker 服务状态
docker info

# 启动 OrbStack（如果使用 OrbStack）
open -a OrbStack

# 或启动 Docker Desktop
# 从应用程序启动 Docker Desktop
```

详细解决方案请参考：[Docker 服务启动指南](docs/故障排除/Docker服务启动指南.md)

### Docker 镜像仓库登录问题

如果遇到阿里云镜像仓库登录失败：

```bash
# 使用管理脚本诊断
./scripts/dev-env.sh registry test

# 查看详细解决方案
./scripts/dev-env.sh registry help
```

详细解决方案请参考：[Docker 镜像仓库登录问题解决方案](docs/故障排除/Docker镜像仓库登录问题解决方案.md)

### 端口冲突

如果遇到端口冲突，可以修改 `docker-compose-dev.yml` 中的端口映射：

```yaml
ports:
  - "13306:3306"  # MySQL 改为 13306
  - "16379:6379"  # Redis 改为 16379
  - "19092:9092"  # Kafka 改为 19092
```

### 服务启动失败

```bash
# 查看详细日志
docker-compose -f docker-compose-dev.yml logs [service-name]

# 重新构建并启动
docker-compose -f docker-compose-dev.yml up -d --force-recreate
```

### 数据持久化

所有数据都存储在 Docker 卷中：

```bash
# 查看数据卷
docker volume ls | grep metersphere

# 备份数据卷
docker run --rm -v metersphere_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz -C /data .
```

## 📝 开发配置

### Spring Boot 配置

在你的 `application-dev.yml` 中添加：

```yaml
spring:
  profiles:
    active: dev
    
  datasource:
    url: jdbc:mysql://localhost:3306/metersphere_test?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: metersphere
    password: Password123@mysql
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  redis:
    host: localhost
    port: 6379
    password: Password123@redis
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: metersphere-dev
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

# MinIO 配置
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin123
  bucket-name: metersphere
```

## 🎯 下一步

1. 启动中间件环境：`./scripts/dev-env.sh start`
2. 配置你的应用连接参数
3. 运行数据库迁移脚本
4. 启动 MeterSphere 应用

---

**注意**: 这是开发环境配置，不适用于生产环境。生产环境请使用更安全的密码和配置。