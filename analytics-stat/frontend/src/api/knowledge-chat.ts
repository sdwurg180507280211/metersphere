import { knowledgeHttp, normalizeKnowledgeError } from './knowledge'

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

interface AskQuestionStreamOptions {
  signal?: AbortSignal
  onChunk: (chunk: string) => void
  onSources: (sources: ChatSource[]) => void
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

const USE_MOCK_CHAT = import.meta.env.VITE_KNOWLEDGE_CHAT_MOCK !== 'false'

function splitTextToChunks(text: string, size = 20): string[] {
  const chunks: string[] = []
  for (let i = 0; i < text.length; i += size) {
    chunks.push(text.slice(i, i + size))
  }
  return chunks
}

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
      topK: params.topK ?? 5,
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
  const response = await askQuestion(params)
  options.onSources(response.sources)

  const chunks = splitTextToChunks(response.answer)
  for (const chunk of chunks) {
    if (options.signal?.aborted) {
      throw new DOMException('Aborted', 'AbortError')
    }
    options.onChunk(chunk)
    await sleep(USE_MOCK_CHAT ? 40 : 20, options.signal)
  }
}
