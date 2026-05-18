/**
 * analytics-stat 微服务入口文件
 * 
 * 技术栈：Vue 2.7 + Vue Router 3 + Element UI + qiankun 2.x
 * 
 * 集成说明：
 * - 使用 metersphere-frontend 的统一样式、图标、插件、指令、过滤器
 * - 使用 metersphere-frontend 的 Layout 布局组件和 login 登录组件
 * - 使用 metersphere-frontend 的路由权限控制
 * 
 * 生命周期函数说明：
 * - bootstrap: 子应用初始化时调用一次
 * - mount: 每次进入子应用时调用
 * - unmount: 每次离开子应用时调用
 * - update: 主应用更新 props 时调用
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

// 关闭生产环境提示
Vue.config.productionTip = false;

// 初始化 Pinia 状态管理
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
 * @param {Object} props - qiankun 传递的 props
 * @param {HTMLElement} props.container - qiankun 提供的挂载容器
 * @param {Vue} props.eventBus - 主应用传递的事件总线
 */
function render(props = {}) {
  const { container, eventBus = new Vue() } = props;
  
  // 添加全局事件总线，用于与主应用通信
  Vue.prototype.$EventBus = eventBus;
  
  // 创建 Vue 实例
  instance = new Vue({
    i18n,
    router,
    pinia,
    render: (h) => h(App),
  }).$mount(container ? container.querySelector("#app") : "#app");

  // 解决 qiankun 下 vue-devtools 不显示的问题
  if (process.env.NODE_ENV === "development") {
    const instanceDiv = document.createElement("div");
    instanceDiv.__vue__ = instance;
    document.body.appendChild(instanceDiv);
  }
}

// 独立运行时（非 qiankun 环境）
if (!window.__POWERED_BY_QIANKUN__) {
  console.log("[analytics-stat] Running in standalone mode");
  render();
}

/**
 * qiankun 生命周期 - bootstrap
 * 子应用初始化时调用一次，下次进入时不会再触发
 * 通常用于全局变量初始化、不会在 unmount 阶段被销毁的缓存等
 */
export async function bootstrap(props) {
  console.log("[analytics-stat] app bootstraped");
}

/**
 * qiankun 生命周期 - mount
 * 每次进入子应用时调用，触发应用渲染
 */
export async function mount(props) {
  console.log("[analytics-stat] props from main framework", props);
  // 监听全局状态变化
  props.onGlobalStateChange((state, prev) => {
    // state: 变更后的状态; prev: 变更前的状态
    console.log("[analytics-stat] global state changed", state, prev);
  });
  // 设置全局状态
  props.setGlobalState({ event: "opendialog" });
  render(props);
}

/**
 * qiankun 生命周期 - unmount
 * 每次离开子应用时调用，销毁 Vue 实例
 */
export async function unmount(props) {
  console.log("[analytics-stat] app unmount");
  if (instance) {
    instance.$destroy();
    instance.$el.innerHTML = "";
    instance = null;
  }
}

/**
 * qiankun 生命周期 - update
 * 主应用更新 props 时调用
 * 目前主要用于路由参数更新
 */
export async function update(props) {
  const { defaultPath, routeParams, routeName } = props;
  // 微服务过来的路由
  if (defaultPath || routeName) {
    router.push({
      path: defaultPath,
      params: routeParams,
      name: routeName,
    });
  }
}
