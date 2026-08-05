# 🌅 24 小时和 CodeBuddy 相伴：Lin 的终端日常（加点 🚀 想象力）

> 今天不聊“命令清单”，我们跟着虚构角色 **Lin**——一位在深圳的 AI 平台工程师——体验 CodeBuddy 的完整工作日。把鼠标放一边，端起咖啡，看看 Lin 如何用终端里的 AI 小伙伴完成每一件事。故事里出现的命令、配置、技巧，你都可以马上照抄。

---

## ⏰ 07:30｜咖啡香 + 第一次登录
**🎧 LinOS 背景音乐响起**：“Node.js 18.20 OK，npm --version OK，那就 GO!”
```bash
npm install -g @tencent-ai/codebuddy-code
codebuddy --version
cbc   # 简写命令
```
- 登录菜单弹出，他在公司网络，选择 **Chinese Site（copilot.tencent.com）**。citeturn0view0
- 第一条指令永远是 `/init`：构建项目知识图谱，之后问什么都更快。citeturn0view0
- 第二条是 `/plan`，把想做的事先列出来，避免“回车即惊喜”。

**🍵 沉浸提示**：`Ctrl+O` 查看“AI 正在思考”，能看到 CodeBuddy 实时读取文件，这一刻特别有安全感。citeturn0view0

---

## 🧠 08:30｜模型策略会
**Lin 的心声**：“要重构，就先安排好模型资源。”
- 用户级 `~/.codebuddy/models.json` 放公共模型，项目级 `<workspace>/.codebuddy/models.json` 做专属配置。citeturn1view0
- 字段可以写 `${ENV_NAME}`，代理参数不再裸奔。citeturn1view0
- `availableModels` 让下拉列表干净利落，团队统一视图。

```json
{
  "models": [
    {
      "id": "gpt-4o",
      "name": "GPT-4o (企业代理)",
      "vendor": "OpenAI",
      "url": "https://proxy.example.com/v1/chat/completions",
      "apiKey": "${CORP_PROXY_KEY}",
      "supportsToolCall": true
    }
  ],
  "availableModels": ["gpt-4o", "gpt-4o-mini"]
}
```

**🪄 沉浸提示**：切换模型下拉时，只看到允许的 ID，会有一种“指挥舰队”的掌控感。

---

## 📒 10:00｜把团队脑子搬进 Memory
**Lin 吐槽**：“再也不想重复讲项目背景。”
- `~/.codebuddy/CODEBUDDY.md` 记录个人偏好，项目级 `.codebuddy/CODEBUDDY.md` 记录业务逻辑。citeturn2view0
- `.codebuddy/rules/` 分角色写规范：`frontend.md`、`security.md`……
- `@import docs/architecture.md` 把长文档一次性塞进上下文。citeturn2view0
- `/memory` 随时复查，`CODEBUDDY_TYPED_MEMORY_ENABLED=true` 让 CLI 记住常用命令。citeturn2view0

```markdown
# 项目速记
- 框架：NestJS + React + pnpm
- 命令：`pnpm dev:api` / `pnpm dev:web`

# 团队规约
1. 新 API 必须补 integration test
2. legacy/ 目录禁止修改
@import docs/architecture.md
```

**🧘 沉浸提示**：让 CodeBuddy 复述这份规约，听到它答对，瞬间感觉“AI 老同事”上线了。

---

## 🗃️ 11:30｜午餐前的环境变量仪式
**Lin 的小心愿**：“希望所有人跑出来的行为都一致。”
```json
{
  "env": {
    "CODEBUDDY_DEFAULT_PERMISSION_MODE": "plan",
    "CODEBUDDY_ALLOWLIST_COMMANDS": "git status,git diff",
    "CODEBUDDY_DENYLIST_COMMANDS": "rm -rf,shutdown",
    "CODEBUDDY_MODEL": "gpt-4o",
    "CODEBUDDY_TYPED_MEMORY_ENABLED": "true"
  }
}
```
- 这段写在 `~/.codebuddy/settings.json`，启动即生效。citeturn3view0
- CI/容器里用 Secrets 注入，避免明文密钥。
- 其他常用变量：`CODEBUDDY_PLUGIN_DIRS`、`CODEBUDDY_DISABLE_CRON`、`CODEBUDDY_LOG_LEVEL` 等。

**🧾 沉浸提示**：把这份 JSON 发到团队群，“复制→粘贴→一键统一”的体验，会让大家直呼舒爽。

---

## 🤖 13:30｜午休后的分身军团
**Lin 想偷懒**：“日志巡检、权限审核、改写 changelog……交给 AI 分身。”
- `/agents` 新建：
  - `日志巡检员`：只读 `logs/`，工具限定 `rg`、`python`。
  - `安全顾问`：`@import rules/security.md`，禁止写生产目录。
  - `自动写手`：专门生成 commit message、changelog。
- `/team` 把他们编成虚拟 Scrum：Product → Dev → QA → Summary，终端聊天窗仿佛 Slack 频道。citeturn4view0

**🎬 沉浸提示**：看着不同代理轮流发言，真的有“团队配合”的视觉错觉，效率直线上升。

---

## 🔌 15:00｜插件 + Daemon，让工作流自动运转
- 插件市场 + MCP 让 CodeBuddy 呼叫浏览器、数据库、监控系统；Lin 为终端挂上 Chrome DevTools MCP，直接调试网页。citeturn0view0
- `codebuddy daemon start` 让 CLI 常驻后台，Scheduled Tasks 设定每天 09:00 自动跑 `/plan`、`/test`，把巡检报告发到 WeCom Bot。citeturn0view0

**🔔 沉浸提示**：办公群里突然出现“AI 值班同事”的早报时，同事们往往还没到工位，就先被提醒了风险。

---

## 🛡️ 16:30｜安全审查时刻
1. **权限模式**：保持 `plan`，所有写操作先看计划。citeturn5view0
2. **Bash Sandboxing**：把 CLI 锁在项目目录，防止误删 `/`。citeturn5view0
3. **IAM / 企业域名**：SSO、VPN 校验、命令拦截统统启用。citeturn5view0
4. **审计 / 成本**：`CODEBUDDY_LOG_LEVEL=debug` + Cost Management 限流策略，让一切有迹可循。citeturn5view0

**⚠️ 沉浸提示**：故意试图执行 “危险命令”，感受权限面板跳出询问的那一刻——那叫一个踏实。

---

## 🌇 18:00｜黄昏的 CLl 俱乐部
Lin 不会把工具“一刀切”，他把常用 CLI 分工：
| 工具 | Lin 的用法 | 亮点 |
| --- | --- | --- |
| **OpenCode** | 作为“实验室”测试新模型、LSP、TUI Mission Control。citeturn6view0 | 开源、75+ 模型、`/share` 链接、Chrome DevTools MCP。注意默认会上传会话标题，记得关闭。citeturn6reddit22 |
| **Claude Code** | Auto Mode 让 AI 自批权限，`/voice` 能直接语音调度终端。citeturn7news13turn7search0 | 体验极佳，但要紧盯源码泄露和第三方收费政策。citeturn7news15turn7news12 |
| **Gemini CLI** | 需要实时 Google Search / Zed 集成时使用。citeturn8search1turn8search6 | 1M token、Search Grounding、开源，可惜得跟着 Google 模型节奏走。citeturn8search3 |
| **Kimi CLI** | 生成整站、长文档，利用 K2.5 Agent Swarm 平行 100 个子代理。citeturn9search2turn9search1 | 国产 1T 模型 + 1,500 次工具调用，需提前申请配额。citeturn9search7 |

**🌈 沉浸提示**：给这些 CLI 各开一个 tmux pane，仿佛拥有多位 AI 同事在不同频道各司其职。

---

## 🌙 21:00｜收工 Checklist ✅
1. 🛠️ 安装、登录、`/init` 都搞定
2. 🧠 用户级 + 项目级 `models.json`
3. 📒 `CODEBUDDY.md` + `.codebuddy/rules/` + Typed Memory
4. ⚙️ `settings.json` 同步模型/权限/记忆策略
5. 🤖 至少 2 个 Sub-Agent + 1 个 Agent Team 跑通 `/plan`
6. 🛡️ Bash Sandboxing、命令白/黑名单、IAM 全部开启
7. 🧩 混搭 OpenCode / Claude Code / Gemini CLI / Kimi CLI，根据场景切换

> “明天的巡检 cron 已排好。” Lin 看着静静运转的 Daemon Mode，心里踏实。AI 不只是答题器，而是终端里的执行搭档——前提是，我们替它把舞台搭建好了。
