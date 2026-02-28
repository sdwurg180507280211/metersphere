<template>
  <div class="knowledge-chat-page">
    <el-row :gutter="16">
      <el-col :span="7">
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
              <el-button text @click="clearMessages">{{ t('analytics.knowledge.clear_chat') }}</el-button>
            </div>
          </template>
          <ChatPanel :messages="messages" :loading="loading" @send="handleAsk" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import ChatPanel from './knowledge/ChatPanel.vue'
import { useKnowledgeChat } from '@/composables/useKnowledgeChat'
import { useChatHistory } from '@/composables/useChatHistory'
import { resolveKnowledgeErrorMessage } from '@/composables/useKnowledgeErrorMessage'

const { t } = useI18n()
const { messages, loading, sendQuestion, clearMessages } = useKnowledgeChat()
const { history, addQuestion, clearHistory } = useChatHistory()

const hotKeywords = [
  'analytics.knowledge.keyword_onboarding',
  'analytics.knowledge.keyword_permission',
  'analytics.knowledge.keyword_upload',
]

const handleAsk = async (question: string) => {
  try {
    await sendQuestion(question)
    addQuestion(question)
  } catch (error) {
    ElMessage.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.chat_failed'))
  }
}
</script>

<style scoped>
.knowledge-chat-page {
  padding: 20px;
}

.history-card,
.keyword-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
