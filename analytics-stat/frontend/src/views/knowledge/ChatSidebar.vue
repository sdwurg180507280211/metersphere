<template>
  <aside class="chat-sidebar">
    <div class="sidebar-brand">
      <span class="brand-text">MeterSphere AI</span>
      <n-button text size="tiny" @click="emit('toggle-sidebar')">{{ t('analytics.knowledge.chat_sidebar_toggle') }}</n-button>
    </div>

    <div class="sidebar-actions">
      <n-button secondary type="primary" block @click="emit('new-session')">{{ t('analytics.knowledge.new_chat') }}</n-button>
      <n-button quaternary @click="showSearch = !showSearch">{{ t('analytics.knowledge.search') }}</n-button>
    </div>

    <div v-if="showSearch" class="sidebar-search">
      <n-input
        v-model:value="localKeyword"
        clearable
        size="small"
        :placeholder="t('analytics.knowledge.chat_session_search')"
        @update:value="emit('update:session-keyword', $event)"
      />
    </div>

    <div class="conversations-header">
      <span>{{ t('analytics.knowledge.chat_your_conversations') }}</span>
      <n-button text type="error" size="tiny" @click="handleClearAll">{{ t('analytics.knowledge.chat_clear_all') }}</n-button>
    </div>

    <div class="sidebar-conversations">
      <template v-if="recentSessions.length">
        <div class="group-label">{{ t('analytics.knowledge.chat_last_7_days') }}</div>
        <ChatSessionItem
          v-for="session in recentSessions"
          :key="session.id"
          :session="session"
          :active="session.id === currentSessionId"
          :negative-count="getSessionNegativeCount(session.id)"
          @select="emit('select-session', session.id)"
          @delete="emit('delete-session', session.id)"
          @rename="emit('rename-session', session.id, session.title)"
        />
      </template>

      <template v-if="olderSessions.length">
        <div class="group-label" style="margin-top: 12px">{{ t('analytics.knowledge.chat_older') }}</div>
        <ChatSessionItem
          v-for="session in olderSessions"
          :key="session.id"
          :session="session"
          :active="session.id === currentSessionId"
          :negative-count="getSessionNegativeCount(session.id)"
          @select="emit('select-session', session.id)"
          @delete="emit('delete-session', session.id)"
          @rename="emit('rename-session', session.id, session.title)"
        />
      </template>

      <div v-if="sessions.length === 0" class="session-empty">{{ t('analytics.knowledge.chat_session_empty') }}</div>
    </div>

    <div class="sidebar-footer">
      <n-tag size="small" :type="llmEnabled ? 'success' : 'info'">
        {{ t('analytics.knowledge.llm_status_label') }}: {{ llmStatusText }}
      </n-tag>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NButton, NInput, NTag, useDialog } from 'naive-ui'
import ChatSessionItem from './ChatSessionItem.vue'
import type { ChatSession } from '@/composables/useChatSessionStore'

const props = defineProps<{
  sessions: ChatSession[]
  currentSessionId: string
  sessionKeyword: string
  llmEnabled: boolean
  llmStatusText: string
}>()

const emit = defineEmits<{
  'new-session': []
  'select-session': [id: string]
  'delete-session': [id: string]
  'rename-session': [id: string, title: string]
  'clear-all': []
  'update:session-keyword': [value: string]
  'toggle-sidebar': []
}>()

const { t } = useI18n()
const dialog = useDialog()
const showSearch = ref(false)
const localKeyword = ref(props.sessionKeyword)

const SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000

const recentSessions = computed(() => props.sessions.filter((s) => Date.now() - s.updatedAt < SEVEN_DAYS))
const olderSessions = computed(() => props.sessions.filter((s) => Date.now() - s.updatedAt >= SEVEN_DAYS))

const getSessionNegativeCount = (sessionId: string) => {
  const session = props.sessions.find((item) => item.id === sessionId)
  if (!session) return 0
  return session.messages.filter((msg) => msg.feedback?.rating === 'down').length
}

const handleClearAll = () => {
  dialog.warning({
    title: t('commons.prompt'),
    content: `${t('analytics.knowledge.chat_clear_all')}?`,
    positiveText: t('commons.confirm'),
    negativeText: t('commons.cancel'),
    onPositiveClick: () => emit('clear-all'),
  })
}
</script>

<style scoped>
.chat-sidebar {
  width: var(--chat-sidebar-width, 260px);
  min-width: var(--chat-sidebar-width, 260px);
  background: var(--chat-sidebar-bg, #ffffff);
  border-right: 1px solid var(--chat-border-color, #e0e0e6);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 14px 16px 10px;
}

.brand-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--chat-text-primary, #333639);
}

.sidebar-actions {
  display: flex;
  gap: 8px;
  padding: 0 16px 10px;
}

.sidebar-search {
  padding: 0 16px 8px;
}

.conversations-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 16px 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--chat-text-tertiary, #999);
}

.sidebar-conversations {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.group-label {
  padding: 10px 8px 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--chat-text-tertiary, #999);
}

.session-empty {
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--chat-text-tertiary, #999);
}

.sidebar-footer {
  padding: 10px 16px;
  border-top: 1px solid var(--chat-border-color, #e0e0e6);
}
</style>
