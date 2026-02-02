<template>
  <el-col>
    <analytics-stat-header-menus />
    <div>
      <transition>
        <keep-alive>
          <router-view :baseUrl="baseUrl" />
        </keep-alive>
      </transition>
    </div>
  </el-col>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AnalyticsStatHeaderMenus from './head/AnalyticsStatHeaderMenus.vue'

// 基础路径，用于子路由
const baseUrl = ref('analytics-stat')
</script>

<style scoped>
/* 业务容器样式 */
</style>
