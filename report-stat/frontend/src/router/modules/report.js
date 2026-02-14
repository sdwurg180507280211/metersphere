import Layout from "metersphere-frontend/src/business/app-layout";
import {isMicroAppEnv} from "metersphere-frontend/src/utils/micro-app-env";

// 微前端环境下用透传组件替换 Layout，避免子应用渲染重复的侧边栏
const PassThrough = {render: h => h('router-view')};

export default {
  path: "/report",
  name: "report",
  redirect: "/report/project-statistics",
  component: isMicroAppEnv() ? PassThrough : Layout,
  children: [
    {
      path: '/report/project-statistics',
      name: 'projectStatistics',
      component: () => import('@/business/projectstatistics/ProjectStatistics'),
    },
    {
      path: "/report/project-report",
      name: "projectReport",
      component: () => import('@/business/enterprisereport/ProjectReport'),
    },
  ]
};

