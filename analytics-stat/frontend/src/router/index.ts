/**
 * Vue Router 4 配置
 *
 * 路由结构：
 * - / → 重定向到 /analytics/home
 * - /analytics → 二级布局（AnalyticsStat.vue）
 *   - /analytics/home → 工作台首页
 *   - /analytics/sql-console → SQL 查询台
 *   - /analytics/data-dictionary → 数据字典
 *
 * 与 Vue 2 版本的差异：
 * - 使用 createRouter / createWebHashHistory 替代 new Router
 * - 不再依赖 metersphere-frontend 的 Layout 和 login 组件
 * - 微前端环境下不需要 PassThrough 透传组件（Vue 3 版本不嵌套 SDK Layout）
 */
import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw, Router } from 'vue-router'

/** 路由配置 */
const routes: RouteRecordRaw[] = [
  // 根路径重定向到工作台首页
  { path: '/', redirect: '/analytics/home' },

  // 分析统计模块路由
  {
    path: '/analytics',
    name: 'analytics',
    redirect: '/analytics/home',
    // 二级布局组件：顶部导航 + 左侧菜单 + 右侧内容
    component: () => import('@/business/AnalyticsStat.vue'),
    children: [
      {
        path: 'home',
        name: 'analyticsHome',
        component: () => import('@/business/home/AnalyticsStatHome.vue'),
        meta: { title: '工作台' },
      },
      {
        path: 'sql-console',
        name: 'sqlConsole',
        component: () => import('@/views/SqlConsole.vue'),
        meta: { title: 'SQL查询台' },
      },
      {
        path: 'data-dictionary',
        name: 'dataDictionary',
        component: () => import('@/views/DataDictionary.vue'),
        meta: { title: '数据字典' },
      },
    ],
  },
]

/**
 * 创建路由实例的工厂函数
 * 每次 micro-app mount 时创建新实例，避免路由状态残留
 */
export function createAppRouter(): Router {
  return createRouter({
    // 使用 hash 模式，与主应用路由保持一致
    history: createWebHashHistory(),
    routes,
    // 路由切换时滚动到顶部
    scrollBehavior: () => ({ top: 0 }),
  })
}
