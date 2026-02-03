# analytics-stat 模块与其他模块差异对比分析

## 概述

本文档记录 `analytics-stat` 微服务与 MeterSphere 其他前端模块（如 `report-stat`、`test-track`、`system-setting`）在 qiankun 微前端集成方面的差异，并解释每个差异的作用和影响。新增微服务模块的完整参考指南。

---

## 零、新增微服务模块清单（Quick Checklist）

如果要新增一个微服务模块，需要完成以下配置：

### 后端配置

| 序号 | 配置项                                          | 文件位置                                                        | 必须 |
| ---- | ----------------------------------------------- | --------------------------------------------------------------- | ---- |
| 1    | **pom.xml** - 模块定义                    | `{module}/backend/pom.xml`                                    | ✅   |
| 2    | **Application 启动类**                    | `{module}/backend/src/main/java/.../Application.java`         | ✅   |
| 3    | **application.properties** - 端口、服务名 | `{module}/backend/src/main/resources/application.properties`  | ✅   |
| 4    | **WebMvcConfig** - 静态资源映射           | `{module}/backend/src/main/java/.../config/WebMvcConfig.java` | ✅   |
| 5    | **Gateway SessionFilter** - 添加模块前缀  | `framework/gateway/.../SessionFilter.java`                    | ✅   |
| 6    | **Eureka 注册** - SDK 依赖自动注册        | 依赖 `sdk` 模块即可                                           | ✅   |
| 7    | **Flyway 迁移脚本**                       | `{module}/backend/src/main/resources/db/migration/`           | 可选 |

### 前端配置

| 序号 | 配置项                                     | 文件位置                                  | 必须 |
| ---- | ------------------------------------------ | ----------------------------------------- | ---- |
| 1    | **package.json** - 依赖和脚本        | `{module}/frontend/package.json`        | ✅   |
| 2    | **main.js** - qiankun 生命周期       | `{module}/frontend/src/main.js`         | ✅   |
| 3    | **public-path.js** - 动态 publicPath | `{module}/frontend/src/public-path.js`  | ✅   |
| 4    | **vue.config.js** - UMD 输出格式     | `{module}/frontend/vue.config.js`       | ✅   |
| 5    | **router/index.js** - 路由配置       | `{module}/frontend/src/router/index.js` | ✅   |
| 6    | **App.vue** - 根组件                 | `{module}/frontend/src/App.vue`         | ✅   |

### 主应用配置

| 序号 | 配置项                              | 文件位置                                           | 说明                             |
| ---- | ----------------------------------- | -------------------------------------------------- | -------------------------------- |
| 1    | **micro-app.js** - 子应用注册 | `framework/sdk-parent/frontend/src/micro-app.js` | 自动从 Eureka 获取，无需手动配置 |
| 2    | **主应用路由** - 菜单入口     | `framework/gateway/src/.../router/`              | 如需在主菜单显示                 |

---

## 一、main.js 差异对比

### 1.1 已实现的功能（与其他模块一致）

| 功能                | 状态      | 说明                        |
| ------------------- | --------- | --------------------------- |
| public-path 导入    | ✅ 已实现 | qiankun 动态设置 publicPath |
| Vue + Element UI    | ✅ 已实现 | 基础 UI 框架                |
| Pinia 状态管理      | ✅ 已实现 | 应用状态管理                |
| bootstrap 生命周期  | ✅ 已实现 | 子应用初始化                |
| mount 生命周期      | ✅ 已实现 | 子应用挂载                  |
| unmount 生命周期    | ✅ 已实现 | 子应用卸载                  |
| onGlobalStateChange | ✅ 已实现 | 监听全局状态变化            |
| setGlobalState      | ✅ 已实现 | 设置全局状态                |
| update 生命周期     | ✅ 已实现 | 主应用更新 props            |
| EventBus 事件总线   | ✅ 已实现 | 与主应用通信                |

### 1.2 已完成的集成（2026-02-03 更新）

| 功能                                | analytics-stat | 其他模块 | 作用说明                                                                            |
| ----------------------------------- | -------------- | -------- | ----------------------------------------------------------------------------------- |
| **metersphere-frontend 样式** | ✅ 已导入      | ✅ 导入  | 统一的全局样式，包括主题色、字体、间距等                                            |
| **i18n 国际化**               | ✅ 已集成      | ✅ 集成  | 多语言支持（中文简体、繁体、英文）                                                  |
| **icons 图标库**              | ✅ 已使用      | ✅ 使用  | SVG 图标组件，可使用 `<svg-icon>` 组件                                            |
| **svg 组件**                  | ✅ 已使用      | ✅ 使用  | SVG 精灵图支持                                                                      |
| **plugins 插件**              | ✅ 已使用      | ✅ 使用  | 全局方法（如 `$alert`、`$confirm`、`$success`）                                 |
| **directives 指令**           | ✅ 已使用      | ✅ 使用  | 自定义指令（如 `v-permission`）                                                   |
| **filters 过滤器**            | ✅ 已使用      | ✅ 使用  | 全局过滤器（如日期格式化）                                                          |
| **permission 路由守卫**       | ✅ 已导入      | ✅ 导入  | 路由权限控制                                                                        |
| **chart 图表**                | ✅ 已使用      | ✅ 使用  | ECharts 封装组件                                                                    |
| **VueShepherd 新手引导**      | ✅ 已使用      | ✅ 使用  | 新手引导功能                                                                        |
| **gotoCancel/gotoNext**       | ✅ 已挂载      | ✅ 挂载  | 引导步骤控制方法                                                                    |
| **vue-devtools 兼容**         | ✅ 已处理      | ✅ 处理  | 开发环境调试支持                                                                    |
| **Layout 布局组件**           | ✅ 已使用      | ✅ 使用  | 使用 metersphere-frontend 的统一布局                                                |
| **login 路由**                | ✅ 已配置      | ✅ 配置  | 登录页面路由                                                                        |
| **store 状态管理**            | ✅ 已配置      | ✅ 配置  | Pinia store 集成 metersphere-frontend 的 user 模块                                  |

### 1.3 代码对比

**analytics-stat/frontend/src/main.js（已完成集成）：**

```javascript
import "./public-path";
import Vue from "vue";
import "metersphere-frontend/src/styles/index.scss";  // 统一样式
import ElementUI from "element-ui";
import App from "./App.vue";
import i18n from "./i18n";  // 国际化
import router from "./router";
import { createPinia, PiniaVuePlugin } from "pinia";
import PersistedState from "pinia-plugin-persistedstate";

// metersphere-frontend 公共资源
import icons from "metersphere-frontend/src/icons";
import svg from "metersphere-frontend/src/components/svg";
import plugins from "metersphere-frontend/src/plugins";
import directives from "metersphere-frontend/src/directive";
import filters from "metersphere-frontend/src/filters";
import "metersphere-frontend/src/router/permission";
import chart from "metersphere-frontend/src/chart";
import VueShepherd from "vue-shepherd";
import "metersphere-frontend/src/assets/shepherd/shepherd-theme.css";
import { gotoCancel, gotoNext } from "metersphere-frontend/src/utils";

Vue.use(ElementUI, { i18n: (key, value) => i18n.t(key, value) });
Vue.use(svg);
Vue.use(icons);
Vue.use(plugins);
Vue.use(directives);
Vue.use(filters);
Vue.use(PiniaVuePlugin);
Vue.use(chart);
Vue.use(VueShepherd);

Vue.prototype.gotoCancel = gotoCancel;
Vue.prototype.gotoNext = gotoNext;
```

**与 report-stat 完全一致**，已完成所有 metersphere-frontend 共享库的集成。

---

## 二、vue.config.js 差异对比

### 2.1 已实现的配置（2026-02-03 更新）

| 配置项                      | 状态      | 说明                                 |
| --------------------------- | --------- | ------------------------------------ |
| publicPath                  | ✅`/`   | 与其他模块一致                       |
| UMD 输出格式                | ✅ 已配置 | qiankun 必须                         |
| CSS 提取                    | ✅ 已配置 | 独立 CSS 文件                        |
| devServer 跨域              | ✅ 已配置 | 开发环境支持                         |
| **svg-sprite-loader** | ✅ 已配置 | 将 SVG 图标打包为精灵图              |
| **vue-i18n alias**    | ✅ 已配置 | 解决 vue-i18n 重复打包问题           |

### 2.2 可选优化配置

| 配置项                 | analytics-stat | 其他模块 | 作用说明                                       |
| ---------------------- | -------------- | -------- | ---------------------------------------------- |
| **splitChunks 优化** | ❌ 未配置      | ✅ 配置  | 代码分割优化。不配置则打包体积较大，首屏加载慢 |

### 2.3 代码对比

**analytics-stat（当前）：**

```javascript
module.exports = defineConfig({
  publicPath: '/',
  configureWebpack: {
    output: {
      library: `${name}-[name]`,
      libraryTarget: 'umd',
      chunkLoadingGlobal: `webpackJsonp_${name}`,
    },
  },
});
```

**report-stat（参考）：**

```javascript
module.exports = defineConfig({
  publicPath: '/',
  configureWebpack: {
    resolve: {
      alias: {
        'vue-i18n': resolve('node_modules/vue-i18n'),  // 避免重复打包
      },
    },
    optimization: {
      splitChunks: {  // 代码分割
        cacheGroups: {
          'chunk-vendors': { ... },
          echarts: { ... },
        },
      },
    },
  },
  chainWebpack: (config) => {
    // SVG 精灵图配置
    config.module.rule('svg').exclude.add(...);
    config.module.rule('icons').test(/\.svg$/).use('svg-sprite-loader')...;
  },
});
```

---

## 三、router/index.js 差异对比

### 3.1 已实现的功能（2026-02-03 更新）

| 功能                      | 状态                      | 说明                                     |
| ------------------------- | ------------------------- | ---------------------------------------- |
| Router.push 错误捕获      | ✅ 已实现                 | 防止路由重复导航报错                     |
| createRouter 工厂函数     | ✅ 已实现                 | 支持路由重置                             |
| 路由路径前缀              | ✅`/analytics-stat/xxx` | 与主应用路由匹配                         |
| **Layout 布局组件** | ✅ 已使用                 | 使用 metersphere-frontend 的统一布局     |
| **login 路由**      | ✅ 已配置                 | 登录页面路由                             |
| **二级菜单处理**    | ✅ 已实现                 | Layout -> AnalyticsStat -> 页面组件      |
| **路由模块化**      | ✅ 已实现                 | 使用 router/modules/analytics.js 模块化  |

---

## 四、package.json 差异对比

### 4.1 已添加的依赖（2026-02-03 更新）

| 依赖              | 作用           | 状态   |
| ----------------- | -------------- | ------ |
| fit2cloud-ui      | UI 组件库      | ✅ 已添加 |
| svg-sprite-loader | SVG 精灵图打包 | ✅ 已添加 |
| vue-shepherd      | 新手引导       | ✅ 已添加 |
| shepherd.js       | 新手引导核心   | ✅ 已添加 |

---

## 五、后端配置差异

### 5.1 Gateway SessionFilter

**已修改**：在 `PREFIX` 数组中添加了 `/analytics-stat`

```java
// framework/gateway/src/main/java/io/metersphere/gateway/filter/SessionFilter.java
private static final String[] PREFIX = new String[]{
    "/setting", "/project", "/api", "/performance", 
    "/track", "/workstation", "/ui", "/report", 
    "/analytics-stat"  // 新增
};
```

**作用**：Gateway 根据 CSS/JS 文件名中的模块标识，将静态资源请求转发到对应的微服务。

### 5.2 WebMvcConfig

**已配置**：静态资源映射

```java
// analytics-stat/backend/.../WebMvcConfig.java
registry.addResourceHandler("/analytics-stat/**")
        .addResourceLocations("classpath:/static/");
```

**作用**：将 `/analytics-stat/` 路径的请求映射到 `classpath:/static/` 目录。

### 5.3 application.properties

**已配置**：服务名和端口

```properties
# analytics-stat/backend/src/main/resources/application.properties
spring.application.name=analytics-stat
server.port=8009
management.server.port=7009
```

**端口规划**：

| 模块                     | 服务端口       | 管理端口       | 前端开发端口   |
| ------------------------ | -------------- | -------------- | -------------- |
| system-setting           | 8001           | 7001           | 4001           |
| project-management       | 8002           | 7002           | 4002           |
| test-track               | 8003           | 7003           | 4003           |
| api-test                 | 8004           | 7004           | 4004           |
| performance-test         | 8005           | 7005           | 4005           |
| report-stat              | 8006           | 7006           | 4006           |
| workstation              | 8007           | 7007           | 4007           |
| **analytics-stat** | **8009** | **7009** | **4009** |

### 5.4 Eureka 服务注册

**自动注册**：依赖 `sdk` 模块后，Eureka 客户端自动配置

```xml
<!-- analytics-stat/backend/pom.xml -->
<dependency>
    <groupId>io.metersphere</groupId>
    <artifactId>sdk</artifactId>
    <version>${revision}</version>
</dependency>
```

SDK 中已包含 Eureka 客户端依赖：

```xml
<!-- framework/sdk-parent/sdk/pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**验证方式**：访问 Eureka 控制台 `http://localhost:8761/` 查看服务是否注册成功。

### 5.5 主应用子应用注册（自动）

**无需手动配置**：主应用通过 Eureka 自动发现子应用

```javascript
// framework/sdk-parent/frontend/src/micro-app.js
getApps().then(res => {
    let apps = [];
    res.data.forEach(svc => {
        let name = svc.serviceId;  // 从 Eureka 获取服务名
        apps.push({
            name,
            entry: '//127.0.0.1:' + (svc.port - 4000),  // 计算前端端口
            container: '#micro-app',
            activeRule: getActiveRule('#/' + name),  // 路由规则：#/{serviceName}
            props: { eventBus }
        });
    });
    registerMicroApps(apps);  // 注册所有子应用
    start();
});
```

**关键点**：

- 服务名 `analytics-stat` 会自动成为路由前缀 `#/analytics-stat`
- 前端端口 = 后端端口 - 4000（如 8009 → 4009）
- 生产环境会替换为 `window.location.host + "/" + app.name`

---

## 六、影响分析

### 6.1 当前状态可正常工作的功能

- ✅ qiankun 微前端加载
- ✅ 路由跳转
- ✅ 页面渲染
- ✅ Element UI 组件
- ✅ 与主应用通信（EventBus）
- ✅ 全局状态同步

### 6.2 当前状态可能存在的问题

| 问题              | 原因                             | 影响程度           |
| ----------------- | -------------------------------- | ------------------ |
| 样式不一致        | 未导入 metersphere-frontend 样式 | 中                 |
| 无法切换语言      | 未集成 i18n                      | 低（如果只需中文） |
| 无法使用 svg-icon | 未配置 svg-sprite-loader         | 低                 |
| 无权限控制        | 未导入 permission 路由守卫       | 中                 |
| 打包体积大        | 未配置 splitChunks               | 低                 |

---

## 六、目录结构（2026-02-03 更新）

### 6.1 目录对比

| 目录              | analytics-stat | 其他模块 | 作用说明                                                      |
| ----------------- | -------------- | -------- | ------------------------------------------------------------- |
| `src/api/`      | ❌ 无          | ✅ 有    | API 接口定义。不创建则 API 调用分散在各组件中                 |
| `src/i18n/`     | ✅ 有          | ✅ 有    | 国际化语言文件（zh-CN、zh-TW、en-US）                         |
| `src/store/`    | ✅ 有          | ✅ 有    | Pinia 状态管理，集成 metersphere-frontend 的 user 模块        |
| `src/template/` | ❌ 无          | ✅ 有    | 页面模板（如分享页面）。不创建则无法支持独立页面              |
| `src/business/` | ✅ 有          | ✅ 有    | 业务组件                                                      |
| `src/router/`   | ✅ 有          | ✅ 有    | 路由配置（含 modules 子目录）                                 |
| `src/views/`    | ✅ 有          | ❌ 无    | 页面组件（analytics-stat 独有，其他模块放在 business 中）     |

### 6.2 analytics-stat 目录结构（已完成）

```
analytics-stat/frontend/src/
├── business/
│   ├── AnalyticsStat.vue              # 二级布局组件
│   └── head/
│       └── AnalyticsStatHeaderMenus.vue  # 二级导航
├── i18n/                              # 国际化（新增）
│   ├── lang/
│   │   ├── zh-CN.js                   # 中文简体
│   │   ├── zh-TW.js                   # 中文繁体
│   │   └── en-US.js                   # 英文
│   └── index.js                       # i18n 配置
├── router/
│   ├── modules/
│   │   └── analytics.js               # 路由模块（使用 Layout）
│   └── index.js                       # 路由配置（含 login 路由）
├── store/                             # 状态管理（新增）
│   └── index.js                       # Pinia store
├── views/
│   ├── Dashboard.vue                  # 数据概览
│   ├── SqlConsole.vue                 # SQL查询台
│   └── DataDictionary.vue             # 数据字典
├── App.vue                            # 根组件
├── main.js                            # 入口文件（已完成集成）
└── public-path.js                     # qiankun publicPath
```

### 6.3 其他模块目录结构（以 report-stat 为例）

```
report-stat/frontend/src/
├── api/                           # API 接口定义
├── business/                      # 业务组件
├── i18n/                          # 国际化
│   ├── lang/                      # 语言文件
│   └── index.js                   # i18n 配置
├── router/
│   ├── modules/                   # 路由模块
│   └── index.js
├── store/                         # 状态管理
├── template/                      # 页面模板
├── App.vue
├── main.js
└── public-path.js
```

---

## 七、public-path.js 差异

### 7.1 代码对比

**analytics-stat（简化版）：**

```javascript
if (window.__POWERED_BY_QIANKUN__) {
  __webpack_public_path__ = window.__INJECTED_PUBLIC_PATH_BY_QIANKUN__;
}
```

**其他模块（完整版）：**

```javascript
import {getApps} from "metersphere-frontend/src/api/apps";

if (window.__POWERED_BY_QIANKUN__) {
  __webpack_public_path__ = window.__INJECTED_PUBLIC_PATH_BY_QIANKUN__;
}

// 独立运行时，获取微服务列表
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

### 7.2 差异说明

| 功能               | analytics-stat | 其他模块 | 作用                                         |
| ------------------ | -------------- | -------- | -------------------------------------------- |
| qiankun publicPath | ✅ 有          | ✅ 有    | 动态设置资源路径                             |
| 获取微服务列表     | ❌ 无          | ✅ 有    | 独立运行时获取其他微服务信息，用于跨模块跳转 |

---

## 八、集成状态总结（2026-02-03 更新）

### 8.1 已完成的集成

- ✅ metersphere-frontend 统一样式
- ✅ i18n 国际化（中文简体、繁体、英文）
- ✅ SVG 图标库和 svg-sprite-loader
- ✅ 全局插件（plugins）
- ✅ 自定义指令（directives）
- ✅ 全局过滤器（filters）
- ✅ 路由权限控制（permission）
- ✅ ECharts 图表组件
- ✅ VueShepherd 新手引导
- ✅ Layout 统一布局组件
- ✅ login 登录路由
- ✅ Pinia store 状态管理
- ✅ vue-devtools 兼容处理

### 8.2 可选优化

1. **splitChunks 代码分割** - 优化打包体积（当前约 4.26 MiB）
2. **API 目录** - 集中管理 API 接口定义

---

## 九、总结

analytics-stat 模块已完成与 MeterSphere 生态的**完整集成**（2026-02-03），实现了与其他模块（如 report-stat）一致的功能：

**已完成**：

- ✅ qiankun 微前端加载
- ✅ metersphere-frontend 共享库完整集成
- ✅ 统一布局（Layout + 二级菜单）
- ✅ 国际化支持
- ✅ 权限控制
- ✅ 前端构建并打包到后端 JAR

**路由结构**：

```
/login                          -> metersphere-frontend 统一登录组件
/analytics-stat                 -> Layout (顶部导航 + 侧边菜单)
  /analytics-stat/dashboard     -> AnalyticsStat (二级布局) -> Dashboard.vue
  /analytics-stat/sql-console   -> AnalyticsStat (二级布局) -> SqlConsole.vue
  /analytics-stat/data-dictionary -> AnalyticsStat (二级布局) -> DataDictionary.vue
```

**构建产物**：

- 前端：`analytics-stat/frontend/dist/`
- 后端 JAR：`analytics-stat/backend/target/analytics-stat-2.10.jar`（含前端静态资源）

---

## 十、新增模块完整流程

### 10.1 后端模块创建

```bash
# 1. 创建目录结构
mkdir -p {module}/backend/src/main/java/io/metersphere/{module}
mkdir -p {module}/backend/src/main/resources/db/migration
mkdir -p {module}/backend/src/main/resources/static

# 2. 创建 pom.xml（参考 analytics-stat/backend/pom.xml）

# 3. 创建 Application.java 启动类
# 4. 创建 application.properties（配置端口、服务名）
# 5. 创建 WebMvcConfig.java（静态资源映射）
# 6. 修改 Gateway SessionFilter（添加模块前缀）
```

### 10.2 前端模块创建

```bash
# 1. 创建目录结构
mkdir -p {module}/frontend/src/{router,business,views}
mkdir -p {module}/frontend/public

# 2. 创建 package.json（参考 analytics-stat/frontend/package.json）
# 3. 创建 vue.config.js（UMD 输出格式）
# 4. 创建 main.js（qiankun 生命周期）
# 5. 创建 public-path.js
# 6. 创建 router/index.js
# 7. 创建 App.vue
```

### 10.3 验证步骤

```bash
# 1. 启动 Eureka
cd framework/eureka && mvn spring-boot:run

# 2. 启动 Gateway
cd framework/gateway && mvn spring-boot:run

# 3. 启动新模块后端
cd {module}/backend && mvn spring-boot:run

# 4. 启动新模块前端
cd {module}/frontend && npm run serve

# 5. 访问 http://localhost:8080/#/{module} 验证
```

### 10.4 常见问题排查

| 问题           | 可能原因             | 解决方案                                     |
| -------------- | -------------------- | -------------------------------------------- |
| 子应用加载失败 | Eureka 未注册        | 检查服务是否启动，访问 http://localhost:8761 |
| 静态资源 404   | SessionFilter 未配置 | 在 PREFIX 数组中添加模块前缀                 |
| 路由不匹配     | activeRule 不正确    | 检查路由前缀是否与服务名一致                 |
| 样式丢失       | publicPath 错误      | 检查 public-path.js 是否正确导入             |
| 跨域错误       | devServer 未配置     | 在 vue.config.js 中配置 headers              |
