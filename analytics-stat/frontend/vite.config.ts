/**
 * Vite 构建配置
 *
 * 关键说明：
 * - 作为 micro-app 子应用，必须配置 CORS 支持跨域加载
 * - 开发端口 4009，与原 Vue CLI 配置保持一致
 * - 生产构建输出 IIFE 格式（非 ESM），因为 micro-app 通过 fetch HTML →
 *   解析 <script> 标签 → appendChild 方式注入脚本，不支持 ES Module
 * - CSS 输出到 css/ 目录，JS 输出到 js/ 目录，与其他 Vue 2 子应用保持一致
 *   （gateway 静态资源路由只识别 /js/ 和 /css/ 前缀）
 * - 所有产物带 analytics-stat 前缀，避免多子应用 chunk 冲突
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import type { Plugin } from 'vite'
import prefixSelector from 'postcss-prefix-selector'

/**
 * 自定义 Vite 插件：移除 HTML 中的 type="module" 和 crossorigin 属性
 *
 * 原因：Vite 默认给所有注入的 <script> 标签添加 type="module"，
 * 但 micro-app 通过 appendChild 方式注入脚本时，
 * type="module" 会导致浏览器以 ESM 模式解析，
 * 即使 JS 内容是 IIFE 格式，也会因为严格模式差异出问题。
 *
 * 此插件在构建完成后处理 HTML：
 * 1. 移除 <script> 上的 type="module" → 变成普通 script
 * 2. 移除 crossorigin 属性 → 同源加载不需要
 * 3. 添加 defer 属性 → 与其他 Vue 2 子应用保持一致
 */
function removeModeulePlugin(): Plugin {
  return {
    name: 'remove-module-type',
    // enforce: 'post' 确保在 Vite 内置插件之后执行
    enforce: 'post',
    transformIndexHtml(html) {
      return html
        // 移除 type="module"，让 script 以 classic 模式加载
        .replace(/<script type="module"/g, '<script defer')
        // 移除 crossorigin 属性（同源加载不需要，micro-app 环境下可能导致 CORS 问题）
        .replace(/ crossorigin/g, '')
    },
  }
}

export default defineConfig({
  plugins: [
    vue(),
    removeModeulePlugin(),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 4009,
    // micro-app 跨域加载子应用必须配置 CORS
    cors: true,
    origin: 'http://127.0.0.1:4009',
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
    proxy: {
      // 排除登录和文档路径，其他请求代理到后端
      '^(?!/login)(?!/document)': {
        target: 'http://localhost:8009',
        changeOrigin: true,
      },
    },
  },
  /**
   * 【关键】PostCSS 配置：给 Element Plus 样式加 #analytics-app 作用域前缀
   *
   * 原因：analytics-stat 使用 Element Plus（Vue 3），主应用使用 Element UI（Vue 2）。
   * 两者有大量同名 CSS 选择器（如 .el-menu-item *、.el-button 等）。
   * micro-app 默认不隔离样式，子应用的 CSS 会注入到主应用 <head> 中，
   * 导致 Element Plus 的规则覆盖 Element UI 的规则（后加载的优先级更高）。
   *
   * 解决方案：使用 postcss-prefix-selector 给所有 CSS 规则加上 #analytics-app 前缀，
   * 使子应用样式只在 #analytics-app 容器内生效，不影响主应用。
   *
   * 例如：.el-menu-item * { vertical-align: bottom }
   * 变为：#analytics-app .el-menu-item * { vertical-align: bottom }
   * 这样就不会影响主应用侧边栏的 .el-menu-item
   */
  css: {
    postcss: {
      plugins: [
        prefixSelector({
          prefix: '#analytics-app',
          transform(prefix: string, selector: string, prefixedSelector: string) {
            // 跳过 html、body、:root 等全局选择器，不加前缀
            if (selector.match(/^(html|body|:root)/)) {
              return selector
            }
            // 如果选择器已经包含 #analytics-app，不重复添加
            if (selector.includes('#analytics-app')) {
              return selector
            }
            // @keyframes 内的选择器（from、to、百分比）不加前缀
            if (selector.match(/^(from|to|\d+%)/)) {
              return selector
            }
            return prefixedSelector
          },
        }),
      ],
    },
  },
  build: {
    target: 'es2015',
    /**
     * 【关键】禁用 CSS 代码分割，所有样式打包到一个文件
     * micro-app 环境下动态加载的 CSS chunk 可能路径不对
     */
    cssCodeSplit: false,
    rollupOptions: {
      output: {
        /**
         * 【关键】输出 IIFE 格式，不使用 ES Module
         *
         * 原因：micro-app 加载子应用的流程是：
         * 1. fetch 子应用 HTML
         * 2. 解析 <script> 标签
         * 3. 通过 appendChild 将 script 注入到主应用 DOM
         *
         * 这个过程不支持 <script type="module">，因为：
         * - appendChild 注入的 script 默认是 classic 模式
         * - ESM 的 export/import 语法在 classic 模式下会报 SyntaxError
         *
         * 使用 IIFE 格式后，所有代码被包裹在立即执行函数中，
         * 没有顶层 export，可以被 appendChild 正常执行
         */
        format: 'iife',
        /**
         * IIFE 格式不支持代码分割（code splitting），
         * 所有代码打包到一个入口文件
         */
        entryFileNames: 'js/analytics-stat-[name].[hash].js',
        /**
         * CSS 输出到 css/ 目录，与其他子应用保持一致
         * gateway 的静态资源路由规则识别 /css/ 前缀
         */
        assetFileNames: (assetInfo) => {
          // CSS 文件输出到 css/ 目录
          if (assetInfo.name && assetInfo.name.endsWith('.css')) {
            return 'css/analytics-stat-[name].[hash].[ext]'
          }
          // 其他资源（图片、字体等）输出到 assets/ 目录
          return 'assets/analytics-stat-[name].[hash].[ext]'
        },
      },
    },
  },
})
