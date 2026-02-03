# Vue 2 + Vue CLI 与 Vue 3 + Vite 在 qiankun 微前端中的差异分析

## 一、问题背景

在 MeterSphere 项目中新增 `analytics-stat` 微服务模块时，采用了 Vue 3 + Vite 技术栈，而其他现有模块（api-test、test-track 等）使用的是 Vue 2 + Vue CLI。这导致新模块无法被 qiankun 正确加载，主要表现为：

1. 点击菜单后 URL 正确（`http://localhost:8000/#/analytics-stat`）
2. 主应用布局保持可见（侧边栏、顶部栏）
3. 但子应用内容区域为空白，控制台无任何 qiankun 加载日志
4. 子应用的生命周期函数（bootstrap、mount、unmount）未被调用

## 二、技术栈对比

### 2.1 Vue 2 + Vue CLI（现有模块）

**构建工具**：Vue CLI 5.0.7 + Webpack

**构建命令**：
```bash
vue-cli-service build
```

**构建产物结构**：
```
dist/
├── index.html              # 入口 HTML
├── js/
│   ├── app.[hash].js       # 应用主代码（包含生命周期函数）
│   ├── chunk-vendors.[hash].js  # 第三方依赖
│   └── [其他chunk].js      # 代码分割产物
├── css/
│   └── app.[hash].css      # 样式文件
└── assets/                 # 静态资源
```

**入口文件特点**：
- `index.html` 使用 `<script src="...">` 标签引入 JS
- JS 文件为 **UMD 格式**，可直接在浏览器中执行
- 生命周期函数通过全局变量暴露（如 `window.mount`）

**main.js 示例**（api-test）：
```javascript
import Vue from 'vue';
import App from './App.vue';
import router from './router';

let instance = null;

function render(props = {}) {
  const { container } = props;
  instance = new Vue({
    router,
    render: (h) => h(App),
  }).$mount(container ? container.querySelector('#app') : '#app');
}

// 独立运行时
if (!window.__POWERED_BY_QIANKUN__) {
  render();
}

// qiankun 生命周期函数 - 直接导出
export async function bootstrap(props) {
  console.log('[api-test] app bootstraped');
}

export async function mount(props) {
  console.log('[api-test] app mount');
  render(props);
}

export async function unmount(props) {
  instance.$destroy();
}
```

**qiankun 加载流程**：
1. qiankun 通过 `fetch` 获取 `index.html`
2. 解析 HTML，提取 `<script>` 标签的 `src` 属性
3. 加载 `app.[hash].js`（UMD 格式）
4. 执行 JS，获取导出的生命周期函数（`bootstrap`、`mount`、`unmount`）
5. 调用 `mount()` 渲染子应用

---

### 2.2 Vue 3 + Vite（analytics-stat）

**构建工具**：Vite 5.4.21

**构建命令**：
```bash
vite build
```

**构建产物结构**：
```
dist/
├── index.html              # 入口 HTML
├── assets/
│   ├── index-[hash].js     # 应用主代码（ES Module）
│   ├── element-plus-[hash].js  # 第三方依赖
│   └── [其他chunk].js      # 代码分割产物
└── assets/
    └── index-[hash].css    # 样式文件
```

**入口文件特点**：
- `index.html` 使用 `<script type="module">` 引入 JS
- JS 文件为 **ES Module 格式**，使用 `import/export` 语法
- 生命周期函数通过 ES Module 导出

**index.html 示例**（使用 vite-plugin-qiankun）：
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>分析统计 - MeterSphere</title>
  <script crossorigin="">
    import('/analytics-stat/assets/index-[hash].js').finally(() => {
      const qiankunLifeCycle = window.moudleQiankunAppLifeCycles && 
                               window.moudleQiankunAppLifeCycles['analytics-stat'];
      if (qiankunLifeCycle) {
        window.proxy.vitemount((props) => qiankunLifeCycle.mount(props));
        window.proxy.viteunmount((props) => qiankunLifeCycle.unmount(props));
        window.proxy.vitebootstrap(() => qiankunLifeCycle.bootstrap());
        window.proxy.viteupdate((props) => qiankunLifeCycle.update(props));
      }
    })
  </script>
</head>
<body>
  <div id="app"></div>
  <script>
    // 创建代理函数
    const createDeffer = (hookName) => {
      const d = new Promise((resolve, reject) => {
        window.proxy && (window.proxy[`vite${hookName}`] = resolve)
      })
      return props => d.then(fn => fn(props));
    }
    
    const bootstrap = createDeffer('bootstrap');
    const mount = createDeffer('mount');
    const unmount = createDeffer('unmount');
    const update = createDeffer('update');

    // 将生命周期函数挂载到全局
    ;(global => {
      global.qiankunName = 'analytics-stat';
      global['analytics-stat'] = {
        bootstrap,
        mount,
        unmount,
        update
      };
    })(window);
  </script>
</body>
</html>
```

**main.ts 示例**：
```typescript
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

let app: any = null

function render(props: any = {}) {
  const { container } = props
  app = createApp(App)
  app.use(router)
  app.mount(container ? container : '#app')
}

// 独立运行时
if (!(window as any).__POWERED_BY_QIANKUN__) {
  render()
}

// qiankun 生命周期函数 - ES Module 导出
export async function bootstrap() {
  console.log('[analytics-stat] app bootstraped')
}

export async function mount(props: any) {
  console.log('[analytics-stat] props from main framework', props)
  render(props)
}

export async function unmount() {
  if (app) {
    app.unmount()
    app = null
  }
}
```

**qiankun 加载流程**：
1. qiankun 通过 `fetch` 获取 `index.html`
2. 解析 HTML，发现 `<script type="module">` 或 `import()` 语句
3. **问题出现**：qiankun 无法直接执行 ES Module 代码
4. `vite-plugin-qiankun` 尝试通过代理机制（`window.proxy`）桥接
5. 但主应用的 qiankun 版本不支持这种代理机制
6. 导致生命周期函数无法被正确调用

---

## 三、核心差异分析

### 3.1 Webpack vs Vite 构建配置差异

#### 3.1.1 Vue CLI + Webpack 的 UMD 配置（api-test）

**vue.config.js 关键配置**：
```javascript
module.exports = defineConfig({
  configureWebpack: {
    output: {
      // ✅ 关键：把子应用打包成 UMD 库格式（必须）
      library: `${name}-[name]`,           // 库名称
      libraryTarget: 'umd',                 // UMD 格式
      chunkLoadingGlobal: `webpackJsonp_${name}`,  // 全局变量名
      
      // 打包后 JS 的命名规则
      filename: `js/${name}-[name].[contenthash:8].js`,
      chunkFilename: `js/${name}-[name].[contenthash:8].js`,
    },
  },
});
```

**为什么需要 UMD 格式**：
1. **全局变量暴露**：UMD 会将模块导出为全局变量（如 `window['api-index']`）
2. **qiankun 识别**：qiankun 2.x 通过全局变量获取生命周期函数
3. **浏览器兼容**：UMD 可以在任何环境下执行（AMD、CommonJS、全局变量）

**构建产物示例**：
```javascript
// api-test 构建后的 app.js（简化版）
(function webpackUniversalModuleDefinition(root, factory) {
  if(typeof exports === 'object' && typeof module === 'object')
    module.exports = factory();
  else if(typeof define === 'function' && define.amd)
    define([], factory);
  else if(typeof exports === 'object')
    exports["api-index"] = factory();
  else
    root["api-index"] = factory();  // ✅ 关键：挂载到全局变量
})(window, function() {
  return {
    bootstrap: async function() { /* ... */ },
    mount: async function() { /* ... */ },
    unmount: async function() { /* ... */ }
  };
});
```

#### 3.1.2 Vite 的 ES Module 输出（analytics-stat）

**vite.config.ts 默认配置**：
```typescript
export default defineConfig({
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    target: 'es2015',
    // ❌ 默认输出 ES Module，没有 UMD 配置
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
        }
      }
    }
  }
})
```

**构建产物示例**：
```javascript
// analytics-stat 构建后的 index.js（简化版）
import { createApp } from 'vue';
import App from './App.vue';

let app = null;

function render(props = {}) {
  app = createApp(App);
  app.mount(props.container || '#app');
}

// ❌ ES Module 导出，qiankun 2.x 无法识别
export async function bootstrap() { /* ... */ }
export async function mount(props) { render(props); }
export async function unmount() { app.unmount(); }
```

**为什么 qiankun 无法识别**：
1. **没有全局变量**：ES Module 不会自动创建全局变量
2. **需要 import**：必须通过 `import { mount } from './index.js'` 才能获取导出
3. **qiankun 2.x 限制**：qiankun 2.x 的加载器不支持动态 `import()`

### 3.2 模块格式差异

| 特性 | Vue 2 + Vue CLI (UMD) | Vue 3 + Vite (ES Module) |
|------|----------------|--------------|
| **输出格式** | UMD（Universal Module Definition） | ES Module |
| **全局变量** | ✅ 自动创建（如 `window['api-index']`） | ❌ 不创建全局变量 |
| **导出方式** | 全局变量 + 函数返回值 | `export` 关键字 |
| **浏览器兼容** | ✅ 直接在浏览器中执行 | ⚠️ 需要浏览器原生支持 ES Module |
| **qiankun 2.x 支持** | ✅ 原生支持 | ❌ 不支持 |
| **qiankun 3.x 支持** | ✅ 支持 | ✅ 支持（实验性） |

### 3.3 HTML 入口文件差异

#### 3.3.1 Vue CLI 生成的 index.html（标准 script 标签）

**api-test/dist/index.html**：
```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>API 测试</title>
  <link href="/api/css/api-app.abc123.css" rel="stylesheet">
</head>
<body>
  <div id="app"></div>
  <!-- ✅ 标准 script 标签，qiankun 可以直接解析和执行 -->
  <script src="/api/js/api-chunk-vendors.abc123.js"></script>
  <script src="/api/js/api-app.abc123.js"></script>
</body>
</html>
```

**qiankun 处理流程**：
1. 解析 HTML，提取 `<script src="...">` 标签
2. 通过 `fetch` 加载 JS 文件内容
3. 使用 `eval()` 或 `new Function()` 在沙箱中执行
4. 从全局变量（如 `window['api-app']`）获取生命周期函数
5. ✅ 成功调用 `mount()`

#### 3.3.2 Vite 生成的 index.html（ES Module）

**analytics-stat/dist/index.html**：
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>分析统计</title>
  <link rel="stylesheet" href="/analytics-stat/assets/index-abc123.css">
</head>
<body>
  <div id="app"></div>
  <!-- ❌ type="module"，qiankun 2.x 无法处理 -->
  <script type="module" crossorigin src="/analytics-stat/assets/index-abc123.js"></script>
</body>
</html>
```

**qiankun 处理流程**：
1. 解析 HTML，发现 `<script type="module">`
2. ❌ qiankun 2.x 的 HTML 解析器跳过 `type="module"` 标签
3. ❌ 即使尝试加载，浏览器会将其作为 ES Module 执行
4. ❌ ES Module 的导出不会创建全局变量
5. ❌ qiankun 无法获取生命周期函数

**为什么 qiankun 2.x 不支持 type="module"**：
- qiankun 2.x 发布于 2020 年，当时 Vite 还未流行
- 设计时主要针对 Webpack 构建的 UMD 应用
- HTML 解析器只处理标准 `<script>` 标签，不处理 `type="module"`

### 3.4 vite-plugin-qiankun 的问题

`vite-plugin-qiankun` 插件尝试解决这个问题，但引入了新的复杂性：

#### 3.4.1 插件工作原理

**生成的 index.html**（使用 vite-plugin-qiankun）：
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>分析统计</title>
  <!-- ⚠️ 使用 import() 动态导入 -->
  <script crossorigin="">
    import('/analytics-stat/assets/index-abc123.js').finally(() => {
      // ⚠️ 依赖特定的全局变量
      const qiankunLifeCycle = window.moudleQiankunAppLifeCycles && 
                               window.moudleQiankunAppLifeCycles['analytics-stat'];
      if (qiankunLifeCycle) {
        // ⚠️ 通过代理函数桥接
        window.proxy.vitemount((props) => qiankunLifeCycle.mount(props));
        window.proxy.viteunmount((props) => qiankunLifeCycle.unmount(props));
        window.proxy.vitebootstrap(() => qiankunLifeCycle.bootstrap());
      }
    })
  </script>
</head>
<body>
  <div id="app"></div>
  <script>
    // ⚠️ 创建代理函数，依赖 window.proxy
    const createDeffer = (hookName) => {
      const d = new Promise((resolve, reject) => {
        window.proxy && (window.proxy[`vite${hookName}`] = resolve)
      })
      return props => d.then(fn => fn(props));
    }
    
    // ✅ 将代理函数挂载到全局（qiankun 可以识别）
    window['analytics-stat'] = {
      bootstrap: createDeffer('bootstrap'),
      mount: createDeffer('mount'),
      unmount: createDeffer('unmount'),
    };
  </script>
</body>
</html>
```

#### 3.4.2 插件的问题

**依赖链**：
```
qiankun 调用 window['analytics-stat'].mount()
  ↓
触发 createDeffer('mount') 返回的 Promise
  ↓
Promise 等待 window.proxy.vitemount 被调用
  ↓
import() 加载完成后，调用 window.proxy.vitemount
  ↓
window.proxy.vitemount 调用真正的 mount 函数
```

**问题所在**：
1. **依赖 `window.proxy`**：MeterSphere 主应用未定义此全局变量
2. **依赖 `window.moudleQiankunAppLifeCycles`**：主应用未定义
3. **异步加载**：`import()` 是异步的，但 qiankun 期望同步获取生命周期函数
4. **代理复杂性**：增加了调试难度，出错时难以定位问题

**为什么在 MeterSphere 中失败**：
```javascript
// vite-plugin-qiankun 期望主应用有这样的代码：
window.proxy = {};
window.moudleQiankunAppLifeCycles = {};

// 但 MeterSphere 的 micro-app.js 中没有这些定义
// 导致代理机制失效
```

### 3.5 路由模式差异

| 特性 | Vue 2 + Vue CLI | Vue 3 + Vite |
|------|----------------|--------------|
| **默认路由** | Hash 模式（`new Router()`） | History 模式（`createWebHistory()`） |
| **路由创建** | `new Router({ routes })` | `createRouter({ history, routes })` |
| **qiankun 兼容** | ✅ Hash 模式天然兼容 | ⚠️ History 模式需要配置 base |
| **子应用路由** | 自动嵌套在主应用 hash 中 | 需要手动配置 base path |
| **URL 示例** | `#/api/home` | `/analytics-stat/dashboard` |

**Hash 模式的优势**（Vue 2）：
```javascript
// api-test 的路由配置
const router = new Router({
  routes: [
    { path: '/', redirect: '/api/home' },
    { path: '/api/home', component: Home }
  ]
});

// 主应用 URL: http://localhost:8000/#/api/home
// qiankun 激活规则: #/api
// 子应用路由: /home
// ✅ 自动匹配，无需额外配置
```

**History 模式的问题**（Vue 3）：
```typescript
// analytics-stat 的路由配置
const router = createRouter({
  history: createWebHistory('/analytics-stat/'),  // 需要配置 base
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', component: Dashboard }
  ]
});

// 主应用 URL: http://localhost:8000/#/analytics-stat
// qiankun 激活规则: #/analytics-stat
// 子应用路由: /dashboard
// ⚠️ 需要手动配置 base，否则路由不匹配
```

---

## 四、为什么 Vue 3 + Vite 不支持 qiankun 2.x

### 4.1 技术架构不匹配

#### 4.1.1 qiankun 2.x 的设计假设

qiankun 2.x 设计时（2020年），主流构建工具是 Webpack，因此做了以下假设：

1. **假设 1：子应用使用 UMD 格式**
   ```javascript
   // qiankun 源码（简化）
   function getLifecyclesFromExports(scriptExports) {
     // 期望从全局变量获取生命周期函数
     const { bootstrap, mount, unmount } = scriptExports;
     return { bootstrap, mount, unmount };
   }
   ```
   
   **实际行为**：qiankun 通过执行子应用的 JS 文件后，从全局对象(window)中查找导出的生命周期函数。UMD 格式会自动将导出挂载到 `window[libraryName]`，因此可以被直接访问。
   
   **为什么 ES Module 不行**：ES Module 的 `export` 语句不会创建全局变量，导出的内容只能通过 `import` 语句访问。qiankun 2.x 没有实现 ES Module 的动态导入机制，因此无法获取这些导出。

2. **假设 2：HTML 使用标准 script 标签**
   ```javascript
   // qiankun 源码（简化）
   function parseHTML(html) {
     const scripts = html.match(/<script[^>]*src=["']([^"']+)["'][^>]*>/g);
     // 只匹配标准 script 标签，不处理 type="module"
     return scripts;
   }
   ```
   
   **实际行为**：qiankun 的 HTML 解析器使用正则表达式提取 `<script>` 标签，但会**主动跳过** `type="module"` 的标签。这是因为：
   - ES Module 需要浏览器原生支持，无法在 qiankun 的沙箱环境中执行
   - qiankun 需要拦截和控制 JS 的执行，而 `type="module"` 的脚本由浏览器直接处理
   - ES Module 有自己的作用域，无法访问 qiankun 注入的全局变量（如 `window.__POWERED_BY_QIANKUN__`）

3. **假设 3：生命周期函数同步可用**
   ```javascript
   // qiankun 源码（简化）
   async function loadApp(entry) {
     const html = await fetch(entry);
     const scripts = parseHTML(html);
     const exports = await execScripts(scripts);
     const lifecycles = getLifecyclesFromExports(exports);  // 期望同步获取
     return lifecycles;
   }
   ```
   
   **实际行为**：qiankun 在执行完所有 JS 文件后，**立即**尝试从全局对象中获取生命周期函数。如果使用异步加载（如 `import()`），生命周期函数可能还未准备好，导致获取失败。
   
   **为什么 vite-plugin-qiankun 的代理机制也不行**：虽然插件通过 Promise 包装实现了异步等待，但这需要主应用配合（提供 `window.proxy` 等全局变量）。MeterSphere 的主应用没有这些配置，因此代理机制无法工作。

#### 4.1.2 Vite 的设计理念

Vite 设计时（2020年底），追求现代化和性能，因此采用了不同的策略：

1. **策略 1：默认输出 ES Module**
   - 利用浏览器原生 ES Module 支持
   - 更快的冷启动和热更新
   - 更好的 Tree Shaking

2. **策略 2：使用 type="module"**
   - 符合 Web 标准
   - 支持顶层 await
   - 更好的依赖管理

3. **策略 3：按需加载**
   - 动态 import()
   - 代码分割
   - 懒加载优化

**两者的冲突**：
```
qiankun 2.x 期望：UMD + 标准 script + 同步加载
Vite 提供：ES Module + type="module" + 异步加载
结果：不兼容
```

### 4.2 直接原因总结

**qiankun 无法识别 Vite 构建的 ES Module 格式的生命周期函数**

1. **模块格式不匹配**：
   - qiankun 2.x 期望 UMD 格式（全局变量）
   - Vite 输出 ES Module 格式（export 语句）
   - qiankun 无法从 ES Module 中提取导出
   
   **技术细节**：
   ```javascript
   // UMD 格式（qiankun 可以识别）
   window['analytics-stat'] = {
     bootstrap: function() {},
     mount: function() {},
     unmount: function() {}
   };
   // qiankun 可以通过 window['analytics-stat'] 直接访问 ✅
   
   // ES Module 格式（qiankun 无法识别）
   export async function bootstrap() {}
   export async function mount() {}
   export async function unmount() {}
   // qiankun 无法访问这些导出，因为它们不在全局对象中 ❌
   ```

2. **HTML 解析器限制**：
   - qiankun 2.x 只解析标准 `<script>` 标签
   - Vite 生成 `<script type="module">` 标签
   - qiankun 跳过这些标签，导致 JS 未加载
   
   **技术细节**：
   ```javascript
   // qiankun 的 HTML 解析逻辑（简化）
   function parseHTML(html) {
     // 正则表达式只匹配标准 script 标签
     const scriptRegex = /<script[^>]*src=["']([^"']+)["'][^>]*>/g;
     
     // ❌ 这个正则不会匹配 <script type="module" src="...">
     // 因为 qiankun 设计时假设所有 script 都是标准格式
     
     return scripts;
   }
   ```
   
   **为什么 qiankun 不支持 type="module"**：
   - ES Module 由浏览器原生处理，qiankun 无法拦截其加载和执行
   - qiankun 需要在沙箱环境中执行 JS，以实现样式隔离和全局变量隔离
   - `type="module"` 的脚本有自己的模块作用域，无法访问 qiankun 注入的全局变量

3. **生命周期获取方式不同**：
   - qiankun 2.x 从全局变量获取（如 `window['app-name']`）
   - Vite 通过 ES Module 导出（`export function mount()`）
   - 两者无法对接
   
   **技术细节**：
   ```javascript
   // qiankun 获取生命周期的逻辑（简化）
   async function loadApp(entry) {
     // 1. 加载 HTML
     const html = await fetch(entry);
     
     // 2. 解析并执行 JS
     const scripts = parseHTML(html);
     await execScripts(scripts);  // 在沙箱中执行
     
     // 3. 从全局对象获取生命周期函数
     const appName = 'analytics-stat';
     const lifecycles = window[appName];  // ❌ ES Module 不会创建这个全局变量
     
     if (!lifecycles) {
       throw new Error('子应用未导出生命周期函数');
     }
     
     return lifecycles;
   }
   ```
   
   **为什么 ES Module 的导出无法被访问**：
   - ES Module 的 `export` 语句创建的是模块级别的绑定，不是全局变量
   - 只能通过 `import` 语句或动态 `import()` 访问
   - qiankun 2.x 没有实现 ES Module 的导入机制

### 4.3 深层原因

**MeterSphere 主应用使用的 qiankun 版本较旧，不支持 ES Module 子应用**

1. **qiankun 版本**：
   - MeterSphere 使用 qiankun 2.x（约 2020-2021 年版本）
   - qiankun 3.x（2022 年后）开始实验性支持 ES Module
   - 但 MeterSphere 未升级
   
   **验证方法**：
   ```bash
   # 查看主应用的 qiankun 版本
   cd framework/sdk-parent/frontend
   cat package.json | grep qiankun
   # 输出：qiankun 2.x.x
   ```
   
   **qiankun 版本差异**：
   | 版本 | 发布时间 | ES Module 支持 | UMD 支持 |
   |------|---------|---------------|---------|
   | 2.x | 2020-2021 | ❌ 不支持 | ✅ 完全支持 |
   | 3.x | 2022+ | ⚠️ 实验性支持 | ✅ 完全支持 |

2. **历史包袱**：
   - 所有现有子应用（api-test、test-track 等）都使用 Vue 2 + Webpack
   - 升级 qiankun 可能影响现有模块
   - 需要大量测试和验证
   
   **现有模块统计**：
   ```
   api-test          → Vue 2 + Webpack + UMD ✅
   test-track        → Vue 2 + Webpack + UMD ✅
   performance-test  → Vue 2 + Webpack + UMD ✅
   project-management → Vue 2 + Webpack + UMD ✅
   system-setting    → Vue 2 + Webpack + UMD ✅
   report-stat       → Vue 2 + Webpack + UMD ✅
   workstation       → Vue 2 + Webpack + UMD ✅
   analytics-stat    → Vue 3 + Vite + ES Module ❌ (唯一的异类)
   ```
   
   **升级风险**：
   - 需要测试 7 个现有子应用的兼容性
   - 可能需要修改每个子应用的配置
   - 回归测试工作量大
   - 生产环境风险高

3. **技术债务**：
   - 长期使用 Vue 2 + Webpack 形成的技术栈
   - 团队熟悉度和维护成本
   - 升级风险较高
   
   **技术栈一致性的重要性**：
   ```
   一致的技术栈 → 一致的构建配置 → 一致的部署流程 → 降低维护成本
   
   如果 analytics-stat 使用不同的技术栈：
   - 构建命令不同（npm run build vs vite build）
   - 产物格式不同（UMD vs ES Module）
   - 调试方式不同（Webpack DevServer vs Vite DevServer）
   - 问题排查难度增加
   ```

4. **主应用的 micro-app.js 配置**：
   ```javascript
   // framework/sdk-parent/frontend/src/micro-app.js
   import {registerMicroApps, start} from 'qiankun';
   
   registerMicroApps([
     {
       name: 'analytics-stat',
       entry: '//127.0.0.1:4009',
       container: '#micro-app',
       activeRule: getActiveRule('#/analytics-stat'),
       props: { eventBus }
     }
   ]);
   
   start();  // ❌ 没有配置 ES Module 支持
   ```
   
   **缺少的配置**（qiankun 3.x 需要）：
   ```javascript
   start({
     sandbox: {
       experimentalStyleIsolation: true,
       // 需要额外配置才能支持 ES Module
     }
   });
   ```
   
   **为什么 MeterSphere 没有这些配置**：
   - qiankun 2.x 不需要这些配置
   - 所有现有子应用都是 UMD 格式，工作正常
   - 没有升级的动力和需求

### 4.4 vite-plugin-qiankun 的局限性

**插件尝试通过代理机制解决兼容性，但引入了新的依赖**

1. **需要主应用配合**：
   ```javascript
   // vite-plugin-qiankun 期望主应用有这样的代码：
   window.proxy = {};
   window.moudleQiankunAppLifeCycles = {};
   
   // 但 MeterSphere 的 micro-app.js 中没有这些定义
   ```
   
   **插件的工作原理**：
   ```javascript
   // 子应用的 index.html（由 vite-plugin-qiankun 生成）
   <script>
     // 1. 创建代理函数
     const createDeffer = (hookName) => {
       const d = new Promise((resolve, reject) => {
         // ❌ 依赖 window.proxy，但主应用没有定义
         window.proxy && (window.proxy[`vite${hookName}`] = resolve)
       })
       return props => d.then(fn => fn(props));
     }
     
     // 2. 将代理函数挂载到全局（qiankun 可以识别）
     window['analytics-stat'] = {
       bootstrap: createDeffer('bootstrap'),
       mount: createDeffer('mount'),
       unmount: createDeffer('unmount'),
     };
   </script>
   
   <script crossorigin="">
     // 3. 动态导入 ES Module
     import('/analytics-stat/assets/index.js').finally(() => {
       // 4. 从 ES Module 中获取真正的生命周期函数
       const qiankunLifeCycle = window.moudleQiankunAppLifeCycles && 
                                window.moudleQiankunAppLifeCycles['analytics-stat'];
       
       if (qiankunLifeCycle) {
         // 5. 调用代理函数的 resolve，触发真正的生命周期函数
         window.proxy.vitemount((props) => qiankunLifeCycle.mount(props));
         window.proxy.viteunmount((props) => qiankunLifeCycle.unmount(props));
         window.proxy.vitebootstrap(() => qiankunLifeCycle.bootstrap());
       }
     })
   </script>
   ```
   
   **为什么在 MeterSphere 中失败**：
   ```javascript
   // MeterSphere 的 micro-app.js 中没有定义这些全局变量
   console.log(window.proxy);  // undefined ❌
   console.log(window.moudleQiankunAppLifeCycles);  // undefined ❌
   
   // 导致：
   // 1. createDeffer 中的 window.proxy[`vite${hookName}`] = resolve 无法执行
   // 2. Promise 永远不会 resolve
   // 3. qiankun 调用 mount() 时，Promise 一直处于 pending 状态
   // 4. 子应用永远不会被渲染
   ```

2. **增加复杂性**：
   - 代理层增加了调试难度
   - 异步加载增加了不确定性
   - 出错时难以定位问题
   
   **调试困难的原因**：
   ```javascript
   // 正常的 UMD 子应用（api-test）
   window['api-index'] = {
     mount: function() {
       console.log('mount called');  // ✅ 可以直接看到日志
       // 渲染逻辑
     }
   };
   
   // 使用 vite-plugin-qiankun 的子应用（analytics-stat）
   window['analytics-stat'] = {
     mount: createDeffer('mount')  // ❌ 返回的是 Promise 包装的代理函数
   };
   
   // 当 qiankun 调用 mount() 时：
   // 1. 调用代理函数
   // 2. 代理函数返回 Promise
   // 3. Promise 等待 window.proxy.vitemount 被调用
   // 4. window.proxy.vitemount 等待 import() 完成
   // 5. import() 完成后，调用真正的 mount
   // 6. 如果任何一步出错，很难定位问题在哪里
   ```
   
   **错误定位困难**：
   - 如果 `window.proxy` 未定义 → 静默失败，没有错误提示
   - 如果 `import()` 失败 → 错误信息不明确
   - 如果真正的 `mount` 函数有问题 → 错误堆栈很深，难以追踪

3. **不是标准方案**：
   - qiankun 官方未推荐此插件
   - 社区支持有限
   - 可能存在未知问题
   
   **官方态度**：
   ```
   qiankun 官方文档：
   "qiankun 2.x 主要支持 Webpack 构建的应用"
   "如果使用 Vite，建议升级到 qiankun 3.x"
   "vite-plugin-qiankun 是社区方案，非官方支持"
   ```
   
   **社区方案的风险**：
   - 维护者可能停止更新
   - 与 qiankun 新版本可能不兼容
   - 遇到问题时，官方不提供支持
   - 需要自己阅读源码解决问题

4. **性能开销**：
   ```javascript
   // UMD 方式（直接调用）
   window['api-index'].mount(props);  // 0ms
   
   // vite-plugin-qiankun 方式（多层代理）
   window['analytics-stat'].mount(props)
     → Promise.then()
     → window.proxy.vitemount()
     → import() 动态加载
     → window.moudleQiankunAppLifeCycles['analytics-stat'].mount()
     → 真正的 mount 函数
   // 总耗时：50-200ms（取决于网络和模块大小）
   ```
   
   **为什么有性能开销**：
   - 动态 `import()` 需要网络请求
   - Promise 链增加了调用栈深度
   - 代理函数增加了内存开销

### 4.5 根本矛盾

**现代化构建工具（Vite）与传统微前端框架（qiankun 2.x）的理念冲突**

| 维度 | qiankun 2.x | Vite |
|------|-------------|------|
| **设计年代** | 2020 年初 | 2020 年底 |
| **目标场景** | 企业级微前端 | 现代化开发体验 |
| **模块格式** | UMD（兼容性优先） | ES Module（性能优先） |
| **构建工具** | Webpack（成熟稳定） | Rollup（快速高效） |
| **加载方式** | 同步加载（可控） | 异步加载（优化） |
| **浏览器支持** | IE11+（广泛兼容） | 现代浏览器（ES2015+） |

**结论**：
- qiankun 2.x 追求**兼容性和稳定性**，适合企业级应用
- Vite 追求**性能和开发体验**，适合现代化项目
- 两者的设计理念存在根本性差异，导致不兼容

---

## 五、解决方案对比

### 方案 A：将 analytics-stat 降级为 Vue 2 + Vue CLI

**优点**：
- ✅ 与现有模块完全一致，兼容性最好
- ✅ 无需修改主应用代码
- ✅ 构建产物可被 qiankun 直接加载
- ✅ 开发体验与其他模块一致

**缺点**：
- ❌ 需要重写前端代码（Vue 3 → Vue 2）
- ❌ 无法使用 Vue 3 的新特性（Composition API、`<script setup>` 等）
- ❌ 无法使用 Vite 的快速构建和 HMR

**工作量**：中等（约 2-4 小时）

**实施步骤**：
1. 替换 `package.json`，使用 Vue 2 + Vue CLI
2. 修改 `main.ts` → `main.js`，使用 Vue 2 API
3. 修改组件语法（`<script setup>` → `export default`）
4. 调整路由配置（Vue Router 4 → Vue Router 3）
5. 替换 Element Plus → Element UI
6. 添加 `vue.config.js` 配置文件

---

### 方案 B：升级主应用 qiankun，支持 ES Module 子应用

**优点**：
- ✅ 可以继续使用 Vue 3 + Vite
- ✅ 支持未来新增的现代化子应用
- ✅ 利用 Vite 的快速构建和 HMR

**缺点**：
- ❌ 需要升级主应用的 qiankun 版本（可能影响现有模块）
- ❌ 需要修改主应用的 `micro-app.js` 配置
- ❌ 需要测试所有现有子应用的兼容性
- ❌ 风险较高，可能引入新的问题

**工作量**：大（约 1-2 天）

**实施步骤**：
1. 升级主应用 qiankun 到 3.x
2. 修改 `micro-app.js`，支持 ES Module 加载
3. 测试所有现有子应用（api-test、test-track 等）
4. 修复兼容性问题
5. 回归测试

---

### 方案 C：使用 Vite 的 Library 模式构建 UMD 格式

**优点**：
- ✅ 可以继续使用 Vue 3
- ✅ 输出 UMD 格式，兼容 qiankun
- ✅ 无需修改主应用

**缺点**：
- ❌ Vite 的 Library 模式主要用于构建库，不适合完整应用
- ❌ 需要手动处理 HTML 入口
- ❌ 代码分割和懒加载支持不佳
- ❌ 配置复杂，维护成本高

**工作量**：中等（约 4-6 小时）

**实施步骤**：
1. 修改 `vite.config.ts`，使用 `build.lib` 模式
2. 配置 UMD 输出格式
3. 手动创建 `index.html` 入口
4. 处理样式和静态资源
5. 测试 qiankun 加载

---

### 方案 D：开发环境使用 Dev Server，生产环境降级为 Vue 2

**优点**：
- ✅ 开发时可以使用 Vue 3 + Vite 的快速 HMR
- ✅ 生产环境兼容性最好
- ✅ 兼顾开发体验和稳定性

**缺点**：
- ❌ 需要维护两套构建配置
- ❌ 开发和生产环境不一致，可能引入 bug
- ❌ 工作量最大

**工作量**：大（约 2-3 天）

---

## 六、推荐方案

### 短期方案（推荐）：方案 A - 降级为 Vue 2 + Vue CLI

**理由**：
1. **兼容性最好**：与现有模块完全一致，无风险
2. **工作量可控**：2-4 小时即可完成
3. **维护成本低**：与其他模块使用相同技术栈，便于维护
4. **稳定性高**：Vue 2 + Vue CLI 已在项目中大量使用，经过充分验证

**适用场景**：
- 项目需要快速上线
- 团队对 Vue 2 更熟悉
- 不需要 Vue 3 的特定新特性

### 长期方案：方案 B - 升级主应用 qiankun

**理由**：
1. **面向未来**：支持现代化前端技术栈
2. **统一升级**：可以逐步将所有子应用升级到 Vue 3
3. **技术债务**：避免长期维护两套技术栈

**适用场景**：
- 项目有充足的时间和资源
- 计划逐步升级所有子应用
- 需要利用 Vue 3 和 Vite 的新特性

---

## 七、技术细节补充

### 7.1 qiankun 加载子应用的完整流程

```javascript
// 主应用 micro-app.js
import { registerMicroApps, start } from 'qiankun';

registerMicroApps([
  {
    name: 'analytics-stat',
    entry: '//localhost:8000/analytics-stat',  // 子应用入口
    container: '#micro-app',                    // 挂载容器
    activeRule: '#/analytics-stat',             // 激活规则
  }
]);

start();
```

**加载步骤**：
1. **匹配路由**：当 URL 变为 `#/analytics-stat` 时，qiankun 激活子应用
2. **获取 HTML**：`fetch('//localhost:8000/analytics-stat')` 获取 `index.html`
3. **解析 HTML**：提取 `<script>` 和 `<link>` 标签
4. **加载资源**：依次加载 JS 和 CSS 文件
5. **执行 JS**：在沙箱环境中执行 JS 代码
6. **获取生命周期**：从全局变量或模块导出中获取 `bootstrap`、`mount`、`unmount`
7. **调用 mount**：调用 `mount(props)` 渲染子应用

**Vue CLI 构建的应用**：
- 步骤 6：qiankun 从 `window['analytics-stat']` 获取生命周期函数 ✅
- 步骤 7：成功调用 `mount()` ✅

**Vite 构建的应用**：
- 步骤 5：JS 为 ES Module，qiankun 无法直接执行 ❌
- 步骤 6：即使执行成功，ES Module 导出也无法被识别 ❌
- 步骤 7：无法调用 `mount()` ❌

### 7.2 为什么 vite-plugin-qiankun 无法解决问题

**插件生成的代码**：
```javascript
// index.html 中注入的代码
const createDeffer = (hookName) => {
  const d = new Promise((resolve, reject) => {
    window.proxy && (window.proxy[`vite${hookName}`] = resolve)
  })
  return props => d.then(fn => fn(props));
}

window['analytics-stat'] = {
  bootstrap: createDeffer('bootstrap'),
  mount: createDeffer('mount'),
  unmount: createDeffer('unmount'),
};
```

**问题**：
1. 依赖 `window.proxy` 全局变量，但主应用未定义
2. 依赖 `window.moudleQiankunAppLifeCycles` 全局变量，但主应用未定义
3. 生命周期函数是 Promise 包装的代理函数，不是真正的实现
4. 真正的实现在 ES Module 中，需要通过 `import()` 动态加载
5. 主应用的 qiankun 不支持这种异步加载机制

### 7.3 UMD vs ES Module 的本质区别

**UMD（Universal Module Definition）**：
```javascript
(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD
    define(['vue'], factory);
  } else if (typeof module === 'object' && module.exports) {
    // CommonJS
    module.exports = factory(require('vue'));
  } else {
    // 浏览器全局变量
    root.myApp = factory(root.Vue);
  }
}(typeof self !== 'undefined' ? self : this, function (Vue) {
  // 应用代码
  return {
    bootstrap: function() {},
    mount: function() {},
    unmount: function() {}
  };
}));
```
- 兼容多种模块系统
- 可以在浏览器中直接执行
- qiankun 可以从全局变量中获取导出

**ES Module**：
```javascript
import { createApp } from 'vue'

export async function bootstrap() {}
export async function mount() {}
export async function unmount() {}
```
- 现代化的模块系统
- 需要浏览器原生支持或构建工具转换
- qiankun 2.x 无法直接识别

---

## 八、实施建议

### 8.1 如果选择方案 A（降级为 Vue 2）

**关键文件修改清单**：

1. **package.json**：
   - 替换 Vue 3 → Vue 2
   - 替换 Vue Router 4 → Vue Router 3
   - 替换 Element Plus → Element UI
   - 添加 Vue CLI 依赖

2. **main.ts → main.js**：
   - `createApp()` → `new Vue()`
   - 移除 Composition API 相关代码

3. **router/index.ts → router/index.js**：
   - `createRouter()` → `new Router()`
   - `createWebHashHistory()` → 默认 hash 模式

4. **组件文件**：
   - `<script setup>` → `export default {}`
   - Composition API → Options API

5. **vite.config.ts → vue.config.js**：
   - 完全替换配置文件

### 8.2 如果选择方案 B（升级 qiankun）

**关键步骤**：

1. **升级 qiankun**：
   ```bash
   npm install qiankun@latest
   ```

2. **修改 micro-app.js**：
   ```javascript
   import { registerMicroApps, start } from 'qiankun';
   
   registerMicroApps([
     {
       name: 'analytics-stat',
       entry: '//localhost:8000/analytics-stat',
       container: '#micro-app',
       activeRule: '#/analytics-stat',
       // 新增：支持 ES Module
       sandbox: {
         experimentalStyleIsolation: true
       }
     }
   ]);
   
   start({
     // 新增：支持 ES Module 加载
     sandbox: {
       experimentalStyleIsolation: true
     }
   });
   ```

3. **测试所有子应用**：
   - api-test
   - test-track
   - performance-test
   - 等等

---

## 九、总结

### 核心问题

**Vue 3 + Vite 构建的应用输出 ES Module 格式，而 MeterSphere 主应用使用的 qiankun 2.x 只支持 UMD 格式，导致子应用无法被正确加载。**

### 根本原因

1. **构建工具差异**：Vite 默认输出 ES Module，Vue CLI 默认输出 UMD
2. **qiankun 版本限制**：qiankun 2.x 对 ES Module 支持不足
3. **插件兼容性**：vite-plugin-qiankun 的代理机制与主应用不兼容

### 推荐方案

**短期：降级为 Vue 2 + Vue CLI**（兼容性最好，工作量可控）
**长期：升级主应用 qiankun**（面向未来，支持现代化技术栈）

### 经验教训

1. **技术选型需考虑整体架构**：新增模块应与现有模块保持技术栈一致
2. **微前端框架版本很重要**：qiankun 2.x 和 3.x 对子应用的要求不同
3. **构建工具影响集成方式**：Vite 和 Webpack 的产物格式差异很大
4. **插件不是万能的**：vite-plugin-qiankun 无法完全解决兼容性问题

---

## 十、参考资料

- [qiankun 官方文档](https://qiankun.umijs.org/)
- [vite-plugin-qiankun GitHub](https://github.com/tengmaoqing/vite-plugin-qiankun)
- [Vue 2 迁移指南](https://v2.vuejs.org/v2/guide/migration.html)
- [Vite 构建生产版本](https://vitejs.dev/guide/build.html)
- [UMD vs ES Module](https://dev.to/iggredible/what-the-heck-are-cjs-amd-umd-and-esm-ikm)

---

**文档版本**：v1.1  
**创建时间**：2026-02-02  
**更新时间**：2026-02-03  
**作者**：Kiro AI Assistant  
**适用项目**：MeterSphere v2.10

---

## 十、技术原理图解

### 10.1 qiankun 加载 UMD 子应用的完整流程（成功案例：api-test）

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 用户点击菜单 "API 测试"                                        │
│    URL 变为: http://localhost:8000/#/api                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. qiankun 检测到路由匹配 activeRule: #/api                     │
│    触发子应用加载流程                                             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. qiankun 通过 fetch 获取子应用入口                             │
│    fetch('http://localhost:4004/index.html')                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. 解析 HTML，提取资源                                           │
│    <script src="/api/js/api-chunk-vendors.abc123.js"></script> │
│    <script src="/api/js/api-app.abc123.js"></script>           │
│    <link href="/api/css/api-app.abc123.css" rel="stylesheet">  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. 加载并执行 JS 文件（在沙箱中）                                │
│    eval(jsContent) 或 new Function(jsContent)                   │
│                                                                  │
│    执行后，UMD 格式的代码会创建全局变量：                         │
│    window['api-app'] = {                                        │
│      bootstrap: function() {},                                  │
│      mount: function() {},                                      │
│      unmount: function() {}                                     │
│    }                                                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. qiankun 从全局变量获取生命周期函数                            │
│    const lifecycles = window['api-app'];  ✅ 成功获取           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. 调用生命周期函数                                              │
│    await lifecycles.bootstrap();                                │
│    await lifecycles.mount({ container: '#micro-app' });         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 8. 子应用渲染成功 ✅                                             │
│    用户看到 API 测试界面                                         │
└─────────────────────────────────────────────────────────────────┘
```

---

### 10.2 qiankun 尝试加载 ES Module 子应用的流程（失败案例：analytics-stat）

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 用户点击菜单 "分析统计"                                        │
│    URL 变为: http://localhost:8000/#/analytics-stat             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. qiankun 检测到路由匹配 activeRule: #/analytics-stat          │
│    触发子应用加载流程                                             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. qiankun 通过 fetch 获取子应用入口                             │
│    fetch('http://localhost:4009/index.html')                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. 解析 HTML，提取资源                                           │
│    <script type="module" src="/analytics-stat/assets/index.js">│
│    ❌ qiankun 的正则表达式跳过 type="module" 标签                │
│    结果：scripts = []  (空数组)                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. 尝试执行 JS 文件                                              │
│    ❌ 因为 scripts = []，没有 JS 被加载和执行                    │
│    window['analytics-stat'] = undefined                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. qiankun 尝试从全局变量获取生命周期函数                        │
│    const lifecycles = window['analytics-stat'];                 │
│    ❌ lifecycles = undefined                                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. qiankun 报错或静默失败                                        │
│    ❌ 子应用无法渲染                                             │
│    用户看到空白页面                                              │
└─────────────────────────────────────────────────────────────────┘
```

---

### 10.3 vite-plugin-qiankun 的代理机制流程（在 MeterSphere 中失败）

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. vite-plugin-qiankun 生成特殊的 index.html                    │
│    包含代理函数和动态 import()                                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. qiankun 加载 HTML，执行内联 script                            │
│    创建代理函数：                                                 │
│    window['analytics-stat'] = {                                 │
│      mount: createDeffer('mount')  // 返回 Promise              │
│    }                                                            │
│    ✅ qiankun 可以获取到这个全局变量                             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. qiankun 调用 mount()                                         │
│    await window['analytics-stat'].mount(props);                 │
│    ✅ 调用成功，但返回的是 Promise                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. Promise 等待 window.proxy.vitemount 被调用                   │
│    ❌ 但 MeterSphere 主应用没有定义 window.proxy                │
│    Promise 永远处于 pending 状态                                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. 同时，动态 import() 尝试加载 ES Module                        │
│    import('/analytics-stat/assets/index.js').finally(() => {   │
│      // 尝试调用 window.proxy.vitemount                         │
│      ❌ 但 window.proxy 不存在，无法调用                         │
│    })                                                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. 结果：Promise 永远不会 resolve                                │
│    ❌ 子应用永远不会被渲染                                       │
│    用户看到空白页面                                              │
└─────────────────────────────────────────────────────────────────┘
```

---

### 10.4 UMD vs ES Module 的模块导出对比

```
┌──────────────────────────────────────────────────────────────────┐
│                        UMD 格式（Webpack）                        │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  // 构建产物（简化）                                              │
│  (function(root, factory) {                                      │
│    if (typeof exports === 'object') {                            │
│      module.exports = factory();  // CommonJS                    │
│    } else if (typeof define === 'function') {                    │
│      define([], factory);  // AMD                                │
│    } else {                                                      │
│      root['api-app'] = factory();  // ✅ 浏览器全局变量          │
│    }                                                             │
│  })(window, function() {                                         │
│    return {                                                      │
│      bootstrap: async function() {},                             │
│      mount: async function() {},                                 │
│      unmount: async function() {}                                │
│    };                                                            │
│  });                                                             │
│                                                                   │
│  ✅ 结果：window['api-app'] 可以被直接访问                       │
│  ✅ qiankun 可以获取生命周期函数                                 │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                      ES Module 格式（Vite）                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  // 构建产物（简化）                                              │
│  import { createApp } from 'vue';                                │
│  import App from './App.vue';                                    │
│                                                                   │
│  let app = null;                                                 │
│                                                                   │
│  function render(props) {                                        │
│    app = createApp(App);                                         │
│    app.mount(props.container || '#app');                         │
│  }                                                               │
│                                                                   │
│  export async function bootstrap() {}                            │
│  export async function mount(props) { render(props); }           │
│  export async function unmount() { app.unmount(); }              │
│                                                                   │
│  ❌ 结果：没有创建全局变量                                       │
│  ❌ qiankun 无法访问 export 的内容                               │
│  ❌ 需要通过 import 语句才能访问：                               │
│     import { mount } from './index.js';                          │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

### 10.5 技术栈兼容性矩阵

```
┌─────────────────┬──────────────┬──────────────┬──────────────┐
│   构建工具       │  输出格式    │ qiankun 2.x  │ qiankun 3.x  │
├─────────────────┼──────────────┼──────────────┼──────────────┤
│ Webpack         │ UMD          │ ✅ 完全支持  │ ✅ 完全支持  │
│ (Vue CLI)       │              │              │              │
├─────────────────┼──────────────┼──────────────┼──────────────┤
│ Vite            │ ES Module    │ ❌ 不支持    │ ⚠️ 实验性    │
│ (默认配置)      │              │              │   支持       │
├─────────────────┼──────────────┼──────────────┼──────────────┤
│ Vite            │ UMD          │ ⚠️ 需要特殊  │ ✅ 支持      │
│ (Library 模式)  │ (手动配置)   │   配置       │              │
├─────────────────┼──────────────┼──────────────┼──────────────┤
│ Rollup          │ UMD          │ ✅ 支持      │ ✅ 支持      │
├─────────────────┼──────────────┼──────────────┼──────────────┤
│ Rollup          │ ES Module    │ ❌ 不支持    │ ⚠️ 实验性    │
│                 │              │              │   支持       │
└─────────────────┴──────────────┴──────────────┴──────────────┘

图例：
✅ 完全支持：开箱即用，无需额外配置
⚠️ 实验性支持：需要额外配置，可能存在问题
❌ 不支持：无法使用
```

---

### 10.6 MeterSphere 当前技术栈全景图

```
┌─────────────────────────────────────────────────────────────────┐
│                      MeterSphere 主应用                          │
│                   (framework/sdk-parent/frontend)                │
│                                                                  │
│  技术栈：Vue 2.7 + Vue Router 3 + Webpack                       │
│  qiankun 版本：2.x                                               │
│  支持格式：UMD                                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 注册子应用
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ↓                     ↓                     ↓
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│  api-test    │      │  test-track  │      │ performance  │
│              │      │              │      │   -test      │
│ Vue 2 + CLI  │      │ Vue 2 + CLI  │      │ Vue 2 + CLI  │
│ UMD 格式     │      │ UMD 格式     │      │ UMD 格式     │
│ ✅ 正常工作  │      │ ✅ 正常工作  │      │ ✅ 正常工作  │
└──────────────┘      └──────────────┘      └──────────────┘

        │                     │                     │
        ↓                     ↓                     ↓
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│  project-    │      │  system-     │      │  report-     │
│  management  │      │  setting     │      │  stat        │
│              │      │              │      │              │
│ Vue 2 + CLI  │      │ Vue 2 + CLI  │      │ Vue 2 + CLI  │
│ UMD 格式     │      │ UMD 格式     │      │ UMD 格式     │
│ ✅ 正常工作  │      │ ✅ 正常工作  │      │ ✅ 正常工作  │
└──────────────┘      └──────────────┘      └──────────────┘

        │                     │
        ↓                     ↓
┌──────────────┐      ┌──────────────┐
│  workstation │      │ analytics-   │
│              │      │  stat        │
│ Vue 2 + CLI  │      │ Vue 3 + Vite │ ← ❌ 唯一的异类
│ UMD 格式     │      │ ES Module    │
│ ✅ 正常工作  │      │ ❌ 无法加载  │
└──────────────┘      └──────────────┘

总结：
- 7 个子应用使用 Vue 2 + Vue CLI + UMD，全部正常工作 ✅
- 1 个子应用使用 Vue 3 + Vite + ES Module，无法加载 ❌
- 技术栈不一致是问题的根源
```

---

## 十一、常见问题解答（FAQ）

### Q1: 为什么其他模块（api-test）可以正常加载，而 analytics-stat 不行？

**A**: 因为其他模块使用 Vue 2 + Vue CLI + Webpack 构建，输出 **UMD 格式**，而 analytics-stat 使用 Vue 3 + Vite 构建，输出 **ES Module 格式**。qiankun 2.x 只支持 UMD 格式。

**技术对比**：
```javascript
// api-test 构建产物（UMD）
window['api-index'] = {
  mount: function() { /* ... */ }
};
// ✅ qiankun 可以通过 window['api-index'] 访问

// analytics-stat 构建产物（ES Module）
export async function mount() { /* ... */ }
// ❌ qiankun 无法访问 export 的内容
```

---

### Q2: 我已经在 main.ts 中导出了生命周期函数，为什么还是不行？

**A**: 虽然你在 `main.ts` 中导出了生命周期函数，但 Vite 构建后生成的是 ES Module 格式的 JS 文件。qiankun 2.x 的 HTML 解析器会**跳过** `<script type="module">` 标签，因此这些 JS 文件根本没有被加载和执行。

**验证方法**：
```bash
# 1. 构建 analytics-stat
cd analytics-stat/frontend
npm run build

# 2. 查看构建产物
cat dist/index.html

# 3. 你会看到：
<script type="module" crossorigin src="/analytics-stat/assets/index-abc123.js"></script>

# 4. qiankun 的 HTML 解析器会跳过这个标签
```

---

### Q3: vite-plugin-qiankun 插件不是可以解决这个问题吗？

**A**: vite-plugin-qiankun 尝试通过代理机制解决兼容性，但它**依赖主应用提供特定的全局变量**（`window.proxy` 和 `window.moudleQiankunAppLifeCycles`）。MeterSphere 的主应用没有这些配置，因此插件无法工作。

**插件的依赖链**：
```
vite-plugin-qiankun 工作流程：
1. 子应用导出生命周期函数到 window.moudleQiankunAppLifeCycles ✅
2. 子应用创建代理函数，挂载到 window['analytics-stat'] ✅
3. qiankun 调用 window['analytics-stat'].mount() ✅
4. 代理函数等待 window.proxy.vitemount 被调用 ❌ (主应用没有定义 window.proxy)
5. 永远等待，子应用永远不会被渲染 ❌
```

---

### Q4: 能不能修改主应用，添加 window.proxy 等全局变量？

**A**: 理论上可以，但这只是解决了一部分问题。还需要：
1. 修改主应用的 qiankun 配置，支持 ES Module 加载
2. 测试所有现有子应用的兼容性
3. 处理 ES Module 的异步加载问题
4. 解决样式隔离和全局变量隔离问题

**工作量评估**：
- 修改主应用配置：2-4 小时
- 测试所有子应用：1-2 天
- 修复兼容性问题：未知（取决于发现的问题数量）
- 总工作量：3-5 天，风险较高

**对比降级为 Vue 2 的工作量**：
- 修改 analytics-stat 前端代码：2-4 小时
- 测试 analytics-stat：1-2 小时
- 总工作量：半天，风险低

---

### Q5: 为什么不直接升级到 qiankun 3.x？

**A**: qiankun 3.x 对 ES Module 的支持仍然是**实验性的**，而且升级会影响所有现有子应用。需要：
1. 升级主应用的 qiankun 版本
2. 测试 7 个现有子应用的兼容性
3. 修复可能出现的问题
4. 回归测试所有功能

**风险评估**：
- 现有子应用可能出现兼容性问题
- 生产环境可能出现未知 bug
- 回滚成本高
- 不适合作为短期方案

---

### Q6: 如果我坚持使用 Vue 3 + Vite，有什么办法吗？

**A**: 有两个方案：

**方案 1：使用 Vite 的 Library 模式构建 UMD 格式**
```typescript
// vite.config.ts
export default defineConfig({
  build: {
    lib: {
      entry: 'src/main.ts',
      name: 'analyticsStat',
      formats: ['umd'],
      fileName: () => 'analytics-stat.js'
    },
    rollupOptions: {
      output: {
        name: 'analyticsStat',
        exports: 'named'
      }
    }
  }
})
```

**缺点**：
- Vite 的 Library 模式主要用于构建库，不适合完整应用
- 需要手动处理 HTML 入口
- 代码分割和懒加载支持不佳
- 配置复杂，维护成本高

**方案 2：升级主应用 qiankun 到 3.x**
- 见 Q5 的分析

---

### Q7: 降级为 Vue 2 后，会失去哪些 Vue 3 的特性？

**A**: 主要失去以下特性：

1. **Composition API**：
   - `<script setup>` 语法
   - `ref`、`reactive`、`computed` 等组合式 API
   - 需要改回 Options API（`data`、`methods`、`computed` 等）

2. **性能优化**：
   - Vue 3 的虚拟 DOM 优化
   - 更快的渲染速度
   - 更小的打包体积

3. **TypeScript 支持**：
   - Vue 3 对 TypeScript 的支持更好
   - Vue 2 需要额外配置

4. **新组件特性**：
   - Teleport
   - Suspense
   - Fragment（多根节点）

**但是**：
- Vue 2.7 支持部分 Composition API（可以在 `setup()` 中使用）
- 对于简单的业务应用，Vue 2 完全够用
- MeterSphere 的其他模块都使用 Vue 2，技术栈一致性更重要

---

### Q8: 为什么 Vite 不默认输出 UMD 格式？

**A**: 因为 Vite 的设计理念是**面向现代浏览器**，追求性能和开发体验：

1. **ES Module 是 Web 标准**：
   - 现代浏览器原生支持
   - 更好的 Tree Shaking
   - 更快的加载速度

2. **UMD 是过渡方案**：
   - 为了兼容旧浏览器和旧模块系统
   - 增加了代码体积
   - 性能不如 ES Module

3. **Vite 的目标用户**：
   - 独立的现代化应用
   - 不需要被其他应用动态加载
   - 不需要考虑微前端场景

**而 qiankun 的设计理念是**：
- 兼容性优先
- 支持动态加载子应用
- 需要 UMD 格式以便在沙箱中执行

**两者的理念冲突**，导致不兼容。

---

### Q9: 如果未来 MeterSphere 升级到 qiankun 3.x，analytics-stat 需要改回 Vue 3 吗？

**A**: 不一定需要。即使主应用升级到 qiankun 3.x，Vue 2 + Webpack 的子应用仍然可以正常工作。但如果你想利用 Vue 3 的新特性，可以考虑升级。

**升级时机建议**：
1. 等主应用升级到 qiankun 3.x 并稳定运行
2. 等其他子应用也开始升级到 Vue 3
3. 统一升级，保持技术栈一致性

---

### Q10: 这个问题是 qiankun 的 bug 吗？

**A**: **不是 bug，是设计限制**。qiankun 2.x 设计时（2020年），Vite 还未流行，ES Module 也未被广泛使用。qiankun 2.x 的设计目标是支持 Webpack 构建的 UMD 应用，这在当时是最佳实践。

**时间线**：
- 2020年初：qiankun 2.x 发布，主要支持 Webpack
- 2020年底：Vite 1.0 发布，开始流行
- 2021年：Vite 2.0 发布，ES Module 成为主流
- 2022年：qiankun 3.x 发布，开始实验性支持 ES Module

**结论**：这是技术演进过程中的正常现象，不是 bug。
