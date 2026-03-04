<template>
  <div class="bot-message">
    <!-- Avatar -->
    <div class="bot-avatar">
      <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z" />
      </svg>
    </div>

    <div class="message-body">
      <!-- Brand badge -->
      <div class="bot-badge">{{ t('analytics.knowledge.chat_assistant') }}</div>

      <!-- Content -->
      <div class="message-content">{{ message.content }}</div>

      <!-- Sources -->
      <SourceList v-if="message.sources?.length" :sources="message.sources" />

      <!-- Action bar -->
      <div class="message-action-bar" v-if="!isLoading">
        <button
          class="action-btn"
          :class="{ active: message.feedback?.rating === 'up' }"
          @click="emitFeedback('up')"
          :title="t('analytics.knowledge.feedback_up')"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3" />
          </svg>
        </button>
        <div class="feedback-down-wrapper">
          <button
            class="action-btn"
            :class="{ active: message.feedback?.rating === 'down' }"
            @click="showDownReasons = !showDownReasons"
            :title="t('analytics.knowledge.feedback_down')"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <path d="M10 15v4a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3zm7-13h2.67A2.31 2.31 0 0 1 22 4v7a2.31 2.31 0 0 1-2.33 2H17" />
            </svg>
          </button>
          <div v-if="showDownReasons" class="down-reasons-dropdown">
            <button
              v-for="reason in downReasons"
              :key="reason"
              class="reason-btn"
              @click="handleDownReason(reason)"
            >
              {{ t(reason) }}
            </button>
          </div>
        </div>
        <button class="action-btn" @click="emit('copy')" :title="t('analytics.knowledge.chat_copy')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
          </svg>
        </button>
        <button v-if="isLast" class="action-btn regenerate-btn" @click="emit('retry')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <polyline points="23 4 23 10 17 10" />
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
          </svg>
          <span>{{ t('analytics.knowledge.chat_regenerate') }}</span>
        </button>
      </div>

      <!-- Loading indicator -->
      <div v-if="isLoading" class="typing-indicator">
        <span></span><span></span><span></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import SourceList from './SourceList.vue'
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
const message = useMessage()
const showDownReasons = ref(false)

const downReasons = [
  'analytics.knowledge.feedback_reason_inaccurate',
  'analytics.knowledge.feedback_reason_irrelevant',
  'analytics.knowledge.feedback_reason_incomplete',
]

const emitFeedback = (rating: 'up' | 'down', reason?: string) => {
  emit('feedback', { rating, reason })
  message.success(t('analytics.knowledge.feedback_submitted'))
}

const handleDownReason = (reason: string) => {
  showDownReasons.value = false
  emitFeedback('down', reason)
}
</script>

<style scoped>
.bot-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
}

.bot-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #10a37f;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-body {
  max-width: 75%;
  min-width: 0;
}

.bot-badge {
  font-size: 12px;
  font-weight: 600;
  color: #8e8ea0;
  margin-bottom: 4px;
}

.message-content {
  background: var(--chat-bot-bubble-bg, #ffffff);
  border-radius: 4px 18px 18px 18px;
  padding: 12px 16px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  color: #303133;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-action-bar {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.bot-message:hover .message-action-bar {
  opacity: 1;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  border: none;
  background: none;
  border-radius: 4px;
  cursor: pointer;
  color: #8e8ea0;
  font-size: 12px;
}

.action-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #303133;
}

.action-btn.active {
  color: var(--chat-accent, #6366f1);
}

.regenerate-btn span {
  margin-left: 2px;
}

.feedback-down-wrapper {
  position: relative;
}

.down-reasons-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  background: #fff;
  border: 1px solid var(--chat-border-color, #e5e5e5);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 10;
  padding: 4px;
  min-width: 140px;
}

.reason-btn {
  display: block;
  width: 100%;
  text-align: left;
  padding: 6px 10px;
  border: none;
  background: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
}

.reason-btn:hover {
  background: var(--chat-session-active-bg, #ececf1);
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #8e8ea0;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
