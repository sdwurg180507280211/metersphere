import { ref, type Ref } from 'vue'
import { askQuestionStream } from '@/api/knowledge-chat'
import type { ChatSource } from '@/api/knowledge-chat'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: number
  sources?: ChatSource[]
}

interface SendQuestionOptions {
  topK?: number
}

interface UseKnowledgeChatOptions {
  messages?: Ref<ChatMessage[]>
}

export function useKnowledgeChat(options: UseKnowledgeChatOptions = {}) {
  const messages = options.messages ?? ref<ChatMessage[]>([])
  const loading = ref(false)
  let currentAbortController: AbortController | null = null

  const sendQuestion = async (question: string, options: SendQuestionOptions = {}) => {
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

    const assistantMessage: ChatMessage = {
      id: `a-${Date.now()}`,
      role: 'assistant',
      content: '',
      createdAt: Date.now(),
      sources: [],
    }
    messages.value.push(assistantMessage)

    currentAbortController?.abort()
    currentAbortController = new AbortController()
    loading.value = true

    try {
      await askQuestionStream(
        { question: normalized, topK: options.topK },
        {
          signal: currentAbortController.signal,
          onChunk: (chunk) => {
            assistantMessage.content += chunk
          },
          onSources: (sources) => {
            assistantMessage.sources = sources
          },
        },
      )
    } catch (error) {
      if ((error as Error)?.name === 'AbortError') {
        return
      }
      messages.value = messages.value.filter((item) => item.id !== assistantMessage.id)
      throw error
    } finally {
      if (currentAbortController?.signal.aborted) {
        currentAbortController = null
      } else {
        currentAbortController = null
      }
      loading.value = false
    }
  }

  const stopGenerating = () => {
    if (loading.value) {
      currentAbortController?.abort()
      loading.value = false
      currentAbortController = null
      const lastMessage = messages.value[messages.value.length - 1]
      if (lastMessage && lastMessage.role === 'assistant' && !lastMessage.content.trim()) {
        messages.value = messages.value.slice(0, -1)
      }
    }
  }

  const clearMessages = () => {
    stopGenerating()
    messages.value = []
  }

  return {
    messages,
    loading,
    sendQuestion,
    clearMessages,
    stopGenerating,
  }
}
