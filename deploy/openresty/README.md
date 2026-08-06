# MeterSphere OpenResty 上传白名单网关

在调用方（浏览器 / 其他服务）与后端 `gateway(8000)` 之间加一层 OpenResty 反向代理，
对**所有文件上传**做扩展名白名单拦截，不在名单的类型直接返回 `400`。

> 本目录配置针对**本地 Mac dev**：后端各服务由 `metersphere-control-panel` 以
> 宿主机 localhost 进程方式启动（gateway=8000、各模块 8001-8009）。

## 接入拓扑

```
浏览器 / 调用方
      │  :8081
      ▼
 OpenResty  (deploy/openresty, 白名单拦截)
      │  host.docker.internal:8000
      ▼
 gateway:8000   ← control-panel 启动的本地进程
      │  (Eureka 服务发现)
      ▼
 各业务模块 api-test / test-track / system-setting ...
```

## 启动步骤

1. 用 `metersphere-control-panel` 把所需后端服务跑起来（至少 `gateway:8000`）。
   ```bash
   # control-panel UI 中批量启动，或确认 gateway 已在 localhost:8000 监听
   lsof -iTCP:8000 -sTCP:LISTEN   # 应能看到监听
   ```
2. 启动 OpenResty（项目根目录执行）：
   ```bash
   cd /Users/edy/ideaProjects/metersphere
   docker compose -f deploy/openresty/docker-compose-openresty.yml up -d
   docker ps | grep ms-openresty   # 状态 healthy
   ```

### 离线镜像（内网/无外网服务器）
桌面原始包 `/Users/edy/Desktop/openresty/openresty.tar`（103MB）是离线镜像：
```bash
docker load -i /Users/edy/Desktop/openresty/openresty.tar
# 再把 docker-compose-openresty.yml 里的 image 改成本地加载出的镜像名
```
> 该 tar 体积大，**不纳入 git**（见项目 .gitignore 约定）。

### 裸跑模式（不用 docker）
若本机已 `brew install openresty`，可不用容器，直接：
```bash
openresty -p $PWD/deploy/openresty -c $PWD/deploy/openresty/nginx.conf
```
并把 `nginx.conf` 里 `proxy_pass http://host.docker.internal:8000` 改为
`http://127.0.0.1:8000`。

## 验证

```bash
# 1) 白名单外类型 → 期望 400
curl -s -F "file=@/tmp/test.psd" http://localhost:8081/project/file/metadata/create
# => {"success":false,"message":"不允许上传该类型文件: test.psd，请联系管理员"}

# 2) 白名单内类型 → 期望转发到 gateway（200/业务响应）
curl -s -F "file=@/tmp/test.xlsx" http://localhost:8081/project/file/metadata/create

# 3) 非上传请求（GET/JSON）→ 直接放行
curl -s http://localhost:8081/health
```

## 白名单类型（严格按产品决策）

| 类别 | 允许后缀 |
|---|---|
| 文档 | xlsx xls xlsb csv docx doc pdf ppt pptx wps |
| 图片 | png jpeg jpg jfif bmp |
| 压缩 | rar zip gzip tar |
| 文本/技术 | txt json sql xml jmx xmind |
| 可执行 | jar |

修改白名单：编辑 `nginx.conf` 的 `ALLOWED_EXTS`（位于 `init_by_lua_block`），
重启容器 `docker compose ... restart openresty` 生效。

## 注意事项

1. **dev 模式前端绕过 gateway，白名单不生效**：
   前端 `workstation/frontend/vue.config.js` 的 dev 代理目标是 `localhost:8007`（workstation 后端），
   不经 gateway、也不经 openresty。要让"全平台拦截"在 dev 生效，需把前端请求改指
   `localhost:8081`（或直接联调时把上传打到 8081）。**生产部署时让入口 nginx/CDN 指向
   openresty:8081 即可全平台生效**。

2. **GIF / MP4 未列入白名单**：当前清单未含 `gif`（存量约 1689）、`mp4`（存量约 598），
   缺陷录屏/操作动图场景会被拒绝。若需支持，把 `.gif` `.mp4` 加进 `ALLOWED_EXTS`。

3. **jar 为可执行代码**：白名单含 `.jar`（性能测试要传依赖包），但建议在生产环境
   把 jar 限制到性能测试上传入口，避免通用附件入口被塞入可执行文件。

4. **只拦新上传、不追溯存量**：已存在的 PSD/EML 等历史文件不受影响。

5. **body 读取**：大文件可能落临时磁盘，Lua 已处理 `get_body_file()` 兜底；
   若发现超大文件校验异常，调大 `client_max_body_size`（已设 1000m）。
