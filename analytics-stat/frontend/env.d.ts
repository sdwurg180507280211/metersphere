/// <reference types="vite/client" />

/**
 * Vue 单文件组件类型声明
 * 让 TypeScript 识别 .vue 文件的默认导出
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

/**
 * micro-app 注入的全局变量类型声明
 */
interface Window {
  /** micro-app 环境标识 */
  __MICRO_APP_ENVIRONMENT__?: boolean
  /** micro-app 注入的子应用公共路径 */
  __MICRO_APP_PUBLIC_PATH__?: string
  /** micro-app 注入的子应用名称 */
  __MICRO_APP_NAME__?: string
  /** micro-app 注入的子应用基础路由 */
  __MICRO_APP_BASE_ROUTE__?: string
  /** micro-app UMD 生命周期 - 渲染 */
  mount: () => void
  /** micro-app UMD 生命周期 - 卸载 */
  unmount: () => void
  /** micro-app 子应用通信对象 */
  microApp?: {
    getData: () => any
    dispatch: (data: any, callback?: (res: any) => void) => void
    addDataListener: (fn: (data: any) => void, autoTrigger?: boolean) => void
    removeDataListener: (fn: (data: any) => void) => void
    clearDataListener: () => void
    setGlobalData: (data: any) => void
    getGlobalData: () => any
    location: Location
    router: any
  }
}
