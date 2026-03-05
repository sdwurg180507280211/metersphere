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
              <button class="model-selector">
                <span>sloth GPT 7.0</span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>
            </div>
            <div class="header-center">
              <button class="header-icon-btn" title="Share">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                  <circle cx="18" cy="5" r="3" />
                  <circle cx="6" cy="12" r="3" />
                  <circle cx="18" cy="19" r="3" />
                  <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                  <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                </svg>
              </button>
              <button class="header-icon-btn" @click="clearMessages" title="Clear">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
              </button>
            </div>
            <div class="header-right">
              <button class="header-icon-btn">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                  <line x1="4" y1="21" x2="4" y2="14" />
                  <line x1="4" y1="10" x2="4" y2="3" />
                  <line x1="12" y1="21" x2="12" y2="12" />
                  <line x1="12" y1="8" x2="12" y2="3" />
                  <line x1="20" y1="21" x2="20" y2="16" />
                  <line x1="20" y1="12" x2="20" y2="3" />
                  <line x1="1" y1="14" x2="7" y2="14" />
                  <line x1="9" y1="8" x2="15" y2="8" />
                  <line x1="17" y1="16" x2="23" y2="16" />
                </svg>
              </button>
              <button class="header-icon-btn" @click="goToKnowledgeBase">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                </svg>
              </button>
              <div class="user-avatar">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
              </div>
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
          <ChatInputBar :loading="loading" :chat-mode="chatMode" @send="handleAskPayload" @stop="stopGenerating" @update:chat-mode="chatMode = $event" />
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
const chatMode = ref<'knowledge' | 'normal'>('normal')
const { messages, loading, sendQuestion, clearMessages, stopGenerating, setMessageFeedback } = useKnowledgeChat({
  messages: messagesRef,
  mode: chatMode,
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
  background: white;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  min-width: 0;
  overflow: hidden;
}

.chat-main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px;
  background: white;
  border-bottom: 1px solid #cbd5e1;
  flex-shrink: 0;
}

.header-left,
.header-center,
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.header-center {
  justify-content: center;
}

.header-right {
  justify-content: flex-end;
}

.model-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 18px;
  font-weight: 700;
  color: #475569;
  line-height: 24px;
  letter-spacing: -0.144px;
}

.model-selector:hover {
  color: #1e293b;
}

.header-icon-btn {
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

.header-icon-btn:hover {
  background: #f8fafc;
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #4f46e5;
  color: white;
  position: relative;
}

.user-avatar::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #22c55e;
  border: 1.5px solid white;
  border-radius: 50%;
}
</style>
