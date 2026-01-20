# MeterSphere 模块化构建脚本使用指南

## 快速开始

### 1. 基本使用

```bash
# 赋予执行权限
chmod +x metersphere-build.sh

# 构建所有模块
./metersphere-build.sh -a

# 构建指定模块
./metersphere-build.sh gateway eureka test-track
```

### 2. 查看帮助

```bash
./metersphere-build.sh -h
```

### 3. 列出所有模块

```bash
./metersphere-build.sh -l
```

## 使用场景

### 场景 1: 首次完整构建

首次构建时需要初始化所有依赖：

```bash
./metersphere-build.sh -a
```

### 场景 2: 快速增量构建

如果已经初始化过依赖，后续构建可以跳过初始化步骤：

```bash
./metersphere-build.sh -s gateway api-test
```

### 场景 3: 并行构建（推荐用于多核机器）

利用多核 CPU 加速构建：

```bash
# 使用 4 个并行任务构建所有模块
./metersphere-build.sh -a -p -j 4

# 并行构建指定模块
./metersphere-build.sh -p -j 2 gateway eureka system-setting
```

**注意**: 并行构建需要更多内存，建议至少 8GB 可用内存

### 场景 4: 只构建镜像，不导出 tar（开发测试）

开发测试时，如果只需要本地镜像而不需要导出：

```bash
./metersphere-build.sh -a -b
```

### 场景 5: 指定版本和输出路径

```bash
./metersphere-build.sh -v v2.10.24-lts -o /data/packages/ms-20241224.tar -a
```

### 场景 6: 使用环境变量

```bash
# 方式 1: 临时设置
IMAGE_VERSION=v2.10.26.03-lts \
PARALLEL_BUILD=true \
MAX_JOBS=4 \
./metersphere-build.sh -a

# 方式 2: 导出环境变量
export IMAGE_VERSION=v2.10.26.03-lts
export PARALLEL_BUILD=true
./metersphere-build.sh -a
```

### 场景 7: 使用配置文件

```bash
# 1. 复制配置文件模板
cp build.config.example .build.config

# 2. 编辑配置文件
vim .build.config

# 3. 加载配置文件后执行
source .build.config && ./metersphere-build.sh -a
```

## 高级用法

### 1. 创建便捷脚本

创建 `build-all.sh`:

```bash
#!/bin/bash
source .build.config
./metersphere-build.sh -a -p -j 4
```

创建 `build-dev.sh`（开发环境快速构建）:

```bash
#!/bin/bash
export SKIP_INIT=true
export BUILD_ONLY=true
./metersphere-build.sh -p -j 4 "$@"
```

使用方式：

```bash
./build-dev.sh gateway api-test
```

### 2. 构建后自动部署

```bash
#!/bin/bash

# 构建
./metersphere-build.sh -a || exit 1

# 自动加载到测试服务器
scp /Users/edy/Desktop/metersphere-*.tar test-server:/tmp/
ssh test-server "docker load < /tmp/metersphere-*.tar"
```

### 3. 定时构建（使用 cron）

```bash
# 每天凌晨 2 点构建
0 2 * * * cd /path/to/project && ./metersphere-build.sh -a >> /var/log/ms-build.log 2>&1
```

### 4. CI/CD 集成

GitHub Actions 示例：

```yaml
name: Build MeterSphere
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build modules
        run: |
          chmod +x metersphere-build.sh
          ./metersphere-build.sh -a -p -j 4
      - name: Upload artifacts
        uses: actions/upload-artifact@v2
        with:
          name: metersphere-images
          path: /tmp/metersphere-*.tar
```

## 性能优化建议

### 1. 构建时间优化

| 策略 | 效果 | 适用场景 |
|------|------|----------|
| 跳过初始化 (`-s`) | 节省 2-5 分钟 | 增量构建 |
| 并行构建 (`-p -j 4`) | 节省 40-60% 时间 | 多核机器 |
| 只构建镜像 (`-b`) | 节省导出时间 | 开发测试 |
| 选择性构建 | 节省 70-90% 时间 | 单模块修改 |

### 2. 资源使用优化

```bash
# 内存限制较小的机器（< 8GB）
./metersphere-build.sh -a  # 串行构建

# 内存充足的机器（>= 16GB）
./metersphere-build.sh -a -p -j 4  # 并行构建

# CPU 密集型机器
./metersphere-build.sh -a -p -j 8  # 更多并行任务
```

### 3. 磁盘空间管理

```bash
# 构建前清理旧镜像
docker image prune -a -f

# 构建后检查大小
du -sh /Users/edy/Desktop/metersphere-*.tar

# 定期清理构建缓存
cd $PROJECT_PATH
mvn clean
```

## 故障排查

### 1. Maven 编译失败

```bash
# 清理 Maven 缓存
rm -rf ~/.m2/repository

# 重新初始化
./metersphere-build.sh -a
```

### 2. Docker 构建失败

```bash
# 检查 Docker 状态
docker info

# 清理 Docker 缓存
docker system prune -a

# 重新构建
./metersphere-build.sh -a
```

### 3. 磁盘空间不足

```bash
# 检查磁盘使用
df -h

# 清理 Docker 资源
docker system prune -a --volumes

# 清理 Maven 缓存
mvn dependency:purge-local-repository
```

### 4. 并行构建失败

如果并行构建出现问题，切换到串行构建：

```bash
./metersphere-build.sh -a  # 不加 -p 参数
```

查看详细日志：

```bash
# 并行构建会生成日志文件
ls /tmp/build_*.log

# 查看失败模块的日志
cat /tmp/build_gateway.log
```

### 5. 查看详细执行过程

编辑脚本，取消注释 `set -x`:

```bash
# 在脚本开头
set -x  # 显示详细执行过程
```

## 最佳实践

### 1. 构建流程

```
开发环境测试
    ↓
本地完整构建（-a）
    ↓
测试验证
    ↓
打标签构建（-v v2.x.x）
    ↓
导出 tar 包
    ↓
部署到生产环境
```

### 2. 版本管理

```bash
# 开发版本
./metersphere-build.sh -v dev-$(date +%Y%m%d) -a

# 测试版本
./metersphere-build.sh -v test-v2.10.26 -a

# 生产版本
./metersphere-build.sh -v v2.10.26-lts -a
```

### 3. 团队协作

建议在团队中统一使用配置文件：

```bash
# 提交配置文件模板到 Git
git add build.config.example

# 团队成员复制并自定义
cp build.config.example .build.config

# 忽略个人配置
echo ".build.config" >> .gitignore
```

### 4. 监控和日志

```bash
# 记录构建日志
./metersphere-build.sh -a 2>&1 | tee build-$(date +%Y%m%d-%H%M%S).log

# 监控构建进度（另开终端）
watch -n 1 'docker images | grep registry.fit2cloud.com/north'
```

## 常见问题

**Q: 为什么首次构建这么慢？**

A: 首次构建需要下载所有 Maven 依赖和 Docker 基础镜像。后续构建会使用缓存，速度会快很多。

**Q: 并行构建会不会有问题？**

A: 并行构建在大多数情况下是安全的，但如果模块间有隐含依赖，可能会失败。失败时使用串行构建。

**Q: 如何确认镜像构建成功？**

A: 使用以下命令验证：

```bash
docker images | grep registry.fit2cloud.com/north
```

**Q: 导出的 tar 文件很大，如何压缩？**

A: 可以使用压缩：

```bash
# 构建并压缩
./metersphere-build.sh -a
gzip /Users/edy/Desktop/metersphere-*.tar

# 或使用管道直接压缩
./metersphere-build.sh -a && \
  gzip /Users/edy/Desktop/metersphere-*.tar
```

**Q: 如何在其他机器上使用？**

A: 修改配置文件中的路径：

```bash
# 编辑配置文件
vim .build.config

# 修改 PROJECT_PATH 为实际路径
PROJECT_PATH=/your/project/path
```

## 扩展开发

### 添加新模块

如果项目增加了新模块，在脚本中添加：

```bash
# 在 MODULES 数组中添加
declare -A MODULES=(
    ...
    ["new-module"]="path/to/new-module"
)

# 如果是简单模块（无 backend 目录），添加到 SIMPLE_MODULES
declare -A SIMPLE_MODULES=(
    ...
    ["new-module"]=1
)
```

### 自定义构建逻辑

可以在 `build_module` 函数中添加自定义逻辑：

```bash
build_module() {
    local module_name=$1
    
    # 自定义逻辑
    if [ "$module_name" = "special-module" ]; then
        # 特殊处理
        npm install
        npm run build
    fi
    
    # 原有逻辑
    ...
}
```

## 支持

如有问题，请检查：

1. 环境要求是否满足（JDK 17+, Maven 3.6+, Docker）
2. 磁盘空间是否充足（建议至少 20GB）
3. 网络连接是否正常（需要访问 Maven 仓库和 Docker Hub）
4. 日志文件中的错误信息（`/tmp/build_*.log`）

更多问题请参考原始文档或联系开发团队。
