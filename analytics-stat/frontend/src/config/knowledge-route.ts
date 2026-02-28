function normalizeRouteBase(base: string | undefined): string {
  const input = (base || '/analytics').trim()
  if (!input) {
    return '/analytics'
  }

  const withLeadingSlash = input.startsWith('/') ? input : `/${input}`
  const normalized = withLeadingSlash.replace(/\/+$/, '')
  return normalized || '/analytics'
}

export const KNOWLEDGE_ROUTE_BASE = normalizeRouteBase(import.meta.env.VITE_KNOWLEDGE_ROUTE_BASE)

export const KNOWLEDGE_ROUTE_PATHS = {
  knowledge: `${KNOWLEDGE_ROUTE_BASE}/knowledge`,
  knowledgeChat: `${KNOWLEDGE_ROUTE_BASE}/knowledge/chat`,
  home: `${KNOWLEDGE_ROUTE_BASE}/home`,
  sqlConsole: `${KNOWLEDGE_ROUTE_BASE}/sql-console`,
  dataDictionary: `${KNOWLEDGE_ROUTE_BASE}/data-dictionary`,
} as const
