# Jenkins 配置

本文档涵盖 Jenkins 的安装与配置，包括插件、凭证、Job 创建。

## 1. 安装 Jenkins

### 1.1 apt 安装

```bash
# 添加 Jenkins 源
sudo install -d -m 0755 /etc/apt/keyrings
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo gpg --dearmor -o /etc/apt/keyrings/jenkins.gpg
sudo chmod 0644 /etc/apt/keyrings/jenkins.gpg
echo "deb [signed-by=/etc/apt/keyrings/jenkins.gpg] https://pkg.jenkins.io/debian-stable binary/" | sudo tee /etc/apt/sources.list.d/jenkins.list

# 安装并启动
sudo apt-get update -y
sudo apt-get install -y jenkins
sudo systemctl enable --now jenkins
```

### 1.2 获取初始密码

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

### 1.3 访问 UI

浏览器访问 `http://<ECS公网IP>:8080`

> 需在阿里云安全组放行 8080/tcp

## 2. 插件安装

**路径**：`Manage Jenkins` → `Plugins` → `Available plugins`

必装插件：

| 插件 | 用途 |
|------|------|
| Pipeline | 流水线基础 |
| Git | 代码拉取 |
| SSH Agent Plugin | SSH 部署 |
| Credentials Binding Plugin | 凭证注入 |
| Config File Provider Plugin | Maven settings.xml |

安装后重启 Jenkins。

**验证**：`Manage Jenkins` 能看到 `Credentials` 和 `Managed files` 入口。

## 3. 凭证配置

**路径**：`Manage Jenkins` → `Credentials` → `System` → `Global credentials` → `Add Credentials`

### 3.1 ACR 登录凭证

用于 Docker 镜像推送。

| 字段 | 值 |
|------|-----|
| Kind | Username with password |
| ID | `aliyun-acr-personal` |
| Username | ACR 用户名（如 `aliyun1688079337`） |
| Password | ACR 访问凭证密码 |

### 3.2 Gitee 代码拉取凭证

用于 Multibranch 分支索引。

| 字段 | 值 |
|------|-----|
| Kind | Username with password |
| ID | `gitee-token` |
| Username | Gitee 用户名 |
| Password | Gitee Personal Access Token |

### 3.3 SSH 部署密钥

用于连接部署服务器。

| 字段 | 值 |
|------|-----|
| Kind | SSH Username with private key |
| ID | `metersphere-deploy-key` |
| Username | `deploy` |
| Private Key | 粘贴私钥内容（RSA 4096 PEM 格式，无口令） |

**生成兼容性最佳的密钥**：

```bash
ssh-keygen -t rsa -b 4096 -m PEM -f ~/.ssh/jenkins_deploy -N ""
```

### 3.4 服务器 IP

| 字段 | 值 |
|------|-----|
| Kind | Secret text |
| ID | `test-server-ip` |
| Secret | 测试服务器 IP |

同样方式创建 `prod-server-ip`（如有生产环境）。

### 3.5 Maven settings.xml

**路径**：`Manage Jenkins` → `Managed files` → `Add a new Config`

| 字段 | 值 |
|------|-----|
| Type | Maven settings.xml |
| ID | `metersphere-maven` |
| Content | 粘贴 settings.xml 内容 |

## 4. 节点配置

Jenkinsfile 要求 `agent { node { label 'metersphere' } }`。

**最简方案**：给 Built-in Node 添加 label。

**路径**：`Manage Jenkins` → `Nodes` → `Built-In Node` → `Configure`

| 字段 | 值 |
|------|-----|
| Labels | `metersphere` |

保存后，任务不再因找不到节点而排队。

## 5. 创建 Job

### 5.1 选择 Job 类型

| 类型 | 适用场景 |
|------|----------|
| Pipeline | 单分支、固定参数构建 |
| Multibranch Pipeline | 多分支自动发现、按分支并行构建 |

推荐：**Multibranch Pipeline**（自动发现 develop/develop-* 分支）

### 5.2 创建 Multibranch Pipeline

**路径**：`Dashboard` → `New Item`

1. 输入名称（如 `metersphere-multibranch`）
2. 选择 `Multibranch Pipeline`
3. 点击 `OK`

### 5.3 配置 Branch Sources

| 字段 | 值 |
|------|-----|
| Repository URL | Gitee 仓库地址 |
| Credentials | 选择 `gitee-token` |

### 5.4 配置 Build Configuration

| 字段 | 值 |
|------|-----|
| Script Path | `Jenkinsfile.enhanced` |

### 5.5 保存并扫描

保存后，点击左侧 `Scan Multibranch Pipeline Now`。

**成功标志**：
- 日志显示 `using GIT_ASKPASS to set credentials gitee-token`
- 发现分支后显示 `'Jenkinsfile.enhanced' found`
- 生成子 Job（如 `develop`、`develop-v2.10.26`）

## 6. 启动问题修复

如果 Jenkins 一直处于 `activating` 状态或返回 503：

```bash
# 创建 systemd 覆盖配置
sudo mkdir -p /etc/systemd/system/jenkins.service.d

cat <<'EOF' | sudo tee /etc/systemd/system/jenkins.service.d/override.conf
[Service]
Type=simple
NotifyAccess=none
TimeoutStartSec=10min
TimeoutStopSec=5min
EOF

# 重载并重启
sudo systemctl daemon-reload
sudo systemctl restart jenkins

# 验证（等待返回 200/302/403）
curl -I http://127.0.0.1:8080/login
```
