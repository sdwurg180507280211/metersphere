<template>
  <aside class="chat-sidebar">
    <!-- Brand -->
    <div class="sidebar-brand">
      <div class="brand-logo">
        <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
          <rect x="4" y="4" width="6" height="6" rx="1" />
          <rect x="14" y="4" width="6" height="6" rx="1" />
          <rect x="4" y="14" width="6" height="6" rx="1" />
          <rect x="14" y="14" width="6" height="6" rx="1" />
        </svg>
      </div>
      <span class="brand-text">slothGPT</span>
      <button class="new-chat-btn" @click="emit('new-session')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </button>
    </div>

    <!-- Quick Actions -->
    <div class="quick-actions">
      <div class="action-item">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24">
          <rect x="4" y="4" width="6" height="6" rx="1" />
          <rect x="14" y="4" width="6" height="6" rx="1" />
          <rect x="4" y="14" width="6" height="6" rx="1" />
          <rect x="14" y="14" width="6" height="6" rx="1" />
        </svg>
        <span>Explore GPTs</span>
      </div>
      <div class="action-item">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24">
          <circle cx="9" cy="21" r="1" />
          <circle cx="20" cy="21" r="1" />
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
        </svg>
        <span>GPT Store</span>
      </div>
    </div>

    <!-- Session list -->
    <div class="sidebar-conversations">
      <template v-if="recentSessions.length">
        <div class="group-header">
          <span class="group-title">Recent 7 Days</span>
          <span class="group-count">{{ recentSessions.length }}</span>
        </div>
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
        <div class="group-header">
          <span class="group-title">Earlier</span>
          <span class="group-count">{{ olderSessions.length }}</span>
        </div>
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
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
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
</script>

<style scoped>
.chat-sidebar {
  width: 360px;
  min-width: 360px;
  background: #f8fafc;
  border-right: 1px solid #cbd5e1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 24px;
  border-bottom: 1px solid #cbd5e1;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #4f46e5;
  border-radius: 8px;
  color: white;
  flex-shrink: 0;
}

.brand-text {
  font-size: 30px;
  font-weight: 800;
  color: #1e293b;
  flex: 1;
  letter-spacing: -0.39px;
  line-height: 38px;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid #cbd5e1;
  background: white;
  border-radius: 50%;
  cursor: pointer;
  color: #475569;
  padding: 0;
  transition: background 0.2s;
}

.new-chat-btn:hover {
  background: #f1f5f9;
}

.quick-actions {
  padding: 20px 24px;
  border-bottom: 1px solid #cbd5e1;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 0;
  cursor: pointer;
  color: #1e293b;
  font-size: 18px;
  font-weight: 700;
  line-height: 24px;
  letter-spacing: -0.144px;
}

.action-item:hover {
  color: #4f46e5;
}

.sidebar-conversations {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px 8px;
}

.group-title {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.group-count {
  font-size: 12px;
  color: #94a3b8;
}

.session-empty {
  padding: 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}
</style>
