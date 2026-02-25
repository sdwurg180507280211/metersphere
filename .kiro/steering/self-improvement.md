---
inclusion: auto
---

# 自我改进技能（Self-Improvement）

> 移植自 self-improving-agent-1.0.11，适配 Kiro 编辑器

## 核心行为

在每次任务执行过程中，持续评估是否产生了可提取的经验知识。

### 触发条件

| 情况 | 操作 |
|------|------|
| 命令/操作失败 | 记录到 `.learnings/ERRORS.md` |
| 用户纠正了你 | 记录到 `.learnings/LEARNINGS.md`，类别 `correction` |
| 用户请求不存在的功能 | 记录到 `.learnings/FEATURE_REQUESTS.md` |
| API/外部工具失败 | 记录到 `.learnings/ERRORS.md`，含集成细节 |
| 发现知识过时 | 记录到 `.learnings/LEARNINGS.md`，类别 `knowledge_gap` |
| 发现更好的做法 | 记录到 `.learnings/LEARNINGS.md`，类别 `best_practice` |
| 广泛适用的经验 | 晋升到 `.kiro/steering/` 目录 |

### 评估清单（每次任务完成后）

1. 是否发现了非显而易见的解决方案？
2. 是否有命令/操作失败需要记录？
3. 是否纠正了之前的错误认知？
4. 是否发现了项目特有的模式或约定？
5. 是否有更好的做法值得记录？

如果以上全部为否，不需要做任何操作。

## 日志格式

### Learning 条目（`.learnings/LEARNINGS.md`）

```markdown
## [LRN-YYYYMMDD-XXX] category

**Logged**: ISO-8601 时间戳
**Priority**: low | medium | high | critical
**Status**: pending
**Area**: frontend | backend | infra | tests | docs | config

### 摘要
一行描述学到了什么

### 详情
完整上下文：发生了什么，什么是错的，什么是正确的

### 建议操作
具体的修复或改进措施

### 元数据
- Source: conversation | error | user_feedback
- Related Files: path/to/file.ext
- Tags: tag1, tag2
- See Also: LRN-20250110-001（如果关联已有条目）

---
```

### Error 条目（`.learnings/ERRORS.md`）

```markdown
## [ERR-YYYYMMDD-XXX] skill_or_command_name

**Logged**: ISO-8601 时间戳
**Priority**: high
**Status**: pending
**Area**: frontend | backend | infra | tests | docs | config

### 摘要
简述什么失败了

### 错误信息
```
实际错误消息或输出
```

### 上下文
- 尝试的命令/操作
- 输入或参数
- 环境细节（如相关）

### 建议修复
如果可识别，什么可能解决此问题

### 元数据
- Reproducible: yes | no | unknown
- Related Files: path/to/file.ext
- See Also: ERR-20250110-001（如果重复出现）

---
```

### Feature Request 条目（`.learnings/FEATURE_REQUESTS.md`）

```markdown
## [FEAT-YYYYMMDD-XXX] capability_name

**Logged**: ISO-8601 时间戳
**Priority**: medium
**Status**: pending
**Area**: frontend | backend | infra | tests | docs | config

### 请求的功能
用户想做什么

### 用户上下文
为什么需要，解决什么问题

### 复杂度估计
simple | medium | complex

### 建议实现
如何构建，可以扩展什么

### 元数据
- Frequency: first_time | recurring
- Related Features: existing_feature_name

---
```

## ID 生成规则

格式：`TYPE-YYYYMMDD-XXX`
- TYPE: `LRN`（学习）、`ERR`（错误）、`FEAT`（功能请求）
- YYYYMMDD: 当前日期
- XXX: 顺序编号（001、002...）

## 经验晋升

当一条经验满足以下条件时，晋升到 `.kiro/steering/` 目录：
- 出现次数 >= 3
- 跨越至少 2 个不同任务
- 在 30 天窗口内出现

晋升后的规则写成简短的预防性规则（编码前/编码中该做什么），而非冗长的事故报告。

## 重要提醒

- 立即记录 — 上下文在问题刚发生时最新鲜
- 具体明确 — 未来的 agent 需要快速理解
- 包含复现步骤 — 尤其是错误
- 关联相关文件 — 方便后续修复
- 建议具体修复 — 不只是"调查一下"
- 没有值得记录的内容时，不做任何操作 — 不要为了记录而记录
