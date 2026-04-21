### 核心结论：**100%可落地实现**
这套「嵌入式AI虚拟伴侣+测试平台」的方案，所有技术模块均有成熟的开源实现、海量现成的免费/商用资源，甚至有大量可直接二次开发的整合Demo，技术上无不可逾越的壁垒，唯一需要投入的是前后端整合与业务联动的开发工作量。

下面是四大核心模块对应的**详细资源网站、开发工具与落地参考**，按开发优先级排序：

---

## 一、视觉与动作控制（Live2D 皮囊层）
这是最成熟、入门最快的模块，半天就能跑通基础效果，有海量合规可用的角色资源。

### 1. 核心开发库与官方工具
| 资源名称 | 官网/仓库地址 | 核心用途 |
| :--- | :--- | :--- |
| pixi-live2d-display | https://github.com/guansss/pixi-live2d-display | 方案核心前端库，兼容Cubism 2/3/4/5全版本Live2D模型，封装了极简的动作/表情API，支持鼠标跟随、点击交互、口型同步，有完整的Vue/React集成示例与在线Demo |
| Live2D 官方Cubism平台 | https://www.live2d.com/ | Live2D官方网站，可免费下载Cubism Editor建模工具（个人非商用免费）、官方SDK、无版权风险的示例模型，还有完整的口型同步、动作编辑教程 |
| live2d-widget 开箱即用看板娘 | https://github.com/stevenjoezhang/live2d-widget | 超火的网页看板娘项目，一行代码即可嵌入网页，自带换装、动作触发、聊天气泡、随机语录功能，已做好前端兼容，可直接修改对接你的后端接口，省去基础布局开发 |

### 2. 免费可商用Live2D模型资源站
合规是核心，以下站点均有明确的授权说明，避免侵权风险：
- **模之屋**：https://www.aplaybox.com/
  国内最大的Live2D模型社区，大量个人作者开源的免费模型，可商用的模型会明确标注，分类齐全，二次元人设丰富，直接下载即可使用，自带现成的动作和表情文件。
- **Live2D 官方免费素材库**：https://www.live2d.com/download/sample-data/
  官方提供的免费模型，完全无版权风险，支持个人与商用，自带完整的动作、表情、口型数据，新手入门首选。
- **Booth**：https://booth.pm/zh-cn
  日本创作者平台，海量低价/免费的Live2D模型，多数作者开放非商用授权，部分可商用，模型质量极高，动作表情丰富，适合做高质感看板娘。
- **B站Live2D社区**：搜索「免费Live2D模型」，大量UP主分享自己制作的开源模型，多数支持非商用使用，还附带详细的网页嵌入教程。

---

## 二、交流的大脑（LLM 灵魂层）
核心是实现符合人设的对话，有开箱即用的商用API和完全私有化的本地部署方案两种选择。

### 1. 开箱即用商用API（无需部署，Spring Boot直接对接）
| 平台 | 官网地址 | 适配优势 |
| :--- | :--- | :--- |
| 深度求索DeepSeek | https://www.deepseek.com/ | 国产大模型，API价格极低，长上下文支持好，角色扮演能力强，自带完整的Java SDK，可无缝集成到Spring Boot后端，适合做AI伴侣人设 |
| 月之暗面Kimi | https://www.moonshot.cn/ | 长文本能力极强，可直接把MeterSphere的测试报错日志、用例文档丢进去分析，对话连贯性与人设稳定性优秀，有完善的Java调用示例 |
| 通义千问 | https://tongyi.aliyun.com/ | 阿里出品，企业级稳定性拉满，有专门的角色扮演微调模型，提供Spring Boot集成starter，开箱即用，和同体系的火山引擎TTS搭配体验最佳 |
| 硅基流动 | https://www.guiji.ai/ | 主打AI数字人/伴侣的大模型平台，自带海量二次元人设模板，API对接极简，甚至内置语音合成能力，适合快速出Demo效果 |

### 2. 内网私有化部署方案（数据不对外，适合企业内网环境）
- **Ollama**：https://ollama.com/
  一行命令即可本地部署大模型的工具，支持Windows/macOS/Linux，一键启动Llama3、Qwen2等开源模型，自带HTTP接口，后端直接调用，完全不用管部署细节，新手本地方案首选。
- **通义千问开源版Qwen2**：https://github.com/QwenLM/Qwen2
  阿里开源的国产大模型，中文能力拉满，7B版本普通消费级显卡即可流畅运行，角色扮演效果优秀，支持本地部署，有完善的Java客户端。
- **Llama 3 系列**：https://github.com/meta-llama/llama3
  Meta开源的全球顶流大模型，7B/8B版本即可实现极强的角色扮演效果，有大量二次元人设的微调版本，社区生态完善。

### 3. 人设Prompt资源
- 中文角色扮演Prompt仓库：https://github.com/PlexPt/awesome-chatgpt-prompts-zh
  海量现成的二次元人设Prompt，傲娇助手、温柔看板娘等风格应有尽有，直接修改即可适配你的测试助理人设。
- 提示词工程指南：https://www.promptingguide.ai/zh
  系统学习稳定人设的Prompt写法，解决长对话角色崩坏、记忆丢失等问题。

---

## 三、专属声线（VITS 语音合成层）
### 1. 开箱即用商用API（无需显卡，直接对接）
- **火山引擎语音合成**：https://www.volcengine.com/product/tts
  字节跳动出品，有大量二次元、动漫风格的精品声线，支持多情感调节、口型同步数据输出，API对接简单，有完整的Java SDK。
- **腾讯云语音合成**：https://cloud.tencent.com/product/tts
  专门的二次元动漫声线专区，支持多情感、多语种，价格低廉，企业级稳定性强。
- **鱼耳TTS**：https://www.yuera.com/
  主打二次元声线的垂直TTS平台，海量V家、动漫风格声线，API专为AI伴侣设计，对接门槛极低。

### 2. 开源本地部署方案（内网可用，完全免费）
| 项目 | 仓库地址 | 核心优势 |
| :--- | :--- | :--- |
| Bert-VITS2 | https://github.com/fishaudio/Bert-VITS2 | 目前最火的二次元中文语音合成模型，支持多情感、多说话人，普通消费级显卡即可部署，自带HTTP接口，后端可直接调用，社区有海量免费声线模型 |
| GPT-SoVITS | https://github.com/RVC-Boss/GPT-SoVITS | 零样本语音克隆神器，仅需5分钟的语音素材，即可克隆你喜欢的动漫声线，效果极强，自带API服务，适合定制专属声线 |
| MoeTTS | https://github.com/haopiziji/MoeTTS | 专为Galgame/二次元角色设计的TTS仓库，内置10+热门动漫角色的VITS模型，自带预编译GUI，新手友好 |

### 3. 声线模型资源站
- **ModelScope魔搭社区**：https://modelscope.cn/models
  阿里AI模型社区，大量用户开源的Bert-VITS2、GPT-SoVITS声线模型，二次元动漫风格应有尽有，直接下载即可免费使用。
- **Hugging Face**：https://huggingface.co/models
  全球最大AI模型社区，搜索「VITS」「Bert-VITS2」即可找到海量中日英多语言的动漫声线模型。
- **B站TTS社区**：搜索「Bert-VITS2 模型分享」，大量UP主分享自己训练的免费动漫声线，附带一键启动包和部署教程。

---

## 四、养成系统核心（状态机+业务联动）
这是方案的精髓，也是和MeterSphere深度绑定的核心，官方提供了完整的二次开发支持。

### 1. MeterSphere 二次开发官方资源
- **官方文档中心**：https://metersphere.io/docs/
  完整的二次开发指南、插件开发教程、后端API文档、事件监听机制说明，详细讲解了如何拦截测试用例执行、流水线构建、Bug提交等核心业务事件，这是你养成系统的触发核心。
- **官方GitHub仓库**：https://github.com/metersphere/metersphere
  完整的前后端源码，可直接参考后端的拦截器、事件发布机制，以及前端Vue项目的结构，直接把Live2D看板娘嵌入到前端页面中。
- **插件开发示例**：官方仓库的`plugin-examples`目录，提供了多种业务扩展插件的示例，推荐用插件方式开发，无需修改核心源码，后续MeterSphere版本升级不受影响，是官方推荐的扩展方式。

### 2. 状态机与数据持久化资源
- **Spring StateMachine**：https://spring.io/projects/spring-statemachine
  Spring官方状态机框架，完美适配Spring Boot后端，用来实现角色等级、心情、好感度的状态流转，以及事件触发逻辑，比手写if-else更优雅、更易维护。
- **MyBatis-Plus**：https://baomidou.com/
  国内最主流的ORM框架，和Spring Boot无缝集成，一行代码即可实现角色属性的增删改查，无需手写复杂SQL，快速实现养成数据的持久化。

### 3. 业务事件拦截实现参考
- **Spring AOP 切面拦截**：https://docs.spring.io/spring-framework/reference/core/aop.html
  用AOP切面拦截MeterSphere里测试执行、构建成功/失败、Bug提交等核心方法，触发对应的经验增加、心情变化、道具奖励等逻辑，实现简单直接。
- **Spring 事件监听机制**：https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events
  基于事件驱动的方式，解耦业务代码和养成系统代码，MeterSphere本身内置了大量业务事件，可直接监听，完全无需修改核心源码，耦合度最低，最推荐使用。

---

## 五、拿来即用的完整整合项目
这些项目已经把「Live2D+LLM+TTS+交互」全部整合完毕，你只需对接MeterSphere的业务事件，即可快速实现需求，无需从零搭建：
1.  **Open-LLM-VTuber**：https://github.com/Open-LLM-VTuber/Open-LLM-VTuber/
    全功能AI虚拟伴侣项目，支持实时语音对话、Live2D形象、离线本地运行，兼容Windows/macOS/Linux，提供Web版和桌面端，自带透明背景桌宠模式，模块化设计极易二次开发对接。
2.  **AI-Vtuber**：https://github.com/Ikaros-521/AI-Vtuber
    模块化开源AI虚拟主播项目，整合了主流大模型、VITS语音合成、Live2D/VRM形象、多模态识别，有完整的前后端分离架构，可直接参考其交互逻辑。
3.  **VTube-Sama-App**：https://github.com/Westworld-AI/VTube-Sama-App
    超低上手成本的AI Vtube项目，支持OpenAI、Ollama本地部署、智谱等大模型，前端用Electron+React开发，动画基于Pixi.js+Live2D实现，自带数据库，可直接复用其状态管理逻辑。

---

## 入门避坑建议
1.  **开发顺序推荐**：先跑通前端Live2D模型加载与动作触发（半天搞定）→ 对接LLM实现基础聊天交互 → 新增语音合成与口型同步 → 最后对接MeterSphere业务事件实现养成系统，循序渐进，每一步都能看到效果，挫败感最低。
2.  **版权提示**：商用场景务必确认模型、声线的授权，优先使用官方免费、作者明确开放商用授权的资源，避免侵权风险。
3.  **性能优化**：Live2D模型避免使用面数过高的资源，防止影响测试平台性能；大模型和TTS调用建议用异步处理，不要阻塞MeterSphere的主业务流程。


这就不仅仅是简单的“换肤”了，而是给测试平台注入了“灵魂”！你实际上是在构建一个 **嵌入式 AI 虚拟伴侣（Web AI Agent + 电子宠物）**。

关于“有没有人实现过”：在企业级后台管理系统中，这种做法极少见（因为老板觉得不务正业）。但在开源社区和极客圈里，**“LLM 大模型 + Live2D 看板娘”** 的组合非常火爆，GitHub 上有大量类似“AI VTuber”或“智能网页桌宠”的开源项目。把你设想的这一套集成到测试平台里，绝对是一个极其硬核且充满创意的极客玩法。

要完整实现这个“可交流 + 养成系”的动漫伴侣，你需要把前后端打通，具体可以拆解为以下四大核心模块：

### 1. 视觉与动作控制 (Live2D + PixiJS)

这是角色的“皮囊”。

* **技术选型**：使用 `pixi-live2d-display` 这个前端库。它支持加载 Live2D 的高精度模型（Cubism 2/3/4 版本）。
* **实现效果**：模型不仅会随着鼠标转头、眨眼，**最关键的是它暴露了动作 API**。你可以通过代码触发她特定的动作（Motion）和表情（Expression）。比如，当她升级时，你调用 API 让她播放一段“欢呼”的动画。

### 2. 交流的大脑 (LLM 大语言模型)

这是角色的“灵魂”，让她能理解你说的话并给出符合人设的回复。

* **技术选型**：在后端接入 DeepSeek、Kimi、通义千问等大模型的 API。
* **核心实现 (Prompt Engineering)**：你需要给她设定一个强大的 System Prompt（系统提示词）。
* *例如：“你是一个傲娇的测试助理，名字叫XXX，当你指出主人的 Bug 时会带有一点嘲讽，但内心其实很关心他。请用符合二次元设定的口吻回答问题。”*


* **交互方式**：在前端做一小块聊天气泡，或者接入 Web Speech API 实现语音输入。

### 3. 专属声线 (VITS 语音合成)

光有文字不够，还得有动漫声线。

* **技术选型**：使用 **VITS**（目前二次元语音合成最强开源模型）或 Bert-VITS2。
* **实现方式**：可以单独起一个 Python 服务跑语音模型，或者找现成的二次元 TTS API。后端 LLM 生成回复文本后，先传给 TTS 转成音频流（WAV/MP3），再返回给前端的 `<audio>` 标签自动播放，同时配合 Live2D 的口型同步（Lip-sync）功能，效果会非常惊艳。

### 4. 养成系统核心 (状态机与业务联动)

这是系统的“心脏”，也是最能体现你二次开发深度的部分。你需要记录她的成长。

* **后端数据持久化**：你可以直接在后端（Spring Boot）里新建一张表（比如 `anime_pet_status`），用来存储她的核心属性：**等级 (Level)、经验值 (EXP)、好感度 (Affection)、心情值 (Mood)**。写几个 Controller 来处理数据的增删改查。
* **与 MeterSphere 业务深度绑定（精髓所在）**：
  养成不能只靠干巴巴的点鼠标，应该和你的日常开发测试工作结合起来：
* **触发经验增长**：当你在平台上成功执行了一次接口测试，或者流水线（CI/CD）成功跑通，后端拦截器自动给角色增加 10 点 EXP。
* **心情系统联动**：如果连续构建失败报错，角色的心情值会下降，甚至画面上会飘出乌云；这时她可能会主动弹气泡安慰你，或者吐槽你的代码。
* **道具系统**：发现并提交一个高优先级的 Bug，奖励一个“草莓蛋糕”道具，在前端点击投喂可以大幅增加好感度。



---

**总结起来：**
前端负责 `Vue + Live2D + 音频播放`，后端负责 `模型状态存储 + 拦截测试平台业务事件 + 调用 LLM/TTS API`。

这个工程量不算小，但也极其有趣。你想先从哪个部分入手？我们可以先聊聊怎么在后端的 MySQL 和代码里设计这个”养成状态机”，或者先探讨如何在前端把带动画的 Live2D 模型跑起来？

---
---

## 六、结合本项目的技术可行性分析

> 以下分析基于对 MeterSphere v2.10 完整源码（前后端所有模块）的实际审查，而非通用性建议。

### 0. 项目现有技术栈速查

| 层次 | 技术 | 版本 | 备注 |
|:---|:---|:---|:---|
| **前端主壳** | Vue 2.7 + Webpack + Element UI | 2.7.3 | Options API，全部业务模块共用 |
| **前端子应用** | Vue 3.4 + Vite + Element Plus + TypeScript | 3.4.15 | 仅 `analytics-stat` 模块，micro-app UMD 模式嵌入 |
| **微前端框架** | @micro-zoe/micro-app | 1.0.0-rc.4 | 子应用隔离、独立路由、独立构建 |
| **后端框架** | Spring Boot 3.2 + Spring Cloud 2023 | 3.2.12 | Java 17，微服务架构 |
| **服务治理** | Eureka + Spring Cloud Gateway | — | 网关端口 8000，按 URL 前缀路由 |
| **数据库** | MySQL + MyBatis + Flyway | MyBatis 3.0.3 | 注意：项目用的是 MyBatis 原生，**不是** MyBatis-Plus |
| **缓存/会话** | Redis (Redisson 3.25) | — | spring-session-data-redis |
| **消息队列** | Apache Kafka | 3.8.1 | 异步解耦，已用于知识库文件处理管道 |
| **文件存储** | MinIO | 8.5.x | 知识库文件、附件存储 |
| **搜索引擎** | Elasticsearch 8.10 | 8.10.0 | KNN 向量 + BM25 混合检索 |
| **WebSocket** | spring-boot-starter-websocket | — | 已有通知推送、任务中心实时更新 |
| **AI 能力** | 通义千问 text-embedding-v4 (2048维) | — | 已在 `analytics-stat` 中接入 Embedding API |
| **安全框架** | Apache Shiro 2.0 | 2.0.1 | Session + X-AUTH-TOKEN |
| **部署** | Docker + Jenkins CI/CD | — | 多平台镜像 (amd64/arm64) |

### 1. 模块一：Live2D 视觉层 — 可行性评估

**结论：完全可行，与现有架构高度兼容**

#### 优势分析

- **micro-app 架构是天然优势**：`analytics-stat` 模块已经是一个独立的 Vue 3 + Vite 子应用（入口文件 `analytics-stat/frontend/src/main.ts`），通过 `window.mount()` / `window.unmount()` 生命周期嵌入主壳。Live2D 看板娘可以作为同类型的独立子应用接入，也可以直接在 `analytics-stat` 模块内新增路由页面，**完全不影响其他 Vue 2 模块**。

- **Vite 构建链完美支持**：`pixi-live2d-display` 是标准的 npm 包，可直接 `npm install` 到 `analytics-stat/frontend/package.json`，Vite 原生支持 Tree-shaking，不会对现有构建产生副作用。

- **现有 CSS 隔离已到位**：`analytics-stat` 使用 `postcss-prefix-selector` 做样式隔离（见 `devDependencies`），Live2D 的 canvas 渲染不依赖 CSS，不会与 Element Plus 样式冲突。

#### 具体集成点

| 集成方式 | 实现路径 | 工作量 |
|:---|:---|:---|
| **方案A：全局桌宠** | 在 Vue 2 主壳的 `App.vue` 中挂载 `live2d-widget`，通过 `<script>` 标签直接引入，所有页面可见 | 0.5 天 |
| **方案B：子应用内嵌** | 在 `analytics-stat/frontend/src/views/` 下新建 `AnimePet.vue`，用 `pixi-live2d-display` + Vue 3 Composition API 封装，注册为新路由 | 1-2 天 |
| **方案C：独立微前端子应用** | 参考 `analytics-stat/frontend/src/main.ts` 的 micro-app UMD 模式，新建独立的 `anime-pet` 子应用，网关层新增路由 `/anime/**` | 2-3 天 |

**推荐方案A快速验证 + 方案B做正式集成**。方案A半天出效果，方案B可以复用 `analytics-stat` 的 Vue 3 + TypeScript 基础设施。

#### 风险点

- `pixi-live2d-display` 依赖 PixiJS，打包体积约 300KB（gzip 后），需确认主壳的 Webpack 加载不会受影响。方案B在 Vite 子应用内则无此顾虑。
- Live2D 模型文件（.moc3 + 贴图）通常 2-10MB，建议存储到 MinIO，按需加载，避免打包进前端静态资源。

---

### 2. 模块二：LLM 对话层 — 可行性评估

**结论：完全可行，现有基础设施已覆盖 80%**

#### 优势分析（已有能力可直接复用）

- **Embedding API 已通**：`analytics-stat` 后端的 `EmbeddingClient` 已对接通义千问 `text-embedding-v4`，证明后端 → 外部 AI API 的网络链路、鉴权、异步调用模式已跑通。新增 LLM Chat API 只需参照同一模式。

- **RAG 检索管道已就绪**：`KnowledgeSearchService` 已实现 KNN + BM25 混合检索，可直接作为 LLM 对话的知识增强源（即”先检索再回答”的标准 RAG 模式），不需要从零构建。

- **前端聊天 UI 已完成**：`ChatPanel.vue` + `useKnowledgeChat.ts` 已实现完整的聊天交互（消息列表、流式输出、中止生成、反馈评价、会话管理），**只需对接真实的 LLM 后端接口即可**。

- **流式输出基础已有**：`useKnowledgeChat.ts` 中的 `askQuestionStream` 已支持 `AbortController` + `onChunk` 回调的流式模式，后端只需实现 SSE（Server-Sent Events）或 chunked transfer 即可对接。

#### 需要新增的开发工作

| 工作项 | 涉及文件/模块 | 工作量 |
|:---|:---|:---|
| 后端 LLM Chat Controller | `analytics-stat/backend/.../knowledge/controller/` 新增 `KnowledgeChatController.java` | 1 天 |
| 后端 LLM Chat Service | 新增 `KnowledgeChatService.java`，调用 LLM API（DeepSeek/通义千问），集成 RAG 检索结果作为 context | 2 天 |
| System Prompt 管理 | 新增配置表或 YAML 配置，存储看板娘人设 Prompt | 0.5 天 |
| 对话历史持久化（可选） | 当前会话存在 localStorage，如需跨设备同步，新增 MySQL 表 + Flyway 迁移脚本 | 1 天 |

#### 关键适配点

- **ORM 注意**：项目使用 **MyBatis 原生**（Example 模式），不是 MyBatis-Plus。新增表需要手写 Mapper XML 或使用 MyBatis Generator 生成，不能直接用 `@TableName` 等 MyBatis-Plus 注解。
- **Flyway 迁移**：新表需在 `analytics-stat/backend/src/main/resources/db/migration/` 下新增 DDL 脚本，命名格式如 `V2_10_24__anime_pet.sql`，版本表为 `analytics_version`。
- **LLM API 调用建议走 WebFlux**：`analytics-stat` 后端已引入 `spring-boot-starter-webflux`，可用 `WebClient` 做非阻塞流式调用，与现有 Embedding 调用模式一致。

---

### 3. 模块三：语音合成层 — 可行性评估

**结论：可行，但属于锦上添花，建议放在最后实现**

#### 架构适配性

- 后端调用 TTS API（火山引擎/腾讯云）返回音频流 → 前端 `<audio>` 播放，这套链路与现有架构无冲突。
- 音频数据可走 MinIO 临时存储（已有 MinIO 基础设施），也可直接通过 HTTP 响应流式返回。
- 口型同步需要 TTS 返回 viseme 时间戳数据（火山引擎支持），前端据此驱动 `pixi-live2d-display` 的 lip-sync API。

#### 风险点

- **性能影响**：TTS 合成有明显延迟（200ms-2s），如果串行在 LLM 回复之后，用户等待时间会翻倍。建议 LLM 生成文本后立即返回前端显示，TTS 异步合成后再推送音频（可利用现有 WebSocket 通道推送）。
- **带宽消耗**：每条语音回复约 50-200KB，高频对话场景下需注意服务端带宽。
- **优先级建议**：MVP 阶段可完全跳过，纯文字 + Live2D 动作已经能达到 80% 的体验效果。

---

### 4. 模块四：养成系统 — 可行性评估

**结论：完全可行，MeterSphere 微服务架构为此提供了天然的事件捕获能力**

#### 事件源分析（项目中已有的可监听事件）

| 业务事件 | 触发模块 | 捕获方式 | 养成效果示例 |
|:---|:---|:---|:---|
| 接口测试执行成功/失败 | `api-test` | Kafka 消息 / Spring AOP | +10 EXP / 心情 -5 |
| 性能测试完成 | `performance-test` | Kafka 消息 | +20 EXP |
| 测试计划通过率达标 | `test-track` | Kafka 消息 | 好感度 +10，触发”欢呼”动作 |
| Bug 提交 | `test-track` | Kafka 消息 | +5 EXP，奖励道具 |
| 用户每日登录 | `system-setting` | Shiro Session 拦截 | 每日签到 +1 好感度 |
| 知识库问答 | `analytics-stat` | 本模块内直接调用 | +2 EXP |

**Kafka 是核心利器**：MeterSphere 的微服务间已通过 Kafka 做异步通信（如知识库文件处理用 `knowledge-file-processing` topic，插件管理用 `platform_plugin` topic）。养成系统只需在 `analytics-stat` 后端新增 `@KafkaListener` 监听相关业务 topic，即可零耦合地捕获其他模块的业务事件——**完全不需要修改其他模块的代码**。

#### 数据模型设计建议

```sql
-- Flyway: V2_10_24__create_anime_pet_tables.sql

CREATE TABLE anime_pet_profile (
    id          VARCHAR(50) PRIMARY KEY,
    user_id     VARCHAR(50) NOT NULL,
    name        VARCHAR(100) DEFAULT '测试酱',
    level       INT DEFAULT 1,
    exp         INT DEFAULT 0,
    affection   INT DEFAULT 50,
    mood        INT DEFAULT 80,
    model_id    VARCHAR(100) DEFAULT 'default',
    created_at  BIGINT NOT NULL,
    updated_at  BIGINT NOT NULL,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE anime_pet_event_log (
    id          VARCHAR(50) PRIMARY KEY,
    user_id     VARCHAR(50) NOT NULL,
    event_type  VARCHAR(50) NOT NULL,
    event_data  TEXT,
    exp_delta   INT DEFAULT 0,
    mood_delta  INT DEFAULT 0,
    created_at  BIGINT NOT NULL,
    INDEX idx_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 关键注意事项

- **Spring StateMachine 过重**：对于本场景（等级/心情/好感度的数值增减），直接用 Service 层逻辑 + 数值计算即可，引入 Spring StateMachine 反而增加复杂度。StateMachine 更适合有明确状态流转图的场景（如工单审批流），不建议在此使用。
- **MyBatis-Plus 不兼容**：文档中推荐的 MyBatis-Plus 与项目实际使用的 MyBatis 原生存在冲突（两者的 Mapper 扫描和 SqlSessionFactory 配置不同）。建议沿用项目现有的 MyBatis + Example 模式，或手写简单的 Mapper XML，保持技术栈一致性。

---

### 5. 综合可行性结论

| 模块 | 可行性 | 与现有架构兼容度 | 预估工作量 | 优先级 |
|:---|:---|:---|:---|:---|
| Live2D 视觉层 | 完全可行 | 高（micro-app 隔离 + Vite 构建） | 1-3 天 | P0 |
| LLM 对话层 | 完全可行 | 极高（RAG + Chat UI 已完成 80%） | 3-5 天 | P0 |
| 语音合成层 | 可行 | 中（需新增音频流通道） | 3-5 天 | P2 |
| 养成系统 | 完全可行 | 高（Kafka 事件驱动零耦合） | 5-7 天 | P1 |

#### 推荐实施路线

```
第一阶段（1 周）: Live2D 看板娘 + LLM 基础对话
  ├─ 前端：analytics-stat 子应用内新增 Live2D 组件
  ├─ 后端：KnowledgeChatController 对接 DeepSeek/通义千问 API
  └─ 效果：点击看板娘弹出聊天框，支持带人设的 AI 对话

第二阶段（1 周）: 养成系统 + 业务事件联动
  ├─ 后端：新增 anime_pet 数据表 + Kafka 事件监听
  ├─ 前端：看板娘状态面板（等级、心情、好感度）
  └─ 效果：执行测试/提交 Bug 时看板娘自动响应

第三阶段（可选）: 语音合成 + 口型同步
  ├─ 后端：对接 TTS API，WebSocket 推送音频
  ├─ 前端：audio 播放 + Live2D lip-sync
  └─ 效果：看板娘”开口说话”
```

#### 最大优势

本项目最大的技术优势在于 **`analytics-stat` 模块已经铺好了所有基础设施**：Vue 3 + TypeScript 前端、Chat UI 组件、LLM API 调用链路、Elasticsearch 知识检索、Kafka 异步管道、micro-app 隔离机制。动漫主题方案不是从零开始，而是在一个已经成型的 AI 子系统上做增量开发，这大幅降低了技术风险和开发周期。
