# analytics-stat qiankun微前端集成指南

## 概述

analytics-stat前端项目已成功接入MeterSphere的qiankun微前端架构。本文档记录了集成过程和配置说明。

## 技术栈差异

### 现有模块（Vue 2 + Webpack）
- Vue 2.7.3
- Webpack 5
- qiankun 2.9.3
- 使用`__webpack_public_path__`动态设置资源路径

### analytics-stat（Vue 3 + Vite）
- Vue 3.4.0
- Vite 5.4.21
- vite-plugin-qiankun
- 使用Vite插件处理qiankun集成

## 集成步骤

### 1. 安装依赖

```bash
cd analytics-stat/frontend
npm install vite-plugin-qiankun --save-dev
```

### 2. 创建 public-path.ts

文件位置：`analytics-stat/frontend/src/public-path.ts`

```typescript
/**
 * qiankun 动态 publicPath 配置
 * 用于支持微前端模式下的资源加载
 */

// 在qiankun环境下，使用动态注入的publicPath
if ((window as any).__POWERED_BY_QIANKUN__) {
  console.log('[analytics-stat] Running in qiankun mode')
}

// 独立运行时
if (!(window as any).__POWERED_BY_QIANKUN__) {
  console.log('[analytics-stat] Running in standalone mode')
}
```

### 3. 修改 main.ts

文件位置：`analytics-stat/frontend/src/main.ts`

关键修改：
- 导入`renderWithQiankun`和`qiankunWindow`
- 使用`renderWithQiankun`包装生命周期钩子
- 支持独立运行和微前端两种模式

```typescript
import './public-path'
import { renderWithQiankun, qiankunWindow } from 'vite-plugin-qiankun/dist/helper'

let app: any = null

function render(props: any = {}) {
  const { container } = props
  app = createApp(App)
  // ... 配置
  app.mount(container ? container.querySelector('#app') : '#app')
}

// qiankun生命周期
renderWithQiankun({
  bootstrap() {},
  mount(props: any) {
    render(props)
  },
  unmount() {
    if (app) {
      app.unmount()
      app = null
    }
  }
})

// 独立运行
if (!qiankunWindow.__POWERED_BY_QIANKUN__) {
  render()
}
```

### 4. 配置 vite.config.ts

文件位置：`analytics-stat/frontend/vite.config.ts`

关键配置：
- 使用`vite-plugin-qiankun`插件
- 配置CORS支持
- 设置开发服务器端口为4009

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
    port: 4009,
    host: '0.0.0.0',
    cors: true,
    origin: 'http://localhost:4009',
    headers: {
      'Access-Control-Allow-Origin': '*'
    }
  }
})
```

### 5. 路由配置

文件位置：`analytics-stat/frontend/src/router/index.ts`

路由已使用`/analytics-stat`作为基础路径，符合qiankun规范：

```typescript
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/analytics-stat/dashboard'
  },
  {
    path: '/analytics-stat',
    name: 'AnalyticsStat',
    component: () => import('@/views/Layout.vue'),
    children: [
      // 子路由...
    ]
  }
]
```

## 主应用注册（待实施）

### 修改 micro-app.js

文件位置：`framework/sdk-parent/frontend/src/micro-app.js`

需要在主应用中注册analytics-stat子应用：

```javascript
// 从网关查所有的服务
getApps()
  .then(res => {
    let apps = []
    res.data.forEach(svc => {
      let name = svc.serviceId;
      
      // 添加analytics-stat的特殊处理
      if (name === 'analytics-stat') {
        apps.push({
          name,
          entry: '//127.0.0.1:4009', // Vite开发服务器端口
          container: '#micro-app',
          activeRule: getActiveRule('#/analytics-stat'),
          props: {
            eventBus
          }
        });
        return;
      }
      
      // 其他服务的处理...
    });
    
    registerMicroApps(apps);
    start();
  })
```

### 后端服务注册

需要确保analytics-stat服务在Eureka中注册，并且Gateway能够正确路由。

## 开发模式

### 独立开发

```bash
cd analytics-stat/frontend
npm run dev
```

访问：http://localhost:4009

### 微前端模式

1. 启动主应用（Gateway + SDK Frontend）
2. 启动analytics-stat后端服务（端口8009）
3. 启动analytics-stat前端服务（端口4009）
4. 在主应用中访问：http://localhost:8080/#/analytics-stat/dashboard

## 生产构建

```bash
cd analytics-stat/frontend
npm run build
```

构建产物会输出到`dist`目录，然后通过Maven插件复制到后端的`src/main/resources/static`目录。

## 端口规划

| 服务 | 开发端口 | 生产端口 | 说明 |
|------|---------|---------|------|
| Gateway | 8080 | 8080 | 主应用入口 |
| analytics-stat后端 | 8009 | 8009 | 后端API服务 |
| analytics-stat前端 | 4009 | - | 开发服务器（生产环境静态资源由后端提供） |

## 路由规则

### 开发环境
- 主应用：`http://localhost:8080`
- 子应用入口：`http://localhost:4009`
- 激活规则：URL hash以`#/analytics-stat`开头

### 生产环境
- 主应用：`http://your-domain:8080`
- 子应用入口：`http://your-domain:8080/analytics-stat/`
- 激活规则：URL hash以`#/analytics-stat`开头

## 注意事项

### 1. CORS配置
开发环境需要配置CORS，允许主应用跨域加载子应用资源。

### 2. 资源路径
- 开发环境：Vite dev server提供资源
- 生产环境：后端静态资源服务提供

### 3. 路由模式
使用`createWebHistory`，但在qiankun环境下会被主应用的路由管理。

### 4. 样式隔离
qiankun会自动处理样式隔离，但建议：
- 使用scoped样式
- 避免全局样式污染
- 使用CSS Modules或BEM命名规范

### 5. 状态管理
- 子应用使用独立的Pinia store
- 通过qiankun的props传递全局状态
- 使用eventBus进行跨应用通信

## 调试技巧

### 1. 查看qiankun状态

```javascript
// 在浏览器控制台
console.log(window.__POWERED_BY_QIANKUN__)
```

### 2. 查看子应用加载状态

```javascript
// 在主应用控制台
import { getApps } from 'qiankun'
console.log(getApps())
```

### 3. 开发者工具
- Vue Devtools支持微前端调试
- 在main.ts中已配置devtools支持

## 常见问题

### Q1: 子应用资源404
**原因**：publicPath配置不正确
**解决**：检查vite.config.ts中的base配置和qiankun插件配置

### Q2: 样式丢失
**原因**：样式没有正确加载或被隔离
**解决**：确保CSS文件正确导入，检查样式隔离配置

### Q3: 路由跳转失败
**原因**：路由基础路径不匹配
**解决**：确保路由配置使用`/analytics-stat`作为基础路径

### Q4: 子应用无法通信
**原因**：eventBus未正确传递
**解决**：检查主应用注册时是否传递了eventBus

## 参考资料

- [qiankun官方文档](https://qiankun.umijs.org/)
- [vite-plugin-qiankun](https://github.com/tengmaoqing/vite-plugin-qiankun)
- [Vue 3 + Vite + qiankun最佳实践](https://github.com/umijs/qiankun/issues/1257)

## 更新记录

- 2026-02-02：完成qiankun集成配置
- 2026-02-02：前端构建验证通过
- 待完成：主应用注册和联调测试
