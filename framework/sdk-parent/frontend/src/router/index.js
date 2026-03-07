import Vue from "vue"
import Router from "vue-router"
import {installSafeRouterPush} from "./install-safe-push"
import Layout from "../business/app-layout"
import {hasPermissions} from "../utils/permission";
import {SECOND_LEVEL_ROUTE_PERMISSION_MAP} from "../utils/constants";
import {MIGRATED_MODULES} from "../micro-app-config";

// 加载modules中的路由
const modules = require.context("./modules", true, /\.js$/)

// 修复路由变更后报错的问题
installSafeRouterPush(Router)

Vue.use(Router)

// 为所有 micro-app 子应用模块生成路由占位
// Layout 组件确保侧边栏和顶部栏正常显示
// 子应用实际内容由 App.vue 中的 <micro-app> 标签渲染
const microAppRoutes = Object.keys(MIGRATED_MODULES).map(name => ({
  path: `/${name}`,
  component: Layout,
  name: name,
  children: [{
    // 匹配所有子路径，如 /track/plan、/api/definition/edit/xxx
    path: '*',
    component: {render: h => h('div')}
  }]
}));

export const constantRoutes = [
  {
    path: "/redirect",
    component: Layout,
    hidden: true,
    children: [
      {
        path: "/redirect/:path(.*)",
        component: () => import("../components/redirect")
      }
    ]
  },
  {
    path: "/login",
    component: () => import("../business/login"),
    hidden: true
  },
  {
    path: "/",
    component: Layout,
    redirect: "/setting/personsetting",
  },
  ...microAppRoutes,
]

/**
 * 用户登录后根据角色加载的路由
 */
export const rolesRoutes = [
  // 先按sort排序
  ...modules.keys().map(key => modules(key).default).sort((r1, r2) => {
    if (!r1.sort) r1.sort = Number.MAX_VALUE
    if (!r2.sort) r2.sort = Number.MAX_VALUE
    return r1.sort - r2.sort
  }),
  {path: "*", redirect: "/", hidden: true}
]

const createRouter = () => new Router({
  scrollBehavior: () => ({y: 0}),
  routes: constantRoutes
})

const router = createRouter()

const LOGIN_CHECK_TTL = 60 * 1000
let lastLoginCheckTime = 0
let loginCheckPromise = null

async function checkLoginWithCache(userStore) {
  const now = Date.now()
  if (now - lastLoginCheckTime < LOGIN_CHECK_TTL) {
    return
  }

  if (!loginCheckPromise) {
    loginCheckPromise = userStore.getIsLogin()
      .then(() => {
        lastLoginCheckTime = Date.now()
      })
      .finally(() => {
        loginCheckPromise = null
      })
  }

  await loginCheckPromise
}

// 刷新整个页面会到这里
import('@/store').then(async ({useUserStore}) => {
  try {
    const userStore = useUserStore();
    await checkLoginWithCache(userStore);
  } catch (e) {
    // nothing
  }
});

let store = null;
router.beforeEach(async (to, from, next) => {
  document.title = localStorage.getItem("default-document-title") || "MeterSphere";
  if (store === null) {
    const {useUserStore} = await import('@/store');
    store = useUserStore();
  }
  let formModule = from.path.split('/')[1];
  let toModule = to.path.split('/')[1];
  if (to.path !== '/login' && formModule && toModule !== formModule) {
    try {
      await checkLoginWithCache(store);
    } catch (e) {
      // nothing
    }
  }

  if (to.name === "login_redirect" || to.path === "/login") {
    next();
    return;
  }

  // 二级菜单权限控制
  let changedPath = getDefaultSecondLevelMenu(to.fullPath);
  sessionStorage.setItem('redirectUrl', changedPath);
  if (changedPath === to.fullPath) {
    // 有权限则放行
    next();
  } else {
    // 未通过校验，放行至有权限路由
    next({path: changedPath});
  }
});

export function getDefaultSecondLevelMenu(toPath) {
  let {TRACK: tracks, API: apis, LOAD: loads, UI: ui, REPORT: report} = SECOND_LEVEL_ROUTE_PERMISSION_MAP;
  if (tracks.map(r => r.router).indexOf(toPath) > -1) {
    return _getDefaultSecondLevelMenu(tracks, toPath);
  } else if (apis.map(r => r.router).indexOf(toPath) > -1) {
    return _getDefaultSecondLevelMenu(apis, toPath);
  } else if (loads.map(r => r.router).indexOf(toPath) > -1) {
    return _getDefaultSecondLevelMenu(loads, toPath);
  } else if (ui.map(r => r.router).indexOf(toPath) > -1) {
    return _getDefaultSecondLevelMenu(ui, toPath);
  } else if (report.map(r => r.router).indexOf(toPath) > -1) {
    return _getDefaultSecondLevelMenu(report, toPath);
  } else {
    return toPath;
  }
}

function _getDefaultSecondLevelMenu(secondLevelRouters, toPath) {
  let toRouter = secondLevelRouters.find(r => r['router'] === toPath);
  if (toRouter && hasPermissions(...toRouter['permission'])) {
    // 将要跳转的路由有权限则放行
    return toPath;
  }
  for (let router of secondLevelRouters) {
    if (hasPermissions(...router['permission'])) {
      // 返回第一个有权限的路由路径
      return router['router'];
    }
  }
  return '/';
}

export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher // reset router
}

export default router
