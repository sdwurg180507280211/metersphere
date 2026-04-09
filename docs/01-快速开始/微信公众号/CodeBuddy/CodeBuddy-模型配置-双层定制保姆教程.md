# 🧠 CodeBuddy 模型配置保姆教程：搞定用户级 + 项目级 `models.json`

> 想把自建代理、OpenRouter、DeepSeek 统统塞进 CodeBuddy？跟着这篇做完就会。

---

## 1. 先弄清楚 `models.json` 放在哪
| 层级 | 路径 | 作用 |
| --- | --- | --- |
| 用户级 | `~/.codebuddy/models.json` | 全局默认模型，影响所有项目 |
| 项目级 | `<workspace>/.codebuddy/models.json` | 针对当前仓库的专属配置，优先级更高 |

> 合并策略：项目级覆盖用户级，同 ID 的模型会被替换，不同 ID 会追加；`availableModels` 则是“谁写谁生效”，不会合并。

---

## 2. 模型条目的关键字段
```json
{
  "id": "gpt-4o",
  "name": "GPT-4o",
  "vendor": "OpenAI",
  "url": "https://api.openai.com/v1/chat/completions",
  "apiKey": "${OPENAI_KEY}",
  "maxInputTokens": 128000,
  "maxOutputTokens": 16384,
  "supportsToolCall": true,
  "supportsImages": true,
  "temperature": 0.3
}
```
- `url` 一定要写完整的接口路径（通常以 `/chat/completions` 结尾），否则 CLI 会报错。
- `apiKey`、`url` 可以写 `${ENV_NAME}`，把敏感信息交给环境变量。
- `supportsToolCall`/`supportsImages` 等布尔值会直接影响界面里能否使用工具调用、图像输入。

---

## 3. 控制模型下拉列表
```json
{
  "availableModels": [
    "gpt-4o",
    "openrouter/qwen-plus",
    "my-enterprise-model"
  ]
}
```
- 没被列出来的模型即使有定义也不会出现在 CLI 下拉框里，适合企业限定“推荐列表”。

---

## 4. 典型场景手把手
### ✅ 给团队加一个 OpenRouter 模型
```json
{
  "models": [
    {
      "id": "openrouter/gpt-4o-mini",
      "name": "OpenRouter · GPT-4o Mini",
      "vendor": "OpenRouter",
      "url": "https://openrouter.ai/api/v1/chat/completions",
      "apiKey": "${OPENROUTER_KEY}",
      "maxInputTokens": 64000,
      "supportsToolCall": true
    }
  ],
  "availableModels": ["openrouter/gpt-4o-mini"]
}
```

### ✅ 覆盖内置模型的调用地址
```json
{
  "models": [
    {
      "id": "gpt-4o",
      "name": "GPT-4o (公司代理)",
      "vendor": "OpenAI",
      "url": "https://proxy.example.com/v1/chat/completions",
      "apiKey": "${PROXY_KEY}"
    }
  ]
}
```

### ✅ 项目 A 使用私有模型
```
project-a/
├── .codebuddy/
│   └── models.json   # 只影响项目 A
```
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

---

## 5. 热更新 + 排障别忘了
1. `models.json` 保存后 1 秒内自动热加载，无需重启 CodeBuddy；如果没生效，先确认文件路径与 JSON 格式。
2. 模型没在列表里？检查 `availableModels` 是否包含对应 ID。
3. 仍失败的话，用 `codebuddy --debug` 查看日志，或确认环境变量是否已导出。

> 只要掌握这套玩法，团队就能把任何 OpenAI 兼容模型挂进 CodeBuddy，既安全又可控。
