# 小白也能用的 AI 编程终端：10 分钟上手 Claude Code

如果你经常写代码、改 Bug、看日志，AI 终端会比网页对话更高效。
今天这篇不讲概念，直接上手 Claude Code：从安装到可用，一步步带你跑通。

官方文档：
https://docs.anthropic.com/zh-CN/docs/claude-code/overview

## 一、先说结论：你将完成什么？

看完这篇，你可以做到：

- 安装并启动 Claude Code
- 完成账号登录或 API Key 配置
- 把命令加入 PATH（终端里直接调用）
- 在本地项目里完成第一次 AI 协作

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

Node.js 官网：
https://nodejs.org/en/download

### 2.2 安装 Claude Code

```bash
# 全局安装 Claude Code
npm i -g @anthropic-ai/claude-code

# 验证是否安装成功
claude --version
```

## 三、首次登录（两种方式）

### 方式 A：账号登录（推荐新手）

```bash
claude
```

首次启动后按终端提示完成登录授权即可。

### 方式 B：使用 API Key

```bash
# 临时生效（当前终端窗口）
export ANTHROPIC_API_KEY="你的API密钥"

# 永久生效（zsh）
echo 'export ANTHROPIC_API_KEY="你的API密钥"' >> ~/.zshrc
source ~/.zshrc
```

## 四、在项目里开始使用

进入你的代码目录，直接运行：

```bash
claude
```

你可以先试几个简单指令：

- “帮我读一下这个项目的目录结构”
- “定位登录模块在哪里”
- “把某个报错的根因分析出来”
- “按现有风格补一个单元测试”

## 五、常见坑位提醒（帮你省时间）

- `claude: command not found`：通常是 PATH 没生效，重开终端或执行 `source ~/.zshrc`
- 登录后无法使用：确认网络环境正常，必要时检查代理设置
- API Key 无效：检查 Key 是否正确、是否过期、是否有可用额度
- Node 版本太低：升级到 22+ 再重试

## 六、写在最后

如果你是第一次接触 AI 编程终端，不用紧张。
先跑通安装和登录，再在真实项目里让它帮你做“小任务”，很快就能形成自己的高效工作流。
