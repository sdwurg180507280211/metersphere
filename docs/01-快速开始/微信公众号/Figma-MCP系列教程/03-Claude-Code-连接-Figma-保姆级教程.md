# 🔥 Claude Code 连接 Figma 保姆级教程！设计稿秒变代码

想让 AI 直接看懂你的 Figma 设计稿，自动生成代码？今天这篇教程，带你用 **Claude Code + Figma MCP**，实现从设计到开发的无缝衔接！

> 官方最新支持：现在用 **Plugin 方式** 连接，比以前简单 10 倍！

---

## 一、先搞懂：Figma MCP 是什么？

简单来说，**MCP（Model Context Protocol）就是 AI 和 Figma 之间的翻译官**：

- 没有 MCP：AI 看不到你的 Figma 文件，只能你手动描述
- 有了 MCP：AI 能直接"看"懂设计稿的图层、颜色、间距、组件...

**Figma 提供两种连接方式：**

| 方式 | 说明 | 推荐度 |
|-----|------|--------|
| **Remote MCP（推荐）** | 官方托管，一条命令连上 | ⭐⭐⭐⭐⭐ |
| Desktop MCP | 本地运行，需要 Figma 桌面端 | ⭐⭐ |

**我们今天用 Remote MCP，官方推荐，最简单！**

---

## 二、准备工作

在开始之前，确保你有：

1. ✅ **Claude Code 已安装**（如果还没装，先去 https://claude.com/product/claude-code 下载）
2. ✅ **一个 Figma 账号**（免费版也可以）
3. ✅ **一个 Figma 设计文件**（用来测试）
4. ✅ **Dev 或 Full seat**（付费计划需要，免费版有使用限制）

---

## 三、正式开始：3 步连上 Figma

### 第一步：安装 Figma Plugin

打开终端，运行这一条命令：

```bash
claude plugin install figma@claude-plugins-official
```

按 `Enter` 开始安装，几秒钟就好。

> 💡 如果 Claude Code 正在运行，安装完需要**重启 Claude Code**。

### 第二步：授权连接

1. 在 Claude Code 中输入 `/plugin` 命令，按 `Enter`
2. 用右方向键 `→` 切换到 **Installed** 标签页
3. 用方向键选择 **figma**，按 `Enter`
4. 再按一次 `Enter`，浏览器会自动打开授权页面
5. 点击 **Allow access**（允许访问）

### 第三步：验证连接

回到 Claude Code，再次输入 `/plugin`：

- 在 **Installed** 标签页下
- figma 服务器应该显示为 **connected**

🎉 恭喜！现在 Claude Code 已经能看懂你的 Figma 设计了！

---

## 四、怎么用？举个例子

### 获取设计稿上下文

1. 在 Figma 中，选中你想要的图层或 Frame
2. 右键 → **Copy link to selection**（或者直接从浏览器地址栏复制 URL）
3. 把链接粘贴到 Claude Code，然后说：

```
根据这个 Figma 设计稿生成 React 组件：
https://www.figma.com/design/xxxxx/xxx?node-id=1-2
```

Claude Code 会自动：
- 读取设计稿的结构
- 提取颜色、间距、字体
- 生成对应的代码

---

## 五、你可能不知道的高级功能

### 1. 直接在 Figma 里写（Write to Canvas）

Remote MCP 支持**反向操作**：让 AI 直接在 Figma 画布上创建和修改内容！

比如你可以说：
```
在 Figma 中创建一个登录页面的设计稿，包含邮箱输入框、密码输入框和登录按钮
```

AI 会直接在 Figma 里生成图层，不用你手动画！

### 2. 把网页变成 Figma 设计（Code to Canvas）

看到一个网页想参考？可以把它直接"抓"成 Figma 设计稿！

对 Claude Code 说：
```
把 localhost:3000 的页面捕获到一个新的 Figma 文件中
```

Claude Code 会：
- 打开浏览器让你访问页面
- 用工具栏捕获页面、元素、状态
- 自动创建 Figma 文件

### 3. 支持的不只是 Claude Code

Figma MCP 支持好多工具，看你用哪个：

| 工具 | Desktop MCP | Remote MCP | Write to Canvas | Code to Canvas |
|-----|------------|-----------|----------------|---------------|
| **Claude Code** | ✓ | ✓ | ✓ | ✓ |
| **Cursor** | ✓ | ✓ | ✓ | ✓ |
| **VS Code** | ✓ | ✓ | ✓ | ✓ |
| **Codex** | ✓ | ✓ | ✓ | ✓ |
| **Warp** | ✓ | ✓ | ✓ | ✓ |

---

## 六、常见问题

### Q: 提示连接失败怎么办？

A: 检查这几点：
1. 确认你是 Dev 或 Full seat（免费版可能有使用限制）
2. 重新运行 `/plugin` 看看状态
3. 可以尝试重新授权：在 Installed 页面选 figma 再按 Enter

### Q: 免费版能用吗？

A: 可以，但有使用限制。Beta 期间这个功能是免费的，以后可能会变成按使用量付费。

### Q: 需要 Figma 桌面端吗？

A: **不需要！** Remote MCP 是官方托管的，网页版 Figma 就可以。

### Q: 国内访问有问题吗？

A: Figma 本身在国内访问可能需要科学上网，确保你的网络能正常访问 Figma。

---

## 七、官方资源

- 📚 **官方文档**: https://help.figma.com/hc/en-us/articles/35281350665623
- 🔗 **Claude Code Plugin**: https://claude.com/plugins/figma
- 🎯 **MCP 目录**: https://figma.com/mcp-catalog

---

## 写在最后

以前设计师给你 Figma，你要手动量间距、看颜色、数像素；现在把链接扔给 Claude Code，它直接给你代码。

这就是 AI 时代的开发效率——**让设计稿自己"写"代码**。

快去试试吧！有问题评论区留言～
