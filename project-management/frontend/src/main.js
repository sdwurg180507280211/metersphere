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
import VuePapaParse from 'vue-papa-parse';
import VueShepherd from 'vue-shepherd'; // 新手引导
import 'metersphere-frontend/src/assets/shepherd/shepherd-theme.css';
import { gotoCancel, gotoNext } from 'metersphere-frontend/src/utils';
// 【新增】引入 EventBus 兼容适配器，替代从 qiankun props 接收 eventBus
import { createEventBusAdapter } from 'metersphere-frontend/src/utils/micro-app-event-bus';
// 【新增】引入 micro-app 环境检测工具
import { isMicroAppEnv } from 'metersphere-frontend/src/utils/micro-app-env';

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
Vue.use(VuePapaParse);
Vue.use(VueShepherd);

Vue.prototype.gotoCancel = gotoCancel;
Vue.prototype.gotoNext = gotoNext;

let instance = null;

/**
 * 渲染函数
 *
 * 【关键】micro-app with 沙箱解析子应用 HTML 时，空的 <div id="app"></div>
 * 可能被丢弃（不会出现在 micro-app-body 中）。因此 mount 时需要确保
 * 挂载点存在，不存在则手动创建。
 */
function mount() {
  // 创建 EventBus
  // micro-app 环境下使用适配器桥接通信，独立运行时使用普通 Vue 实例
  Vue.prototype.$EventBus = isMicroAppEnv()
    ? createEventBusAdapter()
    : new Vue();

  // 确保挂载点 #app 存在（micro-app 可能丢弃空 div）
  let appEl = document.querySelector('#app');
  if (!appEl) {
    appEl = document.createElement('div');
    appEl.id = 'app';
    document.body.appendChild(appEl);
  }

  instance = new Vue({
    i18n,
    router,
    pinia,
    render: (h) => h(App),
  }).$mount(appEl);
}

// micro-app UMD 生命周期模式
// micro-app 会在子应用渲染时自动调用 window.mount()
// 非微前端环境直接调用 mount()
window.mount = () => { mount(); };
window.unmount = () => {
  if (instance) {
    instance.$destroy();
    instance.$el.innerHTML = '';
    instance = null;
  }
};

if (!isMicroAppEnv()) {
  mount();
}
