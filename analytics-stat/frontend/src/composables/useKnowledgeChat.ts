import { ref } from 'vue'
import { askQuestion } from '@/api/knowledge-chat'
import type { ChatSource } from '@/api/knowledge-chat'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: number
  sources?: ChatSource[]
}

export function useKnowledgeChat() {
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)

  const sendQuestion = async (question: string) => {
    const normalized = question.trim()
    if (!normalized) {
      return
    }

    const now = Date.now()
    messages.value.push({
      id: `u-${now}`,
      role: 'user',
      content: normalized,
      createdAt: now,
    })

    loading.value = true
    try {
      const response = await askQuestion({ question: normalized })
      messages.value.push({
        id: `a-${Date.now()}`,
        role: 'assistant',
        content: response.answer,
        createdAt: Date.now(),
        sources: response.sources,
      })
    } finally {
      loading.value = false
    }
  }

  const clearMessages = () => {
    messages.value = []
  }

  return {
    messages,
    loading,
    sendQuestion,
    clearMessages,
  }
}
