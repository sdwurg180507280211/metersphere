<template>
  <div class="menu-bar">
    <n-menu mode="horizontal" :value="activePath" :options="menuOptions" @update:value="handleMenuChange" />
    <span class="module-title">{{ t('analytics.title') }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NMenu, type MenuOption } from 'naive-ui'
import { KNOWLEDGE_ROUTE_BASE, KNOWLEDGE_ROUTE_PATHS } from '@/config/knowledge-route'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const menuOptions = computed<MenuOption[]>(() => [
  { label: t('analytics.menu.knowledge'), key: KNOWLEDGE_ROUTE_PATHS.knowledge },
  { label: t('analytics.menu.knowledge_chat'), key: KNOWLEDGE_ROUTE_PATHS.knowledgeChat },
])

const activePath = computed(() => {
  const path = route.path
  const matched = menuOptions.value.find((item) => path.startsWith(String(item.key)))
  if (matched) return String(matched.key)
  if (path === KNOWLEDGE_ROUTE_BASE) return KNOWLEDGE_ROUTE_PATHS.knowledge
  return path
})

const handleMenuChange = (key: string) => {
  router.push(key)
}
</script>

<style scoped>
.menu-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e6e6;
  background-color: #fff;
  height: 50px;
  padding: 0 20px 0 12px;
}

.menu-bar :deep(.n-menu) {
  flex: 1;
}

.module-title {
  font-size: 14px;
  color: #606266;
  margin-left: 20px;
  white-space: nowrap;
}
</style>
