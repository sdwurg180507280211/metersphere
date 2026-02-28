<template>
  <el-menu
    :default-active="route.path"
    :default-openeds="['knowledge-group']"
    router
    class="knowledge-menu"
  >
    <el-sub-menu index="knowledge-group">
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
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { DataAnalysis, Search, ChatDotRound } from '@element-plus/icons-vue'
import { markRaw } from 'vue'
import type { Component } from 'vue'
import { KNOWLEDGE_ROUTE_PATHS } from '@/config/knowledge-route'

const route = useRoute()
const { t } = useI18n()

interface MenuItem {
  index: string
  i18nKey: string
  icon: Component
}

const menus: MenuItem[] = [
  {
    index: KNOWLEDGE_ROUTE_PATHS.knowledge,
    i18nKey: 'analytics.menu.knowledge',
    icon: markRaw(Search),
  },
  {
    index: KNOWLEDGE_ROUTE_PATHS.knowledgeChat,
    i18nKey: 'analytics.menu.knowledge_chat',
    icon: markRaw(ChatDotRound),
  },
]
</script>

<style scoped>
.knowledge-menu {
  border-right: 0;
}

.menu-item {
  height: 40px;
  line-height: 40px;
}
</style>
