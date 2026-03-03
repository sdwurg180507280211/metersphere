<template>
  <div class="user-message">
    <div class="message-bubble">
      <div class="message-content">{{ message.content }}</div>
    </div>
    <div class="user-avatar">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChatMessage } from '@/composables/useKnowledgeChat'

defineProps<{
  message: ChatMessage
}>()
</script>

<style scoped>
.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
}

.message-bubble {
  background: var(--chat-user-bubble-bg, #f0f0f0);
  color: var(--chat-user-bubble-text, #303133);
  border-radius: 18px 18px 4px 18px;
  padding: 12px 16px;
  max-width: 70%;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--chat-accent, #6366f1);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>
