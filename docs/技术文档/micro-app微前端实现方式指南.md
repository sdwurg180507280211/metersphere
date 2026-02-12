# micro-app 微前端实现方式指南

> 来源：[micro-app 官方文档](https://micro-zoe.github.io/doc/zh/)（京东零售开源）
> 整理目的：为 MeterSphere 从 qiankun 迁移到 micro-app 提供完整参考
> Content was rephrased for compliance with licensing restrictions

---

## 目录

1. [介绍](#1-介绍)
2. [快速开始](#2-快速开始)
3. [配置项](#3-配置项)
4. [核心功能](#4-核心功能)
5. [Vue 接入教程](#5-vue-接入教程)
6. [Vite 接入教程](#6-vite-接入教程)
7. [API 参考](#7-api-参考)
8. [部署](#8-部署)
9. [常见问题](#9-常见问题)
10. [MeterSphere 迁移要点](#10-metersphere-迁移要点)

---

## 1. 介绍

micro-app 是京东零售开源的微前端框架，基于类 WebComponent 进行渲染。
核心思路：将微前端当作组件来使用，通过自定义元素 `<micro-app>` 标签嵌入子应用。

核心特点：
- 基于 WebComponent（CustomElement + ShadowDom 思想），无需子应用修改入口导出
- 提供 JS 沙箱（Proxy）和 CSS 隔离（Scoped CSS）
- 原生支持 Vite + ES Module，不要求 UMD 打包
- 支持 Vue 2/3、React、Angular 等主流框架混合运行
- 提供预加载、keep-alive、虚拟路由系统等高级功能

与 qiankun 的核心区别：
- qiankun 要求子应用导出 bootstrap/mount/unmount 生命周期 + UMD 打包
- micro-app 子应用几乎零改造，只需设置跨域头即可接入

---

## 2. 快速开始

### 主应用

**步骤1：安装依赖**
```bash
npm i @micro-zoe/micro-app --save
```

**步骤2：初始化 micro-app**
```javascript
// main.js
import microApp from '@micro-zoe/micro-app'
microApp.start()
```

**步骤3：嵌入子应用**
```vue
<template>
  <div>
    <h1>子应用👇</h1>
    <!-- name：应用名称, url：应用地址 -->
    <micro-app name='my-app' url='http://localhost:3000/'></micro-app>
  </div>
</template>
```

### 子应用

**唯一要求：设置跨域支持**
```javascript
// vue.config.js
devServer: {
  headers: {
    'Access-Control-Allow-Origin': '*',
  }
}
```

> 就这么简单。子应用不需要导出生命周期函数，不需要 UMD 打包。

### 注意事项
- `name`：必传，必须以字母开头，不可带特殊符号（中划线、下划线除外）
- `url`：必传，必须指向子应用的 index.html
- 子应用必须支持跨域

---

## 3. 配置项

### 3.1 标签属性配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | string | 必传 | 应用名称，必须唯一，以字母开头 |
| `url` | string | 必传 | 应用地址，指向子应用的 index.html |
| `iframe` | boolean | false | 开启 iframe 沙箱（vite 子应用必须开启） |
| `inline` | boolean | false | 使用内联 script 模式，方便调试 |
| `destroy` | boolean | false | 卸载时强制删除缓存资源 |
| `clear-data` | boolean | false | 卸载时清空通讯缓存数据 |
| `disable-scopecss` | boolean | false | 关闭样式隔离 |
| `disable-sandbox` | boolean | false | 关闭 JS 沙箱 |
| `ssr` | boolean | false | 开启 SSR 模式 |
| `keep-alive` | boolean | false | 开启保活模式 |
| `default-page` | string | '' | 指定默认渲染的页面 |
| `router-mode` | string | 'search' | 路由模式：search/native/native-scope/pure |
| `baseroute` | string | '' | 子应用基础路由 |
| `keep-router-state` | boolean | false | 卸载后保留路由状态 |
| `disable-memory-router` | boolean | false | 关闭虚拟路由系统 |
| `disable-patch-request` | boolean | false | 关闭请求自动补全 |
| `fiber` | boolean | false | 开启 fiber 模式（异步执行 JS，减少阻塞） |

### 3.2 全局配置

```javascript
import microApp from '@micro-zoe/micro-app'

microApp.start({
  iframe: true,                    // 全局开启 iframe 沙箱
  inline: true,                    // 全局开启内联 script
  destroy: true,                   // 全局开启 destroy 模式
  'disable-scopecss': true,        // 全局禁用样式隔离
  'disable-sandbox': true,         // 全局禁用沙箱
  'keep-alive': true,              // 全局开启保活
  'disable-memory-router': true,   // 全局关闭虚拟路由
  'keep-router-state': true,       // 全局保留路由状态
  'disable-patch-request': true,   // 全局关闭请求补全
  iframeSrc: location.origin,      // iframe 沙箱的 src 地址
})
```

单个应用可覆盖全局配置：
```html
<micro-app name='xx' url='xx' iframe='false' inline='false'></micro-app>
```

### 3.3 其它配置

**global（共享资源）**：多个子应用使用相同 js/css 时，设置 global 属性共享缓存
```html
<link rel="stylesheet" href="xx.css" global>
<script src="xx.js" global></script>
```

**globalAssets（全局静态资源）**：浏览器空闲时预加载公共资源
```javascript
microApp.start({
  globalAssets: {
    js: ['js地址1', 'js地址2'],
    css: ['css地址1', 'css地址2'],
  }
})
```

**exclude（过滤元素）**：子应用不需要加载某个资源时删除
```html
<script src="xx.js" exclude></script>
```

**ignore（忽略元素）**：micro-app 不处理该元素，原封不动渲染（用于 jsonp 等场景）
```javascript
const script = document.createElement('script')
script.setAttribute('ignore', 'true')
```

---

## 4. 核心功能

### 4.1 生命周期

micro-app 通过 CustomEvent 定义生命周期，在渲染过程中触发相应事件。

**生命周期列表：**
1. `created` — `<micro-app>` 标签初始化后，加载资源前触发
2. `beforemount` — 加载资源完成后，开始渲染之前触发
3. `mounted` — 子应用渲染结束后触发
4. `unmount` — 子应用卸载时触发
5. `error` — 子应用渲染出错时触发（仅导致渲染终止的错误）

**Vue 中监听：**
```vue
<template>
  <micro-app
    name='xx'
    url='xx'
    @created='created'
    @beforemount='beforemount'
    @mounted='mounted'
    @unmount='unmount'
    @error='error'
  />
</template>
```

**全局监听：**
```javascript
microApp.start({
  lifeCycles: {
    created(e) { console.log('created') },
    beforemount(e) { console.log('beforemount') },
    mounted(e) { console.log('mounted') },
    unmount(e) { console.log('unmount') },
    error(e) { console.log('error') },
  }
})
```

**子应用侧全局事件：**
```javascript
// 渲染事件
window.onmount = (data) => {
  console.log('子应用已经渲染', data)
}

// 卸载事件（两种方式）
window.onunmount = () => {
  console.log('子应用已经卸载')
}
// 或
window.addEventListener('unmount', function () {
  console.log('子应用已经卸载')
})
```

### 4.2 环境变量

micro-app 在子应用中注入以下全局变量：

| 变量 | 说明 |
|------|------|
| `window.__MICRO_APP_ENVIRONMENT__` | 判断是否在微前端环境中 |
| `window.__MICRO_APP_NAME__` | 当前应用名称 |
| `window.__MICRO_APP_PUBLIC_PATH__` | 子应用静态资源前缀（用于设置 webpack publicPath） |
| `window.__MICRO_APP_BASE_ROUTE__` | 子应用基础路由 |
| `window.__MICRO_APP_BASE_APPLICATION__` | 判断是否是主应用 |

**publicPath 设置（关键）：**
```javascript
// src/public-path.js
if (window.__MICRO_APP_ENVIRONMENT__) {
  __webpack_public_path__ = window.__MICRO_APP_PUBLIC_PATH__
}
```
在入口文件最顶部引入：`import './public-path'`

### 4.3 JS 沙箱

micro-app 使用 Proxy 拦截全局操作，防止 window 访问和修改，避免全局变量污染。

**两种沙箱方案：**
- **with 沙箱**（默认）：基于 Proxy + with 语句
- **iframe 沙箱**：基于 iframe 天然隔离，vite 子应用必须使用

**注意事项：**
- 沙箱中顶层变量不会泄漏为全局变量（`var xx` 不等于 `window.xx`）
- 获取真实 window 的方式：`new Function("return window")()`、`(0, eval)('window')`、`window.rawWindow`
- 如果遇到 `xxx is not defined` 错误，需要将 `var xx =` 改为 `window.xx =`
- 可通过插件系统批量修改：

```javascript
microApp.start({
  plugins: {
    modules: {
      '应用名称': [{
        loader(code, url) {
          if (url === 'xxx.js') {
            code = code.replace('var xx_dll=', 'window.xx_dll=')
          }
          return code
        }
      }]
    }
  }
})
```

### 4.4 虚拟路由系统

micro-app 通过拦截浏览器路由事件和自定义 location/history，实现虚拟路由系统，与主应用路由隔离。

**四种路由模式：**

| 模式 | 说明 | 浏览器地址 |
|------|------|-----------|
| `search`（默认） | 子应用路由信息作为 query 参数同步到浏览器 | `?micro-app-my-app=%2Fpage1` |
| `native` | 子应用完全基于浏览器路由，地址更简洁 | `/page1` |
| `native-scope` | 同 native，但子应用域名指向自身 | `/page1` |
| `pure` | 子应用独立于浏览器渲染，不修改地址（类似 iframe） | 不变 |

**跨应用路由控制：**

```javascript
// 主应用控制子应用跳转
import microApp from '@micro-zoe/micro-app'
microApp.router.push({name: 'my-app', path: '/page1'})
microApp.router.replace({name: 'my-app', path: '/page1'})
microApp.router.go(-1)

// 子应用控制其他子应用跳转
window.microApp.router.push({name: 'other-app', path: '/page1'})

// 子应用控制主应用跳转
const baseRouter = window.microApp.router.getBaseAppRouter()
baseRouter.push('/main-page')
```

**导航守卫：**
```javascript
// 全局前置守卫
microApp.router.beforeEach((to, from, appName) => {
  console.log('路由变化', to, from, appName)
})

// 全局后置守卫
microApp.router.afterEach((to, from, appName) => {
  console.log('路由变化完成', to, from, appName)
})
```

**获取子应用路由信息：**
```javascript
const routeInfo = microApp.router.current.get('my-app')
```

### 4.5 样式隔离

micro-app 样式隔离默认开启，以 `<micro-app>` 标签的 name 属性为每个样式添加前缀：
```css
/* 原始 */
.test { color: red; }
/* 转换后 */
micro-app[name=xxx] .test { color: red; }
```

注意：主应用样式仍会影响子应用，需通过约定前缀或 CSS Modules 解决。

**禁用样式隔离的四个层次：**
1. 全局禁用：`microApp.start({ disableScopecss: true })`
2. 单个应用禁用：`<micro-app disable-scopecss>`
3. 单个文件禁用：`/*! scopecss-disable */` ... `/*! scopecss-enable */`
4. 单行禁用：`/*! scopecss-disable-next-line */`

> 注释以 `/*!` 开头是为了防止构建时被压缩工具删除

### 4.6 元素隔离

模拟 ShadowDom 的元素隔离，子应用只能操作自身内部的元素。

例如：主应用和子应用都有 `<div id='root'>`，子应用通过 `document.querySelector('#root')` 获取的是自己内部的元素。

主应用可以获取子应用的元素（与 ShadowDom 不同）。

**解除元素绑定：**
```javascript
import { removeDomScope } from '@micro-zoe/micro-app'
removeDomScope(true)
```

### 4.7 数据通信

micro-app 提供绑定式通信（主应用 ↔ 指定子应用）和全局通信两种机制。

#### 子应用获取主应用数据

```javascript
// 方式1：直接获取
const data = window.microApp.getData()

// 方式2：监听变化
window.microApp.addDataListener((data) => {
  console.log('来自主应用的数据', data)
}, true) // true = 有缓存数据时主动触发一次

// 解绑
window.microApp.removeDataListener(dataListener)
window.microApp.clearDataListener()
```

#### 子应用向主应用发送数据

```javascript
// dispatch 只接受对象参数，异步执行，多次调用会合并
window.microApp.dispatch({type: '子应用数据'})

// 带回调
window.microApp.dispatch({city: 'HK'}, (res) => {
  console.log('回调返回值', res)
})

// 强制发送（无论数据是否变化）
window.microApp.forceDispatch({name: 'jack'})
```

#### 主应用向子应用发送数据

```javascript
// 方式1：通过 data 属性（Vue）
<micro-app name='my-app' url='xx' :data='dataForChild' />

// 方式2：手动发送
import microApp from '@micro-zoe/micro-app'
microApp.setData('my-app', {type: '新的数据'})

// 强制发送
microApp.forceSetData('my-app', {name: 'jack'})
```

#### 主应用获取子应用数据

```javascript
// 方式1：直接获取
const childData = microApp.getData('my-app')

// 方式2：Vue 中监听 datachange 事件
<micro-app name='my-app' url='xx' @datachange='handleDataChange' />
// e.detail.data 中获取数据

// 方式3：绑定监听函数
microApp.addDataListener('my-app', (data) => {
  console.log('来自子应用的数据', data)
})
```

#### 全局数据通信（跨应用广播）

```javascript
// 主应用发送全局数据
microApp.setGlobalData({type: '全局数据'})

// 子应用发送全局数据
window.microApp.setGlobalData({type: '全局数据'})

// 监听全局数据
microApp.addGlobalDataListener((data) => {
  console.log('全局数据', data)
})
window.microApp.addGlobalDataListener((data) => {
  console.log('全局数据', data)
})

// 清空全局数据
microApp.clearGlobalData()
```

#### 关闭沙箱后的通信

沙箱关闭后需手动注册通信对象：
```javascript
// 主应用
import { EventCenterForMicroApp } from '@micro-zoe/micro-app'
window.eventCenterForAppxx = new EventCenterForMicroApp(appName)

// 子应用使用 window.eventCenterForAppxx 替代 window.microApp
```

### 4.8 预加载

在浏览器空闲时提前加载子应用静态资源，提升首次渲染速度。

```javascript
import microApp from '@micro-zoe/micro-app'

microApp.preFetch([
  { name: 'my-app1', url: 'xxx' },                    // level 2：加载并解析（默认）
  { name: 'my-app2', url: 'xxx', level: 1 },          // level 1：只加载资源
  { name: 'my-app3', url: 'xxx', level: 3 },          // level 3：加载、解析并渲染
  { name: 'vite-app', url: 'xxx', iframe: true },     // vite 子应用需设置 iframe
], 5000) // 延迟 5 秒执行

// 也可在 start 中配置
microApp.start({
  preFetchApps: [{ name: 'my-app1', url: 'xxx' }],
  prefetchDelay: 5000,  // 修改默认延迟
  prefetchLevel: 1,     // 修改默认 level
})
```

### 4.9 keep-alive

开启后应用卸载时不销毁，推入后台运行，保留状态并提升重复渲染性能。

```html
<micro-app name='xx' url='xx' keep-alive></micro-app>
```

**keep-alive 特有生命周期：**
- `afterhidden` — 子应用推入后台时触发
- `beforeshow` — 子应用推入前台之前触发（初始化时不执行）
- `aftershow` — 子应用推入前台之后触发（初始化时不执行）

**子应用监听状态变化：**
```javascript
window.addEventListener('appstate-change', function (e) {
  if (e.detail.appState === 'afterhidden') console.log('已卸载')
  else if (e.detail.appState === 'beforeshow') console.log('即将重新渲染')
  else if (e.detail.appState === 'aftershow') console.log('已经重新渲染')
})
```

注意：keep-alive 是应用级别的，只保留当前活动页面状态。组件级缓存需使用 Vue 的 keep-alive。

### 4.10 多层嵌套

子应用可以嵌入其它子应用，但需设置不同的标签名避免冲突：

```javascript
// 子应用中
microApp.start({
  tagName: 'micro-app-xxx', // 必须以 micro-app- 开头
})
```
```html
<micro-app-xxx name='xx' url='xx'></micro-app-xxx>
```

注意：无论嵌套多少层，name 都要保证全局唯一。

### 4.11 插件系统

插件系统用于对子应用的 JS 文件进行拦截和处理，修复沙箱中的兼容问题。

```javascript
microApp.start({
  plugins: {
    // 全局插件，作用于所有子应用
    global: [{
      scopeProperties: ['key1'],      // 强隔离的全局变量
      escapeProperties: ['key2'],     // 可逃逸到外部的全局变量
      excludeChecker: (url) => url.includes('/foo.js'),  // 忽略某些资源
      loader(code, url, options, info) {
        console.log('全局插件处理')
        return code
      }
    }],
    // 子应用插件，只作用于指定应用
    modules: {
      'appName1': [{
        loader(code, url) {
          if (url === 'xxx.js') {
            code = code.replace('var abc =', 'window.abc =')
          }
          return code
        }
      }]
    }
  }
})
```

### 4.12 高级功能

**自定义 fetch：** 替换框架自带的 fetch，可修改请求配置或拦截资源

```javascript
microApp.start({
  fetch(url, options, appName) {
    if (url === 'http://localhost:3001/error.js') {
      return Promise.resolve('') // 删除某个 JS 的内容
    }
    return window.fetch(url, Object.assign(options, {
      credentials: 'include', // 带 cookie
    })).then(res => res.text())
  }
})
```

**excludeAssetFilter：** 指定某些动态加载的资源不被 micro-app 劫持
```javascript
microApp.start({
  excludeAssetFilter: (assetUrl) => assetUrl.includes('special-resource')
})
```

---

## 5. Vue 接入教程

### 作为主应用（Vue 2/3）

```javascript
// main.js
import microApp from '@micro-zoe/micro-app'
import Vue from 'vue'

// Vue 2：忽略 micro-app 自定义元素
Vue.config.ignoredElements = ['micro-app']

microApp.start()
```

```vue
<template>
  <div>
    <micro-app name='my-app' url='http://localhost:3000/'></micro-app>
  </div>
</template>
```

Vue 3 需要在 vue.config.js 中配置：
```javascript
// vue.config.js
module.exports = {
  chainWebpack: config => {
    config.module.rule('vue').use('vue-loader').tap(options => {
      options.compilerOptions = {
        ...(options.compilerOptions || {}),
        isCustomElement: (tag) => /^micro-app/.test(tag),
      }
      return options
    })
  }
}
```

### 作为子应用（Vue 2）

**步骤1：设置跨域**
```javascript
// vue.config.js
module.exports = {
  devServer: {
    headers: { 'Access-Control-Allow-Origin': '*' }
  }
}
```

**步骤2：注册卸载函数**
```javascript
// main.js
const app = new Vue(...)

window.unmount = () => {
  app.$destroy()
}
```

**步骤3（可选）：开启 UMD 模式（频繁渲染/卸载时推荐）**
```javascript
// main.js
import Vue from 'vue'
import router from './router'
import App from './App.vue'

let app = null

// 将渲染操作放入 mount 函数
window.mount = () => {
  app = new Vue({
    router,
    render: h => h(App),
  }).$mount('#app')
}

// 将卸载操作放入 unmount 函数
window.unmount = () => {
  app.$destroy()
  app.$el.innerHTML = ''
  app = null
}

// 非微前端环境直接执行
if (!window.__MICRO_APP_ENVIRONMENT__) {
  window.mount()
}
```

**步骤4（可选）：设置 publicPath**
```javascript
// src/public-path.js
if (window.__MICRO_APP_ENVIRONMENT__) {
  __webpack_public_path__ = window.__MICRO_APP_PUBLIC_PATH__
}
```

**步骤5（可选）：设置 chunkLoadingGlobal 避免资源冲突**
```javascript
// vue.config.js
module.exports = {
  configureWebpack: {
    output: {
      chunkLoadingGlobal: 'webpackJsonp_自定义名称',
      globalObject: 'window',
    }
  }
}
```

---

## 6. Vite 接入教程

### 作为主应用
Vite 作为主应用没有特殊之处，参考 Vue 接入文档即可。

### 作为子应用
Vite 子应用只需切换到 iframe 沙箱：

```html
<micro-app name='xxx' url='xxx' iframe></micro-app>
```

**注意事项：**
- vite 构建的 script type 为 module，导致无法拦截 location 操作
- 需使用 micro-app 提供的 location：
```javascript
window.microApp.location.host
window.microApp.location.origin
window.microApp.location.href = 'xxx'
window.microApp.location.pathname = 'xxx'
```

---

## 7. API 参考

### 7.1 主应用 API

| API | 说明 |
|-----|------|
| `microApp.start(options)` | 注册函数，全局执行一次 |
| `microApp.preFetch(apps, delay)` | 预加载子应用资源 |
| `microApp.setData(appName, data)` | 向指定子应用发送数据 |
| `microApp.getData(appName)` | 获取指定子应用的 data |
| `microApp.forceSetData(appName, data)` | 强制发送数据 |
| `microApp.addDataListener(appName, fn, autoTrigger)` | 监听子应用数据变化 |
| `microApp.removeDataListener(appName, fn)` | 解绑数据监听 |
| `microApp.clearDataListener(appName)` | 清空数据监听 |
| `microApp.setGlobalData(data)` | 发送全局数据 |
| `microApp.getGlobalData()` | 获取全局数据 |
| `microApp.addGlobalDataListener(fn)` | 监听全局数据 |
| `microApp.removeGlobalDataListener(fn)` | 解绑全局监听 |
| `microApp.clearGlobalDataListener()` | 清空全局监听 |
| `microApp.clearData(appName)` | 清空发送给子应用的缓存数据 |
| `microApp.clearGlobalData()` | 清空全局数据 |
| `microApp.router.push({name, path})` | 控制子应用跳转 |
| `microApp.router.replace({name, path})` | 控制子应用跳转（replace） |
| `microApp.router.go(n)` | 历史堆栈前进/后退 |
| `microApp.router.beforeEach(fn)` | 全局前置守卫 |
| `microApp.router.afterEach(fn)` | 全局后置守卫 |
| `microApp.router.current.get(name)` | 获取子应用路由信息 |
| `microApp.router.setDefaultPage({name, path})` | 设置默认页面 |
| `microApp.router.setBaseAppRouter(router)` | 注册主应用路由对象 |
| `microApp.router.attachToURL(name)` | 同步子应用路由到浏览器地址 |
| `getActiveApps()` | 获取正在运行的子应用 |
| `getAllApps()` | 获取所有子应用 |
| `unmountApp(name, options)` | 手动卸载应用 |
| `unmountAllApps(options)` | 手动卸载所有应用 |
| `renderApp(options)` | 手动渲染子应用 |
| `microApp.reload(name, destroy)` | 重新渲染子应用 |
| `pureCreateElement(tag)` | 创建无绑定的纯净元素 |
| `removeDomScope(force)` | 解除元素绑定 |

### 7.2 子应用 API

| API | 说明 |
|-----|------|
| `window.microApp.getData()` | 获取主应用下发的数据 |
| `window.microApp.addDataListener(fn, autoTrigger)` | 监听主应用数据变化 |
| `window.microApp.removeDataListener(fn)` | 解绑数据监听 |
| `window.microApp.clearDataListener()` | 清空数据监听 |
| `window.microApp.dispatch(data, callback)` | 向主应用发送数据 |
| `window.microApp.forceDispatch(data, callback)` | 强制发送数据 |
| `window.microApp.setGlobalData(data)` | 发送全局数据 |
| `window.microApp.getGlobalData()` | 获取全局数据 |
| `window.microApp.addGlobalDataListener(fn)` | 监听全局数据 |
| `window.microApp.removeGlobalDataListener(fn)` | 解绑全局监听 |
| `window.microApp.clearGlobalDataListener()` | 清空全局监听 |
| `window.microApp.clearData()` | 清空发送给主应用的数据 |
| `window.microApp.router.push({name, path})` | 控制其他子应用跳转 |
| `window.microApp.router.getBaseAppRouter()` | 获取主应用路由对象 |
| `window.microApp.pureCreateElement(tag)` | 创建纯净元素 |
| `window.microApp.removeDomScope(force)` | 解除元素绑定 |
| `window.microApp.location` | 获取子应用 location |
| `window.rawWindow` | 获取真实 window |
| `window.rawDocument` | 获取真实 document |
| `window.mount` | UMD 模式渲染函数 |
| `window.unmount` | 卸载函数 |
| `window.onmount` | 渲染事件回调 |
| `window.onunmount` | 卸载事件回调 |

---

## 8. 部署

核心原则：保持开发环境和线上环境的 publicPath 一致。

```javascript
// vue.config.js
module.exports = {
  outputDir: 'my-app',
  publicPath: '/my-app/', // 开发和生产保持一致
}
```

**Nginx 配置要点：**
- 所有子应用资源目录需设置 `Access-Control-Allow-Origin: *`
- history 路由模式需配置 `try_files $uri $uri/ /index.html`
- 静态资源建议设置缓存：`Cache-Control max-age=7776000`

**MeterSphere 场景：**
- 子应用通过 Gateway 反向代理访问，路径规则 `/{serviceId}/`
- 开发环境子应用端口 = 后端端口 - 4000
- 生产环境通过 `window.location.origin + '/' + serviceId` 访问

---

## 9. 常见问题

**Q1：子应用一定要支持跨域吗？**
是的。开发环境在 devServer 设置 headers，生产环境通过 nginx 配置。

**Q2：兼容性如何？**
依赖 CustomElements 和 Proxy。PC 端除 IE 外基本兼容，移动端 iOS 10+、Android 5+。

**Q3：Vue 中报 `Unknown custom element: <micro-app>`**
Vue 2：`Vue.config.ignoredElements = ['micro-app']`
Vue 3：在 vue-loader 中配置 `isCustomElement`

**Q4：`an app named xx already exists`**
name 名称冲突，确保每个子应用 name 唯一。

**Q5：主应用样式影响子应用**
micro-app 只隔离子应用样式不影响外部，但主应用样式仍会影响子应用。通过约定前缀或 CSS Modules 解决。

**Q6：子应用中 `xxx is not defined`**
沙箱中顶层变量不泄漏为全局变量。将 `var xx =` 改为 `window.xx =`，或通过插件系统修改。

**Q7：子应用通过 a 标签下载文件失败**
跨域时无法通过 download 属性下载，需转换为 blob 下载或将文件放到主应用域名下。

**Q8：iconfont 图标冲突**
修改冲突应用的 font-family 名称和对应的 class 名。

---

## 10. MeterSphere 迁移要点

基于以上 micro-app 文档，结合 MeterSphere 项目特点，迁移核心要点如下：

### 10.1 qiankun vs micro-app 对照表

| 维度 | qiankun（当前） | micro-app（目标） |
|------|----------------|------------------|
| 子应用注册 | `registerMicroApps()` + `start()` | `microApp.start()` + `<micro-app>` 标签 |
| 子应用打包 | 必须 UMD（`libraryTarget: 'umd'`） | 无要求，标准打包即可 |
| 生命周期导出 | `bootstrap/mount/unmount/update` | `window.mount/unmount`（可选 UMD 模式） |
| 环境判断 | `window.__POWERED_BY_QIANKUN__` | `window.__MICRO_APP_ENVIRONMENT__` |
| publicPath | `window.__INJECTED_PUBLIC_PATH_BY_QIANKUN__` | `window.__MICRO_APP_PUBLIC_PATH__` |
| 按需加载 | `loadMicroApp()` | `<micro-app>` 标签内联 |
| 跨应用通信 | EventBus props + globalState | `data` 属性 + `dispatch` + `setGlobalData` |
| 路由激活 | `activeRule` 函数 | 主应用 Vue Router 控制标签显隐 |
| Vite 支持 | ❌ 不支持 | ✅ iframe 沙箱模式 |

### 10.2 主应用改造清单

1. 安装 `@micro-zoe/micro-app`，调用 `microApp.start()`
2. Vue 2 设置 `Vue.config.ignoredElements = ['micro-app']`
3. App.vue 中用 `<micro-app>` 标签替代 `<div id="micro-app">`
4. 用 `microApp.setData()` / `setGlobalData()` 替代 EventBus props 传递
5. 用 `@datachange` 事件替代 `$EventBus.$on`
6. MicroApp.vue（loadMicroApp）→ MicroAppWrapper.vue（`<micro-app>` 标签）

### 10.3 子应用改造清单（每个模块）

1. `public-path.js`：`__POWERED_BY_QIANKUN__` → `__MICRO_APP_ENVIRONMENT__`
2. `main.js`：移除 qiankun 生命周期导出，添加 `window.unmount` 和 `window.mount`（UMD 模式）
3. `vue.config.js`：移除 `library` 和 `libraryTarget: 'umd'`，保留 `chunkLoadingGlobal`
4. 保留 CORS 头配置

### 10.4 通信机制迁移

| 场景 | qiankun 方式 | micro-app 方式 |
|------|-------------|---------------|
| 主→子 传数据 | props 传递 EventBus | `<micro-app :data='xxx'>` 或 `microApp.setData()` |
| 子→主 传数据 | `$EventBus.$emit` | `window.microApp.dispatch()` |
| 广播所有子应用 | 遍历 EventBus | `microApp.setGlobalData()` |
| 全局状态 | `initGlobalState` | `setGlobalData` / `getGlobalData` |

### 10.5 渐进式迁移策略

1. 双模式并行：通过配置表标记每个模块是否已迁移
2. 已迁移模块用 `<micro-app>` 加载，未迁移模块继续用 qiankun
3. 按复杂度分批迁移：简单模块 → 中等模块 → 含按需加载的复杂模块
4. 全部迁移完成后清理 qiankun 代码和依赖

### 10.6 特别注意

- MeterSphere 使用 hash 路由，micro-app 默认的 search 路由模式兼容 hash
- test-track 模块有 10+ 处 MicroApp.vue 引用需要替换
- api-test 有多个 pages 入口，非主入口不受影响
- 开发环境端口计算规则保持不变（后端端口 - 4000）
- 生产环境通过 Gateway 反向代理，路径规则 `/{serviceId}/` 不变
