# Docker 服务启动指南

## 当前环境检测

根据系统检测，你使用的是 **OrbStack** 作为 Docker 运行环境。

```bash
Docker version 28.5.2, build ecc6942
Context: orbstack
```

## 启动 OrbStack

### 方法一：通过应用程序启动

1. 在 macOS 的 **应用程序** 文件夹中找到 **OrbStack**
2. 双击启动 OrbStack 应用
3. 等待 OrbStack 完全启动（状态栏会显示图标）

### 方法二：通过命令行启动

```bash
# 启动 OrbStack
open -a OrbStack

# 或者使用 orb 命令（如果已安装）
orb start
```

### 方法三：检查 OrbStack 状态

```bash
# 检查 OrbStack 是否运行
ps aux | grep -i orbstack

# 检查 Docker 上下文
docker context ls

# 切换到 OrbStack 上下文（如果需要）
docker context use orbstack
```

## 验证 Docker 服务

启动 OrbStack 后，验证 Docker 服务是否正常：

```bash
# 检查 Docker 版本
docker --version

# 检查 Docker 信息
docker info

# 测试 Docker 运行
docker run hello-world
```

## 启动开发环境

OrbStack 启动后，就可以启动 MeterSphere 开发环境了：

```bash
# 进入项目目录
cd metersphere

# 启动开发环境
./scripts/dev-env.sh start

# 检查服务状态
./scripts/dev-env.sh status

# 测试连接
./scripts/dev-env.sh test
```

## 解决 Docker 登录问题

OrbStack 启动后，可以尝试登录阿里云镜像仓库：

```bash
# 使用管理脚本测试连接
./scripts/docker-registry-helper.sh test

# 登录阿里云镜像仓库
./scripts/docker-registry-helper.sh login

# 或者直接使用 Docker 命令
docker login --username=aliyun1688079337 crpi-tysjadjbz5afeai8.cn-beijing.personal.cr.aliyuncs.com
```

## 常见问题解决

### 问题 1: OrbStack 无法启动

**解决方案**:
```bash
# 检查 OrbStack 进程
ps aux | grep -i orbstack

# 强制终止 OrbStack 进程
pkill -f orbstack

# 重新启动
open -a OrbStack
```

### 问题 2: Docker 上下文错误

**解决方案**:
```bash
# 查看可用上下文
docker context ls

# 切换到 OrbStack 上下文
docker context use orbstack

# 或切换到默认上下文
docker context use default
```

### 问题 3: 权限问题

**解决方案**:
```bash
# 检查 Docker 组权限
groups $USER

# 如果需要，添加用户到 docker 组
sudo usermod -aG docker $USER

# 重新登录或重启终端
```

## 自动启动配置

### 设置 OrbStack 开机自启

1. 打开 **系统偏好设置** > **用户与群组**
2. 选择当前用户，点击 **登录项**
3. 点击 **+** 添加 OrbStack 应用
4. 勾选 **隐藏** 选项（可选）

### 创建启动脚本

```bash
# 创建启动脚本
cat > ~/start-dev-env.sh << 'EOF'
#!/bin/bash

echo "启动 OrbStack..."
open -a OrbStack

echo "等待 Docker 服务启动..."
while ! docker info >/dev/null 2>&1; do
    echo "等待 Docker 服务..."
    sleep 2
done

echo "Docker 服务已启动！"

echo "启动 MeterSphere 开发环境..."
cd ~/path/to/metersphere  # 替换为实际路径
./scripts/dev-env.sh start

echo "开发环境启动完成！"
EOF

# 设置执行权限
chmod +x ~/start-dev-env.sh
```

## 性能优化建议

### OrbStack 资源配置

1. 打开 OrbStack 应用
2. 进入 **设置** > **资源**
3. 根据需要调整：
   - **CPU**: 建议 4-8 核
   - **内存**: 建议 8-16 GB
   - **磁盘**: 建议 100+ GB

### Docker 镜像清理

```bash
# 定期清理无用镜像
./scripts/docker-registry-helper.sh clean

# 或使用 Docker 命令
docker system prune -a -f
```

## 监控和日志

### 查看 OrbStack 日志

```bash
# 查看 OrbStack 日志
tail -f ~/Library/Logs/OrbStack/orbstack.log
```

### 查看容器日志

```bash
# 查看所有服务日志
./scripts/dev-env.sh logs

# 查看特定服务日志
./scripts/dev-env.sh logs mysql
./scripts/dev-env.sh logs redis
```

## 备用方案

如果 OrbStack 持续有问题，可以考虑：

### 方案一：安装 Docker Desktop

```bash
# 下载并安装 Docker Desktop for Mac
# https://www.docker.com/products/docker-desktop

# 卸载 OrbStack（如果需要）
# 从应用程序文件夹删除 OrbStack
```

### 方案二：使用 Colima

```bash
# 安装 Colima
brew install colima

# 启动 Colima
colima start

# 验证安装
docker info
```

## 下一步

1. **启动 OrbStack**: `open -a OrbStack`
2. **验证 Docker**: `docker info`
3. **测试连接**: `./scripts/docker-registry-helper.sh test`
4. **登录仓库**: `./scripts/docker-registry-helper.sh login`
5. **启动环境**: `./scripts/dev-env.sh start`

---

**注意**: 确保 OrbStack 完全启动后再执行 Docker 相关命令，否则会出现连接错误。