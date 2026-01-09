# MeterSphere Jenkinsfile 使用手册

## 📖 简介

这是一个**极简设计、清晰易懂**的 Jenkinsfile，参考了优秀的 Shell 脚本设计理念：

- ✅ 清晰的配置区域
- ✅ 模块化函数设计
- ✅ 简单的主流程
- ✅ 多种部署方式
- ✅ 无复杂的健康检查
- ✅ 适合初学者

---

## 🎯 核心流程

```
准备阶段
   ↓
Maven 编译
   ↓
Docker 构建
   ↓
部署（可选）
```

**就这么简单！** 没有复杂的健康检查、耗时统计、缓存管理等。

---

## 🔧 参数说明

### BUILD_TARGET（构建目标）

| 选项 | 说明 |
|------|------|
| `all` | 构建所有模块 |
| `framework` | 只构建 eureka + gateway |
| `api-test` | 只构建 API 测试模块 |
| `performance-test` | 只构建性能测试模块 |
| 其他 | 只构建指定模块 |

### BUILD_FRONTEND（构建前端）

- `false`（默认）：不构建前端，快速构建后端
- `true`：构建前端，完整构建

### DEPLOY_ENV（部署环境）

| 环境 | 说明 | 需要确认 |
|------|------|----------|
| `none` | 不部署 | ❌ |
| `dev` | 开发环境 | ❌ |
| `test` | 测试环境 | ❌ |
| `staging` | 预发布环境 | ❌ |
| `prod` | 生产环境 | ✅ 需要 |

### DEPLOY_METHOD（部署方式）

| 方式 | 说明 | 场景 |
|------|------|------|
| `docker-compose` | Docker Compose | 单机部署 |
| `k8s-helm` | Kubernetes Helm | K8s集群（推荐） |
| `k8s-kubectl` | Kubernetes kubectl | K8s集群（简单） |
| `manual` | 手动部署 | 仅构建镜像 |

### 其他参数

- **SKIP_TESTS**: 跳过单元测试（默认 true）
- **CLEAN_BUILD**: 清理构建（默认 false）
- **PUSH_IMAGE**: 推送镜像（默认 true）

---

## 📝 使用场景

### 场景1：日常开发（快速）

```
BUILD_TARGET: api-test
BUILD_FRONTEND: false
DEPLOY_ENV: dev
DEPLOY_METHOD: docker-compose
SKIP_TESTS: true
```

**耗时**: 约 5 分钟

---

### 场景2：测试环境发布

```
BUILD_TARGET: all
BUILD_FRONTEND: true
DEPLOY_ENV: test
DEPLOY_METHOD: docker-compose
```

**耗时**: 约 20 分钟

---

### 场景3：生产环境发布（K8s）

```
BUILD_TARGET: all
BUILD_FRONTEND: true
DEPLOY_ENV: prod
DEPLOY_METHOD: k8s-helm
SKIP_TESTS: false
```

**特点**: 需要人工确认，运行单元测试

---

### 场景4：只构建镜像

```
BUILD_TARGET: all
BUILD_FRONTEND: true
DEPLOY_ENV: none
```

**说明**: 只构建并推送镜像，不部署

---

## 🚀 部署方式详解

### 1. Docker Compose 部署

**适用场景**: 单台服务器部署

**前提条件**:
- 目标服务器已安装 Docker 和 Docker Compose
- 目标服务器上有 `/opt/metersphere` 目录
- 该目录下有 `install.conf` 和 `compose_files`

**工作流程**:
1. SSH 连接到目标服务器
2. 修改 `install.conf` 中的镜像版本
3. 执行 `docker-compose pull` 拉取镜像
4. 执行 `docker-compose up -d` 重启服务

**所需凭证**:
- `dev-server-ip`: 开发服务器 IP
- `test-server-ip`: 测试服务器 IP
- `staging-server-ip`: 预发布服务器 IP
- `prod-server-ip`: 生产服务器 IP
- `metersphere-deploy-key`: SSH 密钥

---

### 2. Kubernetes Helm 部署

**适用场景**: K8s 集群部署（推荐）

**前提条件**:
- K8s 集群可访问
- 已安装 Helm 3
- 有 `./helm/metersphere` Chart 包

**工作流程**:
1. 更新 Helm 仓库
2. 执行 `helm upgrade --install` 部署
3. 等待 Pod 就绪

**命名空间**:
- dev → `metersphere-dev`
- test → `metersphere-test`
- staging → `metersphere-staging`
- prod → `metersphere-prod`

---

### 3. Kubernetes kubectl 部署

**适用场景**: K8s 集群简单部署

**前提条件**:
- K8s 集群可访问
- 已有运行中的 Deployment

**工作流程**:
1. 使用 `kubectl set image` 更新镜像
2. 等待滚动更新完成

**说明**: 只更新镜像版本，不会改变其他配置

---

### 4. Manual（手动部署）

**适用场景**: 
- 复杂的部署流程
- 灰度发布
- 需要人工介入

**工作流程**:
1. 构建并推送镜像
2. 在控制台输出镜像标签
3. 由运维人员手动部署

---

## 🔍 关键函数说明

### printBanner()
打印欢迎横幅，让日志更友好。

### printConfig()
打印构建配置，方便调试。

### buildMavenCommand()
动态构建 Maven 命令，根据参数自动添加选项。

### getModulesToBuild()
获取要构建的模块列表。

### unpackJars()
解压 JAR 文件，为 Docker 构建做准备。

### buildDockerImage(module)
构建单个模块的 Docker 镜像并推送。

### deployApplication()
根据选择的部署方式执行部署。

### deployWithDockerCompose()
使用 Docker Compose 部署到远程服务器。

### deployWithHelm()
使用 Helm 部署到 K8s 集群。

### deployWithKubectl()
使用 kubectl 更新 K8s 部署。

---

## 🛠️ Jenkins 配置

### 必需的凭证

| ID | 类型 | 用途 |
|---|---|---|
| `metersphere-maven` | Config File | Maven settings.xml |
| `aliyun-acr-personal` | Username/Password | 阿里云镜像仓库登录 |
| `dev-server-ip` | Secret Text | 开发服务器IP |
| `test-server-ip` | Secret Text | 测试服务器IP |
| `staging-server-ip` | Secret Text | 预发布服务器IP |
| `prod-server-ip` | Secret Text | 生产服务器IP |
| `metersphere-deploy-key` | SSH Key | SSH 部署密钥 |
| `wechat-bot-webhook` | Secret Text | 企业微信机器人（可选） |

### 可选的凭证

如果不使用某个环境，可以不配置对应的凭证。

---

## 📚 快速上手

### 第一次使用

**步骤1**: 配置 Jenkins 凭证
```
进入 Jenkins → 凭据 → 添加凭据
按照上面的表格添加所需凭证
```

**步骤2**: 创建 Pipeline 任务
```
新建任务 → Pipeline → 确定
配置 → Pipeline Script → 粘贴 Jenkinsfile
保存
```

**步骤3**: 第一次构建
```
构建参数：
BUILD_TARGET: framework
BUILD_FRONTEND: false
DEPLOY_ENV: none
```

**步骤4**: 检查结果
```
查看控制台输出
检查镜像是否推送成功：
docker images | grep metersphere
```

---

### 常见问题

**Q1: 构建失败，提示 Maven 错误？**

A: 检查以下几点：
1. `metersphere-maven` 凭证是否配置正确
2. Maven 仓库网络是否正常
3. 是否需要设置 `CLEAN_BUILD: true`

---

**Q2: Docker 镜像推送失败？**

A: 检查：
1. `aliyun-acr-personal` 凭证是否正确
2. 网络是否可以访问镜像仓库
3. 镜像仓库是否有足够空间

---

**Q3: 部署失败，SSH 连接超时？**

A: 检查：
1. 服务器 IP 凭证是否正确
2. SSH 密钥是否配置
3. 服务器防火墙是否开放 SSH 端口

---

**Q4: K8s 部署失败？**

A: 检查：
1. K8s 集群是否可访问
2. Helm Chart 是否存在
3. 命名空间是否已创建

---

**Q5: 如何跳过某个阶段？**

A: 
- 跳过部署：设置 `DEPLOY_ENV: none`
- 跳过测试：设置 `SKIP_TESTS: true`
- 跳过推送：设置 `PUSH_IMAGE: false`

---

## 🎨 日志输出示例

```
╔════════════════════════════════════════╗
║      MeterSphere 自动构建流程          ║
║      Build #123                        ║
╚════════════════════════════════════════╝

═══════════════════════════════════════
 构建配置
═══════════════════════════════════════
分支:         develop
版本:         develop
提交:         a1b2c3d4
构建号:       #123

构建目标:     all
构建前端:     true
跳过测试:     true

部署环境:     test
部署方式:     docker-compose
═══════════════════════════════════════

🔨 开始编译...
📦 Maven 编译中...
✅ Maven 编译完成

🐳 开始构建 Docker 镜像...
📋 需要构建 9 个模块
🔨 构建: eureka
✅ 推送完成: xxx/eureka:develop
🔨 构建: gateway
✅ 推送完成: xxx/gateway:develop
...
✅ Docker 镜像构建完成

🚀 开始部署...
🚀 部署到 test 环境（方式: docker-compose）
📝 更新配置...
📥 拉取镜像...
🔄 重启服务...
✅ 部署完成

╔════════════════════════════════════════╗
║          ✅ 构建成功                   ║
╚════════════════════════════════════════╝
分支: develop
版本: develop
模块: all
环境: test
耗时: 18 min
```

---

## 🔄 与原脚本对比

### 去除的功能

| 功能 | 说明 |
|------|------|
| ❌ 健康检查 | 简化部署流程 |
| ❌ 前端缓存 | 降低复杂度 |
| ❌ 耗时统计 | 关注核心流程 |
| ❌ 产物归档 | 镜像仓库已保存 |
| ❌ 代码质量检查 | 可选功能 |
| ❌ 权限检查 | 简化逻辑 |

### 保留的功能

| 功能 | 说明 |
|------|------|
| ✅ 模块化构建 | 灵活选择模块 |
| ✅ 前端构建控制 | 加速后端开发 |
| ✅ Docker 构建 | 核心功能 |
| ✅ 多种部署方式 | 适应不同场景 |
| ✅ 多环境支持 | dev/test/staging/prod |
| ✅ 通知功能 | 及时反馈 |

---

## 💡 最佳实践

### 开发阶段
```
BUILD_TARGET: 单个模块
BUILD_FRONTEND: false
DEPLOY_ENV: dev
SKIP_TESTS: true
```

### 测试阶段
```
BUILD_TARGET: all
BUILD_FRONTEND: true
DEPLOY_ENV: test
SKIP_TESTS: true
```

### 生产发布
```
BUILD_TARGET: all
BUILD_FRONTEND: true
DEPLOY_ENV: prod
SKIP_TESTS: false
+ 需要人工确认
```

---

## 📞 获取帮助

### 查看构建日志
```
Jenkins → 项目 → 构建历史 → Console Output
```

### 查看镜像信息
```bash
# 在镜像仓库查看
docker images | grep metersphere

# 查看镜像详情
docker inspect <image>
```

### 手动部署
```bash
# Docker Compose
ssh deploy@server
cd /opt/metersphere
docker-compose -f docker-compose.yml up -d

# Kubernetes
kubectl get pods -n metersphere-test
kubectl logs <pod-name> -n metersphere-test
```

---

## ✅ 总结

这个简化版 Jenkinsfile 的优势：

1. **极简设计** - 去除所有不必要的功能
2. **清晰易懂** - 代码结构简单，注释详细
3. **灵活配置** - 支持多种构建和部署场景
4. **适合学习** - 没有复杂逻辑，专注核心流程
5. **生产可用** - 虽然简单，但功能完整

从这个脚本开始学习 Jenkins，是一个很好的起点！🎉

---

## 📖 进阶学习

如果你想添加更多功能，可以参考：

1. **添加健康检查**: 在部署后检查服务状态
2. **添加回滚功能**: 部署失败自动回滚
3. **添加并行构建**: 同时构建多个模块
4. **添加缓存机制**: 加速前端构建
5. **添加测试报告**: 展示测试结果

但记住：**简单就是美！** 不要过早优化。
