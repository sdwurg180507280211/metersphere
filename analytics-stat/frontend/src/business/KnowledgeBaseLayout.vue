<template>
  <el-container direction="vertical" class="knowledge-layout">
    <!-- 顶部导航栏 -->
    <knowledge-base-header-menus />
    <!-- 左右布局 -->
    <el-container>
      <!-- 左侧菜单 -->
      <el-aside width="200px" class="aside-container">
        <knowledge-base-menu />
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
import KnowledgeBaseHeaderMenus from './head/KnowledgeBaseHeaderMenus.vue'
import KnowledgeBaseMenu from './KnowledgeBaseMenu.vue'
</script>

<style scoped>
.knowledge-layout {
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
