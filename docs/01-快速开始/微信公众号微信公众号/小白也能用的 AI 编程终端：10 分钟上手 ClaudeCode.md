# 小白也能用的 AI 编程终端：10 分钟上手 Claude Code

如果你经常写代码、改 Bug、看日志，AI 终端会比网页对话更高效。今天这篇不讲概念，直接上手 Claude Code：从安装到可用，一步步带你跑通。

官方文档：https://docs.anthropic.com/zh-CN/docs/claude-code/overview

## 一、先说结论：你将完成什么？

看完这篇，你可以做到：

- 安装并启动 Claude Code
- 完成账号登录或 API Key 配置
- 把命令加入 PATH（终端里直接调用）
- 在本地项目里完成第一次 AI 协作
- 接入 Chrome DevTools，让 AI 直接读取浏览器里的报错和请求

## 二、准备环境（新手直接复制）

### 2.1 安装 Node.js（版本 >= 22）

推荐使用 nvm 管理版本：

```bash
# 安装 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.4/install.sh | bash

# 加载 nvm（避免重启终端）
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
# 全局安装 Claude Code（不要加 sudo）
npm i -g @anthropic-ai/claude-code

# 验证是否安装成功
claude --version
```

> **注意：不要用 `sudo npm i -g`**
> 用 root 权限安装后，全局包会写入系统目录，之后每次运行 `claude` 都需要 sudo。
> 使用 nvm 管理 Node.js 时，全局包安装在 `~/.nvm/` 下，普通用户即可正常使用。
> 如果之前已经用 sudo 装过，先卸载再重装：
> ```bash
> sudo npm uninstall -g @anthropic-ai/claude-code
> npm i -g @anthropic-ai/claude-code
> ```

## 三、账号登录

```bash
claude
```

如果提示不支持国区或无法完成登录，可以通过修改配置文件跳过登录流程。

```bash
# 编辑配置文件
vim ~/.claude.json
```

在文件中添加以下内容（如果文件已有内容，在最后一个 `}` 之前添加这一行）：

```json
"hasCompletedOnboarding": true
```

编辑完成后，按 `Esc`，输入 `:wq` 保存并退出编辑器，然后重启命令行即可跳过登录。

> **注意：** 跳过登录后仍需配置 API Key 才能正常使用 AI 功能。

### 使用 API Key

Claude Code 会自动读取 `~/.claude/settings.json` 中的配置。如果你购买的是官方 API，直接将 Key 填入下方即可：

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

如果你使用的是国内中转或第三方兼容服务，可以额外添加自定义 API 地址：

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

## 五、接入 Chrome DevTools（进阶）

Claude Code 支持 MCP（Model Context Protocol），可以通过 `chrome-devtools-mcp` 让 AI 直接读取浏览器的控制台日志、网络请求、DOM 结构，不需要手动复制粘贴。

### 5.1 正常打开 Chrome 即可

**不需要**用特殊命令启动 Chrome，日常双击图标打开就行。`chrome-devtools-mcp` 带有 `--autoConnect` 参数，会自动检测并连接到已运行的 Chrome 实例。

### 5.2 配置 MCP

在项目目录下运行：

```bash
# 添加到当前项目（推荐，进项目就生效）
claude mcp add chrome-devtools -- npx -y chrome-devtools-mcp@latest --autoConnect --channel=stable

# 验证是否连接成功
claude mcp list
```

如果想对所有项目都生效，加 `--scope global`：

```bash
claude mcp add --scope global chrome-devtools -- npx -y chrome-devtools-mcp@latest --autoConnect --channel=stable
```

配置完成后，进入 `claude` 会话，MCP 服务会自动启动并连接 Chrome。

> **为什么用命令而不是手动改 JSON？** `claude mcp add` 会写入正确的配置文件，手动编辑 `settings.json` 中的 `mcpServers` 字段有时不会被 `claude mcp list` 识别。

### 5.3 接入后能做什么

配置完成后，在 Claude Code 对话里可以直接说：

- "帮我看一下当前页面有没有控制台报错"
- "抓一下 /api/login 这个接口的请求和响应"
- "截图告诉我页面哪里布局乱了"
- "在当前页面执行这段 JS：`document.title`"
- "查一下有没有失败的网络请求（4xx/5xx）"

AI 会主动调用工具去取数据，不需要你手动打开 DevTools 再复制过来。

---

## 六、常见坑位提醒（帮你省时间）

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
