<template>
  <div id="app">
    <!-- 主应用容器 -->
    <router-view/>
    <!-- micro-app 子应用容器 -->
    <micro-app
      v-if="currentApp"
      :name="currentApp.name"
      :url="currentApp.entry"
      :data="appData"
      :destroy="false"
      :fiber="true"
      :iframe="currentApp.isViteApp || false"
      :inline="true"
      :disable-memory-router="true"
      :disable-scopecss="true"
      @datachange="handleDataChange"
      @error="handleError"
    />
  </div>
</template>
<script>


import {getQueryVariable, getUrlParameterWidthRegExp} from "@/utils";
import axios from "axios";
import {useUserStore} from "@/store";
import {getCurrentUserId} from "@/utils/token";
import {hasPermissions} from "@/utils/permission";
// 引入模块配置表，用于判断当前模块的加载方式和技术栈
import {MIGRATED_MODULES, isMigrated, isViteApp} from "@/micro-app-config";

export default {
  name: "AppLayout",
  data() {
    return {
    };
  },
  computed: {
    /**
     * 从当前 hash 路由中提取模块名称
     * MeterSphere 使用 hash 路由，格式为 #/{moduleName}/...
     * 例如 #/api-test/definition → 模块名为 'api-test'
     */
    currentModuleName() {
      // 优先从 $route.path 获取（Vue Router 已解析 hash）
      const path = this.$route.path || '';
      // 路径格式: /moduleName/... ，提取第一段作为模块名
      const match = path.match(/^\/([^/]+)/);
      return match ? match[1] : '';
    },
    /**
     * 当前激活的子应用配置
     * 根据模块名从配置表中获取迁移状态和技术栈信息
     * 返回 null 表示当前路由不对应任何子应用（如主应用自身页面）
     */
    currentApp() {
      const name = this.currentModuleName;
      if (!name || !MIGRATED_MODULES[name]) {
        return null;
      }
      const config = MIGRATED_MODULES[name];
      return {
        name: name,
        entry: this.getEntryUrl(name),
        migrated: config.migrated,
        isViteApp: config.isViteApp,
      };
    },
    /**
     * 传递给子应用的数据
     * 通过 <micro-app :data="appData"> 传入，子应用在 window.mount(data) 中接收
     */
    appData() {
      return {
        // 当前路由的完整路径，子应用可据此恢复路由状态
        defaultPath: this.$route.path,
      };
    },
  },
  methods: {
    /**
     * 计算子应用入口 URL
     * 开发环境：//127.0.0.1:{port-4000}（与 micro-app.js 中的逻辑一致）
     * 生产环境：{origin}/{serviceId}
     *
     * @param {string} name - 模块名称（serviceId）
     * @returns {string} 子应用入口 URL
     */
    getEntryUrl(name) {
      const microPorts = JSON.parse(sessionStorage.getItem('micro_ports') || '{}');
      if (process.env.NODE_ENV === 'development') {
        return '//127.0.0.1:' + (microPorts[name] - 4000);
      }
      return window.location.origin + '/' + name;
    },
    /**
     * 处理子应用通过 dispatch 发送的数据
     * e.detail.data 包含子应用发送的数据对象
     */
    handleDataChange(e) {
      console.log('[App.vue] 收到子应用数据:', e.detail.data);
    },
    /**
     * 处理子应用加载错误
     * 记录错误日志，便于排查问题
     */
    handleError(e) {
      console.error('[App.vue] 子应用加载出错:', e.detail.name, e.detail.error);
    },
  },
  beforeMount() {
    const router = this.$router
    const code = getQueryVariable('code');
    const state = getQueryVariable('state') || '';
    if (state.split('#')[0] === 'fit2cloud-lark-qr' && state.split('#')[1] === "/" ) {
      this.loading = true;
      try {
        axios.get("/sso/callback/lark?code="+code).then((response) => {
          console.log(response)
          const weComCallback = response.data.data;
          const userStore = useUserStore()
          // 删除缓存
          userStore.checkPermission(response.data);
          sessionStorage.removeItem('changePassword');
          localStorage.setItem('default_language', weComCallback.language);
          sessionStorage.setItem('loginSuccess', 'true');
          sessionStorage.setItem('changePassword', false);
          localStorage.setItem('AuthenticateType', 'QRCODE');
          if (sessionStorage.getItem('lastUser') === getCurrentUserId()) {
            router.push({path: sessionStorage.getItem('redirectUrl') || '/'});
            return;
          }
          let redirectUrl = '/';
          if (hasPermissions('PROJECT_USER:READ', 'PROJECT_ENVIRONMENT:READ', 'PROJECT_OPERATING_LOG:READ', 'PROJECT_FILE:READ+JAR', 'PROJECT_FILE:READ+FILE', 'PROJECT_CUSTOM_CODE:READ', 'PROJECT_MESSAGE:READ', 'PROJECT_TEMPLATE:READ')) {
            redirectUrl = '/project/home';
          } else if (hasPermissions('WORKSPACE_SERVICE:READ', 'WORKSPACE_USER:READ', 'WORKSPACE_PROJECT_MANAGER:READ', 'WORKSPACE_PROJECT_ENVIRONMENT:READ', 'WORKSPACE_OPERATING_LOG:READ')) {
            redirectUrl = '/setting/project/:type';
          } else if (hasPermissions('SYSTEM_USER:READ', 'SYSTEM_WORKSPACE:READ', 'SYSTEM_GROUP:READ', 'SYSTEM_TEST_POOL:READ', 'SYSTEM_SETTING:READ', 'SYSTEM_AUTH:READ', 'SYSTEM_QUOTA:READ', 'SYSTEM_OPERATING_LOG:READ')) {
            redirectUrl = '/setting';
          } else {
            redirectUrl = '/';
          }
          sessionStorage.setItem('redirectUrl', redirectUrl);
          sessionStorage.setItem('lastUser', getCurrentUserId());
          this.loading = false;
          router.push({name: "login_redirect", path: redirectUrl || '/', query: {}});
          localStorage.setItem('loginType', 'LARK');
        }).catch((err)=>{
          this.$message.error(err.response.data.message);
        })
      } catch (err) {
        // eslint-disable-next-line no-console
        console.log(err);
      }
    }
    if (state.split('#')[0] === 'fit2cloud-lark-suite-qr' && state.split('#')[1] === "/") {
      this.loading = true;
      try {
        axios.get("/sso/callback/lark_suite?code="+code).then((response) => {
          const weComCallback = response.data.data;
          const userStore = useUserStore()
          // 删除缓存
          userStore.checkPermission(response.data);
          sessionStorage.removeItem('changePassword');
          localStorage.setItem('default_language', weComCallback.language);
          sessionStorage.setItem('loginSuccess', 'true');
          sessionStorage.setItem('changePassword', false);
          localStorage.setItem('AuthenticateType', 'QRCODE');
          if (sessionStorage.getItem('lastUser') === getCurrentUserId()) {
            router.push({path: sessionStorage.getItem('redirectUrl') || '/'});
            return;
          }
          let redirectUrl = '/';
          if (hasPermissions('PROJECT_USER:READ', 'PROJECT_ENVIRONMENT:READ', 'PROJECT_OPERATING_LOG:READ', 'PROJECT_FILE:READ+JAR', 'PROJECT_FILE:READ+FILE', 'PROJECT_CUSTOM_CODE:READ', 'PROJECT_MESSAGE:READ', 'PROJECT_TEMPLATE:READ')) {
            redirectUrl = '/project/home';
          } else if (hasPermissions('WORKSPACE_SERVICE:READ', 'WORKSPACE_USER:READ', 'WORKSPACE_PROJECT_MANAGER:READ', 'WORKSPACE_PROJECT_ENVIRONMENT:READ', 'WORKSPACE_OPERATING_LOG:READ')) {
            redirectUrl = '/setting/project/:type';
          } else if (hasPermissions('SYSTEM_USER:READ', 'SYSTEM_WORKSPACE:READ', 'SYSTEM_GROUP:READ', 'SYSTEM_TEST_POOL:READ', 'SYSTEM_SETTING:READ', 'SYSTEM_AUTH:READ', 'SYSTEM_QUOTA:READ', 'SYSTEM_OPERATING_LOG:READ')) {
            redirectUrl = '/setting';
          } else {
            redirectUrl = '/';
          }
          sessionStorage.setItem('redirectUrl', redirectUrl);
          sessionStorage.setItem('lastUser', getCurrentUserId());
          this.loading = false;
          router.push({name: "login_redirect", path: redirectUrl || '/', query: {}});
          localStorage.setItem('loginType', 'LARK_SUITE');
        }).catch((err)=>{
          this.$message.error(err.response.data.message);
        })
      } catch (err) {
        // eslint-disable-next-line no-console
        console.log(err);
      }
    }

    if (getQueryVariable('code') && getQueryVariable('state')) {
      const currentUrl = window.location.href;
      const url = new URL(currentUrl);
      getUrlParameterWidthRegExp('code');
      getUrlParameterWidthRegExp('state');
      url.searchParams.delete('code');
      url.searchParams.delete('state');
      const newUrl = url.toString();
      // 或者在不刷新页面的情况下更新URL（比如使用 History API）
      window.history.replaceState({}, document.title, newUrl);
    }
  }
}

</script>
