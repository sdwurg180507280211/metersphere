<template>
  <el-col>
    <!-- 顶部导航栏：项目切换 + 二级菜单 + 右上角按钮组 -->
    <analytics-stat-header-menus />
    <!-- 左右布局：左侧菜单 + 右侧内容 -->
    <ms-container>
      <!-- 左侧菜单 -->
      <ms-aside-container :width="'200px'">
        <analytics-stat-menu />
      </ms-aside-container>
      <!-- 右侧内容区域 -->
      <ms-main-container>
        <keep-alive>
          <router-view />
        </keep-alive>
      </ms-main-container>
    </ms-container>
  </el-col>
</template>

<script>
/**
 * 分析统计业务容器组件
 * 
 * 布局结构（从上到下）：
 * 1. 顶部导航栏（AnalyticsStatHeaderMenus）
 *    - 项目切换
 *    - 二级导航菜单
 *    - 右上角按钮组（用户、语言、通知、任务中心、帮助）
 * 2. 左右布局
 *    - 左侧菜单（AnalyticsStatMenu）
 *    - 右侧内容（router-view）
 * 
 * 参考：
 * - report-stat/frontend/src/business/ReportStatistics.vue
 * - system-setting/frontend/src/business/Setting.vue
 */
import AnalyticsStatHeaderMenus from './head/AnalyticsStatHeaderMenus';
import AnalyticsStatMenu from './AnalyticsStatMenu';
import MsAsideContainer from "metersphere-frontend/src/components/MsAsideContainer";
import MsContainer from "metersphere-frontend/src/components/MsContainer";
import MsMainContainer from "metersphere-frontend/src/components/MsMainContainer";

export default {
  name: 'AnalyticsStat',
  components: {
    AnalyticsStatHeaderMenus,
    AnalyticsStatMenu,
    MsAsideContainer,
    MsContainer,
    MsMainContainer
  }
};
</script>

<style scoped>
.ms-aside-container {
  height: calc(100vh - 50px) !important;
  padding: 0px;
}

.ms-main-container {
  height: calc(100vh - 50px) !important;
}
</style>
