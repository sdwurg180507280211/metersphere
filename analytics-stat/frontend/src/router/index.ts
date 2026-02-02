import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 定义路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  // 兼容主应用的路由路径 /#/analytics-stat
  {
    path: '/analytics-stat',
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

// 子应用使用Hash模式，与主应用的Hash路由兼容
// 在qiankun环境下，子应用的路由会嵌套在主应用的hash路由中
const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
