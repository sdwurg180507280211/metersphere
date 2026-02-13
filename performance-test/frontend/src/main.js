// public-path.js 必须在最顶部引入，确保资源路径在其他模块加载前设置好
import './public-path';
import Vue from 'vue';
import 'metersphere-frontend/src/styles/index.scss';
import ElementUI from 'element-ui';
import App from './App.vue';
import i18n from './i18n';
// 同时引入 router 和 microRouter
// - router：正常路由，用于全局路由激活场景（用户通过导航菜单进入 performance-test）
// - microRouter：内存路由，用于按需加载场景（被其他模块嵌入时，路由不影响浏览器地址栏）
import router, { microRouter } from './router';
import { createPinia, PiniaVuePlugin } from 'pinia';
import PersistedState from 'pinia-plugin-persistedstate';
import icons from 'metersphere-frontend/src/icons';
import svg from 'metersphere-frontend/src/components/svg';
import plugins from 'metersphere-frontend/src/plugins';
import directives from 'metersphere-frontend/src/directive';
import filters from 'metersphere-frontend/src/filters';
import chart from 'metersphere-frontend/src/chart';
import 'metersphere-frontend/src/router/permission';
import VueClipboard from 'vue-clipboard2';
import VueShepherd from 'vue-shepherd'; // 新手引导
import 'metersphere-frontend/src/assets/shepherd/shepherd-theme.css';
import { gotoCancel, gotoNext } from 'metersphere-frontend/src/utils';
// 【新增】引入 EventBus 兼容适配器，替代从 qiankun props 接收 eventBus
import { createEventBusAdapter } from 'metersphere-frontend/src/utils/micro-app-event-bus';

Vue.config.productionTip = false;

// 【关键】Vue 插件注册放在 mount 外部，只执行一次
// UMD 生命周期模式的优势：模块频繁切换时不会重复注册插件
const pinia = createPinia();
pinia.use(PersistedState); // 开启缓存，存储在 localStorage

Vue.use(ElementUI, {
  i18n: (key, value) => i18n.t(key, value),
  zIndex: 9000,
});

Vue.use(svg);
Vue.use(icons);
Vue.use(plugins);
Vue.use(directives);
Vue.use(filters);
Vue.use(chart);
Vue.use(PiniaVuePlugin);
Vue.use(VueClipboard);
Vue.use(VueShepherd);

Vue.prototype.gotoCancel = gotoCancel;
Vue.prototype.gotoNext = gotoNext;

let instance = null;

/**
 * 渲染函数（含按需加载路由处理）
 *
 * micro-app UMD 生命周期模式：micro-app 检测到 window.mount 后自动调用。
 * 配合主应用 <micro-app inline> 属性，确保脚本在沙箱内执行。
 *
 * @param {Object} data - 可选，路由参数（按需加载场景由主应用传入）
 */
function mount(data) {
  Vue.prototype.$EventBus = window.__MICRO_APP_ENVIRONMENT__
    ? createEventBusAdapter()
    : new Vue();

  // 根据 data 参数决定使用哪个路由实例
  const useRouter = (data && (data.defaultPath || data.routeName)) ? microRouter : router;

  instance = new Vue({
    i18n,
    router: useRouter,
    pinia,
    render: (h) => h(App),
  }).$mount('#app');

  if (data && (data.defaultPath || data.routeName)) {
    microRouter.push({
      path: data.defaultPath,
      params: data.routeParams,
      name: data.routeName,
    });
  }
}

// micro-app UMD 生命周期模式
// micro-app 会在子应用渲染时自动调用 window.mount(data)
// data 参数由主应用通过 <micro-app :data="appData"> 传入
window.mount = (data) => {
  // 确保挂载点 #app 存在（micro-app 沙箱可能丢弃空 div）
  let appEl = document.querySelector('#app');
  if (!appEl) {
    appEl = document.createElement('div');
    appEl.id = 'app';
    document.body.appendChild(appEl);
  }
  mount(data);

  // 注册数据监听（替代 qiankun 的 update 钩子）
  if (window.__MICRO_APP_ENVIRONMENT__) {
    window.microApp?.addDataListener((newData) => {
      if (newData && (newData.defaultPath || newData.routeName)) {
        const targetRouter = instance?.$router || microRouter;
        targetRouter.push({
          path: newData.defaultPath,
          params: newData.routeParams,
          name: newData.routeName,
        });
      }
    });
  }
};

window.unmount = () => {
  if (instance) {
    instance.$destroy();
    instance.$el.innerHTML = '';
    instance = null;
  }
};

// 非微前端环境直接挂载
if (!window.__MICRO_APP_ENVIRONMENT__) {
  mount();
}
