<template>
  <div class="chat-panel">
    <div ref="messageListRef" class="message-list">
      <el-empty v-if="messages.length === 0" :description="t('analytics.knowledge.chat_empty')" />

      <div
        v-for="(message, index) in messages"
        :key="message.id"
        class="message-item"
        :class="message.role === 'user' ? 'is-user' : 'is-assistant'"
      >
        <div class="message-role">
          {{ message.role === 'user' ? t('analytics.knowledge.chat_user') : t('analytics.knowledge.chat_assistant') }}
        </div>
        <div class="message-content">{{ message.content }}</div>
        <SourceList v-if="message.role === 'assistant' && message.sources?.length" :sources="message.sources" />
        <div v-if="message.role === 'assistant'" class="message-actions">
          <el-button text size="small" @click="copyMessage(message.content)">
            {{ t('analytics.knowledge.chat_copy') }}
          </el-button>
          <el-button text size="small" @click="retryMessage(index)">
            {{ t('analytics.knowledge.chat_retry') }}
          </el-button>
        </div>
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
        <div class="topk-control">
          <span class="topk-label">{{ t('analytics.knowledge.chat_topk_label') }}</span>
          <el-input-number v-model="topK" :min="1" :max="10" size="small" />
        </div>
        <el-button type="primary" :loading="loading" @click="submit">
          {{ t('analytics.knowledge.chat_send') }}
        </el-button>
        <el-button v-if="loading" @click="emit('stop')">
          {{ t('analytics.knowledge.chat_stop') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import SourceList from './SourceList.vue'
import type { ChatMessage } from '@/composables/useKnowledgeChat'

const props = defineProps<{
  messages: ChatMessage[]
  loading: boolean
}>()

const emit = defineEmits<{
  send: [{ question: string; topK: number }]
  stop: []
  retry: [question: string]
}>()

const { t } = useI18n()
const draft = ref('')
const topK = ref(5)
const messageListRef = ref<HTMLElement>()

const submit = () => {
  const question = draft.value.trim()
  if (!question) {
    return
  }
  emit('send', { question, topK: topK.value })
  draft.value = ''
}

const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success(t('analytics.knowledge.chat_copied'))
  } catch {
    ElMessage.warning(t('analytics.knowledge.chat_copy_failed'))
  }
}

const retryMessage = (assistantIndex: number) => {
  for (let i = assistantIndex - 1; i >= 0; i -= 1) {
    const message = props.messages[i]
    if (message.role === 'user') {
      emit('retry', message.content)
      return
    }
  }
}

watch(
  () => props.messages.length,
  async () => {
    await nextTick()
    if (!messageListRef.value) {
      return
    }
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  },
)
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

.message-actions {
  margin-top: 6px;
  display: flex;
  justify-content: flex-end;
  gap: 6px;
}

.input-box {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}

.topk-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.topk-label {
  font-size: 12px;
  color: #909399;
}
</style>
