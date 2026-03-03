<template>
  <div
    class="session-item"
    :class="{ active }"
    @click="emit('select')"
  >
    <svg class="session-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
    </svg>
    <span class="session-title">{{ session.title }}</span>
    <el-tag v-if="negativeCount > 0" type="danger" size="small" class="neg-badge">
      {{ negativeCount }}
    </el-tag>
    <div class="session-actions">
      <button class="action-btn" @click.stop="emit('rename')" :title="t('analytics.knowledge.rename_session')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
        </svg>
      </button>
      <button class="action-btn" @click.stop="emit('delete')" :title="t('commons.delete')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
          <polyline points="3 6 5 6 21 6" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
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
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
  position: relative;
}

.session-item:hover {
  background: var(--chat-session-active-bg, #ececf1);
}

.session-item.active {
  background: var(--chat-session-active-bg, #ececf1);
}

.session-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: #8e8ea0;
}

.session-title {
  flex: 1;
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.neg-badge {
  flex-shrink: 0;
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

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  border-radius: 4px;
  cursor: pointer;
  color: #8e8ea0;
  padding: 0;
}

.action-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #303133;
}
</style>
