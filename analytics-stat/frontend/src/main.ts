import './public-path'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

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
  // 在qiankun环境中，直接挂载到qiankun提供的容器
  // 在独立运行时，挂载到#app元素
  app.mount(container ? container : '#app')
}

// 导出qiankun生命周期函数
export async function bootstrap() {
  console.log('[analytics-stat] app bootstraped')
}

export async function mount(props: any) {
  console.log('[analytics-stat] props from main framework', props)
  render(props)
}

export async function unmount() {
  console.log('[analytics-stat] app unmount')
  if (app) {
    app.unmount()
    app = null
  }
}

// 独立运行时
if (!(window as any).__POWERED_BY_QIANKUN__) {
  console.log('[analytics-stat] Running in standalone mode')
  render()
}


