import { computed, ref } from 'vue'
import type { ChatMessage } from './useKnowledgeChat'

export interface ChatSession {
  id: string
  title: string
  updatedAt: number
  messages: ChatMessage[]
}

const STORAGE_KEY = 'knowledge-chat-sessions'

function createSession(title = 'New Chat'): ChatSession {
  return {
    id: `s-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    title,
    updatedAt: Date.now(),
    messages: [],
  }
}

function loadSessions(): ChatSession[] {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return []
  }
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.filter((item) => item && typeof item.id === 'string' && Array.isArray(item.messages))
  } catch {
    return []
  }
}

export function useChatSessionStore() {
  const sessions = ref<ChatSession[]>(loadSessions())
  const currentSessionId = ref<string>('')

  const ensureSession = () => {
    if (sessions.value.length === 0) {
      const session = createSession()
      sessions.value = [session]
      currentSessionId.value = session.id
      return
    }
    if (!sessions.value.some((item) => item.id === currentSessionId.value)) {
      currentSessionId.value = sessions.value[0].id
    }
  }

  const persist = () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions.value))
  }

  const touchSession = (id: string, messages: ChatMessage[]) => {
    sessions.value = sessions.value.map((item) => {
      if (item.id !== id) {
        return item
      }

      const firstUserMessage = messages.find((msg) => msg.role === 'user')
      const nextTitle = firstUserMessage?.content.slice(0, 24) || item.title
      return {
        ...item,
        title: nextTitle,
        updatedAt: Date.now(),
        messages,
      }
    })
    sessions.value.sort((a, b) => b.updatedAt - a.updatedAt)
    persist()
  }

  const createNewSession = () => {
    const session = createSession()
    sessions.value = [session, ...sessions.value]
    currentSessionId.value = session.id
    persist()
  }

  const selectSession = (id: string) => {
    if (sessions.value.some((item) => item.id === id)) {
      currentSessionId.value = id
    }
  }

  const deleteSession = (id: string) => {
    sessions.value = sessions.value.filter((item) => item.id !== id)
    ensureSession()
    persist()
  }

  const renameSession = (id: string, title: string) => {
    const normalized = title.trim()
    if (!normalized) {
      return
    }
    sessions.value = sessions.value.map((item) => {
      if (item.id !== id) {
        return item
      }
      return {
        ...item,
        title: normalized,
        updatedAt: Date.now(),
      }
    })
    sessions.value.sort((a, b) => b.updatedAt - a.updatedAt)
    persist()
  }

  const clearAllSessions = () => {
    sessions.value = []
    ensureSession()
    persist()
  }

  ensureSession()
  persist()

  const currentSession = computed(() => {
    return sessions.value.find((item) => item.id === currentSessionId.value) || sessions.value[0]
  })

  return {
    sessions,
    currentSessionId,
    currentSession,
    createNewSession,
    selectSession,
    deleteSession,
    renameSession,
    touchSession,
    clearAllSessions,
  }
}
