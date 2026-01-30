# Jenkinsfile 说明

本文档解释 `Jenkinsfile.enhanced` 的关键配置，便于二次开发。

## 1. 关键变量

| 变量 | 当前值 | 说明 |
|------|--------|------|
| `IMAGE_PREFIX` | `crpi-tysjadjbz5afeai8-vpc.cn-beijing.personal.cr.aliyuncs.com/metersphere-edy` | 镜像名前缀 |
| `ACR_REGISTRY` | `crpi-tysjadjbz5afeai8-vpc.cn-beijing.personal.cr.aliyuncs.com` | docker login 地址 |
| `JAVA_HOME` | `/opt/jdk-17` | JDK 路径 |
| `DOCKER_CONFIG_DIR` | `/var/lib/jenkins/.docker` | docker login 凭证存放 |

## 2. 凭证依赖

Jenkinsfile 中使用的凭证 ID（必须在 Jenkins 中提前创建）：

```groovy
// ACR 登录
withCredentials([usernamePassword(
    credentialsId: 'aliyun-acr-personal',
    usernameVariable: 'ACR_USER',
    passwordVariable: 'ACR_PASS'
)])

// 部署 SSH
sshagent(['metersphere-deploy-key'])

// 服务器 IP
environment {
    TEST_SERVER = credentials('test-server-ip')
    PROD_SERVER = credentials('prod-server-ip')
}

// Maven settings
configFileProvider([configFile(fileId: 'metersphere-maven', targetLocation: 'settings.xml')])
```

## 3. 流水线阶段

```
┌─────────────┐
│  Checkout   │ 拉取代码
└──────┬──────┘
       ▼
┌─────────────┐
│ Build/Test  │ Maven 编译 + 前端构建
└──────┬──────┘
       ▼
┌─────────────┐
│ Docker Push │ 构建镜像并推送 ACR
└──────┬──────┘
       ▼
┌─────────────┐
│Deploy Test  │ SSH 到测试机执行 docker-compose
└─────────────┘
```

### 3.1 Build/Test 阶段

```groovy
stage('Build/Test') {
    steps {
        configFileProvider([configFile(fileId: 'metersphere-maven', targetLocation: 'settings.xml')]) {
            sh '''
                export JAVA_HOME=/opt/jdk-17
                export PATH=/opt/apache-maven-3.8.3/bin:$PATH
                
                # 前端构建优化
                export NODE_OPTIONS="--max-old-space-size=4096"
                export npm_config_registry=https://registry.npmmirror.com
                
                mvn clean package -DskipTests --settings ./settings.xml
            '''
        }
    }
}
```

### 3.2 Docker build & push 阶段

```groovy
stage('Docker build & push') {
    steps {
        withCredentials([usernamePassword(
            credentialsId: 'aliyun-acr-personal',
            usernameVariable: 'ACR_USER',
            passwordVariable: 'ACR_PASS'
        )]) {
            sh '''
                # 登录 ACR
                echo "$ACR_PASS" | docker login --username "$ACR_USER" --password-stdin ${ACR_REGISTRY}
                
                # 多架构构建并推送
                docker buildx build \
                    --platform linux/amd64,linux/arm64 \
                    --push \
                    -t ${IMAGE_PREFIX}/eureka:${BRANCH_NAME} \
                    ./framework/eureka
            '''
        }
    }
}
```

### 3.3 Deploy to Test 阶段

```groovy
stage('Deploy to Test') {
    when {
        anyOf {
            branch 'develop'
            branch 'develop-*'
        }
    }
    steps {
        sshagent(['metersphere-deploy-key']) {
            sh '''
                ssh deploy@${TEST_SERVER} << 'EOF'
                cd /opt/metersphere
                
                # 更新镜像 tag
                sed -i "s/MS_IMAGE_TAG=.*/MS_IMAGE_TAG=${BRANCH_NAME}/" install.conf
                
                # 获取 compose 文件列表
                COMPOSE_FILES=$(cat compose_files)
                
                # 拉取并启动
                docker-compose $COMPOSE_FILES pull
                docker-compose $COMPOSE_FILES up -d --remove-orphans
                EOF
            '''
        }
    }
}
```

## 4. 分支匹配规则

```groovy
when {
    anyOf {
        branch 'develop'      // 精确匹配 develop
        branch 'develop-*'    // 匹配 develop-v2.10.26 等
    }
}
```

## 5. 修改指南

### 5.1 更换镜像仓库

修改以下变量：

```groovy
environment {
    IMAGE_PREFIX = '新的镜像前缀'
    ACR_REGISTRY = '新的 Registry 地址'
}
```

同时在 Jenkins 中更新 `aliyun-acr-personal` 凭证的用户名密码。

### 5.2 添加新服务镜像

在 Docker build 阶段添加：

```groovy
docker buildx build \
    --platform linux/amd64,linux/arm64 \
    --push \
    -t ${IMAGE_PREFIX}/新服务名:${BRANCH_NAME} \
    ./新服务路径
```

### 5.3 修改部署方式

当前部署方式是读取 `compose_files` 执行 docker-compose。如需改为脚本：

```groovy
// 当前方式
docker-compose $COMPOSE_FILES pull
docker-compose $COMPOSE_FILES up -d

// 改为脚本方式
./scripts/deploy.sh ${BRANCH_NAME}
```

## 6. Docker build context 规则

不同模块的 Dockerfile COPY 路径不同，build context 也不同：

| 模块类型 | build context | 示例 |
|----------|---------------|------|
| 业务模块（api-test, test-track） | 仓库根目录 | `docker build -f test-track/Dockerfile .` |
| 框架模块（eureka, gateway） | 模块目录 | `docker build ./framework/gateway` |

**原因**：业务模块 Dockerfile 的 COPY 带模块前缀（如 `COPY test-track/backend/...`），框架模块则是相对路径。
