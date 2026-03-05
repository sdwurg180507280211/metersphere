140元 115美元 0.19
35元

下载Figma桌面版：https://desktop.figma.com/mac-installer/Figma.dmg
页面右下角开始dev model
文档：https://help.figma.com/hc/en-us/articles/32132100833559-Guide-to-the-Figma-MCP-server#h_01K25F7RCDRH0C6M004GG3KMB7



# ClaudeCode连接Figma的完整步骤

要让ClaudeCode与Figma连接，实现UI设计稿到代码的自动转换，以下是详细操作步骤：

## 前提条件
1. 已安装Figma桌面客户端
2. 已安装ClaudeCode（参考[1][2]的安装方法）
3. 有Figma账号且登录状态

## 连接步骤

### 第一步：在Figma中启用MCP服务器
1. 打开Figma桌面客户端
2. 点击菜单栏的"Preferences"（偏好设置）
3. 找到并勾选"Enable Dev Mode"或"Enable local MCP server"选项
4. 重启Figma确保设置生效
5. 验证MCP服务是否启动：在浏览器中访问 `localhost:3845/sse`，如果显示Figma MCP服务页面，说明已成功启用

### 第二步：配置ClaudeCode连接Figma

#### 方法一：使用命令行（推荐）
1. 打开终端（命令行）
2. 执行以下命令：
   ```bash
   claude mcp add --transport http figma https://mcp.figma.com/mcp
   ```
3. 系统会提示你打开浏览器完成登录（如果自动打开失败，会显示一个URL，需手动复制到浏览器打开）
4. 登录Figma账号后，会显示连接成功提示

#### 方法二：通过配置文件（备用方案）
1. 找到ClaudeCode的配置文件：
   - macOS/Linux: `~/.claude/settings.json`
   - Windows: `C:\Users\<用户名>\.claude\settings.json`
2. 如果文件不存在，创建它
3. 添加以下配置内容：
   ```json
   {
     "env": {
       "ANTHROPIC_AUTH_TOKEN": "your_api_key_here",
       "API_TIMEOUT_MS": "3000000",
       "ANTHROPIC_BASE_URL": "https://open.bigmodel.cn/api/anthropic",
       "MCP_TOOL_TIMEOUT": "30000"
     },
     "permissions": {
       "defaultMode": "bypassPermissions"
     },
     "alwaysThinkingEnabled": false
   }
   ```
   > 注意：`ANTHROPIC_AUTH_TOKEN`需要替换为你的API密钥（如GLM 4.7等国产模型的API Key）

### 第三步：验证连接
1. 在终端中启动ClaudeCode：`claude`
2. 输入`/mcp`命令，确认Figma MCP已连接
3. 或者输入`claude mcp list`查看已连接的MCP服务

### 第四步：使用Figma设计稿生成代码
1. 在ClaudeCode中输入提示词：
   ```
   根据提供的Figma链接，精准还原UI设计（不要修改现有导航栏，不要绘制链接中导航栏），绘制在pages/index中，不要做其他改动。Figma链接：https://www.figma.com/design/...
   ```
2. ClaudeCode会自动获取Figma设计稿的结构和样式，生成对应代码

## 常见问题解决

### 问题1：无法连接Figma
- **原因**：Figma未正确启用MCP服务器
- **解决**：重新检查Figma的Preferences > Enable Dev Mode

### 问题2：提示"获取不到Figma链接"
- **原因**：Figma客户端未打开或未登录
- **解决**：确保Figma桌面客户端已打开并登录，然后重新启动ClaudeCode

### 问题3：需要API密钥
- **原因**：如果使用国产模型（如GLM 4.7），需要配置API密钥
- **解决**：按照[1][2][9]的方法获取并配置GLM API Key

## 高级技巧

1. **提升还原度**：在调用MCP工具前，先上传Figma设计稿的截图，让AI更精准理解设计意图
2. **自定义配置**：在`.mcp.json`文件中配置设计Token映射规则
3. **团队协作**：设置设计变更自动通知机制，代码库PR中标记关联的Figma文件版本

> 根据知识库[3]，使用ClaudeCode+MCP可以实现"设计系统与代码库的版本对齐"，生成的代码会自动保留设计系统的层级关系，便于后续迭代。

## 重要提示

- Figma MCP服务仅在桌面客户端中可用，网页版Figma不支持
- 首次连接需要Figma账号授权，确保使用的是有开发权限的账号
- 如果使用国产模型（如GLM 4.7），需要在配置文件中设置正确的API URL和密钥

按照以上步骤操作，你就能成功让ClaudeCode连接Figma，实现从UI设计稿到前端代码的自动化转换。