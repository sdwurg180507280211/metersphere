<template>
  <el-col>
    <!-- 二级导航菜单 -->
    <analytics-stat-header-menus />
    <!-- 页面内容区域 -->
    <div class="content-container">
      <transition name="fade" mode="out-in">
        <keep-alive>
          <router-view :baseUrl="baseUrl" />
        </keep-alive>
      </transition>
    </div>
  </el-col>
</template>

<script>
/**
 * 分析统计业务容器组件
 * 
 * 作用：
 * 1. 包含二级导航菜单
 * 2. 承载子路由内容
 * 3. 使用 keep-alive 缓存页面状态
 */
import AnalyticsStatHeaderMenus from './head/AnalyticsStatHeaderMenus.vue';

export default {
  name: 'AnalyticsStat',
  components: {
    AnalyticsStatHeaderMenus
  },
  data() {
    return {
      // 基础路径，用于子路由跳转
      baseUrl: 'analytics-stat'
    };
  }
};
</script>

<style scoped>
.content-container {
  padding: 15px;
  background-color: #f5f5f5;
  min-height: calc(100vh - 92px);
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}
</style>
