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

### 4.4 `metersphere-build.sh` 选项（参数）到底对应执行了什么？（速查）

我在做：把 `metersphere-build.sh -h` 看到的参数，映射到脚本内部真正调用的函数/命令。
目的是：你看到一条脚本命令，就能反推出它底层大概会跑哪些 mvn/docker/jar 解压动作。
如果不这样做,就无法实现：脚本对你来说是黑盒，一旦构建慢/失败/镜像不更新，你会不知道应该看 Maven 日志还是 Docker 日志。

#### 4.4.1 参数 → 脚本行为（最小理解版）

- `-a/--all`
  - 含义：构建所有模块（脚本会按模块列表循环执行 `build_module`）
- `-l/--list`
  - 含义：仅列出模块名与目录映射（不构建，不跑 Maven/Docker）
- `-v/--version <VERSION>`
  - 含义：设置 `IMAGE_VERSION`（影响镜像 tag，以及 `docker build --build-arg MS_VERSION`）
- `--registry <URL>`
  - 含义：设置 `REGISTRY`（影响镜像仓库前缀，如 `${REGISTRY}/${module}:${IMAGE_VERSION}`）
- `-s/--skip-init`
  - 含义：跳过 `init_dependencies()`（也就不会执行第 4.1 节的两条 Maven 初始化命令）
- `-p/--parallel` + `-j/--jobs <N>`
  - 含义：并行构建模块（本质是并发跑多个 `build_module`，日志会落在 `/tmp/build_<module>.log`）
- `-b/--build-only`
  - 含义：只构建镜像，不导出 tar（跳过 `export_images()`）
- `-o/--output <PATH>`
  - 含义：设置 `PACKAGE_PATH`（决定 `docker save ... > $PACKAGE_PATH` 导出 tar 的路径）

#### 4.4.2 你真正需要记住的 3 个“底层动作”

我在做：把脚本里最关键的 3 个阶段（初始化/构建/导出）用最少的命令标识出来。
目的是：你能快速定位“问题发生在哪个阶段”。
如果不这样做,就无法实现：你会把所有问题都归因于某一个工具（比如一味怀疑 Maven 或一味怀疑 Docker）。

- 初始化阶段：`init_dependencies()`
  - 执行（脚本事实）：
    - `mvn install -N`
    - `mvn clean install -pl framework,framework/sdk-parent,framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter`
- 模块构建阶段：`build_module <module>`（每个模块都会跑一遍）
  - 执行（脚本事实）：
    - `mvn clean install -DskipTests`
    - `jar -xf ../*.jar` 解压到 `target/dependency`（简单模块）或 `backend/target/dependency`（复杂模块）
    - `docker build --build-arg MS_VERSION=${IMAGE_VERSION} -t ${REGISTRY}/${module}:${IMAGE_VERSION} ...`
- 镜像导出阶段：`export_images()`（可选）
  - 触发条件：`BUILD_ONLY != true`
  - 执行（脚本事实）：`docker save "${BUILT_IMAGES[@]}" > $PACKAGE_PATH`

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
- `scripts/BUILD_GUIDE.md`
- `test-track/pom.xml`
- `test-track/frontend/pom.xml`
- `test-track/backend/pom.xml`

---

## 9. Docker 在 MeterSphere 项目中的使用与常见问题（结合本项目）

我在做：用和讲 Maven 同样的方式来讲 Docker：先定职责边界，再映射到本项目脚本/产物/命令。
目的是：你知道“什么时候该动 Maven，什么时候该动 Docker”，以及“动了 Docker 代表什么”。
如果不这样做,就无法解释为什么有时你 Maven 已经成功了，但部署侧仍然是旧版本（因为镜像没有更新/没有重新加载）。

### 9.1 这个项目里 Docker 负责什么？

我在做：把 Docker 的职责边界讲清楚，并和 Maven 做对照。
目的是：你能快速判断一个问题属于“构建产物问题”还是“部署载体问题”。
如果不这样做,就无法实现：你会在 Maven / Docker 之间来回试命令，既慢又不稳定。

- Maven 在本项目负责：
  - 编译/打包各模块 jar
  - （对 `test-track`）驱动前端 build（生成 `dist`）并拷贝进后端资源目录
- Docker 在本项目负责：
  - 把“已经构建好的 jar + 运行所需文件结构”打进镜像
  - 给运行时一个稳定的、可搬运的交付物（镜像、或镜像 tar）
- 一句话对照：
  - **Maven 解决“产物怎么生成”**
  - **Docker 解决“产物怎么交付/怎么跑”**

### 9.2 本项目的 Docker 构建入口在哪里？（脚本事实）

我在做：把 Docker 构建入口指向仓库里的权威实现，而不是泛讲 docker build。
目的是：你后续看脚本就能自解释：镜像怎么命名、版本号怎么传、输出 tar 在哪里。
如果不这样做,就无法实现：你会用通用 docker 命令绕开脚本，导致版本命名、导出方式、依赖解压目录不一致。

- 主要入口：
  - `scripts/metersphere-build.sh`
  - `scripts/Makefile`
  - `scripts/BUILD_GUIDE.md`
- 关键变量（来自 `scripts/metersphere-build.sh`）：
  - `REGISTRY`：镜像仓库前缀（默认：`registry.fit2cloud.com/north`）
  - `IMAGE_VERSION`：镜像 tag（默认：`v2.10.23.02-lts`）
  - `BUILD_ONLY`：只构建镜像不导出 tar（开发/本地验证常用）
  - `PACKAGE_PATH`：导出的镜像 tar 路径

### 9.3 “构建镜像”在本项目里到底做了哪些事？

我在做：把脚本中的“镜像构建”拆成可理解的流水线阶段。
目的是：你能像理解 Maven lifecycle 一样理解镜像构建流程。
如果不这样做,就无法实现：你只看到“make dev/./metersphere-build.sh”成功了，但不知道它产出了什么、放在哪里、为什么容器还是旧的。

以 `scripts/metersphere-build.sh` 的模块构建（`build_module`）为主线，典型流水线可以理解为：

1. Maven 阶段（生成 jar）
   - 脚本对模块执行：`mvn clean install -DskipTests`
   - 输出：各模块的 jar（位于各自模块的 `target/` 或 `backend/target/`）
2. 依赖解压阶段（准备镜像上下文）
   - 脚本会把 jar 解压到 `target/dependency`（简单模块）或 `backend/target/dependency`（复杂模块）
   - 目的：让 Docker 构建时可以用“分层”的方式组织镜像内容（减少重复拷贝/加速缓存命中）
3. Docker build 阶段（生成镜像）
   - 镜像命名规则（脚本拼接）：`${REGISTRY}/${module_name}:${IMAGE_VERSION}`
4.（可选）docker save 阶段（导出镜像 tar）
   - 当你不是 `BUILD_ONLY=true` 时，脚本会把构建的镜像导出成一个 tar（用于离线交付/上机部署）

> 你可以把它类比为：
> - Maven `package/install` = “生成可运行产物”
> - Docker build = “把产物封装成可部署单元”
> - docker save = “把部署单元导出成可搬运文件”

### 9.4 常见场景：我改了代码，到底应该执行什么？

我在做：给出“场景 → 你需要更新哪一层 → 推荐命令”的映射。
目的是：你能像使用 Maven 速查一样使用 Docker 速查。
如果不这样做,就无法实现：你会把所有情况都当成“全量重打镜像”，构建时间被拉爆。

#### 9.4.1 只在 IDEA Run 验证（不跑容器）

- 你要更新的是：后端 classpath/static 资源
- 你不需要更新的是：Docker 镜像
- 参考本文第 6 节（IDEA Run 前端改动生效）

#### 9.4.2 本地用 Docker 跑起来验证（镜像必须更新）

- 你要更新的是：Docker 镜像（因为容器里装的是镜像中的 jar/资源）
- 推荐方式：走脚本（保持与团队一致）
  - `make dev`：开发模式（跳过初始化，只构建镜像，不导出 tar）
  - 或 `./scripts/metersphere-build.sh -a -s -b`（等价于上面的 make dev）

#### 9.4.3 交付到服务器/离线部署（需要 tar）

- 你要更新的是：镜像 + 镜像 tar
- 推荐方式：
  - `./scripts/metersphere-build.sh -a`（构建并导出 tar）
  - 服务器上：`docker load < metersphere-xxxx.tar`

### 9.5 常见命令对照（结合本项目脚本）

我在做：把“你手上可能会用到的 docker 命令”对照到脚本行为。
目的是：你能分清哪些操作是“构建侧”，哪些是“运行侧”。
如果不这样做,就无法实现：你会在运行侧反复重启容器，实际却需要在构建侧重建镜像。

- `docker images`：查看本地镜像是否已更新 tag
  - `make show-images` 是对它的封装（并做了仓库过滤）
- `docker save`：导出镜像
  - `make export` 会导出 `registry.fit2cloud.com/north` 下的镜像到 Desktop tar
- `docker load`：加载镜像 tar
  - `make load FILE=/path/to/file.tar` 是对它的封装
- `docker system prune -f`：清理 Docker 缓存
  - `make clean-docker` 会执行它（注意会删除未使用的资源）

### 9.6 常见误区纠正（Docker 版）

我在做：把 Docker 场景里最容易踩的坑集中纠正。
目的是：避免你把“容器运行问题”误判成“代码未生效”。
如果不这样做,就无法实现：你会重复构建/重复重启，但根因其实是镜像未更新或 tag 复用。

#### 9.6.1 “我 Maven 已经 BUILD SUCCESS，为什么容器里还是旧的？”

- 原因：
  - Maven 只更新了 jar（本机文件系统），**不会自动更新已经存在的镜像**
  - 容器运行的是镜像里的内容，不会读取你宿主机最新的 `target/`
- 正确做法：
  - 重新走脚本构建镜像（例如 `make dev` 或构建指定模块）

#### 9.6.2 “我 docker restart 了，为什么还是旧的？”

- 原因：restart 只是重启容器，**不会替换镜像**
- 正确做法：
  - 先确保镜像 tag 对应的镜像已更新（`docker images` 看创建时间/ID）
  - 必要时删除旧容器并基于新镜像重新启动

#### 9.6.3 “我每次都用同一个 tag（比如 latest），就不用管版本了”

- 风险：
  - tag 复用会导致你难以确认运行的到底是哪次构建
  - 在多人协作/多环境时会放大排查成本
- 本项目建议：
  - 使用脚本的 `IMAGE_VERSION`（对应 tag）来做一次构建一次版本的可追溯性

