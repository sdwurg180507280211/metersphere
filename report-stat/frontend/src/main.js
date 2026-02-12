// public-path.js 必须在最顶部引入，确保资源路径在其他模块加载前设置好
import './public-path';
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

/**
 * 渲染函数
 *
 * 【重要】不再依赖 micro-app 的 UMD 生命周期自动检测来调用 mount()。
 * 新策略：子应用始终自行挂载，保留 window.unmount 供 micro-app 卸载时调用。
 */
function mount() {
  Vue.prototype.$EventBus = window.__MICRO_APP_ENVIRONMENT__
    ? createEventBusAdapter()
    : new Vue();

  instance = new Vue({
    i18n,
    router,
    pinia,
    render: (h) => h(App),
  }).$mount('#app');
}

window.unmount = () => {
  if (instance) {
    instance.$destroy();
    instance.$el.innerHTML = '';
    instance = null;
  }
};

mount();
