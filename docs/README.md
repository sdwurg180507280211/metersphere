# MeterSphere 项目文档索引

本目录收录 MeterSphere 项目本地二次开发、架构设计、系统集成、性能优化及历史排错专项文档。

---

## 目录结构概览

```text
docs/
├── README.md                                  # [本文件] 文档总索引与导航
│
├── 开发指南/                                  # 研发规范、构建依赖与排错运维
│   ├── git-commit-convention.md               # Git 提交规范（type/scope 格式约定）
│   ├── 模块依赖关系.md                        # 各微服务与共享 SDK 构建依赖流转图
│   └── 日志系统说明.md                        # 后端服务日志体系、映射及排错指引
│
├── 系统集成/                                  # 外部对接与消息中间件集成
│   ├── API-Key认证对接文档.md                 # 外部系统调用统计等接口的 API Key 认证机制
│   ├── RocketMQ双模式接入说明.md              # 需求同步 RocketMQ 双客户端模式接入
│   ├── CloudLaunch MQ对接方式总结.md          # 瑞保 CloudLaunch MQ 封装依赖对接说明
│   ├── Hualife MQ SDK对接方式总结.md          # 示例工程与 MQ SDK 对接说明
│   └── 内网CloudLaunch需求同步配置.txt        # 内部联调环境 MQ 关键配置项示例
│
├── 性能优化/                                  # 前端与大场景性能调优专题
│   ├── api-automation-vue-performance-optimization.md # 接口自动化页面 Vue 前端性能优化建议
│   └── 接口测试前端性能问题排查报告.md                # 接口自动化大场景卡顿排查与修复复核
│
├── 接口自动化脚本速查/                        # 接口测试自定义脚本开发手册
│   ├── 接口自动化脚本速查.md                  # Groovy / BeanShell 常用 API 与上下文变量速查
│   └── 接口自动化Python与JavaScript脚本速查.md # Python (Jython 2.7) / JavaScript (Rhino) 语法与速查
│
└── 功能开发/                                  # 各业务模块专项设计、需求改造与修复记录
    ├── SQL查询台/                             # 数据仪表盘与 SQL 查询台优化
    │   ├── 中间件技术在高级搜索中的应用.md
    │   ├── 查询台交互优化-20260928.md
    │   ├── 墨刀.html
    │   └── metersphere-search-redesign-version3.html
    ├── 高级搜索/                             # 综合查询设计
    │   └── JQL综合查询-开发完成总结.md
    ├── 工作流/                               # 流程控制与状态管理
    │   ├── 银保通项目状态字段重复问题分析与修复.md
    │   ├── 银保通项目状态字段重复问题修复.sql
    │   ├── 复测次数字段设计方案对比分析.md
    │   └── 复测次数强制初始化为0实现方案.md
    ├── 自定义字段/                           # 字段重复与全局模板清理
    │   ├── 全局字段与项目字段-完全指南.md
    │   ├── 缺陷导出重复字段问题分析.md
    │   ├── 全局模板和字段异常数据修复报告.md
    │   ├── 全局模板和字段异常数据修复.sql
    │   ├── 清理重复字段后的问题修复.md
    │   ├── 清理重复字段关联.sql
    │   └── 执行清理重复字段.sql
    ├── 用户组权限模型/                       # 权限隔离与数据迁移专项
    │   ├── METERSPHERE_PERMISSION_MODEL.md
    │   ├── USER_GROUP_MIGRATION_GUIDE.md
    │   ├── MIGRATION_IMPACT_ANALYSIS.md
    │   ├── USER_GROUP_FILTER_IMPLEMENTATION.md
    │   ├── SOLUTION_COMPARISON.md
    │   ├── SCOPE_ID_EXPLANATION.md
    │   ├── PAGEHELPER_PAGINATION_ISSUE.md
    │   ├── PURE_FRONTEND_SOLUTION_EVALUATION.md
    │   ├── MIGRATE_USER_GROUP_SIMPLE.sql
    │   ├── MIGRATE_USER_GROUP_TO_SYSTEM.sql
    │   └── TEST_USER_GROUP_FILTER.sql
    ├── 测试跟踪/                             # 缺陷管理与跟踪性能优化
    │   ├── 缺陷管理页面加载优化.md
    │   ├── 缺陷列表前端渲染性能分析.md
    │   └── 缺陷列表前端优化-实施记录.md
    ├── 缺陷管理/                             # 缺陷业务功能修复
    │   └── 缺陷导入默认值缺失问题分析与修复.md
    └── 系统设置/                             # 基础设置相关修复
        └── 公告内容长度限制修复.md
```

---

## 快速导航

### 1. 开发与运维指南
- [Git 提交规范](开发指南/git-commit-convention.md)：规范提交信息前缀与作用域。
- [模块依赖关系与构建流转](开发指南/模块依赖关系.md)：代码修改后如何针对性编译打包和重启对应容器。
- [日志系统说明](开发指南/日志系统说明.md)：Spring Boot + Logback 日志滚动策略与本地 `/opt/metersphere/logs` 路径排查。

### 2. 系统对接与消息队列
- [API Key 认证对接文档](系统集成/API-Key认证对接文档.md)：供第三方系统基于 accessKey 与 AES 签名调用 MeterSphere API。
- [RocketMQ 双模式接入说明](系统集成/RocketMQ双模式接入说明.md)：原生客户端模式与 CloudLaunch 模式切换说明。
- [CloudLaunch MQ 对接方式总结](<系统集成/CloudLaunch MQ对接方式总结.md>)：瑞保 CloudLaunch 依赖包解析。
- [Hualife MQ SDK 对接方式总结](<系统集成/Hualife MQ SDK对接方式总结.md>)：华夏 MQ 示例工程对接说明。
- [内网 CloudLaunch 需求同步配置示例](系统集成/内网CloudLaunch需求同步配置.txt)：本地或开发环境对接 properties 配置项。

### 3. 性能优化专项
- [接口自动化 Vue 前端性能优化建议](性能优化/api-automation-vue-performance-optimization.md)：组件懒加载、防抖、大对象 diff 优化。
- [接口测试前端性能问题排查报告](性能优化/接口测试前端性能问题排查报告.md)：大场景虚拟树渲染、步骤树 DOM 优化与修复跟踪。
- 缺陷列表前端渲染优化：详见 [功能开发/测试跟踪/缺陷列表前端渲染性能分析.md](功能开发/测试跟踪/缺陷列表前端渲染性能分析.md)。

### 4. 接口自动化脚本速查手册
- [Groovy & BeanShell 脚本速查](接口自动化脚本速查/接口自动化脚本速查.md)：适用于 JMeter JSR223 脚本步骤，汇总 vars / ctx / prev / log 常用方法。
- [Python & JavaScript 脚本速查](接口自动化脚本速查/接口自动化Python与JavaScript脚本速查.md)：适用于 Jython 2.7 与 Rhino ES5 引擎语法及常用逻辑。

### 5. 功能开发与数据修复专项
- **SQL 查询台 / 高级搜索**：[JQL 综合查询总结](功能开发/高级搜索/JQL综合查询-开发完成总结.md)、[原型设计预览](功能开发/SQL查询台/metersphere-search-redesign-version3.html)。
- **用户组权限模型**：[权限模型全景文档](功能开发/用户组权限模型/METERSPHERE_PERMISSION_MODEL.md)、[迁移执行指南](功能开发/用户组权限模型/USER_GROUP_MIGRATION_GUIDE.md)。
- **自定义字段修复**：[全局字段与项目字段指南](功能开发/自定义字段/全局字段与项目字段-完全指南.md)、[异常数据修复报告](功能开发/自定义字段/全局模板和字段异常数据修复报告.md)。
- **工作流**：[复测次数字段设计对比](功能开发/工作流/复测次数字段设计方案对比分析.md)、[状态字段重复修复分析](功能开发/工作流/银保通项目状态字段重复问题分析与修复.md)。

---

## 文档归档规范

新编写或导入的文档建议遵循以下归类：
1. **全局开发约定/构建运维**：放入 `docs/开发指南/`
2. **三方系统/MQ/认证对接**：放入 `docs/系统集成/`
3. **性能分析/优化专项报告**：放入 `docs/性能优化/`
4. **业务功能设计/缺陷分析/SQL 脚本**：放入 `docs/功能开发/<业务模块>/`
5. **脚本模板/速查手册**：放入 `docs/接口自动化脚本速查/`
