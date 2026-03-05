<template>
  <div class="chat-input-bar">
    <div class="input-container">
      <!-- Settings button (mode + topK) -->
      <n-popover trigger="click" placement="top-start">
        <template #trigger>
          <button class="input-icon-btn" :title="t('analytics.knowledge.chat_mode_settings')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
          </button>
        </template>
        <div class="settings-popover">
          <!-- Chat mode selection -->
          <div class="setting-item">
            <span class="setting-label">{{ t('analytics.knowledge.chat_mode_label') }}</span>
            <n-select
              v-model:value="localChatMode"
              size="small"
              :options="chatModeOptions"
              style="width: 160px"
              @update:value="handleModeChange"
            />
          </div>

          <!-- TopK setting (only for knowledge mode) -->
          <div v-if="localChatMode === 'knowledge'" class="setting-item">
            <span class="setting-label">{{ t('analytics.knowledge.chat_topk_label') }}</span>
            <n-input-number v-model:value="topK" :min="1" :max="10" size="small" style="width: 100px" />
            <span class="setting-hint">{{ t('analytics.knowledge.chat_topk_hint') }}</span>
          </div>
        </div>
      </n-popover>

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
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NInput, NInputNumber, NPopover, NSelect } from 'naive-ui'

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

const chatModeOptions = computed(() => [
  { label: t('analytics.knowledge.chat_mode_knowledge'), value: 'knowledge' },
  { label: t('analytics.knowledge.chat_mode_normal'), value: 'normal' },
])

const handleModeChange = (value: 'knowledge' | 'normal') => {
  emit('update:chatMode', value)
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
  background: #ffffff;
  border: 1px solid var(--chat-border-color, #e5e5e5);
  border-radius: 24px;
  padding: 8px 12px;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.06);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-container:focus-within {
  border-color: var(--chat-accent, #6366f1);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

.input-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  background: none;
  border-radius: 50%;
  cursor: pointer;
  color: #8e8ea0;
  flex-shrink: 0;
  padding: 0;
}

.input-icon-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #303133;
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
  width: 34px;
  height: 34px;
  border: none;
  background: var(--chat-accent, #6366f1);
  color: white;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
  transition: background-color 0.15s;
}

.send-btn:hover:not(.disabled) {
  background: var(--chat-accent-hover, #4f46e5);
}

.send-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
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

.settings-popover {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 240px;
}

.setting-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-label {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.setting-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.topk-popover {
  display: flex;
  align-items: center;
  gap: 8px;
}

.topk-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
</style>
