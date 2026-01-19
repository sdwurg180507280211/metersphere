# MeterSphere 本地开发快速命令速查表

> 目标：修改代码后，用最少的命令快速看到效果

## 模块依赖关系图

```
┌─────────────────────────────────────────────────────────────┐
│                      framework/sdk-parent                    │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌────────────────────┐ │
│  │ domain  │ │   sdk   │ │ jmeter  │ │  xpack-interface   │ │
│  └────┬────┘ └────┬────┘ └────┬────┘ └─────────┬──────────┘ │
│       │           │           │                │            │
└───────┼───────────┼───────────┼────────────────┼────────────┘
        │           │           │                │
        └───────────┴───────────┴────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  test-track  │   │   api-test   │   │ system-setting│  ... 其他业务模块
└──────────────┘   └──────────────┘   └──────────────┘
```

**关键点**：所有业务模块都依赖 `framework/sdk-parent` 下的模块

---

## 场景一：只修改了某个业务模块的代码

**适用**：只改了 test-track、api-test、system-setting 等业务模块的代码

```bash
# 进入模块目录，编译后端
cd test-track && mvn clean install -DskipTests

# 或者从项目根目录
mvn clean install -DskipTests -pl test-track/backend
```

**各模块快捷命令**：
```bash
# test-track（测试跟踪）
cd test-track && mvn clean install -DskipTests

# api-test（接口测试）
cd api-test && mvn clean install -DskipTests

# performance-test（性能测试）
cd performance-test && mvn clean install -DskipTests

# system-setting（系统设置）
cd system-setting && mvn clean install -DskipTests

# project-management（项目管理）
cd project-management && mvn clean install -DskipTests

# report-stat（报告统计）
cd report-stat && mvn clean install -DskipTests

# workstation（工作台）
cd workstation && mvn clean install -DskipTests
```

---

## 场景二：修改了 framework/sdk-parent 下的代码

**适用**：改了 domain、sdk、xpack-interface、jmeter 等共享模块

### 2.1 只改了 domain（实体类、Mapper）
```bash
# 先编译 domain
mvn clean install -DskipTests -pl framework/sdk-parent/domain

# 再编译依赖它的业务模块（根据你要测试的模块选择）
mvn clean install -DskipTests -pl test-track/backend
```

### 2.2 只改了 sdk（工具类、Service基类）
```bash
# 先编译 sdk（sdk 依赖 domain，会自动编译 domain）
mvn clean install -DskipTests -pl framework/sdk-parent/sdk -am

# 再编译业务模块
mvn clean install -DskipTests -pl test-track/backend
```

### 2.3 只改了 xpack-interface（扩展接口）
```bash
# 先编译 xpack-interface
mvn clean install -DskipTests -pl framework/sdk-parent/xpack-interface

# 再编译业务模块
mvn clean install -DskipTests -pl test-track/backend
```

### 2.4 改了多个 framework 模块（推荐：一键重编译所有 framework）
```bash
# 重新编译整个 framework/sdk-parent
mvn clean install -DskipTests -pl framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter

# 然后编译你要测试的业务模块
mvn clean install -DskipTests -pl test-track/backend
```

---

## 场景三：修改了 gateway 或 eureka

```bash
# gateway（网关）
cd framework/gateway && mvn clean install -DskipTests

# eureka（注册中心）
cd framework/eureka && mvn clean install -DskipTests
```

---

## 场景四：修改了前端代码

```bash
# 进入对应模块的 frontend 目录
cd test-track/frontend

# 开发模式（热更新）
npm run serve

# 或者构建生产版本
npm run build
```

---

## 场景五：不确定改了什么，全量重编译

```bash
# 方式一：跳过前端，只编译后端（推荐，速度快）
mvn clean install -DskipTests -DskipAntRunForJenkins -pl "!framework/sdk-parent/frontend,!api-test/frontend,!performance-test/frontend,!project-management/frontend,!report-stat/frontend,!system-setting/frontend,!test-track/frontend,!workstation/frontend"

# 方式二：全量编译（包含前端，很慢）
mvn clean install -DskipTests
```

---

## 一键命令别名（推荐添加到 ~/.zshrc）

```bash
# MeterSphere 开发快捷命令
alias ms-sdk='cd ~/ideaProjects/metersphere && mvn clean install -DskipTests -pl framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter'
alias ms-track='cd  ~/ideaProjects/metersphere/test-track && mvn clean install -DskipTests'
alias ms-api='cd ~/ideaProjects/metersphere/api-test && mvn clean install -DskipTests'
alias ms-perf='cd ~/ideaProjects/metersphere/performance-test && mvn clean install -DskipTests'
alias ms-sys='cd ~/ideaProjects/metersphere/system-setting && mvn clean install -DskipTests'
alias ms-proj='cd ~/ideaProjects/metersphere/project-management && mvn clean install -DskipTests'
alias ms-report='cd ~/ideaProjects/metersphere/report-stat && mvn clean install -DskipTests'
alias ms-work='cd ~/ideaProjects/metersphere/workstation && mvn clean install -DskipTests'
alias ms-gateway='cd ~/ideaProjects/metersphere/framework/gateway && mvn clean install -DskipTests'
alias ms-eureka='cd ~/ideaProjects/metersphere/framework/eureka && mvn clean install -DskipTests'

# 组合命令：改了 sdk 后重编译 test-track
alias ms-sdk-track='ms-sdk && ms-track'
```

---

## 编译后如何重启服务？

### 方式一：IDEA 直接运行（开发推荐）
1. 在 IDEA 中找到对应模块的 `Application.java`
2. 右键 → Run/Debug
3. 修改代码后，重新编译模块，然后重启应用

### 方式二：命令行运行 JAR
```bash
# 编译后，JAR 文件在 target 目录
java -jar test-track/backend/target/test-track-2.10.jar
```

### 方式三：Docker 方式（生产环境模拟）
```bash
# 使用构建脚本，只构建单个模块的镜像
./scripts/metersphere-build.sh -s -b test-track
```

---

## 常见问题

### Q: 编译报错 "找不到 io.metersphere.xxx"
**A**: 说明依赖的 framework 模块没有安装到本地仓库，先执行：
```bash
mvn install -N  # 安装父 POM
mvn clean install -DskipTests -pl framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter
```

### Q: 前端改动没生效
**A**: 前端需要单独构建：
```bash
cd test-track/frontend && npm run build
```
然后重新编译后端（会把 dist 复制到 resources/static）

### Q: 想跳过某个模块
**A**: 使用 `-pl "!模块名"` 排除：
```bash
mvn clean install -DskipTests -pl "!performance-test"
```

---

## 速查表

| 修改位置 | 命令 |
|---------|------|
| test-track 后端 | `cd test-track && mvn clean install -DskipTests` |
| api-test 后端 | `cd api-test && mvn clean install -DskipTests` |
| framework/sdk-parent/domain | `mvn install -DskipTests -pl framework/sdk-parent/domain` |
| framework/sdk-parent/sdk | `mvn install -DskipTests -pl framework/sdk-parent/sdk -am` |
| 整个 framework | `mvn install -DskipTests -pl framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter` |
| 前端 | `cd xxx/frontend && npm run build` |
| 全量后端 | `mvn clean install -DskipTests -DskipAntRunForJenkins -pl "!framework/sdk-parent/frontend,!api-test/frontend,!..."` |
