/**
 * analytics-stat 微服务入口文件
 * 
 * 技术栈：Vue 2.7 + Vue Router 3 + Element UI
 * 微前端：qiankun 2.x
 * 
 * 生命周期函数说明：
 * - bootstrap: 子应用初始化时调用一次
 * - mount: 每次进入子应用时调用
 * - unmount: 每次离开子应用时调用
 */
import './public-path';
import Vue from 'vue';
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import App from './App.vue';
import router from './router';
import { createPinia, PiniaVuePlugin } from 'pinia';
import PersistedState from 'pinia-plugin-persistedstate';

// 关闭生产环境提示
Vue.config.productionTip = false;

// 初始化 Pinia 状态管理
const pinia = createPinia();
pinia.use(PersistedState);

// 注册 Element UI
Vue.use(ElementUI, {
  size: 'small',
});

// 注册 Pinia
Vue.use(PiniaVuePlugin);

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
    router,
    pinia,
    render: (h) => h(App),
  }).$mount(container ? container.querySelector('#app') : '#app');
}

// 独立运行时（非 qiankun 环境）
if (!window.__POWERED_BY_QIANKUN__) {
  console.log('[analytics-stat] Running in standalone mode');
  render();
}

/**
 * qiankun 生命周期 - bootstrap
 * 子应用初始化时调用一次，下次进入时不会再触发
 * 通常用于全局变量初始化、不会在 unmount 阶段被销毁的缓存等
 */
export async function bootstrap(props) {
  console.log('[analytics-stat] app bootstraped');
}

/**
 * qiankun 生命周期 - mount
 * 每次进入子应用时调用，触发应用渲染
 */
export async function mount(props) {
  console.log('[analytics-stat] props from main framework', props);
  // 监听全局状态变化（与其他模块保持一致）
  props.onGlobalStateChange((state, prev) => {
    // state: 变更后的状态; prev 变更前的状态
    console.log('[analytics-stat] global state changed', state, prev);
  });
  // 设置全局状态（与其他模块保持一致）
  props.setGlobalState({ event: 'opendialog' });
  render(props);
}

/**
 * qiankun 生命周期 - unmount
 * 每次离开子应用时调用，销毁 Vue 实例
 */
export async function unmount(props) {
  console.log('[analytics-stat] app unmount');
  if (instance) {
    instance.$destroy();
    instance.$el.innerHTML = '';
    instance = null;
  }
}

/**
 * qiankun 生命周期 - update
 * 主应用更新 props 时调用（与其他模块保持一致）
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
