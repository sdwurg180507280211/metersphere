# MeterSphere 本地 RocketMQ 集成指南

## 一、文档目标

本文档整理 MeterSphere 在本地 `/opt/metersphere` 部署体系中集成 RocketMQ 的完整过程，目标是：

- 将 RocketMQ 以和 `redis`、`kafka`、`minio` 一致的方式纳入现有 Docker Compose 管理体系
- 保持统一入口：`docker-compose-base.yml + .env + compose_files`
- 明确最终落地文件、配置项、启动命令、验证方法和排障结论

本文档对应的是已经验证可用的本地方案，适用于当前 MeterSphere 中间件部署目录：

```bash
/opt/metersphere
```

---

## 二、最终集成结果

RocketMQ 最终被纳入 `/opt/metersphere` 的标准管理方式，和现有中间件保持一致：

- Compose 文件：`/opt/metersphere/docker-compose-rocketmq.yml`
- Broker 配置：`/opt/metersphere/conf/rocketmq/broker.conf`
- 环境变量：`/opt/metersphere/.env`
- Compose 聚合入口：`/opt/metersphere/compose_files`

最终服务包括 3 个容器：

| 服务 | 作用 | 容器名 | 端口 |
|------|------|--------|------|
| `rocketmq-namesrv` | NameServer，负责注册与路由 | `rocketmq-namesrv` | `9876` |
| `rocketmq-broker` | Broker，负责消息存储与转发 | `rocketmq-broker` | `10911`、`10909` |
| `rocketmq-console` | 控制台，查看 Topic、Broker、消费情况 | `rocketmq-console` | `19876` |

验证通过后的状态：

```bash
docker compose --env-file .env $(cat compose_files) ps rocketmq-namesrv rocketmq-broker rocketmq-console
```

预期结果：

- `rocketmq-namesrv`：`healthy`
- `rocketmq-broker`：`healthy`
- `rocketmq-console`：`Up`

控制台访问地址：

```text
http://localhost:19876
```

---

## 三、为什么不能直接照搬最初方案

最初方案的方向是对的，但直接落地到 MeterSphere 现有部署体系时有几个问题：

### 3.1 网络定义不一致

最初方案使用：

```yaml
networks:
  ms-network:
    external: true
```

但 MeterSphere 现有体系已经在 `docker-compose-base.yml` 中统一定义了 `ms-network`，RocketMQ 不应该再单独声明 `external: true`，否则会破坏统一入口。

### 3.2 `brokerIP1 = 127.0.0.1` 不能作为默认值

如果将 `brokerIP1` 固定为 `127.0.0.1`：

- 宿主机客户端可能可以连接
- 容器内客户端会把 `127.0.0.1` 解析成“自己容器”
- 最终导致控制台或其他服务获取到 Broker 地址后无法真正连通

因此本地默认不配置 `brokerIP1`。只有在宿主机外部客户端确实无法连接 Broker 时，才显式设置为宿主机局域网 IP。

### 3.3 宿主机 bind 目录权限导致 Broker 启动失败

最初思路是像下面这样把数据目录挂到宿主机：

```yaml
volumes:
  - ${MS_BASE}/metersphere/data/rocketmq/broker/logs:/home/rocketmq/logs
  - ${MS_BASE}/metersphere/data/rocketmq/broker/store:/home/rocketmq/store
```

但 `apache/rocketmq:4.9.4` 镜像默认用户是 `rocketmq`，而宿主机目录默认权限不允许该用户写入，最终表现为：

- `rocketmq-broker` 不断重启
- 退出码为 `253`
- 标准输出日志几乎为空

排查后确认，Broker 在不挂载这些宿主机目录时可以正常启动，挂载后失败，根因就是卷权限不匹配。

### 3.4 最终选择：命名卷 + 标准 Compose 管理

为了和 `redis`、`minio` 的管理风格保持一致，同时规避宿主机目录权限问题，最终方案采用：

- 配置文件使用 bind mount：`/opt/metersphere/conf/rocketmq/broker.conf`
- 数据目录使用 Docker 命名卷
- NameServer 和 Broker 明确使用 `user: "0:0"` 启动

这样做的结果是：

- 管理入口统一
- 不依赖手工调整宿主机目录权限
- 后续执行 `docker compose --env-file .env $(cat compose_files) up -d` 不会回退到错误配置

---

## 四、最终目录与文件

### 4.1 `/opt/metersphere/docker-compose-rocketmq.yml`

这是最终正式纳入部署体系的 Compose 文件。

关键设计点：

- 沿用统一网络 `ms-network`
- 端口和内存走 `.env` 变量
- `broker.conf` 从 `/opt/metersphere/conf/rocketmq/broker.conf` 只读挂载
- 日志和存储使用 Docker 命名卷
- `rocketmq-namesrv`、`rocketmq-broker` 使用 `user: "0:0"`

核心结构如下：

```yaml
services:
  rocketmq-namesrv:
    image: apache/rocketmq:4.9.4
    user: "0:0"
    ports:
      - "${MS_ROCKETMQ_NAMESRV_PORT:-9876}:9876"
    volumes:
      - ms-rocketmq-namesrv-logs:/home/rocketmq/logs
      - ms-rocketmq-namesrv-store:/home/rocketmq/store

  rocketmq-broker:
    image: apache/rocketmq:4.9.4
    user: "0:0"
    environment:
      NAMESRV_ADDR: "rocketmq-namesrv:9876"
      JAVA_OPTS: "-Duser.home=/home/rocketmq"
    ports:
      - "${MS_ROCKETMQ_BROKER_PORT:-10911}:10911"
      - "${MS_ROCKETMQ_BROKER_HA_PORT:-10909}:10909"
    volumes:
      - ${MS_BASE}/metersphere/conf/rocketmq/broker.conf:/home/rocketmq/rocketmq-4.9.4/conf/broker.conf:ro
      - ms-rocketmq-broker-logs:/home/rocketmq/logs
      - ms-rocketmq-broker-store:/home/rocketmq/store

  rocketmq-console:
    image: apacherocketmq/rocketmq-console:2.0.0
    ports:
      - "${MS_ROCKETMQ_CONSOLE_PORT:-19876}:8080"

volumes:
  ms-rocketmq-namesrv-logs:
  ms-rocketmq-namesrv-store:
  ms-rocketmq-broker-logs:
  ms-rocketmq-broker-store:
```

### 4.2 `/opt/metersphere/conf/rocketmq/broker.conf`

Broker 配置如下：

```properties
brokerClusterName = DefaultCluster
brokerName = broker-a
brokerId = 0
deleteWhen = 04
fileReservedTime = 48
brokerRole = ASYNC_MASTER
flushDiskType = ASYNC_FLUSH
namesrvAddr = rocketmq-namesrv:9876
listenPort = 10911
autoCreateTopicEnable = true
autoCreateSubscriptionGroup = true

# Do not set brokerIP1 to 127.0.0.1 when clients run inside Docker.
# If external clients cannot reach the broker, set it to the host LAN IP.
# brokerIP1 = 192.168.x.x
```

关键说明：

- `brokerId = 0` 表示当前是 `Master`
- `namesrvAddr = rocketmq-namesrv:9876` 使用 Docker 网络内服务名通信
- `flushDiskType = ASYNC_FLUSH` 适合本地开发环境
- 默认不设置 `brokerIP1`

### 4.3 `/opt/metersphere/.env`

需要增加 RocketMQ 相关环境变量：

```bash
# RocketMQ
MS_ROCKETMQ_NAMESRV_PORT=9876
MS_ROCKETMQ_BROKER_PORT=10911
MS_ROCKETMQ_BROKER_HA_PORT=10909
MS_ROCKETMQ_CONSOLE_PORT=19876
MS_ROCKETMQ_NAMESRV_HEAP_SIZE=512m
MS_ROCKETMQ_BROKER_HEAP_SIZE=512m
MS_ROCKETMQ_CONSOLE_HEAP_SIZE=256m
```

### 4.4 `/opt/metersphere/compose_files`

需要把 RocketMQ compose 文件纳入统一启动入口：

```text
-f docker-compose-base.yml -f docker-compose-mysql.yml -f docker-compose-kafka.yml -f docker-compose-redis.yml -f docker-compose-minio.yml -f docker-compose-elasticsearch.yml -f docker-compose-rocketmq.yml
```

这样后续使用以下命令时，RocketMQ 会和其他中间件一起被管理：

```bash
docker compose --env-file .env $(cat compose_files) up -d
```

---

## 五、标准集成步骤

### 5.1 创建 RocketMQ Broker 配置目录

```bash
mkdir -p /opt/metersphere/conf/rocketmq
```

### 5.2 写入 `broker.conf`

将上文中的 Broker 配置写入：

```bash
/opt/metersphere/conf/rocketmq/broker.conf
```

### 5.3 创建 `docker-compose-rocketmq.yml`

将最终版本的 RocketMQ compose 文件写入：

```bash
/opt/metersphere/docker-compose-rocketmq.yml
```

### 5.4 更新 `.env`

将 RocketMQ 端口和堆内存参数追加到：

```bash
/opt/metersphere/.env
```

### 5.5 更新 `compose_files`

将以下片段追加到已有的 Compose 聚合入口：

```text
-f docker-compose-rocketmq.yml
```

注意：

- 不要删除原有的 `mysql`、`kafka`、`redis`、`minio` 等 compose 文件
- RocketMQ 应该作为“新增一项”接入，而不是替换整行

### 5.6 校验 Compose 语法

建议在真正启动前先执行：

```bash
cd /opt/metersphere
docker compose --env-file .env $(cat compose_files) config
```

如果该命令可以正常展开完整配置，说明 YAML 语法与变量引用没有问题。

### 5.7 启动 RocketMQ

只启动 RocketMQ 相关服务：

```bash
cd /opt/metersphere
docker compose --env-file .env $(cat compose_files) up -d rocketmq-namesrv rocketmq-broker rocketmq-console
```

如果要和所有中间件一起启动：

```bash
cd /opt/metersphere
docker compose --env-file .env $(cat compose_files) up -d
```

---

## 六、验证方法

### 6.1 查看容器状态

```bash
cd /opt/metersphere
docker compose --env-file .env $(cat compose_files) ps rocketmq-namesrv rocketmq-broker rocketmq-console
```

期望看到：

- `rocketmq-namesrv`：`Up ... (healthy)`
- `rocketmq-broker`：`Up ... (healthy)`
- `rocketmq-console`：`Up`

### 6.2 查看 Broker 启动日志

```bash
docker logs rocketmq-broker --tail 100
```

成功标志：

```text
The broker[broker-a, xxx:10911] boot success. serializeType=JSON and name server is rocketmq-namesrv:9876
```

### 6.3 查看 NameServer 启动日志

```bash
docker logs rocketmq-namesrv --tail 50
```

成功标志：

```text
The Name Server boot success. serializeType=JSON
```

### 6.4 访问控制台

浏览器打开：

```text
http://localhost:19876
```

可在控制台中查看：

- Broker 列表
- Topic 列表
- Consumer Group
- 消息轨迹和基本统计信息

---

## 七、关键排障过程

### 7.1 现象：Broker 持续重启

初次接入后，`rocketmq-broker` 状态如下：

```text
Restarting (253)
```

同时：

- `rocketmq-namesrv` 正常
- `rocketmq-console` 正常
- `rocketmq-broker` 标准输出日志几乎为空

### 7.2 排查方向

排查过程中依次验证了以下点：

1. `broker.conf` 语法是否有误
2. NameServer 地址 `rocketmq-namesrv:9876` 是否可达
3. `mqbroker` 启动命令是否正确
4. `brokerIP1` 是否错误影响注册
5. 宿主机 bind 目录和 Docker 命名卷是否可写

### 7.3 根因确认

最终确认根因是卷权限问题：

- `apache/rocketmq:4.9.4` 默认用户是 `rocketmq`
- 宿主机 bind 目录和新建命名卷默认都由 `root` 管理
- Broker 进程无法向 `/home/rocketmq/logs` 和 `/home/rocketmq/store` 写入
- 因此启动阶段直接退出，返回码为 `253`

### 7.4 解决方式

最终采用两项修正：

1. `rocketmq-namesrv` 和 `rocketmq-broker` 显式加上：

```yaml
user: "0:0"
```

2. Broker 增加：

```yaml
JAVA_OPTS: "-Duser.home=/home/rocketmq"
```

这两项修正后，Broker 能稳定启动，并通过健康检查。

---

## 八、为什么最终方案更适合 MeterSphere 现有体系

和最初“单独写一个 RocketMQ compose 文件再手工启动”的方式相比，最终方案更适合当前 MeterSphere 目录结构，原因如下：

### 8.1 管理入口统一

后续你只需要继续使用现有命令：

```bash
docker compose --env-file .env $(cat compose_files) up -d
```

不需要额外记一个新的运行脚本或独立 compose 文件。

### 8.2 配置方式统一

RocketMQ 和其他中间件保持同样的风格：

- 网络来自 `docker-compose-base.yml`
- 端口和堆内存来自 `.env`
- 服务组合来自 `compose_files`

### 8.3 运维方式统一

后续排查时可以直接使用：

```bash
docker compose --env-file .env $(cat compose_files) ps
docker compose --env-file .env $(cat compose_files) logs -f rocketmq-broker
docker compose --env-file .env $(cat compose_files) restart rocketmq-broker
```

### 8.4 避免宿主机目录权限陷阱

如果继续坚持使用宿主机 `data/rocketmq/...` bind mount，本机权限模型会增加运维复杂度。当前以 Docker 命名卷管理 RocketMQ 数据，对本地开发环境更稳妥。

---

## 九、后续建议

### 9.1 如果需要宿主机直接查看数据

当前日志和存储使用的是 Docker 命名卷，而不是 `/opt/metersphere/data/rocketmq/...` 宿主机目录。

如果确实有以下诉求：

- 直接在宿主机查看 RocketMQ 存储文件
- 用宿主机备份脚本处理 RocketMQ 数据
- 强制和 MySQL/Redis 一样走宿主机 bind mount

则需要额外处理目录权限，确保 `rocketmq` 用户或 root 容器用户对目标目录有可写权限。

### 9.2 如果需要外部客户端连接 Broker

当前本地方案默认不设置 `brokerIP1`。

如果出现以下情况：

- 控制台正常
- 宿主机外的 Java 客户端拿到路由后无法连接 Broker

则需要在 `broker.conf` 中显式设置：

```properties
brokerIP1 = <宿主机局域网IP>
```

例如：

```properties
brokerIP1 = 192.168.1.20
```

### 9.3 如果后续项目真正接入 RocketMQ 客户端

建议进一步补充以下内容：

- Topic 命名规范
- Producer / Consumer Group 命名规范
- 重试、死信、幂等消费策略
- 本地、测试、生产三套环境配置差异

---

## 十、相关文件

本仓库中与本次 RocketMQ 集成相关的整理文件如下：

- 文档模板来源：`docs/rocketmq-local/docker-compose-rocketmq.yml`
- 运行期排障版：`docs/rocketmq-local/docker-compose-rocketmq-runtime.yml`
- Broker 配置模板：`docs/rocketmq-local/broker.conf`
- `.env` 变量片段：`docs/rocketmq-local/install.conf.rocketmq.snippet`

最终生效文件位于：

- `/opt/metersphere/docker-compose-rocketmq.yml`
- `/opt/metersphere/conf/rocketmq/broker.conf`
- `/opt/metersphere/.env`
- `/opt/metersphere/compose_files`

---

## 十一、最终结论

RocketMQ 已成功纳入 MeterSphere 现有本地部署体系，最终方案具备以下特征：

- 和现有 `redis`、`kafka`、`minio` 采用同一套 Compose 管理入口
- Broker 与 NameServer 均已可正常启动并通过健康检查
- Console 可通过 `http://localhost:19876` 正常访问
- 已明确记录了宿主机 bind mount 失败的根因和最终修正方式

后续只需使用标准命令管理即可：

```bash
cd /opt/metersphere
docker compose --env-file .env $(cat compose_files) up -d
docker compose --env-file .env $(cat compose_files) ps
docker compose --env-file .env $(cat compose_files) logs -f rocketmq-broker
```
