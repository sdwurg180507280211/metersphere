# Grist 多维表嵌入测试跟踪 - 前端测试指南

## 测试环境

### MeterSphere 中间件容器

- **Compose 文件**: `docker-compose-dev.yml`
- **网络**: `metersphere-dev`
- **启动命令**:
  ```bash
  docker-compose -f docker-compose-dev.yml up -d
  ```

### Grist 容器

- **推荐镜像**: `gristlabs/grist-oss`
- **本地地址**: http://localhost:8484/
- **容器名**: `metersphere-grist`
- **启动命令**:
  ```bash
  docker-compose -f .kiro/specs/grist-multitable-integration/docker-compose-grist.yml up -d
  ```

### 测试跟踪前端

- **模块目录**: `test-track/frontend`
- **开发地址**: http://localhost:4005/
- **启动命令**:
  ```bash
  cd test-track/frontend
  npm run track
  ```

> 注：端口以当前项目脚本实际输出为准。如本地端口被占用，按终端提示调整访问地址。

---

## 1. Grist 容器测试

### 1.1 启动 Grist

```bash
docker-compose -f .kiro/specs/grist-multitable-integration/docker-compose-grist.yml up -d
docker ps | grep metersphere-grist
```

**预期结果**:

- `metersphere-grist` 状态为 `Up`
- 本机可访问 http://localhost:8484/
- 页面进入 Grist 初始化页或默认身份下的首页

### 1.2 验证持久化

1. 在 Grist 中创建一个测试文档。
2. 重启容器：
   ```bash
   docker restart metersphere-grist
   ```
3. 再次访问 http://localhost:8484/

**预期结果**:

- 测试文档仍然存在。
- 容器重启不丢失数据。

### 1.3 验证无外联配置

检查容器环境变量：

```bash
docker inspect metersphere-grist --format '{{json .Config.Env}}'
```

**预期结果**:

- `GRIST_ALLOW_AUTOMATIC_VERSION_CHECKING=false`
- `GRIST_TELEMETRY_LEVEL=off`
- `GRIST_WIDGET_LIST_URL=` 为空
- `GRIST_HIDE_UI_ELEMENTS` 包含外部帮助、模板、计费、Google Drive 等入口

---

## 2. MeterSphere 前端路由测试

### 2.1 访问多维表页面

- **URL**: `http://localhost:4005/#/track/multitable`

**预期结果**:

- 页面正常加载。
- 顶部测试跟踪菜单展示“多维表编写”。
- 当前菜单高亮为“多维表编写”。
- 页面主体显示 Grist iframe。

### 2.2 检查路由配置

- **文件**: `test-track/frontend/src/router/modules/track.js`
- **路由路径**: `/track/multitable`
- **组件**: `GristMultitable.vue`

**预期结果**:

- 路由已配置。
- 路由组件被 `TestTrack.vue` 的内部 `router-view` 渲染。

### 2.3 检查菜单权限

- **文件**: `test-track/frontend/src/business/head/TrackHeaderMenus.vue`
- **权限**: `PROJECT_TRACK_CASE:READ`

**预期结果**:

- 有测试用例读取权限的用户能看到“多维表编写”。
- 无该权限的用户不展示菜单项。

---

## 3. iframe 嵌入测试

### 3.1 正常加载

1. 配置 Grist 地址：
   ```bash
   VUE_APP_GRIST_BASE_URL=http://localhost:8484
   VUE_APP_GRIST_DEFAULT_PATH=/
   ```
2. 启动 `test-track/frontend`。
3. 访问 `/track/multitable`。

**预期结果**:

- iframe 中展示 Grist 页面。
- iframe 宽度占满内容区。
- 页面无明显双重滚动。
- 浏览器控制台无跨域或 frame 拦截错误。

### 3.2 配置缺失

1. 移除 `VUE_APP_GRIST_BASE_URL`。
2. 重启前端。
3. 访问 `/track/multitable`。

**预期结果**:

- 页面展示“未配置 Grist 多维表地址”。
- 不出现空白页。
- 控制台无未捕获异常。

### 3.3 Grist 服务未启动

1. 停止 Grist：
   ```bash
   docker stop metersphere-grist
   ```
2. 访问 `/track/multitable`。

**预期结果**:

- 页面出现加载失败或浏览器连接失败提示。
- “新窗口打开”按钮可用于排查地址。
- MeterSphere 其他页面不受影响。

---

## 4. Grist 用例模板测试

### 4.1 创建模板表

在 Grist 中创建文档 `MeterSphere 测试用例辅助编写`，新增表 `TestCases`。

字段建议：

| 字段 | 类型 |
|------|------|
| case_name | Text |
| module | Text/Choice |
| priority | Choice |
| case_type | Choice |
| prerequisite | Text |
| steps | Text |
| expected_result | Text |
| owner | Text |
| status | Choice |
| requirement_id | Text |
| tags | ChoiceList |
| ms_case_id | Text |
| sync_status | Choice |
| sync_time | DateTime |
| sync_error | Text |

**预期结果**:

- 可以新增、编辑、过滤测试用例数据。
- 可以创建“待评审”“评审通过待同步”等视图。
- 记录刷新后仍保留。

### 4.2 导出模板

**预期结果**:

- 能导出 `.grist` 文件或通过 Grist 自身导出能力备份。
- 文件可作为内网离线模板导入。

---

## 5. 回归测试

### 5.1 原测试跟踪页面

逐个访问：

- `/track/home`
- `/track/case/all`
- `/track/review/all`
- `/track/plan/all`
- `/track/issue`
- `/track/testPlan/reportList`

**预期结果**:

- 原页面均可正常访问。
- 顶部菜单高亮逻辑不被 `/track/multitable` 影响。
- 用例列表、评审、计划、缺陷功能无新增报错。

### 5.2 微前端加载

从主应用进入测试跟踪模块。

**预期结果**:

- 主应用能正常加载 `test-track` 子应用。
- 点击“多维表编写”后仍在 MeterSphere 页面内部。
- 浏览器地址栏 hash 正确变更为 `#/track/multitable`。

---

## 6. 瑞众内网检查项

### 6.1 镜像来源

**检查项**:

- 镜像通过外网机器提前下载。
- 内网通过 `docker load` 导入。
- 记录镜像 tag 和 digest。

### 6.2 外联依赖

**检查项**:

- 容器启动时不需要访问公网。
- 未配置 OpenAI、OpenRouter、Google Drive 等外部服务。
- Widget 清单为空或指向内网地址。
- 模板通过离线 `.grist` 文件导入。

### 6.3 数据边界

**检查项**:

- POC 阶段仅使用脱敏或测试用例样例数据。
- 正式用例主数据仍在 MeterSphere。
- Grist 数据卷位于内网服务器。

---

## 常见问题排查

### 问题 1：Grist 页面打不开

**检查**:

```bash
docker ps | grep metersphere-grist
docker logs --tail=100 metersphere-grist
curl -I http://localhost:8484
```

**处理**:

- 确认端口 8484 未被占用。
- 确认容器已启动。
- 确认防火墙或代理未拦截本机访问。

### 问题 2：iframe 空白

**检查**:

- 浏览器控制台是否有 `X-Frame-Options` 或 `Content-Security-Policy` 错误。
- `VUE_APP_GRIST_BASE_URL` 是否配置正确。
- iframe `src` 是否能在新窗口直接打开。

**处理**:

- POC 阶段先使用同机 `http://localhost:8484`。
- 正式阶段可通过同域反向代理减少 iframe 安全策略冲突。

### 问题 3：菜单不展示

**检查**:

- 当前用户是否具备 `PROJECT_TRACK_CASE:READ`。
- `TrackHeaderMenus.vue` 是否增加菜单项。
- `SECOND_LEVEL_ROUTE_PERMISSION_MAP.TRACK` 是否增加路由映射。

### 问题 4：Grist 数据重启后丢失

**检查**:

```bash
docker inspect metersphere-grist --format '{{json .Mounts}}'
```

**处理**:

- 确认 `/persist` 已挂载到 volume。
- 不要使用临时容器目录存储正式文档。

### 问题 5：内网无法拉镜像

**处理**:

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

---

## 下一步计划

### 优先级 1：完成 POC 嵌入

1. 新增 Grist Compose 示例文件。
2. 启动 Grist 容器。
3. 新增 `/track/multitable` 路由和 iframe 页面。
4. 验证本地 `test-track` 前端可以嵌入 Grist。

### 优先级 2：配置化和内网化

1. 从后端配置读取 Grist 地址。
2. 增加项目到 Grist 文档映射。
3. 固定镜像版本和离线部署包。
4. 接入内网统一身份或可信代理认证。

### 优先级 3：用例同步

1. 定义 Grist 用例模板字段。
2. 实现手动同步 API。
3. 回写同步状态。
4. 评估 Webhook 自动同步。
