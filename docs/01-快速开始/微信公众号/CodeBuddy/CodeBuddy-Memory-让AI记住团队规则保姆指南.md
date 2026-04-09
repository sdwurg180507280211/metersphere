# 🧩 CodeBuddy Memory 全解析：把团队规则喂给 AI 的正确姿势

> 希望 AI 记住“新功能必须有单测”“不能动 legacy 模块”？这一篇教你怎么把知识写进 Memory 层。

---

## 1. Memory 有哪些层？
| 层级 | 存放位置 | 适合内容 |
| --- | --- | --- |
| User Memory | `~/.codebuddy/CODEBUDDY.md` | 个人偏好、常用项目路径 |
| Project Memory | `<workspace>/.codebuddy/CODEBUDDY.md` | 仓库架构、业务术语 |
| Rules 目录 | `.codebuddy/rules/*.md` | 可按角色拆文件，如 `frontend.md`、`security.md` |
| CODEBUDDY.local.md | 与 CODEBUDDY.md 同目录 | 只在本机生效的敏感/临时规则 |
| Auto Memory / Typed Memory | CLI 自动捕获的知识 | 需开启对应开关，适合记住执行历史 |

> 所有 Memory 文件都支持 `@import 相对路径` 把其他文档插进上下文。

---

## 2. 5 步写出一份好用的 CODEBUDDY.md
1. `mkdir -p .codebuddy && touch .codebuddy/CODEBUDDY.md`
2. 写明项目简介、模块目录、构建命令。
3. 用清单列出必守规范，例如“PR 必须包含 CHANGELOG”。
4. 需要长期引用的设计文档，用 `@import docs/architecture.md`；AI 会自动展开内容。
5. 若有敏感或临时信息，写在 `CODEBUDDY.local.md`，防止被同步到 Git。

示例：
```markdown
# 项目速记
- 框架：NestJS + React
- 关键命令：`pnpm dev:api` / `pnpm dev:web`

# 团队规范
1. 新 API 必须补 integration test
2. 禁止修改 legacy/ 目录
@import docs/security-guidelines.md
```

---

## 3. Rules 目录怎么用？
```
.codebuddy/
├── CODEBUDDY.md
├── CODEBUDDY.local.md
└── rules/
    ├── frontend.md
    ├── qa.md
    └── security.md
```
- 在对话里输入 `/memory` 可查看/管理这些规则。
- 当你开 Sub-Agent 时，可以在系统提示里 `@import rules/security.md` 只给“安全官”看对应规则。

---

## 4. 自动记忆 & Typed Memory
- 开启 `CODEBUDDY_TYPED_MEMORY_ENABLED=true` 后，CLI 会把命令执行结果、任务总结等结构化存进 `.codebuddy/memory`，方便下次引用。
- 也可以通过 `CODEBUDDY_AUTO_MEMORY_MAX_ITEMS` 控制容量，避免无限膨胀。

---

## 5. 常见疑问
**Q：修改 Memory 需要重启吗？**  
A：不需要，保存文件后 CLI 会自动同步。

**Q：怎么确认 AI 真的读取了？**  
A：用 `/memory` 或 `/debug` 查看当前上下文；也可以让 AI 复述刚写的规则。

**Q：多语言项目怎么安排？**  
A：在 Rules 下拆多语言文件，或在 CODEBUDDY.md 中写多语言段落；记得指定 `Language` 选项，让 AI 用你期望的语言回复。

> 只要把 Memory 体系搭好，CodeBuddy 就会像你团队的老同事一样懂行，不用每次重复“请写单测”“别动这个目录”啦。
