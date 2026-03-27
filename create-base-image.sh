#!/bin/bash

# 创建基础镜像脚本
set -e

echo "=== 创建 MeterSphere 基础镜像 ==="

# 你的阿里云仓库地址
REGISTRY="crpi-tysjadjbz5afeai8.cn-beijing.personal.cr.aliyuncs.com"
BASE_IMAGE_NAME="metersphere/alpine-openjdk17-jre"
TAG="latest"

# 使用你现有的镜像作为源
SOURCE_IMAGE="registry.fit2cloud.com/north/test-track:v2.10.23.02-lts"

echo "1. 检查 Docker 登录状态..."
if ! docker info > /dev/null 2>&1; then
    echo "错误: Docker 未运行或无权限访问"
    exit 1
fi

echo "2. 检查源镜像是否存在..."
if ! docker image inspect ${SOURCE_IMAGE} > /dev/null 2>&1; then
    echo "错误: 源镜像 ${SOURCE_IMAGE} 不存在"
    echo "请先确保该镜像在本地可用"
    exit 1
fi

echo "3. 从现有镜像 ${SOURCE_IMAGE} 创建基础镜像..."

# 创建临时 Dockerfile
cat > temp-base.Dockerfile << EOF
# 基于现有的 MeterSphere 镜像
FROM ${SOURCE_IMAGE}

# 清理应用相关的内容，只保留 Java 运行环境
RUN rm -rf /app/* 2>/dev/null || true

# 重置环境变量为基础镜像状态
ENV JAVA_CLASSPATH=""
ENV JAVA_MAIN_CLASS=""
ENV MS_VERSION=""

# 保持 Java 环境变量
ENV JAVA_OPTIONS="-Dfile.encoding=utf-8 -Djava.awt.headless=true --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED"

WORKDIR /app

CMD ["/deployments/run-java.sh"]
EOF

echo "4. 构建基础镜像..."
docker build -f temp-base.Dockerfile -t ${REGISTRY}/${BASE_IMAGE_NAME}:${TAG} .

echo "5. 推送到你的仓库..."
echo "推送镜像: ${REGISTRY}/${BASE_IMAGE_NAME}:${TAG}"
docker push ${REGISTRY}/${BASE_IMAGE_NAME}:${TAG}

echo "6. 清理临时文件..."
rm -f temp-base.Dockerfile

echo "=== 基础镜像创建完成 ==="
echo "镜像地址: ${REGISTRY}/${BASE_IMAGE_NAME}:${TAG}"
echo ""
echo "现在你可以使用以下命令测试构建："
echo "docker build -f system-setting/Dockerfile -t test-system-setting ."