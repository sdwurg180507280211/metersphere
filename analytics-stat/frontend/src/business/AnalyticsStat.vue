<template>
  <!-- 分析统计二级布局：顶部导航 + 左侧菜单 + 右侧内容 -->
  <el-container direction="vertical" class="analytics-stat-layout">
    <!-- 顶部导航栏 -->
    <analytics-stat-header-menus />
    <!-- 左右布局 -->
    <el-container>
      <!-- 左侧菜单 -->
      <el-aside width="200px" class="aside-container">
        <analytics-stat-menu />
      </el-aside>
      <!-- 右侧内容区域 -->
      <el-main class="main-container">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
/**
 * 分析统计业务容器组件
 *
 * 布局结构（从上到下）：
 * 1. 顶部导航栏（AnalyticsStatHeaderMenus）
 *    - 二级导航菜单
 *    - 右上角按钮组（用户头像、语言切换等）
 * 2. 左右布局
 *    - 左侧菜单（AnalyticsStatMenu）- 200px 固定宽度
 *    - 右侧内容（router-view）- 使用 keep-alive 缓存页面状态
 *
 * 与 Vue 2 版本的差异：
 * - 使用 Element Plus 的 el-container / el-aside / el-main 替代 SDK 的 MsContainer
 * - 使用 Vue 3 的 <router-view v-slot> 语法实现 keep-alive
 * - 使用 <script setup> 替代 Options API
 */
import AnalyticsStatHeaderMenus from './head/AnalyticsStatHeaderMenus.vue'
import AnalyticsStatMenu from './AnalyticsStatMenu.vue'
</script>

<style scoped>
.analytics-stat-layout {
  height: 100vh;
}

.aside-container {
  height: calc(100vh - 50px);
  overflow-y: auto;
  border-right: 1px solid #e6e6e6;
  padding: 0;
  background-color: #fff;
}

.main-container {
  height: calc(100vh - 50px);
  overflow-y: auto;
  padding: 0;
  background-color: #f5f6f7;
}
</style>
