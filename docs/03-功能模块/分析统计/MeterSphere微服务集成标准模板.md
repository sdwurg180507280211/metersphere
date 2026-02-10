# MeterSphere 微服务集成标准模板

## 概述

本文档是 MeterSphere 新增微服务模块的**标准集成模板**，基于 `analytics-stat` 模块的实践经验总结。

**适用场景**：
- 新增业务模块（如报表、工作流、数据分析等）
- 独立微服务集成到主应用
- qiankun 微前端子应用开发

**核心原则**：
- 遵循 MeterSphere 统一的技术栈和规范
- 复用 metersphere-frontend 共享库
- 保持与现有模块的一致性

**参考模块**：
- `analytics-stat`：最新的标准实现
- `report-stat`：成熟的参考实现
- `performance-test`：完整的功能实现

---

## 一、新增微服务模块清单（Quick Checklist）

新增一个微服务模块需要完成以下配置：

### 1.1 后端配置

| 序号 | 配置项 | 文件位置 | 必须 | 说明 |
| ---- | ------ | -------- | ---- | ---- |
| 1 | **pom.xml** - 模块定义 | `{module}/backend/pom.xml` | ✅ | 定义依赖、插件、打包方式 |
| 2 | **Application 启动类** | `{module}/backend/src/main/java/.../Application.java` | ✅ | Spring Boot 启动入口 |
| 3 | **application.properties** | `{module}/backend/src/main/resources/application.properties` | ✅ | 服务名、端口、数据库等配置 |
| 4 | **WebMvcConfig** | `{module}/backend/src/main/java/.../config/WebMvcConfig.java` | ✅ | 将前端打包文件映射到 HTTP 路径 |
| 5 | **Gateway SessionFilter** | `framework/gateway/.../SessionFilter.java` | ✅ | Gateway 路由转发规则 |
| 6 | **Eureka 注册** | 依赖 `sdk` 模块即可 | ✅ | 服务注册与发现 |
| 7 | **Flyway 迁移脚本** | `{module}/backend/src/main/resources/db/migration/` | **推荐** | 数据库版本管理（表结构、菜单配置等） |
| 8 | **菜单配置 SQL** | `{module}/backend/src/main/resources/db/migration/V2__add_{module}_module.sql` | **推荐** | 在 system_parameter 表中添加模块菜单配置 |
| 9 | **远程调用Controller** | `{module}/backend/.../controller/remote/SystemSettingController.java` | ✅ | 转发系统级公共API到system-setting服务 |
| 10 | **Dockerfile** | `{module}/backend/Dockerfile` | 可选 | Docker 镜像构建 |
| 11 | **根 pom.xml** | `pom.xml` | ✅ | 在 `<modules>` 中添加新模块 |

### 1.2 前端配置

| 序号 | 配置项 | 文件位置 | 必须 | 说明 |
| ---- | ------ | -------- | ---- | ---- |
| 1 | **package.json** | `{module}/frontend/package.json` | ✅ | npm 依赖、构建脚本、ESLint 配置 |
| 2 | **main.js** | `{module}/frontend/src/main.js` | ✅ | 子应用入口、生命周期钩子、共享库集成 |
| 3 | **public-path.js** | `{module}/frontend/src/public-path.js` | ✅ | qiankun 动态资源路径 |
| 4 | **vue.config.js** | `{module}/frontend/vue.config.js` | ✅ | Webpack 配置、UMD 打包、代码分割 |
| 5 | **router/index.js** | `{module}/frontend/src/router/index.js` | ✅ | Vue Router 配置、路由前缀 |
| 6 | **App.vue** | `{module}/frontend/src/App.vue` | ✅ | 应用根组件 |
| 7 | **i18n** | `{module}/frontend/src/i18n/` | 推荐 | 多语言支持（zh-CN、zh-TW、en-US） |
| 8 | **store** | `{module}/frontend/src/store/` | 推荐 | Pinia 状态管理 |
| 9 | **pom.xml** | `{module}/frontend/pom.xml` | ✅ | frontend-maven-plugin 配置 |

### 1.3 主应用配置

| 序号 | 配置项 | 文件位置 | 说明 |
| ---- | ------ | -------- | ---- |
| 1 | **micro-app.js** | `framework/sdk-parent/frontend/src/micro-app.js` | 自动从 Eureka 获取，**无需手动配置** |
| 2 | **主应用路由** | `framework/gateway/src/.../router/` | 如需在主菜单显示，需添加路由配置 |
| 3 | **权限配置** | 数据库 `user_group_permission` 表 | 如需权限控制，需在数据库中配置模块权限 |

**关键点**：
- 子应用注册是**自动化**的，主应用通过 Eureka 服务发现自动注册所有微服务
- 服务名（如 `analytics-stat`）会自动成为路由前缀（如 `#/analytics-stat`）
- 前端开发端口 = 后端端口 - 4000（如后端 8009 → 前端 4009）

---

## 二、后端模块创建详解

### 2.1 目录结构模板

```
{module}/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/io/metersphere/              # ⚠️ 包名必须是 io.metersphere
│   │   │   │   ├── {Module}Application.java      # 启动类
│   │   │   │   ├── config/
│   │   │   │   │   └── WebMvcConfig.java         # 静态资源映射
│   │   │   │   ├── controller/                   # REST API
│   │   │   │   ├── service/                      # 业务逻辑
│   │   │   │   ├── base/mapper/                  # MyBatis Mapper
│   │   │   │   └── dto/                          # 数据传输对象
│   │   │   └── resources/
│   │   │       ├── application.properties        # 配置文件
│   │   │       ├── db/migration/                 # Flyway 迁移脚本
│   │   │       └── static/                       # 前端打包文件（Maven 自动复制）
│   │   └── test/                                 # 单元测试
│   ├── pom.xml                                   # Maven 配置
│   └── Dockerfile                                # Docker 镜像（可选）
├── frontend/                                     # 前端模块（见下节）
└── pom.xml                                       # 父 POM
```

**⚠️ 重要：包名规范**

- **包名必须是 `io.metersphere`**，不能是 `io.metersphere.{module}`
- 原因：Spring Boot 的 `@SpringBootApplication` 默认只扫描当前包及其子包
- SDK 的公共服务（如 `BaseSystemSettingService`）在 `io.metersphere.service` 包中
- 如果使用 `io.metersphere.{module}` 包名，会导致 SDK 的 Bean 无法注入
- 参考：analytics-stat 模块已完成包名规范化重构（2026-02-10）

### 2.2 端口规划建议

| 模块 | 服务端口 | 管理端口 | 前端开发端口 |
| ---- | -------- | -------- | -------------- |
| system-setting | 8001 | 7001 | 4001 |
| project-management | 8002 | 7002 | 4002 |
| test-track | 8003 | 7003 | 4003 |
| api-test | 8004 | 7004 | 4004 |
| performance-test | 8005 | 7005 | 4005 |
| report-stat | 8006 | 7006 | 4006 |
| workstation | 8007 | 7007 | 4007 |
| analytics-stat | 8009 | 7009 | 4009 |
| **新模块** | **80XX** | **70XX** | **40XX** |

---

## 三、配置文件模板

### 3.1 父 POM（`{module}/pom.xml`）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>io.metersphere</groupId>
        <artifactId>metersphere</artifactId>
        <version>${revision}</version>
    </parent>

    <modelVersion>4.0.0</modelVersion>
    <artifactId>{module}-parent</artifactId>
    <packaging>pom</packaging>

    <modules>
        <module>backend</module>
        <module>frontend</module>
    </modules>
</project>
```

### 3.2 后端 POM（`{module}/backend/pom.xml`）

参考 `analytics-stat/backend/pom.xml`，关键配置：
- 依赖 `sdk` 模块（包含 Eureka、Spring Boot、MyBatis 等）
- `maven-resources-plugin` 复制前端打包文件到 `static` 目录
- `spring-boot-maven-plugin` 打包成可执行 JAR

### 3.3 Application.java 启动类

```java
package io.metersphere;  // ⚠️ 必须是 io.metersphere，不能是 io.metersphere.{module}

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * {模块名}微服务启动类
 * 
 * ⚠️ 重要：包名规范
 * - 包名必须是 io.metersphere（不能是 io.metersphere.{module}）
 * - 原因：@SpringBootApplication 默认只扫描当前包及其子包
 * - SDK 的公共服务在 io.metersphere.service 包中
 * - 如果包名是 io.metersphere.{module}，会导致 SDK 的 Bean 无法注入
 * 
 * 参考：analytics-stat 模块已完成包名规范化重构（2026-02-10）
 */
@SpringBootApplication(exclude = {
    // 根据需要排除不需要的自动配置
})
@EnableDiscoveryClient  // 启用 Eureka 服务注册
public class {Module}Application {
    public static void main(String[] args) {
        SpringApplication.run({Module}Application.class, args);
    }
}
```

**关键点说明**：

1. **包名必须是 `io.metersphere`**
   - ❌ 错误：`package io.metersphere.analyticsstat;`
   - ✅ 正确：`package io.metersphere;`

2. **为什么不能用子包名？**
   - `@SpringBootApplication` 包含 `@ComponentScan`
   - 默认扫描范围：当前包及其子包
   - 如果启动类在 `io.metersphere.analyticsstat`，只会扫描 `io.metersphere.analyticsstat.*`
   - SDK 的 Bean 在 `io.metersphere.service`、`io.metersphere.commons` 等包中
   - 结果：SDK 的 Bean 无法被扫描到，导致注入失败

3. **不需要手动配置 `@ComponentScan`**
   - 使用标准包名 `io.metersphere` 后，Spring Boot 会自动扫描所有子包
   - 包括：`io.metersphere.service`、`io.metersphere.commons`、`io.metersphere.controller` 等

4. **其他模块的实现**
   - `api-test`：`package io.metersphere;`
   - `test-track`：`package io.metersphere;`
   - `system-setting`：`package io.metersphere;`
   - `analytics-stat`：`package io.metersphere;`（已重构）

### 3.4 application.properties

```properties
# 服务名（必须）- 会成为路由前缀和模块key
# 推荐使用简化命名: analytics-stat → analytics, report-stat → report
spring.application.name={module}

# 服务端口（必须）
server.port=80XX
management.server.port=70XX

# Eureka 配置（SDK 已提供默认配置）
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

# 数据库配置（如需独立数据库）
spring.datasource.url=jdbc:mysql://localhost:3306/metersphere_{module}?...
spring.datasource.username=root
spring.datasource.password=Password123@mysql

# Flyway 配置（如需数据库迁移）
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.table=flyway_schema_history_{module}
```

**重要说明**：
- `spring.application.name` 的值会自动成为:
  1. Eureka注册的服务名(自动转大写)
  2. Gateway `/services` 接口返回的 `serviceId`
  3. 前端路由前缀 `/#/{module}`
  4. 数据库模块key `metersphere.module.{module}`
- 为避免命名不一致导致菜单不显示,建议使用简化命名

### 3.5 WebMvcConfig.java

```java
package io.metersphere.{module}.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /{module}/ 路径映射到 classpath:/static/
        registry.addResourceHandler("/{module}/**")
                .addResourceLocations("classpath:/static/");
    }
}
```

### 3.6 Gateway SessionFilter 配置

**修改文件**：`framework/gateway/src/main/java/io/metersphere/gateway/filter/SessionFilter.java`

```java
private static final String[] PREFIX = new String[]{
    "/setting", "/project", "/api", "/performance", 
    "/track", "/workstation", "/ui", "/report", 
    "/{module}"  // 新增模块前缀
};
```

### 3.7 根 pom.xml 添加模块

**修改文件**：`pom.xml`

```xml
<modules>
    <module>framework</module>
    <module>api-test</module>
    <!-- ... 其他模块 ... -->
    <module>{module}</module>  <!-- 新增 -->
</modules>
```

---

### 3.8 远程调用Controller（重要）

**创建文件**：`{module}/backend/src/main/java/io/metersphere/{module}/controller/remote/SystemSettingController.java`

```java
package io.metersphere.{module}.controller.remote;

import io.metersphere.service.remote.BaseSystemSettingService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 系统设置远程调用Controller
 * 
 * 功能：将系统级公共API请求转发到 system-setting 服务
 * 
 * 转发路径：
 * - /system/* → system-setting 服务
 * - /module/* → system-setting 服务  
 * - /license/* → system-setting 服务
 * 
 * 原理：
 * 在qiankun微前端架构中,子应用的所有请求都会带上模块前缀(如 /{module})
 * 例如: /{module}/system/theme
 * 
 * Gateway会将请求路由到 {module} 服务
 * 本Controller接收请求后,通过 BaseSystemSettingService 转发到 system-setting 服务
 * 
 * 参考：project-management/backend/.../controller/remote/SystemSettingController.java
 */
@RestController
@RequestMapping(path = {
        "/system",
        "/module",
        "/license"
})
public class SystemSettingController {
    
    @Resource
    BaseSystemSettingService baseSystemSettingService;

    /**
     * 转发POST请求到 system-setting 服务
     */
    @PostMapping("/**")
    public Object post(HttpServletRequest request, @RequestBody Object param) {
        return baseSystemSettingService.post(request, param);
    }

    /**
     * 转发GET请求到 system-setting 服务
     */
    @GetMapping("/**")
    public Object get(HttpServletRequest request) {
        return baseSystemSettingService.get(request);
    }
}
```

**为什么需要这个Controller?**

在qiankun微前端架构中:
1. 子应用的 `baseURL = '/{module}'`,所有请求都会加上模块前缀
2. 例如主框架的 Layout 组件调用 `/system/theme`,在子应用中会变成 `/{module}/system/theme`
3. Gateway 将 `/{module}/system/theme` 路由到 `{module}` 服务
4. 如果 `{module}` 服务没有这个Controller,会返回404
5. 有了这个Controller,请求会被转发到 `system-setting` 服务,返回正确结果

**其他模块的实现**:
- `project-management`: 有 `SystemSettingController`
- `api-test`: 有 `SystemSettingController`
- `test-track`: 有 `SystemSettingController`
- `report-stat`: 有 `SystemSettingController`
- `analytics-stat`: 有 `SystemSettingController`

**如果缺少这个Controller会怎样?**
- 浏览器控制台报错: `GET /{module}/system/theme 404 (Not Found)`
- 浏览器控制台报错: `GET /{module}/module/list 404 (Not Found)`
- 页面样式异常,功能不可用

---

### 3.9 菜单配置 SQL（重要）

**创建文件**：`{module}/backend/src/main/resources/db/migration/V2__add_{module}_module.sql`

```sql
-- {模块中文名}模块菜单配置
-- 创建时间: YYYY-MM-DD
-- 说明: 在 system_parameter 表中添加模块的启用配置，用于控制左侧菜单显示

SET SESSION innodb_lock_wait_timeout = 7200;

-- 插入模块配置
-- param_key: metersphere.module.{moduleKey} (必须与服务名保持一致)
-- param_value: ENABLE (启用) / DISABLE (禁用)
-- type: text
-- sort: 1
-- 
-- 重要: moduleKey 必须与 spring.application.name 保持一致
-- 例如: spring.application.name=analytics → param_key=metersphere.module.analytics
INSERT INTO system_parameter (param_key, param_value, type, sort)
VALUES ('metersphere.module.{moduleKey}', 'ENABLE', 'text', 1)
ON DUPLICATE KEY UPDATE param_value = 'ENABLE';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
```

**命名规则**：
- `param_key` 格式：`metersphere.module.{moduleKey}`
- **推荐使用简化命名**（与服务名保持一致）：
  - `analytics-stat` → `analytics`（推荐）
  - `report-stat` → `report`
  - `api-test` → `api`
  - `test-track` → `track`

**作用**：
- 控制左侧菜单是否显示该模块
- `ENABLE`：显示菜单
- `DISABLE`：隐藏菜单

**参考示例**：

| 模块 | 服务名 | param_key | param_value | 说明 |
| ---- | ------ | --------- | ----------- | ---- |
| api-test | api | metersphere.module.api | ENABLE | 简化命名 |
| performance-test | performance | metersphere.module.performance | ENABLE | 简化命名 |
| test-track | track | metersphere.module.track | ENABLE | 简化命名 |
| report-stat | report | metersphere.module.report | ENABLE | 简化命名 |
| workstation | workstation | metersphere.module.workstation | ENABLE | 完整命名 |
| analytics-stat | analytics | metersphere.module.analytics | ENABLE | 简化命名(推荐) |

**重要说明**：
- 模块key必须与服务名保持一致,否则左侧菜单不显示
- 服务名通过Eureka注册后,Gateway的`/services`接口返回`serviceId`
- 前端`micro-app.js`将`serviceId`存入`sessionStorage.micro_apps`
- 前端`/module/list`接口读取数据库配置,存入`localStorage.modules`
- `AsideMenus.vue`的`check(key)`方法要求两个key完全一致才显示菜单

---

## 四、前端模块创建详解

### 4.1 目录结构模板

```
{module}/frontend/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── api/                          # API 接口定义
│   ├── business/                     # 业务组件
│   │   └── home/
│   │       ├── {Module}Home.vue
│   │       └── components/
│   ├── i18n/                         # 国际化
│   │   ├── lang/
│   │   │   ├── zh-CN.js
│   │   │   ├── zh-TW.js
│   │   │   └── en-US.js
│   │   └── index.js
│   ├── router/
│   │   ├── modules/
│   │   │   └── {module}.js
│   │   └── index.js
│   ├── store/                        # 状态管理
│   │   └── index.js
│   ├── views/                        # 页面组件（可选）
│   ├── App.vue
│   ├── main.js
│   └── public-path.js
├── .npmrc
├── babel.config.js
├── package.json
├── pom.xml
└── vue.config.js
```

### 4.2 package.json 关键依赖

```json
{
  "dependencies": {
    "vue": "^2.7.3",
    "vue-router": "^3.1.3",
    "element-ui": "^2.15.7",
    "pinia": "^2.0.14",
    "axios": "^1.6.0",
    "echarts": "^5.0.2",
    "vue-i18n": "^8.15.3",
    "metersphere-frontend": "file:../../framework/sdk-parent/frontend",
    "fit2cloud-ui": "^1.8.0",
    "vue-shepherd": "^0.3.0"
  }
}
```

### 4.3 main.js 入口文件（完整版）

参考 `analytics-stat/frontend/src/main.js`，关键点：
- 导入 `metersphere-frontend` 所有共享资源
- 实现 qiankun 四个生命周期钩子（bootstrap、mount、unmount、update）
- 支持独立运行（开发环境）
- 兼容 vue-devtools

### 4.4 public-path.js

```javascript
import { getApps } from "metersphere-frontend/src/api/apps";

// qiankun 动态 publicPath
if (window.__POWERED_BY_QIANKUN__) {
  __webpack_public_path__ = window.__INJECTED_PUBLIC_PATH_BY_QIANKUN__;
}

// 独立运行时，获取微服务列表（用于跨模块跳转）
if (!window.__POWERED_BY_QIANKUN__) {
  getApps().then(res => {
    let modules = {}, microPorts = {};
    res.data.forEach(svc => {
      modules[svc.serviceId] = true;
      microPorts[svc.serviceId] = svc.port;
    })
    sessionStorage.setItem("micro_apps", JSON.stringify(modules));
    sessionStorage.setItem("micro_ports", JSON.stringify(microPorts));
  })
}
```

### 4.5 vue.config.js 关键配置

参考 `analytics-stat/frontend/vue.config.js`，关键点：
- `publicPath: '/'`（必须）
- `pages` 配置（qiankun 必须）
- `libraryTarget: 'umd'`（qiankun 必须）
- `splitChunks` 代码分割优化
- `svg-sprite-loader` SVG 精灵图支持

### 4.6 router/index.js

```javascript
import Vue from 'vue';
import VueRouter from 'vue-router';
import Layout from 'metersphere-frontend/src/business/app-layout';
import Login from 'metersphere-frontend/src/business/login';

Vue.use(VueRouter);

const routes = [
  {
    path: '/login',
    component: Login,
  },
  {
    path: '/{module}',
    component: Layout,  // 使用统一布局
    redirect: '/{module}/home',
    children: [
      {
        path: 'home',
        name: '{Module}Home',
        component: () => import('@/business/home/{Module}Home.vue'),
      },
    ],
  },
];

export default function createRouter() {
  return new VueRouter({
    mode: 'hash',
    base: process.env.BASE_URL,
    routes,
  });
}
```

---

## 五、完整创建流程

### 5.1 快速创建脚本（推荐）

```bash
#!/bin/bash
MODULE_NAME=$1
MODULE_PORT=$2

# 创建目录结构
mkdir -p ${MODULE_NAME}/{backend,frontend}
mkdir -p ${MODULE_NAME}/backend/src/main/{java/io/metersphere/${MODULE_NAME},resources}
mkdir -p ${MODULE_NAME}/frontend/src/{api,business,i18n,router,store}

# 复制模板文件（从 analytics-stat 复制）
cp -r analytics-stat/backend/pom.xml ${MODULE_NAME}/backend/
cp -r analytics-stat/frontend/* ${MODULE_NAME}/frontend/

# 替换模块名和端口
find ${MODULE_NAME} -type f -exec sed -i '' "s/analytics-stat/${MODULE_NAME}/g" {} \;
find ${MODULE_NAME} -type f -exec sed -i '' "s/8009/${MODULE_PORT}/g" {} \;

echo "Module ${MODULE_NAME} created successfully!"
```

### 5.2 手动创建步骤

1. **创建目录结构**
2. **复制模板文件**（从 `analytics-stat` 复制）
3. **全局替换**模块名和端口
4. **修改根 pom.xml**（添加模块）
5. **修改 Gateway SessionFilter**（添加路由前缀）
6. **创建菜单配置 SQL**（V2__add_{module}_module.sql）
7. **实现业务逻辑**（Controller、Service、Mapper、页面组件）
8. **启动验证**

### 5.3 验证步骤

```bash
# 1. 启动 Eureka
cd framework/eureka && mvn spring-boot:run

# 2. 启动 Gateway
cd framework/gateway && mvn spring-boot:run

# 3. 启动新模块后端（会自动执行 Flyway 迁移）
cd {module}/backend && mvn spring-boot:run

# 4. 验证菜单配置是否插入成功
mysql -h localhost -u root -p'Password123@mysql' -e \
  "SELECT * FROM metersphere.system_parameter WHERE param_key LIKE 'metersphere.module.%';"

# 5. 启动新模块前端
cd {module}/frontend && npm run serve

# 6. 访问 http://localhost:8080/#/{module}
# 7. 检查左侧菜单是否显示新模块
```

**验证清单**：
- ✅ Eureka 控制台显示服务已注册（http://localhost:8761）
- ✅ 数据库 system_parameter 表中有模块配置记录
- ✅ 主应用左侧菜单显示新模块
- ✅ 点击菜单可以正常跳转到新模块页面
- ✅ 新模块页面样式正常，功能可用

---

## 六、常见问题排查

| 问题 | 可能原因 | 解决方案 |
| ---- | -------- | -------- |
| 子应用加载失败 | Eureka 未注册 | 检查 http://localhost:8761 |
| 静态资源 404 | SessionFilter 未配置 | 在 PREFIX 数组中添加模块前缀 |
| **系统API 404** | **缺少远程调用Controller** | **添加 SystemSettingController** |
| **SDK Bean 无法注入** | **包名不规范** | **包名必须是 `io.metersphere`** |
| 路由不匹配 | 路由前缀与服务名不一致 | 检查 router/index.js 和 application.properties |
| 样式丢失 | 未导入 metersphere-frontend 样式 | 检查 main.js 中的样式导入 |
| 跨域错误 | devServer.headers 未配置 | 检查 vue.config.js 中的 CORS 配置 |
| **左侧菜单不显示** | **未插入 system_parameter 配置** | **检查 Flyway 迁移脚本是否执行成功** |
| **菜单配置未生效** | **param_key 命名错误** | **检查命名是否与服务名一致** |

### 6.1 菜单不显示问题详细排查

**症状**：新模块启动成功，但左侧菜单不显示

**排查步骤**：

1. **检查数据库配置**：
```sql
-- 查询所有模块配置
SELECT * FROM metersphere.system_parameter 
WHERE param_key LIKE 'metersphere.module.%';

-- 检查新模块配置是否存在
SELECT * FROM metersphere.system_parameter 
WHERE param_key = 'metersphere.module.{module}';
```

2. **检查 Flyway 执行状态**：
```sql
-- 查看 Flyway 迁移历史
SELECT * FROM metersphere.flyway_schema_history 
ORDER BY installed_on DESC LIMIT 10;

-- 检查新模块的迁移脚本是否执行
SELECT * FROM metersphere.flyway_schema_history 
WHERE script LIKE '%{module}%';
```

3. **手动插入配置**（如果 Flyway 未执行）：
```sql
INSERT INTO metersphere.system_parameter (param_key, param_value, type, sort)
VALUES ('metersphere.module.{module}', 'ENABLE', 'text', 1)
ON DUPLICATE KEY UPDATE param_value = 'ENABLE';
```

4. **重启服务**：
```bash
# 重启 Gateway（菜单配置由 Gateway 读取）
cd framework/gateway && mvn spring-boot:run
```

5. **清除浏览器缓存**：
- 按 `Ctrl+Shift+Delete`（Windows）或 `Cmd+Shift+Delete`（Mac）
- 清除缓存和 Cookie
- 重新登录

### 6.2 系统API 404问题详细排查

**症状**：浏览器控制台报错 `GET /{module}/system/theme 404 (Not Found)`

**原因**：缺少远程调用Controller,无法将系统级公共API转发到system-setting服务

**排查步骤**：

1. **检查Controller是否存在**：
```bash
# 查看是否有 SystemSettingController
ls -la {module}/backend/src/main/java/io/metersphere/{module}/controller/remote/
```

2. **检查其他模块的实现**：
```bash
# 查看 project-management 的实现
cat project-management/backend/src/main/java/io/metersphere/controller/remote/SystemSettingController.java
```

3. **添加Controller**（参考3.8节）

4. **重启服务**：
```bash
cd {module}/backend && mvn spring-boot:run
```

5. **验证**：
- 打开浏览器开发者工具 → Network
- 访问模块页面
- 检查 `/{module}/system/theme` 请求是否返回200
- 检查 `/{module}/module/list` 请求是否返回200

---

## 七、最佳实践

### 7.1 命名规范

| 类型 | 规范 | 示例 | 说明 |
| ---- | ---- | ---- | ---- |
| 模块名 | kebab-case | `analytics-stat` | 文件夹名称 |
| **Java 包名** | **必须是 `io.metersphere`** | `io.metersphere` | **⚠️ 不能是 `io.metersphere.{module}`** |
| Java 类名 | PascalCase | `AnalyticsStatApplication` | 类名 |
| Vue 组件 | PascalCase | `AnalyticsStatHome.vue` | 组件名 |
| 路由 path | kebab-case | `/analytics-stat/home` | 路由路径 |

**⚠️ 重要：Java 包名规范**

- **包名必须是 `io.metersphere`**，不能是 `io.metersphere.{module}`
- 原因：Spring Boot 的 `@SpringBootApplication` 默认只扫描当前包及其子包
- SDK 的公共服务（如 `BaseSystemSettingService`）在 `io.metersphere.service` 包中
- 如果使用 `io.metersphere.{module}` 包名，会导致 SDK 的 Bean 无法注入
- 参考：analytics-stat 模块已完成包名规范化重构（2026-02-10）

**其他模块的包名对比**：

| 模块 | 包名 | 是否规范 |
| ---- | ---- | -------- |
| api-test | `io.metersphere` | ✅ 规范 |
| test-track | `io.metersphere` | ✅ 规范 |
| system-setting | `io.metersphere` | ✅ 规范 |
| analytics-stat | `io.metersphere` | ✅ 规范（已重构） |

### 7.2 代码复用

**优先使用 metersphere-frontend 共享库**：
- 样式：`metersphere-frontend/src/styles/`
- 组件：`metersphere-frontend/src/components/`
- 工具：`metersphere-frontend/src/utils/`
- API：`metersphere-frontend/src/api/`

### 7.3 性能优化

- 配置 `splitChunks` 代码分割
- 路由懒加载
- SVG 精灵图
- CSS 提取

---

## 八、总结

本文档提供了 MeterSphere 新增微服务模块的**完整标准模板**，使用本模板可以快速创建符合 MeterSphere 规范的新微服务模块，确保与现有生态的无缝集成。

**参考模块**：
- `analytics-stat`：最新的标准实现
- `report-stat`：成熟的参考实现
- `performance-test`：完整的功能实现
