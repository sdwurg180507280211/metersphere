import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import qiankun from 'vite-plugin-qiankun'

// https://vitejs.dev/config/
export default defineConfig({
  // 使用相对路径，让qiankun自动处理资源路径
  // 开发环境使用根路径，生产环境使用相对路径
  base: process.env.NODE_ENV === 'production' ? '/analytics-stat/' : '/',
  plugins: [
    vue(),
    qiankun('analytics-stat', {
      useDevMode: true
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 4009,
    host: '0.0.0.0',
    cors: true, // 允许跨域，qiankun需要
    origin: 'http://localhost:4009', // 开发环境的origin
    headers: {
      'Access-Control-Allow-Origin': '*' // qiankun需要
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8009', // 代理到analytics-stat后端服务
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    target: 'es2015',
    rollupOptions: {
      output: {
        // 保持原有的代码分割配置
        manualChunks: {
          'element-plus': ['element-plus'],
          'echarts': ['echarts', 'vue-echarts']
        }
      }
    }
  }
})


