<template>
  <div ref="chatLayoutRef" class="chat-layout">
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

    <div class="chat-main" :class="{ 'sidebar-collapsed': !sidebarVisible }">
      <div class="chat-main-header">
        <div class="header-left">
          <n-button v-if="!sidebarVisible" text size="small" class="sidebar-toggle-btn" @click="sidebarVisible = true">
            {{ t('analytics.knowledge.chat_sidebar_toggle') }}
          </n-button>
          <span class="header-title">{{ t('analytics.knowledge.chat_title') }}</span>
        </div>
        <div class="header-right">
          <div class="feedback-filter">
            <n-select v-model:value="feedbackFilter" size="small" style="width: 140px" :options="feedbackOptions" />
          </div>
          <n-button text @click="handleExportSession">{{ t('analytics.knowledge.export_chat') }}</n-button>
          <n-button text @click="clearMessages">{{ t('analytics.knowledge.clear_chat') }}</n-button>
        </div>
      </div>

      <ChatWelcome v-if="messages.length === 0" @ask="handleAsk" />
      <ChatConversation
        v-else
        :messages="visibleMessages"
        :loading="loading"
        @retry="handleRetry"
        @feedback="handleFeedback"
      />

      <ChatInputBar :loading="loading" @send="handleAskPayload" @stop="stopGenerating" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NButton, NSelect, NInput, useMessage, useDialog } from 'naive-ui'
import ChatSidebar from './knowledge/ChatSidebar.vue'
import ChatWelcome from './knowledge/ChatWelcome.vue'
import ChatConversation from './knowledge/ChatConversation.vue'
import ChatInputBar from './knowledge/ChatInputBar.vue'
import { getChatBackendStatus } from '@/api/knowledge-chat'
import { useKnowledgeChat } from '@/composables/useKnowledgeChat'
import { useChatHistory } from '@/composables/useChatHistory'
import { useChatSessionStore } from '@/composables/useChatSessionStore'
import { resolveKnowledgeErrorMessage } from '@/composables/useKnowledgeErrorMessage'
import type { ChatFeedback, ChatMessage } from '@/composables/useKnowledgeChat'

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()
const { addQuestion } = useChatHistory()
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

const promptRename = (currentTitle: string) =>
  new Promise<string | null>((resolve) => {
    const inputValue = ref(currentTitle)
    let resolved = false
    const done = (value: string | null) => {
      if (resolved) return
      resolved = true
      resolve(value)
    }
    dialog.create({
      title: t('analytics.knowledge.rename_session'),
      content: () =>
        h(NInput, {
          value: inputValue.value,
          placeholder: t('analytics.knowledge.rename_session_placeholder'),
          'onUpdate:value': (value: string) => {
            inputValue.value = value
          },
        }),
      positiveText: t('commons.confirm'),
      negativeText: t('commons.cancel'),
      onPositiveClick: () => done(inputValue.value),
      onNegativeClick: () => done(null),
      onClose: () => done(null),
    })
  })

const handleRenameSession = async (id: string, currentTitle: string) => {
  const value = await promptRename(currentTitle)
  if (value === null) return
  renameSession(id, value || currentTitle)
}

const handleClearAllSessions = () => {
  clearAllSessions()
}

const handleExportSession = () => {
  if (!currentSession.value) return

  const lines: string[] = [
    `# ${currentSession.value.title}`,
    '',
    ...currentSession.value.messages.flatMap((item) => [
      `## ${item.role === 'user' ? t('analytics.knowledge.chat_user') : t('analytics.knowledge.chat_assistant')}`,
      '',
      item.content,
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

let resizeObserver: ResizeObserver | null = null
onMounted(() => {
  loadLlmStatus()
  if (chatLayoutRef.value) {
    resizeObserver = new ResizeObserver((entries) => {
      const width = entries[0]?.contentRect.width || 0
      if (width < 600 && sidebarVisible.value) {
        sidebarVisible.value = false
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
  height: 100%;
  overflow: hidden;

  --chat-sidebar-width: 260px;
  --chat-accent: #18a058;
  --chat-accent-hover: #0c7a43;
  --chat-accent-suppl: #36ad6a;
  --chat-main-bg: #fafafc;
  --chat-sidebar-bg: #ffffff;
  --chat-user-bubble-bg: #18a058;
  --chat-user-bubble-text: #ffffff;
  --chat-bot-bubble-bg: #ffffff;
  --chat-bot-bubble-text: #333639;
  --chat-border-color: #e0e0e6;
  --chat-border-radius: 3px;
  --chat-session-active-bg: #f0f9eb;
  --chat-text-primary: #333639;
  --chat-text-secondary: #666e7a;
  --chat-text-tertiary: #999;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--chat-main-bg);
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

.chat-main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 48px;
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
  font-size: 16px;
  font-weight: 500;
  color: var(--chat-text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.feedback-filter {
  display: flex;
  align-items: center;
}
</style>
