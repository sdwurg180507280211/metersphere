import { computed, ref, watch, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { LocationQueryRaw } from 'vue-router'
import { KNOWLEDGE_FILE_STATUS } from '@/api/knowledge'
import type { KbFileUpload } from '@/api/knowledge'

interface TranslateFn {
  (key: string): string
}

const DEFAULT_PAGE = 1
const DEFAULT_PAGE_SIZE = 10
const ALLOWED_PAGE_SIZES = [10, 20, 50]
const QUERY_KEYS = {
  keyword: 'keyword',
  status: 'status',
  page: 'page',
  size: 'size',
} as const

const ALLOWED_STATUS_VALUES = new Set([
  'all',
  String(KNOWLEDGE_FILE_STATUS.UPLOADING),
  String(KNOWLEDGE_FILE_STATUS.UPLOADED),
  String(KNOWLEDGE_FILE_STATUS.PROCESSING),
  String(KNOWLEDGE_FILE_STATUS.INDEXED),
  String(KNOWLEDGE_FILE_STATUS.FAILED),
])

function getQueryValue(value: unknown): string {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }
  return typeof value === 'string' ? value : ''
}

function parsePositiveInt(value: string, fallback: number): number {
  const n = Number(value)
  if (!Number.isInteger(n) || n <= 0) {
    return fallback
  }
  return n
}

export function useKnowledgeFileFilters(fileList: Ref<KbFileUpload[]>, t: TranslateFn) {
  const route = useRoute()
  const router = useRouter()

  const searchKeyword = ref('')
  const statusFilter = ref('all')
  const currentPage = ref(DEFAULT_PAGE)
  const pageSize = ref(DEFAULT_PAGE_SIZE)
  let updatingFromRoute = false

  const statusOptions = computed(() => [
    { value: 'all', label: t('analytics.knowledge.status_all') },
    { value: String(KNOWLEDGE_FILE_STATUS.UPLOADING), label: t('analytics.knowledge.status_uploading') },
    { value: String(KNOWLEDGE_FILE_STATUS.UPLOADED), label: t('analytics.knowledge.status_uploaded') },
    { value: String(KNOWLEDGE_FILE_STATUS.PROCESSING), label: t('analytics.knowledge.status_processing') },
    { value: String(KNOWLEDGE_FILE_STATUS.INDEXED), label: t('analytics.knowledge.status_indexed') },
    { value: String(KNOWLEDGE_FILE_STATUS.FAILED), label: t('analytics.knowledge.status_failed') },
  ])

  const filteredFileList = computed(() => {
    const keyword = searchKeyword.value.trim().toLowerCase()
    return fileList.value.filter((item) => {
      const matchesKeyword = !keyword || item.fileName.toLowerCase().includes(keyword)
      const matchesStatus = statusFilter.value === 'all' || String(item.status) === statusFilter.value
      return matchesKeyword && matchesStatus
    })
  })

  const pagedFileList = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return filteredFileList.value.slice(start, start + pageSize.value)
  })

  const applyQueryToState = () => {
    updatingFromRoute = true
    const keyword = getQueryValue(route.query[QUERY_KEYS.keyword]).trim()
    const status = getQueryValue(route.query[QUERY_KEYS.status])
    const page = parsePositiveInt(getQueryValue(route.query[QUERY_KEYS.page]), DEFAULT_PAGE)
    const size = parsePositiveInt(getQueryValue(route.query[QUERY_KEYS.size]), DEFAULT_PAGE_SIZE)

    searchKeyword.value = keyword
    statusFilter.value = ALLOWED_STATUS_VALUES.has(status) ? status : 'all'
    currentPage.value = page
    pageSize.value = ALLOWED_PAGE_SIZES.includes(size) ? size : DEFAULT_PAGE_SIZE
    updatingFromRoute = false
  }

  const getStateQuery = (): LocationQueryRaw => {
    const query: LocationQueryRaw = { ...route.query }

    const keyword = searchKeyword.value.trim()
    if (keyword) {
      query[QUERY_KEYS.keyword] = keyword
    } else {
      delete query[QUERY_KEYS.keyword]
    }

    if (statusFilter.value !== 'all') {
      query[QUERY_KEYS.status] = statusFilter.value
    } else {
      delete query[QUERY_KEYS.status]
    }

    if (currentPage.value !== DEFAULT_PAGE) {
      query[QUERY_KEYS.page] = String(currentPage.value)
    } else {
      delete query[QUERY_KEYS.page]
    }

    if (pageSize.value !== DEFAULT_PAGE_SIZE) {
      query[QUERY_KEYS.size] = String(pageSize.value)
    } else {
      delete query[QUERY_KEYS.size]
    }

    return query
  }

  const shouldSyncQuery = (nextQuery: LocationQueryRaw) => {
    return (
      getQueryValue(route.query[QUERY_KEYS.keyword]) !== getQueryValue(nextQuery[QUERY_KEYS.keyword]) ||
      getQueryValue(route.query[QUERY_KEYS.status]) !== getQueryValue(nextQuery[QUERY_KEYS.status]) ||
      getQueryValue(route.query[QUERY_KEYS.page]) !== getQueryValue(nextQuery[QUERY_KEYS.page]) ||
      getQueryValue(route.query[QUERY_KEYS.size]) !== getQueryValue(nextQuery[QUERY_KEYS.size])
    )
  }

  watch([searchKeyword, statusFilter], () => {
    currentPage.value = 1
  })

  watch([filteredFileList, pageSize], () => {
    const maxPage = Math.max(1, Math.ceil(filteredFileList.value.length / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
  })

  watch([searchKeyword, statusFilter, currentPage, pageSize], async () => {
    if (updatingFromRoute) {
      return
    }
    const nextQuery = getStateQuery()
    if (!shouldSyncQuery(nextQuery)) {
      return
    }
    await router.replace({ query: nextQuery })
  })

  watch(
    () => route.query,
    () => {
      applyQueryToState()
    }
  )

  const initFromRoute = () => {
    applyQueryToState()
  }

  return {
    searchKeyword,
    statusFilter,
    currentPage,
    pageSize,
    statusOptions,
    filteredFileList,
    pagedFileList,
    initFromRoute,
  }
}
