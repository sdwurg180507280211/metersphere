/**
 * analytics-stat 微服务入口文件
 *
 * 技术栈：Vue 2.7 + Vue Router 3 + Element UI + micro-app
 *
 * 集成说明：
 * - 使用 metersphere-frontend 的统一样式、图标、插件、指令、过滤器
 * - 使用 metersphere-frontend 的 Layout 布局组件和 login 登录组件
 * - 使用 metersphere-frontend 的路由权限控制
 *
 * 生命周期模式：micro-app UMD 生命周期
 * - window.mount(data): 每次进入子应用时调用，data 由 micro-app 自动传入
 * - window.unmount(): 每次离开子应用时调用，销毁 Vue 实例
 */
import "./public-path";
import Vue from "vue";

// metersphere-frontend 统一样式（必须在 Element UI 之前引入）
import "metersphere-frontend/src/styles/index.scss";

import ElementUI from "element-ui";
import App from "./App.vue";
import i18n from "./i18n";
import router from "./router";
import { createPinia, PiniaVuePlugin } from "pinia";
import PersistedState from "pinia-plugin-persistedstate";

// metersphere-frontend 公共资源
import icons from "metersphere-frontend/src/icons";           // SVG 图标
import svg from "metersphere-frontend/src/components/svg";    // SVG 组件
import plugins from "metersphere-frontend/src/plugins";       // 公共插件
import directives from "metersphere-frontend/src/directive";  // 公共指令
import filters from "metersphere-frontend/src/filters";       // 公共过滤器
import "metersphere-frontend/src/router/permission";          // 路由权限控制
import chart from "metersphere-frontend/src/chart";           // 图表组件

// 新手引导
import VueShepherd from "vue-shepherd";
import "metersphere-frontend/src/assets/shepherd/shepherd-theme.css";
import { gotoCancel, gotoNext } from "metersphere-frontend/src/utils";

// 【新增】引入 EventBus 兼容适配器，替代从 qiankun props 接收 eventBus
import { createEventBusAdapter } from "metersphere-frontend/src/utils/micro-app-event-bus";
// 【新增】引入 micro-app 环境检测工具，兼容 inline 模式
import { isMicroAppEnv } from "metersphere-frontend/src/utils/micro-app-env";

// 关闭生产环境提示
Vue.config.productionTip = false;

// 【关键】Vue 插件注册放在 mount 外部，只执行一次
// UMD 生命周期模式的优势：模块频繁切换时不会重复注册插件
const pinia = createPinia();
pinia.use(PersistedState);  // 开启持久化，存储在 localStorage

// 注册 Element UI，集成 i18n
Vue.use(ElementUI, {
  i18n: (key, value) => i18n.t(key, value)
});

// 注册 metersphere-frontend 公共资源
Vue.use(svg);
Vue.use(icons);
Vue.use(plugins);
Vue.use(directives);
Vue.use(filters);
Vue.use(PiniaVuePlugin);
Vue.use(chart);
Vue.use(VueShepherd);

// 新手引导工具方法
Vue.prototype.gotoCancel = gotoCancel;
Vue.prototype.gotoNext = gotoNext;

// Vue 实例引用，用于 unmount 时销毁
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
  // 【关键】inline 模式下 window.__MICRO_APP_ENVIRONMENT__ 为 undefined，使用 isMicroAppEnv()
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
