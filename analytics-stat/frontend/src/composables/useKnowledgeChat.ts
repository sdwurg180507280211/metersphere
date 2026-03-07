import { onScopeDispose, ref, type Ref } from 'vue'
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

export interface ChatHistoryTurn {
  userQuestion: string
  assistantAnswer: string
}

interface SendQuestionOptions {
  topK?: number
  modelId?: string
}

interface UseKnowledgeChatOptions {
  messages?: Ref<ChatMessage[]>
  mode?: Ref<'knowledge' | 'normal'>
  conversationId?: Ref<string>
}

interface TokenPayload {
  id?: string
  userId?: string
  sessionId?: string
  lastWorkspaceId?: string
}

interface ActiveRequest {
  requestId: string
  userMessageId: string
  assistantMessageId: string
  cancelled: boolean
  resolve: () => void
  reject: (error: Error) => void
}

const TOKEN_KEY = 'Admin-Token'
const WORKSPACE_ID_KEY = 'workspace_id'
const MAX_HISTORY_TURNS = 10
const RECONNECT_DELAY = 1500
const OPEN_TIMEOUT = 5000

function getTokenPayload(): TokenPayload {
  const tokenStr = localStorage.getItem(TOKEN_KEY)
  if (!tokenStr) {
    return {}
  }
  try {
    return JSON.parse(tokenStr)
  } catch {
    return {}
  }
}

function getSessionToken(): string {
  return getTokenPayload().sessionId || 'anonymous'
}

function getCurrentUserId(): string {
  const token = getTokenPayload()
  return token.id || token.userId || token.sessionId || 'anonymous'
}

function getCurrentWorkspaceId(): string {
  return sessionStorage.getItem(WORKSPACE_ID_KEY) || getTokenPayload().lastWorkspaceId || 'default'
}

function buildWsUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/analytics/ws/${getSessionToken()}`
}

function buildHistory(messages: ChatMessage[]): ChatHistoryTurn[] {
  const turns: ChatHistoryTurn[] = []
  let pendingQuestion = ''

  messages.forEach((message) => {
    if (message.role === 'user') {
      pendingQuestion = message.content.trim()
      return
    }

    if (!pendingQuestion) {
      return
    }

    const answer = message.content.trim()
    if (pendingQuestion || answer) {
      turns.push({
        userQuestion: pendingQuestion,
        assistantAnswer: answer,
      })
    }
    pendingQuestion = ''
  })

  return turns.slice(-MAX_HISTORY_TURNS)
}

export function useKnowledgeChat(options: UseKnowledgeChatOptions = {}) {
  const messages = options.messages ?? ref<ChatMessage[]>([])
  const mode = options.mode ?? ref<'knowledge' | 'normal'>('knowledge')
  const conversationId = options.conversationId ?? ref('default')
  const loading = ref(false)

  const ws = ref<WebSocket | null>(null)
  let openPromise: Promise<void> | null = null
  let reconnectTimer: number | null = null
  let manualClose = false
  let activeRequest: ActiveRequest | null = null

  const clearReconnectTimer = () => {
    if (reconnectTimer !== null) {
      window.clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  const findAssistantMessage = () => {
    if (!activeRequest) {
      return null
    }
    return messages.value.find((item) => item.id === activeRequest?.assistantMessageId) || null
  }

  const rollbackRequestMessages = (request: ActiveRequest) => {
    messages.value = messages.value.filter((item) => item.id !== request.userMessageId && item.id !== request.assistantMessageId)
  }

  const finalizeRequest = (error?: Error) => {
    const request = activeRequest
    if (!request) {
      loading.value = false
      return
    }

    const assistantMessage = findAssistantMessage()
    activeRequest = null
    loading.value = false
    if (assistantMessage?.role === 'assistant' && !assistantMessage.content.trim()) {
      messages.value = messages.value.filter((item) => item.id !== assistantMessage.id)
    }

    if (request.cancelled) {
      request.resolve()
      return
    }

    if (error) {
      request.reject(error)
      return
    }

    request.resolve()
  }

  const handleServerMessage = (raw: string) => {
    let payload: Record<string, any>
    try {
      payload = JSON.parse(raw)
    } catch (error) {
      console.error('解析WebSocket消息失败:', error)
      return
    }

    if (payload.type === 'connection') {
      return
    }

    if (!activeRequest || payload.requestId !== activeRequest.requestId) {
      return
    }

    const assistantMessage = findAssistantMessage()
    if (!assistantMessage) {
      return
    }

    if (payload.type === 'delta') {
      assistantMessage.content += String(payload.data || '')
      return
    }

    if (payload.type === 'sources') {
      assistantMessage.sources = Array.isArray(payload.data) ? payload.data : []
      return
    }

    if (payload.type === 'done' || payload.type === 'cancelled') {
      finalizeRequest()
      return
    }

    if (payload.type === 'error') {
      finalizeRequest(new Error(String(payload.message || '聊天请求失败')))
    }
  }

  const scheduleReconnect = () => {
    if (manualClose || reconnectTimer !== null) {
      return
    }
    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = null
      void ensureSocketOpen().catch(() => {
      })
    }, RECONNECT_DELAY)
  }

  const connect = (): Promise<void> => {
    if (ws.value?.readyState === WebSocket.OPEN) {
      return Promise.resolve()
    }

    if (ws.value?.readyState === WebSocket.CONNECTING && openPromise) {
      return openPromise
    }

    clearReconnectTimer()
    const socket = new WebSocket(buildWsUrl())
    ws.value = socket

    openPromise = new Promise((resolve, reject) => {
      socket.onopen = () => {
        resolve()
      }

      socket.onmessage = (event) => {
        if (typeof event.data === 'string') {
          handleServerMessage(event.data)
        }
      }

      socket.onerror = () => {
        reject(new Error('WebSocket 连接失败'))
      }

      socket.onclose = () => {
        if (ws.value === socket) {
          ws.value = null
        }
        if (activeRequest && !activeRequest.cancelled) {
          finalizeRequest(new Error('WebSocket 连接已断开'))
        }
        if (!manualClose) {
          scheduleReconnect()
        }
      }
    }).finally(() => {
      openPromise = null
    })

    return openPromise
  }

  const ensureSocketOpen = async () => {
    await Promise.race([
      connect(),
      new Promise<void>((_, reject) => {
        window.setTimeout(() => reject(new Error('WebSocket 连接超时')), OPEN_TIMEOUT)
      }),
    ])
  }

  const sendSocketMessage = async (payload: Record<string, unknown>) => {
    await ensureSocketOpen()
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket 尚未建立连接')
    }
    ws.value.send(JSON.stringify(payload))
  }

  const sendQuestion = async (question: string, options: SendQuestionOptions = {}) => {
    if (loading.value) {
      throw new Error('当前仍有回答生成中')
    }

    const normalized = question.trim()
    if (!normalized) {
      return
    }

    const history = buildHistory(messages.value)
    const now = Date.now()
    const requestId = `r-${now}`
    const userMessageId = `u-${now}`
    const assistantMessageId = `a-${now}`

    messages.value.push({
      id: userMessageId,
      role: 'user',
      content: normalized,
      timestamp: now,
    })

    messages.value.push({
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: now,
      sources: [],
    })

    loading.value = true

    const result = new Promise<void>((resolve, reject) => {
      activeRequest = {
        requestId,
        userMessageId,
        assistantMessageId,
        cancelled: false,
        resolve,
        reject,
      }
    })

    try {
      await sendSocketMessage({
        action: 'ask',
        requestId,
        conversationId: conversationId.value,
        question: normalized,
        mode: mode.value,
        topK: options.topK ?? KNOWLEDGE_CONFIG.DEFAULT_TOP_K,
        modelId: options.modelId,
        userId: getCurrentUserId(),
        workspaceId: getCurrentWorkspaceId(),
        history,
      })
    } catch (error) {
      const request = activeRequest
      if (request) {
        activeRequest = null
        loading.value = false
        rollbackRequestMessages(request)
        request.reject(error instanceof Error ? error : new Error('消息发送失败'))
      }
      throw error
    }

    return result
  }

  const stopGenerating = () => {
    if (!activeRequest) {
      return
    }

    const requestId = activeRequest.requestId
    activeRequest.cancelled = true

    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({
        action: 'cancel',
        requestId,
      }))
    }

    finalizeRequest()
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

  void ensureSocketOpen().catch(() => {
  })

  onScopeDispose(() => {
    manualClose = true
    clearReconnectTimer()
    stopGenerating()
    ws.value?.close()
    ws.value = null
  })

  return {
    messages,
    loading,
    sendQuestion,
    clearMessages,
    stopGenerating,
    setMessageFeedback,
  }
}
