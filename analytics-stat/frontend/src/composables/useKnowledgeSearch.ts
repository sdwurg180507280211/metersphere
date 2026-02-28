import { ref } from 'vue'
import { hybridSearch } from '@/api/knowledge'
import type { SearchResult } from '@/api/knowledge'

export const EMPTY_QUERY_ERROR = 'EMPTY_QUERY'

export function useKnowledgeSearch() {
  const query = ref('')
  const topK = ref(10)
  const loading = ref(false)
  const searched = ref(false)
  const results = ref<SearchResult[]>([])

  const search = async () => {
    const q = query.value.trim()
    if (!q) {
      throw new Error(EMPTY_QUERY_ERROR)
    }

    loading.value = true
    searched.value = true
    results.value = []

    try {
      results.value = await hybridSearch(q, topK.value)
      return results.value
    } finally {
      loading.value = false
    }
  }

  const reset = () => {
    query.value = ''
    searched.value = false
    results.value = []
  }

  return {
    query,
    topK,
    loading,
    searched,
    results,
    search,
    reset,
  }
}
