# 🔥 Claude Code 免费接入 Gemini 保姆级教程！

小伙伴们，今天给大家带来一个超实用的教程！Anthropic 官方的 Claude Code 居然可以用上 Google 的 Gemini 模型了！而且完全免费～

## 🤔 什么是"代理"？

简单来说，**代理就像一个翻译官**：

- Claude Code 本来只会说"Anthropic 语言"
- Gemini 只听得懂"Google 语言"
- 代理在中间当翻译，让它们互相能沟通

这样我们就能用 Claude Code 的界面，享受 Gemini 的算力了！

## ❓ 为什么不能直接用 Gemini 的 Base URL？

很多小伙伴问：Gemini 没有自己的 API 地址吗？为什么非要用代理？

**答案是：有！但 Claude Code 听不懂 Gemini 的"方言"！**

### Gemini 的官方 Base URL

```
https://generativelanguage.googleapis.com/v1beta
```

这个地址是真实存在的！但问题是...

### API 格式对比表

| 对比项       | Claude Code (Anthropic)               | Gemini                                   |
| ------------ | ------------------------------------- | ---------------------------------------- |
| **Base URL** | `/v1/messages`                        | `/v1beta/models/{model}:generateContent` |
| **请求格式** | `{"model": "...", "messages": [...]}` | `{"contents": [...]}`                    |
| **响应格式** | Anthropic 格式                        | Gemini 格式                              |

### 实际例子对比

**Claude Code 发的请求：**

```json
{
  "model": "claude-sonnet-4-6",
  "messages": [{"role": "user", "content": "你好"}]
}
```

**Gemini 能懂的请求：**

```json
{
  "contents": [{"role": "user", "parts": [{"text": "你好"}]}]
}
```

**完全不一样！** 如果让 Claude Code 直接连 Gemini，Gemini 会说："你说啥？听不懂！" 😵

### 这就是为什么必须有代理

```
Claude Code          代理（翻译官）         Gemini
    │                    │                    │
    │  Anthropic 格式    │                    │
    ├───────────────────>│                    │
    │                    │  转换成 Gemini 格式 │
    │                    ├───────────────────>│
    │                    │                    │
    │                    │  Gemini 格式响应   │
    │                    │<───────────────────┤
    │  Anthropic 格式响应 │                    │
    │<───────────────────┤                    │
```

---

## 🔧 技术原理深度解析

### 整体流程图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        本地代理 (server.py)                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────┐   (1) 请求   ┌──────────────┐   (2) 翻译   ┌─────────┐
│  │Claude    │ ───────────> │  FastAPI     │ ───────────> │ LiteLLM │
│  │Code      │  Anthropic    │  /v1/messages│  OpenAI格式   │  库     │
│  └──────────┘   格式        └──────────────┘               └────┬────┘
│       ^                                                             │
│       │                    (5) 返回                                │
│       └─────────────────────────────────────────────────────────────┘
│                                                                      │
│                                      (3) 调用                        │
│                              ┌──────────────────┐                    │
│                              │   Gemini API     │                    │
│                              │  (Google 服务器)  │                    │
│                              └─────────┬────────┘                    │
│                                        │ (4) 响应                      │
│                                        └──────────────────────────────┘
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 接口调用链路

其实就是**三个 HTTP 接口在互相调用**：

```
Claude Code              本地代理 (8082)           Gemini 服务器
    │                        │                         │
    │  POST /v1/messages     │                         │
    ├───────────────────────>│                         │
    │   Anthropic 格式       │                         │
    │                        │                         │
    │                        │  POST /v1/models:...   │
    │                        ├────────────────────────>│
    │                        │   Gemini 格式           │
    │                        │                         │
    │                        │       JSON 响应         │
    │                        │<────────────────────────┤
    │                        │                         │
    │      JSON 响应         │                         │
    │<───────────────────────┤                         │
    │   Anthropic 格式       │                         │
```

### 核心衔接代码（5 步走）

**步骤 1：接收请求**

```python
# Claude Code 发请求到代理
@app.post("/v1/messages")
async def create_message(request: MessagesRequest):
    # 收到: {"model": "claude-sonnet-4-6", "messages": [...]}
```

**步骤 2：模型名映射**

```python
# 把 "claude-sonnet" 改成 "gemini-2.0-flash-exp"
if 'sonnet' in clean_v.lower():
    new_model = f"gemini/{BIG_MODEL}"
```

**步骤 3：格式转换**

```python
# Anthropic 格式 → OpenAI/LiteLLM 格式
def convert_anthropic_to_litellm(anthropic_request):
    return litellm_request
```

**步骤 4：调用 Gemini**

```python
# 用 LiteLLM 库调用 Gemini API
litellm_response = litellm.completion(**litellm_request)
```

**步骤 5：返回响应**

```python
# Gemini 响应 → Anthropic 格式
def convert_litellm_to_anthropic(litellm_response):
    return anthropic_response
```

### 最关键的角色：LiteLLM

这个代理的核心不是自己写翻译，而是用了 **LiteLLM** 这个万能库：

```
Claude Code → 代理 → LiteLLM → Gemini
                      ↓
                  也支持 → GPT
                  也支持 → Claude
                  也支持 → 100+ 种模型！
```

LiteLLM 帮我们处理了所有不同 API 的兼容性问题！

---

## 🔍 Claude Code 如何请求 localhost:8082？

很多小伙伴好奇，Claude Code 是怎么知道要去请求本地代理的？让我们一步步拆解！

### 第 1 步：读取配置文件

Claude Code 启动时第一件事就是读配置：`~/.claude/settings.json`

```json
{
  "env": {
    "ANTHROPIC_BASE_URL": "http://localhost:8082",
    "ANTHROPIC_API_KEY": "dummy-key"
  }
}
```

**关键配置项：**

- `ANTHROPIC_BASE_URL`：告诉 Claude Code "API 服务器在哪里"
- **默认值**是 `https://api.anthropic.com`（官方服务器）
- **我们改后**变成 `http://localhost:8082`（本地代理）

就这么简单！改个配置项，Claude Code 就找"错"服务器了 😏

---

### 第 2 步：发送 HTTP POST 请求

当你输入问题时，Claude Code 会发送一个标准的 HTTP 请求：

```
POST http://localhost:8082/v1/messages
Content-Type: application/json
x-api-key: dummy-key
anthropic-version: 2023-06-01

{
  "model": "claude-sonnet-4-6",
  "max_tokens": 4096,
  "messages": [
    {"role": "user", "content": "你好"}
  ],
  "stream": true
}
```

**这就是普通的 Web 请求！** 就像浏览器访问网页一样。

---

### 第 3 步：本地代理接收请求

代理服务器（uvicorn + FastAPI）在 8082 端口监听：

```python
# server.py 第 1095 行
@app.post("/v1/messages")
async def create_message(request: MessagesRequest, raw_request: Request):
    # 这里接收到了 Claude Code 的请求！
    body = await raw_request.body()
    body_json = json.loads(body.decode('utf-8'))
    # body_json 就是上面那个 JSON
```

---

### 第 4 步：自己测试一下（curl 命令）

你可以用 curl 命令模拟 Claude Code 的请求：

```bash
curl -X POST http://localhost:8082/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: dummy-key" \
  -H "anthropic-version: 2023-06-01" \
  -d '{
    "model": "claude-sonnet-4-6",
    "max_tokens": 100,
    "messages": [{"role": "user", "content": "你好"}],
    "stream": false
  }'
```

这和 Claude Code 做的事情**完全一样**！你会直接看到 Gemini 的回复。

---

### 完整序列图

```
┌─────────────┐                    ┌──────────────┐                    ┌──────────────┐
│ Claude Code │                    │  本地代理     │                    │   Gemini     │
│   (客户端)   │                    │  (localhost)  │                    │   (服务器)    │
└──────┬──────┘                    └──────┬───────┘                    └──────┬───────┘
       │                                   │                                   │
       │  1. 读取 settings.json           │                                   │
       │     ANTHROPIC_BASE_URL =         │                                   │
       │     "http://localhost:8082"     │                                   │
       │                                   │                                   │
       │  2. POST /v1/messages            │                                   │
       ├──────────────────────────────────>│                                   │
       │  ┌─────────────────────────┐    │                                   │
       │  │ {                       │    │                                   │
       │  │   "model": "claude-...",│    │                                   │
       │  │   "messages": [...]     │    │                                   │
       │  │ }                       │    │                                   │
       │  └─────────────────────────┘    │                                   │
       │                                   │  3. 验证请求、转换格式            │
       │                                   │  4. 模型映射: claude → gemini    │
       │                                   │                                   │
       │                                   │  5. POST Gemini API               │
       │                                   ├──────────────────────────────────>│
       │                                   │                                   │
       │                                   │  6. Gemini 返回 JSON              │
       │                                   │<──────────────────────────────────┤
       │                                   │                                   │
       │                                   │  7. 转换回 Anthropic 格式        │
       │                                   │                                   │
       │  8. 返回 JSON (或 SSE 流)        │                                   │
       │<──────────────────────────────────┤                                   │
       │                                   │                                   │
```

---

### 总结

**就是这么简单，4 步搞定：**

1. **Claude Code** 从配置文件知道要连 `localhost:8082`（而不是官方服务器）
2. **Claude Code** 发送标准的 HTTP POST 请求到 `/v1/messages`
3. **本地代理** 接收请求，用 LiteLLM 转发给 Gemini
4. **本地代理** 把 Gemini 的响应包装一下发回去给 Claude Code

**这就是普通的 Web 客户端-服务器通信！** 没有魔法，全是 HTTP 🎯

---

## 🎯 准备工作

1. 一个 Gemini API Key（去 Google AI Studio 申请，免费！）
2. Mac 电脑（Windows 原理类似）
3. 一点点耐心 😊

---

## 🔑 官方指南：如何获取 Gemini API Key

根据 Google 官方文档，获取和使用 Gemini API Key 有一套标准流程。

### 第一步：创建 API Key

访问 **Google AI Studio** 的 API Keys 页面：

- 🔗 直达链接：https://aistudio.google.com/app/apikey

**好消息！** 对于新用户，Google AI Studio 会自动：

1. 创建一个默认的 Google Cloud Project
2. 生成一个默认的 API Key
3. 你直接拿来用就行！

### 第二步：管理你的项目

每个 API Key 都关联一个 Google Cloud 项目。你可以：

- 在 Google AI Studio 的 **Projects** 页面管理项目
- 重命名项目（点击项目旁的三个点 → Rename project）
- 导入已有的 Google Cloud 项目

**限制说明：**

- 最多可以创建 10 个项目
- API Keys 页面最多显示 100 个 Key
- Projects 页面最多显示 50 个项目

### 第三步：配置环境变量（推荐方式）

官方推荐设置环境变量，这样 SDK 会自动读取：

**环境变量名称（二选一）：**

- `GEMINI_API_KEY` （推荐）
- `GOOGLE_API_KEY` （优先级更高）

**Mac/Linux (Bash) 设置方法：**

```bash
# 1. 编辑配置文件
open ~/.bashrc

# 2. 添加这一行（替换成你的 Key）
export GEMINI_API_KEY=AIzaSyDoTAWgO_vJ3_HFdBkIz0IUBcFgzsx3Gw0

# 3. 保存后生效
source ~/.bashrc
```

**Mac (Zsh) 设置方法：**

```bash
# 1. 编辑配置文件
open ~/.zshrc

# 2. 添加这一行
export GEMINI_API_KEY=AIzaSyDoTAWgO_vJ3_HFdBkIz0IUBcFgzsx3Gw0

# 3. 保存后生效
source ~/.zshrc
```

### 安全警告 ⚠️

官方特别强调的安全规则：

| 规则                      | 说明                                    |
| ------------------------- | --------------------------------------- |
| **绝对不要** 提交到 Git   | API Key 像密码一样，绝不能进版本控制    |
| **绝对不要** 暴露在客户端 | 不要在网页或移动应用中直接使用          |
| **限制使用范围**          | 在 Google Cloud Console 限制 IP、应用等 |
| **只启用必要的 API**      | 别让一个 Key 能调用所有服务             |
| **定期轮换**              | 经常检查并更换 Key                      |

**最佳实践：**

- 服务器端调用最安全
- 客户端考虑用临时令牌（Live API）
- 添加 API Key 限制减少泄露风险

---

## 🤖 Gemini 官方模型家族

Google 官方提供了多个模型，各有特色：

| 模型                      | 定位     | 特点                                   |
| ------------------------- | -------- | -------------------------------------- |
| **Gemini 3.1 Pro**        | 最智能   | 多模态理解最强，推理能力顶尖           |
| **Gemini 3 Flash**        | 性价比   | 前沿性能，成本只是大模型的零头         |
| **Gemini 3.1 Flash-Lite** | 工作马   | 高并发场景优选，保持 Gemini 3 系列质量 |
| **Nano Banana**           | 图像生成 | 最先进的图像生成和编辑                 |
| **Veo 3.1**               | 视频生成 | 带原生音频的视频生成                   |
| **Gemini Robotics**       | 机器人   | 视觉语言模型，赋能物理世界推理         |

除了这些，还有：

- **Lyria 3** - 音乐生成
- **Imagen** - 图像生成
- **Text-to-speech** - 语音合成
- **Embeddings** - 向量嵌入

### 主要能力

- 📝 **文本生成** - 对话、写作、编码
- 🖼️ **图像理解** - 看图说话、分析图片
- 📄 **文档处理** - 最多 1000 页 PDF
- 🧠 **思考模式** - 复杂任务推理增强
- 🔧 **函数调用** - 连接外部 API 和工具
- 📚 **长上下文** - 支持百万级 Token
- 🎯 **结构化输出** - 强制返回 JSON 格式

---

## 💻 官方快速入门示例

如果你想用官方 SDK 直接测试（不是通过 Claude Code），官方提供了标准方法：

### 环境要求

- Python 3.9+

### 安装官方 SDK

```bash
pip install -q -U google-genai
```

### 最简单的测试代码

```python
from google import genai

# 客户端自动从环境变量 GEMINI_API_KEY 读取 Key
client = genai.Client()

response = client.models.generate_content(
    model="gemini-3-flash-preview",
    contents="Explain how AI works in a few words"
)

print(response.text)
```

这就是官方推荐的标准用法！

## 🛠️ 步骤一：安装 uv

uv 是一个超级快的 Python 包管理工具，我们用它来运行代理服务。

打开终端，输入：

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

如果你在国内，可能需要配置代理：

```bash
export https_proxy=http://127.0.0.1:7890
curl -LsSf https://astral.sh/uv/install.sh | sh
```

## 🛠️ 步骤二：下载代理项目

```bash
cd ~/IdeaProjects
git clone https://github.com/1rgs/claude-code-proxy
cd claude-code-proxy
```

## 🛠️ 步骤三：配置 API Key

在项目目录下创建 `.env` 文件：

```bash
GEMINI_API_KEY=你的Gemini密钥
PREFERRED_PROVIDER=google
BIG_MODEL=gemini-2.0-flash-exp
SMALL_MODEL=gemini-2.0-flash-exp
```

把 `你的Gemini密钥` 替换成你在 Google AI Studio 申请的 Key。

## 🛠️ 步骤四：启动代理服务

```bash
uv run uvicorn server:app --host 0.0.0.0 --port 8082
```

看到类似这样的输出就说明启动成功了：

```
INFO:     Uvicorn running on http://0.0.0.0:8082
```

让它在后台运行，不要关掉这个终端窗口。

## 🛠️ 步骤五：配置 Claude Code

编辑 `~/.claude/settings.json` 文件：

```json
{
  "env": {
    "ANTHROPIC_BASE_URL": "http://localhost:8082",
    "ANTHROPIC_API_KEY": "dummy-key",
    "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": "1"
  },
  "model": "sonnet",
  "skipDangerousModePermissionPrompt": true
}
```

## 🎉 完成！

现在重启 Claude Code，你就会发现它在用 Gemini 模型回答问题了！

## 💡 常用命令

**查看代理是否在运行：**

```bash
ps aux | grep uvicorn
```

**停止代理：**

```bash
ps aux | grep uvicorn | grep -v grep | awk '{print $2}' | xargs kill
```

**以后重新启动代理：**

```bash
cd ~/IdeaProjects/claude-code-proxy
uv run uvicorn server:app --host 0.0.0.0 --port 8082 &
```

## 📌 注意事项

1. 代理服务必须一直运行才能用
2. Gemini 免费版有额度限制，省着点用
3. 国内访问可能需要科学上网

---

*觉得有用就点个赞吧！有问题评论区留言～*
