# 🔥 桌面版 MCP 配置指南：完全本地，数据安全

虽然 Remote MCP 是推荐选择，但如果你有严格的安全合规要求，或者在内网环境，桌面版 MCP 就是你的最佳选择！

这篇文章带你一步步配置桌面版 Figma MCP。

---

## 一、先确认：你真的需要桌面版吗？

在开始之前，先确认一下：

| 问题 | 如果是 | 建议 |
|-----|-------|------|
| 我需要写入画布（AI 直接在 Figma 画图） | ✓ | 用 **Remote MCP** |
| 我需要网页抓图（把网页变成 Figma） | ✓ | 用 **Remote MCP** |
| 我有严格的安全合规要求 | ✓ | 用 **Desktop MCP** |
| 我在内网环境，不能访问外网 | ✓ | 用 **Desktop MCP** |
| 数据绝对不能离开我的电脑 | ✓ | 用 **Desktop MCP** |
| 我不知道选哪个 | - | 用 **Remote MCP** |

---

## 二、准备工作

在开始之前，确保你有：

1. ✅ **Figma 桌面客户端**（必须是桌面端，网页版不行）
2. ✅ **Claude Code 已安装**
3. ✅ **Dev 或 Full seat**（付费计划）
4. ✅ **Figma 账号已登录**（在桌面端登录）

---

## 三、下载并安装 Figma 桌面客户端

### 步骤 1：下载

访问 Figma 下载页面：
- 🔗 https://www.figma.com/downloads/

选择你的操作系统：
- **macOS**: 下载 `.dmg` 文件
- **Windows**: 下载 `.exe` 文件
- **Linux**: 下载 `.AppImage` 或用包管理器

### 步骤 2：安装

**macOS**:
1. 打开 `.dmg` 文件
2. 把 Figma 拖到 Applications 文件夹
3. 打开 Applications → Figma

**Windows**:
1. 运行 `.exe` 安装程序
2. 按提示完成安装
3. 从开始菜单启动 Figma

### 步骤 3：登录

1. 打开 Figma 桌面客户端
2. 点击 "Sign in"（登录）
3. 在浏览器中完成登录
4. 回到 Figma 桌面端，确认已登录

---

## 四、启用 Desktop MCP 服务器

### 步骤 1：打开设置

在 Figma 桌面客户端中：

**macOS**:
- 菜单栏 → Figma → Preferences（偏好设置）
- 或按 `Cmd + ,`

**Windows**:
- 菜单栏 → 编辑 → Preferences（偏好设置）
- 或按 `Ctrl + ,`

### 步骤 2：启用 Dev Mode

在设置中找到：
- "Dev Mode"（开发模式）
- 或 "Enable local MCP server"（启用本地 MCP 服务器）

勾选这个选项！

### 步骤 3：重启 Figma

保存设置后，**完全退出 Figma**，然后重新打开。

### 步骤 4：验证 MCP 服务是否启动

打开浏览器，访问：
```
http://localhost:3845/sse
```

如果看到 Figma MCP 服务页面，说明已成功启用！

---

## 五、配置 Claude Code 连接 Desktop MCP

### 方法一：通过 Plugin（推荐）

在 Claude Code 中：

```
/plugin marketplace add ChromeDevTools/chrome-devtools-mcp
```

然后：

```
/plugin install chrome-devtools-mcp
```

重启 Claude Code。

### 方法二：手动配置

编辑 Claude Code 配置文件：

**macOS/Linux**: `~/.claude/settings.json`
**Windows**: `%USERPROFILE%\.claude\settings.json`

添加以下配置：

```json
{
  "mcp": {
    "servers": {
      "figma-desktop": {
        "command": [
          "npx",
          "-y",
          "@figma/local-mcp-server",
          "--port",
          "3845"
        ],
        "enabled": true
      }
    }
  }
}
```

保存文件，重启 Claude Code。

---

## 六、验证连接

重启 Claude Code 后，输入：

```
/mcp
```

你应该能看到 `figma-desktop` 在已连接的 MCP 服务器列表中！

如果看到类似这样的输出，说明成功了：

```
✓ figma-desktop (connected)
  - get_design_context
  - get_metadata
  - ...
```

---

## 七、使用 Desktop MCP

### 读取设计稿

在 Figma 桌面客户端中：
1. 选中你想要的图层或 Frame
2. 右键 → "Copy link to selection"
3. 或直接从浏览器地址栏复制 URL

然后在 Claude Code 中说：

```
根据这个 Figma 链接生成代码：
https://www.figma.com/design/xxx?node-id=1-2
```

### 注意事项

⚠️ **桌面版 MCP 不支持：**
- ❌ 写入画布（AI 直接在 Figma 画图）
- ❌ 网页抓图（把网页变成 Figma）

✅ **桌面版 MCP 支持：**
- ✓ 读取设计稿
- ✓ 生成代码
- ✓ 读取 FigJam
- ✓ 读取 Make 文件
- ✓ Code Connect
- ✓ Skills

---

## 八、常见问题

### Q: 提示 "无法连接到 localhost:3845"？

A: 检查：
1. Figma 桌面客户端是否已打开
2. 是否已在 Preferences 中启用 Dev Mode / Local MCP
3. 有没有重启 Figma
4. 访问 http://localhost:3845/sse 验证

### Q: 桌面版和 Remote 版能同时用吗？

A: 可以！配置两个不同的服务器，根据需要切换：

```json
{
  "mcp": {
    "servers": {
      "figma-remote": {
        "command": ["..."],
        "enabled": true
      },
      "figma-desktop": {
        "command": ["..."],
        "enabled": false
      }
    }
  }
}
```

### Q: 能用网页版 Figma 吗？

A: 不能！Desktop MCP 只支持 Figma 桌面客户端。

### Q: 免费版能用吗？

A: 需要 Dev 或 Full seat（付费计划）。

### Q: 数据真的不会离开我的电脑吗？

A: 是的！Desktop MCP 完全在本地运行，数据不会上传到任何服务器。

---

## 九、桌面版 vs Remote 版快速参考

| 功能 | 桌面版 | Remote 版 |
|-----|-------|----------|
| 配置难度 | 中等 | 简单 |
| 读取设计稿 | ✓ | ✓ |
| 生成代码 | ✓ | ✓ |
| 写入画布 | ✗ | ✓ |
| 网页抓图 | ✗ | ✓ |
| 完全本地 | ✓ | ✗ |
| 需要联网 | ✗ | ✓ |
| 官方维护 | ✗ | ✓ |

---

## 十、写在最后

桌面版 MCP 是一个很好的选择——如果你有严格的安全合规要求，或者在内网环境。

但如果你没有这些特殊需求，**Remote MCP 仍然是推荐选择**：
- 配置更简单
- 功能更全（支持写入画布、网页抓图）
- 官方维护，更新及时

选择适合你的，就是最好的！
