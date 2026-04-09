# 🛡️ CodeBuddy 安全合规五件套：权限、Sandbox、IAM 全覆盖

> AI 能写代码也能删库，所以安全策略一定要先到位。对照这五步，确保 CodeBuddy 在团队里“可控、可审计、可追责”。

---

## 1. 多层权限模式：Default / Bypass / Accept / Plan
- `Default`：遇到文件写入、命令执行时会弹授权面板
- `Bypass`：跳过所有确认（慎用，仅限个人测试）
- `Accept`：默认同意大部分操作
- `Plan`：先输出执行计划，确认后才真正执行

👉 建议在企业环境下把 `CODEBUDDY_DEFAULT_PERMISSION_MODE` 设为 `plan`，再配合 `/plan` 指令，让所有高危动作先过人眼。

---

## 2. Bash Sandboxing + 命令白/黑名单
- `CODEBUDDY_BASH_SANDBOXING_LEVEL` 控制 CLI 写文件、执行脚本的能力。
- `CODEBUDDY_ALLOWLIST_COMMANDS` 和 `CODEBUDDY_DENYLIST_COMMANDS` 组合拳：
  - 把允许的 CI/CD 命令、构建脚本加进 allowlist
  - 把 `rm -rf`、`shutdown`、`curl ... | sh` 这类危险命令放进 denylist
- 还可以设置 `CODEBUDDY_DANGEROUS_COMMANDS`，让特定命令执行前再确认一次。

---

## 3. 身份与接入管理（IAM）
- 企业用户可以把登录方式接入自家账号体系：
  - 国际站（codebuddy.ai）
  - 国内站（copilot.tencent.com）
  - 企业自建域名
  - 腾讯 iOA
- 配合 `Enterprise Domain` 登录，可实现自建身份认证、私有化部署，甚至绑定 VPN / 内网访问策略。

---

## 4. Sandbox、Daemon、Remote Control 的管控点
- **Sandbox (Beta)**：在隔离空间执行命令，适合生产环境的巡检、脚本执行。
- **Daemon Mode / Remote Control**：一定要结合权限模式与命令白名单，否则远程指令可能绕过本地确认。
- **Scheduled Tasks**：适用于“无人值守”任务，务必给任务加 `/plan` 或自定义守护逻辑，防止误操作。

---

## 5. 审计 & 成本
- CLI 会记录每次对话、命令、生成的 patch，方便审计；企业可把日志同步到集中化平台。
- 使用 `Cost Management` 章节里提到的限流策略，结合 `availableModels` 限定可用模型，有效控制 Token 费用。
- 出现异常时开启 `CODEBUDDY_LOG_LEVEL=debug` + `CODEBUDDY_TRACE_THINKING=true`，既能排查问题，也能留证。

> 做到“权限可控 + 操作可追溯 + 成本可预期”，才能放心地把 CodeBuddy 用在关键业务上。
