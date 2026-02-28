import { computed, onBeforeUnmount, ref } from 'vue'
import { getFileList, KNOWLEDGE_FILE_STATUS } from '@/api/knowledge'
import type { KbFileUpload } from '@/api/knowledge'

interface UseKnowledgeFilesOptions {
  pollingInterval?: number
  onLoadError?: (error: unknown) => void
}

const STATUS_PROCESSING_SET = new Set([
  KNOWLEDGE_FILE_STATUS.UPLOADING,
  KNOWLEDGE_FILE_STATUS.UPLOADED,
  KNOWLEDGE_FILE_STATUS.PROCESSING,
])

export function useKnowledgeFiles(options: UseKnowledgeFilesOptions = {}) {
  const pollingInterval = options.pollingInterval ?? 10_000

  const fileList = ref<KbFileUpload[]>([])
  const loading = ref(false)
  let pollingTimer: number | null = null

  const hasProcessingFile = computed(() => {
    return fileList.value.some((item) => STATUS_PROCESSING_SET.has(item.status))
  })

  const stopPolling = () => {
    if (pollingTimer !== null) {
      window.clearInterval(pollingTimer)
      pollingTimer = null
    }
  }

  const syncPolling = () => {
    if (hasProcessingFile.value) {
      if (pollingTimer === null) {
        pollingTimer = window.setInterval(() => {
          void loadFileList({ silent: true })
        }, pollingInterval)
      }
      return
    }

    stopPolling()
  }

  const loadFileList = async ({ silent = false }: { silent?: boolean } = {}) => {
    if (!silent) {
      loading.value = true
    }

    try {
      fileList.value = await getFileList()
    } catch (error) {
      options.onLoadError?.(error)
    } finally {
      if (!silent) {
        loading.value = false
      }
      syncPolling()
    }
  }

  onBeforeUnmount(() => {
    stopPolling()
  })

  return {
    fileList,
    loading,
    hasProcessingFile,
    loadFileList,
    stopPolling,
  }
}
