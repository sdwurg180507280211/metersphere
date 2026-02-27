/**
 * 知识库检索 API 客户端
 *
 * 接口说明：
 * - GET /analytics/knowledge/search/hybrid?query=xxx&topK=10 — 混合检索（KNN + BM25）
 *
 * 请求路径说明：
 * - 前端发 /analytics/knowledge/search/hybrid
 * - Gateway 匹配 /analytics/** → 转发到 analytics-stat 后端，去掉 /analytics 前缀
 * - 后端收到 /knowledge/search/hybrid
 *
 * 认证说明：
 * - MeterSphere 使用 X-AUTH-TOKEN header 认证（不是 cookie）
 * - 登录后 token 存储在 localStorage 的 "Admin-Token" key 中
 * - 其他 Vue 2 模块通过 SDK 的 request.js 拦截器自动注入
 * - 本模块（Vue 3 独立 axios）需要手动从 localStorage 读取并注入
 *
 * 响应格式：{ code: 200, message: 'success', data: SearchResult[] }
 */
import axios from 'axios'

/**
 * localStorage 中存储用户认证信息的 key
 * 与 SDK 的 constants.js 中 TokenKey = "Admin-Token" 保持一致
 */
const TOKEN_KEY = 'Admin-Token'

/**
 * sessionStorage 中存储工作空间和项目 ID 的 key
 * 与 SDK 的 constants.js 中 WORKSPACE_ID / PROJECT_ID 保持一致
 */
const WORKSPACE_ID_KEY = 'workspace_id'
const PROJECT_ID_KEY = 'project_id'

/**
 * 创建带 /analytics 前缀的 axios 实例
 *
 * 为什么需要前缀：
 * - analytics-stat 在 Eureka 注册的服务名是 "analytics"
 * - Gateway 通过服务发现路由，URL 第一段路径必须是服务名
 * - 不加前缀，Gateway 无法识别该请求应转发给哪个微服务
 */
const http = axios.create({
  baseURL: '/analytics',
  withCredentials: true,
})

/**
 * 请求拦截器：注入认证 header
 *
 * 复刻 SDK request.js 的拦截器逻辑：
 * 1. 从 localStorage["Admin-Token"] 读取 sessionId 和 csrfToken
 * 2. 从 sessionStorage 读取当前工作空间和项目 ID
 * 3. 注入到请求 header 中，与其他 Vue 2 模块的请求行为一致
 *
 * 如果不注入这些 header，Gateway 的 SessionFilter 找不到 session，返回 401
 */
http.interceptors.request.use((config) => {
  // 从 localStorage 读取用户认证信息（登录时由主应用写入）
  const tokenStr = localStorage.getItem(TOKEN_KEY)
  if (tokenStr) {
    try {
      const user = JSON.parse(tokenStr)
      // 注入 session token（Gateway SessionFilter 用此查找 Redis session）
      if (user?.sessionId) {
        config.headers['X-AUTH-TOKEN'] = user.sessionId
      }
      // 注入 CSRF token（防跨站请求伪造）
      if (user?.csrfToken) {
        config.headers['CSRF-TOKEN'] = user.csrfToken
      }
    } catch {
      // JSON 解析失败，忽略（可能是脏数据）
    }
  }

  // 注入工作空间和项目标识（部分接口需要）
  const workspaceId = sessionStorage.getItem(WORKSPACE_ID_KEY)
  if (workspaceId) {
    config.headers['WORKSPACE'] = workspaceId
  }
  const projectId = sessionStorage.getItem(PROJECT_ID_KEY)
  if (projectId) {
    config.headers['PROJECT'] = projectId
  }

  return config
})

/**
 * 响应拦截器：处理认证失效
 *
 * 当 Gateway 返回 401 或 authentication-status: invalid 时，
 * 清除本地存储并跳转到登录页，与 SDK 行为一致
 */
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, headers } = error.response
      if (status === 401 || headers?.['authentication-status'] === 'invalid') {
        localStorage.removeItem(TOKEN_KEY)
        sessionStorage.removeItem(WORKSPACE_ID_KEY)
        sessionStorage.removeItem(PROJECT_ID_KEY)
        window.location.href = '/'
      }
    }
    return Promise.reject(error)
  }
)

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

/** 统一响应类型（MeterSphere SDK ResultHolder 格式） */
interface ApiResponse<T> {
  success: boolean
  message: string | null
  data: T
}

/**
 * 混合检索
 * @param query 搜索关键词
 * @param topK  返回结果数量，默认10
 */
export async function hybridSearch(query: string, topK = 10): Promise<SearchResult[]> {
  const res = await http.get<ApiResponse<SearchResult[]>>('/knowledge/search/hybrid', {
    params: { query, topK },
  })
  if (res.data.success) {
    return res.data.data
  }
  throw new Error(res.data.message || '检索失败')
}

/** 文件记录类型 */
export interface KbFileUpload {
  id: number
  fileMd5: string
  fileName: string
  totalSize: number
  status: number
  userId: string
  workspaceId: string
  isPublic: boolean
  createdAt: string
  updatedAt: string
}

/**
 * 上传文件到知识库
 * @param file     文件对象
 * @param isPublic 是否公开
 */
export async function uploadFile(file: File, isPublic = false): Promise<void> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('isPublic', String(isPublic))
  const res = await http.post<ApiResponse<void>>('/knowledge/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  if (!res.data.success) {
    throw new Error(res.data.message || '上传失败')
  }
}

/**
 * 获取文件列表
 */
export async function getFileList(): Promise<KbFileUpload[]> {
  const res = await http.get<ApiResponse<KbFileUpload[]>>('/knowledge/file/list')
  if (res.data.success) {
    return res.data.data
  }
  throw new Error(res.data.message || '获取文件列表失败')
}

/**
 * 删除文件
 * @param fileId 文件ID
 */
export async function deleteFile(fileId: number): Promise<void> {
  const res = await http.delete<ApiResponse<void>>(`/knowledge/file/${fileId}`)
  if (!res.data.success) {
    throw new Error(res.data.message || '删除失败')
  }
}
