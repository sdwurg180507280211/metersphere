# 手把手教你用 OpenClaw + 飞书，打造专属 AI 机器人

> 当前版本 OpenClaw（2026.2.22-2）已内置飞书插件，无需额外安装。

你有没有想过，在飞书里直接跟 AI 对话，就像跟同事聊天一样自然？

今天这篇文章，带你从零开始，用 **OpenClaw** 搭建一个飞书 AI 机器人。全程命令行操作，10 分钟搞定。

---

## 一、准备工作

### 1.1 安装 Node.js（版本 ≥ 22）

OpenClaw 依赖 Node.js 运行，首先确保你的 Node 版本不低于 22。

推荐使用 nvm 管理 Node 版本，打开终端执行：

```bash
# 安装 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.4/install.sh | bash

# 加载 nvm（避免重启终端）
\. "$HOME/.nvm/nvm.sh"

# 安装 Node.js 24
nvm install 24

# 验证安装
node -v  # 应输出 v24.13.1
npm -v   # 应输出 11.8.0
```

> 📎 Node.js 官网：https://nodejs.org/en/download

![Node.js 下载页面](https://i-blog.csdnimg.cn/img_convert/25c815d397d849b8fbe9ac498f97cbc6.png)

### 1.2 安装 OpenClaw

进入 OpenClaw 官网 https://openclaw.ai/ ，我们通过 npm 全局安装：

```bash
# 安装 OpenClaw
npm i -g openclaw

# 运行引导向导（加 --install-daemon 安装守护进程，让 Gateway 在后台持续运行）
openclaw onboard --install-daemon
```

> 💡 加了 `--install-daemon` 后，Gateway 会作为系统守护进程自动启动（macOS 用 LaunchAgent，Linux 用 systemd），重启电脑也不会丢。如果不加，后面启动 `openclaw gateway` 时关掉终端机器人就断了。

> 📎 GitHub 地址：https://github.com/openclaw/openclaw

![OpenClaw 官网](https://i-blog.csdnimg.cn/img_convert/8fd7fc6dc2d808d3d011f8c4a12bf958.png)

---

## 二、配置 AI 模型

OpenClaw 需要接入一个大语言模型作为"大脑"。你可以使用 Anthropic、OpenAI 等官方 API，也可以通过第三方中转服务获取。

这里以 Claude Haiku 为例。Haiku 是 Anthropic 的轻量级模型，响应快、成本低，非常适合日常对话场景。如果你需要更强的推理能力，也可以换成 `claude-sonnet` 或 `claude-opus` 等模型。

在终端依次执行以下命令：

```bash
# 1. 设置 API 地址
openclaw config set -- models.providers.my_api.baseUrl "你的API地址"

# 2. 设置 API 类型
openclaw config set -- models.providers.my_api.api "openai-completions"

# 3. 设置 API Key
openclaw config set -- models.providers.my_api.apiKey "你的API密钥"

# 4. 设置默认模型（可替换为其他模型）
openclaw config set -- agents.defaults.model.primary "my_api/claude-haiku-4-5-20251001"
```

> ⚠️ 注意：`config set` 和路径之间的 `--` 是两个英文半角短横线，不要从网页复制，手动敲键盘上的减号键两次。

如果你不习惯命令行，也可以通过 Dashboard 配置：

1. 启动 Gateway 后，浏览器打开 http://127.0.0.1:18789/
2. 点击顶部 Config 标签页，切换到 Raw JSON 编辑器
3. 找到 `models.providers` 部分，直接编辑 JSON
4. 保存后 Gateway 自动热加载生效，无需重启

> 💡 Dashboard 的 Form 表单视图对自定义 Provider 支持有限，建议直接用 Raw JSON 编辑器修改。

---

## 三、创建飞书应用

### 3.1 进入飞书开放平台

访问 https://open.feishu.cn/app ，如果没有账号，用手机号注册即可。

### 3.2 创建企业自建应用

按照下图步骤操作：

![创建应用 - 步骤1](https://i-blog.csdnimg.cn/img_convert/68bbc0c7f11493187c67d84dbd45ceae.png)

![创建应用 - 步骤2](https://i-blog.csdnimg.cn/img_convert/764a912c53f1e16ddcf6793a3ab44cf9.png)

![创建应用 - 步骤3](https://i-blog.csdnimg.cn/img_convert/f9d3e399da4e4b9d1cc121bb04b3fca1.png)

![创建应用 - 步骤4](https://i-blog.csdnimg.cn/img_convert/075a60c967f07d7dd4701972c66e5dd2.png)

创建完成后点击**发布**。

### 3.3 记录 App ID 和 App Secret

这两个值后面配置 OpenClaw 时要用到，务必保存好。

![App ID 和 App Secret](https://i-blog.csdnimg.cn/img_convert/d5a5a395dfcdd51e082cfbb3622f6cf0.png)

---

## 四、连接 OpenClaw 与飞书

回到终端，将飞书应用的凭证写入 OpenClaw 配置：

```bash
# 设置飞书 App ID
openclaw config set -- channels.feishu.appId "你的AppID"

# 设置飞书 App Secret
openclaw config set -- channels.feishu.appSecret "你的AppSecret"

# 启动 OpenClaw Gateway
openclaw gateway
```

> 💡 新版 OpenClaw 已内置飞书支持，不需要额外安装插件。

---

## 五、配置飞书机器人权限

回到飞书开放平台，为应用添加必要的权限和事件订阅。

在「权限管理」页面，搜索 `im`，将 `im` 相关的权限**全部勾选**（包括消息读取、发送、群组管理等），确保机器人能正常收发消息。

> 💡 OpenClaw 使用 WebSocket 模式连接飞书，不需要配置事件订阅的回调地址（Request URL），保持为空即可。

![权限配置 - 步骤1](https://i-blog.csdnimg.cn/img_convert/309301c19c666dbef4cacdbfd6b2bd93.png)

![权限配置 - 步骤2](https://i-blog.csdnimg.cn/img_convert/aa2f514604c05177cf71ef145ae862a3.png)

![权限配置 - 步骤3](https://i-blog.csdnimg.cn/img_convert/10b86e36f04175192267465dde39883d.png)

修改完权限后，需要**重新发布一个版本**，点击确认发布。

![重新发布](https://i-blog.csdnimg.cn/img_convert/7d32f8a93d599fc1456355f4b2c0ebfb.png)

---

## 六、首次使用：授权配对

在手机飞书上给机器人发一条消息，你会收到类似这样的提示：

```
OpenClaw: access not configured.
Your Feishu user id: ou_72c25a66a2248f494484a792b18d0c12
Pairing code: RYXBPRNJ
Ask the bot owner to approve with:
openclaw pairing approve feishu RYXBPRNJ
```

这是 OpenClaw 的安全机制，防止陌生人使用你的机器人。需要两步操作：

### 6.1 开启权限

在终端中点击提示的链接，开启相关权限。

![开启权限 - 步骤1](https://i-blog.csdnimg.cn/img_convert/6dc0fd85e8ae292b027ed6481d4c864c.png)

![开启权限 - 步骤2](https://i-blog.csdnimg.cn/img_convert/902d1115348162bfaaafec21e79ebf9c.png)

### 6.2 授权配对

在终端执行配对命令，将你的飞书账号绑定为授权用户：

```bash
openclaw pairing approve feishu RYXBPRNJ
```

> 将 `RYXBPRNJ` 替换为你实际收到的配对码。

![配对成功](https://i-blog.csdnimg.cn/img_convert/e6d3ea8b67fee1e6d52eae1fd17b1389.png)

> ⚠️ 每创建一个新的飞书机器人应用，都需要重新走一遍这个配对流程。

---

## 七、开始使用

一切就绪，现在可以在飞书里愉快地和 AI 对话了 🎉

![使用效果](https://i-blog.csdnimg.cn/img_convert/e603a7d8e55588ec694700f10ce16140.png)

---

## 写在最后

整个流程总结下来就三件事：

1. **装 OpenClaw**：npm 一行命令搞定
2. **建飞书应用**：拿到 App ID 和 Secret
3. **连起来**：配置好模型和飞书凭证，启动 Gateway

如果你在部署过程中遇到问题，欢迎留言交流。
