# 🧑‍💻 5 分钟搞定 CodeBuddy CLI：小白也会的终端版 AI 助手

**目标：照着做就能把 CodeBuddy 装好、登上、跑起来，并知道日常怎么用。**

---

## 1. 检查自己的电脑（1 分钟）

| 必备项 | 如何检查 |
| --- | --- |
| Node.js ≥ 18.20 | `node --version` |
| npm/pnpm/yarn/bun 任一 | `npm --version` |
| 支持系统 | macOS / Linux / Windows 均可 |
| 网络 & Git | 能访问 npm（或腾讯脚本），终端里有 Git |

如果懒得装 Node.js，可以跳到后面用“原生安装器”方案。

---

## 2. 安装方式挑一个

### ✅ 方案 A：包管理器（最常用）

```bash
# 任选 npm/pnpm/yarn/bun
npm install -g @tencent-ai/codebuddy-code
```

### ✅ 方案 B：原生安装器（Beta）

适合没有 Node.js 环境的同学：

```bash
# macOS / Linux
curl -fsSL https://copilot.tencent.com/cli/install.sh | bash
```

```powershell
# Windows
irm https://copilot.tencent.com/cli/install.ps1 | iex
```

⚠️ 安装后一定跑：

```bash
codebuddy --version
```

看到版本号说明成功。

---

## 3. 第一次启动 & 登录

```bash
codebuddy   # 或者 cbc
```

屏幕会出现四个登录方式，用方向键选择：

- **Log in via Chinese Site**：大部分国内用户选它
- **Log in via International Site**：海外用户
- **Enterprise Domain**：公司私有化部署
- **iOA**：腾讯同学专用

按回车后浏览器自动打开完成授权。

---

## 4. 进入项目后的“三板斧”

1. `cd` 到项目目录  
2. 再次运行 `codebuddy`（或 cbc）进入会话  
3. 输入 `/init` 让 AI 先扫一遍项目：  
   - 构建知识图谱 → 理解你的代码结构  
   - 少传重复文件 → 节省 token  
   - 回答更快 → 不用每轮重新扫描

---

## 5. 三种常见用法

| 模式 | 怎么用 | 适合场景 |
| --- | --- | --- |
| 互动对话 | 启动后直接描述：“帮我给 `Login.tsx` 加 Loading” | 日常开发、问答 |
| 单条命令 | `codebuddy -p "检查 services 目录的 TS 类型" -y` | 脚本、CI 输出结果 |
| 管道 / 批处理 | ``cat error.log \| codebuddy "找出根因"`` | 让 AI 帮你读日志、分析 SQL |

额外推荐：对于大任务可以加 `/plan` 先看执行清单，再确认是否落地。

---

## 6. 授权 & 快捷键备忘

- 有读写或执行权限的操作记得加 `-y`（= 已授权），无人值守脚本用 `--dangerously-skip-permissions`。
- 好用快捷键：
  - `Tab`：命令补全
  - `Ctrl+O`：查看“AI 思考过程”
  - `Ctrl+R`：展开/折叠长输出
  - `Shift+Tab` / `Alt+M`：切换权限模式（默认 → Bypass → Accept → Plan）
  - `Esc Esc`：快速清空输入

---

## 7. 进阶玩法（一点点就上手）

- **Sub-Agents**：`/agents` 面板里给不同任务建“分身”，比如“测试工程师”“安全顾问”，每个 Agent 都能绑定独立工具和提示词。
- **Agent Teams**：`/team` 组队，把多个 Sub-Agent 串成“虚拟 Scrum”，适合重构、项目制任务。
- **Plugins / MCP**：把日志平台、接口、数据库包装成插件或 MCP 服务，直接让 AI 调用企业内部资源。

---

## 8. 维护与排障

- 更新：`codebuddy update`，若想关闭自动更新，`export DISABLE_AUTOUPDATER=1`。
- 常见错误：
  - `command not found` → 重新加载 shell：`source ~/.zshrc`
  - 登录失败 → 检查公司网络/VPN
  - npm 拉不动 → 换镜像 `npm config set registry https://registry.npmmirror.com`
- 官方支持：`/help` + 文档 Troubleshooting，如果还是不行发邮件 `codebuddy@tencent.com`。

---

### ✅ 复盘 Checklist

- [ ] 版本号校验通过  
- [ ] 能正常登录  
- [ ] 在项目里完成 `/init`  
- [ ] 学会 `-p` + `-y` + 管道三种模式  
- [ ] 记住最常用快捷键  

把这篇转给同事，只要会 `npm install` + 跟着“三板斧”，他们也能在终端里拥有一个随叫随到的 AI 编程搭子。
