# 一个人也能带 AI 团队：CodeBuddy 终极实战手册

> 一条路径走完：安装 → 配置 → 治理。边看边在自己的项目里执行，每节一个阶段目标，跟完即拥有可落地的终端 AI 搭档。

**你将完成什么：**

| 阶段 | 目标 | 章节 |
| --- | --- | --- |
| 基础篇 | 装好、登录、跑通第一个任务 | 1-3 |
| 进阶篇 | 模型定制、团队记忆、自动化 | 4-6 |
| 治理篇 | 安全权限、竞品搭配 | 7-8 |

---

# 基础篇：先跑起来

## 1. 五步完成安装与首次使用

> 🎯 **本节目标**：终端输入 `codebuddy` 能正常启动，并完成第一个任务。

### Step 1：环境体检

| 必备项 | 检查命令 | 要求 |
| --- | --- | --- |
| Node.js | `node --version` | ≥ 18.20 |
| npm/pnpm/yarn/bun | `npm --version` | 任一即可 |
| 网络 | 能访问 npm 或腾讯脚本 | macOS/Linux/Windows 均可 |

### Step 2：安装

```bash
# 推荐：包管理器安装
npm install -g @tencent-ai/codebuddy-code

# 没装 Node？用官方脚本（macOS / Linux）
curl -fsSL https://copilot.tencent.com/cli/install.sh | bash

# 没装 Node？用官方脚本（Windows PowerShell）
irm https://copilot.tencent.com/cli/install.ps1 | iex
```

验证：

```bash
codebuddy --version
```

看到版本号即成功。

### Step 3：启动 & 登录

```bash
codebuddy   # 或简写 cbc
```

登录菜单用方向键选择：

| 入口 | 适合谁 |
| --- | --- |
| Chinese Site | 国内用户默认选它 |
| International Site | 海外用户 |
| Enterprise Domain | 公司私有化部署 |
| iOA | 腾讯员工 |

回车后浏览器自动完成授权，凭证保存后下次免登。

### Step 4：必跑 `/init`

```text
/init
```

第一次进项目一定要跑。`/init` 会扫描项目结构，识别构建工具、测试框架、代码模式，然后**自动生成 `.codebuddy/CODEBUDDY.md`**。这个文件就是项目的"知识底座"——AI 后续回答会直接引用，省去重复扫描，减少 30-50% 的上下文 Token 开销。

生成的文件位置：

| 文件 | 位置 | 提交到 Git？ |
| --- | --- | --- |
| 项目级 | `./CODEBUDDY.md` 或 `./.codebuddy/CODEBUDDY.md` | 是，团队共享 |
| 用户级 | `~/.codebuddy/CODEBUDDY.md` | 否，个人偏好 |
| 本地临时 | `./CODEBUDDY.local.md` | 否（建议加入 .gitignore） |

项目结构大改时，用 `/clear` + `/init` 重新生成即可。

### Step 5：用 `/plan` 跑通第一个任务

```text
/plan 帮我阅读 README 并总结项目结构
```

CodeBuddy 会列出执行计划，确认后自动读文件、生成总结。需要 Web 面板时输入 `/web`，适合分享会话或查看历史。

试一下单行模式：

```bash
# 单次执行，-y 自动授权读写
codebuddy -p "检查 src 目录的 TypeScript 类型" -y

# 管道模式，让 AI 帮你读日志
cat error.log | codebuddy "定位报错根因"
```

> 💡 多台机器同步：把 `~/.codebuddy` 拷贝到云盘或 Git 私仓（注意排除密钥文件），新电脑复制即可继承配置。

---

## 2. 模型配置：用 `models.json` 做"模型中控"

> 🎯 **本节目标**：让 CodeBuddy 用上你指定的模型，并在下拉框里只显示需要的选项。

### 两层配置，各有分工

| 层级 | 路径 | 作用 |
| --- | --- | --- |
| 用户级 | `~/.codebuddy/models.json` | 全局默认，所有项目共享 |
| 项目级 | `<workspace>/.codebuddy/models.json` | 针对当前仓库覆盖，优先级更高 |

> 合并策略：项目级覆盖用户级，同 ID 替换，不同 ID 追加。

### 手把手：添加一个企业代理模型

复制以下内容到 `~/.codebuddy/models.json`：

```json
{
  "models": [
    {
      "id": "gpt-4o",
      "name": "GPT-4o (公司代理)",
      "vendor": "OpenAI",
      "url": "https://proxy.example.com/v1/chat/completions",
      "apiKey": "${PROXY_KEY}",
      "supportsToolCall": true
    },
    {
      "id": "gpt-4o-mini",
      "name": "GPT-4o Mini (公司代理)",
      "vendor": "OpenAI",
      "url": "https://proxy.example.com/v1/chat/completions",
      "apiKey": "${PROXY_KEY}",
      "supportsToolCall": true
    }
  ],
  "availableModels": ["gpt-4o", "gpt-4o-mini"]
}
```

> **注意**：`availableModels` 中列出的每个 ID 都必须在 `models` 数组中有对应定义（或者是 CodeBuddy 内置模型）。上面 `gpt-4o-mini` 如果只存在于内置模型列表，则不需要重复定义；但如果走公司代理，就必须像上面一样完整定义。

如果项目 A 需要私有模型，复制以下内容到 `<workspace>/.codebuddy/models.json`：

```json
{
  "models": [
    {
      "id": "project-a-llm",
      "name": "Project A Model",
      "vendor": "Enterprise",
      "url": "https://project-a.example.com/v1/chat/completions",
      "apiKey": "${PROJECT_A_KEY}"
    }
  ],
  "availableModels": ["project-a-llm", "gpt-4o"]
}
```

两个文件写好后，在终端验证：

```text
/reload
```

下拉框应该只出现 `availableModels` 中列出的模型 ID。

关键点：

| 字段 | 说明 |
| --- | --- |
| `availableModels` | 决定下拉框显示谁，没列出来的不会出现；**未配置或空数组则显示所有模型**；项目级会完全覆盖用户级，不合并 |
| `${ENV_NAME}` | 引用环境变量，密钥不落盘。设置方法：`export PROXY_KEY=sk-xxx` 写进 `~/.zshrc` 或 `~/.bashrc`，或写进 `settings.json` 的 `env` 字段 |
| `supportsToolCall` / `supportsImages` | 根据模型能力填写，否则工具调用会被拒 |
| `maxInputTokens` / `maxOutputTokens` | 限制上下文长度，避免超 token 报错 |
| `url` | 必须以 `/chat/completions` 结尾 |

保存后 1 秒内热更新，没生效可输入 `/reload` 手动刷新。

> 💡 建议建立"模型攻略"文档，记录不同模型擅长的任务与成本，填好 `availableModels` 后团队成员就不会乱选。

---

## 3. Memory & Rules：让 AI 记住"我们的做事方式"

> 🎯 **本节目标**：写好规则文件，让 CodeBuddy 不用每次重复提醒就能遵守团队规范。

### 四个文件，各管各的

| 文件 | 位置 | 写什么 |
| --- | --- | --- |
| 用户级规则 | `~/.codebuddy/CODEBUDDY.md` | 个人常用命令、编辑器偏好、项目列表 |
| 项目级规则 | `.codebuddy/CODEBUDDY.md` | 项目背景、模块结构、构建/测试命令 |
| 分角色规范 | `.codebuddy/rules/*.md` | `frontend.md`、`backend.md`、`security.md`，Sub-Agent 定向加载 |
| 本地临时 | `CODEBUDDY.local.md` | 只在本机生效，适合临时说明、未公开需求 |

### 实操：写一份项目级规则

复制以下内容到 `.codebuddy/CODEBUDDY.md`：

```markdown
# 项目速记
- 框架：NestJS + React + pnpm
- 启动命令：`pnpm dev:api` / `pnpm dev:web`
- 测试命令：`pnpm test` / `pnpm test:e2e`

# 团队规约
1. 【必须】新 API 必须补 integration test
2. 【禁止】修改 legacy/ 目录下任何文件
3. 【可选】commit message 使用 conventional commits 格式

@import docs/architecture.md
```

再写一份分角色规范，复制到 `.codebuddy/rules/security.md`：

```markdown
# 安全规范
- 【禁止】在代码中硬编码密钥、Token
- 【禁止】直接操作生产数据库
- 【必须】所有外部输入做参数校验
- 【必须】新接口必须有鉴权中间件
```

写好后在终端验证：

```text
/memory
```

应该看到已加载的规则文件列表。`@import` 把长文档一次性引入上下文，保持单一维护点。

### 管理 & 自动记忆

| 操作 | 命令 |
| --- | --- |
| 查看已加载知识 | `/memory` |
| 清空重来 | `/memory clear` → `/init` |

开启自动记忆，在 `settings.json` 中加：

```json
{
  "memory": {
    "enabled": true,
    "autoMemoryEnabled": true,
    "typedMemory": true
  }
}
```

或通过环境变量控制（两者等价，`settings.json` 优先）：

| 变量 | 作用 | 值 |
| --- | --- | --- |
| `CODEBUDDY_MEMORY_ENABLED` | 记忆功能总开关 | `true`/`1` 启用 |
| `CODEBUDDY_DISABLE_AUTO_MEMORY` | 关闭自动记忆 | `1` 禁用，`0` 启用 |
| `CODEBUDDY_TYPED_MEMORY_ENABLED` | Typed Memory 增强模式（4 种类型 + YAML frontmatter） | `true`/`1` 启用 |

层级关系：`CODEBUDDY_MEMORY_ENABLED` 是总开关 → `CODEBUDDY_DISABLE_AUTO_MEMORY` 控制是否自动记忆 → `CODEBUDDY_TYPED_MEMORY_ENABLED` 在自动记忆基础上启用结构化类型。关闭 Typed Memory 时，Auto Memory 回退为简化通用格式。

> 💡 团队规范写成"必须 / 可选 / 禁止"格式，AI 回复时会直接引用这些条目，减少来回确认。

---

# 进阶篇：定制你的 AI 工作流

## 4. 环境变量：一次配置，全员生效

> 🎯 **本节目标**：用 `settings.json` 统一团队的权限、模型、插件策略。

复制以下内容到 `~/.codebuddy/settings.json`：

```json
{
  "env": {
    "CODEBUDDY_DEFAULT_PERMISSION_MODE": "plan",
    "CODEBUDDY_ALLOWLIST_COMMANDS": "git status,git diff",
    "CODEBUDDY_DENYLIST_COMMANDS": "rm -rf,shutdown",
    "CODEBUDDY_MODEL": "gpt-4o",
    "CODEBUDDY_TYPED_MEMORY_ENABLED": "true",
    "CODEBUDDY_PLUGIN_DIRS": "/Users/lin/.codebuddy/plugins",
    "CODEBUDDY_DISABLE_CRON": "false"
  }
}
```

Shell 中临时覆盖：`export CODEBUDDY_DEFAULT_MODEL=gpt-4o`。CI/容器环境写进 Secrets。

### 配置优先级（从高到低）

1. **命令行参数** — 本次会话临时覆盖
2. **本地项目设置** `.codebuddy/settings.local.json` — 个人项目特定，不提交 Git
3. **共享项目设置** `.codebuddy/settings.json` — 团队共享
4. **用户设置** `~/.codebuddy/settings.json` — 个人全局
5. **Shell 环境变量** — `export` 设置，最低优先级

**关键规则**：`settings.json` 的 `env` 字段会在每个会话启动时注入环境变量，覆盖同名的 Shell 环境变量。所有环境变量都可以写在 `env` 字段中，也可以 `export`，但 `env` 字段优先。

### 高频变量速查

| 分类 | 变量 | 作用 |
| --- | --- | --- |
| 模型 | `CODEBUDDY_MODEL` | 默认模型 |
| | `SMALL_FAST_MODEL` | 轻量快速模型 |
| | `BIG_SLOW_MODEL` | 重型推理模型 |
| | `CODEBUDDY_MODEL_MAP` | 模型映射规则 |
| 权限 | `CODEBUDDY_DEFAULT_PERMISSION_MODE` | 默认权限模式（plan/accept/bypass） |
| | `CODEBUDDY_ALLOWLIST_COMMANDS` | 允许执行的命令 |
| | `CODEBUDDY_DENYLIST_COMMANDS` | 禁止执行的命令 |
| 记忆/日志 | `CODEBUDDY_TYPED_MEMORY_ENABLED` | 自动记忆开关 |
| | `CODEBUDDY_DISABLE_AUTO_MEMORY` | 关闭自动记忆 |
| | `CODEBUDDY_LOG_LEVEL` | 日志级别（debug/info/warn） |
| 插件/调度 | `CODEBUDDY_PLUGIN_DIRS` | 插件目录 |
| | `CODEBUDDY_DISABLE_CRON` | 关闭定时任务 |
| | `CODEBUDDY_DAEMON_STARTUP` | 守护进程启动 |

> 💡 多人协作时，在 Git 仓库放 `settings.template.json`，同事复制后填入密钥即可。

---

## 5. 多 Agent：把 AI 分工明确

> 🎯 **本节目标**：创建至少 2 个 Sub-Agent，让不同角色各司其职。

### 创建分身

用 `/agents` 面板创建，每个 Agent 独立设置角色提示、目录权限、可用工具：

| Agent 名 | 只读目录 | 可用工具 | 角色提示要点 |
| --- | --- | --- | --- |
| 日志巡检员 | `logs/` | `rg`、`python` | 专注分析日志，不写代码 |
| 安全顾问 | 全项目（只读） | 全部（只读） | 绑定 `rules/security.md`，禁止写生产目录 |
| 自动写手 | `src/`、`docs/` | 编辑器、Git | 专门生成 commit message、changelog |

### 实操：创建一个日志巡检员

**方式一：交互式创建**

在 CodeBuddy 会话中输入 `/agents`，选择「新建 Agent」，填写：

- **名称**：`日志巡检员`
- **角色提示**：

```text
你是一位日志分析专家。只读 logs/ 目录下的文件，使用 rg 和 python 工具进行分析。
你的职责是：定位报错根因、统计错误频率、发现异常模式。
你不得修改任何代码文件。
```

- **允许目录**：`logs/`
- **可用工具**：`rg`、`python`

创建完成后，在对话中用 `@` 调用：

```text
@日志巡检员 分析 logs/app.log 中最近 1 小时的 ERROR
```

**方式二：写配置文件**

在 `<workspace>/.codebuddy/agents/log-inspector.md` 中写入：

```markdown
---
name: 日志巡检员
allowedDirectories:
  - logs/
allowedTools:
  - Grep
  - Bash
---

你是一位日志分析专家。只读 logs/ 目录下的文件，使用 rg 和 python 工具进行分析。
你的职责是：定位报错根因、统计错误频率、发现异常模式。
你不得修改任何代码文件。
```

保存后在对话中直接 `@日志巡检员` 即可调用。

**方式三：CLI 参数动态定义**

```bash
codebuddy --agents '{"日志巡检员": {"description": "日志分析专家", "prompt": "只读 logs/ 目录，使用 rg 和 python 分析", "tools": ["Grep", "Bash"]}}'
```

### 编队协作

Agent Team 不是通过 `/team` 命令创建的，而是**用自然语言描述**：

```text
创建一个团队从不同角度分析这个项目：一个负责用户体验设计，一个负责技术架构。
```

CodeBuddy 会根据描述自动创建团队、生成成员、分配任务。团队运行时的操作：

| 操作 | 方法 |
| --- | --- |
| 直接与某个成员对话 | `@成员名 你的指令` |
| 切换委派模式 | `Shift+Tab` |
| 切换任务列表显示 | `Ctrl+T` |
| 告知领导意图 | 自然语言描述 |

### 代理配置备份

代理配置保存为 Markdown 模板，方便团队复用。复制到 `<workspace>/.codebuddy/agents/log-inspector.md`：

```markdown
---
name: 日志巡检员
allowedDirectories:
  - logs/
allowedTools:
  - Grep
  - Bash
---

你是一位日志分析专家。只读 logs/ 目录下的文件，使用 rg 和 python 工具进行分析。
你的职责是：定位报错根因、统计错误频率、发现异常模式。
你不得修改任何代码文件。
```

换机器直接复制 `.codebuddy/agents/` 目录即可。

### 使用建议

- Sub-Agent 适合单一职责的独立任务（巡检、审查、生成文档）
- Team 适合多角色协作的复杂任务（需求拆解→开发→测试→复盘）
- Agent 配置文件用 `.md` 格式，提交到 Git 后团队可共享
- 个人常用 Agent 放 `~/.codebuddy/agents/`，项目专属放 `.codebuddy/agents/`

---

## 6. 插件、MCP、Daemon：让 AI 自动运转

> 🎯 **本节目标**：配置至少一个自动化流程，让 CodeBuddy 不用你盯着也能干活。

### 三大扩展方式

| 方式 | 做什么 | 入口 |
| --- | --- | --- |
| 插件市场 + MCP | 调用浏览器、数据库、外部 API | `/plugins` |
| Daemon 常驻 | CLI 后台运行，随时接受指令 | `codebuddy daemon start` |
| Scheduled Tasks | 定时任务，自动跑计划/测试 | `/tasks` 或 `config scheduledTasks` |

### 实操 1：启动 Daemon 常驻

```bash
# 启动守护进程（自动分配端口）
codebuddy daemon start

# 指定端口
codebuddy daemon start --port 8080

# 查看状态（返回 JSON：status、pid、endpoint、startedAt）
codebuddy daemon status
# 输出示例：{"status":"running","pid":12345,"endpoint":"http://127.0.0.1:51862","startedAt":1775498920401}

# 重启
codebuddy daemon restart

# 停止
codebuddy daemon stop
```

启动后即使关掉终端窗口，CodeBuddy 仍在后台待命。验证方法：`codebuddy daemon status` 返回 `"status":"running"` 即成功。

**后台会话管理**：

```bash
codebuddy --bg "实现登录页面"           # 后台执行任务
codebuddy --bg --name feature-login "实现登录页面"  # 命名后台任务
codebuddy ps                            # 列出所有活跃 Worker
codebuddy logs feature-login            # 查看日志
codebuddy logs feature-login -f         # 持续跟踪日志
codebuddy attach feature-login          # 附加到后台会话
codebuddy kill feature-login            # 终止后台任务
```

### 实操 2：配置定时巡检任务

在 `settings.json` 中添加 `scheduledTasks`：

```json
{
  "scheduledTasks": [
    {
      "name": "晨会巡检",
      "cron": "0 8 * * 1-5",
      "prompt": "/plan 检查最近 24 小时的 PR，总结风险点"
    }
  ]
}
```

**Cron 表达式格式**：`分钟 小时 日期 月份 周几`

| 字段 | 取值范围 | 特殊符号 |
| --- | --- | --- |
| 分钟 | 0-59 | `*`、`*/N`、`N,M`、`N-M` |
| 小时 | 0-23 | 同上 |
| 日期 | 1-31 | 同上 |
| 月份 | 1-12 | 同上 |
| 周几 | 0-6（0 和 7 均为周日） | 同上 |

**限制须知**：

- 定时任务是**会话级别**的，退出 CodeBuddy 后自动清除
- 每个会话最多同时 50 个定时任务
- 最小间隔 1 分钟
- 循环任务 **3 天后自动过期删除**
- 会话中断期间错过的任务不会补跑

**通知推送**：定时任务本身在会话内执行，不会主动推送。如需推送到企业微信等渠道，需结合 Channels 功能（微信、Telegram、Discord、自定义 Webhook）配置。

禁用定时任务：设置 `CODEBUDDY_DISABLE_CRON=1`。

### 实操 3：安装 Chrome DevTools MCP

在 `settings.json` 中添加 MCP 配置：

```json
{
  "mcpServers": {
    "chrome-devtools": {
      "command": "npx",
      "args": ["-y", "chrome-devtools-mcp@latest", "--autoConnect"],
      "env": {
        "PATH": "/usr/local/bin:/usr/bin:/bin"
      }
    }
  }
}
```

安装后重启 CodeBuddy，在对话中即可：

```text
打开 http://localhost:3000 并截图首页
```

CodeBuddy 会直接调起浏览器、执行脚本、返回截图。

### 三个典型组合速查

| 场景 | 配置 | 效果 |
| --- | --- | --- |
| 晨会机器人 | Daemon + Scheduled Task → 每天 8 点跑 `/plan` | 会话内产出巡检结果，配合 Channels 可推送到企业微信 |
| 日志哨兵 | 插件连接 ELK/Kibana → 每小时读告警 | 自动提示下一步命令 |
| 网页调试官 | Chrome DevTools MCP | 直接打开页面、执行脚本、截图 |

---

# 治理篇：守规矩、选搭档

## 7. 安全 & 权限：上线前必须完成的动作

> 🎯 **本节目标**：所有安全项全部开启，确保 CodeBuddy 在合规边界内运行。

### 五件套 Checklist

| # | 动作 | 验证方法 |
| --- | --- | --- |
| 1 | 权限模式保持 `plan` | 让 CodeBuddy 写一个文件，终端应弹出计划确认，需你同意后才执行 |
| 2 | 开启沙箱 | 在 `settings.json` 中设置 `sandbox.enabled: true`，或在启动时加 `--sandbox` 参数；沙箱内执行项目外命令会被限制 |
| 3 | 企业域名 / SSO 登录 | 登录入口选择 Enterprise Domain，确认走公司身份 |
| 4 | 配置命令白/黑名单 | 在 `settings.json` 的 `env` 中设置 `CODEBUDDY_DENYLIST_COMMANDS`，包含 `rm -rf` 的命令会被拒绝 |
| 5 | 开启审计日志 | 设置 `CODEBUDDY_LOG_LEVEL=debug`，查看 `~/.codebuddy/logs/` 下日志有命令记录 |

### 实操：完整安全配置

复制以下内容到 `~/.codebuddy/settings.json`：

```json
{
  "sandbox": {
    "enabled": true
  },
  "env": {
    "CODEBUDDY_DEFAULT_PERMISSION_MODE": "plan",
    "CODEBUDDY_ALLOWLIST_COMMANDS": "git status,git diff,git log,cat,ls,rg,grep",
    "CODEBUDDY_DENYLIST_COMMANDS": "rm -rf,shutdown,reboot,dd,mkfs",
    "CODEBUDDY_LOG_LEVEL": "debug"
  }
}
```

在终端验证每个安全项：

```text
# 验证权限模式：让 AI 写文件，应该弹出计划确认
/plan 在 src/hello.ts 写一个 hello world 函数

# 验证黑名单：让 AI 删除文件，命令应被拒绝
帮我执行 rm -rf /tmp/test

# 验证审计日志：查看日志文件
cat ~/.codebuddy/logs/latest.log
```

### 额外加固

- 联网任务启用代理白名单/黑名单，防止访问未知域。
- Git 钩子或 CI 中自动跑 CodeBuddy 生成的 patch 测试。
- 关键操作（部署、删库）必须 `/plan` + 负责人复核。

---

## 8. 竞品搭配：不同场景用不同 CLI

> 🎯 **本节目标**：明确每款 CLI 的最佳场景，做到"主力用 CodeBuddy，其他各取所长"。

| 工具 | 最佳场景 | 核心亮点 | 注意事项 |
| --- | --- | --- | --- |
| **OpenCode** | 测试新模型、LSP、TUI Mission Control | 完全开源，75+ LLM 提供商，`opencode share` 链接 | 默认会上传会话标题，记得关闭 |
| **Claude Code** | Auto Mode、语音 `/voice`、Cowork GUI | 体验极佳 | 当前版本 2.x，关注权限策略和第三方收费 |
| **Gemini CLI** | Google Search grounding、1M token | 开源、原生 Google 生态，默认 Auto(Gemini 3) | 模型上下线要跟官方节奏 |
| **Kimi CLI** | 大规模生成网站/文档、Agent Swarm | K2.5 MoE 1T 参数，最多 100 子代理并行 | 需申请配额 |

**推荐分工**：CodeBuddy 承担主力开发与治理，OpenCode 当实验室，Gemini CLI 负责查实时资料，Kimi CLI 负责批量生成。

---

# 附录：模型与套餐速览

| 工具 | 默认可用模型 | 可自带模型？ | 费用要点 |
| --- | --- | --- | --- |
| CodeBuddy（中国站） | 腾讯云混元系列、行业模型、Claude Sonnet 等 | 是，`models.json` 接入 | CLI 免费，模型费按腾讯云计费 |
| CodeBuddy（海外站） | Global 模型、Claude Sonnet 等 | 是 | CLI 免费，绑定海外腾讯云或外部 API |
| OpenCode | 75+ 模型（OpenAI/Anthropic/Gemini/Bedrock/Groq） | 是，完全 BYO | 开源免费，仅付模型 API 费 |
| Claude Code | Opus/Sonnet/Haiku | 否 | 需 Anthropic 订阅或 API 额度 |
| Gemini CLI | Gemini 2.5/3.x + 1M Token | 否 | CLI 免费，按 Gemini API 计费 |
| Kimi CLI | Kimi K2.5（1T 参数） | 否 | CLI 开源，需购买 Coding 套餐或申请企业额度 |

> 共性：所有 CLI 都是"工具免费 + 模型计费"。评估时把模型费用纳入预算，关注供应商配额策略。

### CodeBuddy 套餐 FAQ
- **Free**：注册腾讯云账号即可领取，通常含约 250 credits/14 天，足够体验 CLI 与常用模型；额度用完需等待刷新或补充 credits。
- **Pro**：面向个人/小团队，约 1,000 credits/月 + 更高并发，可按月/年付款，适合日常开发。可额外购买 1,000 credits 加包（与 Pro 同价），按需堆叠。
- **Enterprise**：定制 seat 数、专属额度、企业实名/私有化部署、SLA 与技术支持，联系商务开启。
- **如何查额度**：执行 `/plan quota` 或登录 copilot.tencent.com →「模型额度」页面；充值、升级套餐也在该页面完成。
- **如何扩展模型**：在 `models.json` 写入自有代理或 OpenRouter、DeepSeek 等 Key，直接切换到外部计费渠道，实现“官方套餐 + 自带模型”混合策略。

---

# 落地 Checklist

### 基础篇
- [ ] 安装完成：`codebuddy --version` 输出版本号
- [ ] 登录成功：终端显示已登录账号
- [ ] 项目初始化：在项目目录执行 `/init`，提示知识图谱构建完成
- [ ] 首个任务：用 `/plan` 跑通一个简单需求

### 进阶篇
- [ ] 模型配置：`models.json` 保存后下拉框出现新模型
- [ ] 规则生效：在 `.codebuddy/CODEBUDDY.md` 写一条规则，AI 回复时引用了它
- [ ] 环境变量：`settings.json` 保存后权限模式切换为 `plan`
- [ ] Agent 分工：至少创建 2 个 Sub-Agent，各有独立职责
- [ ] 自动化：配置 1 个 Scheduled Task 或 Daemon 流程跑通

### 治理篇
- [ ] 沙箱开启：`sandbox.enabled: true` 或 `--sandbox` 启动，项目外命令被限制
- [ ] 命令黑名单生效：`CODEBUDDY_DENYLIST_COMMANDS` 中的命令被拒绝
- [ ] 审计日志可查：`CODEBUDDY_LOG_LEVEL=debug` 后 `~/.codebuddy/logs/` 有记录
- [ ] 竞品分工明确：每款 CLI 有清晰的使用场景划分

---

照着做，CodeBuddy 会从"聊天机器人"升级为"终端执行搭档"——既懂你的项目，又守你的规矩，还能自动运行日常任务。
