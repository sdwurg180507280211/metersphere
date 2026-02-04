# Jenkins CI/CD 落地指南（阿里云 ECS + MeterSphere）

## 项目目标

在阿里云 ECS 上搭建 Jenkins CI/CD，实现 MeterSphere 的自动化构建与部署：

```
代码提交 → Jenkins 构建 → Docker 镜像推送 → 测试环境自动部署
```

## 环境概览

| 组件 | 版本/配置 |
|------|-----------|
| ECS OS | Ubuntu 24.04 LTS |
| Jenkins | apt 安装，端口 8080 |
| JDK | 17（路径：`/opt/jdk-17`） |
| Maven | apt 安装（路径：`/opt/apache-maven-3.8.3`） |
| 镜像仓库 | 阿里云 ACR 个人版（北京） |
| 部署目录 | `/opt/metersphere` |

## 文档索引

按顺序阅读完成首次搭建，或按需跳转：

| 文档 | 内容 | 何时阅读 |
|------|------|----------|
| [01-ECS环境准备](./01-ECS环境准备.md) | JDK、Maven、Docker、用户配置 | 首次搭建 |
| [02-Jenkins配置](./02-Jenkins配置.md) | 安装、插件、凭证、Job 创建 | 首次搭建 |
| [03-Jenkinsfile说明](./03-Jenkinsfile说明.md) | 流水线脚本的变量与阶段 | 二次开发 |
| [04-故障排查](./04-故障排查.md) | 常见报错与修复方案 | 遇到问题时 |
| [05-概念说明](./05-概念说明.md) | 用户职责、Job 类型等背景知识 | 新人入门 |

## 快速检查清单

首次搭建完成后，用此清单验证：

```bash
# 1. Jenkins 服务运行中
curl -I http://127.0.0.1:8080/login  # 应返回 200/302/403

# 2. JDK 路径正确
/opt/jdk-17/bin/java -version

# 3. Maven 路径正确
/opt/apache-maven-3.8.3/bin/mvn -version

# 4. Docker 可用（jenkins 用户）
sudo -u jenkins docker version

# 5. buildx 支持多架构
docker buildx inspect --bootstrap | grep -i platform
```

## 关键凭证 ID（Jenkinsfile 依赖）

在 Jenkins 中必须创建以下凭证，ID 必须一致：

| 凭证 ID | 类型 | 用途 |
|---------|------|------|
| `aliyun-acr-personal` | Username with password | ACR 镜像推送 |
| `metersphere-deploy-key` | SSH Username with private key | 部署服务器连接 |
| `test-server-ip` | Secret text | 测试服务器 IP |
| `gitee-token` | Username with password | Gitee 代码拉取 |
| `metersphere-maven` | Maven settings.xml | Maven 私服配置 |

## 镜像仓库地址

```bash
# VPC 内网（推荐，速度快）
crpi-tysjadjbz5afeai8-vpc.cn-beijing.personal.cr.aliyuncs.com/metersphere-edy

# 公网
crpi-tysjadjbz5afeai8.cn-beijing.personal.cr.aliyuncs.com/metersphere-edy
```
