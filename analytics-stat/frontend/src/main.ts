import './public-path'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { renderWithQiankun, qiankunWindow } from 'vite-plugin-qiankun/dist/helper'

import App from './App.vue'
import router from './router'

let app: any = null

/**
 * 渲染函数
 * @param props qiankun传递的props
 */
function render(props: any = {}) {
  const { container } = props
  app = createApp(App)

  // 注册所有 Element Plus 图标
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus, {
    locale: zhCn,
  })

  // 挂载应用
  const containerElement = container ? container.querySelector('#app') : '#app'
  app.mount(containerElement)
}

// 使用vite-plugin-qiankun提供的渲染函数
renderWithQiankun({
  // bootstrap 只会在微应用初始化的时候调用一次
  bootstrap() {
    console.log('[analytics-stat] app bootstraped')
  },
  // 应用每次进入都会调用 mount 方法
  mount(props: any) {
    console.log('[analytics-stat] props from main framework', props)
    render(props)
  },
  // 应用每次切出/卸载会调用的unmount方法
  unmount() {
    console.log('[analytics-stat] app unmount')
    if (app) {
      app.unmount()
      app = null
    }
  },
  // 可选，仅使用 loadMicroApp 方式加载微应用时生效
  update(props: any) {
    console.log('[analytics-stat] app update', props)
  }
})

// 独立运行时
if (!qiankunWindow.__POWERED_BY_QIANKUN__) {
  render()
}


