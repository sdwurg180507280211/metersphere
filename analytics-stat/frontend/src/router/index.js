/**
 * Vue Router 配置
 * 
 * 技术栈：Vue Router 3.x（适配 Vue 2）
 * 
 * 路由结构：
 * - /login - 登录页（使用 metersphere-frontend 统一登录组件）
 * - /analytics-stat/dashboard - 数据概览
 * - /analytics-stat/sql-console - SQL 查询台
 * - /analytics-stat/data-dictionary - 数据字典
 * 
 * 布局说明：
 * - 使用 metersphere-frontend 的 Layout 组件作为统一布局
 * - Layout 提供顶部导航栏、侧边菜单、面包屑等 UI
 */
import Vue from "vue";
import Router from "vue-router";
import Analytics from "@/router/modules/analytics";

// 修复路由变更后报错的问题（参考其他模块）
// 原理：Vue Router 3.x 在导航到相同路由时会抛出 NavigationDuplicated 错误
// 这里通过重写 push 方法，捕获并忽略该错误
const routerPush = Router.prototype.push;
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch(error => error);
};

// 注册 Vue Router
Vue.use(Router);

/**
 * 二级菜单路由处理
 * 
 * 原理说明：
 * 1. Layout 组件是一级布局（顶部导航栏 + 侧边菜单）
 * 2. AnalyticsStat 组件是二级布局（二级导航菜单 + 内容区域）
 * 3. 这段代码将路由结构从 Layout -> 页面组件 改为 Layout -> AnalyticsStat -> 页面组件
 * 
 * 处理前：/analytics-stat/dashboard -> Dashboard.vue
 * 处理后：/analytics-stat/dashboard -> AnalyticsStat.vue -> Dashboard.vue
 * 
 * 这样可以在每个页面上方显示二级导航菜单
 */
Analytics.children.forEach(item => {
  // 将原来的组件放到更深一层的子路由中
  item.children = [{ path: "", component: item.component }];
  // 将当前路由的组件替换为二级布局组件
  item.component = () => import("@/business/AnalyticsStat.vue");
});

/**
 * 常量路由配置
 * 
 * 包含：
 * 1. 根路径重定向
 * 2. 登录页（使用 metersphere-frontend 统一登录组件）
 * 3. Analytics 模块路由（使用 Layout 布局）
 */
export const constantRoutes = [
  // 根路径重定向到数据概览
  { path: "/", redirect: "/analytics-stat/dashboard" },
  
  // 登录页 - 使用 metersphere-frontend 的统一登录组件
  // hidden: true 表示不在菜单中显示
  {
    path: "/login",
    component: () => import("metersphere-frontend/src/business/login"),
    hidden: true
  },
  
  // Analytics 模块路由（包含 Layout 布局）
  Analytics
];

/**
 * 创建路由实例的工厂函数
 * 使用工厂函数便于路由重置
 */
const createRouter = () => new Router({
  scrollBehavior: () => ({ y: 0 }),  // 路由切换时滚动到顶部
  routes: constantRoutes
});

/**
 * 重置路由
 * 用于热更新或用户登出时清理路由状态
 */
export function resetRouter() {
  const newRouter = createRouter();
  router.matcher = newRouter.matcher;
}

const router = createRouter();

export default router;
