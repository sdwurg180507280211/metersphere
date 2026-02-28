/**
 * Vue Router 4 配置
 *
 * 路由结构：
 * - / → 重定向到 知识库首页
 * - <base> → 二级布局（KnowledgeBaseLayout.vue）
 *   - <base>/knowledge → 知识库
 *   - <base>/knowledge/chat → 知识问答
 *   - <base>/home | <base>/sql-console | <base>/data-dictionary
 *     兼容历史入口，统一重定向到知识库（可通过 VITE_ENABLE_ANALYTICS_LEGACY_ROUTES=false 关闭）
 * - <base> 默认是 /analytics，可通过 VITE_KNOWLEDGE_ROUTE_BASE 配置
 *
 * 与 Vue 2 版本的差异：
 * - 使用 createRouter / createWebHashHistory 替代 new Router
 * - 不再依赖 metersphere-frontend 的 Layout 和 login 组件
 * - 微前端环境下不需要 PassThrough 透传组件（Vue 3 版本不嵌套 SDK Layout）
 */
import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw, Router } from 'vue-router'
import { KNOWLEDGE_ROUTE_BASE, KNOWLEDGE_ROUTE_PATHS } from '@/config/knowledge-route'

const enableLegacyRedirectRoutes = import.meta.env.VITE_ENABLE_ANALYTICS_LEGACY_ROUTES !== 'false'

const legacyRedirectRoutes: RouteRecordRaw[] = enableLegacyRedirectRoutes
  ? [
      {
        path: 'home',
        redirect: KNOWLEDGE_ROUTE_PATHS.knowledge,
      },
      {
        path: 'sql-console',
        redirect: KNOWLEDGE_ROUTE_PATHS.knowledge,
      },
      {
        path: 'data-dictionary',
        redirect: KNOWLEDGE_ROUTE_PATHS.knowledge,
      },
    ]
  : []

/** 路由配置 */
const routes: RouteRecordRaw[] = [
  // 根路径重定向到知识库
  { path: '/', redirect: KNOWLEDGE_ROUTE_PATHS.knowledge },

  // 分析统计模块路由
  {
    path: KNOWLEDGE_ROUTE_BASE,
    name: 'analytics',
    redirect: KNOWLEDGE_ROUTE_PATHS.knowledge,
    // 二级布局组件：顶部导航 + 左侧菜单 + 右侧内容
    component: () => import('@/business/KnowledgeBaseLayout.vue'),
    children: [
      ...legacyRedirectRoutes,
      {
        path: 'knowledge',
        name: 'knowledgeBase',
        component: () => import('@/views/KnowledgeBase.vue'),
        meta: { title: '知识库' },
      },
      {
        path: 'knowledge/chat',
        name: 'knowledgeChat',
        component: () => import('@/views/KnowledgeChat.vue'),
        meta: { title: '知识问答' },
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
