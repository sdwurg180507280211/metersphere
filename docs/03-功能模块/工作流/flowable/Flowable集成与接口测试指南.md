# Flowable 集成与接口测试指南（workflow-service）

我在写这份文档；目的是把 **Flowable 在 MeterSphere 中以独立微服务（workflow-service）方式集成**、以及 **Swagger 接口测试（含新手入参/出参解释）** 沉淀成可复用的步骤；如果不这样做,就无法实现“别人照着做也能跑起来”的可复制性。

---

## 1. 目标与整体架构（你现在落地的方案）

我在说明整体方案；目的是先让你建立“为什么要这样拆、各模块如何协作”的全局认知；如果不这样做,就无法实现后续对 Maven、网关、鉴权、数据库隔离等细节的正确理解。

- **目标**：在 MeterSphere 中引入 Flowable 7.x（BPMN 引擎），以独立微服务 `workflow-service` 承载引擎与流程相关 API。
- **边界**：
  - `workflow-service`：提供流程部署、启动、任务查询、任务完成等能力（PoC）。
  - `gateway`：统一入口（路由 + Swagger 聚合）。
  - `eureka`：服务注册发现。
- **现状**：`workflow-service` 已能启动；为 PoC 阶段，临时排除了部分 MeterSphere 通用能力（见“启动排障记录”）。

---

## 2. Maven 多模块：workflow-service 如何耦合进本项目

我在解释 Maven 耦合方式；目的是让你清楚“一个微服务如何被纳入 metersphere 统一构建/统一依赖体系”；如果不这样做,就无法实现你后续新增微服务或拆分模块时的正确操作。

### 2.1 根工程 `metersphere/pom.xml`：把微服务作为一个模块加入

我在说明根 POM 的模块聚合；目的是解释为什么 `mvn -pl ...`、`mvn install` 能同时构建多个服务；如果不这样做,就无法实现“在同一个仓库里统一构建/统一版本”的效果。

根 POM 关键点：

- `packaging` 为 `pom`
- `<modules>` 中显式加入：
  - `<module>workflow-service</module>`

这代表：

- `workflow-service` 与 `framework/test-track/...` 等其它模块**同级**参与整个项目的 Maven reactor 构建。
- 版本号统一使用根工程的 `${revision}`（例如 `2.10`）。

### 2.2 `workflow-service/pom.xml`：微服务自己的父聚合 POM

我在解释为什么要多一层 parent；目的是让你理解“微服务内还可以再拆 backend/frontend 等子模块”的标准做法；如果不这样做,就无法实现后续把 workflow 的前端单独拆出来的扩展能力。

当前结构：

- `workflow-service/pom.xml`
  - `packaging=pom`
  - `parent` 指向根工程：
    - `groupId=io.metersphere`
    - `artifactId=metersphere`
    - `version=${revision}`
  - `<modules>`：
    - `backend`

含义：

- `workflow-service` 继承根工程的依赖管理与统一版本（Spring Boot 版本、Java 版本等）。
- `workflow-service` 自己再聚合一个或多个子模块，目前只有 `backend`。

### 2.3 `workflow-service/backend/pom.xml`：真正的 Spring Boot 微服务模块

我在说明“真正跑起来的 jar 在哪里”；目的是让你把 Maven 模块与运行产物对应起来；如果不这样做,就无法实现你后续定位依赖/打包/启动入口。

关键内容：

- `artifactId=workflow-service`
- 依赖：
  - `io.metersphere:sdk:${revision}`
    - 复用 MeterSphere 的公共能力：
      - 配置加载（`commons.properties` 等）
      - Shiro（鉴权链）
      - 通用工具类
      - ResultHolder 等通用返回结构
  - `org.flowable:flowable-spring-boot-starter-process:${flowable.version}`
    - 引入 Flowable 7.x 的流程引擎 starter

- `spring-boot-maven-plugin`：
  - `mainClass=io.metersphere.WorkflowApplication`
  - 说明这个模块会被打成**可运行的 Spring Boot jar**。

### 2.4 “耦合点”总结（新手理解版）

我在总结耦合点；目的是给你一个“看 POM 就知道集成关系”的速查表；如果不这样做,就无法实现快速判断一个模块是“独立服务”还是“业务子模块”。

- **根工程聚合**：`metersphere/pom.xml` 把 `workflow-service` 加到 `<modules>`。
- **统一版本**：`workflow-service/pom.xml` 继承根工程 `${revision}`。
- **复用公共能力**：`backend/pom.xml` 引入 `io.metersphere:sdk`。
- **引入 Flowable 引擎**：`backend/pom.xml` 引入 `flowable-spring-boot-starter-process`。

---

## 3. 关键代码入口（你应该从哪看起）

我在列出入口文件；目的是你排查问题时知道“从启动类→配置→controller”的路径；如果不这样做,就无法实现高效定位。

- 启动类：`workflow-service/backend/src/main/java/io/metersphere/WorkflowApplication.java`
- PoC Controller：`workflow-service/backend/src/main/java/io/metersphere/workflow/controller/WorkflowPocController.java`
- Swagger/OpenAPI 配置：`workflow-service/backend/src/main/java/io/metersphere/workflow/config/WorkflowOpenApiConfig.java`（如你已创建/配置）

---

### 3.1 Flowable 工作流实现机制（截至目前 PoC 状态）

我在补充“实现机制”；目的是让你理解 PoC 现在到底是如何跑起来的、以及后续产品化应该往哪里扩展；如果不这样做,就无法实现你把 PoC 改成“可用能力”时的可控演进。

#### 3.1.1 分层与职责（从 HTTP 到引擎）

我在说明分层；目的是你一眼知道问题应该去哪一层排查；如果不这样做,就无法实现快速定位（比如到底是路由问题、鉴权问题、还是引擎问题）。

- **HTTP 层（Controller）**：
  - `WorkflowPocController` 负责把“部署/启动/查询/完成”这四类操作暴露为 REST API。
- **应用层（Service 调用 Flowable API）**：
  - PoC 阶段没有再封装一层业务 Service，而是直接在 Controller 中调用 Flowable 的 `RepositoryService`、`RuntimeService`、`TaskService`。
  - 长期建议：把 Controller 只做参数校验与 DTO 转换，引擎操作下沉到 `WorkflowAppService` 之类的应用服务，做到高内聚低耦合。
- **引擎层（Flowable Engine）**：
  - 由 `flowable-spring-boot-starter-process` 自动装配 `ProcessEngine` 以及各类 `xxxService` Bean。
  - 你的代码调用 `RuntimeService/TaskService`，最终会进入 Flowable 的 Command 执行链（含事务、表读写、历史记录等）。
- **持久化层（Flowable 表）**：
  - 引擎把流程定义、流程实例、任务、变量等落在 `ACT_` 前缀的表中（如 `ACT_RE_*`, `ACT_RU_*`, `ACT_HI_*`）。

#### 3.1.2 你现在这 4 个 PoC 接口分别做了什么（数据流视角）

我在按接口梳理引擎数据流；目的是你能把“接口行为”与“引擎写了哪些表/生成了哪些对象”对应起来；如果不这样做,就无法实现你后续补字段、加能力时不破坏引擎语义。

1. **部署流程 `POST /workflow/poc/deploy`**
   - 我在说明：Controller 接收 `multipart/form-data` 上传的 BPMN 文件；目的是把 BPMN 转成 Flowable 认可的部署资源；如果不这样做,就无法实现流程定义进入引擎。
   - 我在说明：通过 `RepositoryService` 创建 Deployment（并把 BPMN 作为资源加入）；目的是让引擎解析 BPMN 并生成流程定义；如果不这样做,就无法实现后续启动流程。
   - 我在说明：部署成功后会得到 `deploymentId`、`processDefinitionId`、`processDefinitionKey`；目的是后续启动流程用 `key` 或 `id` 精确定位定义；如果不这样做,就无法实现稳定启动。

2. **启动流程 `POST /workflow/poc/start`**
   - 我在说明：通过 `RuntimeService.startProcessInstanceByKey(...)` 启动一个流程实例；目的是生成运行中的实例（Runtime）；如果不这样做,就无法实现产生待办任务。
   - 我在说明：`businessKey` 被绑定到流程实例；目的是把业务域主键与工作流实例关联（后续你可以从业务表反查流程）；如果不这样做,就无法实现跨域追溯。
   - 我在说明：`variables` 会成为流程变量；目的是驱动 UserTask 的 assignee、网关条件、Listener 等；如果不这样做,就无法实现流程分支与自动化。

3. **查询任务 `GET /workflow/poc/tasks`**
   - 我在说明：通过 `TaskService.createTaskQuery()` 查询待办；目的是得到当前可操作的用户任务；如果不这样做,就无法实现“我的待办”。
   - 我在说明：PoC 仅按 `assignee` 过滤；目的是先验证最小闭环；如果不这样做,就无法实现后续你按候选人/候选组/业务键/流程实例等维度扩展查询。

4. **完成任务 `POST /workflow/poc/tasks/{taskId}/complete`**
   - 我在说明：通过 `TaskService.complete(taskId, variables)` 完成任务；目的是推进流程向后流转；如果不这样做,就无法实现流程闭环。
   - 我在说明：你可携带变量用于后续网关判断；目的是让“完成操作”同时推动下一步决策；如果不这样做,就无法实现条件流转。

#### 3.1.3 Flowable 相关 Bean 是怎么来的（自动装配机制）

我在解释自动装配；目的是你知道为什么你没有写 `@Bean` 也能注入 `RepositoryService/RuntimeService/TaskService`；如果不这样做,就无法实现你后续排查 Bean 冲突或替换默认实现。

- **来源**：`org.flowable:flowable-spring-boot-starter-process`
  - 我在说明：starter 会自动创建 `ProcessEngine`，并将 `RepositoryService`、`RuntimeService`、`TaskService` 等注册为 Spring Bean；目的是做到开箱即用；如果不这样做,就无法实现最小集成。
- **你踩过的坑（Bean 名冲突）**：
  - 我在说明：MeterSphere 自身也存在名为 `taskService` 的业务 Bean；目的是解释为什么 `@Resource` 会误注入；如果不这样做,就无法实现稳定启动。
  - 我在说明：PoC 里改成 `@Autowired` 按类型注入 `org.flowable.engine.TaskService`；目的是规避同名 Bean 冲突；如果不这样做,就无法实现正确注入。

#### 3.1.4 为什么 PoC 阶段要排除部分 MeterSphere 自动配置（隔离的本质）

我在解释“排除”的本质；目的是让你明白这不是绕路，而是为了让 workflow-service 真正可以独立演进；如果不这样做,就无法实现数据库隔离与服务自治。

- **排除 `RsaConfig`**
  - 我在说明：它会在启动阶段通过 `FileService.checkRsaKey()` 访问 `file_content` 表；目的是初始化 RSA 密钥；如果不这样做,就无法实现 MeterSphere 主系统的文件加密能力。
  - 我在说明：workflow-service 使用独立库时并没有这些通用表，所以会启动失败；目的是解释独立库缺表的真实原因；如果不这样做,就无法实现你后续“共享库/独立库”的策略判断。

- **排除 `PermissionConfig`（PoC 临时）**
  - 我在说明：sdk 的权限初始化会读取 classpath 的 `permission.json`；目的是把权限点注册到系统；如果不这样做,就无法实现权限体系的统一管控。
  - 我在说明：PoC 阶段还没提供 workflow-service 专属权限文件，所以先排除以保证服务可启动；目的是先跑通引擎闭环；如果不这样做,就无法实现 PoC 的最小可运行。

- **排除 `MinIOConfig`（PoC 临时）**
  - 我在说明：PoC 阶段流程部署采用“直接上传 BPMN 文件并部署”，不需要对象存储；目的是减少依赖面；如果不这样做,就无法实现最小启动。

#### 3.1.5 数据库：Flowable 表存什么、workflow-service 自己要存什么

我在说明数据库边界；目的是避免你把“业务表”与“引擎表”混在一起导致耦合失控；如果不这样做,就无法实现长期可维护。

- **Flowable 引擎表（必需）**：
  - 我在说明：引擎会维护 `ACT_` 系列表；目的是存储流程定义、运行时、历史、任务、变量等；如果不这样做,就无法实现引擎持久化。
- **workflow-service 的业务表（后续可选）**：
  - 我在说明：你未来很可能需要单独的“工作流域”业务表（例如流程模板元数据、与 MeterSphere 业务对象映射、扩展表单、审批意见等）；目的是承载非引擎原生的数据；如果不这样做,就无法实现产品化能力。

#### 3.1.6 网关与前端联调：为什么要有 `/workflow` 前缀

我在解释路由前缀；目的是让你把“前端调用路径 / 网关路由 / 服务端 controller path”统一起来；如果不这样做,就无法实现你从浏览器 Network 直接判断请求走向。

- **网关对外暴露前缀**：
  - 我在说明：目前你在 gateway 聚合 Swagger 中使用了 `/workflow/v3/api-docs`；目的是证明 gateway 侧已经认可 `/workflow` 作为 workflow-service 的前缀；如果不这样做,就无法实现统一入口。
- **服务端真实接口前缀**：
  - 我在说明：`WorkflowPocController` 的 BasePath 是 `/workflow/poc`；目的是让你知道最终 URL 由“网关前缀 + controller path”组合（或在直连时就是 controller path）；如果不这样做,就无法实现正确路由。
- **本地开发（system-setting 微前端 4001）联调机制**：
  - 我在说明：system-setting 使用统一 request 封装，在微前端环境可能带有 `baseURL=/setting`；目的是解释为什么相对路径可能被拼成 `/setting/...`；如果不这样做,就无法实现避免路径污染。
  - 我在说明：因此 PoC 前端调用采用 `window.location.origin + '/workflow/poc'` 形式构造绝对 URL，并在 `vue.config.js` 把 `/workflow/**` 代理到 `http://localhost:8008`；目的是让 4001 只负责前端页面，后端请求由代理转发到 workflow-service；如果不这样做,就无法实现消除 404/CORS 的联调阻碍。

---

## 4. 启动排障记录（你已踩过的坑：原因 + 解决方式）

我在把排障原因写清楚；目的是让你之后换环境或同事复现时能快速解决；如果不这样做,就无法实现稳定复现。

### 4.1 Bean 名称冲突：`taskService`（Flowable vs 项目自定义 TaskService）

我在解释冲突来源；目的是让你知道 `@Resource` 按名称注入的风险；如果不这样做,就无法实现你后续在 MeterSphere 里同时引入第三方同名 Bean 的安全用法。

- **现象**：启动时报 `Bean named 'taskService' is expected to be of type 'org.flowable.engine.TaskService' but was actually of type 'io.metersphere.task.service.TaskService'`。
- **原因**：
  - `@Resource` 默认按字段名注入，字段名是 `taskService`。
  - MeterSphere 自己也有一个名为 `taskService` 的 Bean。
- **解决**：在 `WorkflowPocController` 中将 Flowable 相关注入改为按类型注入（`@Autowired`）。

### 4.2 MinIO 自动配置依赖缺失

我在解释为什么会报 MinioProperties 缺失；目的是让你理解 sdk 复用带来的“隐式自动配置”；如果不这样做,就无法实现 microservice 最小启动。

- **现象**：`MinIOConfig` 需要 `MinioProperties` Bean，启动失败。
- **解决**：在 `WorkflowApplication` 中排除 `MinIOConfig`（PoC 阶段不需要对象存储）。

### 4.3 `file_content` 表不存在（独立库启动失败）

我在解释“为什么启动时会查 file_content”；目的是让你理解“ApplicationRunner 在启动阶段会访问数据库”；如果不这样做,就无法实现数据库隔离后的稳定启动。

- **现象**：`metersphere_workflow.file_content doesn't exist`
- **原因链路**：`RsaConfig (ApplicationRunner)` → `FileService.checkRsaKey()` → 查询 `file_content`
- **解决（PoC 推荐）**：在 `WorkflowApplication` 中排除 `RsaConfig`。

### 4.4 `PermissionConfig` 读取 `permission.json` NPE

我在解释权限文件为何导致 NPE；目的是让你知道“sdk 的权限初始化依赖资源文件”；如果不这样做,就无法实现后续权限体系接入。

- **现象**：`PermissionConfig.run` 中 `PermissionConfig.class.getResourceAsStream("/permission.json")` 返回 null，`IOUtils.toString` NPE。
- **解决（PoC 推荐）**：在 `WorkflowApplication` 中排除 `PermissionConfig`。
- **长期方案**：为 workflow-service 增加自己的 `permission.json` 并接入权限体系。

---

## 5. Swagger 测试：为什么你请求 `POST /workflow/poc/start` 会出现 302

我在解释 302 的根因；目的是让你知道“不是接口没写对，而是被鉴权拦截”；如果不这样做,就无法实现你用 Swagger 测试 PoC 接口。

### 5.1 302 常见原因（MeterSphere 场景）

- 通过 **gateway** 访问时，网关有 `LoginFilter`：
  - 未携带 session（或未携带 API Key）会返回 **401/重定向相关行为**（浏览器里常表现为 302/跳转）。
- 在服务端（sdk）侧，Shiro 默认链：
  - `/** = apikey, csrf, authc`
  - 如果你没登录/没带 API Key，就会被拦截（可能跳 `/` 或 `/signin`）。

### 5.2 PoC 阶段怎么测最省事

我在给出最省事的测试方式；目的是减少你在鉴权/CSRF 上的学习成本，先验证 Flowable 引擎能力；如果不这样做,就无法实现快速闭环。

- **推荐**：先直连 workflow-service 的端口访问 Swagger + PoC 接口（不要经网关）。
- 如果必须走网关：
  - 要么先在系统里登录形成 session
  - 要么按 MeterSphere 的 `API_ACCESS_KEY` / `API_SIGNATURE` 方式构造调用（PoC 阶段不推荐，学习成本高）。

---

## 6. Swagger / OpenAPI：如何用 Swagger 进行 PoC 接口测试（新手版）

我在写“点哪里、填什么、看什么返回”；目的是你不懂 curl 也能按步骤测通；如果不这样做,就无法实现新手可操作。

### 6.1 你的 PoC 接口列表（以代码为准）

Controller：`WorkflowPocController`

- BasePath：`/workflow/poc`

接口：

1. `POST /workflow/poc/deploy`
2. `POST /workflow/poc/start`
3. `GET /workflow/poc/tasks`
4. `POST /workflow/poc/tasks/{taskId}/complete`

### 6.2 Swagger 页面怎么打开

我在说明 Swagger 入口；目的是你能进入测试界面；如果不这样做,就无法实现后续操作。

常见入口（二选一）：

- 直连服务：`http://localhost:<workflow端口>/swagger-ui/index.html`
- 网关聚合：`http://localhost:<gateway端口>/swagger-ui/index.html`（然后在下拉里选择 workflow-service）

> 如果你看到 404：优先确认 springdoc 的路径配置；再确认是否走网关导致被过滤器拦截。

---

## 7. PoC 接口：入参/出参详解（含可复制示例）

我在把每个接口的“入参怎么填、返回怎么看”讲清楚；目的是新手可直接复制粘贴；如果不这样做,就无法实现你独立完成接口验证。

### 7.1 部署流程：`POST /workflow/poc/deploy`

我在解释 multipart 上传；目的是你知道 Swagger 里 `file` 怎么选；如果不这样做,就无法实现部署成功。

**用途**：上传 BPMN 文件并部署为 Flowable 流程定义。

**请求（Swagger 操作）**：

- 点击 `Try it out`
- 选择参数：
  - `file`（必填）：BPMN 文件（通常是 `*.bpmn20.xml`）
  - `name`（可选）：部署名
- 点击 `Execute`

**成功响应（示例）**：

```json
{
  "success": true,
  "data": {
    "deploymentId": "2501",
    "processDefinitionId": "demo:1:2504",
    "processDefinitionKey": "demo",
    "processDefinitionName": "Demo Process",
    "processDefinitionVersion": 1
  }
}
```

你需要记住：

- `processDefinitionKey`：后续启动流程要用

### 7.2 启动流程：`POST /workflow/poc/start`

我在解释 JSON Body；目的是你知道字段含义；如果不这样做,就无法实现启动成功。

**用途**：根据 `processDefinitionKey` 启动一个流程实例。

**请求 Body（示例，可直接粘贴）**：

```json
{
  "processDefinitionKey": "demo",
  "businessKey": "BK-001",
  "variables": {
    "assignee": "zhangsan",
    "amount": 100
  }
}
```

字段说明：

- `processDefinitionKey`（必填）
  - 对应 BPMN 里 `<process id="...">` 的 `id`
  - 也就是 deploy 返回的 `processDefinitionKey`
- `businessKey`（可选）
  - 业务侧的关联主键（例如：缺陷ID、工单ID、需求ID）
  - Flowable 会把它绑定到流程实例上，方便你从业务追溯流程
- `variables`（可选）
  - 流程变量 Map（key/value）
  - 常用于给 UserTask 的 assignee、网关条件等提供值

**成功响应（示例）**：

```json
{
  "success": true,
  "data": {
    "processInstanceId": "5001",
    "processDefinitionId": "demo:1:2504",
    "businessKey": "BK-001"
  }
}
```

你需要记住：

- `processInstanceId`：用于排查流程实例

### 7.3 查询任务：`GET /workflow/poc/tasks`

我在解释 query 参数；目的是你知道 `assignee` 是干什么的；如果不这样做,就无法实现正确查询。

**用途**：查询当前待办任务列表。

**请求参数**：

- `assignee`（可选）：按任务处理人过滤

**响应（示例）**：

```json
{
  "success": true,
  "data": [
    {
      "id": "7501",
      "name": "经理审批",
      "assignee": "zhangsan",
      "processInstanceId": "5001",
      "createTime": "2025-12-18T18:00:00.000+08:00"
    }
  ]
}
```

你需要记住：

- `id`（taskId）：完成任务要用

### 7.4 完成任务：`POST /workflow/poc/tasks/{taskId}/complete`

我在解释 path 参数和可选变量；目的是你能顺利推进流程；如果不这样做,就无法实现流程闭环。

**用途**：完成某个待办任务，并可携带变量推进网关条件等。

**请求路径参数**：

- `taskId`：来自 `/tasks` 返回的 `id`

**请求 Body（可选）**：

```json
{
  "variables": {
    "approved": true,
    "comment": "ok"
  }
}
```

**成功响应（示例）**：

```json
{
  "success": true,
  "data": true
}
```

---

## 8. 最小闭环验证清单（你测通就算 PoC 过关）

我在给出验收清单；目的是你知道“测到哪一步算结束”；如果不这样做,就无法实现明确的完成标准。

- 能在 Swagger 里看到 `workflow/poc` 这组接口
- `deploy` 成功返回 `processDefinitionKey`
- `start` 成功返回 `processInstanceId`
- `tasks` 能查到任务（或确认流程无 UserTask）
- `complete` 返回 `true`

---

## 9. 下一阶段建议（可选）

我在给出后续演进方向；目的是你把 PoC 推向可用产品；如果不这样做,就无法实现与主系统协作。

- **权限体系接入**：恢复 `PermissionConfig`，并提供 workflow-service 专属 `permission.json`
- **鉴权与网关联调**：解决 302/401，支持从网关正常访问 PoC 接口
- **前端模块接入**：新增“我的待办/流程管理”等页面入口
- **数据库隔离策略**：明确 workflow 库只存工作流域数据，跨域信息通过 API/事件集成

---

## 状态总结

我在总结当前文档覆盖范围；目的是对齐“已记录哪些内容、还缺什么”；如果不这样做,就无法实现持续补全。

- 已覆盖：Maven 耦合方式、启动排障关键点、Swagger PoC 测试步骤、每个接口入参/出参（新手版）。
- 待补充（如果你需要）：
  - 你实际使用的 BPMN 示例文件（可附在文档里作为一键测试样例）
  - 通过网关访问时如何带认证头（API Key/Session）的具体方法
