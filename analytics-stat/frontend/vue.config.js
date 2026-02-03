const path = require('path');
const { name } = require('./package');
const { defineConfig } = require('@vue/cli-service');

function resolve(dir) {
  return path.join(__dirname, dir);
}

module.exports = defineConfig({
  // 与其他模块保持一致，使用根路径
  // qiankun 会通过 Gateway 的 SessionFilter 转发静态资源请求
  publicPath: '/',
  productionSourceMap: false,
  devServer: {
    port: 4009,
    client: {
      webSocketTransport: 'sockjs',
      overlay: false,
    },
    allowedHosts: 'all',
    webSocketServer: 'sockjs',
    proxy: {
      // 排除登录和文档路径，其他请求代理到后端
      ['^((?!/login)(?!/document))']: {
        target: 'http://localhost:8009',
        ws: false,
      },
    },
    // 跨域配置,支持qiankun加载
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
  },
  configureWebpack: {
    devtool: 'cheap-module-source-map',
    resolve: {
      alias: {
        '@': resolve('src'),
        // 确保 vue-i18n 使用本地版本，避免与 metersphere-frontend 冲突
        'vue-i18n': resolve('node_modules/vue-i18n'),
      },
    },
    output: {
      // 把子应用打包成 umd 库格式(qiankun必须)
      library: `${name}-[name]`,
      libraryTarget: 'umd',
      chunkLoadingGlobal: `webpackJsonp_${name}`,
      // 打包后js的名称
      filename: `js/${name}-[name].[contenthash:8].js`,
      chunkFilename: `js/${name}-[name].[contenthash:8].js`,
    },
  },
  css: {
    // 将组件内的 CSS 提取到一个单独的 CSS 文件
    extract: {
      ignoreOrder: true,
      filename: `css/${name}-[name].[contenthash:8].css`,
      chunkFilename: `css/${name}-[name].[contenthash:8].css`,
    },
  },
  /**
   * chainWebpack 配置
   * 
   * SVG 图标处理说明：
   * 1. 默认的 svg 规则会将 svg 文件作为普通图片处理
   * 2. 我们需要将 metersphere-frontend 的图标目录排除
   * 3. 然后用 svg-sprite-loader 处理这些图标，生成 SVG Sprite
   * 4. 这样可以通过 <svg-icon icon-class="xxx" /> 使用图标
   */
  chainWebpack: (config) => {
    // 排除 metersphere-frontend 的图标目录，不使用默认的 svg 规则
    config.module
      .rule('svg')
      .exclude.add(
        resolve('../../framework/sdk-parent/frontend/src/assets/module')
      )
      .end();
    
    // 使用 svg-sprite-loader 处理 metersphere-frontend 的图标
    config.module
      .rule('icons')
      .test(/\.svg$/)
      .include.add(
        resolve('../../framework/sdk-parent/frontend/src/assets/module')
      )
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]',
      });
  },
});
