<template>
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
</template>

<script>
/**
 * 分析统计业务容器组件（左右布局）
 * 
 * 作用：
 * 1. 左侧显示功能菜单
 * 2. 右侧承载子路由内容
 * 3. 使用 keep-alive 缓存页面状态
 * 
 * 布局结构：
 * - ms-container: 外层容器
 *   - ms-aside-container: 左侧菜单容器（宽度 200px）
 *     - analytics-stat-menu: 菜单组件
 *   - ms-main-container: 右侧内容容器
 *     - router-view: 路由视图
 * 
 * 参考：system-setting/frontend/src/business/Setting.vue
 */
import AnalyticsStatMenu from './AnalyticsStatMenu';
import MsAsideContainer from "metersphere-frontend/src/components/MsAsideContainer";
import MsContainer from "metersphere-frontend/src/components/MsContainer";
import MsMainContainer from "metersphere-frontend/src/components/MsMainContainer";

export default {
  name: 'AnalyticsStat',
  components: {
    AnalyticsStatMenu,
    MsAsideContainer,
    MsContainer,
    MsMainContainer
  }
};
</script>

<style scoped>
.ms-aside-container {
  height: calc(100vh) !important;
  padding: 0px;
}

.ms-main-container {
  height: calc(100vh) !important;
}
</style>
