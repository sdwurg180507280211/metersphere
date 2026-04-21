# 🛠️ CodeBuddy 高级特性全家桶：Sub-Agent、插件、Daemon 一网打尽

> 当你想让 AI 扮演“安全官”“前端体验官”甚至“全天候巡检员”，这些功能就派上用场啦。

---

## 1. Sub-Agent：给 AI 分身设定角色
- 在 CLI 输入 `/agents` 新建一个“分身”，可以自定义：
  - System Prompt（角色设定）
  - 可访问目录/文件
  - 允许使用的工具/模型
- 常见玩法：
  - `前端体验官`：只允许访问 `web/` 目录，专注 React 调优
  - `日志巡检员`：只读 `logs/`，默认对 log 文件做分析
  - `安全顾问`：绑定 `rules/security.md`，专门检查依赖漏洞

🙋 小技巧：Sub-Agent 会继承主会话上下文，但你可以通过 `@import` 给它额外知识，例如 `@import rules/frontend.md`。

---

## 2. Agent Teams：把多个分身组团打仗
- 命令 `/team` 可以创建“虚拟 Scrum”，指定：
  1. Team 名称 & 目标
  2. 成员（来自 Sub-Agent 列表）
  3. 流程（如 需求 → 设计 → 开发 → 测试 ）
- 一次大的重构可以这样玩：
  - Product Agent 负责拆需求
  - Dev Agent 实施代码变更并产出 patch
  - QA Agent 编写测试计划
  - 最后系统会生成总结和待办

> Agent Team 适合协作型任务，尤其是需要多轮 review/复盘的情况。

---

## 3. Skills & Plugins：把你的脚本封装给 AI 用
- **Skills**：轻量级脚本/命令片段，可在会话内随叫随到。
- **Plugins**：更完整的扩展，可以连接 HTTP API、数据库、云服务，甚至发布到插件市场。
- 模式参考：
  - 用 Skills 处理“拷贝模版、格式化”类小工具
  - 用 Plugins 打通“缺陷管理、监控平台、知识库”
- 如果团队已经有 MCP（Model Context Protocol）服务，还能让 CodeBuddy 直接调用，做到“问就查工单”。

---

## 4. Sandbox / Checkpointing / Daemon
- **Sandbox (Beta)**：在受限容器里执行命令，降低误删风险。
- **Checkpointing**：让长任务支持中途保存、恢复，特别适合大范围 refactor。
- **Daemon Mode**：CodeBuddy 背景常驻，随时响应远程 CLI、Web UI、WeCom Bot 的请求。
- **Scheduled Tasks**：配合 Daemon 设置定时任务，比如“每天 9 点跑一次单测汇总”。

---

## 5. Web UI、Channels、WeCom Bot
- Web UI 提供可视化会话/任务面板，方便非终端用户参与。
- Channels (Beta) 可以把外部事件推送进会话，让 AI 对接外部 webhook。
- WeCom Bot / Remote Control 让 CodeBuddy 直接进 IM 群里播报任务、接收指令。

---

## 6. 推荐启用顺序
1. 先在 `/agents` 里配置 2~3 个常驻角色。
2. 需要跨角色协作时，再建 `/team`，别一次把所有分身拉上。
3. 把常用脚本提炼成 Skills，再把“需要联网/系统集成”的能力做成 Plugins。
4. 对生产库进行修改前，务必开启 Sandbox 或至少配置 `CODEBUDDY_DENYLIST_COMMANDS`。
5. 当项目进入长期维护期，再启用 Daemon + Scheduled Tasks，打造“AI 值班工程师”。

> 善用这些高级特性，CodeBuddy 不只是聊天机器人，而是一个可以编排多角色协作、挂载自家系统、7×24 小时值守的 AI DevOps 平台。
