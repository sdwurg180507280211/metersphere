import Vue from "vue"
import Router from "vue-router"
import Workstation from "@/router/modules/workstation";
import {SUPER_GROUP} from "metersphere-frontend/src/utils/constants";
import {isLogin} from "metersphere-frontend/src/api/user";

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

async function loadCurrentUser() {
  try {
    const response = await isLogin();
    return response.data || {};
  } catch (e) {
    // 登录态由共享权限守卫兜底处理，这里只避免直达页面时拿到过期用户信息。
    return {};
  }
}

function canAccessSqlQuery(user) {
  const groups = user.groups || [];
  const userGroups = user.userGroups || [];
  return groups.some(group => group.id === SUPER_GROUP)
    || userGroups.some(userGroup => userGroup.groupId === SUPER_GROUP);
}

router.beforeEach(async (to, from, next) => {
  if (to.matched.some(record => record.meta && record.meta.superOnly)) {
    const user = await loadCurrentUser();
    if (!canAccessSqlQuery(user)) {
      next(user.id ? '/workstation/dashboard' : '/login');
      return;
    }
  }

  next();
});

export default router
