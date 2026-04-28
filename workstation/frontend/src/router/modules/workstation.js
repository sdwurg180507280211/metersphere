import Layout from "metersphere-frontend/src/business/app-layout";
import {isMicroAppEnv} from "metersphere-frontend/src/utils/micro-app-env";
import Upcoming from '@/business/upcoming/Upcoming'
import Focus from '@/business/focus/Focus'
import Creation from '@/business/creation/Creation'
import Dashboard from '@/business/dashboard/Dashboard'
import AdvancedSearch from '@/business/advanced-search/AdvancedSearch'

// 微前端环境下用透传组件替换 Layout，避免子应用渲染重复的侧边栏
const PassThrough = {render: h => h('router-view')};

export default {
  path: "/workstation",
  name: "workstation",
  redirect: "/workstation/dashboard",
  component: isMicroAppEnv() ? PassThrough : Layout,
  children: [
    {
      path: 'dashboard',
      name: 'workstationDashboard',
      component: Dashboard,
    },
    {
      path: 'upcoming',
      name: 'workstationUpcoming',
      component: Upcoming,
    },
    {
      path: 'focus',
      name: 'workstationFocus',
      component: Focus,
    },
    {
      path: 'creation',
      name: 'workstationCreation',
      component: Creation,
    },
    {
      path: 'advanced-search',
      name: 'workstationAdvancedSearch',
      component: AdvancedSearch,
    },
  ]
};

