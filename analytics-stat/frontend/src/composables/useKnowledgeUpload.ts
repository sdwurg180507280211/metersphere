import { ref } from 'vue'
import { uploadFile } from '@/api/knowledge'

export const EMPTY_UPLOAD_FILE_ERROR = 'EMPTY_UPLOAD_FILE'

export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${Math.round((bytes / Math.pow(k, i)) * 100) / 100} ${sizes[i]}`
}

export function useKnowledgeUpload() {
  const selectedFile = ref<File | null>(null)
  const isPublic = ref(false)
  const uploading = ref(false)

  const setSelectedFile = (file: File | null) => {
    selectedFile.value = file
  }

  const upload = async () => {
    if (!selectedFile.value) {
      throw new Error(EMPTY_UPLOAD_FILE_ERROR)
    }

    uploading.value = true
    try {
      await uploadFile(selectedFile.value, isPublic.value)
    } finally {
      uploading.value = false
    }
  }

  const reset = () => {
    selectedFile.value = null
    isPublic.value = false
  }

  return {
    selectedFile,
    isPublic,
    uploading,
    setSelectedFile,
    upload,
    reset,
  }
}
