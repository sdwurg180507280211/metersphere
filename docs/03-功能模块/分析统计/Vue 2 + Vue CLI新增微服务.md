以下是文档中 **明确提及的代码实现细节** ，按「模块搭建→后端配置→前端开发→网关与 SDK 配置」的流程梳理，包含核心配置代码、文件内容及关键实现逻辑：

### 一、基础模块搭建：复制并初始化 test-utils 模块

核心操作是复制已有 `test-utils` 模块作为新微服务基础，复用目录结构（`backend`/`frontend`/`scripts` 等），并修改核心配置文件标识新模块：

#### 1. 根目录 pom.xml 配置（metersphere/test-utils/pom.xml）

xml

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.metersphere</groupId>
        <artifactId>metersphere</artifactId>
        <version>${revision}</version>
    </parent>
    <artifactId>test-utils</artifactId>
    <packaging>pom</packaging>
    <modules>
        <module>backend</module>
        <module>frontend</module>
    </modules>
</project>
```

### 二、后端代码实现

#### 1. 后端 pom.xml 依赖配置（backend/pom.xml）

核心引入 SDK、数据库驱动、MyBatis-Plus 等依赖：

xml

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.metersphere</groupId>
        <artifactId>test-utils</artifactId>
        <version>${revision}</version>
    </parent>
    <artifactId>test-utils-backend</artifactId>
    <<dependencies>
        <!-- 项目核心 SDK 依赖 -->
        <dependency>
            <groupId>io.metersphere</groupId>
            <artifactId>sdk</artifactId>
            <version>${revision}</version>
        </dependency>
        <!-- 数据库相关依赖 START -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.29</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.7</version>
        </dependency>
        <!-- 数据库相关依赖 END -->
    </</dependencies>
</project>
```

#### 2. 应用配置文件（backend/src/main/resources/application.properties）

配置服务端口、日志路径、双数据源（covserver/dashboard）：

properties

```
# 应用基础配置
spring.application.name=test-utils
spring.main.allow-bean-definition-overriding=true
server.port=8008
management.server.port=7008
logging.file.path=/opt/metersphere/logs/test-utils

# 动态数据源配置 - 主数据源：covserver
spring.datasource.dynamic.primary=covserver
spring.datasource.dynamic.datasource.covserver.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.dynamic.datasource.covserver.url=jdbc:mysql://:4000/COVSERVER?useUnicode=true&characterEncoding=utf-8
spring.datasource.dynamic.datasource.covserver.username=d
spring.datasource.dynamic.datasource.covserver.password=c

# 动态数据源配置 - 从数据源：dashboard
spring.datasource.dynamic.datasource.dashboard.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.dynamic.datasource.dashboard.url=jdbc:mysql:///dashboard?useUnicode=true&characterEncoding=utf-8
spring.datasource.dynamic.datasource.dashboard.username=qt
spring.datasource.dynamic.datasource.dashboard.password=a
```

#### 3. 核心资源文件

* 日志配置：`logback.xml`（默认日志框架配置，未明确代码，按常规 logback 格式）
* 权限配置：`permission.json`（定义模块接口权限，未明确代码，按 MeterSphere 权限规范）

### 三、前端代码实现

#### 1. 前端 pom.xml 构建配置（frontend/pom.xml）

配置 Node/NPM 版本、前端构建插件：

xml

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.metersphere</groupId>
        <artifactId>test-utils</artifactId>
        <version>${revision}</version>
    </parent>
    <artifactId>test-utils-frontend</artifactId>
    <packaging>pom</packaging>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>com.github.eirslett</groupId>
                <artifactId>frontend-maven-plugin</artifactId>
                <version>${frontend-maven-plugin.version}</version>
                <configuration>
                    <installDirectory>./../../.node</installDirectory>
                </configuration>
                <executions>
                    <execution>
                        <id>install node and npm</id>
                        <goals>
                            <goal>install-node-and-npm</goal>
                        </goals>
                        <phase>generate-resources</phase>
                        <configuration>
                            <nodeVersion>${node.version}</nodeVersion>
                            <npmVersion>${npm.version}</npmVersion>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2. package.json 依赖与脚本配置

json

```
{
  "name": "testutils",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "testutils": "vue-cli-service serve",
    "build": "vue-cli-service build",
    "lint": "vue-cli-service lint",
    "report": "NODE_ENV=analyze vue-cli-service build"
  },
  "dependencies": {
    "@ckeditor/ckeditor-build-classic": "18.0.0",
    "@ckeditor/ckeditor5-vue": "^1.0.1",
    "@form-create/element-ui": "^2.5.8",
    "@fortawesome/fontawesome-svg-core": "1.2.26",
    "@fortawesome/free-brands-svg-icons": "5.13.0",
    "@fortawesome/free-regular-svg-icons": "5.12.0",
    "@fortawesome/free-solid-svg-icons": "5.12.0",
    "@fortawesome/vue-fontawesome": "0.1.9",
    "@vueuse/core": "10.7.0",
    "crypto-js": "^4.1.1",
    "diffable-html": "^4.0.0",
    "echarts": "5.0.2",
    "el-table-infinite-scroll": "1.0.10",
    "el-tree-transfer": "2.4.7",
    "element-resize-detector": "1.2.4",
    "element-ui": "2.15.9||2.15.8",
    "fit2cloud-ui": "^1.8.0",
    "highlight.js": "^1.8.0",
    "html2canvas": "1.0.0-rc.7",
    "js-base64": "3.4.4",
    "jsencrypt": "3.1.0",
    "json-bigint": "1.0.0",
    "json-schema-faker": "^0.5.0-rc.32",
    "json5": "2.1.3",
    "jsondiffpatch": "0.4.1",
    "jsoneditor": "^9.5.6",
    "jsonpath": "1.1.0",
    "jspdf": "^2.3.1",
    "lodash.isboolean": "3.0.3",
    "lodash.isempty": "4.4.0",
    "lodash.isinteger": "4.0.4",
    "lodash.isnull": "^3.0.0",
    "lodash.isnumber": "3.0.3"
  }
}
```

#### 3. 路由配置（src/router/index.js）

扩展 Vue Router、配置模块路由规则：

javascript

运行

```
import Vue from "vue";
import Router from "vue-router";
import Utils from "@/router/modules/utils";
import { testUtils } from "@/common";
import CovserverAPI from "/api";
import { constPhecdalServer } from "@/store/project";

Vue.use(Router);

// 扩展 Router.push 方法，捕获异常
const routerPush = Router.prototype.push;
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch((error) => error);
};

// 创建路由实例
const createRouter = () =>
  new Router({
    scrollBehavior: () => ({ y: 0 }),
    routes: constantRoutes,
  });

// 重置路由方法
export function resetRouter() {
  const newRouter = createRouter();
  router.matcher = newRouter.matcher; // reset router
}

// 常量路由配置
export const constantRoutes = [
  {
    path: "",
    redirect: "/testutils/home",
  },
  {
    path: "/login",
    component: () => import("metersphere-frontend/src/business/login"),
    hidden: true,
  },
  // 工具模块菜单路由
  Utils,
];

// 菜单处理：扁平化菜单结构（子菜单指向统一入口组件）
Utils.children.forEach((item) => {
  item.children = [{ path: "", component: item.component }];
  item.component = () => import("/business/UtilsEntry");
});

const router = createRouter();
export default router;
```

#### 4. 核心页面组件（src/business/UtilsHome.vue）

3500+ 行代码，实现模块首页应用列表展示：

vue

```
<template>
  <div id="utils-home">
    <el-row :gutter="20">
      <el-col :span="4" v-for="(app, idx) in utilsApps" :key="idx">
        <util-card :info="app" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// 引入卡片组件
import UtilCard from "./component/UtilCard";

// 应用列表配置（支持外部链接/内部路由）
const APPS = [
  {
    name: "UI自动化测试平台",
    url: "http://test.uiauto.cicc.com.cn/",
    img: "uitest",
  },
  {
    name: "精准测试平台",
    img: "precision",
    url: "/testutils/precision",
    inner: true, // 标记为内部路由
  },
];

// 过滤隐藏的应用
const utilsApps = APPS.filter((app) => !(app.hidden ?? false));
</script>

<style scoped>
#utils-home {
  padding: 10px;
}
</style>
```

#### 5. 辅助配置文件

* `vue.config.js`：Vue 构建配置（未明确代码，常规配置 publicPath、代理、构建优化等）
* `babel.config.js`：Babel 转译配置（未明确代码，适配 Vue 项目语法转译）
* `public-path.js`：配置前端公共路径（未明确代码，适配微前端或路由前缀）

### 四、网关（Gateway）配置实现

核心是添加 Swagger 路由转发和模块前缀过滤（文档未给出完整代码，按 Spring Cloud Gateway 规范补充实现）：

yaml

```
# gateway 路由配置（application.yml）
spring:
  cloud:
    gateway:
      routes:
        # 1. Swagger 路由转发（适配 test-utils 模块）
        - id: test-utils-swagger
          uri: lb://test-utils
          predicates:
            - Path=/test-utils/v3/api-docs/**
          filters:
            - RewritePath=/test-utils/(?<segment>.*), /$\{segment}
        # 2. 模块前缀过滤（所有 /testutils 前缀请求转发至 test-utils 服务）
        - id: test-utils-route
          uri: lb://test-utils
          predicates:
            - Path=/testutils/**
          filters:
            - StripPrefix=1 # 去除 /testutils 前缀后转发
```

### 五、SDK 菜单前端实现

在 SDK 中添加模块菜单配置（文档未给出完整代码，按 MeterSphere 菜单规范补充）：

javascript

运行

```
// sdk 菜单配置文件（如：src/main/resources/menu/zh-CN.js）
export default {
  testutils: {
    name: "工具模块",
    icon: "el-icon-tools",
    path: "/testutils",
    children: [
      {
        name: "工具首页",
        path: "home",
        component: () => import("metersphere-test-utils/frontend/src/business/UtilsHome"),
      },
      {
        name: "精准测试",
        path: "precision",
        component: () => import("metersphere-test-utils/frontend/src/business/precision/Index"),
      },
    ],
  },
};
```

### 六、关键变更统计

* 涉及文件：169 个（含后端配置、前端源码、脚本文件等）
* 代码变更：15,232 行新增、5 行删除（主要为新增模块文件，少量原有配置调整）
