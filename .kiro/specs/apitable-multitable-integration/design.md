# 设计文档：测试跟踪模块嵌入 APITable 多维表

## 概述

本方案在 MeterSphere V2.10 测试跟踪模块中新增 APITable POC 入口，嵌入内网部署的 APITable 页面，验证其作为测试用例辅助编写多维表的可行性。

设计分三期：

1. **一期：本地 POC**  
   使用 `apitable/all-in-one` 镜像启动 APITable demo/test 实例，MeterSphere 测试跟踪模块通过 iframe 嵌入，不做自动同步。
2. **二期：内网试点**  
   使用官方 Docker Compose 离线部署包和内网镜像仓库部署 APITable，补齐配置化、权限、备份和资源监控。
3. **三期：数据同步闭环**  
   通过 APITable REST API / Webhook / 定时任务与 MeterSphere 测试用例 API 打通，实现手动或自动同步。

## 核心设计目标

1. **验证产品形态**：确认 APITable 的多视图、权限、API、嵌入能力是否满足用例编写场景。
2. **离线可评估**：适配瑞众保险内网无外网环境，明确镜像和 Compose 离线导入路径。
3. **低侵入**：不改 MeterSphere 原有测试用例主流程，先新增 APITable POC 入口。
4. **合规可控**：明确 AGPL-3.0 和嵌入许可风险，不移除开源版品牌标识。
5. **配置化**：APITable URL 不硬编码，支持本地、测试内网、生产内网差异。
6. **渐进同步**：先嵌入，后同步，避免一开始引入双写风险。

## 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 多维表 | APITable Open Source Edition | AGPL-3.0，自托管，REST API，Share/Embed |
| POC 部署 | `apitable/all-in-one` | 官方说明仅用于 demo/testing，不用于企业/生产 |
| 正式部署 | 官方 Docker Compose 包 | 需离线化镜像和 Compose，固定版本 |
| 前端 | Vue 2 + Element UI | 测试跟踪子应用现有技术栈 |
| 嵌入方式 | iframe | POC 阶段最低成本、低侵入 |
| 配置 | 环境变量 / 后端配置接口 | 避免前端硬编码内网地址 |
| 同步 | REST API + 定时任务/Webhook | 二期后再实现 |

## 总体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    MeterSphere Gateway / 主应用                  │
└───────────────────────────────┬─────────────────────────────────┘
                                │ micro-app / route
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                 test-track/frontend 测试跟踪子应用               │
│                                                                 │
│  TrackHeaderMenus.vue                                           │
│       └── /track/apitable                                       │
│              └── ApitableMultitable.vue                         │
│                     └── iframe                                  │
└───────────────────────────────┬─────────────────────────────────┘
                                │ http://apitable:80 / 内网域名
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    APITable 容器 / 正式内网集群                  │
│                                                                 │
│  Space / Datasheet       测试用例辅助编写区                      │
│  Share / Embed           前端嵌入入口                            │
│  REST API                后续同步读取数据                        │
│  Webhook / Automation    后续变更通知                            │
└───────────────────────────────┬─────────────────────────────────┘
                                │ 后续可选
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                  test-track/backend 同步桥接能力                 │
│                                                                 │
│  APITable API Client -> TestCaseService -> MeterSphere MySQL     │
└─────────────────────────────────────────────────────────────────┘
```

## 部署设计

### 本地 POC 方式

本地 POC 使用 all-in-one 镜像，目的是验证嵌入、模板和 API 可用性。该镜像不建议进入企业或生产环境。

当前本机的 MeterSphere 中间件由 `/opt/metersphere/docker-compose-*.yml` 启动，Docker 网络名为 `metersphere_ms-network`；仓库中的 `docker-compose-dev.yml` 使用的是 `metersphere-dev` 网络。POC Compose 默认适配当前 `/opt/metersphere` 部署，可通过 `APITABLE_DOCKER_NETWORK` 切换到其他网络。

文件：`.kiro/specs/apitable-multitable-integration/docker-compose-apitable-poc.yml`

```yaml
services:
  apitable:
    image: ${APITABLE_IMAGE:-apitable/all-in-one:latest}
    container_name: metersphere-apitable
    platform: ${APITABLE_PLATFORM:-linux/amd64}
    ports:
      - "${APITABLE_HOST_PORT:-8088}:80"
    volumes:
      - ./apitable-all-in-one-entrypoint.sh:/opt/metersphere-apitable-entrypoint.sh:ro
      - apitable_data:/apitable
    command: ["bash", "/opt/metersphere-apitable-entrypoint.sh"]
    restart: unless-stopped
    networks:
      - metersphere

volumes:
  apitable_data:
    driver: local

networks:
  metersphere:
    external: true
    name: ${APITABLE_DOCKER_NETWORK:-metersphere_ms-network}
```

启动顺序：

```bash
docker compose -f .kiro/specs/apitable-multitable-integration/docker-compose-apitable-poc.yml up -d
```

`apitable-all-in-one-entrypoint.sh` 只用于 POC 镜像启动前修复 `apitable/all-in-one:latest` 中 `init-appdata.sh` 的 Liquibase 参数引号问题。未修复时，首次初始化可能把 `-DDB_ENGINE=mysql` 拼进 `${table.prefix}`，生成类似 `apitable_     -DDB_ENGINE=mysqluser` 的错误表名，并导致 backend-server 持续重启。

如果使用仓库里的 `docker-compose-dev.yml` 而不是 `/opt/metersphere` 中间件栈：

```bash
docker compose -f docker-compose-dev.yml up -d
APITABLE_DOCKER_NETWORK=metersphere-dev docker compose -f .kiro/specs/apitable-multitable-integration/docker-compose-apitable-poc.yml up -d
```

访问地址：

```text
http://localhost:8088/
```

### 正式内网部署方式

APITable 官方 README 提供一键安装命令：

```bash
curl https://apitable.github.io/install.sh | bash
```

该命令会访问外网下载 Docker Compose 包并拉取镜像，因此不适合瑞众内网直接执行。正式内网试点应采用以下流程：

1. 外网环境下载 APITable 官方 Compose 包。
2. 外网环境拉取 Compose 中引用的所有镜像。
3. 使用 `docker save` 导出镜像，或推送到内网镜像仓库。
4. 将 Compose、`.env`、镜像 tar、初始化模板一起打成离线部署包。
5. 内网环境使用 `docker load` 或内网镜像仓库部署。
6. 固定镜像 tag 或 digest，不直接使用 `latest`。

示例：

```bash
docker pull apitable/all-in-one:latest
docker save apitable/all-in-one:latest -o apitable-all-in-one-latest.tar
```

内网导入：

```bash
docker load -i apitable-all-in-one-latest.tar
docker images | grep apitable
```

## 前端设计

### 路由设计

POC 路由建议使用 `/track/apitable`，避免与 Grist 方案的 `/track/multitable` 冲突，便于并行比较。

修改 `test-track/frontend/src/router/modules/track.js`：

```javascript
{
  path: "apitable",
  name: "testCaseApitable",
  component: () => import("@/business/multitable/ApitableMultitable.vue")
}
```

正式阶段建议抽象为统一多维表 provider：

```javascript
{
  path: "multitable",
  name: "testCaseMultitable",
  component: () => import("@/business/multitable/MultitableProvider.vue")
}
```

### 菜单设计

修改 `test-track/frontend/src/business/head/TrackHeaderMenus.vue`，在测试用例菜单后增加：

```javascript
{
  path: "/track/apitable",
  name: this.$t("test_track.multitable.apitable_authoring"),
  permission: "PROJECT_TRACK_CASE:READ"
}
```

路由高亮逻辑增加：

```javascript
} else if (to.path.indexOf("/track/apitable") >= 0) {
  this.pathName = "/track/apitable";
}
```

### 二级权限映射

修改 `framework/sdk-parent/frontend/src/utils/constants.js`：

```javascript
{router: "/track/apitable", permission: ["PROJECT_TRACK_CASE:READ"]},
```

POC 阶段复用测试用例读取权限。正式阶段可以新增更细权限，例如 `PROJECT_TRACK_MULTITABLE:READ`。

### iframe 组件设计

建议新增：

```
test-track/frontend/src/business/multitable/
├── ApitableMultitable.vue
└── apitable-config.js
```

组件职责：

1. 读取 APITable 嵌入 URL。
2. 拼接当前项目 ID、工作空间 ID 等上下文参数。
3. 渲染 iframe。
4. 展示 loading、error、empty config 状态。
5. 提供“新窗口打开”按钮便于排查 iframe 限制。
6. 不主动隐藏或遮挡 APITable 开源版品牌标识。

示意结构：

```vue
<template>
  <div class="apitable-page">
    <div v-if="!apitableUrl" class="apitable-empty">
      未配置 APITable 多维表地址
    </div>
    <iframe
      v-else
      class="apitable-frame"
      :src="apitableUrl"
      title="APITable Multitable"
      @load="onLoad"
    />
  </div>
</template>
```

样式要点：

```scss
.apitable-page {
  height: calc(100vh - 120px);
  background: #fff;
}

.apitable-frame {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}
```

## 配置设计

### POC 前端配置

短期可使用前端环境变量：

```bash
VUE_APP_APITABLE_BASE_URL=http://localhost:8088
VUE_APP_APITABLE_DEFAULT_PATH=/
```

生成 URL：

```javascript
const base = process.env.VUE_APP_APITABLE_BASE_URL || "";
const path = process.env.VUE_APP_APITABLE_DEFAULT_PATH || "/";
export const getApitableEmbedUrl = () => `${base}${path}`;
```

### 正式配置

正式阶段建议由后端提供配置接口，避免构建包绑定环境：

```text
GET /track/multitable/config?projectId={projectId}&provider=apitable
```

响应示例：

```json
{
  "enabled": true,
  "provider": "apitable",
  "baseUrl": "https://apitable-test.example.internal",
  "embedUrl": "https://apitable-test.example.internal/share/shrxxxx",
  "openInNewWindow": true,
  "projectMappingEnabled": true
}
```

## 认证设计

### POC 阶段

可选方案：

1. APITable 本地用户登录，iframe 中保留 APITable 会话。
2. 创建独立测试 Space，仅放脱敏样例数据。
3. POC 只验证嵌入和模板，不接生产身份。

### 正式阶段

优先级：

1. 评估 APITable 开源版与瑞众统一身份源集成能力。
2. 如开源版身份集成能力不足，评估可信反向代理或企业版能力。
3. APITable 本地账号仅用于隔离 POC，不进入正式环境。

安全要求：

1. 不在 URL 中传递长期 Token。
2. 反向代理必须清理外部传入的身份头，再由代理自身注入。
3. MeterSphere 入口权限与 APITable Space / Datasheet 权限都要生效。

## APITable 用例模板设计

建议创建 Space：`MeterSphere 测试用例辅助编写`

建议创建 Datasheet：`TestCases`

### 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| case_name | Text | 用例名称 |
| module | SingleSelect/Text | 所属模块 |
| priority | SingleSelect | P0/P1/P2/P3 |
| case_type | SingleSelect | 功能/接口/UI/性能 |
| prerequisite | LongText | 前置条件 |
| steps | LongText | 测试步骤 |
| expected_result | LongText | 预期结果 |
| owner | Member/Text | 负责人 |
| status | SingleSelect | 草稿/待评审/评审通过/已同步 |
| requirement_id | Text | 关联需求 ID |
| tags | MultiSelect | 标签 |
| ms_case_id | Text | MeterSphere 用例 ID |
| source_record_id | Text | APITable 记录 ID |
| sync_status | SingleSelect | 未同步/同步成功/同步失败 |
| sync_time | DateTime | 同步时间 |
| sync_error | LongText | 同步错误 |

### 视图

| 视图 | 用途 |
|------|------|
| 全量用例 | 批量录入和维护 |
| 待评审 | 过滤 `status = 待评审` |
| 评审通过待同步 | 过滤 `status = 评审通过` 且 `sync_status != 同步成功` |
| 按负责人看板 | 按 owner 或 status 看板展示 |
| P0/P1 高优先级 | 聚焦核心用例 |

## 同步设计

POC 不做同步。后续同步桥接可放在 `test-track/backend`：

```text
APITable REST API
     │
     ▼
ApitableCaseImportService
     │
     ├── 字段映射
     ├── 数据校验
     ├── 幂等判断 source_record_id / ms_case_id
     └── 调用 TestCaseService 创建/更新用例
```

### 手动同步接口建议

```text
POST /track/multitable/sync/{projectId}
```

请求：

```json
{
  "provider": "apitable",
  "spaceId": "spcxxxx",
  "datasheetId": "dstxxxx",
  "viewId": "viwxxxx",
  "recordIds": ["recxxxx1", "recxxxx2"]
}
```

响应：

```json
{
  "total": 2,
  "success": 1,
  "failed": 1,
  "errors": [
    {"recordId": "recxxxx2", "message": "用例名称为空"}
  ]
}
```

## 安全与合规设计

1. APITable 容器仅暴露到测试内网或本机，不直接暴露公网。
2. POC 阶段不导入生产敏感数据。
3. iframe 仅加载白名单 APITable 域名。
4. 后续同步 API 必须校验 MeterSphere 项目权限。
5. 同步日志不记录敏感业务数据全文，仅记录记录 ID 和错误摘要。
6. 保留开源版 “Powered by APITable” 标识，除非完成商业许可或法务确认。
7. 如果修改 APITable 开源版代码并提供网络服务，需要按 AGPL-3.0 评估源码开放义务。

## 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| APITable 资源占用高 | 本机或内网试点机器变卡 | POC 单独启停；正式部署按 4C/8G 起步并监控 |
| all-in-one 不适合生产 | 正式部署不稳定 | POC 只用 all-in-one；正式用官方 Compose 离线包 |
| all-in-one 初始化脚本兼容问题 | Liquibase 生成错误表名前缀，backend-server 重启 | POC Compose 启动前修复脚本；正式部署改用官方 Compose 包并固定版本 |
| arm64/Apple Silicon 性能差 | 本机 POC 慢 | 优先 x86_64 机器验证；Apple Silicon 只做轻量体验 |
| AGPL / 嵌入许可风险 | 合规无法通过 | 保留品牌标识；法务评审；必要时采购企业许可 |
| iframe 被响应头阻止 | 页面无法嵌入 | 先用 Share/Embed 链接；必要时同域反向代理 |
| 内网无法拉取镜像 | 无法部署 | 外网导出镜像和 Compose 包，内网 `docker load` |
| APITable 与 MeterSphere 权限不一致 | 数据越权 | 正式阶段接统一身份源，按项目隔离 Space/Datasheet |
| 辅助表和正式用例数据不一致 | 执行数据混乱 | MeterSphere 始终作为主数据，APITable 只做草稿区 |

## 与 Grist 方案对比

| 维度 | APITable | Grist |
|------|----------|-------|
| 产品形态 | 更接近 Airtable，视图和团队协作更强 | 更轻量，表格数据库能力清晰 |
| 许可证 | AGPL-3.0，嵌入许可需关注 | Apache 2.0，相对宽松 |
| 部署复杂度 | 较高，推荐 4C/8G | 较低 |
| 本机性能 | 可能偏重，arm64 风险明显 | 相对轻 |
| 内网试点成本 | 中高 | 低中 |
| 作为主推方案 | 备选 | 推荐首选 |

## 参考资料

1. APITable GitHub: https://github.com/apitable/apitable
2. APITable README Installation: https://github.com/apitable/apitable#installation
3. APITable Licensing: https://github.com/apitable/apitable/blob/develop/LICENSING.md
4. APITable Developer Center: https://developers.aitable.ai/
