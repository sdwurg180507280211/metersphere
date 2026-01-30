# ECS 环境准备

本文档涵盖 Jenkins 构建所需的 ECS 环境配置。

> ⚠️ 以下命令会修改系统状态，执行前确认在测试机而非生产机。

## 1. JDK 17 配置

Jenkinsfile 写死 `JAVA_HOME=/opt/jdk-17`，需创建软链接指向实际安装位置。

```bash
# 检查系统 JDK
java -version

# 创建软链接（对齐 Jenkinsfile）
sudo rm -f /opt/jdk-17
sudo ln -sfn /usr/lib/jvm/java-17-openjdk-amd64 /opt/jdk-17

# 验证
/opt/jdk-17/bin/java -version
```

## 2. Maven 配置

Jenkinsfile 写死 `/opt/apache-maven-3.8.3/bin/mvn`。

```bash
# apt 安装 Maven
sudo apt-get update -y
sudo apt-get install -y maven

# 验证系统安装
mvn -version

# 创建软链接（对齐 Jenkinsfile）
sudo rm -rf /opt/apache-maven-3.8.3
sudo ln -sfn /usr/share/maven /opt/apache-maven-3.8.3

# 验证
/opt/apache-maven-3.8.3/bin/mvn -version
```

## 3. Docker 与 buildx

### 3.1 验证 Docker 可用

```bash
docker version
docker buildx version
```

### 3.2 启用多架构支持

Jenkinsfile 使用 `--platform linux/amd64,linux/arm64`，需确保 buildx 支持：

```bash
# 查看当前 builder
docker buildx ls

# 检查支持的平台
docker buildx inspect --bootstrap
```

### 3.3 Docker 权限配置

jenkins 和 deploy 用户都需要 Docker 权限：

```bash
# 确保 docker 组存在
getent group docker >/dev/null 2>&1 || sudo groupadd docker

# 加入 docker 组
sudo usermod -aG docker jenkins
sudo usermod -aG docker deploy

# 修复 docker.sock 权限
sudo chgrp docker /var/run/docker.sock
sudo chmod 660 /var/run/docker.sock

# 重启服务使权限生效
sudo systemctl restart docker
sudo systemctl restart jenkins

# 验证
sudo -u jenkins docker version
sudo -u deploy docker version
```

## 4. deploy 用户配置

deploy 用户用于 Jenkins 远程 SSH 执行部署命令。

### 4.1 创建用户

```bash
# 创建用户（已存在则跳过）
id deploy >/dev/null 2>&1 || sudo useradd -m -s /bin/bash deploy

# 加入 docker 组
sudo usermod -aG docker deploy
```

### 4.2 目录权限

```bash
# /opt/metersphere 目录可写
sudo chgrp deploy /opt/metersphere
sudo chmod 775 /opt/metersphere

# install.conf 可能是软链，需授权真实文件
REAL_CONF=$(readlink -f /opt/metersphere/install.conf || echo /opt/metersphere/install.conf)
sudo chgrp deploy "$REAL_CONF"
sudo chmod 664 "$REAL_CONF"

# 验证
sudo -u deploy test -w /opt/metersphere && echo "目录可写"
sudo -u deploy test -w "$REAL_CONF" && echo "配置文件可写"
```

### 4.3 SSH 密钥配置

```bash
# 创建 .ssh 目录
sudo -u deploy mkdir -p /home/deploy/.ssh
sudo -u deploy chmod 700 /home/deploy/.ssh

# 将 Jenkins 的公钥追加到 authorized_keys
# （公钥内容从 Jenkins 凭证对应的私钥生成）
echo "ssh-rsa AAAA..." | sudo -u deploy tee -a /home/deploy/.ssh/authorized_keys
sudo -u deploy chmod 600 /home/deploy/.ssh/authorized_keys
```

## 5. 前端构建优化（可选）

如果 npm install 卡死，在 Jenkinsfile 中添加：

```bash
# Node 内存优化
export NODE_OPTIONS="--max-old-space-size=4096"

# npm 镜像源和超时配置
export npm_config_registry=https://registry.npmmirror.com
export npm_config_fetch_retries=5
export npm_config_fetch_timeout=300000
```

## 环境验证清单

全部配置完成后，执行以下验证：

```bash
echo "=== JDK ==="
/opt/jdk-17/bin/java -version

echo "=== Maven ==="
/opt/apache-maven-3.8.3/bin/mvn -version

echo "=== Docker (jenkins) ==="
sudo -u jenkins docker version --format '{{.Server.Version}}'

echo "=== Docker (deploy) ==="
sudo -u deploy docker version --format '{{.Server.Version}}'

echo "=== buildx ==="
docker buildx version

echo "=== deploy 用户权限 ==="
sudo -u deploy test -w /opt/metersphere && echo "目录可写: OK"
```
