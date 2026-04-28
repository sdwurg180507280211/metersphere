/**
 * analytics-stat 微服务入口文件
 *
 * 技术栈：Vue 3.4 + Vue Router 4 + Pinia + Naive UI + TypeScript
 *
 * 生命周期模式：micro-app UMD 生命周期
 * - window.mount(): 每次进入子应用时调用
 * - window.unmount(): 每次离开子应用时调用，销毁 Vue 实例
 *
 * 与 Vue 2 版本的关键差异：
 * - 不再依赖 metersphere-frontend（Vue 2 SDK），所有组件独立实现
 * - 使用 Naive UI 作为 UI 组件库
 * - 使用 Composition API + <script setup> 替代 Options API
 * - 使用 vue-i18n 9.x 替代 8.x
 */
import { createApp } from 'vue'
import type { App as VueApp } from 'vue'
import { createPinia } from 'pinia'
import PersistedState from 'pinia-plugin-persistedstate'

import App from './App.vue'
import { createAppRouter } from './router'
import { i18n } from './i18n'
import { isMicroAppEnv } from './micro-app-env'

import type { Router } from 'vue-router'

let app: VueApp | null = null
let router: Router | null = null

/**
 * 渲染函数
 *
 * 【关键】micro-app with 沙箱解析子应用 HTML 时，空的 <div id="app"></div>
 * 可能被丢弃。因此 mount 时需要确保挂载点存在，不存在则手动创建。
 */
function mount() {
  // 确保挂载点 #app 存在（micro-app 可能丢弃空 div）
  let appEl = document.querySelector('#app')
  if (!appEl) {
    appEl = document.createElement('div')
    appEl.id = 'app'
    document.body.appendChild(appEl)
  }

  // 每次 mount 创建新的 router 实例，避免微前端环境下路由状态残留
  router = createAppRouter()

  const pinia = createPinia()
  pinia.use(PersistedState)

  app = createApp(App)
  app.use(router)
  app.use(pinia)
  app.use(i18n)
  app.mount('#app')
}

/**
 * 卸载函数
 * 销毁 Vue 实例，释放资源
 */
function unmount() {
  app?.unmount()
  app = null
  router = null
}

// micro-app UMD 生命周期模式
// micro-app 会在子应用渲染时自动调用 window.mount()
window.mount = () => { mount() }
window.unmount = () => { unmount() }

// 非微前端环境直接挂载（独立开发调试用）
if (!isMicroAppEnv()) {
  mount()
}
