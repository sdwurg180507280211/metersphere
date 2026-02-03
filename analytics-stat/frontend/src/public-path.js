/**
 * qiankun 动态 publicPath 配置
 * 
 * 作用：在 qiankun 环境下，动态设置 webpack 的 publicPath
 * 确保子应用的静态资源（JS、CSS、图片等）能够正确加载
 * 
 * 原理：
 * - qiankun 会注入 __INJECTED_PUBLIC_PATH_BY_QIANKUN__ 变量
 * - 该变量包含子应用的实际访问路径
 * - 通过设置 __webpack_public_path__，webpack 会使用该路径加载资源
 */

// eslint-disable-next-line no-undef
if (window.__POWERED_BY_QIANKUN__) {
  // qiankun 环境下，使用动态注入的 publicPath
  // eslint-disable-next-line no-undef
  __webpack_public_path__ = window.__INJECTED_PUBLIC_PATH_BY_QIANKUN__;
  console.log('[analytics-stat] Running in qiankun mode, publicPath:', __webpack_public_path__);
}
