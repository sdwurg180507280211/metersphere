import { ref } from 'vue'

const HISTORY_KEY = 'knowledge-chat-history'
const MAX_HISTORY_SIZE = 30

function loadHistory(): string[] {
  const raw = localStorage.getItem(HISTORY_KEY)
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.filter((item): item is string => typeof item === 'string').slice(0, MAX_HISTORY_SIZE)
  } catch {
    return []
  }
}

export function useChatHistory() {
  const history = ref<string[]>(loadHistory())

  const persist = () => {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value))
  }

  const addQuestion = (question: string) => {
    const normalized = question.trim()
    if (!normalized) {
      return
    }

    history.value = [normalized, ...history.value.filter((item) => item !== normalized)].slice(0, MAX_HISTORY_SIZE)
    persist()
  }

  const clearHistory = () => {
    history.value = []
    persist()
  }

  return {
    history,
    addQuestion,
    clearHistory,
  }
}
