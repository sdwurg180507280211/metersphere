import { normalizeKnowledgeError } from './knowledge'

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

interface AskQuestionParams {
  question: string
  topK?: number
}

const USE_MOCK_CHAT = import.meta.env.VITE_KNOWLEDGE_CHAT_MOCK !== 'false'

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
  const response = await fetch('/analytics/knowledge/chat/ask', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify({
      question: params.question,
      topK: params.topK ?? 5,
    }),
  })

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`)
  }

  const data = await response.json()
  return {
    answer: String(data?.data?.answer || ''),
    sources: Array.isArray(data?.data?.sources) ? data.data.sources : [],
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
