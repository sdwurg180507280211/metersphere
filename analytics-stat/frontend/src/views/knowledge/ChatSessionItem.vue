<template>
  <div class="session-item" :class="{ active }" @click="emit('select')">
    <span class="session-title">{{ session.title }}</span>
    <n-tag v-if="negativeCount > 0" type="error" size="small" class="neg-badge">{{ negativeCount }}</n-tag>
    <div class="session-actions">
      <n-button text size="tiny" @click.stop="emit('rename')">{{ t('analytics.knowledge.rename_session') }}</n-button>
      <n-button text size="tiny" type="error" @click.stop="emit('delete')">{{ t('commons.delete') }}</n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NTag, NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
</script>

<style scoped>
.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: var(--chat-border-radius, 3px);
  cursor: pointer;
  transition: background-color 0.3s, color 0.3s;
}

.session-item:hover {
  background: var(--chat-session-active-bg, #f0f9eb);
}

.session-item.active {
  background: var(--chat-session-active-bg, #f0f9eb);
  color: var(--chat-accent, #18a058);
}

.session-title {
  flex: 1;
  font-size: 14px;
  color: var(--chat-text-primary, #333639);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-actions {
  display: none;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.session-item:hover .session-actions {
  display: flex;
}
</style>
