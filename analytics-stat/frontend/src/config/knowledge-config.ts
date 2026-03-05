/**
 * 知识库配置
 */
export const KNOWLEDGE_CONFIG = {
  /** 默认召回数量（从知识库检索的文档片段数量） */
  DEFAULT_TOP_K: 5,

  /** 最小召回数量 */
  MIN_TOP_K: 1,

  /** 最大召回数量 */
  MAX_TOP_K: 20,
} as const
