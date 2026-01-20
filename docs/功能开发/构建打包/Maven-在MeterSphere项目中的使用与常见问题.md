# Maven 在 MeterSphere 项目中的使用与常见问题（结合本项目）

> 本文目标：把我们在聊天里关于 Maven 的讨论，结合本仓库真实配置（`scripts/metersphere-build.sh`、`test-track/frontend/pom.xml`、`test-track/backend/pom.xml` 等）整理成一份可复用的说明文档。

---

## 1. 这个项目里 Maven 负责什么？

我在做：先明确 Maven 的职责边界。
目的是：你能区分 Maven / 前端构建 / Docker 镜像构建各自解决的问题。
如果不这样做,就无法判断某个问题应该用哪个工具去解决。

- Maven 主要负责：
  - Java 编译、测试、打包
  - 依赖解析与多模块依赖编排
  - 将产物安装到本地仓库（供其他模块引用）
  - （在本项目中）通过插件驱动前端构建，并把前端产物拷贝进后端资源目录
- Docker 主要负责：
  - 将 Maven 产物（jar + 依赖）组装进镜像，形成可部署的运行单元
- qiankun 微前端主要负责：
  - 运行时子应用如何被加载/挂载（不直接决定 Maven 要跑哪些阶段）

---

## 2. Maven 生命周期：`clean / compile / package / install` 到底是什么关系？

我在做：用“流水线阶段”的方式解释 Maven 常用命令。
目的是：你看到命令就知道它做了哪些事。
如果不这样做,就会把 `install` 理解成“只有给别人用才需要”。

### 2.1 `mvn clean`

- 含义：删除上次构建产物，主要是当前模块的 `target/` 目录
- 本项目补充：
  - `test-track/frontend` 还配置了 `maven-clean-plugin`，会清理 `dist/`
  - `test-track/backend` 也配置了 `maven-clean-plugin`，会清理 `src/main/resources/static` 与 `public`（这些目录包含从前端拷贝进来的文件）

### 2.2 `mvn compile`

- 含义：编译 Java 主代码，输出到 `target/classes`
- 重要：
  - **不等于**生成可运行 jar
  - **不保证**你们的前端构建/资源拷贝链路会执行（取决于插件绑定的 phase）

### 2.3 `mvn package`

- 含义：把当前模块打包成 jar/war（输出在 `target/` 下）
- 对 Spring Boot：
  - 若配置了 `spring-boot-maven-plugin`，通常会生成可运行的 boot jar（或者对 jar 做 repackage）

### 2.4 `mvn install`

- 含义：在 `package` 的基础上，把产物安装到本地仓库 `~/.m2/repository`
- 关键理解：
  - **即使没有其他模块依赖它，你也可以 install。**
  - 在多模块工程中，install 的价值是：保证后续构建/IDE 运行时依赖解析一致（不引用旧 jar）。

---

## 3. 常见 Maven 参数（结合本项目脚本）

我在做：把脚本里出现的参数逐个解释。
目的是：你以后能自己推导“什么时候能跳过初始化、什么时候必须全量安装依赖”。
如果不这样做,就只能死记脚本命令。

### 3.1 `-DskipTests`

- 含义：跳过测试执行（但测试编译是否跳过取决于项目配置）
- 脚本使用位置：`mvn clean install -DskipTests`
- 目的：显著加速构建

### 3.2 `-N`

- 含义：`--non-recursive`，只执行当前 pom，不下钻到子模块
- 脚本使用：`mvn install -N`
- 目的：先把父 POM 安装到本地仓库，避免子模块解析 parent 失败

### 3.3 `-pl`

- 含义：`--projects`，只构建指定模块（Project List）
- 脚本初始化使用：
  - `mvn clean install -pl framework,framework/sdk-parent,...`

### 3.4 `-am`

- 含义：`--also-make`，当你构建某模块时，把它依赖的模块也一起构建
- 典型用法示例：
  - `mvn clean install -pl test-track/backend -am`

---

## 4. `scripts/metersphere-build.sh` 的“初始化”到底是什么？为什么可以跳过？

我在做：把脚本的“初始化”明确为实际 Maven 命令。
目的是：解释 `-s/--skip-init` 的真实含义。
如果不这样做,就会误以为“跳过初始化=跳过前端构建/跳过模块构建”。

### 4.1 初始化做了什么（脚本事实）

`init_dependencies()` 会执行：

1. `mvn install -N`
2. `mvn clean install -pl framework,framework/sdk-parent,framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter`

### 4.2 为什么后续构建可以跳过初始化

- 因为上述核心依赖一旦 `install` 到本地仓库，后续构建业务模块（如 `test-track`）时，依赖可以直接从 `~/.m2` 解析，无需重复安装。

### 4.3 什么时候不能跳过初始化

- 你改了 framework / sdk 相关模块
- 你清空过 `~/.m2/repository`（比如 `make clean-all`）
- 你切换分支导致核心依赖发生变化（本地仓库可能仍是旧版本）

---

## 5. Maven 如何打包 `test-track` 的前端（本项目真实链路）

我在做：基于 `test-track/frontend/pom.xml` 与 `test-track/backend/pom.xml` 解释前端打包链路。
目的是：回答“改了前端，为什么后端也要重新打包/至少重新执行资源拷贝”。
如果不这样做,就无法解释 IDEA 点 Run 时前端为什么不更新。

### 5.1 `test-track` 是父聚合模块

文件：`test-track/pom.xml`

- `packaging=pom`
- modules：
  - `frontend`
  - `backend`

### 5.2 前端模块：`test-track/frontend/pom.xml`

通过 `frontend-maven-plugin` 完成：

1. `install-node-and-npm`（phase：`generate-resources`）
   - node/npm 安装目录：`../../.node`
2. `npm install`
3. `npm run build`
   - 产物输出：`test-track/frontend/dist`

并且通过 `maven-clean-plugin` 在 clean 时清理 `dist/`。

### 5.3 后端模块：`test-track/backend/pom.xml`

通过 `maven-antrun-plugin` 在 `generate-resources` 阶段执行文件拷贝：

- `../frontend/dist` 中的 **非 html** 文件拷贝到：
  - `backend/src/main/resources/static`
- `../frontend/dist` 中的 **html** 文件拷贝到：
  - `backend/src/main/resources/public`

随后 Spring Boot 在运行时会从 classpath 下的 `static/public` 提供静态资源。

> 这就是“改了前端后，后端也得重新打包一下”的本质原因：
> - 你改的是前端源码
> - 实际被后端服务提供的是 `static/public` 中的拷贝结果（来自 dist）
> - 不触发前端 build + 后端 copy，页面不会更新

---

## 6. IDEA 点击 Run（不使用前端 dev server）时，前端改动怎么生效？

我在做：把你最关心的运行方式明确下来：IDEA Run = 后端提供静态资源。
目的是：你知道该触发哪条构建链路。
如果不这样做,就会误以为 `mvn compile` 足够。

### 6.1 关键结论

- **`mvn compile` 只编译 Java，不保证前端 dist 更新、更不保证 dist 被拷贝进后端资源目录。**
- 要让 IDEA Run 的页面看到前端改动，至少要满足：
  1. `test-track/frontend/dist` 是最新（发生过 `npm run build`）
  2. `test-track/backend/src/main/resources/static|public` 拷贝了最新 dist

### 6.2 最稳的触发方式（原则）

- 触发前端模块到 `generate-resources`（前端 build 在这里绑定）
- 触发后端模块到 `generate-resources`（后端 copy 在这里绑定）

在 Maven 里，`package/install` 通常会包含这些阶段，因此“重新打包一次test-track模块”最稳。
最稳（推荐）：从 test-track 父模块触发到 package 或 install，
等价于“一次性保证 frontend build + backend copy + 后端 jar 更新”
---

## 7. 常见误区纠正（结合本项目）

我在做：把聊天中出现的典型误区集中纠正。
目的是：减少团队内部沟通成本。
如果不这样做,就会反复出现同类问题。

### 7.1 “只要有 class 文件就能跑 Spring Boot”

- 部分成立：如果你用 IDE 或 `spring-boot:run`，确实可以直接用 classpath（`target/classes`）启动。
- 但在“镜像/部署”或 `java -jar` 场景，必须有可运行 jar。
- 在本项目的前端打包链路里，仅 class 文件不解决“静态资源更新”问题。

### 7.2 “微前端 qiankun 所以改前端必须前后端都 clean package”

- qiankun 是运行时加载机制，不直接决定 Maven 阶段。
- 本项目的关键是：前端产物通过 Maven 插件拷贝进后端资源目录后再被 Spring Boot 提供。

### 7.3 “install 只有被别人依赖时才需要”

- 不对。install 的核心意义是把产物写入本地仓库，保证依赖解析一致。
- 你们脚本统一用 `clean install` 是合理的工程化策略。

---

## 8. 参考文件

- `scripts/metersphere-build.sh`
- `scripts/Makefile`
- `test-track/pom.xml`
- `test-track/frontend/pom.xml`
- `test-track/backend/pom.xml`

