import { ref, type Ref } from 'vue'
import { askQuestionStream, askNormalChatStream } from '@/api/knowledge-chat'
import type { ChatSource } from '@/api/knowledge-chat'

export interface ChatFeedback {
  rating: 'up' | 'down'
  reason?: string
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: number
  sources?: ChatSource[]
  feedback?: ChatFeedback
}

interface SendQuestionOptions {
  topK?: number
}

interface UseKnowledgeChatOptions {
  messages?: Ref<ChatMessage[]>
  mode?: Ref<'knowledge' | 'normal'>
}

export function useKnowledgeChat(options: UseKnowledgeChatOptions = {}) {
  const messages = options.messages ?? ref<ChatMessage[]>([])
  const mode = options.mode ?? ref<'knowledge' | 'normal'>('knowledge')
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
      if (mode.value === 'knowledge') {
        // 知识库模式：使用 RAG
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
      } else {
        // 普通对话模式：不使用 RAG
        await askNormalChatStream(
          { question: normalized },
          {
            signal: currentAbortController.signal,
            onChunk: (chunk) => {
              assistantMessage.content += chunk
            },
          },
        )
      }
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

  const setMessageFeedback = (messageId: string, feedback: ChatFeedback) => {
    messages.value = messages.value.map((item) => {
      if (item.id !== messageId) {
        return item
      }
      return {
        ...item,
        feedback,
      }
    })
  }

  return {
    messages,
    loading,
    sendQuestion,
    clearMessages,
    stopGenerating,
    setMessageFeedback,
  }
}
