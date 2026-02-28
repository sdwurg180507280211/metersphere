<template>
  <div class="file-list">
    <div class="toolbar">
      <el-input
        v-model="searchKeyword"
        clearable
        class="toolbar-item search-input"
        :placeholder="t('analytics.knowledge.file_name_search_placeholder')"
      />
      <el-select v-model="statusFilter" class="toolbar-item status-select">
        <el-option
          v-for="option in statusOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
    </div>

    <el-table
      v-loading="loading"
      :data="pagedFileList"
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

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="filteredFileList.length"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { deleteFile } from '@/api/knowledge'
import type { KbFileUpload } from '@/api/knowledge'
import { KNOWLEDGE_FILE_STATUS } from '@/api/knowledge'
import { useKnowledgeFiles } from '@/composables/useKnowledgeFiles'
import { resolveKnowledgeErrorMessage } from '@/composables/useKnowledgeErrorMessage'
import { useKnowledgeFileFilters } from '@/composables/useKnowledgeFileFilters'

const { t, locale } = useI18n()
const { loading, fileList, loadFileList } = useKnowledgeFiles({
  onLoadError: (error: any) => {
    ElMessage.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.load_failed'))
  },
})
const {
  searchKeyword,
  statusFilter,
  currentPage,
  pageSize,
  statusOptions,
  filteredFileList,
  pagedFileList,
  initFromRoute,
} = useKnowledgeFileFilters(fileList, t)

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
  return date.toLocaleString(locale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusType = (status: number): string => {
  const statusMap: Record<number, string> = {
    [KNOWLEDGE_FILE_STATUS.UPLOADING]: 'info',
    [KNOWLEDGE_FILE_STATUS.UPLOADED]: 'info',
    [KNOWLEDGE_FILE_STATUS.PROCESSING]: 'warning',
    [KNOWLEDGE_FILE_STATUS.INDEXED]: 'success',
    [KNOWLEDGE_FILE_STATUS.FAILED]: 'danger',
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: number): string => {
  const statusMap: Record<number, string> = {
    [KNOWLEDGE_FILE_STATUS.UPLOADING]: t('analytics.knowledge.status_uploading'),
    [KNOWLEDGE_FILE_STATUS.UPLOADED]: t('analytics.knowledge.status_uploaded'),
    [KNOWLEDGE_FILE_STATUS.PROCESSING]: t('analytics.knowledge.status_processing'),
    [KNOWLEDGE_FILE_STATUS.INDEXED]: t('analytics.knowledge.status_indexed'),
    [KNOWLEDGE_FILE_STATUS.FAILED]: t('analytics.knowledge.status_failed'),
  }
  return statusMap[status] || t('analytics.knowledge.status_unknown')
}

const handleDelete = async (row: KbFileUpload) => {
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
      ElMessage.error(resolveKnowledgeErrorMessage(error, t, 'commons.delete_failed'))
    }
  }
}

defineExpose({
  loadFileList
})

onMounted(() => {
  initFromRoute()
  loadFileList()
})
</script>

<style scoped>
.file-list {
  margin-top: 20px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-item {
  width: 240px;
}

.search-input {
  max-width: 320px;
}

.status-select {
  width: 180px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
