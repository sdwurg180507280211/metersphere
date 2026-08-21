# 百度网盘接入 AI 编程工具：MCP 让你的云盘文件"开口说话"

你的百度网盘里存了几百个文件，想找个东西翻半天？想让 AI 帮你整理、搜索、管理网盘文件？

今天这篇教程，带你用 **百度网盘 MCP Server**，让 Claude Code / CodeBuddy Code 这类 AI 编程工具直接操作你的百度网盘——浏览、搜索、上传、移动、重命名、删除，一句话搞定！

---

## 一、百度网盘 MCP 是什么？

### 简单理解

**百度网盘 MCP Server** 是百度官方开源的 MCP 协议服务端，让 AI 工具能直接操作你的百度网盘。

没有 MCP 时：
- 你："AI，帮我找一下网盘里那个 Java 教程"
- AI："我没有访问你网盘的能力..."

有了 MCP 后：
- 你："帮我找网盘里所有 Java 相关的文件"
- AI：（搜索网盘 → 找到文件 → 列出位置和详情）

### 它能做什么？

| 功能 | 说明 |
|-----|------|
| 📁 **文件浏览** | 列出指定目录下的所有文件（支持按类型筛选：文档/图片/视频） |
| 🔍 **文件搜索** | 关键词搜索 + 语义搜索（自然语言描述） |
| 📝 **文件管理** | 创建文件夹、复制、移动、重命名、删除 |
| ⬆️ **文件上传** | URL 上传、文本内容上传（SSE 模式）；本地上传（Stdio 模式） |
| 🔗 **文件分享** | 创建分享链接，支持设置密码和有效期 |
| 👤 **用户信息** | 查看账号信息、网盘容量 |

### 17 项工具清单

| 类别 | 工具名 | 说明 |
|------|--------|------|
| 文件浏览 | `file_list` | 获取文件列表 |
| | `file_doc_list` | 获取文档列表 |
| | `file_image_list` | 获取图片列表 |
| | `file_video_list` | 获取视频列表 |
| | `file_meta` | 获取文件详细信息 |
| 文件管理 | `make_dir` | 创建文件夹 |
| | `file_copy` | 复制文件 |
| | `file_move` | 移动文件 |
| | `file_rename` | 重命名文件 |
| | `file_del` | 删除文件 |
| 文件上传 | `file_upload_stdio` | 上传本地文件（仅 Stdio 模式） |
| | `file_upload_by_url` | 通过 URL 上传 |
| | `file_upload_by_content` | 通过文本内容上传 |
| 文件搜索 | `file_keyword_search` | 关键词搜索 |
| | `file_semantics_search` | 语义搜索 |
| 分享/用户 | `file_sharelink_set` | 创建分享链接 |
| | `user_info` | 获取用户信息 |
| | `get_quota` | 获取网盘容量 |

---

## 二、两种接入方式

百度网盘 MCP 提供两种模式，按需选择：

| | SSE 远程模式 | Stdio 本地模式 |
|--|-------------|---------------|
| **安装复杂度** | 零安装，配置一行 URL | 需下载 Python SDK |
| **本地上传** | 不支持 | 支持 |
| **其他功能** | 全部支持 | 全部支持 |
| **适合场景** | 大多数用户（推荐） | 需要上传本地文件的用户 |

---

## 三、获取 Access Token

无论用哪种模式，都需要先获取 Access Token。

### 方式一：个人用户（快速体验）

1. 注册/登录百度账号
2. 点击授权链接：[https://openapi.baidu.com/oauth/2.0/authorize?response_type=token&client_id=QHOuRXiepJBMjtk0esLhrPoNlQyYd0mF&redirect_uri=oob&scope=basic,netdisk](https://openapi.baidu.com/oauth/2.0/authorize?response_type=token&client_id=QHOuRXiepJBMjtk0esLhrPoNlQyYd0mF&redirect_uri=oob&scope=basic,netdisk)
3. 点击"授权"后，从跳转的 URL 中复制 `access_token=` 后面的值

> ⚠️ 个人体验密钥仅供测试，会不定期变更。正式使用需申请企业认证。

### 方式二：企业开发者

1. 注册百度账号 → 完成实名认证
2. 在[百度网盘开放平台](https://pan.baidu.com/union/console/applist)创建应用
3. 通过 OAuth 2.0 授权流程获取 Access Token

---

## 四、配置 SSE 模式（推荐）

### Claude Code 配置

编辑 `~/.claude.json`，添加：

```json
{
  "mcpServers": {
    "baidu-netdisk": {
      "type": "sse",
      "url": "https://mcp-pan.baidu.com/sse?access_token=你的AccessToken"
    }
  }
}
```

### CodeBuddy Code 配置

编辑 `~/.codebuddy/.mcp.json`，添加：

```json
{
  "mcpServers": {
    "baidu-netdisk": {
      "type": "sse",
      "url": "https://mcp-pan.baidu.com/sse?access_token=你的AccessToken",
      "description": "百度网盘 MCP Server - 文件管理、搜索、上传、分享等"
    }
  }
}
```

### Cursor / Cline 配置

在对应工具的 MCP 配置文件中添加相同配置即可。

保存后**重启 AI 工具**，配置即生效。

---

## 五、配置 Stdio 模式（支持本地上传）

### 步骤 1：安装依赖

确保已安装 Python 和 [uv](https://github.com/astral-sh/uv)（Python 包管理器）。

### 步骤 2：下载 SDK

从百度网盘开放平台下载 [Python SDK-Stdio 模式](https://pan.baidu.com/union/doc/Cm9si7mfw)源码，解压到本地目录。

### 步骤 3：配置

```json
{
  "mcpServers": {
    "baidu-netdisk-local": {
      "type": "stdio",
      "command": "/你的uv路径/uv",
      "args": [
        "--directory",
        "/SDK解压目录/netdisk-mcp-server-stdio",
        "run",
        "netdisk.py"
      ],
      "env": {
        "BAIDU_NETDISK_ACCESS_TOKEN": "你的AccessToken"
      }
    }
  }
}
```

> macOS 可通过 `which uv` 获取 uv 的绝对路径。

---

## 六、验证连接

重启 AI 工具后，输入以下命令验证：

**Claude Code / CodeBuddy Code：**
```
/mcp
```

如果看到 `baidu-netdisk` 在已连接列表中，说明配置成功！

你也可以直接对话测试：

```
查看我的网盘容量
```

AI 会调用 `get_quota` 工具，返回你的网盘空间使用情况。

---

## 七、实战演示

### 1. 查看网盘信息

```
查看我的网盘容量和账号信息
```

AI 返回：
- 总空间：13 TB
- 已使用：1.34 TB
- 账号昵称、VIP 类型等

### 2. 浏览文件

```
查看网盘根目录有什么文件
```

```
查看"编程"文件夹里的内容
```

AI 调用 `file_list`，返回目录下所有文件和文件夹的名称、大小、类型。

### 3. 搜索文件

**关键词搜索：**
```
搜索网盘中所有包含"Java"的文件
```

**语义搜索（更智能）：**
```
找一下网盘里关于微服务架构的资料
```

语义搜索支持自然语言描述，比如"去年拍的照片"、"和项目部署相关的文档"，AI 都能理解并搜索。

### 4. 创建文件夹

```
在网盘根目录创建一个叫"项目文档"的文件夹
```

### 5. 移动和重命名

```
把"编程"文件夹里的"idea_project.rar"移动到"软件"文件夹下
```

```
把"运行 若依项目"重命名为"若依环境搭建"
```

### 6. 批量整理网盘

这是最强大的用法！一句话让 AI 帮你整理混乱的网盘：

```
帮我整理网盘：
1. 查看根目录所有文件夹
2. 把文件按"学习/软件/个人/文档/媒体"5大类重新归类
3. 不规范的文件夹名帮我重命名
```

AI 会自动扫描全盘 → 分析分类 → 创建目录 → 移动文件 → 重命名，全程自动完成！

### 7. 上传文件

**通过 URL 上传（SSE/Stdio 均支持）：**
```
把这个 URL 的文件上传到网盘的"资料"文件夹：https://example.com/file.pdf
```

**通过文本内容上传：**
```
把以下内容保存为"会议纪要.docx"上传到网盘根目录：
[你的文本内容...]
```

**上传本地文件（仅 Stdio 模式）：**
```
把本地的 /Users/me/report.xlsx 上传到网盘的"工作"文件夹
```

### 8. 创建分享链接

```
把"学习资料"文件夹创建一个分享链接，有效期7天，密码1234
```

---

## 八、进阶技巧

### 技巧 1：环境变量管理 Token

不要把 Access Token 明文写在配置文件里！用环境变量更安全：

```json
{
  "mcpServers": {
    "baidu-netdisk": {
      "type": "sse",
      "url": "https://mcp-pan.baidu.com/sse?access_token=${BAIDU_NETDISK_TOKEN}"
    }
  }
}
```

然后在系统中设置环境变量：

```bash
export BAIDU_NETDISK_TOKEN="你的AccessToken"
```

### 技巧 2：批量操作用异步模式

移动、复制、删除大量文件时，指定异步模式避免超时：

```
把以下文件移动到"归档"文件夹，用异步模式：
- /旧文件1
- /旧文件2
- /旧文件3
```

异步模式会返回 taskid，AI 会自动跟踪任务状态。

### 技巧 3：用语义搜索找"说不清"的文件

有时候你记不清文件名，但记得内容：

```
找一下网盘里关于数据库分库分表的资料
```

```
搜索网盘中和 Docker 部署相关的文件
```

语义搜索比关键词搜索更智能，能理解你的意图。

### 技巧 4：同时配置 SSE + Stdio

如果你既想用远程便捷访问，又偶尔需要上传本地文件，可以同时配置两个：

```json
{
  "mcpServers": {
    "baidu-netdisk": {
      "type": "sse",
      "url": "https://mcp-pan.baidu.com/sse?access_token=${BAIDU_NETDISK_TOKEN}"
    },
    "baidu-netdisk-local": {
      "type": "stdio",
      "command": "uv",
      "args": ["--directory", "/path/to/sdk", "run", "netdisk.py"],
      "env": {
        "BAIDU_NETDISK_ACCESS_TOKEN": "${BAIDU_NETDISK_TOKEN}"
      }
    }
  }
}
```

---

## 九、常见问题

### Q: Access Token 过期了怎么办？

A: Token 有有效期，过期后需要重新获取。个人体验 Token 有效期较短，建议申请企业认证获取长期 Token。更新 Token 后修改配置文件中的值，重启 AI 工具即可。

### Q: 提示"系统繁忙"或操作失败？

A: 可能原因：
1. Token 已过期 → 重新获取
2. 操作频率过高 → 稍后再试
3. 文件路径不存在 → 检查路径是否正确（路径需以 `/` 开头）

### Q: 文件夹移动失败，报 errno -9？

A: 百度网盘 API 对文件夹移动有特殊要求，可以尝试：
1. 使用异步模式（async=2）
2. 不指定 newname，先移动再重命名
3. 检查目标路径是否存在

### Q: 删除文件夹提示需要身份验证？

A: 百度网盘对批量删除有安全验证机制，需要在网盘客户端确认操作，或分批小量删除。

### Q: SSE 模式和 Stdio 模式能同时用吗？

A: 可以！SSE 模式响应更快（远程服务），Stdio 模式支持本地上传。两个可以同时配置，按需使用。

### Q: 支持哪些 AI 工具？

A: 所有支持 MCP 协议的工具都可以接入，包括：
- Claude Code
- CodeBuddy Code
- Cursor
- Cline
- Windsurf
- 以及其他 MCP 兼容工具

---

## 十、官方资源

- 📦 **GitHub 仓库**: [baidu-netdisk/mcp](https://github.com/baidu-netdisk/mcp)
- 📚 **开放平台文档**: [https://pan.baidu.com/union/doc/](https://pan.baidu.com/union/doc/)
- 🔑 **应用管理控制台**: [https://pan.baidu.com/union/console/applist](https://pan.baidu.com/union/console/applist)
- 🐍 **Python SDK-Stdio**: [https://pan.baidu.com/union/doc/Cm9si7mfw](https://pan.baidu.com/union/doc/Cm9si7mfw)

---

## 写在最后

以前管理百度网盘，你要打开网页 → 一层层点进去 → 手动拖拽移动 → 翻半天找不到文件；现在让 AI 一句话搞定——搜索、整理、上传、分享，全部自然语言交互。

**百度网盘 MCP 让你的云盘文件从"存着吃灰"变成"随时可用"**。

快去试试吧！有问题评论区留言～
