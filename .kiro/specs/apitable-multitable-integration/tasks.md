# 任务清单：测试跟踪模块嵌入 APITable 多维表

## 1. 本地 APITable 容器 POC

### 1.1 Docker Compose
- [ ] 1.1.1 新增 `.kiro/specs/apitable-multitable-integration/docker-compose-apitable-poc.yml` POC Compose 文件
- [ ] 1.1.2 使用 `apitable/all-in-one` 镜像，仅用于 demo/testing
- [ ] 1.1.3 配置 `metersphere-dev` 外部网络
- [ ] 1.1.4 配置 `apitable_data` 持久化卷
- [ ] 1.1.5 配置端口映射 `8088:80`
- [ ] 1.1.6 在文档中标注 all-in-one 不进入正式企业/生产环境
- [ ] 1.1.7 在 Apple Silicon 环境记录 `linux/amd64` 模拟带来的性能风险

### 1.2 离线镜像流程
- [ ] 1.2.1 编写外网机器 `docker pull` / `docker save` 命令
- [ ] 1.2.2 编写内网机器 `docker load` / `docker images` 验证命令
- [ ] 1.2.3 固定镜像 tag 或 digest，避免正式环境使用不可追溯的 `latest`
- [ ] 1.2.4 整理镜像、Compose、`.env`、初始化模板的离线部署包目录结构

### 1.3 正式 Compose 离线包
- [ ] 1.3.1 外网下载官方 install.sh 引用的 Docker Compose 包
- [ ] 1.3.2 展开 Compose 包并记录所有镜像列表
- [ ] 1.3.3 逐个拉取并导出镜像
- [ ] 1.3.4 替换 Compose 中的镜像地址为内网镜像仓库地址
- [ ] 1.3.5 在内网测试环境验证无需公网即可启动

### 1.4 APITable 初始化
- [ ] 1.4.1 启动 APITable POC 容器并访问 `http://localhost:8088`
- [ ] 1.4.2 完成首次初始化或注册测试账号
- [ ] 1.4.3 创建 `MeterSphere 测试用例辅助编写` Space
- [ ] 1.4.4 创建 `TestCases` Datasheet 和基础视图
- [ ] 1.4.5 导出 CSV/Excel 模板或记录 APITable 导入步骤，供内网重复初始化

## 2. 测试跟踪前端嵌入

### 2.1 路由
- [ ] 2.1.1 修改 `test-track/frontend/src/router/modules/track.js`
- [ ] 2.1.2 新增 `/track/apitable` POC 路由
- [ ] 2.1.3 路由组件指向 `@/business/multitable/ApitableMultitable.vue`
- [ ] 2.1.4 正式阶段评估抽象 `/track/multitable` provider

### 2.2 菜单
- [ ] 2.2.1 修改 `test-track/frontend/src/business/head/TrackHeaderMenus.vue`
- [ ] 2.2.2 在测试用例菜单后新增“APITable 编写”
- [ ] 2.2.3 POC 阶段复用 `PROJECT_TRACK_CASE:READ` 权限
- [ ] 2.2.4 增加 `/track/apitable` 路由高亮逻辑

### 2.3 权限映射
- [ ] 2.3.1 修改 `framework/sdk-parent/frontend/src/utils/constants.js`
- [ ] 2.3.2 在 `SECOND_LEVEL_ROUTE_PERMISSION_MAP.TRACK` 增加 `/track/apitable`
- [ ] 2.3.3 正式阶段评估是否新增 `PROJECT_TRACK_MULTITABLE:READ` 权限

### 2.4 嵌入组件
- [ ] 2.4.1 创建 `test-track/frontend/src/business/multitable/ApitableMultitable.vue`
- [ ] 2.4.2 创建 `test-track/frontend/src/business/multitable/apitable-config.js`
- [ ] 2.4.3 支持 iframe 加载状态
- [ ] 2.4.4 支持配置缺失提示
- [ ] 2.4.5 支持加载失败提示和新窗口打开
- [ ] 2.4.6 处理页面高度，避免双重滚动
- [ ] 2.4.7 不隐藏或遮挡 APITable 开源版品牌标识

### 2.5 国际化
- [ ] 2.5.1 修改 `framework/sdk-parent/frontend/src/i18n/lang/track/zh-CN.js`
- [ ] 2.5.2 修改 `framework/sdk-parent/frontend/src/i18n/lang/track/zh-TW.js`
- [ ] 2.5.3 修改 `framework/sdk-parent/frontend/src/i18n/lang/track/en-US.js`
- [ ] 2.5.4 增加“APITable 编写”“未配置 APITable 地址”“打开新窗口”等词条

## 3. 配置能力

### 3.1 POC 前端配置
- [ ] 3.1.1 增加 `VUE_APP_APITABLE_BASE_URL`
- [ ] 3.1.2 增加 `VUE_APP_APITABLE_DEFAULT_PATH`
- [ ] 3.1.3 在本地 `.env` 或启动命令中配置 APITable 地址
- [ ] 3.1.4 确保未配置时前端不报错

### 3.2 后端配置接口（正式阶段）
- [ ] 3.2.1 在 `test-track/backend` 增加多维表 provider 配置属性
- [ ] 3.2.2 支持从 `/opt/metersphere/conf/metersphere.properties` 读取配置
- [ ] 3.2.3 提供 `GET /track/multitable/config?provider=apitable`
- [ ] 3.2.4 支持按项目返回不同 APITable Datasheet 或 Share URL
- [ ] 3.2.5 增加配置白名单校验，防止 iframe 加载任意外部地址

## 4. 同步桥接能力（后续阶段）

### 4.1 APITable API Client
- [ ] 4.1.1 创建 APITable API Client 封装
- [ ] 4.1.2 支持读取指定 Space / Datasheet / View 的记录
- [ ] 4.1.3 支持回写同步状态字段
- [ ] 4.1.4 API Token 仅保存在后端配置或密钥管理中

### 4.2 字段映射
- [ ] 4.2.1 定义 APITable 字段到 MeterSphere 测试用例字段的映射
- [ ] 4.2.2 实现必填字段校验
- [ ] 4.2.3 实现优先级、类型、状态等枚举转换
- [ ] 4.2.4 实现步骤/预期结果结构转换

### 4.3 手动同步
- [ ] 4.3.1 提供 `POST /track/multitable/sync/{projectId}`
- [ ] 4.3.2 校验当前用户项目权限
- [ ] 4.3.3 调用 MeterSphere 用例服务创建/更新用例
- [ ] 4.3.4 记录同步结果和错误信息
- [ ] 4.3.5 使用 APITable record id + projectId + ms_case_id 做幂等

### 4.4 自动同步
- [ ] 4.4.1 评估 APITable Webhook 在内网环境的可用性
- [ ] 4.4.2 支持“评审通过”记录触发同步
- [ ] 4.4.3 增加失败重试和死信记录
- [ ] 4.4.4 增加同步开关，默认关闭自动同步

## 5. 安全与合规

### 5.1 开源合规材料
- [ ] 5.1.1 记录 APITable Open Source Edition AGPL-3.0 许可证
- [ ] 5.1.2 记录 “Powered by APITable” 标识保留要求
- [ ] 5.1.3 输出镜像来源、版本和 digest
- [ ] 5.1.4 输出第三方依赖清单或 SBOM（正式阶段）
- [ ] 5.1.5 如修改 APITable 源码，提交法务评估 AGPL 源码开放义务

### 5.2 网络隔离验证
- [ ] 5.2.1 检查 APITable 容器启动后无必须公网依赖
- [ ] 5.2.2 禁用或内网化模板、帮助、统计、更新检查入口
- [ ] 5.2.3 禁用或内网化 Widget / Automation 外部依赖
- [ ] 5.2.4 禁用外部 AI 服务配置

### 5.3 访问控制
- [ ] 5.3.1 POC 阶段记录本地账号使用范围
- [ ] 5.3.2 正式阶段接入瑞众统一身份源或可信代理
- [ ] 5.3.3 按项目拆分 APITable Space / Datasheet 权限
- [ ] 5.3.4 对同步 API 做 MeterSphere 项目权限校验

## 6. 测试验证

### 6.1 容器测试
- [ ] 6.1.1 `docker compose -f docker-compose-dev.yml up -d`
- [ ] 6.1.2 `docker compose -f .kiro/specs/apitable-multitable-integration/docker-compose-apitable-poc.yml up -d`
- [ ] 6.1.3 验证 `http://localhost:8088` 可访问
- [ ] 6.1.4 重启容器后验证 APITable 数据不丢失
- [ ] 6.1.5 记录 CPU、内存占用，判断本机是否适合继续跑 POC

### 6.2 前端测试
- [ ] 6.2.1 启动 `test-track/frontend`
- [ ] 6.2.2 访问 `/track/apitable`
- [ ] 6.2.3 验证顶部菜单展示和高亮
- [ ] 6.2.4 验证 iframe 正常加载 APITable
- [ ] 6.2.5 验证未配置 APITable 地址时的空状态
- [ ] 6.2.6 验证无权限用户不展示入口

### 6.3 回归测试
- [ ] 6.3.1 测试原测试用例列表 `/track/case/all`
- [ ] 6.3.2 测试用例评审 `/track/review/all`
- [ ] 6.3.3 测试测试计划 `/track/plan/all`
- [ ] 6.3.4 测试缺陷管理 `/track/issue`
- [ ] 6.3.5 确认新增页面不影响已有路由和菜单

