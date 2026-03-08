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
          />

          <!-- Main Area -->
          <div class="chat-main" :class="{ 'sidebar-collapsed': !sidebarVisible }">
            <!-- Header -->
            <div class="chat-header">
              <div class="header-left">
                <button 
                  v-if="!sidebarVisible" 
                  class="header-btn icon-btn"
                  @click="sidebarVisible = true"
                  :title="t('analytics.knowledge.show_sidebar')"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                    <line x1="3" y1="12" x2="21" y2="12" />
                    <line x1="3" y1="6" x2="21" y2="6" />
                    <line x1="3" y1="18" x2="21" y2="18" />
                  </svg>
                </button>
                
                <n-dropdown
                  :options="modelOptions"
                  @select="handleModelSelect"
                  :disabled="llmStatusLoading || models.length === 0"
                  placement="bottom-start"
                >
                  <button class="model-selector" :class="{ loading: llmStatusLoading, disabled: models.length === 0 }">
                    <div class="model-icon">
                      <svg v-if="llmStatusLoading" class="loading-spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83"/>
                      </svg>
                      <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="3"/>
                        <path d="M12 1v6m0 6v6M5.64 5.64l4.24 4.24m4.24 4.24l4.24 4.24M1 12h6m6 0h6M5.64 18.36l4.24-4.24m4.24-4.24l4.24-4.24"/>
                      </svg>
                    </div>
                    <span class="model-name">{{ selectedModelName }}</span>
                    <svg class="dropdown-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                      <polyline points="6 9 12 15 18 9" />
                    </svg>
                  </button>
                </n-dropdown>
              </div>

              <div class="header-center">
                <div class="connection-status" :class="connectionStatusClass">
                  <span class="status-indicator"></span>
                  <span class="status-text">{{ connectionStatusText }}</span>
                </div>
              </div>

              <div class="header-right">
                <button class="header-btn icon-btn" @click="clearMessages" :title="t('analytics.knowledge.clear_chat')">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                    <polyline points="3 6 5 6 21 6" />
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                  </svg>
                </button>
                <button class="header-btn icon-btn" @click="goToKnowledgeBase" :title="t('analytics.knowledge.knowledge_base')">
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

            <!-- Content Area -->
            <div class="chat-content">
              <ChatWelcome v-if="messages.length === 0" @ask="handleAsk" />
              <ChatConversation
                v-else
                :messages="visibleMessages"
                :loading="loading"
                @retry="handleRetry"
                @feedback="handleFeedback"
                @edit="handleEditMessage"
              />
            </div>

            <!-- Input Area -->
            <ChatInputBar
              ref="chatInputBarRef"
              :loading="loading"
              :disabled="sendDisabled"
              :chat-mode="chatMode"
              @send="handleAskPayload"
              @stop="stopGenerating"
              @update:chat-mode="chatMode = $event"
            />
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
import { NConfigProvider, NMessageProvider, NDialogProvider, NInput, NDropdown, createDiscreteApi } from 'naive-ui'
import ChatSidebar from './knowledge/ChatSidebar.vue'
import ChatWelcome from './knowledge/ChatWelcome.vue'
import ChatConversation from './knowledge/ChatConversation.vue'
import ChatInputBar from './knowledge/ChatInputBar.vue'
import { getChatBackendStatus, listModels, type ModelInfo } from '@/api/knowledge-chat'
import { useKnowledgeChat } from '@/composables/useKnowledgeChat'
import { useChatHistory } from '@/composables/useChatHistory'
import { useChatSessionStore } from '@/composables/useChatSessionStore'
import { resolveKnowledgeErrorMessage } from '@/composables/useKnowledgeErrorMessage'
import { KNOWLEDGE_ROUTE_PATHS } from '@/config/knowledge-route'
import type { ChatConnectionState, ChatFeedback, ChatMessage } from '@/composables/useKnowledgeChat'

const { t } = useI18n()
const router = useRouter()
const { message, dialog } = createDiscreteApi(['message', 'dialog'])
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

const themeOverrides = {
  common: {
    primaryColor: '#6366f1',
    primaryColorHover: '#4f46e5',
    primaryColorPressed: '#4338ca',
    borderRadius: '10px',
  },
}

const MODEL_STORAGE_KEY = 'knowledge-chat-selected-model'

const messagesRef = ref<ChatMessage[]>([])
const chatMode = ref<'knowledge' | 'normal'>('knowledge')
const chatInputBarRef = ref<{ focus: () => void; restoreDraft: (value: string) => void } | null>(null)
const sessionSyncPaused = ref(false)
const activeStreamingSessionId = ref('')

const {
  messages,
  loading,
  connectionState,
  sendQuestion,
  clearMessages,
  stopGenerating,
  setMessageFeedback,
  reconnect,
} = useKnowledgeChat({
  messages: messagesRef,
  mode: chatMode,
  conversationId: currentSessionId,
})

watch(
  () => currentSession.value?.id,
  (_nextId, previousId) => {
    sessionSyncPaused.value = true

    if (loading.value) {
      stopGenerating()
      const sourceSessionId = activeStreamingSessionId.value || previousId || ''
      if (sourceSessionId) {
        touchSession(sourceSessionId, [...messagesRef.value], { syncToStorage: 'immediate' })
      }
      activeStreamingSessionId.value = ''
    }

    messagesRef.value = [...(currentSession.value?.messages || [])]
    sessionSyncPaused.value = false
    window.requestAnimationFrame(() => chatInputBarRef.value?.focus())
  },
  { immediate: true },
)

watch(
  messagesRef,
  (value) => {
    if (sessionSyncPaused.value) return

    const targetSessionId = loading.value
      ? activeStreamingSessionId.value || currentSession.value?.id
      : currentSession.value?.id

    if (!targetSessionId) return

    touchSession(targetSessionId, value, loading.value
      ? { refreshUpdatedAt: false, resort: false, syncToStorage: 'deferred' }
      : { syncToStorage: 'immediate' })
  },
  { deep: true },
)

watch(loading, (value, previousValue) => {
  if (value) {
    activeStreamingSessionId.value = currentSession.value?.id || ''
    return
  }

  if (!previousValue) return

  const finishedSessionId = activeStreamingSessionId.value
  activeStreamingSessionId.value = ''
  if (sessionSyncPaused.value || !finishedSessionId) return

  touchSession(finishedSessionId, [...messagesRef.value], { syncToStorage: 'immediate' })
})

const sessionKeyword = ref('')
const sidebarVisible = ref(true)
const chatLayoutRef = ref<HTMLElement>()

const filteredSessions = computed(() => {
  const keyword = sessionKeyword.value.trim().toLowerCase()
  if (!keyword) return sessions.value
  return sessions.value.filter((item) => item.title.toLowerCase().includes(keyword))
})

const llmStatusLoading = ref(true)
const llmEnabled = ref(false)
const models = ref<ModelInfo[]>([])
const selectedModel = ref<string>(localStorage.getItem(MODEL_STORAGE_KEY) || '')

const selectedModelName = computed(() => {
  if (llmStatusLoading.value) return t('analytics.knowledge.loading_models')
  if (models.value.length === 0) return t('analytics.knowledge.no_models')
  if (!selectedModel.value) return t('analytics.knowledge.select_model')
  const model = models.value.find((item) => item.id === selectedModel.value)
  return model?.name || t('analytics.knowledge.select_model')
})

const sendDisabled = computed(() => {
  return llmStatusLoading.value || !llmEnabled.value || models.value.length === 0 || !selectedModel.value
})

const connectionStatusClass = computed(() => `is-${connectionState.value}`)

const connectionStatusTextMap: Record<ChatConnectionState, string> = {
  connected: 'analytics.knowledge.status_connected',
  connecting: 'analytics.knowledge.status_connecting',
  reconnecting: 'analytics.knowledge.status_reconnecting',
  disconnected: 'analytics.knowledge.status_disconnected',
}

const connectionStatusText = computed(() => t(connectionStatusTextMap[connectionState.value]))

const modelOptions = computed(() =>
  models.value.map(m => ({ label: m.name, key: m.id }))
)

const handleModelSelect = (key: string) => {
  selectedModel.value = key
}

watch(selectedModel, (value) => {
  if (value) {
    localStorage.setItem(MODEL_STORAGE_KEY, value)
  } else {
    localStorage.removeItem(MODEL_STORAGE_KEY)
  }
})

const visibleMessages = computed(() => messages.value)

const ensureChatReady = () => {
  if (llmStatusLoading.value) {
    message.warning(t('analytics.knowledge.chat_model_loading'))
    return false
  }
  if (!llmEnabled.value) {
    message.warning(t('analytics.knowledge.chat_llm_unavailable'))
    return false
  }
  if (!selectedModel.value) {
    message.warning(t('analytics.knowledge.chat_model_required'))
    return false
  }
  return true
}

const restoreDraftAfterFailure = (question: string) => {
  chatInputBarRef.value?.restoreDraft(question)
}

const handleReconnectClick = async () => {
  if (connectionState.value === 'connected' || connectionState.value === 'connecting') return

  try {
    await reconnect()
    message.success(t('analytics.knowledge.chat_connection_reconnected'))
  } catch (error) {
    message.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.chat_failed'))
  }
}

const askQuestion = async (question: string, options: { topK?: number } = {}, restoreDraft = false) => {
  if (!ensureChatReady()) {
    if (restoreDraft) restoreDraftAfterFailure(question)
    return
  }

  try {
    await sendQuestion(question, { ...options, modelId: selectedModel.value })
    addQuestion(question)
  } catch (error) {
    if (restoreDraft) restoreDraftAfterFailure(question)
    message.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.chat_failed'))
  }
}

const handleAsk = async (question: string) => {
  await askQuestion(question)
}

const handleAskPayload = async (payload: { question: string; topK: number }) => {
  await askQuestion(payload.question, { topK: payload.topK }, true)
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
  const inputValue = ref(currentTitle)
  dialog.create({
    title: t('analytics.knowledge.rename_session'),
    content: () => {
      return h('div', [
        h(NInput, {
          value: inputValue.value,
          placeholder: t('analytics.knowledge.rename_session_placeholder'),
          onUpdateValue: (v: string) => { inputValue.value = v },
        }),
      ])
    },
    positiveText: t('commons.confirm'),
    negativeText: t('commons.cancel'),
    onPositiveClick: () => {
      renameSession(id, inputValue.value)
    },
  })
}

const handleClearAllSessions = () => {
  clearAllSessions()
}

const handleFeedback = (payload: { messageId: string; rating: 'up' | 'down'; reason?: string }) => {
  const feedback: ChatFeedback = {
    rating: payload.rating,
    reason: payload.reason,
  }
  setMessageFeedback(payload.messageId, feedback)
}

const handleEditMessage = (content: string) => {
  chatInputBarRef.value?.restoreDraft(content)
}

const goToKnowledgeBase = () => {
  router.push(KNOWLEDGE_ROUTE_PATHS.knowledge)
}

const loadLlmStatus = async () => {
  llmStatusLoading.value = true
  try {
    const [status, modelList] = await Promise.all([
      getChatBackendStatus(),
      listModels(),
    ])
    llmEnabled.value = status.llmEnabled
    models.value = modelList

    const preferredModelId = localStorage.getItem(MODEL_STORAGE_KEY) || selectedModel.value
    selectedModel.value = modelList.some((item) => item.id === preferredModelId)
      ? preferredModelId
      : (modelList[0]?.id || '')
  } catch (error) {
    console.error('加载状态失败:', error)
    llmEnabled.value = false
    models.value = []
    selectedModel.value = ''
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
      if (width === 0) return
      if (width < 768) {
        sidebarVisible.value = false
      } else if (width >= 1024) {
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

/* Header */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: white;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
  gap: 16px;
}

.header-left,
.header-center,
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.header-center {
  justify-content: center;
}

.header-right {
  justify-content: flex-end;
}

.header-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 10px;
  cursor: pointer;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  gap: 6px;
  transition: all 0.2s;
}

.header-btn:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #475569;
}

.header-btn.icon-btn {
  width: 40px;
  padding: 0;
}

/* Model Selector */
.model-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.model-selector:hover:not(.disabled) {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.model-selector.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.model-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6366f1;
}

.loading-spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.model-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-arrow {
  color: #94a3b8;
}

/* Connection Status */
.connection-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.connection-status.is-connected {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

.connection-status.is-connected .status-indicator {
  background: #22c55e;
}

.connection-status.is-connecting,
.connection-status.is-reconnecting {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}

.connection-status.is-connecting .status-indicator,
.connection-status.is-reconnecting .status-indicator {
  background: #f59e0b;
  animation: pulse 1.5s ease-in-out infinite;
}

.connection-status.is-disconnected {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.connection-status.is-disconnected .status-indicator {
  background: #ef4444;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

/* User Avatar */
.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  position: relative;
}

.user-avatar::after {
  content: '';
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 12px;
  height: 12px;
  background: #22c55e;
  border: 2px solid white;
  border-radius: 50%;
}

/* Content Area */
.chat-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .chat-header {
    padding: 10px 16px;
  }
  
  .model-name {
    display: none;
  }
  
  .status-text {
    display: none;
  }
  
  .connection-status {
    padding: 6px;
  }
}
</style>
