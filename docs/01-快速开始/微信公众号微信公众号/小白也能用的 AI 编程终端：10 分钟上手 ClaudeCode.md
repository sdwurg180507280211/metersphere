# 小白也能用的 AI 编程终端：10 分钟上手 Claude Code

如果你经常写代码、改 Bug、看日志，AI 终端会比网页对话更高效。今天这篇不讲概念，直接上手 Claude Code：从安装到可用，一步步带你跑通。

官方文档：https://docs.anthropic.com/zh-CN/docs/claude-code/overview

## 一、你将完成什么？

看完这篇，你可以做到：

- 安装并启动 Claude Code
- 完成账号登录或 API Key 配置
- 把命令加入 PATH（终端里直接调用）
- 在本地项目里完成第一次 AI 协作
- 接入 MCP 服务，让 AI 直接读取浏览器报错、操作 SaaS 工具

## 二、准备环境

### 2.1 安装 Node.js（版本 >= 22）

推荐使用 nvm 管理版本：

```bash
# 安装 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.4/install.sh | bash

# 加载 nvm（无需重启终端）
. "$HOME/.nvm/nvm.sh"

# 安装 Node.js 24
nvm install 24

# 验证安装
node -v
npm -v
```

Node.js 官网：https://nodejs.org/en/download

### 2.2 安装 Claude Code

```bash
# 全局安装（不要加 sudo）
npm i -g @anthropic-ai/claude-code

# 验证是否安装成功
claude --version
```

> **注意：不要用 `sudo npm i -g`**
> 用 root 权限安装后，全局包会写入系统目录，之后每次运行 `claude` 都需要 sudo。
> 使用 nvm 管理 Node.js 时，全局包安装在 `~/.nvm/` 下，普通用户即可正常使用。
>
> 如果之前已经用 sudo 装过，先卸载再重装：
> ```bash
> sudo npm uninstall -g @anthropic-ai/claude-code
> npm i -g @anthropic-ai/claude-code
> ```

## 三、账号登录

```bash
claude
```

如果提示不支持国区或无法完成登录，可以通过修改配置文件跳过登录流程：

```bash
vim ~/.claude.json
```

在文件中添加以下内容（如果文件已有内容，在最后一个 `}` 之前插入这一行）：

```json
"hasCompletedOnboarding": true
```

编辑完成后，按 `Esc`，输入 `:wq` 保存退出，然后重启终端即可。

> **注意：** 跳过登录后仍需配置 API Key 才能正常使用 AI 功能。

### 配置 API Key

Claude Code 会自动读取 `~/.claude/settings.json` 中的配置。如果你使用的是官方 API，直接填入 Key 即可：

```json
{
  "env": {
    "ANTHROPIC_AUTH_TOKEN": "你的API密钥"
  },
  "permissions": {
    "allow": [],
    "deny": []
  }
}
```

如果你使用的是国内中转或第三方兼容服务，额外添加自定义 API 地址：

```json
{
  "env": {
    "ANTHROPIC_AUTH_TOKEN": "你的API密钥",
    "ANTHROPIC_BASE_URL": "https://your-api-proxy.com"
  },
  "permissions": {
    "allow": [],
    "deny": []
  }
}
```

配置完成后重启终端即可生效。这种方式的优势：

- 配置持久化，无需每次手动设置环境变量
- 权限管理更灵活
- 支持更多高级配置选项

## 四、在项目里开始使用

进入你的代码目录，直接运行：

```bash
claude
```

可以先试几个简单指令热热身：

- "帮我读一下这个项目的目录结构"
- "定位登录模块在哪里"
- "把某个报错的根因分析出来"
- "按现有风格补一个单元测试"

## 五、接入 MCP 服务（进阶）

Claude Code 支持 MCP（Model Context Protocol），可以给 AI 接上各种外部工具——读取浏览器报错、查 Sentry 日志、操作项目管理工具等。MCP 服务分两大类，安装命令写法不同，先搞清楚这个，后面所有 MCP 都能举一反三。
这部分的内容有相当比例我是用AI写的，主要是想和大家一块学习一下mcp安装命令，如果大家觉得无用的信息太多，可以自行忽略，直接运行命令执行安装即可，谢谢大家🙏。

### 5.1 两种 MCP 类型

| 类型 | 运行方式 | 典型场景 | 命令写法 |
|------|----------|----------|----------|
| **本地 stdio** | 在你电脑上启动一个本地进程 | Chrome DevTools、文件操作、本地数据库 | `claude mcp add <名称> -- <启动命令>` |
| **远程 HTTP** | 请求云端的 MCP 服务 URL | Sentry、Monday、Linear 等 SaaS 工具 | `claude mcp add --transport http <名称> <URL>` |

两种写法的核心区别：
- 本地 stdio：用 `--`（双破折号）隔开服务名和本地启动命令，AI 会在后台帮你启动这个进程
- 远程 HTTP：加 `--transport http` 指定传输方式，后面直接跟服务的 URL，不需要 `--`

### 5.2 本地 stdio 类型：以 Chrome DevTools 为例

**不需要**用特殊命令启动 Chrome，日常双击图标打开就行。`chrome-devtools-mcp` 带有 `--autoConnect` 参数，会自动检测并连接到已运行的 Chrome 实例。

```bash
claude mcp add --scope user chrome-devtools -- npx -y chrome-devtools-mcp@latest --autoConnect --channel=stable
```

**命令各部分含义：**

| 部分 | 含义 |
|------|------|
| `--scope user` | 对当前用户所有项目生效（写入 `~/.claude.json`） |
| `chrome-devtools` | 服务的本地别名（自定义名称） |
| `--` | 分隔符，后面是在本地启动 MCP 进程的命令 |
| `npx -y chrome-devtools-mcp@latest` | 通过 npx 运行 MCP 包 |
| `--autoConnect --channel=stable` | 自动连接到已打开的 Chrome 稳定版 |

配置完成后，进入 `claude` 会话，MCP 服务会自动在后台启动并连接 Chrome。你可以直接说：

- "帮我看一下当前页面有没有控制台报错"
- "抓一下 /api/login 这个接口的请求和响应"
- "截图告诉我页面哪里布局乱了"
- "查一下有没有失败的网络请求（4xx/5xx）"

AI 会主动调用工具取数据，不需要你手动打开 DevTools 再复制过来。

### 5.3 远程 HTTP 类型：以 Sentry 为例

越来越多的 SaaS 工具提供了官方 MCP 接口，只需要一行命令就能接入：

```bash
# Sentry：查看错误日志和 Issue
claude mcp add --transport http --scope user sentry https://mcp.sentry.dev/mcp

# Monday：管理项目任务
claude mcp add --transport http --scope user monday https://mcp.monday.com/mcp

# Linear：管理 Issue 和里程碑
claude mcp add --transport http --scope user linear https://mcp.linear.app/mcp
```

**命令各部分含义：**

| 部分 | 含义 |
|------|------|
| `--transport http` | 指定通过 HTTP 协议与远端 MCP 服务通信 |
| `--scope user` | 对当前用户所有项目生效 |
| `sentry` / `monday` | 服务的本地别名（自定义名称） |
| `https://mcp.sentry.dev/mcp` | 远端 MCP 服务的 URL |

> **关于认证：** 大多数远程 MCP 服务需要登录授权。首次在 `claude` 会话中使用该工具时，会自动跳转 OAuth 认证页面，完成一次授权即可。

接入后，在 `claude` 会话里可以直接说：

- "帮我看一下 Sentry 里最近的错误，有没有跟登录相关的"
- "把 Monday 里这周到期的任务列一下"
- "在 Linear 里帮我创建一个 Bug Issue：标题是 '登录页超时'"

### 5.4 scope 参数说明

每次添加 MCP 时都需要选择 scope（作用范围）：

| 参数 | 配置存储位置 | 适用场景 |
|------|-------------|----------|
| `--scope user` | `~/.claude.json` | 个人工具，跨所有项目可用（推荐） |
| `--scope project` | 项目根目录 `.mcp.json` | 团队共享，可提交到 Git，所有成员共用 |
| 不加（默认 local） | `~/.claude.json`（按项目路径隔离） | 当前项目私用，不影响其他项目 |

### 5.5 验证配置

```bash
claude mcp list
```

---

## 六、常见问题

| 问题 | 解决方式 |
|------|----------|
| 提示不支持国区 | 跳过登录，然后配置 API Key |
| 登录后无法使用 | 确认网络环境正常，必要时检查代理设置 |
| API Key 无效 | 检查 Key 是否正确、是否过期、是否有可用额度 |
| Node 版本太低 | 升级到 22+ 再重试 |
| 修改配置后不生效 | 确保完全重启了命令行窗口 |
| MCP 启动失败 | 用 `claude mcp add` 命令添加，不要手动改 JSON |
| AI 说无法连接 Chrome | 确认 Chrome 已打开，`--autoConnect` 会自动连接 |
| 连上了但工具没响应 | 在 Chrome 里打开目标页面后再操作 |

## 七、写在最后

如果你是第一次接触 AI 编程终端，不用紧张。先跑通安装和登录，再在真实项目里让它帮你做"小任务"。接入 Chrome DevTools 之后，调试 Web 问题的方式会有明显变化——AI 能直接"看到"浏览器里发生了什么，不再需要你在 DevTools 和对话框之间来回搬运信息。
