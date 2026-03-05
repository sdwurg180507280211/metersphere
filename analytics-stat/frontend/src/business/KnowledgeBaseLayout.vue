<template>
  <div class="knowledge-layout">
    <router-view v-slot="{ Component }">
      <keep-alive>
        <component :is="Component" />
      </keep-alive>
    </router-view>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.knowledge-layout {
  width: 100%;
  height: 100%;
  overflow: auto;
}
</style>
