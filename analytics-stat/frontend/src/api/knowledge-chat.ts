import { knowledgeHttp, normalizeKnowledgeError } from './knowledge'
import { KNOWLEDGE_CONFIG } from '@/config/knowledge-config'

export interface ChatSource {
  fileId?: number
  fileName: string
  chunkId: number | string
  snippet: string
  score?: number
}

export interface ChatResponse {
  answer: string
  sources: ChatSource[]
}

export interface AskQuestionParams {
  question: string
  topK?: number
}

export interface AskNormalChatParams {
  question: string
}

export interface ChatBackendStatus {
  llmEnabled: boolean
}

interface AskQuestionStreamOptions {
  signal?: AbortSignal
  onChunk: (chunk: string) => void
  onSources: (sources: ChatSource[]) => void
}

interface AskNormalChatStreamOptions {
  signal?: AbortSignal
  onChunk: (chunk: string) => void
}

interface ApiResponse<T> {
  success: boolean
  message: string | null
  data: T
}

interface ChatApiData {
  answer: string
  sources: ChatSource[]
}

const USE_MOCK_CHAT = import.meta.env.VITE_KNOWLEDGE_CHAT_MOCK === 'true'

function splitTextToChunks(text: string, size = 20): string[] {
  const chunks: string[] = []
  for (let i = 0; i < text.length; i += size) {
    chunks.push(text.slice(i, i + size))
  }
  return chunks
}

const TOKEN_KEY = 'Admin-Token'
const WORKSPACE_ID_KEY = 'workspace_id'
const PROJECT_ID_KEY = 'project_id'

function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException('Aborted', 'AbortError'))
      return
    }

    const timer = window.setTimeout(() => {
      cleanup()
      resolve()
    }, ms)

    const onAbort = () => {
      window.clearTimeout(timer)
      cleanup()
      reject(new DOMException('Aborted', 'AbortError'))
    }

    const cleanup = () => {
      signal?.removeEventListener('abort', onAbort)
    }

    signal?.addEventListener('abort', onAbort)
  })
}

function createMockSources(question: string): ChatSource[] {
  return [
    {
      fileName: 'knowledge-onboarding.md',
      chunkId: 1,
      score: 0.92,
      snippet: `与问题"${question}"最相关的基础说明片段，建议先核对术语定义再扩展检索范围。`,
    },
    {
      fileName: 'knowledge-faq.md',
      chunkId: 7,
      score: 0.86,
      snippet: '该片段包含常见问题排查路径，可作为补充上下文进行交叉验证。',
    },
  ]
}

async function askQuestionByMock(params: AskQuestionParams): Promise<ChatResponse> {
  await new Promise((resolve) => window.setTimeout(resolve, 500))

  return {
    answer: `这是基于知识库检索结果生成的示例回答：\n\n你的问题是“${params.question}”。建议先确认文档版本与权限范围，再按来源片段逐条验证关键结论。`,
    sources: createMockSources(params.question),
  }
}

async function askQuestionByApi(params: AskQuestionParams): Promise<ChatResponse> {
  const response = await knowledgeHttp.post<ApiResponse<ChatApiData>>('/knowledge/chat/ask', {
      question: params.question,
      topK: params.topK ?? KNOWLEDGE_CONFIG.DEFAULT_TOP_K,
  })

  if (!response.data.success) {
    throw new Error(response.data.message || '知识问答请求失败')
  }

  const data = response.data.data
  return {
    answer: String(data?.answer || ''),
    sources: Array.isArray(data?.sources) ? data.sources : [],
  }
}

function buildAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  const tokenStr = localStorage.getItem(TOKEN_KEY)
  if (tokenStr) {
    try {
      const user = JSON.parse(tokenStr)
      if (user?.sessionId) {
        headers['X-AUTH-TOKEN'] = user.sessionId
      }
      if (user?.csrfToken) {
        headers['CSRF-TOKEN'] = user.csrfToken
      }
    } catch {
    }
  }

  const workspaceId = sessionStorage.getItem(WORKSPACE_ID_KEY)
  if (workspaceId) {
    headers.WORKSPACE = workspaceId
  }

  const projectId = sessionStorage.getItem(PROJECT_ID_KEY)
  if (projectId) {
    headers.PROJECT = projectId
  }

  return headers
}

function parseSseEvents(rawEvent: string): { event: string; data: string } {
  const lines = rawEvent.split(/\r?\n/)
  let event = 'message'
  const dataLines: string[] = []

  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
      return
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  })

  return {
    event,
    data: dataLines.join('\n'),
  }
}

function handleSseEventText(eventText: string, options: AskQuestionStreamOptions): 'done' | void {
  if (!eventText.trim()) {
    return
  }
  const { event, data } = parseSseEvents(eventText)
  if (event === 'delta') {
    options.onChunk(data)
    return
  }
  if (event === 'sources') {
    try {
      const parsed = JSON.parse(data)
      options.onSources(Array.isArray(parsed) ? parsed : [])
    } catch {
      options.onSources([])
    }
    return
  }
  if (event === 'error') {
    throw new Error(data || '知识流式问答请求失败')
  }
  if (event === 'done') {
    return 'done'
  }
}

async function askQuestionStreamByApi(
  params: AskQuestionParams,
  options: AskQuestionStreamOptions,
): Promise<void> {
  const response = await fetch('/analytics/knowledge/chat/stream', {
    method: 'POST',
    headers: buildAuthHeaders(),
    credentials: 'include',
    signal: options.signal,
    body: JSON.stringify({
      question: params.question,
      topK: params.topK ?? KNOWLEDGE_CONFIG.DEFAULT_TOP_K,
    }),
  })

  if (!response.ok) {
    throw new Error(`知识流式问答请求失败: ${response.status}`)
  }

  if (!response.body) {
    throw new Error('未获取到流式响应体')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''

    for (const eventText of events) {
      const status = handleSseEventText(eventText, options)
      if (status === 'done') {
        return
      }
    }
  }

  if (buffer.trim()) {
    const status = handleSseEventText(buffer, options)
    if (status === 'done') {
      return
    }
  }
}

export async function askQuestion(params: AskQuestionParams): Promise<ChatResponse> {
  try {
    if (USE_MOCK_CHAT) {
      return askQuestionByMock(params)
    }

    return askQuestionByApi(params)
  } catch (error) {
    throw normalizeKnowledgeError(error, '知识问答请求失败')
  }
}

export async function askQuestionStream(
  params: AskQuestionParams,
  options: AskQuestionStreamOptions,
): Promise<void> {
  if (!USE_MOCK_CHAT) {
    try {
      await askQuestionStreamByApi(params, options)
      return
    } catch (error) {
      throw normalizeKnowledgeError(error, '知识流式问答请求失败')
    }
  }

  const response = await askQuestion(params)
  options.onSources(response.sources)

  const chunks = splitTextToChunks(response.answer)
  for (const chunk of chunks) {
    if (options.signal?.aborted) {
      throw new DOMException('Aborted', 'AbortError')
    }
    options.onChunk(chunk)
    await sleep(40, options.signal)
  }
}

export async function getChatBackendStatus(): Promise<ChatBackendStatus> {
  try {
    const response = await knowledgeHttp.get<ApiResponse<ChatBackendStatus>>('/knowledge/chat/status')
    if (!response.data.success) {
      throw new Error(response.data.message || '获取问答状态失败')
    }
    return {
      llmEnabled: Boolean(response.data.data?.llmEnabled),
    }
  } catch (error) {
    throw normalizeKnowledgeError(error, '获取问答状态失败')
  }
}

/**
 * 普通 AI 对话流式接口（不使用 RAG）
 */
async function askNormalChatStreamByApi(
  params: AskNormalChatParams,
  options: AskNormalChatStreamOptions,
): Promise<void> {
  const response = await fetch('/analytics/knowledge/chat/normal-stream', {
    method: 'POST',
    headers: buildAuthHeaders(),
    credentials: 'include',
    signal: options.signal,
    body: JSON.stringify({
      question: params.question,
    }),
  })

  if (!response.ok) {
    throw new Error(`普通对话请求失败: ${response.status}`)
  }

  if (!response.body) {
    throw new Error('未获取到流式响应体')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''

    for (const eventText of events) {
      if (!eventText.trim()) {
        continue
      }
      const { event, data } = parseSseEvents(eventText)
      if (event === 'delta') {
        options.onChunk(data)
      } else if (event === 'error') {
        throw new Error(data || '普通对话请求失败')
      } else if (event === 'done') {
        return
      }
    }
  }

  if (buffer.trim()) {
    const { event, data } = parseSseEvents(buffer)
    if (event === 'delta') {
      options.onChunk(data)
    } else if (event === 'done') {
      return
    }
  }
}

export async function askNormalChatStream(
  params: AskNormalChatParams,
  options: AskNormalChatStreamOptions,
): Promise<void> {
  try {
    await askNormalChatStreamByApi(params, options)
  } catch (error) {
    throw normalizeKnowledgeError(error, '普通对话请求失败')
  }
}
