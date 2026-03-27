# MeterSphere Windows 系统启动方案

## 📋 文档目的

本文档提供在 **Windows 10/11** 系统下启动和运行 MeterSphere 项目的完整方案，重点说明 **Linux 与 Windows 的配置差异** 以及 **需要修改的代码和配置文件**。

## ⚠️ 重要提示

**MeterSphere 原生仅支持 Linux/Docker 部署**，代码中硬编码了大量 Linux 路径（如 `/opt/metersphere/`）。在 Windows 下开发需要：

1. **修改源代码中的路径常量**（多个 Java 文件）
2. **修改配置文件中的路径**（application.properties、logback.xml）
3. **使用环境变量或外部配置文件覆盖路径**

---

## 🔑 Linux vs Windows 路径差异总览

| 用途 | Linux 路径 | Windows 等价路径 |
|-----|-----------|----------------|
| 安装根目录 | `/opt/metersphere/` | `C:\metersphere\` 或项目根目录 |
| 配置文件目录 | `/opt/metersphere/conf/` | `C:\metersphere\conf\` |
| 日志目录 | `/opt/metersphere/logs/` | `C:\metersphere\logs\` |
| 数据目录 | `/opt/metersphere/data/` | `C:\metersphere\data\` |
| Body 文件 | `/opt/metersphere/data/body` | `C:\metersphere\data\body\` |
| 图片目录 | `/opt/metersphere/data/image/markdown` | `C:\metersphere\data\image\markdown\` |
| 附件目录 | `/opt/metersphere/data/attachment` | `C:\metersphere\data\attachment\` |
| JAR 包目录 | `/opt/metersphere/data/jar` | `C:\metersphere\data\jar\` |
| 临时目录 | `/opt/metersphere/data/tmp` | `C:\metersphere\data\tmp\` |

---

## 🎯 前置要求

### 必需环境

| 环境 | 版本 | 下载地址 | 说明 |
|-----|------|---------|------|
| **JDK** | 17+ | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 或 [OpenJDK](https://adoptium.net/) | 必须 17 以上 |
| **Maven** | 3.6+ | [Maven 下载](https://maven.apache.org/download.cgi) | 或使用项目自带 |
| **Node.js** | v20.8.1 | [Node.js 下载](https://nodejs.org/) | 前端构建必需 |
| **MySQL** | 8.0+ | [MySQL 下载](https://dev.mysql.com/downloads/) | 或使用 Docker |
| **Redis** | 6.0+ | [Redis Windows](https://github.com/tporadowski/redis) | 或使用 Docker |
| **Git** | 最新 | [Git 下载](https://git-scm.com/) | 版本控制 |

### 可选环境

| 环境 | 用途 |
|-----|------|
| **Docker Desktop** | 容器化部署 MySQL/Redis/Kafka |
| **Kafka** | 消息队列（知识库功能必需） |
| **MinIO** | 对象存储 |
| **Elasticsearch** | 全文检索 |

---

## 🔧 需要修改的代码和配置文件（重要）

### 一、必须修改的 Java 源代码

#### 1. FileUtils.java - 文件路径常量

**文件位置：** `framework/sdk-parent/sdk/src/main/java/io/metersphere/commons/utils/FileUtils.java`

**需要修改的常量（第 28-36 行）：**

```java
// 原始 Linux 路径
public static final String ROOT_DIR = "/opt/metersphere/";
public static final String BODY_FILE_DIR = "/opt/metersphere/data/body";
public static final String MD_IMAGE_DIR = "/opt/metersphere/data/image/markdown";
public static final String MD_IMAGE_TEMP_DIR = "/opt/metersphere/data/image/markdown/temp";
public static final String UI_IMAGE_DIR = "/opt/metersphere/data/image/ui/screenshots";
public static final String ATTACHMENT_DIR = "/opt/metersphere/data/attachment";
public static final String ATTACHMENT_TMP_DIR = "/opt/metersphere/data/attachment/tmp";
```

**修改为 Windows 路径：**

```java
// Windows 路径（根据实际安装位置修改）
public static final String ROOT_DIR = "C:/metersphere/";
public static final String BODY_FILE_DIR = "C:/metersphere/data/body";
public static final String MD_IMAGE_DIR = "C:/metersphere/data/image/markdown";
public static final String MD_IMAGE_TEMP_DIR = "C:/metersphere/data/image/markdown/temp";
public static final String UI_IMAGE_DIR = "C:/metersphere/data/image/ui/screenshots";
public static final String ATTACHMENT_DIR = "C:/metersphere/data/attachment";
public static final String ATTACHMENT_TMP_DIR = "C:/metersphere/data/attachment/tmp";
```

**还需修改第 350 行的临时目录路径：**

```java
// 原代码（第 350 行）
String dir = "/opt/metersphere/data/body/tmp" + File.separator;

// 修改为
String dir = BODY_FILE_DIR + File.separator + "tmp" + File.separator;
```

---

#### 2. CompressUtils.java - ZIP 临时路径

**文件位置：** `framework/sdk-parent/sdk/src/main/java/io/metersphere/commons/utils/CompressUtils.java`

**修改第 44 行：**

```java
// 原代码
private final static String ZIP_PATH = "/opt/metersphere/data/tmp/";

// 修改为
private final static String ZIP_PATH = "C:/metersphere/data/tmp/";
```

---

#### 3. JarConfigService.java - JAR 包目录

**文件位置：** `framework/sdk-parent/sdk/src/main/java/io/metersphere/service/JarConfigService.java`

**修改第 31 行：**

```java
// 原代码
private static final String JAR_FILE_DIR = "/opt/metersphere/data/jar";

// 修改为
private static final String JAR_FILE_DIR = "C:/metersphere/data/jar";
```

---

#### 4. AbstractPlatform.java - 图片目录

**文件位置：** `framework/sdk-parent/metersphere-platform-plugin-sdk/src/main/java/io/metersphere/platform/api/AbstractPlatform.java`

**修改第 34-35 行：**

```java
// 原代码
public static final String MD_IMAGE_DIR = "/opt/metersphere/data/image/markdown";
public static final String MD_IMAGE_TEMP_DIR = "/opt/metersphere/data/image/markdown/temp";

// 修改为
public static final String MD_IMAGE_DIR = "C:/metersphere/data/image/markdown";
public static final String MD_IMAGE_TEMP_DIR = "C:/metersphere/data/image/markdown/temp";
```

---

### 二、必须修改的配置文件

#### 1. application.properties - 日志路径

以下每个服务的 `application.properties` 都需要修改日志文件路径：

| 模块 | 文件路径 | 原配置 | 修改为 |
|-----|---------|-------|-------|
| **eureka** | `framework/eureka/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/${spring.application.name}` | `logging.file.path=C:/metersphere/logs/${spring.application.name}` |
| **gateway** | `framework/gateway/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/gateway` | `logging.file.path=C:/metersphere/logs/gateway` |
| **system-setting** | `system-setting/backend/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/system-setting` | `logging.file.path=C:/metersphere/logs/system-setting` |
| **api-test** | `api-test/backend/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/api-test` | `logging.file.path=C:/metersphere/logs/api-test` |
| **test-track** | `test-track/backend/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/test-track` | `logging.file.path=C:/metersphere/logs/test-track` |
| **performance-test** | `performance-test/backend/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/performance-test` | `logging.file.path=C:/metersphere/logs/performance-test` |
| **project-management** | `project-management/backend/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/project-management` | `logging.file.path=C:/metersphere/logs/project-management` |
| **report-stat** | `report-stat/backend/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/report-stat` | `logging.file.path=C:/metersphere/logs/report-stat` |
| **workstation** | `workstation/backend/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/workstation` | `logging.file.path=C:/metersphere/logs/workstation` |
| **workflow-service** | `workflow-service/backend/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/workflow` | `logging.file.path=C:/metersphere/logs/workflow` |
| **analytics-stat** | `analytics-stat/backend/src/main/resources/application.properties` | `logging.file.path=/opt/metersphere/logs/analytics` | `logging.file.path=C:/metersphere/logs/analytics` |

---

#### 2. application.properties - Redisson 配置文件路径

**文件位置：** `framework/eureka/src/main/resources/application.properties`（第 18 行）

```properties
# 原配置
spring.redis.redisson.file=file:/opt/metersphere/conf/redisson.yml

# 修改为（使用 Windows 路径）
spring.redis.redisson.file=file:C:/metersphere/conf/redisson.yml
```

**同样需要修改 SDK 中的配置：**

文件位置：`framework/sdk-parent/sdk/src/main/resources/commons.properties`（第 160 行）

```properties
# 原配置
spring.redis.redisson.file=file:/opt/metersphere/conf/redisson.yml

# 修改为
spring.redis.redisson.file=file:C:/metersphere/conf/redisson.yml
```

---

#### 3. logback.xml - 配置文件路径

以下文件中的 `property file` 路径需要修改：

| 文件位置 | 原配置 | 修改为 |
|---------|-------|-------|
| `framework/eureka/src/main/resources/logback.xml` | `<property file="/opt/metersphere/conf/metersphere.properties"/>` | `<property file="C:/metersphere/conf/metersphere.properties"/>` |
| `framework/gateway/src/main/resources/logback.xml` | `<property file="/opt/metersphere/conf/metersphere.properties"/>` | `<property file="C:/metersphere/conf/metersphere.properties"/>` |
| `system-setting/backend/src/main/resources/logback.xml` | `<property file="/opt/metersphere/conf/metersphere.properties"/>` | `<property file="C:/metersphere/conf/metersphere.properties"/>` |
| `api-test/backend/src/main/resources/logback.xml` | `<property file="/opt/metersphere/conf/metersphere.properties"/>` | `<property file="C:/metersphere/conf/metersphere.properties"/>` |
| `test-track/backend/src/main/resources/logback.xml` | `<property file="/opt/metersphere/conf/metersphere.properties"/>` | `<property file="C:/metersphere/conf/metersphere.properties"/>` |
| `performance-test/backend/src/main/resources/logback.xml` | `<property file="/opt/metersphere/conf/metersphere.properties"/>` | `<property file="C:/metersphere/conf/metersphere.properties"/>` |
| `project-management/backend/src/main/resources/logback.xml` | `<property file="/opt/metersphere/conf/metersphere.properties"/>` | `<property file="C:/metersphere/conf/metersphere.properties"/>` |
| `report-stat/backend/src/main/resources/logback.xml` | `<property file="/opt/metersphere/conf/metersphere.properties"/>` | `<property file="C:/metersphere/conf/metersphere.properties"/>` |
| `workstation/backend/src/main/resources/logback.xml` | `<property file="/opt/metersphere/conf/metersphere.properties"/>` | `<property file="C:/metersphere/conf/metersphere.properties"/>` |

---

#### 4. Application.java - 外部配置文件路径

所有微服务的 Application 入口类都配置了外部配置文件路径，需要修改：

**文件列表：**
- `framework/eureka/src/main/java/io/metersphere/eureka/EurekaApplication.java` (第 12 行)
- `framework/gateway/src/main/java/io/metersphere/gateway/GatewayApplication.java` (第 24 行)
- `system-setting/backend/src/main/java/io/metersphere/SettingApplication.java` (第 20 行)
- `api-test/backend/src/main/java/io/metersphere/ApiApplication.java` (第 22 行)
- `test-track/backend/src/main/java/io/metersphere/TrackApplication.java` (第 22 行)
- `performance-test/backend/src/main/java/io/metersphere/PerformanceApplication.java` (第 22 行)
- `project-management/backend/src/main/java/io/metersphere/ProjectApplication.java` (第 20 行)
- `report-stat/backend/src/main/java/io/metersphere/ReportApplication.java` (第 22 行)
- `workstation/backend/src/main/java/io/metersphere/WorkstationApplication.java` (第 22 行)
- `workflow-service/backend/src/main/java/io/metersphere/WorkflowApplication.java` (第 30 行)
- `analytics-stat/backend/src/main/java/io/metersphere/AnalyticsStatApplication.java` (第 42 行)

**修改方式（所有文件相同）：**

```java
// 原代码
@PropertySource(value = {
    "file:/opt/metersphere/conf/metersphere.properties"
}, ignoreResourceNotFound = true)
```

```java
// 修改为
@PropertySource(value = {
    "file:C:/metersphere/conf/metersphere.properties"
}, ignoreResourceNotFound = true)
```

---

#### 5. generatorConfig.xml - MyBatis 生成器配置

**文件位置：** `framework/sdk-parent/domain/src/main/resources/generatorConfig.xml`

```xml
<!-- 原配置 -->
<properties url="file:///opt/metersphere/conf/metersphere.properties"/>

<!-- 修改为 -->
<properties url="file:///C:/metersphere/conf/metersphere.properties"/>
```

---

### 三、推荐的替代方案（不修改源代码）

如果不想修改源代码，可以使用以下方案：

#### 方案 A：使用符号链接（Symbolic Link）

在 Windows 上创建符号链接，将 `C:/opt/metersphere` 映射到实际目录：

```powershell
# 以管理员身份运行 PowerShell
# 创建符号链接
New-Item -ItemType SymbolicLink -Path "C:\opt\metersphere" -Target "C:\metersphere"
```

这样代码中的 `/opt/metersphere/` 路径可以通过 JVM 参数重定向：

```powershell
# 启动时添加 JVM 参数
-Duser.language=en -Dfile.encoding=UTF-8
```

#### 方案 B：使用环境变量覆盖

在每个服务的启动配置中添加 JVM 参数，覆盖路径变量：

```powershell
# eureka 启动示例
java -Dlogging.file.path=C:/metersphere/logs/eureka ^
     -Dspring.redis.redisson.file=file:C:/metersphere/conf/redisson.yml ^
     -jar eureka.jar
```

---

### 四、修改后的验证清单

修改完成后，请验证以下项目：

- [ ] 创建 `C:/metersphere/` 根目录
- [ ] 创建 `C:/metersphere/conf/` 目录，放入 `metersphere.properties` 和 `redisson.yml`
- [ ] 创建 `C:/metersphere/logs/` 目录
- [ ] 创建 `C:/metersphere/data/body/` 目录
- [ ] 创建 `C:/metersphere/data/image/markdown/` 目录
- [ ] 创建 `C:/metersphere/data/attachment/` 目录
- [ ] 创建 `C:/metersphere/data/jar/` 目录
- [ ] 创建 `C:/metersphere/data/tmp/` 目录
- [ ] 修改所有 `application.properties` 中的日志路径
- [ ] 修改所有 `logback.xml` 中的配置文件路径
- [ ] 修改 `FileUtils.java` 中的所有路径常量
- [ ] 修改 `CompressUtils.java` 中的 ZIP 路径
- [ ] 修改 `JarConfigService.java` 中的 JAR 路径
- [ ] 修改所有 `Application.java` 中的 `@PropertySource` 路径
- [ ] **在 `metersphere.properties` 中配置 `tcp.mock.port=9000-9100`（必须，否则启动报错）**

---

## ⚠️ 常见启动错误及解决方案

### 1. Could not resolve placeholder 'tcp.mock.port'

**错误信息：**
```
Caused by: java.lang.IllegalArgumentException: Could not resolve placeholder 'tcp.mock.port' in value "${tcp.mock.port}"
```

**原因：** `metersphere.properties` 中缺少 `tcp.mock.port` 配置

**解决：** 在 `C:/metersphere/conf/metersphere.properties` 中添加：
```properties
tcp.mock.port=9000-9100
```

**受影响的服务：** workstation、api-test、project-management

---

### 2. HikariPool - Connection is not available

**原因：** 数据库连接配置错误

**解决：** 检查 `metersphere.properties` 中的数据库配置：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/metersphere_dev?...
spring.datasource.username=root
spring.datasource.password=你的密码
```

---

### 3. Cannot get Jedis connection

**原因：** Redis 未启动或连接配置错误

**解决：**
- Docker 方式：`docker ps | findstr redis`
- 手动方式：确认 `redis-server.exe` 正在运行
- [ ] 修改 `commons.properties` 中的 Redisson 路径

---

## 🚀 快速启动方案（保留原文档内容）

### 方案一：Docker 方式（推荐）

使用 Docker 可以快速启动所需的中间件，无需手动安装。

#### 1. 安装 Docker Desktop

1. 下载并安装 [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)
2. 启动 Docker Desktop，确保状态为绿色（运行中）
3. 启用 WSL 2 后端（设置 → Resources → WSL Integration）

#### 2. 创建 docker-compose.yml

在项目根目录创建 `docker-compose-dev.yml`：

```yaml
version: '3.8'

services:
  # MySQL 8.0
  mysql:
    image: mysql:8.0
    container_name: metersphere-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: Password123@mysql
      MYSQL_DATABASE: metersphere_dev
      MYSQL_USER: ms_dev
      MYSQL_PASSWORD: DevPass123
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./docs/05-部署运维/MySQL 初始化脚本:/docker-entrypoint-initdb.d
    command: >
      --default-authentication-plugin=mysql_native_password
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
    networks:
      - metersphere-network

  # Redis
  redis:
    image: redis:7-alpine
    container_name: metersphere-redis
    restart: always
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes
    networks:
      - metersphere-network

  # Kafka (可选，知识库功能需要)
  kafka:
    image: bitnami/kafka:3.6
    container_name: metersphere-kafka
    restart: always
    environment:
      - KAFKA_CFG_BROKER_ID=0
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
      - KAFKA_CFG_OFFSETS_TOPIC_REPLICATION_FACTOR=1
      - KAFKA_CFG_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
      - KAFKA_CFG_TRANSACTION_STATE_LOG_MIN_ISR=1
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
    networks:
      - metersphere-network

  # Zookeeper (Kafka 依赖)
  zookeeper:
    image: bitnami/zookeeper:3.8
    container_name: metersphere-zookeeper
    restart: always
    environment:
      - ALLOW_ANONYMOUS_LOGIN=yes
    ports:
      - "2181:2181"
    networks:
      - metersphere-network

networks:
  metersphere-network:
    driver: bridge

volumes:
  mysql-data:
  redis-data:
```

#### 3. 启动中间件

```powershell
# 在 PowerShell 中执行
docker-compose -f docker-compose-dev.yml up -d

# 查看状态
docker-compose -f docker-compose-dev.yml ps

# 查看日志（可选）
docker-compose -f docker-compose-dev.yml logs -f mysql
```

#### 4. 验证中间件状态

```powershell
# 测试 MySQL 连接
docker exec -it metersphere-mysql mysql -u root -pPassword123@mysql -e "SELECT VERSION();"

# 测试 Redis 连接
docker exec -it metersphere-redis redis-cli ping

# 测试 Kafka 连接
docker exec -it metersphere-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

---

### 方案二：纯手动方式（无 Docker）

如果不使用 Docker，需要手动安装所有中间件。

#### 1. 安装 MySQL

1. 下载 [MySQL Installer for Windows](https://dev.mysql.com/downloads/installer/)
2. 运行安装程序，选择 **Developer Default** 或 **Server only**
3. 配置 root 密码：`Password123@mysql`
4. 创建数据库：

```sql
CREATE DATABASE metersphere_dev
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER 'ms_dev'@'%' IDENTIFIED BY 'DevPass123';
GRANT ALL PRIVILEGES ON metersphere_dev.* TO 'ms_dev'@'%';
FLUSH PRIVILEGES;
```

#### 2. 安装 Redis

**方式 A：使用 WSL 2（推荐）**

```powershell
# 安装 WSL
wsl --install

# 在 WSL 中安装 Redis
wsl
sudo apt update
sudo apt install redis-server
sudo service redis-server start

# 修改配置允许远程访问
sudo nano /etc/redis/redis.conf
# 找到 bind 127.0.0.1 并修改为 bind 0.0.0.0
```

**方式 B：使用 Windows 原生版本**

1. 下载 [Redis for Windows](https://github.com/tporadowski/redis/releases)
2. 解压到 `C:\redis`
3. 运行：
```powershell
cd C:\redis
redis-server.exe
```

#### 3. 安装 Kafka（可选）

1. 下载 [Kafka](https://kafka.apache.org/downloads)
2. 解压到 `C:\kafka`
3. 启动 Zookeeper：
```powershell
cd C:\kafka
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
```

4. 启动 Kafka：
```powershell
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

---

## 🔧 配置项目

### 1. 配置 metersphere.properties

创建 `/opt/metersphere/conf/metersphere.properties` 的 Windows 等价配置。

在项目根目录创建 `metersphere.properties`：

```properties
# ========== 数据库配置 ==========
spring.datasource.url=jdbc:mysql://localhost:3306/metersphere_dev?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=Password123@mysql
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ========== Redis 配置 ==========
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0

# ========== Kafka 配置（可选） ==========
kafka.bootstrap-servers=localhost:9092

# ========== Eureka 配置 ==========
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

### 2. 修改服务启动配置

各服务需要在启动时指定 metersphere.properties 路径。

---

## 🏃 启动服务

### 方式一：使用 Maven 启动（推荐开发使用）

#### 1. 初始化 SDK

在 PowerShell 中执行：

```powershell
cd C:\path\to\metersphere

# 设置环境变量
$env:MAVEN_OPTS="-Xmx2g -Xms1g"

# 安装父 POM
.\mvnw.cmd install -N

# 构建核心 SDK
.\mvnw.cmd clean install -pl framework,framework/sdk-parent,framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter -DskipTests
```

#### 2. 启动 Eureka（服务注册中心）

```powershell
cd framework\eureka
.\mvnw.cmd spring-boot:run
```

等待看到日志：
```
Started EurekaApplication in X seconds
Tomcat started on port(s): 8761
```

#### 3. 启动 Gateway（API 网关）

新开一个 PowerShell 窗口：

```powershell
cd framework\gateway
.\mvnw.cmd spring-boot:run
```

等待看到日志：
```
Started GatewayApplication in X seconds
Tomcat started on port(s): 8000
```

#### 4. 启动系统设置服务

新开一个 PowerShell 窗口：

```powershell
cd system-setting\backend
# 指定 metersphere.properties 路径
.\mvnw.cmd spring-boot:run `-Dspring-boot.run.jvmArguments="-Dmetersphere.conf.file=C:\path\to\metersphere\metersphere.properties"
```

等待看到日志：
```
Started SettingApplication in X seconds
Tomcat started on port(s): 8001
```

#### 5. 启动其他业务服务

根据需要启动其他服务：

```powershell
# 项目管理
cd project-management\backend
.\mvnw.cmd spring-boot:run

# API 测试
cd api-test\backend
.\mvnw.cmd spring-boot:run

# 测试跟踪
cd test-track\backend
.\mvnw.cmd spring-boot:run
```

### 方式二：使用 IDEA 启动（最方便）

#### 1. 导入项目

1. 打开 IntelliJ IDEA
2. File → Open → 选择项目根目录
3. 等待 Maven 导入完成

#### 2. 配置运行配置

**启动 Eureka：**

1. 找到 `framework/eureka/src/main/java/io/metersphere/eureka/EurekaApplication.java`
2. 右键 → Run 'EurekaApplication'
3. 或点击绿色运行按钮

**启动 Gateway：**

1. 找到 `framework/gateway/src/main/java/io/metersphere/gateway/GatewayApplication.java`
2. 右键 → Run 'GatewayApplication'

**启动系统设置服务：**

1. 找到 `system-setting/backend/src/main/java/io/metersphere/setting/SettingApplication.java`
2. 右键 → Run 'SettingApplication'
3. 配置 VM Options：`-Dmetersphere.conf.file=C:\path\to\metersphere\metersphere.properties`

#### 3. 配置多服务同时启动

1. Run → Edit Configurations
2. 点击 `+` → Compound
3. 添加需要同时启动的服务
4. 点击 Run 一键启动所有服务

---

## 🌐 启动前端

### 1. 进入前端目录

```powershell
cd system-setting\frontend
```

### 2. 安装依赖

```powershell
# 使用项目自带的 Node.js（推荐）
cd ..\..\.node\node
.\node.exe ..\..\system-setting\frontend\node_modules\npm\bin\npm-cli.js install

# 或使用全局 Node.js
npm install
```

### 3. 配置开发服务器

创建/修改 `vue.config.js`：

```javascript
module.exports = {
  devServer: {
    port: 8080,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true
      }
    }
  }
}
```

### 4. 启动前端

```powershell
npm run serve
```

访问：http://localhost:8080

---

## ✅ 验证启动

### 1. 检查服务状态

| 服务 | 端口 | 访问地址 | 健康检查 |
|-----|------|---------|---------|
| Eureka | 8761 | http://localhost:8761 | http://localhost:8761/actuator/health |
| Gateway | 8000 | http://localhost:8000 | http://localhost:8000/actuator/health |
| System-Setting | 8001 | http://localhost:8001 | http://localhost:8001/actuator/health |
| 前端 | 8080 | http://localhost:8080 | - |

### 2. 检查 Eureka 控制台

访问 http://localhost:8761，查看已注册的服务：

- ✅ gateway - 应显示 UP
- ✅ setting - 应显示 UP
- ✅ 其他服务...

### 3. 访问前端

打开浏览器访问：http://localhost:8080

默认账号：
- 用户名：`admin`
- 密码：`metersphere`

---

## 🛠️ 常见问题

### 1. Maven 构建失败

**问题：** 依赖下载失败

**解决：**
```powershell
# 清理 Maven 缓存
.\mvnw.cmd clean

# 使用国内镜像
# 在 ~/.m2/settings.xml 配置阿里云镜像
<mirrors>
  <mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### 2. 端口被占用

**问题：** `Port 8761 was already in use`

**解决：**
```powershell
# 查找占用端口的进程
netstat -ano | findstr :8761

# 杀掉进程（替换 PID）
taskkill /F /PID <PID>

# 或修改服务端口
# 在 application.properties 中修改 server.port
```

### 3. MySQL 连接失败

**问题：** `Communications link failure`

**解决：**
1. 确认 MySQL 服务已启动
2. 检查端口 3306 是否开放
3. 确认数据库用户权限正确
4. 尝试关闭 Windows 防火墙

### 4. Redis 连接失败

**问题：** `Cannot get Jedis connection`

**解决：**
```powershell
# Docker 方式
docker ps | findstr redis

# 手动方式
# 确认 redis-server 正在运行
# 尝试 telnet localhost 6379
```

### 5. 前端构建失败

**问题：** `npm install` 卡住

**解决：**
```powershell
# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com

# 清理缓存
npm cache clean --force

# 删除 node_modules 重新安装
rmdir /s /q node_modules
npm install
```

### 6. 内存不足

**问题：** `Java heap space` 或 `OutOfMemoryError`

**解决：**
```powershell
# 增加 Maven 内存
$env:MAVEN_OPTS="-Xmx4g -Xms2g"

# 增加服务启动内存
# 在 IDEA Run Configuration 中配置 VM Options
-Xmx2g -Xms1g
```

---

## 📝 启动脚本

### 启动所有服务（PowerShell 脚本）

创建 `start-all.ps1`：

```powershell
# start-all.ps1 - MeterSphere 一键启动脚本

$ErrorActionPreference = "Stop"
$PROJECT_ROOT = "C:\path\to\metersphere"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  MeterSphere Windows 启动脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. 启动中间件（Docker 方式）
Write-Host "`n[1/4] 启动中间件..." -ForegroundColor Yellow
docker-compose -f "$PROJECT_ROOT\docker-compose-dev.yml" up -d
Start-Sleep -Seconds 10

# 2. 启动 Eureka
Write-Host "`n[2/4] 启动 Eureka..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", @"
cd '$PROJECT_ROOT\framework\eureka'
..\..\..\.node\node\node.exe ..\..\..\mvnw.cmd spring-boot:run
"@

Start-Sleep -Seconds 30

# 3. 启动 Gateway
Write-Host "`n[3/4] 启动 Gateway..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", @"
cd '$PROJECT_ROOT\framework\gateway'
..\..\..\.node\node\node.exe ..\..\..\mvnw.cmd spring-boot:run
"@

Start-Sleep -Seconds 15

# 4. 启动 System-Setting
Write-Host "`n[4/4] 启动 System-Setting..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", @"
cd '$PROJECT_ROOT\system-setting\backend'
\$env:MAVEN_OPTS='-Xmx2g'
..\..\..\.node\node\node.exe ..\..\..\mvnw.cmd spring-boot:run
"@

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  所有服务已启动！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "`n访问地址:"
Write-Host "  Eureka:  http://localhost:8761"
Write-Host "  Gateway: http://localhost:8000"
Write-Host "  前端：   http://localhost:8080"
```

### 停止所有服务

创建 `stop-all.ps1`：

```powershell
# stop-all.ps1 - MeterSphere 一键停止脚本

Write-Host "正在停止所有服务..." -ForegroundColor Yellow

# 停止 Docker 容器
docker-compose -f "docker-compose-dev.yml" down

# 停止 Java 进程
Get-Process | Where-Object { $_.ProcessName -eq "java" } | Stop-Process -Force

Write-Host "所有服务已停止" -ForegroundColor Green
```

---

## 📊 服务架构

```
┌─────────────────────────────────────────────────────┐
│                    前端 (8080)                        │
│                  Vue.js + Node.js                     │
└─────────────────────┬───────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│                   Gateway (8000)                      │
│              Spring Cloud Gateway                     │
└─────────────────────┬───────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
┌───────────────┬───────────────┬───────────────┐
│   Eureka      │   Setting     │   其他业务     │
│   (8761)      │   (8001)      │   服务         │
│  服务注册     │  系统设置     │   ...         │
└───────────────┴───────────────┴───────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
   ┌────────┐   ┌────────┐   ┌────────┐
   │ MySQL  │   │ Redis  │   │ Kafka  │
   │ (3306) │   │ (6379) │   │ (9092) │
   └────────┘   └────────┘   └────────┘
```

---

## 🔗 相关文档

- [开发指南](../02-开发指南/README.md)
- [Maven 使用指南](../02-开发指南/环境搭建/Maven-在 MeterSphere 项目中的使用与常见问题.md)
- [微服务架构](../04-技术架构/微服务架构/服务多环境数据库隔离.md)
- [CI-CD 部署](../05-部署运维/CI-CD/README.md)

---

**文档版本：** v1.0
**最后更新：** 2026-03-23
**适用系统：** Windows 10/11
