<template>
  <n-layout class="knowledge-layout" has-sider>
    <n-layout-header class="layout-header">
      <knowledge-base-header-menus />
    </n-layout-header>
    <n-layout has-sider class="layout-body">
      <n-layout-sider bordered collapse-mode="width" :collapsed-width="64" :width="220" class="aside-container">
        <knowledge-base-menu />
      </n-layout-sider>
      <n-layout-content class="main-container">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { NLayout, NLayoutHeader, NLayoutSider, NLayoutContent } from 'naive-ui'
import KnowledgeBaseHeaderMenus from './head/KnowledgeBaseHeaderMenus.vue'
import KnowledgeBaseMenu from './KnowledgeBaseMenu.vue'
</script>

<style scoped>
.knowledge-layout {
  height: 100vh;
}

.layout-header {
  height: 50px;
}

.layout-body {
  height: calc(100vh - 50px);
}

.aside-container {
  height: 100%;
  overflow-y: auto;
  padding: 0;
  background-color: #fff;
}

.main-container {
  height: 100%;
  overflow-y: auto;
  padding: 0;
  background-color: #f5f6f7;
}
</style>
