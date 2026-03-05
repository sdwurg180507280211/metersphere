<template>
  <div class="bot-message">
    <div class="bot-avatar">
      <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
        <rect x="4" y="4" width="6" height="6" rx="1" />
        <rect x="14" y="4" width="6" height="6" rx="1" />
        <rect x="4" y="14" width="6" height="6" rx="1" />
        <rect x="14" y="14" width="6" height="6" rx="1" />
      </svg>
    </div>
    <div class="message-wrapper">
      <div class="message-header">
        <span class="bot-name">{{ t('analytics.knowledge.chat_assistant') }}</span>
        <span class="timestamp">{{ formatTime(message.timestamp) }}</span>
      </div>
      <div v-if="message.content" class="message-bubble">
        <VueMarkdownIt :content="message.content" />
      </div>
      <div v-if="isLoading && !message.content" class="loading-text">
        <span class="typing-dot"></span>
        <span class="typing-dot"></span>
        <span class="typing-dot"></span>
      </div>
      <div v-if="message.content" class="message-actions">
        <button class="action-btn" @click="emit('copy')" title="Copy">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <rect x="9" y="9" width="13" height="13" rx="2" />
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
          </svg>
        </button>
        <button class="action-btn" @click="emit('retry')" title="Retry">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
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

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true })
}
</script>

<style scoped>
.bot-message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 24px;
}

.bot-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #eef2ff;
  color: #4f46e5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 24px;
}

.bot-name {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.timestamp {
  font-size: 14px;
  font-weight: 500;
  color: #94a3b8;
}

.message-bubble {
  background: #f8fafc;
  color: #475569;
  border-radius: 24px;
  padding: 12px;
  font-size: 16px;
  line-height: 1.6;
}

.loading-text {
  display: flex;
  gap: 4px;
  padding: 12px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) { animation-delay: -0.32s; }
.typing-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.message-actions {
  display: flex;
  gap: 16px;
  margin-top: 4px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}

.action-btn:hover {
  color: #475569;
}
</style>
