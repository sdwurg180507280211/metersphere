/**
 * 子应用 vue.config.js output 配置改造模板（qiankun → micro-app）
 *
 * ============================================================
 * 【重要概念区分】
 * ============================================================
 * - webpack 的 `libraryTarget: 'umd'`：将代码打包为 UMD 模块格式（打包概念）
 *   → micro-app 不需要此配置，必须移除
 *
 * - micro-app 的「UMD 生命周期模式」：将渲染/卸载逻辑放入 window.mount / window.unmount（运行时概念）
 *   → 在 main.js 中实现，与 webpack 打包格式无关
 *
 * qiankun 同时要求两者（UMD 打包 + 生命周期导出）
 * micro-app 只需要 UMD 生命周期模式（main.js），不需要 UMD 打包格式（vue.config.js）
 * ============================================================
 *
 * 改造说明：
 * 1. 移除 `library` 和 `libraryTarget: 'umd'`
 *    - qiankun 要求子应用以 UMD 库格式打包，micro-app 不需要
 *    - 移除后子应用输出标准 webpack 产物，体积更小，构建更快
 *
 * 2. 保留 `chunkLoadingGlobal`
 *    - 避免多个子应用的 webpack chunk 加载函数命名冲突
 *    - 每个子应用使用独立的 `webpackJsonp_{name}` 前缀
 *
 * 3. 新增 `globalObject: 'window'`
 *    - micro-app 官方指南推荐配置
 *    - 确保全局对象指向 window，避免沙箱环境下的引用问题
 *    - webpack 默认的 globalObject 是 'self'，在 micro-app 沙箱中可能指向错误的上下文
 *
 * 4. 保留 CORS 头配置
 *    - 开发环境下子应用需要跨域访问，`Access-Control-Allow-Origin: *` 必须保留
 *    - 生产环境通过网关反向代理，不需要 CORS 头
 *
 * 5. 保留 filename / chunkFilename 命名规则
 *    - 带模块名前缀的文件命名避免多应用资源冲突
 *    - contenthash 确保缓存正确更新
 *
 * 适用范围：所有 8 个子应用
 *   - workstation, report-stat, analytics-stat
 *   - project-management, system-setting, performance-test
 *   - test-track, api-test
 *
 * 对应需求：Requirements 2.4, 5.1, 5.3
 */

// ============================================================
// 改造前（qiankun 要求 UMD 打包格式）
// ============================================================
// configureWebpack: {
//   output: {
//     // 把子应用打包成 umd 库格式（qiankun 必须）
//     library: `${name}-[name]`,        // ← 【移除】UMD 库名称，micro-app 不需要
//     libraryTarget: "umd",             // ← 【移除】UMD 打包格式，micro-app 不需要
//     chunkLoadingGlobal: `webpackJsonp_${name}`,
//     filename: `js/${name}-[name].[contenthash:8].js`,
//     chunkFilename: `js/${name}-[name].[contenthash:8].js`,
//   },
// }

// ============================================================
// 改造后（micro-app 不需要 UMD 打包格式）
// ============================================================
// configureWebpack: {
//   output: {
//     // 【已移除】library 和 libraryTarget（micro-app 不需要 UMD 打包格式）
//     // 注意：移除的是 webpack 的 UMD 打包配置，保留的是 main.js 中的 UMD 生命周期模式
//     chunkLoadingGlobal: `webpackJsonp_${name}`,              // 【保留】避免多应用 chunk 冲突
//     globalObject: 'window',                                   // 【新增】确保全局对象指向 window
//     filename: `js/${name}-[name].[contenthash:8].js`,         // 【保留】带模块名前缀避免冲突
//     chunkFilename: `js/${name}-[name].[contenthash:8].js`,    // 【保留】带模块名前缀避免冲突
//   },
// }

// ============================================================
// 完整的 output 配置示例（可直接复制到各子应用 vue.config.js）
// ============================================================
// 以 workstation 为例，name 来自 package.json 的 name 字段
const { name } = require('./package');

const outputConfig = {
  // micro-app 不需要 UMD 打包格式，因此不设置 library 和 libraryTarget
  // micro-app 通过 HTML Entry 加载子应用，直接解析 HTML 中的 <script> 标签
  // 子应用只需输出标准的 webpack 产物即可

  // 保留：每个子应用使用独立的 chunkLoadingGlobal，防止多应用 chunk 加载冲突
  // 如果不设置，所有子应用共用默认的 webpackJsonp，会导致 chunk 加载错乱
  chunkLoadingGlobal: `webpackJsonp_${name}`,

  // 新增：确保全局对象指向 window（micro-app 指南推荐）
  // webpack 默认 globalObject 为 'self'，在 micro-app 的 with 沙箱中
  // 'self' 可能指向沙箱代理对象而非真实 window，导致运行时错误
  globalObject: 'window',

  // 保留：带模块名前缀的文件命名，避免多应用资源文件名冲突
  filename: `js/${name}-[name].[contenthash:8].js`,
  chunkFilename: `js/${name}-[name].[contenthash:8].js`,
};

// ============================================================
// CORS 头配置（保持不变）
// ============================================================
// devServer: {
//   headers: {
//     'Access-Control-Allow-Origin': '*',  // 【保留】开发环境跨域必须
//   },
// }

module.exports = { outputConfig };
