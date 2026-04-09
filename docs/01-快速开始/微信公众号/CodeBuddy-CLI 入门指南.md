# CodeBuddy CLI：用自然语言驱动整个开发流程的 AI 编程神器

> 腾讯云智能编码助手 CodeBuddy Code，让代码开发更智能、更高效

## 前言

在 AI 编程助手层出不穷的今天，你是否还在为寻找一款真正能提升开发效率的工具而烦恼？

今天要给大家介绍的 **CodeBuddy CLI**，是一款基于腾讯云 AI 技术的智能编程工具。它不仅能帮你写代码，更能用自然语言驱动从代码编写、测试、调试到部署的整个开发流程！

## 🚀 CodeBuddy 是什么？

CodeBuddy Code 是一个智能编程环境，与传统问答式助手不同，它能够：

- **读取文件** - 自动理解你的项目结构
- **执行命令** - 运行测试、构建、部署
- **修改代码** - 直接编辑文件并保存
- **自主解决问题** - 在你观察、指导或离开的情况下独立完成任务

## ✨ 核心特性

### 1. 原生终端，无缝集成

**熟悉的环境**：在你熟悉的命令行环境中获得 AI 辅助，无需切换开发工具或学习新界面

**零学习曲线**：保持原有的开发习惯，AI 助手在后台默默工作

**原生体验**：完美融入现有开发工作流，支持所有主流操作系统和终端

### 2. 强大的开箱即用能力

- **内置工具链**：集成文件编辑、命令执行、Git 操作、测试执行等核心开发工具
- **智能提交**：自动生成规范的 commit 消息，支持代码审查和变更管理
- **灵活扩展**：通过 MCP（Model Context Protocol）轻松集成第三方工具和服务
- **自定义开发工具**：根据项目需求定制专属开发助手

### 3. AI 集成 Unix 哲学

像 `grep` 和 `awk` 一样，CodeBuddy 原生支持管道输入，可进行智能分析：

```bash
# 管道集成示例
git log --oneline | codebuddy "分析这些提交，找出潜在问题"
cat error.log | codebuddy "帮我分析这些错误日志"
```

## 🛠️ 快速开始

### 环境要求

- Node.js 18.0+

### 一键安装

```bash
npm install -g @tencent-ai/codebuddy-code
```

### 立即体验

```bash
# 进入项目目录
cd my-project

# 启动 CodeBuddy
codebuddy
# 或使用别名
cbc

# 或者直接提问
codebuddy "帮我优化这个函数的性能"
cbc "帮我优化这个函数的性能"
```

## 💡 核心使用场景

### 1. 代码探索与学习

向 CodeBuddy 提问，就像问一位资深工程师：

- 日志系统是如何工作的？
- 如何创建一个新的 API 端点？
- 第 134 行的 `async move { ... }` 是做什么的？
- 这个代码为什么调用 `foo()` 而不是 `bar()`？

这是熟悉新代码库的高效方式！

### 2. 智能开发工作流

推荐的四步工作流：

**第一步：探索**
```
进入 plan 模式，CodeBuddy 读取文件并回答问题，但不做任何修改
读取 src/auth 了解我们如何处理会话和登录
```

**第二步：规划**
```
我想添加 OAuth 登录，需要修改哪些文件？会话流程是什么？请创建一个计划
按 Ctrl+G 在文本编辑器中打开计划，在 CodeBuddy 继续之前直接编辑
```

**第三步：实现**
```
切回正常模式，让 CodeBuddy 根据计划编码
按照你的计划实现 OAuth 流程。编写回调处理程序的测试，运行测试套件并修复任何失败
```

**第四步：提交**
```
用描述性消息提交并创建 MR
```

### 3. 调试与问题解决

提供丰富的上下文让 CodeBuddy 更高效：

- **使用 @ 引用文件**：无需描述代码位置
- **直接粘贴截图**：拖放图片到提示中
- **提供 URL**：链接到文档和 API 参考
- **管道输入数据**：运行 `cat error.log | codebuddy` 直接发送文件内容

### 4. 代码审查与优化

使用子代理（Sub-Agent）进行专项审查：

```bash
# 创建一个安全审查子代理
# .codebuddy/agents/security-reviewer.md
---
name: security-reviewer
description: 审查代码中的安全漏洞
tools: Read, Grep, Glob, Bash
model: opus
---

你是资深安全工程师。审查代码中的：
- 注入漏洞（SQL、XSS、命令注入）
- 认证和授权缺陷
- 代码中的密钥或凭证
- 不安全的数据处理

提供具体的行号引用和修复建议。
```

然后告诉 CodeBuddy：`使用子代理审查这段代码的安全问题`

## 📚 高级功能

### CODEBUDDY.md - 项目专属配置

在项目根目录创建 `CODEBUDDY.md` 文件，CodeBuddy 会在每次会话开始时读取它。包含：

- Bash 命令
- 代码风格规范
- 工作流规则

示例：

```markdown
# 代码风格
- 使用 ES 模块（import/export）语法，不使用 CommonJS（require）
- 尽可能使用解构导入（如 `import { foo } from 'bar'`）

# 工作流
- 完成一系列代码更改后始终运行类型检查
- 为了性能，优先运行单个测试而不是整个测试套件
```

### Skills（技能）系统

在 `.codebuddy/skills/` 目录下创建 `SKILL.md` 文件，为 CodeBuddy 提供领域知识和可复用的工作流。

示例 - 修复 Issue 的技能：

```markdown
# .codebuddy/skills/fix-issue/SKILL.md
---
name: fix-issue
description: 修复 GitHub issue
disable-model-invocation: true
---

分析并修复 GitHub issue：$ARGUMENTS。

1. 使用 `gh issue view` 获取 issue 详情
2. 理解 issue 中描述的问题
3. 搜索代码库中的相关文件
4. 实现必要的更改来修复问题
5. 编写并运行测试验证修复
6. 确保代码通过 lint 和类型检查
7. 创建描述性的提交消息
8. 推送并创建 MR

运行 /fix-issue 1234 来调用它。
```

### MCP 集成

通过 MCP 服务器，你可以让 CodeBuddy：

- 从 issue 跟踪器实现功能
- 查询数据库
- 分析监控数据
- 集成 Figma 设计
- 自动化工作流

```bash
codebuddy mcp add
```

### 插件市场

运行 `/plugin` 浏览插件市场。插件将技能、钩子、子代理和 MCP 服务器打包成可安装的单元。

### 检查点（Checkpointing）

CodeBuddy 的每次操作都会创建检查点。双击 Escape 或运行 `/rewind` 打开检查点菜单，你可以：

- 仅恢复对话（保留代码更改）
- 仅恢复代码（保留对话）
- 两者都恢复

检查点跨会话持久化，即使关闭终端也能回滚！

### 会话恢复

```bash
codebuddy --continue    # 继续上次会话
codebuddy --resume      # 从最近会话中选择
```

使用 `/rename` 给会话起描述性名字（如 "oauth-migration"、"debugging-memory-leak"）方便查找。

## 🎯 最佳实践

### 1. 提供验证方式

**核心建议**：提供测试、截图或预期输出，让 CodeBuddy 能够自我验证。

| 策略 | 之前 | 之后 |
|------|------|------|
| 提供验证标准 | "实现一个验证邮件的函数" | "编写 validateEmail 函数。测试用例：user@example.com 返回 true，invalid 返回 false，user@.com 返回 false。实现后运行测试" |
| 视觉验证 UI 变更 | "让仪表盘更好看" | "[粘贴截图] 实现这个设计。完成后截图对比原设计，列出差异并修复" |

### 2. 提供具体的上下文

**核心建议**：指令越精确，需要的修正越少。

| 策略 | 之前 | 之后 |
|------|------|------|
| 限定范围 | "给 foo.py 添加测试" | "给 foo.py 编写测试，覆盖用户登出的边界情况。避免使用 mock" |
| 指向来源 | "为什么 ExecutionFactory 的 API 这么奇怪？" | "查看 ExecutionFactory 的 git 历史，总结其 API 如何演变" |
| 引用现有模式 | "添加日历组件" | "查看首页现有组件的实现方式理解模式。HotDogWidget.php 是好例子。遵循此模式实现支持月份选择和年份导航的日历组件" |

### 3. 积极管理上下文

**核心建议**：在不相关任务之间运行 `/clear` 重置上下文。

当接近上下文限制时，CodeBuddy 会自动压缩对话历史。但最好主动管理：

- 在不同任务之间使用 `/clear`
- 使用子代理进行调查，保持主会话整洁
- 使用 `/compact <instructions>` 自定义压缩行为

### 4. 并行运行多个会话

**核心建议**：并行运行多个 CodeBuddy 会话加速开发。

**多终端窗口**：在不同目录启动多个 CodeBuddy 实例

**Git Worktrees**：每个会话在独立的工作树中工作

**作者/审查者模式**：
- 会话 A（作者）：实现速率限制器
- 会话 B（审查者）：审查速率限制器实现，寻找边界条件、竞态条件

### 5. 避免常见错误

1. **大杂烩会话**：在一个会话中做不相关的任务，上下文被无关信息填满
   - 解决：使用 `/clear` 分隔不相关任务

2. **重复修正**：CodeBuddy 错了，你修正，还错，再修正
   - 解决：两次修正失败后，`/clear` 并编写更好的初始提示

3. **过度的 CODEBUDDY.md**：文件太长，CodeBuddy 会忽略一半
   - 解决：无情地修剪。如果 CodeBuddy 没有那条指令也能做对，删除它

4. **信任但不验证**：CodeBuddy 的实现看起来合理但不处理边界情况
   - 解决：始终提供验证（测试、脚本、截图）

5. **无限探索**：让 CodeBuddy "调查" 某事但没有范围限制
   - 解决：限定调查范围，或使用子代理避免消耗主上下文

## 🔧 配置技巧

### 设置首选语言

```bash
/config
# 选择 Language，输入"Simplified Chinese"
```

或在 `~/.codebuddy/settings.json` 中配置：

```json
{
  "language": "Simplified Chinese"
}
```

### 配置权限

使用 `/permissions` 允许安全命令，或使用 `/sandbox` 启用 OS 级隔离。

```bash
# 允许特定命令
codebuddy --allowedTools "Edit,Bash(git commit *)"

# 沙箱模式
codebuddy --sandbox

# 跳过所有权限检查（仅限沙箱环境）
codebuddy --dangerously-skip-permissions
```

### 无头模式

在 CI、pre-commit 钩子或脚本中使用：

```bash
# 一次性查询
codebuddy -p "解释这个项目是做什么的"

# 脚本的结构化输出
codebuddy -p "列出所有 API 端点" --output-format json

# 流式输出用于实时处理
codebuddy -p "分析这个日志文件" --output-format stream-json
```

## 📖 完整文档目录

通过 `codebuddy.ai/docs/cli/` 访问完整文档：

**入门指南**
- Overview - 概述
- Quickstart - 快速开始
- Installation - 安装指南
- Common Workflows - 常见工作流
- Interactive Mode - 交互模式
- Headless Mode - 无头模式
- Troubleshooting - 故障排除
- Best Practices - 最佳实践

**配置**
- Settings - 设置配置
- Models - 模型配置
- Memory - 内存管理
- Status Line - 状态行
- Environment Variables - 环境变量
- Terminal Configuration - 终端配置
- IDE Integrations - IDE 集成
- ACP Protocol - ACP 协议
- MCP - MCP 集成
- Slash Commands - 斜杠命令

**功能特性**
- Sub-Agents - 子代理
- Agent Teams - 代理团队
- Skills - 技能系统
- Hooks Guide - 钩子指南
- Plugins - 插件
- Plugin Marketplaces - 插件市场
- Sandbox - 沙箱
- Checkpointing - 检查点
- Git Worktree - Git 工作树
- Remote Control - 远程控制
- Web UI - Web 界面
- Scheduled Tasks - 定时任务
- WeCom Bot Setup - 企业微信机器人

**安全**
- Security Overview - 安全概述
- Identity and Access Management - 身份和访问管理
- Bash Sandboxing - Bash 沙箱

**参考**
- CLI Reference - CLI 参考
- Hooks Reference - 钩子参考
- Plugins Reference - 插件参考
- Cost Management - 成本管理
- Tools Reference - 工具参考
- HTTP API - HTTP API（Beta）

**SDK**
- Quickstart - SDK 快速开始
- Python SDK Reference - Python SDK 参考
- TypeScript SDK Reference - TypeScript SDK 参考
- Hook System - 钩子系统
- Permission Control - 权限控制
- Session Management - 会话管理
- SDK Custom Tools - SDK 自定义工具
- SDK MCP Integration - SDK MCP 集成
- SDK Demo Projects - SDK 示例项目

## 📧 反馈与支持

- 📧 技术支持：codebuddy@tencent.com
- 🌐 中国官网：https://copilot.tencent.com/cli
- 🌐 国际官网：https://www.codebuddy.ai/cli

## 结语

CodeBuddy CLI 不仅仅是一个代码助手，它是一个完整的智能开发环境。通过自然语言驱动整个开发流程，它能显著提升你的开发效率。

从简单的代码修改到复杂的架构重构，从日常的 Bug 修复到全新的功能开发，CodeBuddy 都能成为你最得力的编程伙伴。

现在就安装体验吧，让 AI 为你的开发工作赋能！

---

*本文基于 CodeBuddy CLI 官方文档编写，更多详细功能请参考 [官方文档](https://www.codebuddy.ai/docs/cli/overview)*
