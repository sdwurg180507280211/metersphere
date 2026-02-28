<template>
  <div class="chat-panel">
    <div class="message-list">
      <el-empty v-if="messages.length === 0" :description="t('analytics.knowledge.chat_empty')" />

      <div
        v-for="message in messages"
        :key="message.id"
        class="message-item"
        :class="message.role === 'user' ? 'is-user' : 'is-assistant'"
      >
        <div class="message-role">
          {{ message.role === 'user' ? t('analytics.knowledge.chat_user') : t('analytics.knowledge.chat_assistant') }}
        </div>
        <div class="message-content">{{ message.content }}</div>
        <SourceList v-if="message.role === 'assistant' && message.sources?.length" :sources="message.sources" />
      </div>
    </div>

    <div class="input-box">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="4"
        :placeholder="t('analytics.knowledge.chat_placeholder')"
        @keydown.enter.exact.prevent="submit"
      />
      <div class="input-actions">
        <el-button type="primary" :loading="loading" @click="submit">
          {{ t('analytics.knowledge.chat_send') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import SourceList from './SourceList.vue'
import type { ChatMessage } from '@/composables/useKnowledgeChat'

const props = defineProps<{
  messages: ChatMessage[]
  loading: boolean
}>()

const emit = defineEmits<{
  send: [question: string]
}>()

const { t } = useI18n()
const draft = ref('')

const submit = () => {
  const question = draft.value.trim()
  if (!question) {
    return
  }
  emit('send', question)
  draft.value = ''
}
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-list {
  min-height: 360px;
  max-height: 520px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.message-item {
  margin-bottom: 10px;
  padding: 10px;
  border-radius: 8px;
}

.message-item.is-user {
  background: #ecf5ff;
}

.message-item.is-assistant {
  background: #f5f7fa;
}

.message-role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #303133;
}

.input-box {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
