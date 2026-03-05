<template>
  <div class="chat-input-bar">
    <div class="input-container">
      <!-- Mode toggle button -->
      <button
        class="mode-toggle-btn"
        :class="{ 'knowledge-mode': localChatMode === 'knowledge' }"
        :title="localChatMode === 'knowledge' ? t('analytics.knowledge.chat_mode_knowledge') : t('analytics.knowledge.chat_mode_normal')"
        @click="toggleMode"
      >
        <svg v-if="localChatMode === 'knowledge'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
      </button>

      <!-- Text input -->
      <n-input
        ref="inputRef"
        v-model:value="draft"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 5 }"
        :placeholder="t('analytics.knowledge.chat_input_placeholder')"
        class="chat-textarea"
        @keydown.enter.exact.prevent="submit"
      />

      <!-- Send / Stop button -->
      <button
        v-if="!loading"
        class="send-btn"
        :class="{ disabled: !draft.trim() }"
        :disabled="!draft.trim()"
        @click="submit"
      >
        <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
          <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
        </svg>
      </button>
      <button v-else class="stop-btn" @click="emit('stop')">
        <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
          <rect x="6" y="6" width="12" height="12" rx="2" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NInput } from 'naive-ui'

const props = defineProps<{
  loading: boolean
  chatMode: 'knowledge' | 'normal'
}>()

const emit = defineEmits<{
  send: [{ question: string; topK: number }]
  stop: []
  'update:chatMode': ['knowledge' | 'normal']
}>()

const { t } = useI18n()
const draft = ref('')
const topK = ref(5)
const localChatMode = ref(props.chatMode)

// 同步 props.chatMode 到 localChatMode
watch(() => props.chatMode, (newMode) => {
  localChatMode.value = newMode
})

const toggleMode = () => {
  const newMode = localChatMode.value === 'knowledge' ? 'normal' : 'knowledge'
  localChatMode.value = newMode
  emit('update:chatMode', newMode)
}

const submit = () => {
  const question = draft.value.trim()
  if (!question) return
  emit('send', { question, topK: topK.value })
  draft.value = ''
}

const focus = () => {
  // exposed for parent to call
}

defineExpose({ focus })
</script>

<style scoped>
.chat-input-bar {
  padding: 16px 24px 24px;
  background: var(--chat-main-bg, #f7f7f8);
  flex-shrink: 0;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 768px;
  margin: 0 auto;
  background: linear-gradient(to bottom, #ffffff, #fafafa);
  border: 1.5px solid #e0e0e0;
  border-radius: 28px;
  padding: 10px 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.input-container:focus-within {
  border-color: #667eea;
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.15), 0 0 0 3px rgba(102, 126, 234, 0.08);
  background: #ffffff;
  transform: translateY(-1px);
}

.mode-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: #f3f4f6;
  border-radius: 50%;
  cursor: pointer;
  color: #6b7280;
  flex-shrink: 0;
  padding: 0;
  transition: all 0.2s ease;
}

.mode-toggle-btn:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15), rgba(118, 75, 162, 0.15));
  color: #667eea;
  transform: scale(1.05);
}

.mode-toggle-btn.knowledge-mode {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.mode-toggle-btn.knowledge-mode:hover {
  background: linear-gradient(135deg, #5568d3 0%, #6a3f8f 100%);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.input-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  border-radius: 50%;
  cursor: pointer;
  color: #9ca3af;
  flex-shrink: 0;
  padding: 0;
  transition: all 0.2s ease;
}

.input-icon-btn:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  color: #667eea;
  transform: rotate(90deg);
}

.chat-textarea {
  flex: 1;
  min-width: 0;
}

.chat-textarea :deep(.n-input__textarea-el) {
  border: none !important;
  box-shadow: none !important;
  padding: 6px 4px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  background: transparent;
}

.chat-textarea :deep(.n-input__border),
.chat-textarea :deep(.n-input__state-border) {
  display: none !important;
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.send-btn:hover:not(.disabled) {
  background: linear-gradient(135deg, #5568d3 0%, #6a3f8f 100%);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
}

.stop-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--chat-border-color, #e5e5e5);
  background: #fff;
  color: #303133;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
}

.stop-btn:hover {
  background: #f5f5f5;
}
</style>
