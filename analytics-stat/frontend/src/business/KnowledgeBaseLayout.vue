<template>
  <el-container direction="vertical" class="knowledge-layout">
    <!-- 顶部导航栏 -->
    <knowledge-base-header-menus />
    <!-- 内容区域 -->
    <el-main class="main-container">
      <router-view v-slot="{ Component }">
        <keep-alive>
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import KnowledgeBaseHeaderMenus from './head/KnowledgeBaseHeaderMenus.vue'
</script>

<style scoped>
.knowledge-layout {
  height: 100vh;
}

.main-container {
  height: calc(100vh - 50px);
  overflow-y: auto;
  padding: 0;
  background-color: #f5f6f7;
}
</style>
