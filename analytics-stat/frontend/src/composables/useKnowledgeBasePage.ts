import { ref } from 'vue'

interface FileListExpose {
  loadFileList: (options?: { silent?: boolean }) => Promise<void> | void
}

export function useKnowledgeBasePage() {
  const showSearch = ref(false)
  const showUpload = ref(false)
  const fileListRef = ref<FileListExpose>()

  const openSearchDialog = () => {
    showSearch.value = true
  }

  const openUploadDialog = () => {
    showUpload.value = true
  }

  const refreshFileList = () => {
    fileListRef.value?.loadFileList()
  }

  const handleUploadSuccess = () => {
    refreshFileList()
  }

  return {
    showSearch,
    showUpload,
    fileListRef,
    openSearchDialog,
    openUploadDialog,
    refreshFileList,
    handleUploadSuccess,
  }
}
