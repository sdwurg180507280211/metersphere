# 🔥 阿里开源「小爪子」CoPaw：三条命令拥有专属 AI 管家，钉钉飞书 QQ 随叫随到

> 上周 Anthropic 的 Claude Code 让"AI 编程终端"刷屏，这周阿里通义实验室也憋不住了——CoPaw 正式开源，一只会记住你、懂你习惯、还能跨平台聊天的"小爪子"来了。

---

## 它到底是什么？

**CoPaw**，全称 **Co Personal Agent Workstation**（协同个人智能体工作台），是阿里云通义实验室基于 AgentScope 打造的开源个人 AI 助理。

名字里藏了一个双关：co-paw，既是"协同"，也是那只随时准备帮你的"小爪子"。团队希望它不是冷冰冰的工具，而是数字生活里最默契的搭档。

一句话概括它的定位：**你在哪个 App 发消息，它就在哪个 App 回你。**

支持接入：钉钉 / 飞书 / QQ / Discord / iMessage / 原生控制台

开源协议：Apache 2.0，免费商用。

---

## 三条命令，本地跑起来

环境要求：Python 3.10 ~ 3.13

```bash
pip install copaw        # 安装
copaw init               # 交互式初始化（引导你配置模型和 API Key）
copaw app                # 启动服务
```

启动后打开浏览器访问 `http://127.0.0.1:8088/`，进入 Web 控制台，对话、频道、定时任务、技能、模型全在这里管。

Docker安装方式如下：

```bash
docker pull agentscope/copaw:latest
docker run -d --name copaw -p 8088:8088 -v copaw-data:/app/working agentscope/copaw:latest
```

没有服务器也没关系，魔搭创空间支持**一键云端部署**，零本地环境，打开页面就能用。注意把创空间设为非公开，否则别人也能操控你的 CoPaw。

---

## 配置 API Key：三种方式任选

CoPaw 支持 DashScope（通义千问）、ModelScope、OpenAI 以及自定义 Base URL，也支持本地模型（llama.cpp / MLX，Apple Silicon 原生加速）。

**方式一：`copaw init` 引导配置**（推荐新手）

运行初始化命令时，会一步步引导你选择提供商、填写 Key。

**方式二：控制台配置**

启动后进入 `http://127.0.0.1:8088/` → 设置 → 模型，填写 API Key 并启用即可。

**方式三：环境变量 / `.env` 文件**

```bash
DASHSCOPE_API_KEY=your-key-here
# 也支持
# OPENAI_API_KEY=your-openai-key
# MODELSCOPE_API_KEY=your-modelscope-key
```

命令行党也有专属命令：

```bash
copaw models list                      # 查看当前状态
copaw models config-key dashscope      # 只配 DashScope Key
copaw models config-key custom         # 配置自定义 Base URL + Key
copaw models set-llm                   # 切换模型
```

**想完全本地运行、零 API 费用？**

```bash
copaw models download Qwen/Qwen3-4B-GGUF   # 下载本地模型
copaw models                                 # 选择刚下载的模型
copaw app                                    # 启动
```

---

## 它能帮你干什么？

### 1. 随叫随到的多平台聊天

在控制台配置好频道凭据后，你在钉钉、飞书、QQ 发消息，CoPaw 就在那个 App 里直接回你。不用切换 App，不用单独开网页。

```bash
copaw channels add dingtalk    # 添加钉钉
copaw channels config          # 交互式填写 AppKey / AppSecret
```

### 2. 越用越懂你的长期记忆

CoPaw 的"灵魂"存在工作目录下的 `PROFILE.md` 等文件里。第一次对话时它会引导你自我介绍，之后会主动把你的**决策、偏好、待办**写进记忆，并在心跳期间定期维护。

这意味着：你说过"我不喜欢太长的总结"，下次它就记得。

### 3. 定时任务 + 心跳机制

告诉它"每天早上 8 点给我发一份科技早报"，它就会按时执行。内置技能覆盖：

- 小红书 / 知乎 / Reddit / B站 热帖摘要
- 邮件 & Newsletter 精华推送
- 天气查询、股价监控
- 本地文件整理与搜索
- 文档阅读与摘要

### 4. 自定义 Skills，无限扩展

在工作目录的 `skills/` 下新建 `SKILL.md` 文件，描述清楚功能，CoPaw 就能学会新技能。不需要改底层代码，普通用户也能扩展。

---

## 架构亮点（给开发者看的）

- **模块化核心**：Prompt、Hooks、Tools、Memory 解耦，可独立替换任意模块。
- **多推理后端**：云端 API / 自建推理 / Ollama / llama.cpp / MLX，一套配置全适配。
- **可靠的多频道消息队列**：引入消费队列机制，多平台并发消息不丢失。
- **记忆底层**：由 ReMe 提供，支持跨回合经验沉淀、MEMORY.md、每日笔记等结构化记忆。

配置和数据统一存储在 `~/.copaw` 目录，便于备份迁移。

---

## 和 Claude Code 有什么不同？

| | Claude Code | CoPaw |
|---|---|---|
| 定位 | AI 编程终端 | 个人 AI 生活助理 |
| 交互入口 | 终端命令行 | 钉钉 / 飞书 / QQ 等聊天软件 |
| 核心能力 | 代码生成与工程任务 | 日程、资讯、文件、记忆管理 |
| 记忆机制 | 项目级上下文 | 长期个人记忆，越用越懂你 |
| 开源 | 部分开源 | 完全开源（Apache 2.0） |
| 本地运行 | 需要订阅 API | 支持完全本地模型（零 API 费用）|

两者定位互补，不是竞争关系。

---

## 未来路线图

官方透露的下一步方向：

1. **大小模型协同**：轻量本地模型处理隐私数据，云端强模型处理复杂规划和编码。
2. **多模态交互**：期待与你的 CoPaw 进行**语音和视频通话**。

---

## 上手资源

- 官网：https://copaw.agentscope.io
- GitHub：https://github.com/agentscope-ai/CoPaw
- 阿里云开发者社区文档：https://developer.aliyun.com/article/1713682
- 魔搭创空间一键部署（搜索 CoPaw）

---

三条命令，5 分钟，你就能拥有一个住在钉钉或飞书里、记得你所有偏好的 AI 管家。

阿里这只「小爪子」，值得一试。
