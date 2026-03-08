<template>
  <div ref="conversationRef" class="chat-conversation">
    <VueMarkdownItProvider>
      <div
        v-for="(message, index) in messages"
        :key="message.id"
        class="message-row"
      >
        <ChatMessageUser 
          v-if="message.role === 'user'" 
          :message="message"
          @edit="(content) => emit('edit', content)"
        />
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
    </VueMarkdownItProvider>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { VueMarkdownItProvider } from 'vue-markdown-shiki'
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
  edit: [content: string]
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
  height: 100%;
  overflow-y: auto;
  padding: 0 24px;
  background: white;
}

.chat-conversation > .message-row {
  max-width: 900px;
  margin: 0 auto;
}

/* Scrollbar */
.chat-conversation::-webkit-scrollbar {
  width: 8px;
}

.chat-conversation::-webkit-scrollbar-track {
  background: transparent;
}

.chat-conversation::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.chat-conversation::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .chat-conversation {
    padding: 0 16px;
  }
}
</style>
