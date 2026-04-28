# 文档目录索引

> 最后更新：2026-04-28

## 分支合并方法论

合并 develop 分支代码到 master 时，按文件后缀分类处理，避免全量 merge 带来的文档/脚本冲突：

| 类别 | 后缀 | 处理方式 |
|------|------|---------|
| **代码** | `.java` `.vue` `.js` `.ts` `.xml` `.properties` `.sql`(仅Flyway) | 从 develop 检出同步 |
| **文档** | `.md` `.html` `.docx` | 按 master 保留，不同步 |
| **脚本/配置** | `.sh` `.yml` `.example` Makefile `.css` | 按 master 保留，不同步 |
| **构建产物** | `dist/` `static/js/` 下的 JS/CSS | 不同步，各分支自行构建 |

**操作步骤：**

1. `git fetch origin` 更新远程分支
2. `git checkout develop-v2.10.26 && git pull` 更新本地 develop 分支
3. `git checkout master` 切回 master
4. `git diff --name-only master...develop-v2.10.26` 获取差异文件列表
5. 按后缀筛选出代码文件（排除 dist/、static/、docs/）
6. `git checkout develop-v2.10.26 -- <code_files>` 逐个检出
7. **检查 master-only 代码是否被覆盖**（见下方注意事项）
8. `git diff --cached` 验证暂存区仅含代码后缀
9. 提交

**优势：** 避免 `git merge` 的 rename/delete 冲突、中文路径 unstage 难题，以及 develop 新增文档污染 master 目录结构。

### master-only 代码保护清单

以下文件包含 master 分支独有的 micro-app 微前端代码，develop 分支没有，checkout 后**必须恢复**：

| 文件 | master 独有内容 |
|------|----------------|
| `framework/sdk-parent/frontend/src/business/app-layout/index.vue` | `<micro-app>` 标签、currentApp/appData 计算属性、handleDataChange/handleError |
| `framework/sdk-parent/frontend/src/micro-app-config.js` | 完整文件（MIGRATED_MODULES、getEntryUrl、toShortName 等） |
| `framework/sdk-parent/frontend/src/micro-app-setup.js` | 完整文件（microApp.start 配置、preFetchApps） |
| `framework/sdk-parent/frontend/src/utils/micro-app-env.js` | `__MICRO_APP_PROXY_WINDOW__` 双窗口检查 |
| `framework/sdk-parent/frontend/src/utils/micro-app-event-bus.js` | 完整文件（createEventBusAdapter、broadcastEvent） |
| `framework/sdk-parent/frontend/src/plugins/request.js` | `__MICRO_APP_PROXY_WINDOW__` 双窗口检查 |
| `framework/sdk-parent/frontend/src/components/MicroAppWrapper.vue` | 完整文件（按需加载子应用组件） |
| `framework/sdk-parent/frontend/src/router/index.js` | microAppRoutes 占位路由、beforeEach 子应用直接放行 |
| `test-track/frontend/src/router/modules/track.js` | PassThrough 替代 Layout（micro-app 环境下） |
| `workstation/frontend/src/router/modules/workstation.js` | PassThrough 替代 Layout（micro-app 环境下） |
| `analytics-stat/frontend/src/micro-app-env.ts` | `__MICRO_APP_PROXY_WINDOW__` 双窗口检查 |
| `analytics-stat/frontend/src/main.ts` | UMD 生命周期 + Pinia PersistedState |
| `api-test/frontend/src/main.js` | addDataListener 内存泄漏修复 |
| `performance-test/frontend/src/main.js` | addDataListener 内存泄漏修复 |

**恢复命令（checkout 后立即执行）：**
```bash
git checkout HEAD -- \
  framework/sdk-parent/frontend/src/business/app-layout/index.vue \
  framework/sdk-parent/frontend/src/micro-app-config.js \
  framework/sdk-parent/frontend/src/micro-app-setup.js \
  framework/sdk-parent/frontend/src/utils/micro-app-env.js \
  framework/sdk-parent/frontend/src/utils/micro-app-event-bus.js \
  framework/sdk-parent/frontend/src/plugins/request.js \
  framework/sdk-parent/frontend/src/components/MicroAppWrapper.vue \
  framework/sdk-parent/frontend/src/router/index.js \
  test-track/frontend/src/router/modules/track.js \
  workstation/frontend/src/router/modules/workstation.js
```

---

## 目录树
docs/
├── README.md
├── INDEX.md
│
├── 01-快速开始/
│   └── 项目结构与架构总览.md
│
├── 02-开发指南/
│   ├── git-commit-convention.md
│   ├── 模块依赖与构建指南.md
│   ├── 核心机制/
│   │   ├── 全局性能优化方案-切换项目卡顿问题.md
│   │   └── 前端构建与微前端资源加载机制.md
│   ├── 前端组件/
│   │   └── NodeTree组件升级对比.md
│   └── 性能优化/
│       ├── metersphere项目优化建议（排除control-panel）.md
│       ├── routing-optimization-analysis.md
│       └── 接口测试页面性能优化方案.md
│
├── 03-功能模块/
│   ├── API测试/
│   │   └── API自动化模块逻辑.md
│   ├── 高级搜索/
│   │   ├── JQL综合查询前端设计.html
│   │   ├── 记忆功能-项目隔离升级说明.md
│   │   ├── 记忆功能评估报告.md
│   │   └── 高级搜索快捷键功能.md
│   ├── 需求统计报告.md
│   ├── 工作流/
│   │   ├── flowable/
│   │   │   └── 工作流引擎集成与接口测试指南.md
│   │   ├── 微服务架构/
│   │   │   ├── 工作流微服务启动修复.md
│   │   │   ├── 工作流微服务迁移记录.md
│   │   │   ├── 流程设计器迁移指南.md
│   │   │   └── 独立微服务模块方案评估.md
│   │   └── 缺陷状态流转/ (7 files)
│   ├── 权限管理/
│   │   ├── 01-设计方案/ (6 files)
│   │   ├── 02-实现文档/ (7 files)
│   │   └── 03-验证测试/ (3 files)
│   ├── 自定义字段/
│   │   ├── 01-全局字段与模板/ (11 files)
│   │   ├── 02-UI创建全局字段/ (5 files)
│   │   ├── 03-国际化/ (2 files)
│   │   ├── 04-问题修复/ (3 files)
│   │   └── 05-具体字段实现/ (4 files)
│   ├── 测试跟踪/
│   │   ├── 功能用例/ (3 files)
│   │   └── 缺陷管理/ (9 files)
│   ├── 分析统计/
│   │   ├── 01-技术选型/ (5 files)
│   │   ├── 02-微前端集成/ (5 files)
│   │   ├── 03-实施记录/ (4 files)
│   │   ├── 04-问题排查/ (2 files)
│   │   └── 知识库检索迁移评估.md
│   └── 其他功能/
│       └── SQL查询台/ (3 files)
│
├── 04-技术架构/
│   ├── 微前端架构/
│   │   ├── UI实现方式对比分析.md
│   │   ├── 乾坤微前端技术细节.md
│   │   ├── 微前端框架实现方式指南.md
│   │   └── 模板文件/ (4 files)
│   ├── 微服务架构/ (4 files)
│   └── 中间件集成/
│       └── RocketMQ中间件/
│           └── MeterSphere本地RocketMQ集成指南.md
│
├── 05-部署运维/
│   ├── CI-CD/
│   │   └── 总结报告/
│   │       └── 持续集成集成完成说明.md
│   └── Windows 系统启动方案.md
│
├── 06-数据库/
│   └── SQL脚本/ (4 files)
│
├── 全流程平台对接/
│   ├── 详细设计文档.md
│   ├── 开发排期（截至2026-06-30）.md
│   ├── 需求平台字段确认清单.md
│   ├── generate_word.py
│   └── demandQuery/ (Vue前端组件)
│
└── 99-个人笔记/
    ├── 计算机基础/
    │   ├── CPU与指令执行周期.md
    │   └── 编译器与解释器.md
    ├── 微信公众号/ (32 articles)
    │   ├── CodeBuddy/ (10 files)
    │   ├── Figma-MCP系列教程/ (8 files)
    │   ├── Figma-Sites系列教程/ (7 files)
    │   └── ... (7 standalone articles)
    ├── PPT工具/ (6 files)
    ├── 医路同行文档工具/ (3 Python scripts)
    ├── 创意想法/
    │   └── 动漫主题-Live2D虚拟助手.md
    ├── 职场与个人/
    │   ├── 个人影响力建设指南.md
    │   ├── 简历项目经验模板.md
    │   ├── 入职进展周报.md
    │   ├── 工作选择复盘.md
    │   ├── 职场防坑指南.md
    │   ├── 简历.md
    │   └── 需求统计报告.docx
    └── 生活/
        └── 车次.md
```
