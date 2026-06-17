# APITable 多维表嵌入测试跟踪 - 前端测试指南

## 测试环境

### MeterSphere 中间件容器

- **当前本机 Compose 文件**: `/opt/metersphere/docker-compose-*.yml`
- **当前本机网络**: `metersphere_ms-network`
- **仓库开发 Compose 文件**: `docker-compose-dev.yml`
- **仓库开发网络**: `metersphere-dev`
- **启动命令**:
  ```bash
  # 当前本机如已通过 /opt/metersphere 启动中间件，可跳过
  # 使用仓库开发中间件时再执行：
  docker compose -f docker-compose-dev.yml up -d
  ```

### APITable POC 容器

- **POC 镜像**: `apitable/all-in-one`
- **本地地址**: http://localhost:8088/
- **容器名**: `metersphere-apitable`
- **启动命令**:
  ```bash
  docker compose -f .kiro/specs/apitable-multitable-integration/docker-compose-apitable-poc.yml up -d
  ```

> 注：POC Compose 默认挂载当前本机的 `metersphere_ms-network`。如果使用仓库里的 `docker-compose-dev.yml`，启动 APITable 时增加 `APITABLE_DOCKER_NETWORK=metersphere-dev`。

> 注：`apitable/all-in-one` 仅用于 demo/testing，不建议用于企业或生产环境。APITable 官方 README 建议至少 4 CPU / 8GB RAM，Apple Silicon 或 arm64 环境可能性能较差。

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

## 1. APITable 容器测试

### 1.1 启动 APITable

```bash
docker compose -f .kiro/specs/apitable-multitable-integration/docker-compose-apitable-poc.yml up -d
docker ps | grep metersphere-apitable
```

**预期结果**:

- `metersphere-apitable` 状态为 `Up`。
- 本机可访问 http://localhost:8088/。
- 页面进入 APITable 初始化、注册或登录页。
- 首次启动可能需要等待数分钟。

### 1.2 验证持久化

1. 在 APITable 中创建一个测试 Space 或 Datasheet。
2. 重启容器：
   ```bash
   docker restart metersphere-apitable
   ```
3. 再次访问 http://localhost:8088/。

**预期结果**:

- 测试 Space / Datasheet 仍然存在。
- 容器重启不丢失数据。

### 1.3 记录资源占用

```bash
docker stats metersphere-apitable
```

**预期结果**:

- 记录 CPU 和内存峰值。
- 如果本机明显卡顿，停止 APITable POC 容器：
  ```bash
  docker stop metersphere-apitable
  ```

### 1.4 验证 no-internet 风险

**检查项**:

- 容器启动是否依赖公网。
- 初始化、模板、帮助、Widget、Automation 是否访问公网。
- 是否有外部 AI、统计、更新检查配置。

**预期结果**:

- POC 阶段记录所有外联域名。
- 正式内网试点前禁用或替换为内网地址。

---

## 2. MeterSphere 前端路由测试

### 2.1 访问 APITable 页面

- **URL**: `http://localhost:4005/#/track/apitable`

**预期结果**:

- 页面正常加载。
- 顶部测试跟踪菜单展示“APITable 编写”或“多维表编写（APITable）”。
- 当前菜单高亮为 APITable 菜单项。
- 页面主体显示 APITable iframe。

### 2.2 检查路由配置

- **文件**: `test-track/frontend/src/router/modules/track.js`
- **路由路径**: `/track/apitable`
- **组件**: `ApitableMultitable.vue`

**预期结果**:

- 路由已配置。
- 路由组件被 `TestTrack.vue` 的内部 `router-view` 渲染。

### 2.3 检查菜单权限

- **文件**: `test-track/frontend/src/business/head/TrackHeaderMenus.vue`
- **权限**: `PROJECT_TRACK_CASE:READ`

**预期结果**:

- 有测试用例读取权限的用户能看到 APITable 入口。
- 无该权限的用户不展示菜单项。

---

## 3. iframe 嵌入测试

### 3.1 正常加载

1. 配置 APITable 地址：
   ```bash
   VUE_APP_APITABLE_BASE_URL=http://localhost:8088
   VUE_APP_APITABLE_DEFAULT_PATH=/
   ```
2. 启动 `test-track/frontend`。
3. 访问 `/track/apitable`。

**预期结果**:

- iframe 中展示 APITable 页面。
- iframe 宽度占满内容区。
- 页面无明显双重滚动。
- 浏览器控制台无跨域或 frame 拦截错误。
- APITable 开源版品牌标识未被遮挡或隐藏。

### 3.2 配置缺失

1. 移除 `VUE_APP_APITABLE_BASE_URL`。
2. 重启前端。
3. 访问 `/track/apitable`。

**预期结果**:

- 页面展示“未配置 APITable 多维表地址”。
- 不出现空白页。
- 控制台无未捕获异常。

### 3.3 APITable 服务未启动

1. 停止 APITable：
   ```bash
   docker stop metersphere-apitable
   ```
2. 访问 `/track/apitable`。

**预期结果**:

- 页面出现加载失败或浏览器连接失败提示。
- “新窗口打开”按钮可用于排查地址。
- MeterSphere 其他页面不受影响。

---

## 4. APITable 用例模板测试

### 4.1 创建模板表

在 APITable 中创建 Space `MeterSphere 测试用例辅助编写`，新增 Datasheet `TestCases`。

字段建议：

| 字段 | 类型 |
|------|------|
| case_name | Text |
| module | SingleSelect/Text |
| priority | SingleSelect |
| case_type | SingleSelect |
| prerequisite | LongText |
| steps | LongText |
| expected_result | LongText |
| owner | Member/Text |
| status | SingleSelect |
| requirement_id | Text |
| tags | MultiSelect |
| ms_case_id | Text |
| source_record_id | Text |
| sync_status | SingleSelect |
| sync_time | DateTime |
| sync_error | LongText |

**预期结果**:

- 可以新增、编辑、过滤测试用例数据。
- 可以创建“待评审”“评审通过待同步”等视图。
- 可以创建按负责人或状态分组的看板视图。
- 记录刷新后仍保留。

### 4.2 导出模板

**预期结果**:

- 能通过 APITable 自身能力或 CSV/Excel 方式导出模板。
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
- 顶部菜单高亮逻辑不被 `/track/apitable` 影响。
- 用例列表、评审、计划、缺陷功能无新增报错。

### 5.2 微前端加载

从主应用进入测试跟踪模块。

**预期结果**:

- 主应用能正常加载 `test-track` 子应用。
- 点击 APITable 入口后仍在 MeterSphere 页面内部。
- 浏览器地址栏 hash 正确变更为 `#/track/apitable`。

---

## 6. 瑞众内网检查项

### 6.1 镜像来源

**检查项**:

- 镜像通过外网机器提前下载。
- 内网通过 `docker load` 或内网镜像仓库导入。
- 记录镜像 tag 和 digest。
- 正式试点不使用 all-in-one 作为生产部署形态。

### 6.2 外联依赖

**检查项**:

- 容器启动时不需要访问公网。
- 未配置外部 AI、统计、更新检查。
- Widget / Automation / 模板等资源为空或指向内网地址。
- 模板通过离线 CSV/Excel 或内网导入文件初始化。

### 6.3 数据边界

**检查项**:

- POC 阶段仅使用脱敏或测试用例样例数据。
- 正式用例主数据仍在 MeterSphere。
- APITable 数据卷或数据库位于内网服务器。

### 6.4 合规边界

**检查项**:

- 记录 APITable AGPL-3.0 许可证。
- 不移除或遮挡 “Powered by APITable” 标识。
- 如果修改 APITable 开源版代码，先进行法务评审。
- 如果需要高级嵌入或去品牌，评估商业许可。

---

## 常见问题排查

### 问题 1：APITable 页面打不开

**检查**:

```bash
docker ps | grep metersphere-apitable
docker logs --tail=100 metersphere-apitable
curl -I http://localhost:8088
```

**处理**:

- 确认端口 8088 未被占用。
- 确认容器已启动。
- 首次启动等待数分钟。
- 确认防火墙或代理未拦截本机访问。

### 问题 2：iframe 空白

**检查**:

- 浏览器控制台是否有 `X-Frame-Options` 或 `Content-Security-Policy` 错误。
- `VUE_APP_APITABLE_BASE_URL` 是否配置正确。
- iframe `src` 是否能在新窗口直接打开。

**处理**:

- POC 阶段先使用同机 `http://localhost:8088`。
- 优先使用 APITable Share / Embed 链接。
- 正式阶段可通过同域反向代理减少 iframe 安全策略冲突。

### 问题 3：菜单不展示

**检查**:

- 当前用户是否具备 `PROJECT_TRACK_CASE:READ`。
- `TrackHeaderMenus.vue` 是否增加菜单项。
- `SECOND_LEVEL_ROUTE_PERMISSION_MAP.TRACK` 是否增加路由映射。

### 问题 4：APITable 数据重启后丢失

**检查**:

```bash
docker inspect metersphere-apitable --format '{{json .Mounts}}'
```

**处理**:

- 确认 `/apitable` 已挂载到 volume。
- 不要使用临时容器目录存储正式数据。

### 问题 5：内网无法拉镜像

**处理**:

外网机器：

```bash
docker pull apitable/all-in-one:latest
docker save apitable/all-in-one:latest -o apitable-all-in-one-latest.tar
```

内网机器：

```bash
docker load -i apitable-all-in-one-latest.tar
docker images | grep apitable
```

正式试点还需要下载官方 Compose 包并导出其中引用的全部镜像。

### 问题 6：APITable backend-server 反复重启

**典型日志**:

```text
ALTER TABLE apitable_     -DDB_ENGINE=mysqlclient_release_version
```

**原因**:

`apitable/all-in-one:latest` 的 `/usr/local/bin/init-appdata.sh` 在 Liquibase 参数中存在引号问题，`-DDB_ENGINE=mysql` 会被拼进 `table.prefix`。

**处理**:

- 使用本规格的 `docker-compose-apitable-poc.yml` 启动。该 Compose 会挂载 `apitable-all-in-one-entrypoint.sh`，在启动前修复引号问题。
- 如果已经产生错误表名，可清理 APITable POC volume 后重建；不要在正式数据环境中直接删除 volume。
- 正式试点优先使用 APITable 官方 Compose 离线部署包，并固定镜像版本。

### 问题 7：本机明显变卡

**检查**:

```bash
docker stats metersphere-apitable
```

**处理**:

- 停止 APITable POC：
  ```bash
  docker stop metersphere-apitable
  ```
- 优先在 x86_64 测试服务器验证 APITable。
- 本机只保留 Grist 或 APITable 其中一个 POC 容器运行。

---

## 下一步计划

### 优先级 1：完成 APITable POC 嵌入

1. 启动 APITable POC 容器。
2. 创建测试用例 Datasheet。
3. 新增 `/track/apitable` 路由和 iframe 页面。
4. 验证本地 `test-track` 前端可以嵌入 APITable。

### 优先级 2：内网化与合规

1. 准备官方 Compose 离线部署包。
2. 固定镜像版本和 digest。
3. 梳理 APITable 外联行为。
4. 完成 AGPL 和嵌入许可评审。

### 优先级 3：用例同步

1. 定义 APITable 用例模板字段。
2. 实现手动同步 API。
3. 回写同步状态。
4. 评估 Webhook 或定时同步。
