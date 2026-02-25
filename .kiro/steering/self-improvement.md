---
inclusion: auto
---

# 自我改进机制

## 何时记录

完成任务后，评估是否有可提取的知识：

| 触发场景 | 记录位置 | 分类 |
|---------|---------|------|
| 命令/操作失败 | `.learnings/ERRORS.md` | - |
| 用户纠正了你 | `.learnings/LEARNINGS.md` | correction |
| 用户要求不存在的功能 | `.learnings/FEATURE_REQUESTS.md` | - |
| 发现知识过时/错误 | `.learnings/LEARNINGS.md` | knowledge_gap |
| 发现更好的做法 | `.learnings/LEARNINGS.md` | best_practice |
| API/外部工具失败 | `.learnings/ERRORS.md` | - |

## 记录格式

Learning 条目追加到 `.learnings/LEARNINGS.md`：
```
## [LRN-YYYYMMDD-XXX] category
**Logged**: ISO-8601 时间戳
**Priority**: low | medium | high | critical
**Status**: pending
**Area**: frontend | backend | infra | tests | docs | config
### Summary
一句话描述
### Details
完整上下文
### Suggested Action
具体修复建议
### Metadata
- Source: conversation | error | user_feedback
- Related Files: 相关文件路径
- Tags: 标签
---
```

Error 条目追加到 `.learnings/ERRORS.md`：
```
## [ERR-YYYYMMDD-XXX] 命令或技能名
**Logged**: ISO-8601 时间戳
**Priority**: high
**Status**: pending
**Area**: frontend | backend | infra | tests | docs | config
### Summary
简述失败内容
### Error
实际错误信息
### Context
- 尝试的命令/操作
- 输入参数
- 环境信息
### Suggested Fix
可能的修复方案
### Metadata
- Reproducible: yes | no | unknown
- Related Files: 相关文件路径
---
```

## 晋升规则

当经验被证明具有广泛适用性时（出现 3 次以上、跨多个任务），将其提炼为简短规则写入 `.kiro/steering/` 目录下的对应 steering 文件。

## 行为准则

- 立即记录，上下文最新鲜
- 具体明确，未来的 AI 需要快速理解
- 包含复现步骤（尤其是错误）
- 建议具体修复方案，而非"待调查"
- 开始重大任务前，先回顾 `.learnings/` 中的相关条目
