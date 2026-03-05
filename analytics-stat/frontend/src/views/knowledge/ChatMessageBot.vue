<template>
  <div class="bot-message">
    <div class="message-header">
      <n-avatar class="bot-avatar">
        <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z" />
        </svg>
      </n-avatar>
      <div class="header-info">
        <n-text class="bot-name">{{ t('analytics.knowledge.chat_assistant') }}</n-text>
        <n-text class="timestamp">{{ formatTimestamp(message.timestamp) }}</n-text>
      </div>
    </div>

    <div v-if="message.content" class="message-content">
      <VueMarkdownIt :content="message.content" />
    </div>

    <n-text v-if="isLoading && !message.content" class="loading-text">
      <span class="typing-dot"></span>
      <span class="typing-dot"></span>
      <span class="typing-dot"></span>
    </n-text>

    <n-divider class="message-divider" />

    <div class="message-actions">
      <n-button quaternary @click="emit('copy')">
        <template #icon>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
          </svg>
        </template>
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { NAvatar, NText, NButton, NDivider } from 'naive-ui'
import { VueMarkdownIt } from 'vue-markdown-shiki'
import type { ChatMessage } from '@/composables/useKnowledgeChat'

const props = defineProps<{
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

const formatTimestamp = (timestamp: number) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`

  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.bot-message {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 32px;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.bot-avatar {
  width: 36px;
  height: 36px;
  background: var(--chat-accent, #6366f1);
  flex-shrink: 0;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bot-name {
  font-size: 16px;
  font-weight: 600;
}

.timestamp {
  font-size: 12px;
  color: #909399;
}

.loading-text {
  margin-left: 52px;
  margin-top: 8px;
  display: flex;
  gap: 4px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) { animation-delay: -0.32s; }
.typing-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.message-content {
  margin-left: 52px;
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
}

.message-divider {
  margin-left: 52px;
  width: calc(100% - 52px);
  margin-top: 8px;
  margin-bottom: 0;
}

.message-actions {
  margin-left: 52px;
  display: flex;
  gap: 16px;
}
</style>
