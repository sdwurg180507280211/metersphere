/**
 * 子应用 main.js 高级改造模板（含按需加载/内存路由支持）
 *
 * 适用范围：含按需加载场景的复杂模块
 *   - api-test（被 test-track 和 TaskCenter 按需嵌入，需要 microRouter 支持）
 *   - test-track（类似场景，如需要也可参考此模板）
 *
 * 与标准模板的差异：
 * 1. mount 中根据 data 参数决定使用 router（正常路由）还是 microRouter（内存路由）
 *    - data 参数由 micro-app 自动传入，来源于 <micro-app :data="appData"> 的 data 属性
 *    - 当 data 包含 defaultPath 或 routeName 时，说明是按需加载场景，使用 microRouter
 * 2. 添加 addDataListener 监听主应用后续的路由更新
 *    - mount(data) 只处理初始数据，运行时的动态更新通过 addDataListener 接收
 *    - 场景：MicroAppWrapper 组件的 to/routeParams 属性变化时，
 *      主应用通过 setData 发送新的路由参数，子应用通过 addDataListener 接收并跳转
 *
 * 改造要点（与标准模板相同的部分）：
 * - 移除 qiankun 生命周期导出（export async function bootstrap/mount/unmount/update）
 * - 采用 micro-app 的 UMD 生命周期模式（window.mount / window.unmount）
 * - Vue 插件注册放在 mount 外部，只执行一次
 * - 集成 EventBus 兼容适配器
 *
 * 对应需求：Requirements 2.1, 2.3, 2.5
 *
 * ============================================================
 * 以下以 api-test 模块为例展示改造后的完整代码
 * 注意：api-test 有多个 pages 入口（shareApiReport、shareDocument、apiDocument），
 * 这些入口不在微前端环境下运行，不受此改造影响
 * ============================================================
 */

// public-path.js 必须在最顶部引入
import './public-path';
import Vue from 'vue';
import 'metersphere-frontend/src/styles/index.scss';
import ElementUI from 'element-ui';
import App from './App.vue';
import i18n from './i18n';
// 【关键】同时引入 router 和 microRouter
// - router：正常路由，用于全局路由激活场景（用户通过导航菜单进入 api-test）
// - microRouter：内存路由，用于按需加载场景（被其他模块嵌入时，路由不影响浏览器地址栏）
import router, { microRouter } from './router';
import { createPinia, PiniaVuePlugin } from 'pinia';
import PersistedState from 'pinia-plugin-persistedstate';
import icons from 'metersphere-frontend/src/icons';
import svg from 'metersphere-frontend/src/components/svg';
import plugins from 'metersphere-frontend/src/plugins';
import directives from 'metersphere-frontend/src/directive';
import filters from 'metersphere-frontend/src/filters';
import 'metersphere-frontend/src/router/permission';
import chart from 'metersphere-frontend/src/chart';
import VueFab from 'vue-float-action-button';
import VueClipboard from 'vue-clipboard2';
import VuePapaParse from 'vue-papa-parse';
import VueShepherd from 'vue-shepherd';
import 'metersphere-frontend/src/assets/shepherd/shepherd-theme.css';
import { gotoCancel, gotoNext } from 'metersphere-frontend/src/utils';
import VueVirtualTree from '@fit2cloud-ui/vue-virtual-tree';
// 【新增】引入 EventBus 兼容适配器
import { createEventBusAdapter } from 'metersphere-frontend/src/utils/micro-app-event-bus';

Vue.config.productionTip = false;

// ============================================================
// 【关键】Vue 插件注册放在 mount 外部，只执行一次
// UMD 生命周期模式的优势：模块频繁切换时不会重复注册插件
// ============================================================
const pinia = createPinia();
pinia.use(PersistedState); // 开启缓存，存储在 localStorage

Vue.use(ElementUI, {
  i18n: (key, value) => i18n.t(key, value),
});

Vue.use(directives);
Vue.use(svg);
Vue.use(icons);
Vue.use(plugins);
Vue.use(filters);
Vue.use(PiniaVuePlugin);
Vue.use(chart);
Vue.use(VueClipboard);
Vue.use(VueFab);
Vue.use(VuePapaParse);
Vue.use(VueShepherd);
Vue.use(VueVirtualTree);

Vue.prototype.gotoCancel = gotoCancel;
Vue.prototype.gotoNext = gotoNext;

let instance = null;

// ============================================================
// UMD 生命周期模式：渲染函数（含按需加载路由处理）
// ============================================================
// 【改造前 - qiankun】：
//   function render(props = {}) {
//     const { container, eventBus = new Vue(), defaultPath, routeParams, routeName } = props;
//     Vue.prototype.$EventBus = eventBus;
//     instance = new Vue({
//       router: defaultPath || routeName ? microRouter : router,
//       ...
//     }).$mount(container ? container.querySelector('#app') : '#app');
//     if (defaultPath || routeName) { microRouter.push({ ... }); }
//   }
//   export async function mount(props) { render(props); }
//   export async function update(props) { microRouter.push({ ... }); }
//
// 【改造后 - micro-app UMD 模式】：
//   window.mount = (data) => { ... }
//   - data 参数由 micro-app 自动传入
//   - 按需加载场景：data 包含 defaultPath/routeName，使用 microRouter
//   - 全局路由场景：data 为空或不含路由参数，使用 router
//   - update 钩子的功能由 addDataListener 替代（见下方）
// ============================================================
window.mount = (data) => {
  // 创建 EventBus（与标准模板相同）
  Vue.prototype.$EventBus = window.__MICRO_APP_ENVIRONMENT__
    ? createEventBusAdapter()
    : new Vue();

  // 根据 data 参数决定使用哪个路由实例：
  // - 有 defaultPath 或 routeName → 按需加载场景，使用 microRouter（内存路由）
  //   此时子应用被 MicroAppWrapper 嵌入到其他模块中，路由不应影响浏览器地址栏
  // - 无路由参数 → 全局路由激活场景，使用 router（正常 hash 路由）
  //   此时用户通过导航菜单进入 api-test 模块
  const useRouter = (data && (data.defaultPath || data.routeName)) ? microRouter : router;

  instance = new Vue({
    i18n,
    router: useRouter,
    pinia,
    render: (h) => h(App),
  }).$mount('#app');

  // 如果有目标路由参数，跳转到指定页面
  // 场景：test-track 中嵌入 API 报告，传入 defaultPath 指向具体的报告页面
  if (data && (data.defaultPath || data.routeName)) {
    microRouter.push({
      path: data.defaultPath,
      params: data.routeParams,
      name: data.routeName,
    });
  }
};

// ============================================================
// UMD 生命周期模式：卸载函数（与标准模板相同）
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
if (!window.__MICRO_APP_ENVIRONMENT__) {
  window.mount();
}

// ============================================================
// 【新增】监听主应用后续数据更新（替代 qiankun 的 update 钩子）
// ============================================================
// 【改造前 - qiankun】：
//   export async function update(props) {
//     const { defaultPath, routeParams, routeName } = props;
//     if (defaultPath || routeName) { microRouter.push({ ... }); }
//   }
//
// 【改造后 - micro-app】：
//   通过 addDataListener 监听主应用通过 setData 发送的数据变化
//
// 工作原理：
//   1. window.mount(data) 处理初始数据（子应用首次加载时的路由参数）
//   2. addDataListener 处理运行时动态更新（子应用已加载后，主应用更新路由参数）
//   两种机制互补，覆盖完整的数据传递生命周期
//
// 典型场景：
//   MicroAppWrapper 组件的 to/routeParams 属性变化时（如用户在 test-track 中
//   切换查看不同的 API 报告），Vue 的响应式系统触发 appData 更新，
//   micro-app 自动调用 setData 将新数据发送给子应用，
//   子应用通过 addDataListener 接收并执行路由跳转
// ============================================================
if (window.__MICRO_APP_ENVIRONMENT__) {
  window.microApp?.addDataListener((data) => {
    if (data && (data.defaultPath || data.routeName)) {
      // 优先使用当前 Vue 实例的 $router（已挂载的路由实例），
      // 降级使用 microRouter（实例尚未创建的边界情况）
      const targetRouter = instance?.$router || microRouter;
      targetRouter.push({
        path: data.defaultPath,
        params: data.routeParams,
        name: data.routeName,
      });
    }
  });
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
// 功能映射：
//   - bootstrap → 不需要（Vue 插件注册已在 mount 外部，只执行一次）
//   - mount     → window.mount = (data) => { ... }
//   - unmount   → window.unmount = () => { ... }
//   - update    → window.microApp.addDataListener((data) => { ... })
