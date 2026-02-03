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
      '/api': {
        target: 'http://localhost:8009',
        changeOrigin: true,
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
});
