<template>
  <div
    class="session-item"
    :class="{ active }"
    @click="emit('select')"
  >
    <span class="session-title">{{ session.title }}</span>
  </div>
</template>

<script setup lang="ts">
import type { ChatSession } from '@/composables/useChatSessionStore'

defineProps<{
  session: ChatSession
  active: boolean
  negativeCount: number
}>()

const emit = defineEmits<{
  select: []
  delete: []
  rename: []
}>()
</script>

<style scoped>
.session-item {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  cursor: pointer;
  transition: background-color 0.15s;
  border-right: 4px solid transparent;
}

.session-item:hover {
  background: #eef2ff;
}

.session-item.active {
  background: #eef2ff;
  border-right-color: #4f46e5;
}

.session-title {
  flex: 1;
  font-size: 16px;
  font-weight: 500;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 22px;
  letter-spacing: -0.112px;
}
</style>
