<template>
  <div class="chat-input-bar">
    <div class="input-container">
      <n-popover trigger="click" placement="top-start" :width="220">
        <template #trigger>
          <n-button quaternary class="input-icon-btn">{{ t('analytics.knowledge.chat_topk_label') }}</n-button>
        </template>
        <div class="topk-popover">
          <span class="topk-label">{{ t('analytics.knowledge.chat_topk_label') }}</span>
          <n-input-number v-model:value="topK" :min="1" :max="10" size="small" />
        </div>
      </n-popover>

      <n-input
        v-model:value="draft"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 5 }"
        :placeholder="t('analytics.knowledge.chat_input_placeholder')"
        class="chat-textarea"
        @keydown.enter.exact.prevent="submit"
      />

      <n-button v-if="!loading" type="primary" :disabled="!draft.trim()" class="send-btn" @click="submit">{{ t('analytics.knowledge.search') }}</n-button>
      <n-button v-else class="stop-btn" @click="emit('stop')">Stop</n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NPopover, NInputNumber, NInput, NButton } from 'naive-ui'

defineProps<{
  loading: boolean
}>()

const emit = defineEmits<{
  send: [{ question: string; topK: number }]
  stop: []
}>()

const { t } = useI18n()
const draft = ref('')
const topK = ref(5)

const submit = () => {
  const question = draft.value.trim()
  if (!question) return
  emit('send', { question, topK: topK.value })
  draft.value = ''
}
</script>

<style scoped>
.chat-input-bar {
  padding: 12px 24px 20px;
  background: var(--chat-main-bg, #fafafc);
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 768px;
  margin: 0 auto;
  background: #ffffff;
  border: 1px solid var(--chat-border-color, #e0e0e6);
  border-radius: var(--chat-border-radius, 3px);
  padding: 6px 10px;
}

.input-icon-btn {
  flex-shrink: 0;
}

.chat-textarea {
  flex: 1;
  min-width: 0;
}

.send-btn,
.stop-btn {
  flex-shrink: 0;
}

.topk-popover {
  display: flex;
  align-items: center;
  gap: 8px;
}

.topk-label {
  font-size: 13px;
  color: var(--chat-text-secondary, #666e7a);
  white-space: nowrap;
}
</style>
