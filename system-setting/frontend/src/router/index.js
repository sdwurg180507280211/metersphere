import Vue from "vue"
import Router from "vue-router"
import {installSafeRouterPush} from "metersphere-frontend/src/router/install-safe-push"
import Setting from "@/router/modules/setting";


// 修复路由变更后报错的问题
installSafeRouterPush(Router)

Vue.use(Router)

// setting 左右菜单渲染问题
Setting.children.forEach(item => {
  item.children = [{path: '', component: item.component}];
  item.component = () => import('@/business/Setting')
})

export const constantRoutes = [
  {path: "/", redirect: "/setting/dashboard"},
  {
    path: "/login",
    component: () => import("metersphere-frontend/src/business/login"),
    hidden: true
  },
  Setting
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

export default router
