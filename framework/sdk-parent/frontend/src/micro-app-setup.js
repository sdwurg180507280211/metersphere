/**
 * micro-app 微前端框架初始化配置
 *
 * 本文件负责初始化京东 micro-app 微前端框架。
 *
 * 【设计决策】
 * - 不在全局设置 iframe: true，只有 Vue 3 + Vite 子应用需要 iframe 沙箱
 * - Vue 2 + Webpack 子应用使用默认的 with 沙箱
 * - Vite 子应用通过 <micro-app iframe> 标签属性单独开启
 */
import microApp from '@micro-zoe/micro-app';
import Vue from 'vue';
// 引入模块配置表和共享工具函数
import { MIGRATED_MODULES, getEntryUrl, isViteApp } from './micro-app-config';

// 【关键】Vue 2 必须忽略 micro-app 自定义元素
// 否则 Vue 会对 <micro-app> 标签报 "Unknown custom element" 警告
Vue.config.ignoredElements = ['micro-app'];

// 初始化 micro-app 框架
microApp.start({
  // 开启 fiber 模式：异步执行子应用 JS，减少主线程阻塞
  // MeterSphere 有 8 个子应用，fiber 模式可改善首屏性能
  fiber: true,
  /**
   * 自定义 fetch —— 修复生产环境子应用静态资源路径问题
   *
   * 【问题背景】
   * 子应用 vue.config.js 中 publicPath: "/"，构建产物 HTML 中资源路径为绝对路径：
   *   <script src="/js/performance-index.88d254c6.js">
   *   <link href="/css/performance-index.5e60ba21.css">
   *
   * micro-app 的 CompletionPath() 对绝对路径（以 / 开头）只补全 origin，不补全路径：
   *   CompletionPath("/js/xxx", "http://host/performance/")
   *   → "http://host/js/xxx"  （期望 "http://host/performance/js/xxx"）
   *
   * 导致请求到达 Gateway 时，Gateway 的 static 目录中没有子应用的 JS/CSS，
   * 请求 fallback 到主应用 index.html，返回 HTML 而非 JS → 解析失败。
   *
   * 【修复方案】
   * 在 custom fetch 中检测：如果请求 URL 的路径以 /js/ 或 /css/ 开头，
   * 且 appName 对应一个已注册的子应用，则在路径前补全 /{appName}/ 前缀，
   * 使请求正确路由到子应用后端的 static 目录。
   *
   * 【影响范围】
   * - 仅影响生产环境（开发环境子应用直接访问 dev server，不经过 Gateway）
   * - 仅影响 HTML 中以绝对路径引用的初始 JS/CSS（动态 chunk 由 __webpack_public_path__ 控制）
   * - 不影响 API 请求（API 路径不以 /js/ 或 /css/ 开头）
   */
  fetch(url, options, appName) {
    // 仅在生产环境且有子应用名称时进行路径修正
    if (process.env.NODE_ENV !== 'development' && appName && MIGRATED_MODULES[appName]) {
      try {
        const urlObj = new URL(url, window.location.origin);
        const pathname = urlObj.pathname;
        // 检测绝对路径的静态资源：/js/xxx、/css/xxx
        // 注意：fonts/img 等二进制资源由浏览器原生加载，此处不拦截
        // 且路径中尚未包含 /{appName}/ 前缀（避免重复补全）
        if (/^\/(js|css)\//.test(pathname) && !pathname.startsWith('/' + appName + '/')) {
          // 补全路径前缀：/js/xxx → /{appName}/js/xxx
          urlObj.pathname = '/' + appName + pathname;
          const newUrl = urlObj.toString();
          return window.fetch(newUrl, options).then(res => res.text());
        }
      } catch (e) {
        // URL 解析失败时不阻塞，降级为默认行为
          console.warn('[micro-app] 资源路径修正失败:', url, e);
      }
    }
    return window.fetch(url, options).then(res => res.text());
  },
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
      url: getEntryUrl(svc.serviceId),
      // Vite 子应用预加载时也需设置 iframe: true
      // 因为 Vite 输出的 ES Module 需要 iframe 沙箱才能正确加载
      ...(isViteApp(svc.serviceId) ? { iframe: true } : {}),
    }));

  // 延迟 3 秒执行预加载，避免影响首屏渲染
  microApp.preFetch(apps, 3000);
}
