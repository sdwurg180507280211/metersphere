<template>
  <div class="knowledge-chat-page">
    <el-row :gutter="16">
      <el-col :span="7">
        <el-card class="session-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ t('analytics.knowledge.chat_sessions') }}</span>
              <el-button text @click="handleNewSession">{{ t('analytics.knowledge.new_chat') }}</el-button>
            </div>
          </template>
          <el-input
            v-model="sessionKeyword"
            clearable
            class="session-search"
            :placeholder="t('analytics.knowledge.chat_session_search')"
          />
          <div class="session-list">
            <div
              v-for="item in filteredSessions"
              :key="item.id"
              class="session-item"
              :class="{ active: item.id === currentSessionId }"
            >
              <el-button text class="session-title" @click="handleSelectSession(item.id)">
                {{ item.title }}
              </el-button>
              <el-button text class="session-rename" @click="handleRenameSession(item.id, item.title)">
                {{ t('analytics.knowledge.rename_session') }}
              </el-button>
              <el-button text class="session-delete" @click="handleDeleteSession(item.id)">
                {{ t('commons.delete') }}
              </el-button>
            </div>
            <el-empty v-if="filteredSessions.length === 0" :description="t('analytics.knowledge.chat_session_empty')" />
          </div>
        </el-card>

        <el-card class="history-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ t('analytics.knowledge.chat_history') }}</span>
              <el-button text @click="clearHistory">{{ t('analytics.knowledge.clear_history') }}</el-button>
            </div>
          </template>
          <el-empty v-if="history.length === 0" :description="t('analytics.knowledge.chat_history_empty')" />
          <div v-else class="history-list">
            <el-button
              v-for="item in history"
              :key="item"
              text
              class="history-item"
              @click="handleAsk(item)"
            >
              {{ item }}
            </el-button>
          </div>
        </el-card>

        <el-card class="keyword-card" shadow="never">
          <template #header>
            <span>{{ t('analytics.knowledge.hot_keywords') }}</span>
          </template>
          <div class="keyword-list">
            <el-tag
              v-for="item in hotKeywords"
              :key="item"
              class="keyword-tag"
              @click="handleAsk(t(item))"
            >
              {{ t(item) }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :span="17">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ t('analytics.knowledge.chat_title') }}</span>
              <div class="chat-header-actions">
                <el-button text @click="handleExportSession">{{ t('analytics.knowledge.export_chat') }}</el-button>
                <el-button text @click="clearMessages">{{ t('analytics.knowledge.clear_chat') }}</el-button>
              </div>
            </div>
          </template>
          <ChatPanel
            :messages="messages"
            :loading="loading"
            @send="handleAskPayload"
            @stop="stopGenerating"
            @retry="handleRetry"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatPanel from './knowledge/ChatPanel.vue'
import { useKnowledgeChat } from '@/composables/useKnowledgeChat'
import { useChatHistory } from '@/composables/useChatHistory'
import { useChatSessionStore } from '@/composables/useChatSessionStore'
import { resolveKnowledgeErrorMessage } from '@/composables/useKnowledgeErrorMessage'
import type { ChatMessage } from '@/composables/useKnowledgeChat'

const { t } = useI18n()
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
} = useChatSessionStore()

const messagesRef = ref<ChatMessage[]>([])
const { messages, loading, sendQuestion, clearMessages, stopGenerating } = useKnowledgeChat({
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
    if (!currentSession.value) {
      return
    }
    touchSession(currentSession.value.id, value)
  },
  { deep: true },
)

const hotKeywords = [
  'analytics.knowledge.keyword_onboarding',
  'analytics.knowledge.keyword_permission',
  'analytics.knowledge.keyword_upload',
]

const sessionKeyword = ref('')

const filteredSessions = computed(() => {
  const keyword = sessionKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return sessions.value
  }
  return sessions.value.filter((item) => item.title.toLowerCase().includes(keyword))
})

const handleAsk = async (question: string) => {
  try {
    await sendQuestion(question)
    addQuestion(question)
  } catch (error) {
    ElMessage.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.chat_failed'))
  }
}

const handleAskPayload = async (payload: { question: string; topK: number }) => {
  try {
    await sendQuestion(payload.question, { topK: payload.topK })
    addQuestion(payload.question)
  } catch (error) {
    ElMessage.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.chat_failed'))
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
  try {
    const { value } = await ElMessageBox.prompt(
      t('analytics.knowledge.rename_session_prompt'),
      t('analytics.knowledge.rename_session'),
      {
        inputValue: currentTitle,
        inputPlaceholder: t('analytics.knowledge.rename_session_placeholder'),
        confirmButtonText: t('commons.confirm'),
        cancelButtonText: t('commons.cancel'),
      },
    )
    renameSession(id, value || currentTitle)
  } catch {
  }
}

const handleExportSession = () => {
  if (!currentSession.value) {
    return
  }

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
  ElMessage.success(t('analytics.knowledge.export_chat_success'))
}
</script>

<style scoped>
.knowledge-chat-page {
  padding: 20px;
}

.session-card,
.history-card,
.keyword-card {
  margin-bottom: 16px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-search {
  margin-bottom: 10px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 4px 6px;
}

.session-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.session-title {
  flex: 1;
  justify-content: flex-start;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-delete {
  flex-shrink: 0;
}

.session-rename {
  flex-shrink: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header-actions {
  display: flex;
  gap: 8px;
}

.history-list {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.history-item {
  width: 100%;
  justify-content: flex-start;
  white-space: normal;
  text-align: left;
  line-height: 1.5;
}

.keyword-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-tag {
  cursor: pointer;
}
</style>
