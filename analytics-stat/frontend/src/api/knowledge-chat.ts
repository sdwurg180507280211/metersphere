import { knowledgeHttp, normalizeKnowledgeError } from './knowledge'

export interface ChatSource {
  fileId?: number
  fileName: string
  chunkId: number | string
  snippet: string
  score?: number
}

export interface ChatBackendStatus {
  llmEnabled: boolean
}

interface ApiResponse<T> {
  success: boolean
  message: string | null
  data: T
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

export interface ModelInfo {
  id: string
  name: string
}

export async function listModels(): Promise<ModelInfo[]> {
  try {
    const response = await knowledgeHttp.get<ApiResponse<ModelInfo[]>>('/knowledge/chat/models')
    if (!response.data.success) {
      throw new Error(response.data.message || '获取模型列表失败')
    }
    return Array.isArray(response.data.data) ? response.data.data : []
  } catch (error) {
    throw normalizeKnowledgeError(error, '获取模型列表失败')
  }
}
