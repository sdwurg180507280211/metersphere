# Jenkinsfile 使用说明

## 文件说明

| 文件 | 说明 |
|------|------|
| `Jenkinsfile.simple` | 流水线脚本（放到代码仓库根目录） |
| `credentials.groovy` | 凭证配置（放到 Jenkins 服务器本地） |

## 快速开始

### 1. 配置凭证文件

```bash
# 在 Jenkins 服务器上操作

# 创建配置目录
sudo mkdir -p /var/lib/jenkins/config

# 复制配置模板
sudo cp credentials.groovy /var/lib/jenkins/config/

# 编辑配置
sudo vim /var/lib/jenkins/config/credentials.groovy

# 设置权限（仅 jenkins 用户可读）
sudo chown jenkins:jenkins /var/lib/jenkins/config/credentials.groovy
sudo chmod 600 /var/lib/jenkins/config/credentials.groovy
```

### 2. 配置 SSH 免密登录

```bash
# 生成密钥对（如果没有）
ssh-keygen -t rsa -b 4096 -f /var/lib/jenkins/.ssh/jenkins_deploy -N ""

# 设置权限
chown jenkins:jenkins /var/lib/jenkins/.ssh/jenkins_deploy*
chmod 600 /var/lib/jenkins/.ssh/jenkins_deploy

# 将公钥复制到目标服务器
ssh-copy-id -i /var/lib/jenkins/.ssh/jenkins_deploy.pub deploy@目标服务器IP

# 测试连接
sudo -u jenkins ssh -i /var/lib/jenkins/.ssh/jenkins_deploy deploy@目标服务器IP
```

### 3. 创建 Jenkins Job

1. `Dashboard` → `New Item`
2. 输入名称，选择 `Pipeline` 或 `Multibranch Pipeline`
3. 配置 SCM（Git 仓库地址）
4. Script Path 填写 `Jenkinsfile.simple`

## 凭证配置项说明

| 配置项 | 说明 | 获取方式 |
|--------|------|----------|
| `ACR_USERNAME` | ACR 用户名 | 阿里云控制台 → 容器镜像服务 → 访问凭证 |
| `ACR_PASSWORD` | ACR 密码 | 同上 |
| `TEST_SERVER_IP` | 测试服务器 IP | 部署 MeterSphere 的服务器 |
| `PROD_SERVER_IP` | 生产服务器 IP | 可选 |
| `DEPLOY_USER` | 部署用户名 | 默认 `deploy` |
| `SSH_PRIVATE_KEY_PATH` | SSH 私钥路径 | 默认 `/var/lib/jenkins/.ssh/jenkins_deploy` |
| `WECHAT_WEBHOOK` | 企业微信机器人 | 可选，不配置则不发通知 |

## 构建参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `IMAGE_TAG` | v2.10.26.0-lts | 镜像版本号 |
| `BUILD_TARGET` | all | 构建模块（all/framework/api-test/...） |
| `BUILD_FRONTEND` | false | 是否构建前端 |
| `DEPLOY_ENV` | test | 部署环境（test/prod） |
| `CLEAN_BUILD` | false | 清理构建 |
| `PUSH_IMAGE` | true | 推送镜像 |
| `DO_DEPLOY` | true | 执行部署 |

## 常用场景

### 只构建后端，不部署

```
BUILD_TARGET: all
BUILD_FRONTEND: false
DO_DEPLOY: false
```

### 只构建单个模块

```
BUILD_TARGET: api-test
BUILD_FRONTEND: false
```

### 完整构建并部署到测试环境

```
BUILD_TARGET: all
BUILD_FRONTEND: true
DEPLOY_ENV: test
DO_DEPLOY: true
```

## 安全建议

⚠️ **重要**：凭证配置文件包含敏感信息

1. **不要提交到 Git**：将 `credentials.groovy` 加入 `.gitignore`
2. **限制文件权限**：`chmod 600 credentials.groovy`
3. **生产环境建议**：使用 Jenkins Credentials 而非硬编码
4. **定期更换密码**：ACR 密码、SSH 密钥定期轮换

## 与 Jenkins Credentials 的对比

| 方式 | 优点 | 缺点 |
|------|------|------|
| 配置文件 | 简单直观，易于调试 | 安全性较低，需手动管理 |
| Jenkins Credentials | 安全性高，统一管理 | 配置复杂，需要 UI 操作 |

**建议**：
- 开发/测试环境：可用配置文件方式
- 生产环境：强烈建议使用 Jenkins Credentials
