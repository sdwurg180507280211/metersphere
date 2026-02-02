/**
 * qiankun 动态 publicPath 配置
 * 用于支持微前端模式下的资源加载
 */

// 在qiankun环境下，使用动态注入的publicPath
if ((window as any).__POWERED_BY_QIANKUN__) {
  // Vite项目需要在运行时设置base
  // 注意：Vite不支持__webpack_public_path__，需要在vite.config.ts中配置
  console.log('[analytics-stat] Running in qiankun mode')
}

// 独立运行时，可以在这里做一些初始化工作
if (!(window as any).__POWERED_BY_QIANKUN__) {
  console.log('[analytics-stat] Running in standalone mode')
}
