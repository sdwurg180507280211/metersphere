# 🚀 CodeBuddy 是什么？5 分钟读懂腾讯云全链路 AI 终端

> 这不是一篇空谈“概念”的科普，而是一份小白也能照着玩起来的攻略。

---

## 0. 一眼看懂 CodeBuddy

| 你关心的点 | 一句话说明 |
| --- | --- |
| 它是谁 | 腾讯云官方的 AI 编程终端（CLI），别名 CodeBuddy Code |
| 用来干嘛 | 用中文/英文和终端里的 Agent 对话，完成写代码、修 Bug、跑测试、上线等 DevOps 全链路任务 |
| 运行位置 | 直接在你熟悉的 Bash / zsh / PowerShell 内，不用换 IDE |
| 支持平台 | macOS、Linux、Windows 都可 |
| 亮点 | 自然语言就能驱动命令、Git、脚本、CI 流水线，支持子 Agent、插件、MCP 扩展 |

---

## 1. CodeBuddy 能帮我做啥？（举 4 个真情景）

1. **改一段代码**：把需求发给终端，例如“帮我把 `src/login.tsx` 加上短信验证码”，AI 会自动读文件、给出修改建议，还能直接生成 commit message。
2. **跨文件重构**：输入 “把项目里的 class 组件都换成函数组件”，它会列出计划、逐步执行，并询问你是否授权写文件。
3. **日志分析/排障**：`cat error.log | codebuddy "定位报错根因"`，几秒内给你问题定位+修复建议。
4. **自动化脚本**：`codebuddy -p "生成一个 GitLab CI 脚本，分编译、测试、部署三步"`，直接在终端打印结果，复制即用。

---

## 2. 3 步就能开箱体验

> 先准备 Node.js 18.20+ 与 npm（或直接用原生安装器），再跟着下面做。

### ✅ Step1：安装

```bash
# 推荐：npm/pnpm/yarn/bun 任一
npm install -g @tencent-ai/codebuddy-code
```

不想装 Node？可以用官方原生安装脚本：

```bash
# macOS / Linux
curl -fsSL https://copilot.tencent.com/cli/install.sh | bash
```

### ✅ Step2：运行 & 登录

```bash
codebuddy   # 简写 cbc 也行
```

打开后用上下键选择登录方式：

- Log in via Chinese Site（国内默认）
- Log in via International Site（海外）
- Enterprise Domain（企业私有化）
- iOA（仅限腾讯员工）

回车后浏览器会自动完成授权。

### ✅ Step3：初始化项目上下文

```text
> /init
```

第一次在某个仓库使用时一定要跑 `/init`：

- 生成项目知识图谱，AI 更懂你的文件结构
- 少传重复上下文，节省 30%~50% token
- 之后响应速度更快

---

## 3. 常用命令 & 工作流小抄

| 场景 | 示例命令 | 小提示 |
| --- | --- | --- |
| 互动问答 | `codebuddy` 然后直接描述需求 | `Ctrl+O` 可以看“AI 在想啥” |
| 单次执行 | `codebuddy -p "检查 services 目录的 TypeScript 类型" -y` | `-y` 代表同意读写权限 |
| 管道分析 | ``git log --oneline \| codebuddy "总结最近 5 次提交的风险"`` | 适合日志、SQL、报错一把梭 |
| 项目级任务 | `codebuddy -p "给 services/ 里所有 API 加单测" -y` | 大任务配合 `/plan` 预览执行步骤 |

常用快捷键：

- `Tab` 自动补全命令
- `Shift+Tab`（或 Windows 上 `Alt+M`）轮换权限模式：默认 → Bypass → Accept → Plan
- `Ctrl+R` 展开/折叠长输出
- `Esc Esc` 清空输入

---

## 4. 多 Agent + 插件生态怎么用？

### 👯 Sub-Agents
给不同任务创建“小分身”。例如：

```
/agents
├─ 前端体验官：专注 React、CSS
├─ DevOps 老王：熟悉 Docker、CI/CD
└─ BugHunter：专门查日志
```

它们共享对话上下文，但可绑定不同工具与目录，避免每次重新提示。

### 👨‍👩‍👧 Agent Teams
当一个需求需要多人协作时，用 `/team` 把几个 Sub-Agent 组成“虚拟 Scrum”：

1. Product Agent 负责拆解任务
2. Dev Agent 写代码
3. QA Agent 写用例

AI 会自动串流程、产出复盘记录。

### 🧩 Skills / Plugins / MCP

- 把你已有的脚本、HTTP API、数据库封装成插件，就能在 CLI、Web UI、企业 IM 机器人里复用。
- CodeBuddy 支持 Model Context Protocol (MCP)，可以把内部知识库、工单系统接入，让 AI 真正懂你的业务。

---

## 5. 安全与可控性

1. **权限面板**：每次读写文件或执行命令都会弹出确认，保证“最小权限”。
2. **Bash Sandboxing**：黑名单/白名单机制，避免误删文件、乱跑脚本。
3. **日志审计**：所有对话、命令、结果都能回放，满足企业合规。
4. **多入口部署**：支持 GitLab CI/CD、Dev Container、WeCom Bot、Web UI、Daemon 背景常驻等多种形态。

---

## 6. 最近更新亮点（v2.79.0）

- **Daemon 模式**：让 CodeBuddy 常驻后台，随时接受指令。
- **新 Web UI + 任务看板**：浏览器里可视化查看 Agent 执行状态。
- **优化变更提案**：大任务会自动生成计划、差异、风险提醒。

版本更新频率很高，记得不时 `codebuddy update` 或关注 Release Notes。

---

## 7. 小白落地清单 ✅

1. 装好 Node.js 或运行官方安装脚本。
2. `codebuddy --version` 确认命令可用，启动后完成登录。
3. 在项目目录运行 `/init`。
4. 把热键、`-p` 模式、`-y` 授权记熟。
5. 尝试首个任务：让它“阅读 README 并总结项目结构”。
6. 逐步引入 Sub-Agent、插件，扩展自己的 AI 工具箱。

> 让 AI 真正融入终端工作流的关键，是把“想法”变成“可执行的命令”。CodeBuddy 已经帮你打通最后一公里，剩下就看你如何驱动它啦。
