import Layout from 'metersphere-frontend/src/business/app-layout';
import {isMicroAppEnv} from 'metersphere-frontend/src/utils/micro-app-env';

// 微前端环境下用透传组件替换 Layout，避免子应用渲染重复的侧边栏
const PassThrough = {render: h => h('router-view')};

export default {
  path: '/api',
  name: 'api',
  redirect: '/api/home',
  component: isMicroAppEnv() ? PassThrough : Layout,
  children: [
    {
      path: 'home',
      name: 'fucHome',
      component: () => import('@/business/home/ApiHome'),
    },
    {
      path: 'automation/report',
      name: 'ApiReportList',
      component: () => import('@/business/automation/report/ApiReportList'),
    },
    {
      path: 'automation/report/view/:reportId',
      name: 'ApiScenarioReportView',
      component: () => import('@/business/automation/report/ApiReportView'),
    },
    {
      path: 'automation/report/:versionId?/report/:redirectID?/:dataType?/:dataSelectRange',
      name: 'ApiReportListWithQuery',
      component: () => import('@/business/automation/report/ApiReportList'),
    },
    {
      path: 'definition/report/view/:reportId',
      name: 'ApiReportView',
      component: () => import('@/business/definition/components/response/ApiResponseView'),
    },
    {
      path: 'definition',
      name: 'ApiDefinition',
      component: () => import('@/business/definition/ApiDefinition'),
    },
    {
      path: 'definition/:versionId?/:redirectID?/:dataType?/:dataSelectRange?/:projectId?/:type?/:workspaceId?',
      name: 'ApiDefinitionWithQuery',
      component: () => import('@/business/definition/ApiDefinition'),
    },
    {
      path: 'automation/:versionId?/:redirectID?/:dataType?/:dataSelectRange?/:projectId?/:workspaceId?',
      name: 'ApiAutomationWithQuery',
      component: () => import('@/business/automation/ApiAutomation'),
    },
    {
      path: 'automation',
      name: 'ApiAutomation',
      component: () => import('@/business/automation/ApiAutomation'),
    },
    {
      path: 'definition/edit/:definitionId',
      name: 'editCompleteContainer',
      component: () => import('@/business/definition/ApiDefinition'),
    },
  ],
};
