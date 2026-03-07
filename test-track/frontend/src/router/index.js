import Vue from "vue"
import Router from "vue-router"
import {installSafeRouterPush} from "metersphere-frontend/src/router/install-safe-push"
import Track from "@/router/modules/track";


// 修复路由变更后报错的问题
installSafeRouterPush(Router)

Vue.use(Router)

//  顶部菜单
Track.children.forEach(item => {
  item.children = [{path: '', component: item.component}];
  item.component = () => import('@/business/TestTrack')
})

export const constantRoutes = [
  {path: "/", redirect: "/track/home"},
  {
    path: "/login",
    component: () => import("metersphere-frontend/src/business/login"),
    hidden: true
  },
  Track
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
