import Vue from "vue"
import Router from "vue-router"
import Workstation from "@/router/modules/workstation";
import {useUserStore} from "@/store";
import {SUPER_GROUP} from "metersphere-frontend/src/utils/constants";

const SQL_QUERY_ALLOWED_ACCOUNT = 'kjls_zhaozhiwei001';

// 修复路由变更后报错的问题
const routerPush = Router.prototype.push;
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch(error => error)
}

Vue.use(Router)

//  顶部菜单
Workstation.children.forEach(item => {
  item.children = [{path: '', component: item.component}];
  item.component = () => import('@/business/Workstation')
})

export const constantRoutes = [
  {path: "/", redirect: "/workstation/dashboard"},
  {
    path: "/login",
    component: () => import("metersphere-frontend/src/business/login"),
    hidden: true
  },
  Workstation
]

const createRouter = () => new Router({
  scrollBehavior: () => ({y: 0}),
  routes: constantRoutes
})

export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher // reset router
}

const router = createRouter()

function canAccessSqlQuery() {
  const user = useUserStore().currentUser || {};

  if (!user.id) {
    return true;
  }

  const groups = user.groups || [];
  const userGroups = user.userGroups || [];
  const isAllowedAccount = user.id === SQL_QUERY_ALLOWED_ACCOUNT;
  const isSuperUser = groups.some(group => group.id === SUPER_GROUP)
    || userGroups.some(userGroup => userGroup.groupId === SUPER_GROUP);

  return isAllowedAccount && isSuperUser;
}

router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta && record.meta.superOnly) && !canAccessSqlQuery()) {
    next('/workstation/dashboard');
    return;
  }

  next();
});

export default router
