<template>
  <!-- 左侧菜单：分析统计功能导航 -->
  <el-menu
    :default-active="route.path"
    :default-openeds="['analytics-group']"
    router
    class="analytics-menu"
  >
    <el-sub-menu index="analytics-group">
      <template #title>
        <el-icon><DataAnalysis /></el-icon>
        <span>{{ t('commons.analytics_stat') }}</span>
      </template>
      <el-menu-item
        v-for="menu in menus"
        :key="menu.index"
        :index="menu.index"
        class="menu-item"
      >
        <el-icon><component :is="menu.icon" /></el-icon>
        <span>{{ t(menu.i18nKey) }}</span>
      </el-menu-item>
    </el-sub-menu>
  </el-menu>
</template>

<script setup lang="ts">
/**
 * 分析统计左侧菜单组件
 *
 * 功能：
 * 1. 显示分析统计模块的功能菜单
 * 2. 支持路由跳转（通过 el-menu 的 router 属性）
 * 3. 自动高亮当前激活菜单（通过 default-active 绑定 route.path）
 *
 * 与 Vue 2 版本的差异：
 * - 使用 Element Plus 的 el-icon + 图标组件替代 Element UI 的 i 标签图标
 * - 使用 useRoute() 替代 this.$route
 * - 使用 useI18n() 替代 this.$t()
 */
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { DataAnalysis, HomeFilled, Document, Collection, Search } from '@element-plus/icons-vue'
import { markRaw } from 'vue'
import type { Component } from 'vue'

const route = useRoute()
const { t } = useI18n()

/** 菜单项类型定义 */
interface MenuItem {
  index: string
  i18nKey: string
  icon: Component
}

/** 菜单配置（使用 i18n key，支持多语言切换） */
const menus: MenuItem[] = [
  {
    index: '/analytics/home',
    i18nKey: 'analytics.menu.home',
    icon: markRaw(HomeFilled),
  },
  {
    index: '/analytics/sql-console',
    i18nKey: 'analytics.menu.sql_console',
    icon: markRaw(Document),
  },
  {
    index: '/analytics/data-dictionary',
    i18nKey: 'analytics.menu.data_dictionary',
    icon: markRaw(Collection),
  },
  {
    index: '/analytics/knowledge',
    i18nKey: 'analytics.menu.knowledge',
    icon: markRaw(Search),
  },
]
</script>

<style scoped>
.analytics-menu {
  border-right: 0;
}

.menu-item {
  height: 40px;
  line-height: 40px;
}
</style>
