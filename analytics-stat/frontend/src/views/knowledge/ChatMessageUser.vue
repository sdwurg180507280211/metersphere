<template>
  <div class="user-message">
    <div class="message-content">
      <div class="message-header">
        <span class="user-name">{{ t('analytics.knowledge.chat_user') }}</span>
        <span class="timestamp">{{ formatTime(message.timestamp) }}</span>
      </div>
      <div class="message-bubble">
        <div class="message-text">{{ message.content }}</div>
      </div>
      <div class="message-actions">
        <button class="action-btn" @click="handleEdit" :title="t('commons.edit')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>
      </div>
    </div>
    <div class="user-avatar">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { ChatMessage } from '@/composables/useKnowledgeChat'

const props = defineProps<{
  message: ChatMessage
}>()

const emit = defineEmits<{
  edit: [content: string]
}>()

const { t } = useI18n()

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const handleEdit = () => {
  emit('edit', props.message.content)
}
</script>

<style scoped>
.user-message {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 20px 0;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  max-width: 80%;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 4px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.timestamp {
  font-size: 12px;
  color: #94a3b8;
}

.message-bubble {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border-radius: 16px;
  border-bottom-right-radius: 4px;
  padding: 14px 18px;
  font-size: 15px;
  line-height: 1.6;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25);
  position: relative;
  word-break: break-word;
}

.message-text {
  white-space: pre-wrap;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(245, 87, 108, 0.3);
}

/* Message Actions */
.message-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.user-message:hover .message-actions {
  opacity: 1;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 6px;
  padding: 0;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f1f5f9;
  color: #64748b;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .user-message {
    gap: 12px;
    padding: 16px 0;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .user-avatar {
    width: 32px;
    height: 32px;
  }
  
  .message-bubble {
    padding: 12px 16px;
    font-size: 14px;
  }
  
  .message-actions {
    opacity: 1;
  }
}
</style>
