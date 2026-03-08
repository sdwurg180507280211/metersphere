<template>
  <aside class="chat-sidebar">
    <!-- Header -->
    <div class="sidebar-header">
      <div class="brand">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
            <rect x="4" y="4" width="6" height="6" rx="1.5" />
            <rect x="14" y="4" width="6" height="6" rx="1.5" />
            <rect x="4" y="14" width="6" height="6" rx="1.5" />
            <rect x="14" y="14" width="6" height="6" rx="1.5" />
          </svg>
        </div>
        <span class="brand-text">AI 助手</span>
      </div>
      <button class="new-chat-btn" @click="emit('new-session')" :title="t('analytics.knowledge.new_chat')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </button>
    </div>

    <!-- Search -->
    <div class="search-section">
      <div class="search-input-wrapper">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          v-model="searchKeyword"
          type="text"
          :placeholder="t('analytics.knowledge.search_sessions')"
          class="search-input"
        />
        <button v-if="searchKeyword" class="clear-search" @click="searchKeyword = ''">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Session List -->
    <div class="sessions-container">
      <!-- Today's Sessions -->
      <div v-if="todaySessions.length" class="session-group">
        <div class="group-header">
          <span class="group-title">{{ t('analytics.knowledge.today') }}</span>
          <span class="group-count">{{ todaySessions.length }}</span>
        </div>
        <div class="session-list">
          <ChatSessionItem
            v-for="session in todaySessions"
            :key="session.id"
            :session="session"
            :active="session.id === currentSessionId"
            @select="emit('select-session', session.id)"
            @delete="emit('delete-session', session.id)"
            @rename="(newTitle) => emit('rename-session', session.id, newTitle)"
          />
        </div>
      </div>

      <!-- Yesterday's Sessions -->
      <div v-if="yesterdaySessions.length" class="session-group">
        <div class="group-header">
          <span class="group-title">{{ t('analytics.knowledge.yesterday') }}</span>
          <span class="group-count">{{ yesterdaySessions.length }}</span>
        </div>
        <div class="session-list">
          <ChatSessionItem
            v-for="session in yesterdaySessions"
            :key="session.id"
            :session="session"
            :active="session.id === currentSessionId"
            @select="emit('select-session', session.id)"
            @delete="emit('delete-session', session.id)"
            @rename="(newTitle) => emit('rename-session', session.id, newTitle)"
          />
        </div>
      </div>

      <!-- Last 7 Days -->
      <div v-if="last7DaysSessions.length" class="session-group">
        <div class="group-header">
          <span class="group-title">{{ t('analytics.knowledge.last_7_days') }}</span>
          <span class="group-count">{{ last7DaysSessions.length }}</span>
        </div>
        <div class="session-list">
          <ChatSessionItem
            v-for="session in last7DaysSessions"
            :key="session.id"
            :session="session"
            :active="session.id === currentSessionId"
            @select="emit('select-session', session.id)"
            @delete="emit('delete-session', session.id)"
            @rename="(newTitle) => emit('rename-session', session.id, newTitle)"
          />
        </div>
      </div>

      <!-- Earlier -->
      <div v-if="earlierSessions.length" class="session-group">
        <div class="group-header">
          <span class="group-title">{{ t('analytics.knowledge.earlier') }}</span>
          <span class="group-count">{{ earlierSessions.length }}</span>
        </div>
        <div class="session-list">
          <ChatSessionItem
            v-for="session in earlierSessions"
            :key="session.id"
            :session="session"
            :active="session.id === currentSessionId"
            @select="emit('select-session', session.id)"
            @delete="emit('delete-session', session.id)"
            @rename="(newTitle) => emit('rename-session', session.id, newTitle)"
          />
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="filteredSessions.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </div>
        <p class="empty-title">{{ searchKeyword ? t('analytics.knowledge.no_search_results') : t('analytics.knowledge.no_sessions') }}</p>
        <p class="empty-desc">{{ searchKeyword ? t('analytics.knowledge.try_different_keyword') : t('analytics.knowledge.start_new_chat') }}</p>
      </div>
    </div>

    <!-- Footer -->
    <div class="sidebar-footer">
      <div class="llm-status" :class="{ enabled: llmEnabled }">
        <span class="status-dot"></span>
        <span class="status-text">{{ llmStatusText }}</span>
      </div>
      <button 
        v-if="sessions.length > 0" 
        class="clear-all-btn" 
        @click="showClearConfirm = true"
        :title="t('analytics.knowledge.clear_all_sessions')"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <polyline points="3 6 5 6 21 6" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        </svg>
      </button>
    </div>

    <!-- Clear Confirm Dialog -->
    <Teleport to="body">
      <div v-if="showClearConfirm" class="confirm-overlay" @click="showClearConfirm = false">
        <div class="confirm-dialog" @click.stop>
          <div class="confirm-icon warning">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
          </div>
          <h3 class="confirm-title">{{ t('analytics.knowledge.clear_all_confirm_title') }}</h3>
          <p class="confirm-desc">{{ t('analytics.knowledge.clear_all_confirm_desc') }}</p>
          <div class="confirm-actions">
            <button class="confirm-btn secondary" @click="showClearConfirm = false">
              {{ t('commons.cancel') }}
            </button>
            <button class="confirm-btn danger" @click="handleClearAll">
              {{ t('commons.confirm') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Teleport } from 'vue'
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
}>()

const { t } = useI18n()
const searchKeyword = ref('')
const showClearConfirm = ref(false)

// Time constants
const ONE_DAY = 24 * 60 * 60 * 1000
const SEVEN_DAYS = 7 * ONE_DAY

const filteredSessions = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return props.sessions
  return props.sessions.filter((s) => s.title.toLowerCase().includes(keyword))
})

const todaySessions = computed(() => {
  const now = new Date()
  return filteredSessions.value.filter((s) => {
    const date = new Date(s.updatedAt)
    return isSameDay(date, now)
  })
})

const yesterdaySessions = computed(() => {
  const yesterday = new Date(Date.now() - ONE_DAY)
  return filteredSessions.value.filter((s) => {
    const date = new Date(s.updatedAt)
    return isSameDay(date, yesterday)
  })
})

const last7DaysSessions = computed(() => {
  const now = Date.now()
  return filteredSessions.value.filter((s) => {
    const diff = now - s.updatedAt
    return diff > ONE_DAY && diff < SEVEN_DAYS
  })
})

const earlierSessions = computed(() => {
  const now = Date.now()
  return filteredSessions.value.filter((s) => now - s.updatedAt >= SEVEN_DAYS)
})

const isSameDay = (d1: Date, d2: Date) => {
  return d1.getFullYear() === d2.getFullYear() &&
    d1.getMonth() === d2.getMonth() &&
    d1.getDate() === d2.getDate()
}

const handleClearAll = () => {
  emit('clear-all')
  showClearConfirm.value = false
}
</script>

<style scoped>
.chat-sidebar {
  width: 300px;
  min-width: 300px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Header */
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  color: white;
  flex-shrink: 0;
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.3px;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 10px;
  cursor: pointer;
  color: #64748b;
  padding: 0;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  border-color: #6366f1;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.05);
}

/* Search */
.search-section {
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #94a3b8;
  pointer-events: none;
}

.search-input {
  width: 100%;
  height: 40px;
  padding: 0 36px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  color: #1e293b;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.search-input::placeholder {
  color: #94a3b8;
}

.clear-search {
  position: absolute;
  right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: #f1f5f9;
  border-radius: 6px;
  cursor: pointer;
  color: #64748b;
  padding: 0;
  transition: all 0.2s;
}

.clear-search:hover {
  background: #e2e8f0;
  color: #475569;
}

/* Sessions Container */
.sessions-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.session-group {
  margin-bottom: 16px;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px 6px;
}

.group-title {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.group-count {
  font-size: 11px;
  font-weight: 500;
  color: #94a3b8;
  background: #e2e8f0;
  padding: 2px 8px;
  border-radius: 10px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 12px;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.empty-icon {
  color: #cbd5e1;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  margin: 0 0 4px;
}

.empty-desc {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

/* Footer */
.sidebar-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid #e2e8f0;
  background: #f1f5f9;
}

.llm-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  animation: pulse 2s infinite;
}

.llm-status.enabled .status-dot {
  background: #22c55e;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.clear-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  color: #94a3b8;
  padding: 0;
  transition: all 0.2s;
}

.clear-all-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

/* Confirm Dialog */
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s;
}

.confirm-dialog {
  background: white;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 360px;
  text-align: center;
  animation: scaleIn 0.2s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.confirm-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  margin: 0 auto 16px;
}

.confirm-icon.warning {
  background: #fef3c7;
  color: #f59e0b;
}

.confirm-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px;
}

.confirm-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 24px;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.confirm-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.confirm-btn.secondary {
  background: #f1f5f9;
  color: #64748b;
}

.confirm-btn.secondary:hover {
  background: #e2e8f0;
}

.confirm-btn.danger {
  background: #ef4444;
  color: white;
}

.confirm-btn.danger:hover {
  background: #dc2626;
}

/* Scrollbar */
.sessions-container::-webkit-scrollbar {
  width: 6px;
}

.sessions-container::-webkit-scrollbar-track {
  background: transparent;
}

.sessions-container::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.sessions-container::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .chat-sidebar {
    width: 100%;
    min-width: unset;
  }
}
</style>
