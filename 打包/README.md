# MeterSphere 模块化构建系统

一个强大、灵活、易用的 MeterSphere 项目构建工具，支持模块化构建、并行处理、每服务独立版本等特性。

## ✨ 特性

- 🎯 **模块化构建** - 可以选择性构建单个或多个模块
- ⚡ **并行构建** - 支持多核并行构建，大幅提升构建速度
- 🔄 **增量构建** - 支持跳过初始化步骤，加快重复构建
- 🏷️ **独立版本** - 每个服务拥有独立镜像版本，支持自动递增
- 🛠️ **灵活配置** - 支持命令行参数、环境变量、配置文件多种配置方式
- 📊 **详细日志** - 彩色输出，清晰的进度提示和错误信息
- 🔍 **环境检查** - 自动检查构建环境和依赖
- 💾 **可选导出** - 可以只构建镜像而不导出 tar 文件
- 🎨 **友好界面** - 提供 Makefile 接口，命令简洁易记

## 📋 环境要求

- JDK 17+
- Maven 3.6+
- Docker
- Bash 4.0+
- 至少 8GB 可用内存（并行构建建议 16GB）
- 至少 20GB 可用磁盘空间

## 🚀 快速开始

### 方式 1: 使用脚本（推荐）

```bash
# 1. 赋予执行权限
chmod +x metersphere-build.sh

# 2. 查看帮助
./metersphere-build.sh -h

# 3. 构建所有模块（需设置版本环境变量，或由控制面板传入）
SERVICE_VERSION_EUREKA=v2.10.26.01-lts \
SERVICE_VERSION_GATEWAY=v2.10.26.01-lts \
SERVICE_VERSION_API_TEST=v2.10.26.05-lts \
./metersphere-build.sh -a

# 4. 并行构建（更快）
SERVICE_VERSION_EUREKA=v2.10.26.01-lts \
SERVICE_VERSION_GATEWAY=v2.10.26.01-lts \
SERVICE_VERSION_API_TEST=v2.10.26.05-lts \
./metersphere-build.sh -a -p -j 4
```

### 方式 2: 使用 Makefile（更简单）

```bash
# 1. 查看所有命令
make help

# 2. 并行构建所有模块
make parallel

# 3. 构建单个模块（需指定版本）
make api-test SERVICE_VERSION_API_TEST=v2.10.26.05-lts
```

## 📖 使用示例

### 基础用法

```bash
# 构建所有模块（串行）
./metersphere-build.sh -a

# 并行构建所有模块（4 个并行任务）
./metersphere-build.sh -a -p -j 4

# 构建指定模块
./metersphere-build.sh gateway eureka test-track

# 快速构建（跳过初始化）
./metersphere-build.sh -s gateway api-test
```

### 每服务独立版本

每个模块通过 `SERVICE_VERSION_<MOD>` 环境变量指定版本，MOD 名大写，`-` 替换为 `_`：

```bash
# 为不同服务指定不同版本
SERVICE_VERSION_EUREKA=v2.10.26.01-lts \
SERVICE_VERSION_GATEWAY=v2.10.26.02-lts \
SERVICE_VERSION_API_TEST=v2.10.26.05-lts \
./metersphere-build.sh api-test eureka gateway

# 使用配置文件批量设置版本
cp build.config.example .build.config
vim .build.config   # 编辑 SERVICE_VERSION_* 变量
source .build.config && ./metersphere-build.sh -a
```

版本命名规则：
- 格式：`v2.10.26.01-lts`（主版本.序号-后缀）
- 构建成功后序号自动 +1（`01` → `02`，保留前导零）
- 版本持久化在控制面板 `config.json` 中，可提交到 Git

### 使用 Makefile

```bash
# 并行构建（推荐）
make parallel

# 开发模式（跳过初始化，不导出 tar）
make dev

# 构建核心模块
make core

# 构建单个模块
make gateway SERVICE_VERSION_GATEWAY=v2.10.26.02-lts
make api-test SERVICE_VERSION_API_TEST=v2.10.26.05-lts

# 指定并行数
make parallel JOBS=8
```

### 高级用法

```bash
# 指定输出路径
./metersphere-build.sh -o /data/packages/ms-20241224.tar -a

# 只构建镜像不导出（开发测试）
./metersphere-build.sh -a -b

# 不使用 Docker 构建缓存
./metersphere-build.sh -a --no-cache

# 使用 Maven 额外选项
MAVEN_OPTS="-Dmaven.test.skip=true" ./metersphere-build.sh -a

# 使用配置文件
source .build.config && ./metersphere-build.sh -a
```

## 📂 文件说明

```
.
├── metersphere-build.sh    # 主构建脚本
├── Makefile                # Make 接口
├── build.config.example    # 配置文件模板（含 SERVICE_VERSION_* 示例）
├── BUILD_GUIDE.md          # 详细使用指南
└── README.md               # 本文件
```

## ⚙️ 配置方式

### 1. 命令行参数（优先级最高）

```bash
./metersphere-build.sh \
  -o /tmp/ms.tar \
  -p -j 4 \
  --no-cache \
  gateway eureka
```

### 2. 环境变量

```bash
export SERVICE_VERSION_API_TEST=v2.10.26.05-lts
export SERVICE_VERSION_EUREKA=v2.10.26.01-lts
export PARALLEL_BUILD=true
export MAX_JOBS=4
export NO_CACHE=true
./metersphere-build.sh -a
```

### 3. 配置文件

```bash
# 创建配置文件
cp build.config.example .build.config

# 编辑配置（包含 SERVICE_VERSION_* 变量）
vim .build.config

# 使用配置
source .build.config && ./metersphere-build.sh -a

# 或使用 Makefile
make init-config
make edit-config
make all
```

## 🎯 常用场景

### 场景 1: 首次构建

```bash
# 完整构建，导出 tar 包
source .build.config && ./metersphere-build.sh -a

# 或使用 Make
make all
```

### 场景 2: 日常开发

```bash
# 快速构建修改的模块（跳过初始化，不导出）
./metersphere-build.sh -s -b gateway api-test

# 或使用 Make
make dev
```

### 场景 3: 生产发布

```bash
# 确保版本正确后构建
source .build.config && ./metersphere-build.sh -a

# 或使用 Make
make parallel
```

### 场景 4: 性能优化

```bash
# 多核机器并行构建
./metersphere-build.sh -a -p -j 8

# 或使用 Make
make parallel JOBS=8
```

## 📊 性能对比

| 构建方式 | 模块数 | 耗时 | 适用场景 |
|---------|--------|------|---------|
| 串行构建 | 9 | ~45分钟 | 内存<8GB |
| 并行构建 (j=2) | 9 | ~25分钟 | 内存>=8GB |
| 并行构建 (j=4) | 9 | ~15分钟 | 内存>=16GB |
| 单模块 | 1 | ~5分钟 | 开发调试 |
| 增量构建 | 3 | ~8分钟 | 部分修改 |

*实际时间取决于硬件配置和网络环境*

## 🔧 可用模块

| 模块名称 | 说明 | 构建命令 | 版本环境变量 |
|---------|------|---------|------------|
| eureka | 注册中心 | `make eureka` | `SERVICE_VERSION_EUREKA` |
| gateway | 网关服务 | `make gateway` | `SERVICE_VERSION_GATEWAY` |
| system-setting | 系统设置 | `make system-setting` | `SERVICE_VERSION_SYSTEM_SETTING` |
| project-management | 项目管理 | `make project-management` | `SERVICE_VERSION_PROJECT_MANAGEMENT` |
| performance-test | 性能测试 | `make performance-test` | `SERVICE_VERSION_PERFORMANCE_TEST` |
| api-test | API测试 | `make api-test` | `SERVICE_VERSION_API_TEST` |
| test-track | 测试跟踪 | `make test-track` | `SERVICE_VERSION_TEST_TRACK` |
| report-stat | 报告统计 | `make report-stat` | `SERVICE_VERSION_REPORT_STAT` |
| workstation | 工作台 | `make workstation` | `SERVICE_VERSION_WORKSTATION` |

> **注意**: `workflow-service` 和 `analytics-stat` 不在构建范围内。

## 🐛 故障排查

### Maven 编译失败

```bash
# 清理缓存重试
make clean
./metersphere-build.sh -a
```

### Docker 构建失败

```bash
# 清理 Docker 缓存
make clean-docker

# 或不使用缓存重新构建
./metersphere-build.sh -a --no-cache
```

### 并行构建失败

```bash
# 查看详细日志（并行构建日志在临时目录中）
ls ${TMPDIR:-/tmp}/ms-build.*

# 切换到串行构建
./metersphere-build.sh -a
```

### 版本未设置

```bash
# 检查哪些模块缺少版本
./metersphere-build.sh -l

# 设置缺失的版本
export SERVICE_VERSION_API_TEST=v2.10.26.05-lts
./metersphere-build.sh api-test
```

### 磁盘空间不足

```bash
# 检查空间
df -h

# 清理资源
make clean-all
docker system prune -a
```

更多问题请查看 [BUILD_GUIDE.md](BUILD_GUIDE.md)

## 📚 文档

- [BUILD_GUIDE.md](BUILD_GUIDE.md) - 详细使用指南
- [build.config.example](build.config.example) - 配置文件模板

## 💡 提示和技巧

### 1. 加速构建

```bash
# 使用本地 Maven 仓库镜像
# 编辑 ~/.m2/settings.xml 添加阿里云镜像

# 使用 Docker 镜像加速
# 配置 Docker daemon.json 使用国内镜像
```

### 2. 监控构建进度

```bash
# 方式 1: 使用 watch
make watch

# 方式 2: 查看日志
make logs

# 方式 3: 查看状态
make status
```

### 3. 自动化脚本

```bash
# 创建每日构建脚本
cat > daily-build.sh << 'EOF'
#!/bin/bash
cd /path/to/project
source .build.config
./metersphere-build.sh -a -p -j 4
# 上传到文件服务器
scp $HOME/Desktop/metersphere-*.tar fileserver:/releases/
EOF

# 添加到 crontab
crontab -e
# 每天凌晨 2 点执行
0 2 * * * /path/to/daily-build.sh
```

### 4. 验证构建结果

```bash
# 查看构建的镜像
make show-images

# 或
docker images | grep registry.fit2cloud.com/north

# 测试镜像
docker run --rm registry.fit2cloud.com/north/gateway:v2.10.26.01-lts --version
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📝 更新日志

### v1.1.0 (2025-04)
- 🏷️ 支持每服务独立镜像版本（SERVICE_VERSION_*）
- 🔄 版本自动递增，持久化到 config.json
- 🛡️ 严格错误处理（set -euo pipefail）
- 🧹 并行构建使用 mktemp 隔离临时文件
- 🚫 移除全局 IMAGE_VERSION，改为每服务独立版本
- 📦 新增 --no-cache、NO_CACHE、MAVEN_OPTS 支持

### v1.0.0 (2024-12-24)
- ✨ 初始版本
- 🎯 支持模块化构建
- ⚡ 支持并行构建
- 🛠️ 完善的配置系统
- 📊 友好的日志输出

## 📄 许可证

MIT License
