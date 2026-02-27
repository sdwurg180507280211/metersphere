<template>
  <div class="file-list">
    <el-table
      v-loading="loading"
      :data="fileList"
      style="width: 100%"
      :empty-text="t('analytics.knowledge.no_files')"
    >
      <el-table-column
        prop="fileName"
        :label="t('analytics.knowledge.file_name')"
        min-width="200"
      >
        <template #default="{ row }">
          <el-icon style="margin-right: 8px"><Document /></el-icon>
          {{ row.fileName }}
        </template>
      </el-table-column>

      <el-table-column
        prop="totalSize"
        :label="t('analytics.knowledge.file_size')"
        width="120"
      >
        <template #default="{ row }">
          {{ formatFileSize(row.totalSize) }}
        </template>
      </el-table-column>

      <el-table-column
        prop="status"
        :label="t('analytics.knowledge.status')"
        width="120"
      >
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        prop="isPublic"
        :label="t('analytics.knowledge.visibility')"
        width="100"
      >
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'" size="small">
            {{ row.isPublic ? t('analytics.knowledge.public') : t('analytics.knowledge.private') }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        prop="createdAt"
        :label="t('analytics.knowledge.upload_time')"
        width="180"
      >
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column
        :label="t('commons.operating')"
        width="100"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            type="danger"
            size="small"
            link
            @click="handleDelete(row)"
          >
            {{ t('commons.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { getFileList, deleteFile } from '@/api/knowledge'

interface FileItem {
  id: number
  fileName: string
  fileMd5: string
  totalSize: number
  status: number
  isPublic: boolean
  createdAt: string
}

const { t } = useI18n()

const loading = ref(false)
const fileList = ref<FileItem[]>([])

const loadFileList = async () => {
  loading.value = true
  try {
    const data = await getFileList()
    fileList.value = data
  } catch (error: any) {
    ElMessage.error(error.message || t('analytics.knowledge.load_failed'))
  } finally {
    loading.value = false
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusType = (status: number): string => {
  const statusMap: Record<number, string> = {
    0: 'info',      // 上传中
    1: 'info',      // 已上传
    2: 'warning',   // 解析中
    3: 'success',   // 已入库
    [-1]: 'danger'  // 失败
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: number): string => {
  const statusMap: Record<number, string> = {
    0: t('analytics.knowledge.status_uploading'),
    1: t('analytics.knowledge.status_uploaded'),
    2: t('analytics.knowledge.status_processing'),
    3: t('analytics.knowledge.status_indexed'),
    [-1]: t('analytics.knowledge.status_failed')
  }
  return statusMap[status] || t('analytics.knowledge.status_unknown')
}

const handleDelete = async (row: FileItem) => {
  try {
    await ElMessageBox.confirm(
      t('analytics.knowledge.delete_confirm', { name: row.fileName }),
      t('commons.prompt'),
      {
        confirmButtonText: t('commons.confirm'),
        cancelButtonText: t('commons.cancel'),
        type: 'warning'
      }
    )

    await deleteFile(row.id)
    ElMessage.success(t('commons.delete_success'))
    await loadFileList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || t('commons.delete_failed'))
    }
  }
}

defineExpose({
  loadFileList
})

onMounted(() => {
  loadFileList()
})
</script>

<style scoped>
.file-list {
  margin-top: 20px;
}
</style>
