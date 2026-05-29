# AI 模块演进记录：运行时标识统一

## 演进背景

在初期的架构设计中，AI 相关的页面被作为 `analytics-stat` (分析统计) 模块的一部分，通过前端路由和 Gateway 反向代理，暴露为 `/ai` 的访问路径。这种 "别名方案" 在使用上达到了目的，但在系统底层遗留了不一致的标识。

为了长期维护的规范化和统一，我们决定将该模块的**运行时标识**（如微服务注册名、前端路由名、数据库配置 key）统一为 `ai`。

> **注意**：本次重构仅针对运行时标识。为了兼容现有的构建链路和代码结构，以下**历史命名仍被保留**：
> - **Maven 模块目录**：`analytics-stat/`
> - **Maven Artifact ID**：`analytics-stat-parent`、`analytics-stat`
> - **前端工程包名**：`analytics-stat-frontend`
> - **Java 包名与入口类**：`io.metersphere.analyticsstat.AnalyticsStatApplication`
> - **Dockerfile 与构建脚本**：相关入口和依赖仍引用 `analytics-stat`。

## 修改核心路径

本次演进主要完成了以下运行时标识的对齐：

1. **前端子应用挂载标识统一**：
   在 `framework/sdk-parent/frontend/src/micro-app-config.js` 中，将微应用（micro-app）挂载时的 `serviceId` 由 `analytics-stat` 修改为 `ai`。

2. **微服务注册名与日志路径对齐**：
   修改了 `analytics-stat/backend/src/main/resources/application.properties`：
   - 将 `spring.application.name` 更改为 `ai`，确保服务在 Eureka 注册中心显示为 `ai`。
   - 将 `logging.file.path` 的输出目录更改为 `ai`。

3. **数据库系统参数初始化调整**：
   直接调整原始 Flyway 初始化脚本 `analytics-stat/backend/src/main/resources/db/migration/V1__add_analytics_stat_module.sql`，使新环境默认写入 `metersphere.module.ai`。对于已经存在旧数据的环境，由运维或开发人员手动将 `system_parameter` 表中的 `metersphere.module.analytics` 更新为 `metersphere.module.ai`，避免新增一次性迁移脚本。

4. **API 路径说明修正**：
   更新了 `analytics-stat/frontend/src/api/knowledge.ts` 中的文档注释，明确了 `/ai` 路由与 `analytics-stat` 工程名之间的转发对应关系。

## 演进总结

经过此次调整，平台内部的微前端容器、Eureka 注册中心、Spring Gateway 路由以及底层数据库的模块开关均已达到运行时标识的对齐。`ai` 已成为该功能模块在系统运行时的唯一合法身份。未来若需进行彻底的工程重命名，可在本项目标的基础上进一步推进。
