<template>
  <aside class="chat-sidebar">
    <!-- Brand -->
    <div class="sidebar-brand">
      <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22" class="brand-icon">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z" />
      </svg>
      <span class="brand-text">MeterSphere AI</span>
      <button class="sidebar-close-btn" @click="emit('toggle-sidebar')" :title="t('analytics.knowledge.chat_sidebar_toggle')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
          <line x1="9" y1="3" x2="9" y2="21" />
        </svg>
      </button>
    </div>

    <!-- New Chat + Search -->
    <div class="sidebar-actions">
      <button class="new-chat-btn" @click="emit('new-session')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        <span>{{ t('analytics.knowledge.new_chat') }}</span>
      </button>
      <button class="search-toggle-btn" @click="showSearch = !showSearch">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
      </button>
    </div>

    <!-- Search -->
    <div v-if="showSearch" class="sidebar-search">
      <n-input
        v-model:value="localKeyword"
        clearable
        size="small"
        :placeholder="t('analytics.knowledge.chat_session_search')"
        @update:value="emit('update:session-keyword', $event)"
      />
    </div>

    <!-- Conversation header -->
    <div class="conversations-header">
      <span>{{ t('analytics.knowledge.chat_your_conversations') }}</span>
      <button class="clear-all-btn" @click="handleClearAll">{{ t('analytics.knowledge.chat_clear_all') }}</button>
    </div>

    <!-- Session list -->
    <div class="sidebar-conversations">
      <!-- Recent group -->
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

      <!-- Older group -->
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

      <div v-if="sessions.length === 0" class="session-empty">
        {{ t('analytics.knowledge.chat_session_empty') }}
      </div>
    </div>

    <!-- Footer -->
    <div class="sidebar-footer">
      <n-tag size="small" :type="llmEnabled ? 'success' : 'default'" :bordered="false">
        {{ t('analytics.knowledge.llm_status_label') }}: {{ llmStatusText }}
      </n-tag>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NInput, NTag, useDialog } from 'naive-ui'
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

const recentSessions = computed(() =>
  props.sessions.filter((s) => Date.now() - s.updatedAt < SEVEN_DAYS),
)

const olderSessions = computed(() =>
  props.sessions.filter((s) => Date.now() - s.updatedAt >= SEVEN_DAYS),
)

const getSessionNegativeCount = (sessionId: string) => {
  const session = props.sessions.find((item) => item.id === sessionId)
  if (!session) return 0
  return session.messages.filter((msg) => msg.feedback?.rating === 'down').length
}

const handleClearAll = () => {
  dialog.warning({
    title: t('commons.prompt'),
    content: t('analytics.knowledge.chat_clear_all') + '?',
    positiveText: t('commons.confirm'),
    negativeText: t('commons.cancel'),
    onPositiveClick: () => {
      emit('clear-all')
    },
  })
}
</script>

<style scoped>
.chat-sidebar {
  width: var(--chat-sidebar-width, 260px);
  min-width: var(--chat-sidebar-width, 260px);
  background: var(--chat-sidebar-bg, #ffffff);
  border-right: 1px solid var(--chat-border-color, #e5e5e5);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 16px 12px;
}

.brand-icon {
  color: var(--chat-accent, #6366f1);
  flex-shrink: 0;
}

.brand-text {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  flex: 1;
  letter-spacing: 0.3px;
}

.sidebar-close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  border-radius: 6px;
  cursor: pointer;
  color: #8e8ea0;
  padding: 0;
}

.sidebar-close-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #303133;
}

.sidebar-actions {
  display: flex;
  gap: 8px;
  padding: 0 16px 12px;
}

.new-chat-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 36px;
  border: 1px solid var(--chat-border-color, #e5e5e5);
  background: #ffffff;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  transition: background-color 0.15s, border-color 0.15s;
}

.new-chat-btn:hover {
  background: var(--chat-session-active-bg, #ececf1);
  border-color: #d0d0d0;
}

.search-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--chat-border-color, #e5e5e5);
  background: #ffffff;
  border-radius: 8px;
  cursor: pointer;
  color: #8e8ea0;
  flex-shrink: 0;
  padding: 0;
}

.search-toggle-btn:hover {
  background: var(--chat-session-active-bg, #ececf1);
  color: #303133;
}

.sidebar-search {
  padding: 0 16px 8px;
}

.conversations-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 16px 8px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #8e8ea0;
}

.clear-all-btn {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 11px;
  color: #8e8ea0;
  padding: 0;
  text-transform: none;
  letter-spacing: normal;
  font-weight: 400;
}

.clear-all-btn:hover {
  color: #f56c6c;
}

.sidebar-conversations {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.group-label {
  padding: 8px 8px 4px;
  font-size: 11px;
  font-weight: 600;
  color: #8e8ea0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.session-empty {
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: #8e8ea0;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--chat-border-color, #e5e5e5);
}
</style>
