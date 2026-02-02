import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 定义路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/',
    component: () => import('@/business/AnalyticsStat.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据概览' }
      },
      {
        path: 'sql-console',
        name: 'SqlConsole',
        component: () => import('@/views/SqlConsole.vue'),
        meta: { title: 'SQL查询台' }
      },
      {
        path: 'data-dictionary',
        name: 'DataDictionary',
        component: () => import('@/views/DataDictionary.vue'),
        meta: { title: '数据字典' }
      }
    ]
  }
]

// 在 qiankun 环境下，不使用 BASE_URL，让 qiankun 自动处理路径
// 独立运行时，使用 BASE_URL
const base = (window as any).__POWERED_BY_QIANKUN__ ? '/' : import.meta.env.BASE_URL

const router = createRouter({
  history: createWebHistory(base),
  routes
})

export default router
