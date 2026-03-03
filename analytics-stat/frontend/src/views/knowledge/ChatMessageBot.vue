<template>
  <div class="bot-message">
    <div class="bot-avatar">AI</div>

    <div class="message-body">
      <div class="bot-badge">{{ t('analytics.knowledge.chat_assistant') }}</div>
      <div class="message-content">{{ message.content }}</div>
      <SourceList v-if="message.sources?.length" :sources="message.sources" />

      <div class="message-action-bar" v-if="!isLoading">
        <n-button text size="tiny" :type="message.feedback?.rating === 'up' ? 'success' : 'default'" @click="emitFeedback('up')">
          {{ t('analytics.knowledge.feedback_up') }}
        </n-button>
        <n-popover trigger="click" placement="bottom-start">
          <template #trigger>
            <n-button text size="tiny" :type="message.feedback?.rating === 'down' ? 'error' : 'default'">
              {{ t('analytics.knowledge.feedback_down') }}
            </n-button>
          </template>
          <div class="down-reasons-dropdown">
            <n-button v-for="reason in downReasons" :key="reason" text size="small" class="reason-btn" @click="handleDownReason(reason)">
              {{ t(reason) }}
            </n-button>
          </div>
        </n-popover>
        <n-button text size="tiny" @click="emit('copy')">{{ t('analytics.knowledge.chat_copy') }}</n-button>
        <n-button v-if="isLast" text size="tiny" @click="emit('retry')">{{ t('analytics.knowledge.chat_regenerate') }}</n-button>
      </div>

      <div v-if="isLoading" class="typing-indicator">
        <span></span><span></span><span></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { NButton, NPopover, useMessage } from 'naive-ui'
import SourceList from './SourceList.vue'
import type { ChatMessage } from '@/composables/useKnowledgeChat'

defineProps<{
  message: ChatMessage
  isLast: boolean
  isLoading: boolean
}>()

const emit = defineEmits<{
  copy: []
  retry: []
  feedback: [{ rating: 'up' | 'down'; reason?: string }]
}>()

const { t } = useI18n()
const messageApi = useMessage()

const downReasons = [
  'analytics.knowledge.feedback_reason_inaccurate',
  'analytics.knowledge.feedback_reason_irrelevant',
  'analytics.knowledge.feedback_reason_incomplete',
]

const emitFeedback = (rating: 'up' | 'down', reason?: string) => {
  emit('feedback', { rating, reason })
  messageApi.success(t('analytics.knowledge.feedback_submitted'))
}

const handleDownReason = (reason: string) => {
  emitFeedback('down', reason)
}
</script>

<style scoped>
.bot-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 6px 0;
}

.bot-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--chat-border-radius, 3px);
  background: var(--chat-accent, #18a058);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
}

.message-body {
  max-width: 75%;
  min-width: 0;
}

.bot-badge {
  font-size: 12px;
  font-weight: 500;
  color: var(--chat-text-tertiary, #999);
  margin-bottom: 4px;
}

.message-content {
  background: var(--chat-bot-bubble-bg, #ffffff);
  border-radius: 2px 6px 6px 6px;
  padding: 10px 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  color: var(--chat-text-primary, #333639);
  border: 1px solid var(--chat-border-color, #e0e0e6);
}

.message-action-bar {
  display: flex;
  gap: 4px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.down-reasons-dropdown {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.reason-btn {
  justify-content: flex-start;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 10px 0;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--chat-accent, #18a058);
  animation: typing-bounce 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}
</style>
