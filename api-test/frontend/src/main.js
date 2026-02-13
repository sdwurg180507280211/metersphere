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
// 【新增】引入 micro-app 环境检测工具，兼容 inline 模式
import { isMicroAppEnv } from 'metersphere-frontend/src/utils/micro-app-env';

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
 * micro-app UMD 生命周期模式：micro-app 检测到 window.mount 后自动调用。
 * 配合主应用 <micro-app inline> 属性，确保脚本在沙箱内执行。
 *
 * 【路由选择逻辑】
 * - micro-app 子应用模式（整个模块加载）：使用 router（完整路由表，含 /api/home 等）
 * - 按需加载模式（被其他模块嵌入，如报告查看）：使用 microRouter（仅含嵌入路由）
 * - 独立运行模式：使用 router
 *
 * @param {Object} data - 可选，路由参数（micro-app 或按需加载场景传入）
 */
function mount(data) {
  // 创建 EventBus：
  // - micro-app 环境下：使用适配器，桥接 micro-app 数据通信到本地 EventBus
  // - 独立运行时：使用普通 Vue 实例（与原 qiankun 独立运行时行为一致）
  // 【关键】inline 模式下 window.__MICRO_APP_ENVIRONMENT__ 为 undefined，使用 isMicroAppEnv()
  Vue.prototype.$EventBus = isMicroAppEnv()
    ? createEventBusAdapter()
    : new Vue();

  // 【路由选择】
  // micro-app 子应用模式：使用完整路由（router），因为需要渲染 /api/home 等页面
  // 按需加载模式（非 micro-app 环境，由其他模块通过 data 参数嵌入）：使用 microRouter
  const isMicroAppMode = isMicroAppEnv();
  const isEmbedMode = !isMicroAppMode && data && (data.defaultPath || data.routeName);
  const useRouter = isEmbedMode ? microRouter : router;

  // 确保挂载点 #app 存在（micro-app with 沙箱可能丢弃空 div）
  let appEl = document.querySelector('#app');
  if (!appEl) {
    appEl = document.createElement('div');
    appEl.id = 'app';
    document.body.appendChild(appEl);
  }

  instance = new Vue({
    i18n,
    router: useRouter,
    pinia,
    render: (h) => h(App),
  }).$mount(appEl);

  // micro-app 模式下，根据主应用传入的 defaultPath 导航到对应页面
  if (isMicroAppMode && data && data.defaultPath) {
    router.push({ path: data.defaultPath }).catch(() => {});
  }

  // 按需加载模式下，使用 microRouter 导航
  // 场景：test-track 中嵌入接口测试报告，传入 defaultPath 指向具体的报告页面
  if (isEmbedMode) {
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
  // 典型场景：MicroAppWrapper 组件的 to/routeParams 属性变化时，
  // 主应用通过 setData 发送新的路由参数，子应用通过 addDataListener 接收并跳转
  // 【关键】inline 模式下 window.__MICRO_APP_ENVIRONMENT__ 为 undefined，使用 isMicroAppEnv()
  if (isMicroAppEnv()) {
    window.microApp?.addDataListener((newData) => {
      if (newData && (newData.defaultPath || newData.routeName)) {
        // micro-app 模式下使用完整路由（instance.$router 即 router）
        const targetRouter = instance?.$router || router;
        targetRouter.push({
          path: newData.defaultPath,
          params: newData.routeParams,
          name: newData.routeName,
        }).catch(() => {});
      }
    });
  }
};

window.unmount = () => {
  if (instance) {
    instance.$destroy();
    instance.$el.innerHTML = ''; // 清空 DOM 内容，防止残留
    instance = null;             // 释放 Vue 实例引用
  }
};

// 非微前端环境直接挂载
// 【关键】inline 模式下 window.__MICRO_APP_ENVIRONMENT__ 为 undefined，
// 必须使用 isMicroAppEnv() 检测，否则子应用会在 micro-app 环境下自动 mount，
// 与 micro-app 调用 window.mount() 冲突导致双重挂载
if (!isMicroAppEnv()) {
  mount();
}
