// public-path.js 必须在最顶部引入，确保资源路径在其他模块加载前设置好
import './public-path';
import Vue from 'vue';
import 'metersphere-frontend/src/styles/index.scss';
import ElementUI from 'element-ui';
import App from './App.vue';
import i18n from './i18n';
// 同时引入 router 和 microRouter
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
// import formCreate from '@form-create/element-ui';
import VuePapaParse from 'vue-papa-parse';
import VueShepherd from 'vue-shepherd'; // 新手引导
import 'metersphere-frontend/src/assets/shepherd/shepherd-theme.css';
import { gotoCancel, gotoNext } from 'metersphere-frontend/src/utils';
import VueVirtualTree from '@fit2cloud-ui/vue-virtual-tree';
// 【新增】引入 EventBus 兼容适配器，替代从 qiankun props 接收 eventBus
import { createEventBusAdapter } from 'metersphere-frontend/src/utils/micro-app-event-bus';

Vue.config.productionTip = false;

// 【关键】Vue 插件注册放在 mount 外部，只执行一次
// UMD 生命周期模式的优势：模块频繁切换时不会重复注册插件
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
// Vue.use(JSONPathPicker);
// Vue.use(formCreate);
Vue.use(VuePapaParse);
Vue.use(VueShepherd);
Vue.use(VueVirtualTree);

Vue.prototype.gotoCancel = gotoCancel;
Vue.prototype.gotoNext = gotoNext;

let instance = null;

/**
 * 渲染函数（含按需加载路由处理）
 *
 * 【重要】不再依赖 micro-app 的 UMD 生命周期自动检测来调用 mount()。
 * 原因：micro-app with 沙箱的 defineProperty 劫持在当前 webpack 打包模式下
 * 无法正确拦截 window.mount 的赋值，导致 UMD 模式检测失败，子应用永远不会被挂载。
 *
 * 新策略：子应用始终自行挂载（无论独立运行还是 micro-app 环境），
 * 同时保留 window.unmount 供 micro-app 在子应用卸载时调用。
 *
 * @param {Object} data - 可选，路由参数（按需加载场景由主应用传入）
 */
function mount(data) {
  // 创建 EventBus：
  // - micro-app 环境下：使用适配器，桥接 micro-app 数据通信到本地 EventBus
  // - 独立运行时：使用普通 Vue 实例（与原 qiankun 独立运行时行为一致）
  Vue.prototype.$EventBus = window.__MICRO_APP_ENVIRONMENT__
    ? createEventBusAdapter()
    : new Vue();

  // 根据 data 参数决定使用哪个路由实例：
  // - 有 defaultPath 或 routeName → 按需加载场景，使用 microRouter（内存路由）
  // - 无路由参数 → 全局路由激活场景，使用 router（正常 hash 路由）
  const useRouter = (data && (data.defaultPath || data.routeName)) ? microRouter : router;

  instance = new Vue({
    i18n,
    router: useRouter,
    pinia,
    render: (h) => h(App),
  }).$mount('#app');

  // 如果有目标路由参数，跳转到指定页面
  // 场景：test-track 中嵌入接口测试报告，传入 defaultPath 指向具体的报告页面
  if (data && (data.defaultPath || data.routeName)) {
    microRouter.push({
      path: data.defaultPath,
      params: data.routeParams,
      name: data.routeName,
    });
  }
}

// 卸载函数：供 micro-app 在子应用卸载时调用
window.unmount = () => {
  if (instance) {
    instance.$destroy();
    instance.$el.innerHTML = ''; // 清空 DOM 内容，防止残留
    instance = null;             // 释放 Vue 实例引用
  }
};

// 直接挂载（micro-app 环境和独立运行均适用）
mount();

// 监听主应用后续数据更新（替代 qiankun 的 update 钩子）
// window.mount(data) 处理初始数据，addDataListener 处理运行时动态更新
// 典型场景：MicroAppWrapper 组件的 to/routeParams 属性变化时，
// 主应用通过 setData 发送新的路由参数，子应用通过 addDataListener 接收并跳转
if (window.__MICRO_APP_ENVIRONMENT__) {
  window.microApp?.addDataListener((data) => {
    if (data && (data.defaultPath || data.routeName)) {
      // 优先使用当前 Vue 实例的 $router，降级使用 microRouter
      const targetRouter = instance?.$router || microRouter;
      targetRouter.push({
        path: data.defaultPath,
        params: data.routeParams,
        name: data.routeName,
      });
    }
  });
}
