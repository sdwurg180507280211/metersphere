import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  // 生产环境使用相对路径
  base: '/analytics-stat/',
  plugins: [
    vue()
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 4009,
    host: '0.0.0.0',
    cors: true,
    headers: {
      'Access-Control-Allow-Origin': '*'
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8009',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    target: 'es2015',
    // 关键配置：生成library模式，暴露生命周期函数
    lib: {
      entry: resolve(__dirname, 'src/main.ts'),
      name: 'analyticsStat',
      formats: ['umd'],
      fileName: () => 'analytics-stat.js'
    },
    rollupOptions: {
      // 不要external任何依赖，全部打包
      external: [],
      output: {
        // UMD模式下的全局变量名
        name: 'analyticsStat',
        // 确保生命周期函数被正确导出
        exports: 'named'
      }
    }
  }
})


