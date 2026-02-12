/**
 * micro-app 微前端框架初始化配置
 *
 * 本文件负责初始化京东 micro-app 微前端框架，替代 qiankun 的注册和启动逻辑。
 * 与现有 micro-app.js（qiankun 逻辑）并行运行，支持渐进式迁移。
 *
 * 【设计决策】
 * - 不在全局设置 iframe: true，只有 Vue 3 + Vite 子应用需要 iframe 沙箱
 * - Vue 2 + Webpack 子应用使用默认的 with 沙箱即可
 * - Vite 子应用通过 <micro-app iframe> 标签属性单独开启
 */
import microApp from '@micro-zoe/micro-app';
import Vue from 'vue';
// 引入模块配置表，用于预加载时判断 Vite 子应用是否需要 iframe 沙箱
import { MIGRATED_MODULES } from './micro-app-config';

// 【关键】Vue 2 必须忽略 micro-app 自定义元素
// 否则 Vue 会对 <micro-app> 标签报 "Unknown custom element" 警告
Vue.config.ignoredElements = ['micro-app'];

// 初始化 micro-app 框架
microApp.start({
  // 开启 fiber 模式：异步执行子应用 JS，减少主线程阻塞
  // MeterSphere 有 8 个子应用，fiber 模式可改善首屏性能
  fiber: true,
  // 全局生命周期回调，用于监控子应用加载状态
  lifeCycles: {
    created(e) {
      console.log('[micro-app] 子应用容器已创建', e.detail.name);
    },
    beforemount(e) {
      console.log('[micro-app] 子应用即将挂载', e.detail.name);
    },
    mounted(e) {
      console.log('[micro-app] 子应用已挂载', e.detail.name);
    },
    unmount(e) {
      console.log('[micro-app] 子应用已卸载', e.detail.name);
    },
    error(e) {
      console.error('[micro-app] 子应用加载出错', e.detail.name, e.detail.error);
    },
  },
});

/**
 * 根据服务信息和当前环境计算子应用入口 URL
 *
 * - 开发环境：使用本地地址 //127.0.0.1:{port-4000}
 *   （前端端口约定为后端端口减 4000，例如后端 8004 → 前端 4004）
 * - 生产环境：使用当前域名 + 服务路径 {origin}/{serviceId}
 *   （通过网关反向代理访问各子应用静态资源）
 *
 * @param {Object} svc - 服务信息对象，来自 GET /services 接口
 * @param {string} svc.serviceId - 服务标识，如 'api-test'
 * @param {number} svc.port - 后端端口号，如 8004
 * @returns {string} 子应用入口 URL
 */
function getEntryUrl(svc) {
  if (process.env.NODE_ENV === 'development') {
    // 开发环境：前端端口 = 后端端口 - 4000
    return '//127.0.0.1:' + (svc.port - 4000);
  }
  // 生产环境：通过网关反向代理，路径规则 /{serviceId}/
  return window.location.origin + '/' + svc.serviceId;
}

/**
 * 预加载子应用资源
 *
 * 在获取服务列表后调用，利用 micro-app 的 preFetch API
 * 在浏览器空闲时预加载子应用资源，提升模块切换体验。
 *
 * 【注意】Vite 子应用预加载时需设置 iframe: true，
 * 因为 Vite 输出的 <script type="module"> 需要 iframe 沙箱环境。
 *
 * @param {Array<Object>} services - 服务列表，来自 GET /services 接口
 */
export function preFetchApps(services) {
  const apps = services
    // 排除网关服务，网关不是子应用
    .filter(svc => svc.serviceId !== 'gateway')
    .map(svc => ({
      name: svc.serviceId,
      url: getEntryUrl(svc),
      // Vite 子应用预加载时也需设置 iframe: true
      // 因为 Vite 输出的 ES Module 需要 iframe 沙箱才能正确加载
      ...(MIGRATED_MODULES[svc.serviceId]?.isViteApp ? { iframe: true } : {}),
    }));

  // 延迟 3 秒执行预加载，避免影响首屏渲染
  microApp.preFetch(apps, 3000);
}
