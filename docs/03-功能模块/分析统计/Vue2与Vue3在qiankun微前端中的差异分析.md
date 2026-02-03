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

### 3.1 模块格式差异

| 特性 | Vue 2 + Vue CLI | Vue 3 + Vite |
|------|----------------|--------------|
| **输出格式** | UMD（Universal Module Definition） | ES Module |
| **浏览器兼容** | 直接在浏览器中执行 | 需要浏览器原生支持 ES Module |
| **导出方式** | 全局变量（`window.mount`） | ES Module 导出（`export function mount`） |
| **qiankun 支持** | ✅ 原生支持 | ❌ 需要额外处理 |

### 3.2 入口文件差异

**Vue CLI 生成的 index.html**：
```html
<script src="/api/js/app.abc123.js"></script>
```
- qiankun 可以直接加载并执行
- 生命周期函数通过 UMD 导出，qiankun 可以获取

**Vite 生成的 index.html**：
```html
<script type="module" crossorigin src="/analytics-stat/assets/index-abc123.js"></script>
```
- qiankun 无法直接处理 `type="module"`
- 即使加载成功，ES Module 的导出也无法被 qiankun 识别

### 3.3 vite-plugin-qiankun 的问题

`vite-plugin-qiankun` 插件尝试解决这个问题，但引入了新的复杂性：

**工作原理**：
1. 在 `index.html` 中注入代理代码
2. 使用 `window.proxy` 和 `window.moudleQiankunAppLifeCycles` 作为桥接
3. 将 ES Module 导出的生命周期函数转换为全局函数

**问题**：
- 主应用的 qiankun 版本（2.x）不支持这种代理机制
- 代理代码依赖特定的全局变量，与主应用不兼容
- 生成的 HTML 结构与 Vue CLI 完全不同

### 3.4 路由模式差异

| 特性 | Vue 2 + Vue CLI | Vue 3 + Vite |
|------|----------------|--------------|
| **默认路由** | Hash 模式（`createRouter()`） | History 模式（`createWebHistory()`） |
| **qiankun 兼容** | ✅ Hash 模式天然兼容 | ⚠️ History 模式需要配置 base |
| **子应用路由** | 自动嵌套在主应用 hash 中 | 需要手动配置 base path |

---

## 四、问题根源总结

### 4.1 直接原因

**qiankun 无法识别 Vite 构建的 ES Module 格式的生命周期函数**

- qiankun 期望子应用通过 UMD 格式导出生命周期函数
- Vite 默认输出 ES Module 格式
- 两者不兼容

### 4.2 深层原因

**MeterSphere 主应用使用的 qiankun 版本较旧，不支持 ES Module 子应用**

- 主应用使用 qiankun 2.x
- qiankun 2.x 设计时主要针对 Webpack 构建的应用
- 对 Vite 等现代构建工具支持不足

### 4.3 vite-plugin-qiankun 的局限性

**插件尝试通过代理机制解决兼容性，但引入了新的依赖**

- 需要主应用支持特定的全局变量（`window.proxy`、`window.moudleQiankunAppLifeCycles`）
- MeterSphere 主应用未实现这些全局变量
- 导致代理机制失效

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

**文档版本**：v1.0  
**创建时间**：2026-02-02  
**作者**：Kiro AI Assistant  
**适用项目**：MeterSphere v2.10
