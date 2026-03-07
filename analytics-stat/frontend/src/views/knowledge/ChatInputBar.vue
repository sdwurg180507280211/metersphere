<template>
  <div class="chat-input-bar">
    <div class="input-container">
      <n-input
        ref="inputRef"
        v-model:value="draft"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 5 }"
        :placeholder="t('analytics.knowledge.chat_input_placeholder')"
        class="chat-textarea"
        @keydown.enter.exact.prevent="submit"
      />

      <button
        v-if="!props.loading"
        class="send-btn"
        :class="{ disabled: !draft.trim() || props.disabled }"
        :disabled="!draft.trim() || props.disabled"
        @click="submit"
      >
        <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
          <path d="M5 12h14M12 5l7 7-7 7" stroke="currentColor" stroke-width="2" fill="none" />
        </svg>
      </button>
      <button v-else class="stop-btn" @click="emit('stop')">
        <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12">
          <rect x="6" y="6" width="12" height="12" rx="2" />
        </svg>
      </button>
    </div>
    <p class="disclaimer">{{ t('analytics.knowledge.chat_assistant') }} can make mistakes. Check our Terms & Conditions.</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NInput } from 'naive-ui'

const props = defineProps<{
  loading: boolean
  chatMode: 'knowledge' | 'normal'
  disabled?: boolean
}>()

const emit = defineEmits<{
  send: [{ question: string; topK: number }]
  stop: []
  'update:chatMode': ['knowledge' | 'normal']
}>()

const { t } = useI18n()
const inputRef = ref<InstanceType<typeof NInput> | null>(null)
const draft = ref('')
const topK = ref(5)

const submit = () => {
  const question = draft.value.trim()
  if (!question || props.loading || props.disabled) return
  emit('send', { question, topK: topK.value })
  draft.value = ''
}

const focus = () => {
  inputRef.value?.focus?.()
}

const restoreDraft = (value: string) => {
  draft.value = value
  focus()
}

defineExpose({ focus, restoreDraft })
</script>

<style scoped>
.chat-input-bar {
  padding: 24px 96px;
  background: white;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 100%;
  width: 100%;
  background: white;
  border: 1px solid #cbd5e1;
  border-radius: 24px;
  padding: 12px 14px;
  box-shadow: 0 4px 8px -2px rgba(23, 23, 23, 0.1), 0 2px 4px -2px rgba(23, 23, 23, 0.06);
}

.input-container:focus-within {
  border-color: #4f46e5;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.15);
}

.chat-textarea {
  flex: 1;
  min-width: 0;
}

.chat-textarea :deep(.n-input__textarea-el) {
  border: none !important;
  box-shadow: none !important;
  padding: 8px 4px;
  font-size: 16px;
  line-height: 1.375;
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
  width: 40px;
  height: 40px;
  border: none;
  background: #4f46e5;
  color: white;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
  transition: all 0.2s;
}

.send-btn:hover:not(.disabled) {
  background: #4338ca;
}

.send-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.stop-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid #cbd5e1;
  background: white;
  color: #1e293b;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
}

.stop-btn:hover {
  background: #f8fafc;
}

.disclaimer {
  font-size: 14px;
  font-weight: 500;
  color: #94a3b8;
  text-align: center;
  margin: 0;
}
</style>
