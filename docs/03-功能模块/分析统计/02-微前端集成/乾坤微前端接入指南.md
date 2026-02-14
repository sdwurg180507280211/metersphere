# qiankun微前端接入指南

## 概述

analytics-stat模块已经完成了qiankun微前端的配置，可以作为子应用接入到MeterSphere主应用中。

## 自动发现机制

MeterSphere使用**自动发现机制**来注册qiankun子应用：

1. **Eureka注册**：所有微服务启动后自动注册到Eureka
2. **主应用发现**：主应用通过`/api/services`接口从Eureka获取所有注册的微服务
3. **自动注册**：主应用自动将所有微服务注册为qiankun子应用
4. **路由规则**：`#/{serviceId}` 对应子应用的入口

### 关键代码

主应用的`micro-app.js`：

```javascript
import {registerMicroApps, start} from 'qiankun';
import {getApps} from './api/apps'

// 从网关查所有的服务
getApps()
  .then(res => {
    let apps = []
    res.data.forEach(svc => {
      let name = svc.serviceId;
      
      // 网关排除
      if (name === 'gateway') {
        return;
      }
      
      apps.push({
        name,
        entry: '//127.0.0.1:' + (svc.port - 4000), // 前端端口 = 后端端口 - 4000
        container: '#micro-app',
        activeRule: getActiveRule('#/' + name),
        props: {
          eventBus
        }
      });
    });

    //注册子应用
    registerMicroApps(apps);
    //启动
    start();
  })
```

## analytics-stat配置清单

### 1. 后端配置

#### application.properties

```properties
spring.application.name=analytics-stat
server.port=8009
management.server.port=7009
```

**关键点**：
- `spring.application.name`：服务名称，也是qiankun子应用的name
- `server.port`：后端服务端口
- Eureka配置继承自`commons.properties`，无需额外配置

#### Eureka自动注册

SDK的`commons.properties`中已配置：

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true
eureka.instance.instance-id=${spring.cloud.client.ip-address}:${server.port}
```

### 2. 前端配置

#### vite.config.ts

```typescript
import qiankun from 'vite-plugin-qiankun'

export default defineConfig({
  plugins: [
    vue(),
    qiankun('analytics-stat', {
      useDevMode: true
    })
  ],
  server: {
    port: 4009, // 前端端口 = 后端端口 - 4000
    host: '0.0.0.0',
    cors: true, // 允许跨域，qiankun需要
    origin: 'http://localhost:4009',
    headers: {
      'Access-Control-Allow-Origin': '*' // qiankun需要
    }
  }
})
```

**关键点**：
- 前端端口规则：`前端端口 = 后端端口 - 4000`
- analytics-stat：后端8009，前端4009
- 必须开启CORS支持

#### main.ts

```typescript
import { renderWithQiankun, qiankunWindow } from 'vite-plugin-qiankun/dist/helper'

function render(props: any = {}) {
  const { container } = props
  app = createApp(App)
  // ... 其他配置
  
  // 挂载应用
  const containerElement = container ? container.querySelector('#app') : '#app'
  app.mount(containerElement)
}

// 使用vite-plugin-qiankun提供的渲染函数
renderWithQiankun({
  bootstrap() {
    console.log('[analytics-stat] app bootstraped')
  },
  mount(props: any) {
    console.log('[analytics-stat] props from main framework', props)
    render(props)
  },
  unmount() {
    console.log('[analytics-stat] app unmount')
    if (app) {
      app.unmount()
      app = null
    }
  }
})

// 独立运行时
if (!qiankunWindow.__POWERED_BY_QIANKUN__) {
  render()
}
```

#### public-path.ts

```typescript
// qiankun 动态 publicPath 配置
if ((window as any).__POWERED_BY_QIANKUN__) {
  console.log('[analytics-stat] Running in qiankun mode')
}

if (!(window as any).__POWERED_BY_QIANKUN__) {
  console.log('[analytics-stat] Running in standalone mode')
}
```

### 3. Gateway配置

Gateway的`application.properties`中已添加：

```properties
springdoc.swagger-ui.urls[10].url=/analytics-stat/v3/api-docs
springdoc.swagger-ui.urls[10].name=analytics-stat
```

这确保了Gateway会正确路由到analytics-stat服务。

## 启动步骤

### 1. 启动基础服务

```bash
# 1. 启动Eureka
cd framework/eureka
java -jar target/eureka-2.10.jar

# 2. 启动Gateway
cd framework/gateway
java -jar target/gateway-2.10.jar
```

### 2. 启动analytics-stat服务

```bash
# 后端
cd analytics-stat/backend
java -jar target/analytics-stat-2.10.jar

# 前端（开发模式）
cd analytics-stat/frontend
npm run dev
```

### 3. 访问主应用

打开浏览器访问：`http://localhost:8000`

主应用会自动从Eureka发现analytics-stat服务并注册为子应用。

### 4. 访问analytics-stat

在主应用中，访问：`http://localhost:8000/#/analytics-stat`

或者独立访问：`http://localhost:4009`

## 验证清单

### 1. Eureka注册验证

访问Eureka控制台：`http://localhost:8761`

确认看到：
- ✅ ANALYTICS-STAT服务已注册
- ✅ 实例状态为UP

### 2. Gateway路由验证

访问：`http://localhost:8000/analytics-stat/health`

应该返回：
```json
{
  "status": "UP"
}
```

### 3. 前端独立运行验证

访问：`http://localhost:4009`

应该看到analytics-stat的独立页面。

### 4. qiankun集成验证

访问：`http://localhost:8000/#/analytics-stat`

应该看到：
- ✅ analytics-stat子应用加载成功
- ✅ 控制台输出：`[analytics-stat] props from main framework`
- ✅ 页面正常显示

## 端口规则总结

| 服务 | 后端端口 | 管理端口 | 前端端口 | 说明 |
|------|---------|---------|---------|------|
| eureka | 8761 | - | - | 服务注册中心 |
| gateway | 8000 | 7421 | - | API网关 |
| system-setting | 8001 | 7001 | 4001 | 系统设置 |
| project-management | 8002 | 7002 | 4002 | 项目管理 |
| test-track | 8003 | 7003 | 4003 | 测试跟踪 |
| api-test | 8004 | 7004 | 4004 | 接口测试 |
| performance-test | 8005 | 7005 | 4005 | 性能测试 |
| report-stat | 8006 | 7006 | 4006 | 报告统计 |
| workstation | 8007 | 7007 | 4007 | 工作台 |
| workflow-service | 8008 | 7008 | - | 工作流（无前端） |
| **analytics-stat** | **8009** | **7009** | **4009** | **分析统计** |

**规则**：
- 前端端口 = 后端端口 - 4000
- 管理端口 = 后端端口 - 1000

## 常见问题

### 1. 子应用无法加载

**症状**：访问`/#/analytics-stat`时页面空白

**排查步骤**：
1. 检查analytics-stat后端是否启动：`curl http://localhost:8009/health`
2. 检查analytics-stat前端是否启动：`curl http://localhost:4009`
3. 检查Eureka注册：访问`http://localhost:8761`
4. 检查浏览器控制台是否有CORS错误

**解决方案**：
- 确保前端配置了CORS支持
- 确保vite.config.ts中配置了qiankun插件
- 确保main.ts中正确使用了renderWithQiankun

### 2. 路由冲突

**症状**：访问子应用时路由跳转异常

**解决方案**：
- 确保子应用的路由使用hash模式
- 确保子应用的base路径配置正确

### 3. 样式隔离问题

**症状**：子应用的样式影响了主应用

**解决方案**：
- qiankun默认开启样式隔离
- 如果需要自定义，可以在registerMicroApps中配置sandbox选项

## 下一步

1. **添加菜单入口**：在主应用的导航菜单中添加"分析统计"入口
2. **权限控制**：配置analytics-stat的访问权限
3. **数据联调**：实现与其他模块的数据交互
4. **性能优化**：优化子应用的加载速度

## 参考文档

- [qiankun官方文档](https://qiankun.umijs.org/zh/guide)
- [vite-plugin-qiankun](https://github.com/tengmaoqing/vite-plugin-qiankun)
- [分析统计微服务实施记录](./分析统计微服务实施记录.md)


## 生产环境配置（重要）

### 资源路径配置

在生产环境下，子应用的静态资源需要通过 Gateway 代理加载。需要进行以下配置：

#### 1. vite.config.ts 配置

```typescript
export default defineConfig({
  // ... 其他配置
  
  // 关键配置：生产环境下设置资源路径前缀
  base: process.env.NODE_ENV === 'production' ? '/analytics-stat/' : '/'
})
```

**说明**：
- 开发环境：`base: '/'`，资源从本地开发服务器加载
- 生产环境：`base: '/analytics-stat/'`，资源从 Gateway 代理加载
- 这样浏览器会请求 `http://localhost:8000/analytics-stat/assets/...` 而不是 `http://localhost:8000/assets/...`

#### 2. 后端 pom.xml 配置

在 `analytics-stat/backend/pom.xml` 中添加 maven-antrun-plugin：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-antrun-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>main-class-placement</id>
            <phase>generate-resources</phase>
            <configuration>
                <target>
                    <!-- 复制前端构建产物到后端资源目录 -->
                    <copy todir="${basedir}/src/main/resources/static">
                        <fileset dir="${basedir}/../frontend/dist">
                            <exclude name="index.html" />
                        </fileset>
                    </copy>
                    <copy todir="${basedir}/src/main/resources/public">
                        <fileset dir="${basedir}/../frontend/dist">
                            <include name="index.html" />
                        </fileset>
                    </copy>
                </target>
            </configuration>
            <goals>
                <goal>run</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**说明**：
- `index.html` 放在 `public/` 目录（qiankun 入口文件）
- 其他资源（JS、CSS、图片等）放在 `static/` 目录
- Maven 打包时自动执行，将前端资源复制到后端

#### 3. 生产环境资源加载流程

1. 用户点击"分析统计"菜单
2. qiankun 请求 `http://localhost:8000/analytics-stat/` 获取 HTML
3. Gateway 将请求代理到 analytics-stat 后端服务（端口 8009）
4. 后端返回 `src/main/resources/public/index.html`
5. 浏览器解析 HTML，发现资源路径为 `/analytics-stat/assets/index-xxx.js`
6. 浏览器请求 `http://localhost:8000/analytics-stat/assets/index-xxx.js`
7. Gateway 将请求代理到 analytics-stat 后端服务
8. 后端返回 `src/main/resources/static/assets/index-xxx.js`
9. 子应用加载完成，渲染到主应用的容器中

### 生产环境构建步骤

```bash
# 1. 构建前端
cd analytics-stat/frontend
npm run build

# 2. 构建后端（会自动复制前端资源）
cd ../backend
mvn clean package -DskipTests

# 3. 启动服务
java -jar target/analytics-stat-2.10.jar
```

## 子应用布局结构（重要）

### 正确的组件结构

子应用必须遵循以下结构，以避免覆盖主应用的布局：

```
App.vue (简单容器)
  └─ <router-view />
       └─ AnalyticsStat.vue (业务容器)
            ├─ AnalyticsStatHeaderMenus.vue (二级导航栏)
            └─ <router-view /> (页面内容)
```

### 1. App.vue（简单容器）

```vue
<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// 分析统计微服务主应用
</script>

<!-- 不要添加任何样式！ -->
```

**关键点**：
- ✅ 只包含 `<router-view />`，不包含任何布局组件
- ✅ 不要添加任何全局样式或 scoped 样式
- ❌ 不要使用 Layout 组件（侧边栏、顶部栏等）
- ❌ 不要添加全局样式，会影响主应用

### 2. AnalyticsStat.vue（业务容器）

```vue
<template>
  <el-col>
    <analytics-stat-header-menus />
    <div>
      <transition>
        <keep-alive>
          <router-view :baseUrl="baseUrl" />
        </keep-alive>
      </transition>
    </div>
  </el-col>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AnalyticsStatHeaderMenus from './head/AnalyticsStatHeaderMenus.vue'

const baseUrl = ref('analytics-stat')
</script>

<style scoped>
/* 业务容器样式 */
</style>
```

**关键点**：
- ✅ 包含二级导航栏（HeaderMenus）
- ✅ 包含页面内容区域（router-view）
- ✅ 使用 scoped 样式，避免影响主应用

### 3. AnalyticsStatHeaderMenus.vue（二级导航）

```vue
<template>
  <div id="menu-bar" v-if="isRouterAlive">
    <el-row type="flex">
      <el-col :span="24">
        <el-menu 
          class="header-menu" 
          :unique-opened="true" 
          mode="horizontal" 
          router
          :default-active="pathName"
        >
          <el-menu-item 
            v-for="menu in menus" 
            :key="menu.path" 
            :index="menu.path"
          >
            {{ menu.name }}
          </el-menu-item>
        </el-menu>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isRouterAlive = ref(true)
const pathName = ref('')

const menus = ref([
  { path: '/analytics-stat/dashboard', name: '数据概览' },
  { path: '/analytics-stat/sql-console', name: 'SQL查询台' },
  { path: '/analytics-stat/data-dictionary', name: '数据字典' }
])

watch(
  () => route.path,
  (newPath) => {
    if (newPath.indexOf('/analytics-stat/dashboard') >= 0) {
      pathName.value = '/analytics-stat/dashboard'
    } else if (newPath.indexOf('/analytics-stat/sql-console') >= 0) {
      pathName.value = '/analytics-stat/sql-console'
    } else if (newPath.indexOf('/analytics-stat/data-dictionary') >= 0) {
      pathName.value = '/analytics-stat/data-dictionary'
    } else {
      pathName.value = newPath
    }
  },
  { immediate: true }
)
</script>

<style scoped>
#menu-bar {
  border-bottom: 1px solid #e6e6e6;
  background-color: #fff;
}

.el-menu-item {
  padding: 0 10px;
}
</style>
```

### 4. 路由配置

```typescript
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/analytics-stat/dashboard'
  },
  {
    path: '/analytics-stat',
    component: () => import('@/business/AnalyticsStat.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据概览' }
      },
      {
        path: 'sql-console',
        name: 'SqlConsole',
        component: () => import('@/views/SqlConsole.vue'),
        meta: { title: 'SQL查询台' }
      },
      {
        path: 'data-dictionary',
        name: 'DataDictionary',
        component: () => import('@/views/DataDictionary.vue'),
        meta: { title: '数据字典' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
```

**关键点**：
- ✅ 使用嵌套路由，父路由加载业务容器
- ✅ 子路由加载具体页面组件
- ❌ 不要在路由中使用 Layout 组件

### 对比其他模块

| 模块 | App.vue | 业务容器 | 二级导航 |
|------|---------|---------|---------|
| test-track | 简单容器 | TestTrack.vue | TrackHeaderMenus.vue |
| api-test | 简单容器 | ApiTest.vue | ApiHeaderMenus.vue |
| analytics-stat | 简单容器 | AnalyticsStat.vue | AnalyticsStatHeaderMenus.vue |

**一致性原则**：
- 所有子应用的 App.vue 都是简单容器，只包含 `<router-view />`
- 所有子应用都有自己的业务容器组件
- 所有子应用都有自己的二级导航组件
- 所有子应用的页面组件都使用 scoped 样式

### 常见错误

#### ❌ 错误 1：在 App.vue 中添加全局样式

```vue
<!-- 错误示例 -->
<template>
  <div id="app">
    <router-view />
  </div>
</template>

<style>
/* 这会影响主应用！ */
body {
  margin: 0;
  padding: 0;
}
</style>
```

#### ❌ 错误 2：直接在路由中渲染页面

```typescript
// 错误示例
const routes = [
  {
    path: '/analytics-stat/dashboard',
    component: () => import('@/views/Dashboard.vue')
  }
]
```

#### ❌ 错误 3：使用完整的 Layout 组件

```vue
<!-- 错误示例 -->
<template>
  <div id="app">
    <Layout>
      <router-view />
    </Layout>
  </div>
</template>
```

#### ✅ 正确做法

1. App.vue 保持简单，只包含 `<router-view />`
2. 使用业务容器组件（AnalyticsStat.vue）包含二级导航
3. 使用嵌套路由，父路由加载业务容器
4. 所有样式都使用 scoped，避免影响主应用

## Vite vs Vue CLI 对比

| 特性 | Vue CLI（其他模块） | Vite（analytics-stat） |
|------|-------------------|----------------------|
| 构建工具 | Webpack | Vite |
| 配置文件 | vue.config.js | vite.config.ts |
| 资源路径配置 | publicPath | base |
| 默认行为 | publicPath: "/" | base: "/" |
| qiankun 插件 | webpack-plugin-qiankun | vite-plugin-qiankun |
| 生产环境配置 | publicPath: "/" | base: "/analytics-stat/" |

**关键差异**：
- Vue CLI 模块：qiankun 会自动处理资源路径
- Vite 模块：需要手动配置 `base: '/analytics-stat/'`

## 主应用菜单集成

### 1. 添加菜单图标

创建 `framework/sdk-parent/frontend/src/assets/module/analytics-stat.svg`

### 2. 修改 AsideMenus.vue

在 `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue` 中添加：

```vue
<el-menu-item index="/analytics-stat/dashboard">
  <img :src="require('@/assets/module/analytics-stat.svg')" />
  <span>分析统计</span>
</el-menu-item>
```

### 3. 重新构建 SDK 和 Gateway

```bash
# 1. 构建 SDK 前端
cd framework/sdk-parent/frontend
npm run build

# 2. 打包 SDK
cd ..
mvn clean install -DskipTests

# 3. 打包 Gateway
cd ../gateway
mvn clean package -DskipTests

# 4. 重启 Gateway
java -jar target/gateway-2.10.jar
```

**重要**：修改主应用的菜单后，必须重新构建 SDK 和 Gateway，否则菜单不会显示。

## 完整验证清单

### 开发环境验证

- [ ] analytics-stat 后端服务启动成功（端口 8009）
- [ ] analytics-stat 前端服务启动成功（端口 4009）
- [ ] Eureka 显示 analytics-stat 服务已注册
- [ ] 独立访问 `http://localhost:4009` 正常显示
- [ ] 主应用显示"分析统计"菜单
- [ ] 点击菜单后，子应用正常加载
- [ ] 浏览器控制台输出 `[analytics-stat] mount`
- [ ] 主应用的布局（侧边栏、顶部栏）保持不变
- [ ] 子应用的二级导航正常显示
- [ ] 子应用的页面内容正常显示

### 生产环境验证

- [ ] 前端构建成功（`npm run build`）
- [ ] 后端打包成功（`mvn clean package`）
- [ ] 前端资源已复制到后端 `src/main/resources/static/`
- [ ] `index.html` 已复制到后端 `src/main/resources/public/`
- [ ] 启动后端服务，访问 `http://localhost:8009/` 返回 index.html
- [ ] 访问 `http://localhost:8009/assets/index-xxx.js` 返回 JS 文件
- [ ] 通过 Gateway 访问 `http://localhost:8000/analytics-stat/` 正常加载
- [ ] 浏览器控制台没有 404 或 500 错误
- [ ] 主应用的布局保持不变
- [ ] 子应用的功能正常

## 故障排查

### 问题 1：资源加载 500 错误

**症状**：点击菜单后，浏览器控制台报错 `GET http://localhost:8000/assets/index-xxx.js 500`

**原因**：Vite 默认的 `base` 配置是 `/`，导致资源路径没有 `/analytics-stat/` 前缀

**解决方案**：
1. 在 `vite.config.ts` 中设置 `base: process.env.NODE_ENV === 'production' ? '/analytics-stat/' : '/'`
2. 重新构建前端：`npm run build`
3. 重新打包后端：`mvn clean package -DskipTests`
4. 重启后端服务

### 问题 2：主应用布局被覆盖

**症状**：点击菜单后，主应用的侧边栏和顶部栏消失

**原因**：子应用的 App.vue 中包含了全局样式或 Layout 组件

**解决方案**：
1. 确保 App.vue 只包含 `<router-view />`，不包含任何样式
2. 确保路由配置使用嵌套路由，父路由加载业务容器
3. 确保业务容器组件（AnalyticsStat.vue）包含二级导航
4. 重新构建前端和后端

### 问题 3：菜单不显示

**症状**：重启 Gateway 后，"分析统计"菜单不显示

**原因**：SDK 前端没有重新构建和打包到 Gateway

**解决方案**：
1. 重新构建 SDK 前端：`cd framework/sdk-parent/frontend && npm run build`
2. 重新打包 SDK：`cd .. && mvn clean install -DskipTests`
3. 重新打包 Gateway：`cd ../gateway && mvn clean package -DskipTests`
4. 重启 Gateway

## 总结

analytics-stat 模块已成功接入 qiankun 微前端架构，关键配置包括：

1. **前端配置**：
   - vite.config.ts：配置 qiankun 插件和 base 路径
   - main.ts：使用 renderWithQiankun 实现生命周期
   - 布局结构：App.vue（简单容器）→ AnalyticsStat.vue（业务容器）→ 页面组件

2. **后端配置**：
   - pom.xml：配置 maven-antrun-plugin 复制前端资源
   - application.properties：配置服务名称和端口

3. **主应用配置**：
   - AsideMenus.vue：添加菜单项
   - 重新构建 SDK 和 Gateway

4. **生产环境**：
   - 前端资源打包到后端 JAR
   - 通过 Gateway 代理访问
   - 资源路径使用 `/analytics-stat/` 前缀

遵循以上配置，可以确保子应用正确加载，且不会影响主应用的布局和样式。
