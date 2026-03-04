<template>
  <div ref="conversationRef" class="chat-conversation">
    <div
      v-for="(message, index) in messages"
      :key="message.id"
      class="message-row"
    >
      <ChatMessageUser v-if="message.role === 'user'" :message="message" />
      <ChatMessageBot
        v-else
        :message="message"
        :is-last="index === messages.length - 1"
        :is-loading="loading && index === messages.length - 1"
        @copy="copyMessage(message.content)"
        @retry="retryFromMessage(index)"
        @feedback="(payload) => emit('feedback', { messageId: message.id, ...payload })"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import ChatMessageUser from './ChatMessageUser.vue'
import ChatMessageBot from './ChatMessageBot.vue'
import type { ChatMessage } from '@/composables/useKnowledgeChat'

const props = defineProps<{
  messages: ChatMessage[]
  loading: boolean
}>()

const emit = defineEmits<{
  retry: [question: string]
  feedback: [{ messageId: string; rating: 'up' | 'down'; reason?: string }]
}>()

const { t } = useI18n()
const message = useMessage()
const conversationRef = ref<HTMLElement>()

const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    message.success(t('analytics.knowledge.chat_copied'))
  } catch {
    message.warning(t('analytics.knowledge.chat_copy_failed'))
  }
}

const retryFromMessage = (assistantIndex: number) => {
  for (let i = assistantIndex - 1; i >= 0; i -= 1) {
    const message = props.messages[i]
    if (message.role === 'user') {
      emit('retry', message.content)
      return
    }
  }
}

watch(
  () => props.messages.length,
  async () => {
    await nextTick()
    if (!conversationRef.value) return
    conversationRef.value.scrollTop = conversationRef.value.scrollHeight
  },
)
</script>

<style scoped>
.chat-conversation {
  flex: 1;
  overflow-y: auto;
  padding: 24px 24px 8px;
}

.chat-conversation > .message-row {
  max-width: 768px;
  margin: 0 auto 16px;
}
</style>
