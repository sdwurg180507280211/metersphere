# Learnings Log

MeterSphere 二次开发过程中的经验、纠正和发现。在开始重大任务前回顾。

**分类**: correction | insight | knowledge_gap | best_practice
**区域**: frontend | backend | infra | tests | docs | config
**状态**: pending | in_progress | resolved | wont_fix | promoted

---

## [LRN-20260225-001] best_practice

**Logged**: 2026-02-25T10:00:00Z
**Priority**: medium
**Status**: pending
**Area**: config

### Summary
self-improving-agent 从 OpenClaw/Claude Code 适配到 Kiro 的映射关系

### Details
self-improving-agent 1.0.11 原生支持 OpenClaw 和 Claude Code，但不直接支持 Kiro。适配时发现以下对应关系：
- OpenClaw 的 `CLAUDE.md` / `AGENTS.md` / `SOUL.md` → Kiro 的 `.kiro/steering/*.md`（auto inclusion）
- OpenClaw 的 `hooks/openclaw/handler.js`（agent:bootstrap 事件）→ Kiro 的 `agentStop` hook（askAgent 类型）
- Claude Code 的 `UserPromptSubmit` hook + `scripts/activator.sh` → Kiro 的 `agentStop` hook + prompt 内联
- Claude Code 的 `PostToolUse` hook + `scripts/error-detector.sh` → Kiro 的 `postToolUse` hook（toolTypes: shell）+ prompt 内联
- `.learnings/` 目录结构和记录格式完全通用，无需修改

关键差异：Kiro hook 不支持直接执行外部 shell 脚本作为 askAgent 的输入，需要把脚本逻辑转化为 hook 的 `outputPrompt` 字段中的自然语言指令。

### Suggested Action
后续如果有其他 OpenClaw/Claude Code 技能需要适配到 Kiro，可参考此映射关系快速完成。

### Metadata
- Source: conversation
- Related Files: .kiro/steering/self-improvement.md, .kiro/hooks/self-improve-reminder.kiro.json, .kiro/hooks/error-detector.kiro.json, docs/self-improving-agent-1.0.11/SKILL.md
- Tags: kiro, openclaw, hook-adaptation, self-improvement, cross-platform

---

