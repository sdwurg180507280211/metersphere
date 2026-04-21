# 文档目录索引

> 最后更新：2026-04-21

## 分支合并方法论

合并 develop 分支代码到 master 时，按文件后缀分类处理，避免全量 merge 带来的文档/脚本冲突：

| 类别 | 后缀 | 处理方式 |
|------|------|---------|
| **代码** | `.java` `.vue` `.js` `.xml` `.properties` `.sql`(仅Flyway) | 从 develop 检出同步 |
| **文档** | `.md` `.html` `.docx` | 按 master 保留，不同步 |
| **脚本/配置** | `.sh` `.yml` `.example` Makefile `.css` | 按 master 保留，不同步 |
| **构建产物** | `dist/` `static/js/` 下的 JS/CSS | 不同步，各分支自行构建 |

**操作步骤：**

1. `git diff --name-only -z master...develop` 获取差异文件列表
2. 按后缀筛选出代码文件
3. `git checkout develop -- <code_files>` 逐个检出
4. `git diff --cached` 验证暂存区仅含代码后缀
5. 提交

**优势：** 避免 `git merge` 的 rename/delete 冲突、中文路径 unstage 难题，以及 develop 新增文档污染 master 目录结构。

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
