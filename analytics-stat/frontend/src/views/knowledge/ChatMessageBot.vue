<template>
  <div class="bot-message">
    <div class="bot-avatar">
      <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22">
        <rect x="4" y="4" width="6" height="6" rx="1.5" />
        <rect x="14" y="4" width="6" height="6" rx="1.5" />
        <rect x="4" y="14" width="6" height="6" rx="1.5" />
        <rect x="14" y="14" width="6" height="6" rx="1.5" />
      </svg>
    </div>
    <div class="message-wrapper">
      <div class="message-header">
        <div class="header-left">
          <span class="bot-name">{{ t('analytics.knowledge.chat_assistant') }}</span>
          <span class="model-tag" v-if="message.model">{{ message.model }}</span>
        </div>
        <span class="timestamp">{{ formattedTime }}</span>
      </div>
      
      <!-- Loading State -->
      <div v-if="isLoading && !message.content" class="loading-bubble">
        <span class="typing-dot"></span>
        <span class="typing-dot"></span>
        <span class="typing-dot"></span>
      </div>
      
      <!-- Content -->
      <div v-else class="message-bubble">
        <VueMarkdownIt 
          :content="message.content" 
          class="markdown-content"
        />
        
        <!-- Sources -->
        <div v-if="message.sources?.length" class="sources-section">
          <div class="sources-divider"></div>
          <div class="sources-header" @click="showSources = !showSources">
            <svg class="sources-icon" :class="{ expanded: showSources }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <polyline points="9 18 15 12 9 6" />
            </svg>
            <span>{{ t('analytics.knowledge.sources') }} ({{ message.sources.length }})</span>
          </div>
          <div v-show="showSources" class="sources-list">
            <a 
              v-for="(source, idx) in message.sources" 
              :key="idx"
              :href="source.url" 
              target="_blank" 
              class="source-item"
            >
              <span class="source-number">{{ idx + 1 }}</span>
              <span class="source-title">{{ source.title || source.url }}</span>
            </a>
          </div>
        </div>
      </div>
      
      <!-- Actions -->
      <div v-if="message.content" class="message-actions">
        <div class="action-group">
          <button 
            class="action-btn" 
            :class="{ active: feedbackRating === 'up' }"
            @click="handleFeedback('up')" 
            :title="t('analytics.knowledge.feedback_helpful')"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3" />
            </svg>
          </button>
          <button 
            class="action-btn" 
            :class="{ active: feedbackRating === 'down' }"
            @click="handleFeedback('down')" 
            :title="t('analytics.knowledge.feedback_not_helpful')"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M10 15v4a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3zm7-13h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-3" />
            </svg>
          </button>
        </div>
        
        <div class="action-divider"></div>
        
        <div class="action-group">
          <button class="action-btn" @click="emit('copy')" :title="t('commons.copy')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <rect x="9" y="9" width="13" height="13" rx="2" />
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
            </svg>
          </button>
          <button class="action-btn" @click="emit('retry')" :title="t('commons.retry')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <polyline points="23 4 23 10 17 10" />
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
            </svg>
          </button>
        </div>
      </div>
      
      <!-- Feedback Form -->
      <div v-if="showFeedbackForm" class="feedback-form">
        <textarea 
          v-model="feedbackReason"
          :placeholder="t('analytics.knowledge.feedback_placeholder')"
          class="feedback-textarea"
          rows="2"
        ></textarea>
        <div class="feedback-actions">
          <button class="feedback-btn secondary" @click="cancelFeedback">
            {{ t('commons.cancel') }}
          </button>
          <button class="feedback-btn primary" @click="submitFeedback">
            {{ t('commons.submit') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
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
const showSources = ref(false)
const showFeedbackForm = ref(false)
const feedbackRating = ref<'up' | 'down' | null>(props.message.feedback?.rating || null)
const feedbackReason = ref(props.message.feedback?.reason || '')

const formattedTime = computed(() => {
  const date = new Date(props.message.timestamp)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()

  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
})

const handleFeedback = (rating: 'up' | 'down') => {
  if (feedbackRating.value === rating) {
    // Toggle off
    feedbackRating.value = null
    showFeedbackForm.value = false
    emit('feedback', { rating })
    return
  }
  
  feedbackRating.value = rating
  if (rating === 'down') {
    showFeedbackForm.value = true
  } else {
    showFeedbackForm.value = false
    emit('feedback', { rating })
  }
}

const submitFeedback = () => {
  if (feedbackRating.value) {
    emit('feedback', { 
      rating: feedbackRating.value, 
      reason: feedbackReason.value 
    })
  }
  showFeedbackForm.value = false
}

const cancelFeedback = () => {
  feedbackRating.value = null
  feedbackReason.value = ''
  showFeedbackForm.value = false
}
</script>

<style scoped>
.bot-message {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 20px 0;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.bot-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.message-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bot-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.model-tag {
  font-size: 11px;
  font-weight: 500;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  padding: 2px 8px;
  border-radius: 12px;
}

.timestamp {
  font-size: 12px;
  color: #94a3b8;
}

/* Loading State */
.loading-bubble {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 16px 20px;
  background: #f8fafc;
  border-radius: 16px;
  width: fit-content;
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  animation: typingBounce 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) { animation-delay: -0.32s; }
.typing-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes typingBounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* Message Bubble */
.message-bubble {
  background: #f8fafc;
  color: #334155;
  border-radius: 16px;
  padding: 16px 20px;
  font-size: 15px;
  line-height: 1.7;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.markdown-content :deep(p) {
  margin: 0 0 12px;
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(pre) {
  background: #1e293b;
  border-radius: 12px;
  padding: 16px;
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-content :deep(code) {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.markdown-content :deep(pre code) {
  color: #e2e8f0;
  background: transparent;
  padding: 0;
}

.markdown-content :deep(:not(pre) > code) {
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.markdown-content :deep(ul), 
.markdown-content :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin: 6px 0;
}

.markdown-content :deep(blockquote) {
  border-left: 3px solid #6366f1;
  margin: 12px 0;
  padding: 8px 16px;
  background: rgba(99, 102, 241, 0.05);
  border-radius: 0 8px 8px 0;
  color: #475569;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 14px;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  text-align: left;
}

.markdown-content :deep(th) {
  background: #f1f5f9;
  font-weight: 600;
  color: #1e293b;
}

.markdown-content :deep(tr:nth-child(even)) {
  background: #f8fafc;
}

/* Sources Section */
.sources-section {
  margin-top: 16px;
}

.sources-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #e2e8f0, transparent);
  margin: 12px 0;
}

.sources-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  padding: 4px 0;
  user-select: none;
  transition: color 0.2s;
}

.sources-header:hover {
  color: #6366f1;
}

.sources-icon {
  transition: transform 0.2s;
}

.sources-icon.expanded {
  transform: rotate(90deg);
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: white;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s;
  border: 1px solid #e2e8f0;
}

.source-item:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
}

.source-number {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #6366f1;
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 6px;
  flex-shrink: 0;
}

.source-title {
  font-size: 13px;
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Message Actions */
.message-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.bot-message:hover .message-actions {
  opacity: 1;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-divider {
  width: 1px;
  height: 16px;
  background: #e2e8f0;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 8px;
  padding: 0;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f1f5f9;
  color: #64748b;
}

.action-btn.active {
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
}

.action-btn.active:hover {
  background: rgba(99, 102, 241, 0.15);
}

/* Feedback Form */
.feedback-form {
  background: #f8fafc;
  border-radius: 12px;
  padding: 12px;
  margin-top: 8px;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.feedback-textarea {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  resize: vertical;
  min-height: 60px;
  font-family: inherit;
  transition: border-color 0.2s;
}

.feedback-textarea:focus {
  outline: none;
  border-color: #6366f1;
}

.feedback-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.feedback-btn {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.feedback-btn.secondary {
  background: transparent;
  color: #64748b;
}

.feedback-btn.secondary:hover {
  background: #e2e8f0;
}

.feedback-btn.primary {
  background: #6366f1;
  color: white;
}

.feedback-btn.primary:hover {
  background: #4f46e5;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .bot-message {
    gap: 12px;
    padding: 16px 0;
  }
  
  .bot-avatar {
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
