<template>
  <div id="menu-bar" v-if="isRouterAlive">
    <el-row type="flex">
      <!-- 项目切换组件 -->
      <project-switch :project-name="currentProject" />
      <!-- 二级导航菜单 -->
      <el-col :span="14">
        <el-menu
          class="header-menu"
          :unique-opened="true"
          mode="horizontal"
          router
          :default-active="pathName"
        >
          <el-menu-item
            v-for="menu in menus"
            :key="menu.path"
            :index="menu.path"
          >
            {{ $t(menu.i18nKey) }}
          </el-menu-item>
        </el-menu>
      </el-col>
      <!-- 右上角按钮组（用户头像、语言切换、工作空间、任务中心、通知、帮助） -->
      <el-col :span="10">
        <ms-header-right-menus />
      </el-col>
    </el-row>
  </div>
</template>

<script>
/**
 * 分析统计顶部导航菜单组件
 * 
 * 功能：
 * 1. 左侧：项目切换（ProjectSwitch）
 * 2. 中间：二级导航菜单（数据概览、SQL查询台、数据字典）
 * 3. 右侧：公共按钮组（MsHeaderRightMenus）
 *    - 用户头像
 *    - 语言切换
 *    - 工作空间切换
 *    - 任务中心
 *    - 通知
 *    - 帮助引导
 * 
 * 参考：report-stat/frontend/src/business/header/ReportStatisticsHeaderMenus.vue
 */
import ProjectSwitch from "metersphere-frontend/src/components/head/ProjectSwitch";
import MsHeaderRightMenus from "metersphere-frontend/src/components/layout/HeaderRightMenus";
import { PROJECT_NAME } from "metersphere-frontend/src/utils/constants";

export default {
  name: 'AnalyticsStatHeaderMenus',
  components: {
    ProjectSwitch,
    MsHeaderRightMenus
  },
  data() {
    return {
      // 路由是否存活（用于强制刷新）
      isRouterAlive: true,
      // 当前项目名称
      currentProject: sessionStorage.getItem(PROJECT_NAME),
      // 当前激活的路径
      pathName: '',
      // 二级导航菜单配置（使用 i18n key，支持多语言切换）
      menus: [
        {
          path: '/analytics/home',
          i18nKey: 'analytics.menu.home'
        },
        {
          path: '/analytics/sql-console',
          i18nKey: 'analytics.menu.sql_console'
        },
        {
          path: '/analytics/data-dictionary',
          i18nKey: 'analytics.menu.data_dictionary'
        }
      ]
    };
  },
  watch: {
    /**
     * 监听路由变化，更新激活的菜单项
     */
    '$route.path': {
      handler(newPath) {
        this.updateActivePath(newPath);
      },
      immediate: true
    }
  },
  methods: {
    /**
     * 根据当前路径设置激活的菜单项
     * @param {string} path - 当前路由路径
     */
    updateActivePath(path) {
      if (path.indexOf('/analytics/home') >= 0 || path === '/analytics') {
        this.pathName = '/analytics/home';
      } else if (path.indexOf('/analytics/sql-console') >= 0) {
        this.pathName = '/analytics/sql-console';
      } else if (path.indexOf('/analytics/data-dictionary') >= 0) {
        this.pathName = '/analytics/data-dictionary';
      } else {
        this.pathName = path;
      }
    },
    /**
     * 强制刷新路由
     */
    reload() {
      this.isRouterAlive = false;
      this.$nextTick(function () {
        this.isRouterAlive = true;
      });
    }
  }
};
</script>

<style scoped>
#menu-bar {
  border-bottom: 1px solid #e6e6e6;
  background-color: #fff;
}

.header-menu {
  border-bottom: none;
}

.el-menu-item {
  padding: 0 10px;
}
</style>
