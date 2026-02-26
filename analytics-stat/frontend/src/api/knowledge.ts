/**
 * 知识库检索 API 客户端
 *
 * 接口说明：
 * - GET /knowledge/search/hybrid?query=xxx&topK=10 — 混合检索（KNN + BM25）
 *
 * 响应格式：{ code: 200, message: 'success', data: SearchResult[] }
 */
import axios from 'axios'

/** 检索结果类型 */
export interface SearchResult {
  fileMd5: string
  chunkId: number
  textContent: string
  score: number
  fileName: string | null
  userId: string | null
  orgTag: string | null
  isPublic: boolean
}

/** 统一响应类型 */
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/**
 * 混合检索
 * @param query 搜索关键词
 * @param topK  返回结果数量，默认10
 */
export async function hybridSearch(query: string, topK = 10): Promise<SearchResult[]> {
  const res = await axios.get<ApiResponse<SearchResult[]>>('/knowledge/search/hybrid', {
    params: { query, topK },
  })
  if (res.data.code === 200) {
    return res.data.data
  }
  throw new Error(res.data.message || '检索失败')
}
