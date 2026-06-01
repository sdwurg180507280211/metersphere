# develop 与 master 差异总结

> 记录时间：2026-06-01
> 对比范围：`master` 与 `develop-v2.10.26`
> 关联文档：`docs/04-技术架构/微前端架构/分支合并.md`

本文记录 `develop-v2.10.26` 与 `master` 在选择性同步后的差异口径。当前重点不是逐文件列出所有 diff，而是说明哪些差异已经处理，哪些差异应作为整体专项保留，哪些仍需单独确认。

## 1. 总体结论

本轮处理后，普通业务逻辑差异已经基本收敛。

已处理完成的差异方向：

- 用例导入文本步骤重复检测修复已同步到 `develop-v2.10.26`。
- 系统站点 URL 环境变量覆盖已同步到 `develop-v2.10.26`。
- `master` 已去掉 `LoginFilter` 中的 `/.well-known/**` 白名单。
- `master` 已删除 `framework/gateway/src/main/resources/logback.xml`。
- workflow 工作流微服务及系统设置入口已从 `master` 移除；`develop-v2.10.26` 当前已无 workflow 业务源码和入口。
- 5 个模块的 `application.properties` 文件末尾换行已统一为“保留末尾换行”。

当前仍保留的主要差异应按两个整体专项处理：

- **AI/分析统计模块**：原分析统计模块，当前作为 `master` 独有或重点演进方向整体保留。
- **qiankun -> micro-app 微前端迁移**：`master` 已完成微前端架构迁移，相关差异整体保留。

除这两个专项，以及文档、`.kiro`、构建产物、运行日志、本地环境配置之外，当前没有明显需要继续从 `develop` 同步到 `master` 的普通业务逻辑代码。

## 2. 本轮处理记录

### 2.1 develop-v2.10.26 侧同步

```text
d8e75438ad fix(测试跟踪): 修复文本步骤导入重复检测
254d7e7a85 feat(系统设置): 支持站点地址环境变量覆盖
e45c6f739f style(config): 统一配置文件末尾换行
```

说明：

- `TestCaseNoModelDataListener` 中的文本描述模式导入重复检测逻辑已与 `master` 对齐。
- `SystemParameterService` 已支持通过 `METERSPHERE_SITE_URL` 覆盖系统站点 URL。
- 相关 `application.properties` 文件统一保留末尾换行。

### 2.2 master 侧处理

```text
0d2fae2850 fix(gateway): 移除 well-known 白名单
c24e2c3dc9 chore(gateway): 移除独立日志配置
d72cde4689 refactor(workflow): 移除工作流微服务
25cc77cd65 style(config): 统一配置文件末尾换行
2a38c0d8f1 style(config): 保留配置文件末尾换行
```

说明：

- `LoginFilter` 中的 `/.well-known/**` 白名单已移除，管理端口默认值和重复放行判断已与 `develop` 收敛。
- gateway 独立 `logback.xml` 已删除。
- workflow 微服务、system-setting workflow API/页面/路由/权限/i18n、gateway workflow swagger 配置、根 POM workflow 模块均已移除。
- 配置文件末尾换行最终统一为保留换行。

## 3. 归入 AI/分析统计整体差异的内容

以下内容不再逐项展开，统一作为 AI/分析统计模块差异处理：

```text
analytics-stat/**
framework/gateway/src/main/java/io/metersphere/gateway/filter/SessionFilter.java
framework/gateway/src/main/resources/application.properties
system-setting/backend/src/main/java/io/metersphere/service/ModuleService.java
system-setting/frontend/src/business/system/setting/MsModule.vue
framework/sdk-parent/frontend/src/i18n/lang/en-US.js
framework/sdk-parent/frontend/src/i18n/lang/zh-CN.js
framework/sdk-parent/frontend/src/i18n/lang/zh-TW.js
pom.xml 中的 analytics-stat 模块声明
```

典型差异包括：

- `/ai` 或 `/analytics` 相关网关路由与静态资源路径；
- AI/分析统计模块开关；
- `analytics_stat` 或 AI 模块菜单与国际化文案；
- `analytics-stat` 后端、前端、静态构建产物和迁移脚本。

结论：作为整体专项保留 `master`，不按普通业务代码同步。

## 4. 归入 qiankun -> micro-app 整体差异的内容

以下内容统一作为微前端架构迁移差异处理，不逐项作为普通业务差异判断：

```text
framework/sdk-parent/frontend/src/app-init.js
framework/sdk-parent/frontend/src/micro-app-config.js
framework/sdk-parent/frontend/src/micro-app-setup.js
framework/sdk-parent/frontend/src/components/MicroAppWrapper.vue
framework/sdk-parent/frontend/src/utils/micro-app-*.js
framework/sdk-parent/frontend/src/business/app-layout/index.vue
framework/sdk-parent/frontend/src/components/head/HeaderWs.vue
framework/sdk-parent/frontend/src/components/head/ProjectSearchList.vue
framework/sdk-parent/frontend/src/components/task/TaskCenter.vue
*/frontend/src/main.js
*/frontend/src/public-path.js
*/frontend/vue.config.js
*/frontend/src/router/index.js
*/frontend/src/router/modules/*.js
```

测试报告相关 Vue 文件也归入该类，因为差异本质是 micro-app 组件接入方式：

```text
test-track/frontend/src/business/plan/view/comonents/api/TestPlanApiCaseResult.vue
test-track/frontend/src/business/plan/view/comonents/api/TestPlanApiScenarioList.vue
test-track/frontend/src/business/plan/view/comonents/load/TestPlanLoadCaseList.vue
test-track/frontend/src/business/plan/view/comonents/report/detail/component/ApiScenarioFailureResult.vue
test-track/frontend/src/business/plan/view/comonents/report/detail/component/LoadAllResult.vue
test-track/frontend/src/business/plan/view/comonents/report/detail/component/UiScenarioResult.vue
test-track/frontend/src/business/plan/view/comonents/ui/TestPlanUiScenarioList.vue
```

典型差异包括：

- `MicroAppWrapper` 替代旧 `MicroApp`/qiankun 接入方式；
- 子应用 `window.mount(data)` / `window.unmount()` 生命周期；
- `__MICRO_APP_PUBLIC_PATH__` public path 适配；
- router 透传和 `PassThrough` 布局；
- 移除 qiankun UMD library 配置；
- 去除 qiankun prefetch 或 props 通信依赖。

结论：作为整体专项保留 `master`，不按普通业务代码同步。

## 5. 仍需单独确认的非专项差异

### 5.1 系统设置首页跳转路径

文件：

```text
system-setting/frontend/src/business/SettingHome.vue
system-setting/frontend/src/business/common/config.js
```

差异方向：

- `master` 使用实际路径，例如 `/setting/project/all`。
- `develop` 中存在 `/setting/project/:type` 这类动态占位符路径。

判断：

- `:type` 更像路由声明中的占位符，不适合作为实际跳转路径。
- 如需调整，应单独验证系统设置首页跳转，不建议作为分支同步的一部分直接覆盖。

结论：暂保留 `master`。

## 6. 可忽略或不建议同步的非业务差异

### 6.1 文档与 Kiro 规范

示例：

```text
docs/**
.kiro/**
*.md
```

结论：默认不随 develop -> master 业务代码同步。

### 6.2 构建产物与静态资源

示例：

```text
dist/**
static/js/**
static/css/**
target/**
```

结论：不作为普通源码同步对象。

### 6.3 运行日志与本地环境配置

示例：

```text
test-track/backend/test-track-startup.log
*.npmrc
.nvmrc
.codebuddy/settings.local.json
.idea/**
.flattened-pom.xml
```

结论：不作为业务逻辑同步对象。

## 7. 当前建议

后续同步时建议采用以下口径：

1. AI/分析统计模块作为整体专项处理，不拆成普通代码差异。
2. qiankun -> micro-app 迁移作为整体专项处理，不拆成普通代码差异。
3. workflow 微服务已废弃并从源码入口移除，后续不再作为分支差异保留。
4. 普通业务逻辑差异已经基本处理完成；剩余 `SettingHome/config` 跳转路径如需收敛，建议单独建任务验证。

综上：排除 AI/分析统计、micro-app、文档、构建产物、运行日志和本地环境配置后，当前两个分支已经没有明显需要继续从 `develop` 同步到 `master` 的普通业务逻辑代码。
