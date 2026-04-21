# 🧾 CodeBuddy 环境变量速查：30+ 个开关帮你控住 AI

> 不想每次进 CLI 手动调模式？用环境变量一键搞定模型、权限、日志、自动更新。

---

## 1. 环境变量写在哪？
1. 直接在 Shell 导出：`export CODEBUDDY_DEFAULT_MODEL=gpt-4o`
2. 写进 `~/.codebuddy/settings.json` 的 `env` 块：
```json
{
  "env": {
    "CODEBUDDY_DEFAULT_MODEL": "gpt-4o",
    "CODEBUDDY_THEME": "tokyonight"
  }
}
```
3. CI/CD 或远程容器里用系统 Secrets 注入，避免写死在仓库。

---

## 2. 模型 & 记忆相关
| 变量 | 作用 |
| --- | --- |
| `CODEBUDDY_DEFAULT_MODEL` | 设置全局默认模型 ID |
| `CODEBUDDY_CODE_MODEL` / `CODEBUDDY_CHAT_MODEL` | 分别指定代码/聊天模型 |
| `CODEBUDDY_USE_LEGACY_MODEL_NAMES` | 启用旧版命名兼容 |
| `CODEBUDDY_MEMORY_ENABLED` | 总开关，禁用后不会读取 Memory |
| `CODEBUDDY_TYPED_MEMORY_ENABLED` | 是否启用结构化记忆 |

---

## 3. 权限 & Sandbox
| 变量 | 作用 |
| --- | --- |
| `CODEBUDDY_DEFAULT_PERMISSION_MODE` | 默认权限模式（default/bypass/accept/plan） |
| `CODEBUDDY_ALLOWLIST_COMMANDS` | 允许的 shell 命令列表（逗号分隔） |
| `CODEBUDDY_DENYLIST_COMMANDS` | 禁止命令，例如 `rm -rf,/sbin/shutdown` |
| `CODEBUDDY_BASH_SANDBOXING_LEVEL` | Bash sandbox 等级，限制文件写入 |
| `CODEBUDDY_DANGEROUS_COMMANDS` | 执行前必须确认的敏感命令 |

---

## 4. 运行体验
| 变量 | 作用 |
| --- | --- |
| `DISABLE_AUTOUPDATER` / `CODEBUDDY_DISABLE_AUTOUPDATER` | 关闭自动更新 |
| `CODEBUDDY_AUTO_UPDATE_CHANNEL` | 切换 release/stable/beta |
| `CODEBUDDY_THEME` | UI 配色（tokyonight, dracula 等） |
| `CODEBUDDY_STATUSLINE_ENABLED` | 打开/关闭状态栏 |
| `CODEBUDDY_LOG_LEVEL` | 设置日志级别（info/debug/trace） |
| `CODEBUDDY_LANGUAGE` | 指定默认回复语言 |
| `CODEBUDDY_SKIP_WELCOME` | 启动时跳过欢迎页 |

---

## 5. 网络 & 代理
| 变量 | 作用 |
| --- | --- |
| `HTTP_PROXY` / `HTTPS_PROXY` | 标准代理 |
| `CODEBUDDY_API_PROXY` | 只给模型请求走的代理 |
| `CODEBUDDY_DISABLE_CERT_VALIDATION` | 跳过证书校验（仅限测试环境） |
| `CODEBUDDY_SSH_AUTH_SOCK` | 指定 SSH 代理，方便在容器内使用本机密钥 |

---

## 6. 诊断 & 其他
| 变量 | 作用 |
| --- | --- |
| `CODEBUDDY_TRACE_THINKING` | 输出更详细的 Reasoning，便于排查 |
| `CODEBUDDY_DEBUG_SAVE_RESPONSES` | 将响应保存到磁盘 |
| `CODEBUDDY_DISABLE_ANALYTICS` | 关闭匿名遥测 |
| `CODEBUDDY_SEND_STACKTRACES` | 控制错误栈上报 |
| `CODEBUDDY_COMPOSED_MESSAGE_MAX_LINES` | 限制消息最大行数 |

---

## 7. 推荐模版
```bash
# ~/.zshrc
export CODEBUDDY_DEFAULT_MODEL=gpt-4o
export CODEBUDDY_LANGUAGE=zh-CN
export CODEBUDDY_DEFAULT_PERMISSION_MODE=plan
export CODEBUDDY_ALLOWLIST_COMMANDS="git status,git diff"
export CODEBUDDY_DENYLIST_COMMANDS="rm -rf"
```
- 团队配合 `direnv` 或 `.envrc`，可以在不同项目自动切换模型 & 权限策略。
- 在 CI 中配置 `CODEBUDDY_CHECKPOINT_DIR`、`CODEBUDDY_LOG_LEVEL=debug`，方便追踪无头模式任务。

> 学会这套变量组合，CodeBuddy 的行为就完全在你掌控之中。
