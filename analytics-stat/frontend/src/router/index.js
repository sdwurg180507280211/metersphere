/**
 * Vue Router 配置
 * 
 * 技术栈：Vue Router 3.x（适配 Vue 2）
 * 
 * 路由结构：
 * - /analytics-stat/dashboard - 数据概览
 * - /analytics-stat/sql-console - SQL 查询台
 * - /analytics-stat/data-dictionary - 数据字典
 */
import Vue from 'vue';
import Router from 'vue-router';

// 修复路由变更后报错的问题（参考其他模块）
const routerPush = Router.prototype.push;
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch(error => error);
};

// 注册 Vue Router
Vue.use(Router);

// 布局组件
import AnalyticsStat from '@/business/AnalyticsStat.vue';

// 路由配置
export const constantRoutes = [
  {
    path: '/',
    redirect: '/analytics-stat/dashboard'
  },
  {
    path: '/analytics-stat',
    name: 'analytics-stat',
    redirect: '/analytics-stat/dashboard',
    component: AnalyticsStat,
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
];

// 创建路由实例的工厂函数
const createRouter = () => new Router({
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
});

// 重置路由（用于热更新）
export function resetRouter() {
  const newRouter = createRouter();
  router.matcher = newRouter.matcher;
}

const router = createRouter();

export default router;
