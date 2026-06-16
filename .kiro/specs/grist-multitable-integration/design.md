# 设计文档：测试跟踪模块嵌入 Grist Core 多维表

## 概述

本方案在 MeterSphere V2.10 测试跟踪模块中新增“多维表编写”入口，嵌入内网部署的 Grist Core 页面，提供表格化、结构化、多人协作的测试用例辅助编写能力。

设计分三期：

1. **一期：本地/内网 POC**  
   使用 Docker 启动 `gristlabs/grist-oss`，MeterSphere 测试跟踪模块通过 iframe 嵌入 Grist 页面，不做自动同步。
2. **二期：配置化嵌入**  
   Grist 地址、默认文档、项目映射从配置读取，补齐异常状态和权限入口。
3. **三期：数据同步闭环**  
   通过 Grist REST API / Webhook 与 MeterSphere 测试用例 API 打通，实现手动或自动同步。

## 核心设计目标

1. **离线可部署**：适配瑞众保险内网无外网环境。
2. **低侵入**：不改 MeterSphere 原有测试用例主流程，先新增辅助入口。
3. **合规友好**：优先使用 `grist-oss` 纯开源镜像，Apache 2.0 许可证。
4. **配置化**：Grist URL 不硬编码，支持本地、测试内网、生产内网差异。
5. **渐进同步**：先嵌入，后同步，避免一开始引入双写风险。

## 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 多维表 | Grist Core / grist-oss | Apache 2.0，自托管，REST API/Webhook |
| 部署 | Docker Compose | 与 MeterSphere 本地中间件部署方式一致 |
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
│       └── /track/multitable                                     │
│              └── GristMultitable.vue                            │
│                     └── iframe                                  │
└───────────────────────────────┬─────────────────────────────────┘
                                │ http://grist:8484 / 内网域名
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Grist Core / grist-oss 容器                   │
│                                                                 │
│  /persist/docs        Grist 文档数据                             │
│  REST API             后续同步读取数据                           │
│  Webhook              后续变更通知                               │
└───────────────────────────────┬─────────────────────────────────┘
                                │ 后续可选
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                  test-track/backend 同步桥接能力                 │
│                                                                 │
│  Grist API Client -> TestCaseService -> MeterSphere MySQL        │
└─────────────────────────────────────────────────────────────────┘
```

## Docker 部署设计

### POC 推荐方式

先新增独立 Compose 文件，例如 `.kiro/specs/grist-multitable-integration/docker-compose-grist.yml`，后续也可以移动到项目根目录：

```yaml
services:
  grist:
    image: gristlabs/grist-oss:latest
    container_name: metersphere-grist
    ports:
      - "8484:8484"
    environment:
      PORT: 8484
      GRIST_DATA_DIR: /persist/docs
      GRIST_IN_SERVICE: "true"
      GRIST_DEFAULT_EMAIL: admin@metersphere.local
      GRIST_ALLOW_AUTOMATIC_VERSION_CHECKING: "false"
      GRIST_TELEMETRY_LEVEL: "off"
      GRIST_HIDE_UI_ELEMENTS: "helpCenter,billing,templates,sendToDrive,tutorials,supportGrist,importFromAirtable,automations"
      GRIST_WIDGET_LIST_URL: ""
    volumes:
      - grist_data:/persist
    restart: unless-stopped
    networks:
      - metersphere-dev

volumes:
  grist_data:
    driver: local

networks:
  metersphere-dev:
    external: true
```

启动顺序：

```bash
docker-compose -f docker-compose-dev.yml up -d
docker-compose -f .kiro/specs/grist-multitable-integration/docker-compose-grist.yml up -d
```

### 内网离线镜像导入

外网机器：

```bash
docker pull gristlabs/grist-oss:latest
docker save gristlabs/grist-oss:latest -o grist-oss-latest.tar
```

内网机器：

```bash
docker load -i grist-oss-latest.tar
docker images | grep grist-oss
```

正式内网建议固定镜像 tag 或 digest，避免 `latest` 不可追溯。

## 前端设计

### 路由设计

修改 `test-track/frontend/src/router/modules/track.js`：

```javascript
{
  path: "multitable",
  name: "testCaseMultitable",
  component: () => import("@/business/multitable/GristMultitable.vue")
}
```

### 菜单设计

修改 `test-track/frontend/src/business/head/TrackHeaderMenus.vue`，在测试用例菜单后增加：

```javascript
{
  path: "/track/multitable",
  name: this.$t("test_track.multitable.case_authoring"),
  permission: "PROJECT_TRACK_CASE:READ"
}
```

路由高亮逻辑增加：

```javascript
} else if (to.path.indexOf("/track/multitable") >= 0) {
  this.pathName = "/track/multitable";
}
```

### 二级权限映射

修改 `framework/sdk-parent/frontend/src/utils/constants.js`：

```javascript
{router: "/track/multitable", permission: ["PROJECT_TRACK_CASE:READ"]},
```

POC 阶段复用测试用例读取权限，正式阶段可新增更细权限，例如 `PROJECT_TRACK_MULTITABLE:READ`。

### iframe 组件设计

建议新增：

```
test-track/frontend/src/business/multitable/
├── GristMultitable.vue
└── grist-config.js
```

组件职责：

1. 读取 Grist 嵌入 URL。
2. 拼接当前项目 ID、工作空间 ID 等上下文参数。
3. 渲染 iframe。
4. 展示 loading、error、empty config 状态。
5. 提供“新窗口打开”按钮便于排查 iframe 限制。

示意结构：

```vue
<template>
  <div class="grist-page">
    <div v-if="!gristUrl" class="grist-empty">
      未配置 Grist 多维表地址
    </div>
    <iframe
      v-else
      class="grist-frame"
      :src="gristUrl"
      title="Grist Multitable"
      @load="onLoad"
    />
  </div>
</template>
```

样式要点：

```scss
.grist-page {
  height: calc(100vh - 120px);
  background: #fff;
}

.grist-frame {
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
VUE_APP_GRIST_BASE_URL=http://localhost:8484
VUE_APP_GRIST_DEFAULT_PATH=/
```

生成 URL：

```javascript
const base = process.env.VUE_APP_GRIST_BASE_URL || "";
const path = process.env.VUE_APP_GRIST_DEFAULT_PATH || "/";
export const getGristEmbedUrl = () => `${base}${path}`;
```

### 正式配置

正式阶段建议由后端提供配置接口，避免构建包绑定环境：

```
GET /track/multitable/config?projectId={projectId}
```

响应示例：

```json
{
  "enabled": true,
  "baseUrl": "https://grist-test.example.internal",
  "embedUrl": "https://grist-test.example.internal/o/docs/docId",
  "openInNewWindow": true,
  "projectMappingEnabled": true
}
```

## 认证设计

### POC 阶段

可选方案：

1. `GRIST_DEFAULT_EMAIL=admin@metersphere.local`，单用户快速验证。
2. Grist 本地用户登录，iframe 中保留 Grist 会话。

### 正式阶段

优先级：

1. OIDC/SAML 接入瑞众统一身份源。
2. 可信反向代理 Forward Auth，代理注入用户邮箱。
3. Grist 本地账号仅用于隔离 POC，不进入正式环境。

安全要求：

1. 不在 URL 中传递长期 Token。
2. 反向代理必须清理外部传入的身份头，再由代理自身注入。
3. MeterSphere 入口权限与 Grist 文档权限都要生效。

## Grist 用例模板设计

建议创建文档：`MeterSphere 测试用例辅助编写`

### 表：TestCases

| 字段 | 类型 | 说明 |
|------|------|------|
| case_name | Text | 用例名称 |
| module | Choice/Text | 所属模块 |
| priority | Choice | P0/P1/P2/P3 |
| case_type | Choice | 功能/接口/UI/性能 |
| prerequisite | Text | 前置条件 |
| steps | Text | 测试步骤 |
| expected_result | Text | 预期结果 |
| owner | Text/User | 负责人 |
| status | Choice | 草稿/待评审/评审通过/已同步 |
| requirement_id | Text | 关联需求 ID |
| tags | ChoiceList | 标签 |
| ms_case_id | Text | MeterSphere 用例 ID |
| sync_status | Choice | 未同步/同步成功/同步失败 |
| sync_time | DateTime | 同步时间 |
| sync_error | Text | 同步错误 |

### 视图

| 视图 | 用途 |
|------|------|
| 全量用例 | 批量录入和维护 |
| 待评审 | 过滤 `status = 待评审` |
| 评审通过待同步 | 过滤 `status = 评审通过` 且 `sync_status != 同步成功` |
| 按负责人 | 按 owner 分组 |
| P0/P1 高优先级 | 聚焦核心用例 |

## 同步设计

POC 不做同步。后续同步桥接可放在 `test-track/backend`：

```
Grist REST API
     │
     ▼
GristCaseImportService
     │
     ├── 字段映射
     ├── 数据校验
     ├── 幂等判断 ms_case_id / source_record_id
     └── 调用 TestCaseService 创建/更新用例
```

### 手动同步接口建议

```
POST /track/multitable/sync/{projectId}
```

请求：

```json
{
  "docId": "grist-doc-id",
  "tableId": "TestCases",
  "view": "ReadyToSync",
  "recordIds": [1, 2, 3]
}
```

响应：

```json
{
  "total": 3,
  "success": 2,
  "failed": 1,
  "errors": [
    {"recordId": 3, "message": "用例名称为空"}
  ]
}
```

## 安全设计

1. Grist 容器仅暴露到测试内网或本机，不直接暴露公网。
2. POC 阶段不导入生产敏感数据。
3. iframe 仅加载白名单 Grist 域名。
4. 后续同步 API 必须校验 MeterSphere 项目权限。
5. 同步日志不记录敏感业务数据全文，仅记录记录 ID 和错误摘要。

## 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| iframe 被 Grist 响应头阻止 | 页面无法嵌入 | 先用同域反向代理或确认 Grist 配置；保留新窗口打开 |
| 内网无法拉取镜像 | 无法部署 | 外网导出 `docker save`，内网 `docker load` |
| Grist 与 MeterSphere 权限不一致 | 数据越权 | 正式阶段接统一身份源，项目级文档隔离 |
| 辅助表和正式用例数据不一致 | 执行数据混乱 | MeterSphere 始终作为主数据，Grist 只做草稿区 |
| 同步重复创建用例 | 数据污染 | 使用 `ms_case_id`、Grist record id 和项目 ID 做幂等键 |
