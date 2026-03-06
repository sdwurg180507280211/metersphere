import { ref, watch, type Ref } from 'vue'
import { useWebSocket } from '@vueuse/core'
import type { ChatSource } from '@/api/knowledge-chat'
import { KNOWLEDGE_CONFIG } from '@/config/knowledge-config'

export interface ChatFeedback {
  rating: 'up' | 'down'
  reason?: string
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  sources?: ChatSource[]
  feedback?: ChatFeedback
}

interface SendQuestionOptions {
  topK?: number
  modelId?: string
}

interface UseKnowledgeChatOptions {
  messages?: Ref<ChatMessage[]>
  mode?: Ref<'knowledge' | 'normal'>
}

function getToken(): string {
  const tokenStr = localStorage.getItem('Admin-Token')
  if (tokenStr) {
    try {
      const user = JSON.parse(tokenStr)
      return user?.sessionId || 'anonymous'
    } catch {
      return 'anonymous'
    }
  }
  return 'anonymous'
}

export function useKnowledgeChat(options: UseKnowledgeChatOptions = {}) {
  const messages = options.messages ?? ref<ChatMessage[]>([])
  const mode = options.mode ?? ref<'knowledge' | 'normal'>('knowledge')
  const loading = ref(false)

  const token = getToken()
  const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/analytics/ws/${token}`

  const { status, data: wsData, send: wsSend, open: wsOpen, close: wsClose } = useWebSocket(wsUrl, {
    autoReconnect: true,
    heartbeat: {
      message: 'ping',
      interval: 30000,
    },
  })

  watch(wsData, (val) => {
    if (!val) return
    try {
      const message = JSON.parse(val)
      handleWebSocketMessage(message)
    } catch (e) {
      console.error('解析WebSocket消息失败:', e)
    }
  })

  function handleWebSocketMessage(message: any) {
    const { type, data, sessionId } = message

    if (type === 'connection') {
      console.log('WebSocket连接成功, sessionId:', sessionId)
      return
    }

    const lastMessage = messages.value[messages.value.length - 1]

    if (type === 'delta' && lastMessage?.role === 'assistant') {
      lastMessage.content += data
      return
    }

    if (type === 'sources' && lastMessage?.role === 'assistant') {
      lastMessage.sources = data
      return
    }

    if (type === 'done') {
      loading.value = false
      return
    }

    if (type === 'error') {
      loading.value = false
      if (lastMessage?.role === 'assistant' && !lastMessage.content) {
        messages.value.pop()
      }
      console.error('WebSocket错误:', message.message)
      return
    }
  }

  const sendQuestion = async (question: string, options: SendQuestionOptions = {}) => {
    const normalized = question.trim()
    if (!normalized) return

    const now = Date.now()
    messages.value.push({
      id: `u-${now}`,
      role: 'user',
      content: normalized,
      timestamp: now,
    })

    messages.value.push({
      id: `a-${Date.now()}`,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      sources: [],
    })

    loading.value = true

    wsSend(JSON.stringify({
      question: normalized,
      mode: mode.value,
      topK: options.topK ?? KNOWLEDGE_CONFIG.DEFAULT_TOP_K,
      modelId: options.modelId,
    }))
  }

  const stopGenerating = () => {
    if (loading.value) {
      loading.value = false
      const lastMessage = messages.value[messages.value.length - 1]
      if (lastMessage?.role === 'assistant' && !lastMessage.content.trim()) {
        messages.value.pop()
      }
    }
  }

  const clearMessages = () => {
    stopGenerating()
    messages.value = []
  }

  const setMessageFeedback = (messageId: string, feedback: ChatFeedback) => {
    messages.value = messages.value.map((item) => {
      if (item.id !== messageId) return item
      return { ...item, feedback }
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
