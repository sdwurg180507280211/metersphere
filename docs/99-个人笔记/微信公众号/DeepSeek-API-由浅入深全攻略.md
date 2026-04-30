# DeepSeek API 从零到精通：一份递归遍历官网文档的全攻略

> 一条路径走完：申请 Key → 首次调用 → 思考模式 → 工具调用 → Agent 接入。每节一个阶段目标，跟完即拥有可落地的 DeepSeek API 开发能力。

**你将完成什么：**

| 阶段 | 目标 | 章节 |
| --- | --- | --- |
| 入门篇 | 拿到 Key、跑通第一次调用、搞清计费 | 1-3 |
| 核心篇 | 思考模式、多轮对话、JSON/Tool Calls | 4-7 |
| 高级篇 | FIM 补全、前缀续写、硬盘缓存 | 8-10 |
| 生态篇 | Anthropic 兼容、Agent 工具接入 | 11-13 |

---

# 入门篇：先跑起来

## 1. 五分钟完成首次调用

> **本节目标**：终端里发出请求，收到 DeepSeek 的回复。

### Step 1：申请 API Key

前往 [platform.deepseek.com/api_keys](https://platform.deepseek.com/api_keys)，登录后点击「创建 API Key」。复制保存，只显示一次。

### Step 2：选模型

| 模型 | 定位 | 上下文 | 最大输出 | 价格（输入/输出，每百万 Token） |
| --- | --- | --- | --- | --- |
| `deepseek-v4-flash` | 轻量快速 | 1M | 384K | 1 元 / 2 元 |
| `deepseek-v4-pro` | 深度推理 | 1M | 384K | 3 元 / 6 元（2.5 折优惠至 2026/05/31） |

> 旧模型名 `deepseek-chat` 和 `deepseek-reasoner` 将于 2026/07/24 弃用，分别对应 v4-flash 的非思考与思考模式。

### Step 3：调用

**curl：**

```bash
curl https://api.deepseek.com/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${DEEPSEEK_API_KEY}" \
  -d '{
    "model": "deepseek-v4-pro",
    "messages": [
      {"role": "system", "content": "You are a helpful assistant."},
      {"role": "user", "content": "Hello!"}
    ],
    "stream": false
  }'
```

**Python：**

```python
# pip3 install openai
import os
from openai import OpenAI

client = OpenAI(
    api_key=os.environ.get('DEEPSEEK_API_KEY'),
    base_url="https://api.deepseek.com"
)

response = client.chat.completions.create(
    model="deepseek-v4-pro",
    messages=[
        {"role": "system", "content": "You are a helpful assistant"},
        {"role": "user", "content": "Hello"},
    ],
    stream=False,
)

print(response.choices[0].message.content)
```

**Node.js：**

```javascript
// npm install openai
import OpenAI from "openai";

const openai = new OpenAI({
    baseURL: 'https://api.deepseek.com',
    apiKey: process.env.DEEPSEEK_API_KEY,
});

async function main() {
  const completion = await openai.chat.completions.create({
    messages: [{ role: "system", content: "You are a helpful assistant." }],
    model: "deepseek-v4-pro",
    stream: false,
  });
  console.log(completion.choices[0].message.content);
}

main();
```

看到模型回复即成功。把 `stream` 设为 `true` 可切换流式输出。

---

## 2. 计费 & Token：别让账单吓到你

> **本节目标**：理解 Token 计费规则，能估算成本。

### Token 换算

| 语言 | 换算比例 |
| --- | --- |
| 英文 | 1 个字符 ≈ 0.3 Token |
| 中文 | 1 个字符 ≈ 0.6 Token |

实际以 API 返回的 `usage` 字段为准。离线计算可用官方 tokenizer：[deepseek_v3_tokenizer.zip](https://cdn.deepseek.com/api-docs/deepseek_v3_tokenizer.zip)。

### 缓存命中 = 便宜 10-120 倍

| 计费项 | deepseek-v4-flash | deepseek-v4-pro |
| --- | --- | --- |
| 输入（缓存命中） | **0.02 元**/百万 Token | **0.025 元**/百万 Token |
| 输入（缓存未命中） | 1 元/百万 Token | 3 元/百万 Token |
| 输出 | 2 元/百万 Token | 6 元/百万 Token |

缓存命中价已降至首发 1/10，多轮对话场景成本极低。详见第 10 节「硬盘缓存」。

### 扣费规则

- 费用 = Token 消耗量 × 模型单价
- 充值余额与赠送余额同时存在时，**优先扣赠送余额**
- 充值余额永不过期，赠送余额有期限（在「账单」页查看）

---

## 3. 限速 & 错误码：遇到问题不慌

> **本节目标**：知道 429/503 怎么处理，知道哪些错误是自己的锅。

### 限速

DeepSeek 采用**动态限流**——没有固定 RPM/TPM 上限，根据系统负载实时调整。

| 情况 | 表现 |
| --- | --- |
| 被限流 | HTTP 429 |
| 等待中（非流式） | 持续返回空行 |
| 等待中（流式） | 持续返回 `: keep-alive` |
| 等待超时 | 10 分钟未开始推理，服务器关闭连接 |

处理方式：加指数退避重试，或用流式输出提升体感速度。

### 错误码速查

| 错误码 | 含义 | 怎么办 |
| --- | --- | --- |
| 400 | 请求体格式错误 | 根据错误提示改请求体 |
| 401 | API Key 错误 | 检查 Key 是否正确 |
| 402 | 余额不足 | 去[充值](https://platform.deepseek.com/top_up) |
| 422 | 参数错误 | 根据错误提示改参数 |
| 429 | 请求速率达上限 | 降低并发，加重试 |
| 500 | 服务器故障 | 等待后重试，持续则联系官方 |
| 503 | 服务器繁忙 | 稍后重试 |

---

# 核心篇：掌握 DeepSeek 的杀手级能力

## 4. 思考模式：让模型"想清楚再回答"

> **本节目标**：理解思考模式的开关、强度、上下文拼接规则。

DeepSeek 模型默认开启思考模式——输出最终回答前，先输出一段思维链（`reasoning_content`），提升准确性。

### 开关与强度

| 功能 | OpenAI 格式 | Anthropic 格式 |
| --- | --- | --- |
| 思考开关 | `{"thinking": {"type": "enabled/disabled"}}` | — |
| 思考强度 | `{"reasoning_effort": "high/max"}` | `{"output_config": {"effort": "high/max"}}` |

- 默认开关为 `enabled`，普通请求默认 effort 为 `high`
- Agent 类请求（如 Claude Code）自动设为 `max`
- `low`/`medium` 兼容映射为 `high`，`xhigh` 映射为 `max`

Python 示例：

```python
response = client.chat.completions.create(
    model="deepseek-v4-pro",
    messages=[...],
    reasoning_effort="high",
    extra_body={"thinking": {"type": "enabled"}}
)
```

### 不支持的参数

思考模式下 `temperature`、`top_p`、`presence_penalty`、`frequency_penalty` **不生效**（不报错但被忽略）。

### 上下文拼接规则

这是最容易踩坑的地方：

| 场景 | `reasoning_content` 处理 |
| --- | --- |
| 未进行工具调用 | **无需回传**，传了也会被忽略 |
| 进行了工具调用 | **必须回传**，否则 400 报错 |

简单记忆：**有工具调用必回传，无工具调用可省略**。

### 非流式调用示例

```python
from openai import OpenAI
client = OpenAI(api_key="<Key>", base_url="https://api.deepseek.com")

# Turn 1
messages = [{"role": "user", "content": "9.11 and 9.8, which is greater?"}]
response = client.chat.completions.create(
    model="deepseek-v4-pro",
    messages=messages,
    reasoning_effort="high",
    extra_body={"thinking": {"type": "enabled"}},
)
reasoning_content = response.choices[0].message.reasoning_content
content = response.choices[0].message.content

# Turn 2 — reasoning_content 被忽略
messages.append(response.choices[0].message)
messages.append({'role': 'user', 'content': "How many Rs are there in 'strawberry'?"})
response = client.chat.completions.create(
    model="deepseek-v4-pro",
    messages=messages,
    reasoning_effort="high",
    extra_body={"thinking": {"type": "enabled"}},
)
```

---

## 5. 多轮对话：服务端无状态，客户端拼接

> **本节目标**：掌握多轮对话的标准拼接方式。

DeepSeek `/chat/completions` 是**无状态 API**——服务端不记上下文，客户端每次要把完整历史带上。

```python
from openai import OpenAI
client = OpenAI(api_key="<Key>", base_url="https://api.deepseek.com")

# Round 1
messages = [{"role": "user", "content": "What's the highest mountain in the world?"}]
response = client.chat.completions.create(
    model="deepseek-v4-pro",
    messages=messages
)
messages.append(response.choices[0].message)

# Round 2
messages.append({"role": "user", "content": "What is the second?"})
response = client.chat.completions.create(
    model="deepseek-v4-pro",
    messages=messages
)
messages.append(response.choices[0].message)
```

核心就一句话：**每次请求把 `messages` 列表原样带上，再追加新消息**。

---

## 6. JSON Output：让模型输出结构化数据

> **本节目标**：强制模型输出合法 JSON。

### 三个要点

1. 设置 `response_format` 为 `{'type': 'json_object'}`
2. system 或 user prompt 中**必须含 `json` 字样**，并给出 JSON 格式样例
3. 合理设置 `max_tokens`，防止 JSON 被截断

> 已知问题：有概率返回空 content，可通过优化 prompt 缓解。

```python
import json
from openai import OpenAI

client = OpenAI(api_key="<Key>", base_url="https://api.deepseek.com")

system_prompt = """
The user will provide some exam text. Please parse the "question" and "answer" and output them in JSON format.

EXAMPLE INPUT:
Which is the highest mountain in the world? Mount Everest.

EXAMPLE JSON OUTPUT:
{
    "question": "Which is the highest mountain in the world?",
    "answer": "Mount Everest"
}
"""

user_prompt = "Which is the longest river in the world? The Nile River."

messages = [{"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}]

response = client.chat.completions.create(
    model="deepseek-v4-pro",
    messages=messages,
    response_format={'type': 'json_object'}
)

print(json.loads(response.choices[0].message.content))
# {"question": "Which is the longest river in the world?", "answer": "The Nile River"}
```

---

## 7. Tool Calls：让模型调用外部工具

> **本节目标**：实现一个完整的工具调用流程。

### 非思考模式示例

```python
from openai import OpenAI

def send_messages(messages):
    response = client.chat.completions.create(
        model="deepseek-v4-pro",
        messages=messages,
        tools=tools
    )
    return response.choices[0].message

client = OpenAI(api_key="<Key>", base_url="https://api.deepseek.com")

tools = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "Get weather of a location.",
            "parameters": {
                "type": "object",
                "properties": {
                    "location": {
                        "type": "string",
                        "description": "The city and state, e.g. San Francisco, CA",
                    }
                },
                "required": ["location"]
            },
        }
    },
]

messages = [{"role": "user", "content": "How's the weather in Hangzhou?"}]
message = send_messages(messages)

tool = message.tool_calls[0]
messages.append(message)
messages.append({"role": "tool", "tool_call_id": tool.id, "content": "24℃"})

message = send_messages(messages)
print(message.content)
# "The current temperature in Hangzhou is 24°C."
```

### 执行流程

```
用户提问 → 模型返回 tool_call → 你执行函数 → 把结果回传 → 模型输出自然语言
```

> 模型本身不执行函数，你需要在客户端实现函数逻辑并回传结果。

### 思考模式 + Tool Calls

DeepSeek-V3.2 起支持思考模式下的工具调用。模型可进行多轮思考+工具调用。

**关键规则**：有工具调用的轮次，`reasoning_content` **必须回传**，否则 400 报错。

### strict 模式（Beta）

强制模型严格遵循 Function 的 JSON Schema：

| 要求 | 说明 |
| --- | --- |
| `base_url` | 必须设为 `https://api.deepseek.com/beta` |
| `strict` | 每个 function 设为 `true` |
| `additionalProperties` | 必须为 `false` |
| `required` | object 的所有属性均须列入 |

```json
{
    "type": "function",
    "function": {
        "name": "get_weather",
        "strict": true,
        "description": "Get weather of a location.",
        "parameters": {
            "type": "object",
            "properties": {
                "location": {"type": "string", "description": "The city"}
            },
            "required": ["location"],
            "additionalProperties": false
        }
    }
}
```

### strict 模式支持的 JSON Schema 类型

| 类型 | 支持的约束 | 不支持的约束 |
| --- | --- | --- |
| object | properties, required | — |
| string | pattern, format（email/hostname/ipv4/ipv6/uuid） | minLength, maxLength |
| number/integer | const, default, minimum, maximum, exclusiveMin/Max, multipleOf | — |
| array | items | minItems, maxItems |
| enum | — | — |
| anyOf | — | — |
| $ref/$def | 模块化引用 | — |

---

# 高级篇：进阶能力与省钱技巧

## 8. 对话前缀续写（Beta）：控制输出的开头

> **本节目标**：强制模型从指定前缀开始输出。

适用场景：强制输出代码块、控制回答格式。

### 三个要点

1. `messages` 最后一条 `role` 为 `assistant`，且设 `prefix: True`
2. `base_url` 改为 `https://api.deepseek.com/beta`
3. 用 `stop` 参数控制结束位置

```python
from openai import OpenAI

client = OpenAI(
    api_key="<Key>",
    base_url="https://api.deepseek.com/beta",
)

messages = [
    {"role": "user", "content": "Please write quick sort code"},
    {"role": "assistant", "content": "```python\n", "prefix": True}
]

response = client.chat.completions.create(
    model="deepseek-v4-pro",
    messages=messages,
    stop=["```"],
)

print(response.choices[0].message.content)
```

这样模型只会输出 Python 代码，不会带额外解释。

---

## 9. FIM 补全（Beta）：填中间，不是接尾巴

> **本节目标**：用 FIM 做代码补全。

FIM（Fill In the Middle）让你给前缀 + 后缀，模型补中间。常用于 IDE 代码补全。

### 两个要点

1. 最大补全长度 **4K Token**
2. `base_url` 改为 `https://api.deepseek.com/beta`

```python
from openai import OpenAI

client = OpenAI(
    api_key="<Key>",
    base_url="https://api.deepseek.com/beta",
)

response = client.completions.create(
    model="deepseek-v4-pro",
    prompt="def fib(a):",
    suffix="    return fib(a-1) + fib(a-2)",
    max_tokens=128
)

print(response.choices[0].text)
```

### 配合 Continue 插件

[Continue](https://continue.dev) 是 VSCode 代码补全插件，官方提供了[配置指南](https://github.com/deepseek-ai/awesome-deepseek-integration/blob/main/docs/continue/README_cn.md)，几步即可接入。

---

## 10. 上下文硬盘缓存：自动省钱，不用改代码

> **本节目标**：理解缓存命中规则，最大化命中概率。

硬盘缓存**默认开启，无需修改代码**。每个请求都会构建缓存，后续请求前缀匹配即可命中。

### 缓存命中规则

缓存以"缓存前缀单元"为粒度，只有**完整匹配**某个单元才能命中。

| 落盘时机 | 说明 |
| --- | --- |
| 请求结束位置 | 用户输入结束 + 模型输出结束，各产生一个单元 |
| 公共前缀检测 | 多次请求间存在公共前缀时，自动落盘 |
| 固定 Token 间隔 | 长输入/输出中按间隔截取，避免无法缓存 |

### 例一：多轮对话（直接命中）

```
请求1: system + "中国的首都是哪里？"
请求2: system + "中国的首都是哪里？" + assistant回复 + "美国的首都哪里？"
```

请求2 完整匹配请求1 的缓存前缀单元，命中。

### 例二：长文本问答（公共前缀命中）

```
请求1: system + <财报内容> + "请总结关键信息"
请求2: system + <财报内容> + "请分析盈利情况"
请求3: system + <财报内容> + "请分析收入支出占比"
```

请求1、2 不命中。但系统检测到公共前缀 `system + <财报内容>`，落盘。请求3 命中。

### 查看命中情况

API 返回的 `usage` 字段中：

| 字段 | 含义 |
| --- | --- |
| `prompt_cache_hit_tokens` | 缓存命中的 Token 数 |
| `prompt_cache_miss_tokens` | 缓存未命中的 Token 数 |

### 省钱技巧

- 把 system prompt 和长文档放在消息最前面，不要每次改
- 多轮对话自然命中率高（前几轮不变）
- 缓存命中价是未命中的 **1/50 到 1/120**，效果显著

---

# 生态篇：接入各种 Agent 工具

## 11. Anthropic API 兼容：用 Claude 的 SDK 调 DeepSeek

> **本节目标**：让已有 Anthropic 生态的代码无缝切换到 DeepSeek。

### 配置

```bash
export ANTHROPIC_BASE_URL=https://api.deepseek.com/anthropic
export ANTHROPIC_API_KEY=${YOUR_DEEPSEEK_API_KEY}
```

### 调用

```python
import anthropic

client = anthropic.Anthropic()
message = client.messages.create(
    model="deepseek-v4-pro",
    max_tokens=1000,
    system="You are a helpful assistant.",
    messages=[
        {
            "role": "user",
            "content": [{"type": "text", "text": "Hi, how are you?"}]
        }
    ]
)
print(message.content)
```

> 不支持的模型名会自动映射到 `deepseek-v4-flash`。

### 兼容性速查

| 字段 | 状态 | 字段 | 状态 |
| --- | --- | --- | --- |
| max_tokens | 完全支持 | temperature | 支持（0.0~2.0） |
| stream | 完全支持 | system | 完全支持 |
| stop_sequences | 完全支持 | top_p | 完全支持 |
| thinking | 支持（budget_tokens 忽略） | top_k | 忽略 |
| tool_choice（none/auto/any/tool） | 支持 | image/document | 不支持 |

---

## 12. 接入 Claude Code：让 DeepSeek 驱动 Claude 的终端

> **本节目标**：5 分钟把 Claude Code 的后端换成 DeepSeek。

### Step 1：安装 Claude Code

```bash
npm install -g @anthropic-ai/claude-code
claude --version
```

### Step 2：配置环境变量

**Linux / Mac：**

```bash
export ANTHROPIC_BASE_URL=https://api.deepseek.com/anthropic
export ANTHROPIC_AUTH_TOKEN=<你的 DeepSeek API Key>
export ANTHROPIC_MODEL=deepseek-v4-pro[1m]
export ANTHROPIC_DEFAULT_OPUS_MODEL=deepseek-v4-pro[1m]
export ANTHROPIC_DEFAULT_SONNET_MODEL=deepseek-v4-pro[1m]
export ANTHROPIC_DEFAULT_HAIKU_MODEL=deepseek-v4-flash
export CLAUDE_CODE_SUBAGENT_MODEL=deepseek-v4-flash
export CLAUDE_CODE_EFFORT_LEVEL=max
```

**Windows PowerShell：**

```powershell
$env:ANTHROPIC_BASE_URL="https://api.deepseek.com/anthropic"
$env:ANTHROPIC_AUTH_TOKEN="<你的 DeepSeek API Key>"
$env:ANTHROPIC_MODEL="deepseek-v4-pro[1m]"
$env:ANTHROPIC_DEFAULT_OPUS_MODEL="deepseek-v4-pro[1m]"
$env:ANTHROPIC_DEFAULT_SONNET_MODEL="deepseek-v4-pro[1m]"
$env:ANTHROPIC_DEFAULT_HAIKU_MODEL="deepseek-v4-flash"
$env:CLAUDE_CODE_SUBAGENT_MODEL="deepseek-v4-flash"
$env:CLAUDE_CODE_EFFORT_LEVEL="max"
```

### Step 3：启动

```bash
cd /path/to/my-project
claude
```

从此 Claude Code 的推理能力由 DeepSeek 驱动，费用走 DeepSeek 的计费体系。

---

## 13. 接入 OpenCode & OpenClaw

> **本节目标**：用交互式方式快速接入。

### 接入 OpenCode

1. 下载安装 [OpenCode](https://opencode.ai/zh/download)，版本 >= v1.14.24
2. 执行 `opencode`
3. 输入 `/connect` → 选择 `deepseek` → 填入 API Key
4. 选择 `DeepSeek-V4-Pro` 模型，开始使用

### 接入 OpenClaw

1. 安装：

```bash
# Linux / Mac
curl -fsSL https://openclaw.ai/install.sh | bash

# Windows
iwr -useb https://openclaw.ai/install.ps1 | iex
```

2. 配置：执行 `openclaw onboard --install-daemon`，交互式选择 QuickStart → DeepSeek → 填入 API Key → 默认模型填 `deepseek-v4-pro`

3. 使用：

```bash
openclaw dashboard   # Web UI
openclaw tui         # 终端 UI
openclaw terminal    # 终端对话
```

---

# 附录

## A. FAQ 速查

| 问题 | 答案 |
| --- | --- |
| 账号被停用？ | 填写[申诉表单](https://trtgsjkv6r.feishu.cn/share/base/form/shrcn13OBmQ3oXJKYLdHjUfeDHh)，3 个工作日内审核 |
| 邮箱无法注册？ | 用 Gmail/Outlook/Hotmail/Yahoo 等 |
| 注销账号？ | 个人信息页 → 注销，余额清零不可恢复 |
| 个人认证改企业？ | 充值页 → 对公汇款 → 企业实名认证 → 去变更 |
| 企业改个人？ | 不支持 |
| 如何充值？ | 实名后，充值页用支付宝/微信；企业可用对公汇款 |
| 余额过期？ | 充值余额永不过期，赠送余额有期限 |
| 如何开发票？ | 账单页 → 发票管理 |
| 可以退款吗？ | 未消费金额支持退款，在线支付可在账单页自助操作 |
| 如何分 Key 看用量？ | 用量信息页 → 选择月份 → 导出 CSV |
| 并发上限？ | 动态限流，无固定上限，暂不支持单独提额 |
| API 比网页慢？ | 网页默认流式，API 默认非流式，开启 stream 即可 |
| 支持 LangChain？ | 支持，下载 [deepseek_langchain.py](https://cdn.deepseek.com/api-docs/deepseek_langchain.py) |

## B. 关键链接

| 资源 | 地址 |
| --- | --- |
| API Key 申请 | [platform.deepseek.com/api_keys](https://platform.deepseek.com/api_keys) |
| 充值 | [platform.deepseek.com/top_up](https://platform.deepseek.com/top_up) |
| 账单 | [platform.deepseek.com/transactions](https://platform.deepseek.com/transactions) |
| 用量 | [platform.deepseek.com/usage](https://platform.deepseek.com/usage) |
| 官方文档 | [api-docs.deepseek.com](https://api-docs.deepseek.com/zh-cn/) |
| GitHub | [github.com/deepseek-ai](https://github.com/deepseek-ai) |
| Discord | [discord.gg/Tc7c45Zzu5](https://discord.gg/Tc7c45Zzu5) |
| 技术支持邮箱 | api-service@deepseek.com |

## C. 官方文档完整目录

```
快速开始
├── 首次调用 API
├── 模型 & 价格
├── Token 用量计算
├── 限速
└── 错误码

API 指南
├── 思考模式
├── 多轮对话
├── 对话前缀续写（Beta）
├── FIM 补全（Beta）
├── JSON Output
├── Tool Calls
├── 上下文硬盘缓存
├── Anthropic API
└── 接入 Agent 工具
    ├── Claude Code
    ├── OpenCode
    ├── OpenClaw
    └── 贡献你的 Agent 接入

API 文档
├── 对话（Chat Completions）
├── 补全（Completions）
├── 模型列表
└── 余额查询

新闻 / 常见问题 / 更新日志
```

---

# 落地 Checklist

### 入门篇
- [ ] 拿到 API Key 并成功调用
- [ ] 理解 Token 计费，知道缓存命中价差
- [ ] 知道 429/503 怎么处理

### 核心篇
- [ ] 思考模式：能开关、能调 effort、会拼接 reasoning_content
- [ ] 多轮对话：每次请求带上完整 messages
- [ ] JSON Output：prompt 含 json 字样 + response_format
- [ ] Tool Calls：跑通完整的 调用→执行→回传 流程

### 高级篇
- [ ] 前缀续写：beta 端点 + prefix: True
- [ ] FIM 补全：prompt + suffix 填中间
- [ ] 硬盘缓存：理解命中规则，会看 prompt_cache_hit_tokens

### 生态篇
- [ ] Anthropic API：用 Claude SDK 调通 DeepSeek
- [ ] Agent 接入：至少接入一款（Claude Code / OpenCode / OpenClaw）

---

照着做，DeepSeek API 会从"能调通"升级为"用得溜"——思考模式提质量，工具调用扩能力，硬盘缓存省成本，Agent 接入覆盖全场景。
