import {
  KNOWLEDGE_ERROR_CODE,
  normalizeKnowledgeError,
} from '@/api/knowledge'

interface TranslateFn {
  (key: string): string
}

export function resolveKnowledgeErrorMessage(
  error: unknown,
  t: TranslateFn,
  fallbackKey: string,
): string {
  const normalizedError = normalizeKnowledgeError(error, t(fallbackKey))

  if (normalizedError.code === KNOWLEDGE_ERROR_CODE.AUTH_INVALID) {
    return t('analytics.knowledge.error_auth_invalid')
  }

  if (normalizedError.code === KNOWLEDGE_ERROR_CODE.FORBIDDEN) {
    return t('analytics.knowledge.error_forbidden')
  }

  if (normalizedError.code === KNOWLEDGE_ERROR_CODE.NETWORK) {
    return t('analytics.knowledge.error_network')
  }

  if (normalizedError.code === KNOWLEDGE_ERROR_CODE.SERVER) {
    return t('analytics.knowledge.error_server')
  }

  return normalizedError.message || t(fallbackKey)
}
