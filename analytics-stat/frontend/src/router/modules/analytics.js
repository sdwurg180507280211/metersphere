/**
 * analytics 路由模块
 * 
 * 使用 metersphere-frontend 的统一布局组件 Layout
 * Layout 组件提供：顶部导航栏、侧边菜单、面包屑等统一 UI
 * 
 * 路由结构：
 * - /analytics/home - 工作台首页
 * - /analytics/sql-console - SQL 查询台
 * - /analytics/data-dictionary - 数据字典
 */
import Layout from "metersphere-frontend/src/business/app-layout";

export default {
  path: "/analytics",
  name: "analytics",
  redirect: "/analytics/home",
  component: Layout,  // 使用统一布局组件
  children: [
    {
      path: "home",
      name: "analyticsStatHome",
      component: () => import("@/business/home/AnalyticsStatHome.vue"),
      meta: { 
        title: "工作台",
        requiresAuth: true
      }
    },
    {
      path: "sql-console",
      name: "SqlConsole",
      component: () => import("@/views/SqlConsole.vue"),
      meta: { title: "SQL查询台" }
    },
    {
      path: "data-dictionary",
      name: "DataDictionary",
      component: () => import("@/views/DataDictionary.vue"),
      meta: { title: "数据字典" }
    }
  ]
};
