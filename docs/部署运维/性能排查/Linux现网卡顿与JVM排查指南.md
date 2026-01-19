# Linux 现网卡顿与 JVM 排查指南（Docker 场景）

> 适用场景：MeterSphere 已在 Linux 正式环境部署（Docker/Compose/K8s 皆可参考），出现“页面卡/接口慢/偶发卡死几秒”。
>
> 目标：用**最小侵入**方式快速判断瓶颈归因：
>
>- 宿主机：CPU/Load、内存/Swap、磁盘 IO、网络
>- 容器：cgroup limit、容器 CPU/内存是否打满
>- JVM：线程阻塞/死锁/锁竞争/线程池耗尽、GC STW 暂停、热点方法

---

## 0. 原则（务必遵守）

- **先取证再优化**：先抓线程栈/GC 指标/系统指标，再决定是否改 JVM 参数。
- **一次只改一个变量**：否则无法回溯“哪一项调整生效”。
- **线上优先低侵入**：优先使用 `kill -3`、`jcmd Thread.print`、`docker stats` 等无需重启的手段。

---

## 1. 信息基线（5 分钟内必须拿到）

> 建议在“最卡”的时间窗口采集。

### 1.1 宿主机层

- `uptime`（load 是否异常）
- `free -h`（Swap 是否在使用）
- `vmstat 1 10`（`r` 运行队列、`wa` IO wait、`si/so` swap in/out）

> 若有 `iostat`：`iostat -x 1 10`（磁盘是否高 await）

### 1.2 容器层

- `docker ps`（确认核心 Java 服务容器名）
- `docker stats --no-stream`（哪几个容器 CPU/内存飙高）
- `docker inspect <container> | grep -i -E "Memory|Cpu"`（是否设置了 limit）

### 1.3 JVM 层

- Java 进程 PID：`docker exec -it <container> sh -lc "ps -ef | grep java"`
- JVM 实际参数：优先 `jcmd <pid> VM.flags`（无 `jcmd` 时先记录启动命令行）

---

## 2. 第一优先：线程栈（虚拟堆栈）抓取

> 目标：回答“卡住时线程都在做什么”。这是定位瓶颈最有效的证据。

### 2.1 最低侵入：`kill -3` 打印线程栈到日志（推荐）

#### 2.1.1 依赖链检查（系统自带优先）

- `command -v docker`
- `command -v ps`
- `command -v kill`

> 若缺失：先安装对应工具再进行（不要跳过检查）。

#### 2.1.2 操作步骤

1. 找 PID

```bash
docker exec -it <container> sh -lc "ps -ef | grep java"
```

2. 连续抓 3 次线程栈（间隔 5~10 秒）

```bash
docker exec -it <container> sh -lc "kill -3 <pid>; sleep 5; kill -3 <pid>; sleep 5; kill -3 <pid>"
```

3. 查看日志中的线程栈

```bash
docker logs <container> --since 10m | grep -n "Full thread dump" -n
# 或直接 docker logs <container> --since 10m
```

#### 2.1.3 如何解读（速判）

- 大量线程处于 `BLOCKED`：
  - 典型：锁竞争、synchronized/Lock 争用
- 大量线程处于 `WAITING/TIMED_WAITING` 且栈顶是 `java.util.concurrent`：
  - 典型：线程池/队列等待、资源不足或下游慢
- 大量线程卡在 JDBC/HTTP 客户端：
  - 典型：DB 慢查询、连接池耗尽、下游服务慢
- 出现 `Found one Java-level deadlock`：
  - 明确死锁

> 建议把 3 次 dump 中“重复出现最多的线程栈段落”提取出来对比。

### 2.2 更可控：`jcmd Thread.print -l`（若容器带 JDK 工具）

#### 2.2.1 依赖链检查

- `command -v jcmd`

#### 2.2.2 操作

```bash
docker exec -it <container> sh -lc "jcmd <pid> Thread.print -l > /tmp/thread.txt && tail -n 80 /tmp/thread.txt"
```

---

## 3. 第二优先：判断是否 GC 暂停（STW）导致“卡几秒”

> 典型现象：页面/接口“每隔几秒卡一下”，CPU 未必打满。

### 3.1 无重启快速拿 JVM 堆/GC 概况（jcmd）

- `jcmd <pid> GC.heap_info`
- `jcmd <pid> VM.flags`
- `jcmd <pid> GC.class_histogram`（怀疑内存泄漏/类暴涨时）

### 3.2 若可用 `jstat`（JDK8 常见）

依赖检查：`command -v jstat`

```bash
# 观察 60 秒的 GC 趋势
jstat -gcutil <pid> 1000 60
```

### 3.3 需要重启才能做的“长期证据”（可后续推进）

- 打开 GC 日志（不同 JDK 参数不同）
- 打开 JFR（JDK11+ 可用），用于定位 STW、热点方法、锁竞争

---

## 4. 第三优先：热点方法与慢调用定位

> 当线程栈显示业务方法运行但看不出谁最耗时，建议采样分析。

### 4.1 Arthas（推荐，侵入相对低）

可用于：

- `dashboard`：整体线程/内存/GC
- `thread -n 20`：最忙线程
- `trace`：方法调用耗时
- `watch`：观察入参/返回值

> 是否能用取决于容器网络/权限/是否允许 attach；后续按环境单独制定接入方式。

### 4.2 async-profiler（更强但更谨慎）

适合 CPU 高、需要火焰图时使用；容器权限/内核支持是关键前置条件。

---

## 5. “不是 JVM 但很像 JVM”的常见根因（必须排除）

### 5.1 Swap 抖动

- `free -h`：Swap used > 0
- `vmstat 1 10`：`si/so` 持续非 0

### 5.2 磁盘 IO 等待

- `vmstat`：`wa` 高
- `iostat -x`：`await` 高、`util` 接近 100%

### 5.3 连接池耗尽（DB/HTTP/线程池）

- 线程栈大量卡在获取连接/等待队列
- 日志出现连接池超时

---

## 6. 最小闭环（10 分钟可跑完）

1. `docker stats --no-stream`（定位最可疑容器）
2. 对最可疑 Java 容器 `kill -3` 连续抓 3 次
3. 同时采集 `uptime`、`free -h`、`vmstat 1 10`
4. 基于证据选择后续：
   - 线程阻塞/死锁/池耗尽 → 优先线程栈 + 下游定位
   - STW/GC 频繁 → 优先 GC/JFR/堆参数
   - Swap/IO → 优先系统与容器资源治理

---

## 7. 与项目现有文档的关系

项目已有排查片段（来源：`docs/部署运维/CI-CD/6.微服务部署策略与最佳实践.md`）：

- `docker stats`
- `docker exec <service> jmap -heap <pid>`
- `docker exec <service> jstack <pid>`

本文在此基础上补齐：

- 更推荐的 `kill -3`（无需 jstack 工具也能抓线程栈）
- `jcmd Thread.print/GC.heap_info/VM.flags`
- Swap/IO/cgroup 等“非 JVM 但导致卡顿”的系统性排查

---

## 8. 输出物（后续给研发/我继续协助时你需要提供什么）

- 3 次线程栈（或从中提取重复出现最多的 3~5 段）
- `docker stats --no-stream` 输出
- `uptime`、`free -h`、`vmstat 1 10` 输出
- 目标容器的资源 limit（docker inspect 结果摘要）
- JVM 参数（`jcmd VM.flags` 或启动命令行）
