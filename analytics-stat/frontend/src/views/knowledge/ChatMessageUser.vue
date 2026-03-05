<template>
  <div class="user-message">
    <div class="user-avatar">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
    </div>
    <div class="message-wrapper">
      <div class="message-header">
        <span class="user-name">You</span>
        <span class="timestamp">{{ formatTime(message.timestamp) }}</span>
      </div>
      <div class="message-bubble">{{ message.content }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChatMessage } from '@/composables/useKnowledgeChat'

defineProps<{
  message: ChatMessage
}>()

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true })
}
</script>

<style scoped>
.user-message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--chat-accent, #6366f1);
  color: white;
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

.user-name {
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
  white-space: pre-wrap;
  word-break: break-word;
  display: inline-block;
  max-width: fit-content;
}
</style>
