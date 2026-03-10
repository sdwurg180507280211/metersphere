# MeterSphere 项目优化建议（排除 `control-panel`）

## 1. 分析范围

- 分析对象：`framework`、`api-test`、`performance-test`、`project-management`、`report-stat`、`system-setting`、`test-track`、`workflow-service`、`analytics-stat`、`workstation`
- 排除范围：`control-panel`
- 分析维度：仓库结构、前后端技术栈、代码规模、重复建设、共享层耦合、构建与 CI、仓库卫生、可维护性

## 2. 总体判断

这个仓库的主要问题，不是单点性能问题，而是**工程复杂度高、共享边界模糊、重复代码较多、重构成本偏高**。

从抽样结果看：

- 后端 Java 代码量约 `293456` 行
- 前端代码量约 `380373` 行
- 后端测试文件仅 `6` 个
- 前端测试文件 `0` 个
- 7 个 Vue 2 子应用对共享前端层 `metersphere-frontend/src/*` 的深度引用约 `3186` 处

这意味着项目当前最大的问题是：

1. 改动很容易牵一发而动全身
2. 前端共享层更像“源码复用”，不是“稳定能力复用”
3. 后端核心业务类过大，后续维护和定位问题成本高
4. 前端双技术栈并行，长期维护压力会上升

## 3. 主要优化方向

### 3.1 前端共享层收口

这是我认为**最值得优先做**的一项。

#### 现状

多个前端子应用大量直接引用共享包内部源码，例如：

- `api-test/frontend/src/main.js`
- `test-track/frontend/src/router/index.js`
- `workstation/frontend/src/main.js`

引用方式普遍是：

```js
import xxx from 'metersphere-frontend/src/...'
```

这类写法的问题是：

- 子应用依赖的是共享层“内部目录结构”
- 共享层只要改文件位置、改实现方式、改技术栈，就会波及所有业务模块
- 共享层无法形成稳定 API，迁移到 Vue 3 / Vite 时阻力非常大
- 共享层和业务层边界不清，后续很难拆分

#### “收口”是什么意思

“收口”的意思不是删功能，而是**把共享层对外暴露的能力收敛到少量稳定入口**，不要再让业务模块直接 import 共享层内部源码。

目标是把：

```js
import Layout from 'metersphere-frontend/src/business/app-layout'
import { isMicroAppEnv } from 'metersphere-frontend/src/utils/micro-app-env'
import user from 'metersphere-frontend/src/store/modules/user'
```

逐步改成类似：

```js
import { Layout, isMicroAppEnv, userStoreModule } from 'metersphere-frontend'
```

或者至少改成稳定子路径：

```js
import { Layout } from 'metersphere-frontend/layout'
import { isMicroAppEnv } from 'metersphere-frontend/micro-app'
```

#### 收口后有什么好处

- 共享层可以单独演进，不容易影响业务模块
- Vue 2 → Vue 3 迁移时，只需要兼容公共出口，不需要全仓逐个修引用
- 减少“复制一份再改一点”的情况
- 新模块接入时更容易模板化

#### 建议做法

1. 先禁止新增 `metersphere-frontend/src/*` 深度引用
2. 在 `framework/sdk-parent/frontend` 中建立统一导出入口
3. 先抽出最通用的能力：`Layout`、登录页、路由守卫、权限工具、微前端工具、公共 store、通用组件
4. 子应用分批替换旧引用
5. 等深度引用足够少之后，再考虑共享层内部重构

---

### 3.2 前端重复代码收敛

#### 现状

多个子应用各自维护一套相似的：

- `src/main.js`
- `src/router/index.js`
- `vue.config.js`
- `babel.config.js`
- `package.json` scripts

虽然每个模块有少量差异，但整体结构高度相似。

#### 问题

- 微前端生命周期、路由初始化、插件注册逻辑在多个子应用重复出现
- 同类变更需要改 7 份以上
- 某个模块修了 bug，别的模块可能还保留旧问题

#### 建议

- 抽一个“子应用启动模板”
- 抽一个“通用路由工厂”
- 抽一个“通用 webpack/vue-cli 配置基类”
- 允许模块只覆盖少量差异项，例如端口、代理、入口组件、特殊拆包规则

---

### 3.3 后端超大服务类拆分

#### 现状

抽样发现几个非常重的业务服务类：

- `test-track/backend/src/main/java/io/metersphere/service/TestCaseService.java`
- `test-track/backend/src/main/java/io/metersphere/service/IssuesService.java`
- `test-track/backend/src/main/java/io/metersphere/plan/service/TestPlanService.java`
- `api-test/backend/src/main/java/io/metersphere/service/definition/ApiDefinitionService.java`

这些类承担了太多职责，通常同时包含：

- 查询
- 写入
- 校验
- 导入导出
- 权限处理
- 远程调用
- 事件发送
- 异步流程编排

#### 问题

- 定位 bug 成本高
- 改动范围很难控制
- 新人或未来的自己重新读代码也会很吃力
- 很难单独替换其中某一部分逻辑

#### 建议

按职责拆成更小的 service，例如：

- QueryService：查询与分页
- CommandService：新增/修改/删除
- ImportExportService：导入导出
- ExecuteService：执行流程与异步任务
- DomainValidator：规则校验
- FacadeService：保留对 Controller 的统一入口

这样拆分后，不一定要一次性重构完成，可以按需求逐步迁移。

---

### 3.4 前端技术栈统一路线

#### 现状

- 大部分子应用仍是 `Vue 2 + Vue CLI + Element UI`
- `analytics-stat` 已经使用 `Vue 3 + Vite + TypeScript`

#### 问题

- 双技术栈会长期增加维护成本
- 公共组件、公共工具、微前端兼容都要维护两套思路
- 共享层如果仍强耦合 Vue 2 内部实现，会拖慢整体升级

#### 建议

先明确一个终局方案：

- 方案 A：长期维持 Vue 2 主体，Vue 3 仅少量增量模块使用
- 方案 B：新模块全部 Vue 3 + Vite，旧模块逐步迁移

如果按长期可维护性考虑，更建议走 **方案 B**。但前提是先做“共享层收口”，不然迁移成本会很高。

---

### 3.5 数据访问层瘦身

#### 现状

仓库中存在大量 MyBatis Generator 风格的 `*Example` 类和 Mapper XML。

典型文件如：

- `framework/sdk-parent/domain/src/main/java/io/metersphere/base/domain/TestCaseExample.java`

#### 问题

- 生成代码规模大，影响阅读体验
- 领域模型和生成代码混在一起，不利于聚焦业务
- 代码质量统计容易被生成文件干扰

#### 建议

- 将生成代码目录与手写业务代码更清晰地隔离
- Sonar/静态分析尽量排除生成代码
- 新增复杂查询时，尽量将查询对象和业务逻辑分层，不继续把复杂度堆到 Example/Mapper 体系中

---

### 3.6 构建与 CI 过于偏“打包导向”

#### 现状

- Sonar 工作流执行 `verify` 时带了 `-DskipTests`
- Jenkins 构建命令同样默认 `-DskipTests`

#### 问题

- CI 更像“能不能打包”检查，不像“能不能稳定演进”检查
- 一旦做重构，很难快速知道有没有破坏核心能力

#### 说明

如果是**个人使用、个人维护**，这项优先级可以低于“共享层收口”和“服务拆分”。

你不一定需要完整测试体系，但建议至少保留：

- 少量核心路径的手工回归清单，或者
- 极少数关键自动化冒烟检查

这样在你自己后面做结构性重构时，会更稳。

---

### 3.7 仓库卫生优化

#### 现状

仓库里还存在一些应当清理的噪音文件，例如：

- `hs_err_pid*.log`
- 一些截图文件
- 个别构建产物

#### 建议

- 清理历史 crash 日志和临时截图
- 补充 `.gitignore`
- 尽量避免把调试中间产物放进主仓库

这类优化不会直接提升功能，但会降低仓库噪音，提高日常使用体验。

## 4. 如果你是个人使用，优先级怎么排

如果这个项目主要是你自己使用、自己维护，我建议优先级改成下面这样：

### P1：先做

1. 前端共享层收口
2. 前端重复代码收敛
3. 后端超大服务类按需求逐步拆分

### P2：有空再做

4. 前端 Vue 3 / Vite 迁移路线
5. 数据访问层与生成代码隔离
6. 仓库卫生清理

### P3：最后再考虑

7. 自动化测试基线
8. 更完整的 CI 质量门禁

也就是说：

**你不是完全不需要测试，而是现阶段不必把“建立完整测试体系”放到第一优先级。**

## 5. 对你当前最实用的建议

如果只选最有收益的三件事：

### 5.1 先限制共享层深度引用

- 以后新增代码不再写 `metersphere-frontend/src/*`
- 先从 `main.js`、`router`、`store` 这几类高频公共能力开始收口

### 5.2 做一个子应用公共模板

把下面几类公共逻辑抽出来：

- 微前端 mount/unmount
- 路由初始化
- 插件注册
- 公共 store 初始化
- 公共样式与权限处理

### 5.3 拆一个最大的后端服务类试点

建议优先从下面二选一开始：

- `test-track/backend/src/main/java/io/metersphere/service/TestCaseService.java`
- `api-test/backend/src/main/java/io/metersphere/service/definition/ApiDefinitionService.java`

原因是：

- 这两个模块代码体量大
- 改动频率通常也高
- 拆分收益最明显

## 6. 一句话总结

排除 `control-panel` 后，这个项目**最值得优化的不是“某个点慢”，而是“共享边界、重复建设和模块复杂度”**。

对个人项目来说，最务实的路线是：

**先收口前端共享层，再减少重复代码，最后逐步拆后端大类；测试体系可以后置，但最好保留最小安全网。**
