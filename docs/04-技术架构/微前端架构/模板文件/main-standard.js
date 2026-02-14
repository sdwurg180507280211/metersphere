/**
 * 子应用 main.js 标准改造模板（qiankun → micro-app UMD 生命周期模式）
 *
 * 适用范围：简单模块（无按需加载/内存路由场景）
 *   - workstation, report-stat, analytics-stat
 *   - project-management, system-setting, performance-test
 *
 * 改造要点：
 * 1. 移除 qiankun 生命周期导出（export async function bootstrap/mount/unmount/update）
 * 2. 采用 micro-app 的 UMD 生命周期模式：
 *    - 渲染逻辑放入 `window.mount = (data) => { ... }`
 *    - 卸载逻辑放入 `window.unmount = () => { ... }`
 *    【关键】这里的「UMD 模式」是 micro-app 的生命周期管理概念，
 *    与 webpack 的 `libraryTarget: 'umd'` 打包格式完全无关，后者必须移除。
 * 3. `window.mount(data)` 的 `data` 参数由 micro-app 自动传入，
 *    来源于主应用 `<micro-app :data="appData">` 的 data 属性
 * 4. Vue 插件注册（Vue.use()）放在 mount 外部，只执行一次
 *    这是 UMD 生命周期模式的优势：模块切换时不会重复注册插件
 * 5. 将 `!window.__POWERED_BY_QIANKUN__` 替换为 `!window.__MICRO_APP_ENVIRONMENT__`
 * 6. 集成 EventBus 兼容适配器（createEventBusAdapter() 替代从 props 接收 eventBus）
 *
 * 对应需求：Requirements 2.1, 2.3, 2.5
 *
 * ============================================================
 * 以下以 workstation 模块为例展示改造后的完整代码
 * 实际使用时，请根据各模块的 import 和 Vue.use() 进行调整
 * ============================================================
 */

// public-path.js 必须在最顶部引入，确保资源路径在其他模块加载前设置好
import './public-path';
import '@/business/component/js/track-table-header';
import Vue from 'vue';
import 'metersphere-frontend/src/styles/index.scss';
import ElementUI from 'element-ui';
import App from './App.vue';
import i18n from './i18n';
import router from './router';
import { createPinia, PiniaVuePlugin } from 'pinia';
import PersistedState from 'pinia-plugin-persistedstate';
import icons from 'metersphere-frontend/src/icons';
import svg from 'metersphere-frontend/src/components/svg';
import plugins from 'metersphere-frontend/src/plugins';
import directives from 'metersphere-frontend/src/directive';
import filters from 'metersphere-frontend/src/filters';
import 'metersphere-frontend/src/router/permission';
import chart from 'metersphere-frontend/src/chart';
import VueShepherd from 'vue-shepherd';
import 'metersphere-frontend/src/assets/shepherd/shepherd-theme.css';
import { gotoCancel, gotoNext } from 'metersphere-frontend/src/utils';
// 【新增】引入 EventBus 兼容适配器，替代从 qiankun props 接收 eventBus
import { createEventBusAdapter } from 'metersphere-frontend/src/utils/micro-app-event-bus';

Vue.config.productionTip = false;

// ============================================================
// 【关键】Vue 插件注册放在 mount 外部，只执行一次
// UMD 生命周期模式的优势：模块频繁切换时（mount/unmount 反复调用），
// 插件不会被重复注册，避免内存泄漏和副作用
// ============================================================
const pinia = createPinia();
pinia.use(PersistedState); // 开启缓存，存储在 localStorage

Vue.use(ElementUI, {
  i18n: (key, value) => i18n.t(key, value),
});

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

let instance = null;

// ============================================================
// UMD 生命周期模式：渲染函数
// ============================================================
// 【改造前 - qiankun】：
//   function render(props = {}) {
//     const { container, eventBus = new Vue() } = props;
//     Vue.prototype.$EventBus = eventBus;
//     instance = new Vue({ ... }).$mount(container ? container.querySelector('#app') : '#app');
//   }
//   export async function mount(props) { render(props); }
//
// 【改造后 - micro-app UMD 模式】：
//   window.mount = (data) => { ... }
//   - data 参数由 micro-app 自动传入，来源于 <micro-app :data="appData"> 的 data 属性
//   - 不再从 props 中解构 container 和 eventBus
//   - 挂载点固定为 '#app'（micro-app 沙箱会自动隔离 DOM）
// ============================================================
window.mount = (data) => {
  // 创建 EventBus：
  // - micro-app 环境下：使用适配器，桥接 micro-app 数据通信到本地 EventBus
  //   子应用内部的 $EventBus.$emit / $on 继续工作，跨应用事件通过 micro-app 通信机制传递
  // - 独立运行时：使用普通 Vue 实例（与原 qiankun 独立运行时行为一致）
  Vue.prototype.$EventBus = window.__MICRO_APP_ENVIRONMENT__
    ? createEventBusAdapter()
    : new Vue();

  instance = new Vue({
    i18n,
    router,
    pinia,
    render: (h) => h(App),
  }).$mount('#app');
};

// ============================================================
// UMD 生命周期模式：卸载函数
// ============================================================
// 【改造前 - qiankun】：
//   export async function unmount(props) {
//     instance.$destroy();
//   }
//
// 【改造后 - micro-app UMD 模式】：
//   window.unmount = () => { ... }
//   - 增加 innerHTML 清空，确保 DOM 完全清理
//   - 增加 instance = null，释放引用避免内存泄漏
// ============================================================
window.unmount = () => {
  if (instance) {
    instance.$destroy();
    instance.$el.innerHTML = ''; // 清空 DOM 内容，防止残留
    instance = null;             // 释放 Vue 实例引用
  }
};

// ============================================================
// 非微前端环境直接渲染（开发模式独立运行）
// ============================================================
// 【改造前】：if (!window.__POWERED_BY_QIANKUN__) { render(); }
// 【改造后】：if (!window.__MICRO_APP_ENVIRONMENT__) { window.mount(); }
//   - __MICRO_APP_ENVIRONMENT__ 是 micro-app 在子应用运行时自动注入的环境标识
//   - 独立运行时该变量不存在，直接调用 window.mount() 渲染
if (!window.__MICRO_APP_ENVIRONMENT__) {
  window.mount();
}

// ============================================================
// 【已移除】qiankun 生命周期导出
// ============================================================
// 以下代码已移除，不再需要：
//   export async function bootstrap(props) {}
//   export async function mount(props) { ... }
//   export async function unmount(props) { ... }
//   export async function update(props) { ... }
//
// micro-app 的 UMD 生命周期模式通过 window.mount / window.unmount 管理，
// 不需要 ES Module 导出的生命周期函数。
