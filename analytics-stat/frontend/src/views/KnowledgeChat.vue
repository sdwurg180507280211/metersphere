<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <div ref="chatLayoutRef" class="chat-layout">
        <!-- Sidebar -->
        <ChatSidebar
          v-show="sidebarVisible"
          :sessions="filteredSessions"
          :current-session-id="currentSessionId"
          :session-keyword="sessionKeyword"
          :llm-enabled="llmEnabled"
          :llm-status-text="llmStatusText"
          @new-session="handleNewSession"
          @select-session="handleSelectSession"
          @delete-session="handleDeleteSession"
          @rename-session="handleRenameSession"
          @clear-all="handleClearAllSessions"
          @update:session-keyword="sessionKeyword = $event"
          @toggle-sidebar="sidebarVisible = false"
        />

        <!-- Main area -->
        <div class="chat-main" :class="{ 'sidebar-collapsed': !sidebarVisible }">
          <!-- Header -->
          <div class="chat-main-header">
            <div class="header-left">
              <button v-if="!sidebarVisible" class="sidebar-toggle-btn" @click="sidebarVisible = true" :title="t('analytics.knowledge.chat_sidebar_toggle')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                  <line x1="9" y1="3" x2="9" y2="21" />
                </svg>
              </button>
              <span class="header-title">{{ t('analytics.knowledge.chat_title') }}</span>
            </div>
            <div class="header-right">
              <div class="feedback-filter">
                <n-select v-model:value="feedbackFilter" size="small" style="width: 130px" :options="feedbackOptions" />
              </div>
              <button class="header-action-btn" @click="goToKnowledgeBase" :title="t('analytics.knowledge.go_to_knowledge_base')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                </svg>
              </button>
              <button class="header-action-btn" @click="handleExportSession" :title="t('analytics.knowledge.export_chat')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="7 10 12 15 17 10" />
                  <line x1="12" y1="15" x2="12" y2="3" />
                </svg>
              </button>
              <button class="header-action-btn" @click="clearMessages" :title="t('analytics.knowledge.clear_chat')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Welcome or Conversation -->
          <ChatWelcome v-if="messages.length === 0" @ask="handleAsk" />
          <ChatConversation
            v-else
            :messages="visibleMessages"
            :loading="loading"
            @retry="handleRetry"
            @feedback="handleFeedback"
          />

          <!-- Input bar -->
          <ChatInputBar :loading="loading" @send="handleAskPayload" @stop="stopGenerating" />
        </div>
      </div>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { NConfigProvider, NMessageProvider, NDialogProvider, NSelect, NInput, createDiscreteApi } from 'naive-ui'
import ChatSidebar from './knowledge/ChatSidebar.vue'
import ChatWelcome from './knowledge/ChatWelcome.vue'
import ChatConversation from './knowledge/ChatConversation.vue'
import ChatInputBar from './knowledge/ChatInputBar.vue'
import { getChatBackendStatus } from '@/api/knowledge-chat'
import { useKnowledgeChat } from '@/composables/useKnowledgeChat'
import { useChatHistory } from '@/composables/useChatHistory'
import { useChatSessionStore } from '@/composables/useChatSessionStore'
import { resolveKnowledgeErrorMessage } from '@/composables/useKnowledgeErrorMessage'
import { KNOWLEDGE_ROUTE_PATHS } from '@/config/knowledge-route'
import type { ChatFeedback, ChatMessage } from '@/composables/useKnowledgeChat'

const { t } = useI18n()
const router = useRouter()
const { message, dialog } = createDiscreteApi(['message', 'dialog'])
const { history, addQuestion, clearHistory } = useChatHistory()
const {
  sessions,
  currentSession,
  currentSessionId,
  createNewSession,
  selectSession,
  deleteSession,
  renameSession,
  touchSession,
  clearAllSessions,
} = useChatSessionStore()

const themeOverrides = {
  common: {
    primaryColor: '#18a058',
    primaryColorHover: '#36ad6a',
    primaryColorPressed: '#0c7a43',
    borderRadius: '3px',
  },
}

const messagesRef = ref<ChatMessage[]>([])
const { messages, loading, sendQuestion, clearMessages, stopGenerating, setMessageFeedback } = useKnowledgeChat({
  messages: messagesRef,
})

watch(
  () => currentSession.value?.id,
  () => {
    stopGenerating()
    messagesRef.value = [...(currentSession.value?.messages || [])]
  },
  { immediate: true },
)

watch(
  messagesRef,
  (value) => {
    if (!currentSession.value) return
    touchSession(currentSession.value.id, value)
  },
  { deep: true },
)

const sessionKeyword = ref('')
const sidebarVisible = ref(true)
const chatLayoutRef = ref<HTMLElement>()

const filteredSessions = computed(() => {
  const keyword = sessionKeyword.value.trim().toLowerCase()
  if (!keyword) return sessions.value
  return sessions.value.filter((item) => item.title.toLowerCase().includes(keyword))
})

const feedbackFilter = ref<'all' | 'down'>('all')
const feedbackOptions = computed(() => [
  { label: t('analytics.knowledge.feedback_filter_all'), value: 'all' },
  { label: t('analytics.knowledge.feedback_filter_negative'), value: 'down' },
])

const llmStatusLoading = ref(true)
const llmEnabled = ref(false)

const visibleMessages = computed(() => {
  if (feedbackFilter.value === 'all') return messages.value
  return messages.value.filter((item) => item.role === 'user' || item.feedback?.rating === 'down')
})

const handleAsk = async (question: string) => {
  try {
    await sendQuestion(question)
    addQuestion(question)
  } catch (error) {
    message.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.chat_failed'))
  }
}

const handleAskPayload = async (payload: { question: string; topK: number }) => {
  try {
    await sendQuestion(payload.question, { topK: payload.topK })
    addQuestion(payload.question)
  } catch (error) {
    message.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.chat_failed'))
  }
}

const handleRetry = async (question: string) => {
  await handleAskPayload({ question, topK: 5 })
}

const handleNewSession = () => {
  stopGenerating()
  createNewSession()
}

const handleSelectSession = (id: string) => {
  selectSession(id)
}

const handleDeleteSession = (id: string) => {
  stopGenerating()
  deleteSession(id)
}

const handleRenameSession = async (id: string, currentTitle: string) => {
  dialog.create({
    title: t('analytics.knowledge.rename_session'),
    content: () => {
      const inputRef = ref(currentTitle)
      return h('div', [
        h(NInput, {
          value: inputRef.value,
          placeholder: t('analytics.knowledge.rename_session_placeholder'),
          onUpdateValue: (v: string) => { inputRef.value = v },
        }),
      ])
    },
    positiveText: t('commons.confirm'),
    negativeText: t('commons.cancel'),
    onPositiveClick: () => {
      renameSession(id, currentTitle)
    },
  })
}

const handleClearAllSessions = () => {
  clearAllSessions()
}

const handleExportSession = () => {
  if (!currentSession.value) return

  const lines: string[] = [
    `# ${currentSession.value.title}`,
    '',
    ...currentSession.value.messages.flatMap((message) => [
      `## ${message.role === 'user' ? t('analytics.knowledge.chat_user') : t('analytics.knowledge.chat_assistant')}`,
      '',
      message.content,
      '',
    ]),
  ]

  const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `${currentSession.value.title.replace(/\s+/g, '-') || 'chat'}.md`
  anchor.click()
  URL.revokeObjectURL(url)
  message.success(t('analytics.knowledge.export_chat_success'))
}

const handleFeedback = (payload: { messageId: string; rating: 'up' | 'down'; reason?: string }) => {
  const feedback: ChatFeedback = {
    rating: payload.rating,
    reason: payload.reason,
  }
  setMessageFeedback(payload.messageId, feedback)
}

const goToKnowledgeBase = () => {
  router.push(KNOWLEDGE_ROUTE_PATHS.knowledge)
}

const loadLlmStatus = async () => {
  llmStatusLoading.value = true
  try {
    const status = await getChatBackendStatus()
    llmEnabled.value = status.llmEnabled
  } catch {
    llmEnabled.value = false
  } finally {
    llmStatusLoading.value = false
  }
}

const llmStatusText = computed(() => {
  if (llmStatusLoading.value) return t('analytics.knowledge.llm_status_checking')
  return llmEnabled.value ? t('analytics.knowledge.llm_status_on') : t('analytics.knowledge.llm_status_off')
})

// Auto-collapse sidebar on narrow container
let resizeObserver: ResizeObserver | null = null
onMounted(() => {
  loadLlmStatus()
  if (chatLayoutRef.value) {
    resizeObserver = new ResizeObserver((entries) => {
      const width = entries[0]?.contentRect.width || 0
      if (width === 0) return // Ignore initial layout
      if (width < 600) {
        sidebarVisible.value = false
      } else if (width >= 800) {
        sidebarVisible.value = true
      }
    })
    resizeObserver.observe(chatLayoutRef.value)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;

  /* Design tokens */
  --chat-sidebar-width: 260px;
  --chat-accent: #6366f1;
  --chat-accent-hover: #4f46e5;
  --chat-main-bg: #f7f7f8;
  --chat-sidebar-bg: #ffffff;
  --chat-user-bubble-bg: #f0f0f0;
  --chat-user-bubble-text: #303133;
  --chat-bot-bubble-bg: #ffffff;
  --chat-bot-bubble-text: #303133;
  --chat-dark-card-bg: #202123;
  --chat-dark-card-text: #ffffff;
  --chat-light-card-bg: #ffffff;
  --chat-light-card-text: #303133;
  --chat-border-color: #e5e5e5;
  --chat-session-active-bg: #ececf1;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--chat-main-bg);
  min-width: 0;
  overflow: hidden;
}

.chat-main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #ffffff;
  border-bottom: 1px solid var(--chat-border-color);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.sidebar-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  border-radius: 6px;
  cursor: pointer;
  color: #8e8ea0;
  padding: 0;
}

.sidebar-toggle-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  border-radius: 6px;
  cursor: pointer;
  color: #8e8ea0;
  padding: 0;
}

.header-action-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #303133;
}

.feedback-filter {
  display: flex;
  align-items: center;
}
</style>
