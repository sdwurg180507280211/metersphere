<template>
  <div class="file-list">
    <div class="toolbar">
      <n-input
        v-model:value="searchKeyword"
        clearable
        class="toolbar-item search-input"
        :placeholder="t('analytics.knowledge.file_name_search_placeholder')"
      />
      <n-select v-model:value="statusFilter" class="toolbar-item status-select" :options="statusOptions" />
    </div>

    <n-data-table
      :loading="loading"
      :columns="columns"
      :data="pagedFileList"
      :bordered="false"
    />

    <div class="pagination-wrapper">
      <n-pagination
        v-model:page="currentPage"
        v-model:page-size="pageSize"
        :item-count="filteredFileList.length"
        :page-sizes="[10, 20, 50]"
        show-size-picker
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { NDataTable, NInput, NSelect, NPagination, NTag, NButton, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { deleteFile } from '@/api/knowledge'
import type { KbFileUpload } from '@/api/knowledge'
import { KNOWLEDGE_FILE_STATUS } from '@/api/knowledge'
import { useKnowledgeFiles } from '@/composables/useKnowledgeFiles'
import { resolveKnowledgeErrorMessage } from '@/composables/useKnowledgeErrorMessage'
import { useKnowledgeFileFilters } from '@/composables/useKnowledgeFileFilters'

const { t, locale } = useI18n()
const message = useMessage()
const dialog = useDialog()

const { loading, fileList, loadFileList } = useKnowledgeFiles({
  onLoadError: (error: any) => {
    message.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.load_failed'))
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

const columns: DataTableColumns<KbFileUpload> = [
  {
    title: t('analytics.knowledge.file_name'),
    key: 'fileName',
    minWidth: 200,
    render: (row) => h('span', { style: 'display: flex; align-items: center;' }, [
      h('svg', { viewBox: '0 0 24 24', fill: 'currentColor', width: '16', height: '16', style: 'margin-right: 8px;' }, [
        h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6z' }),
        h('polyline', { points: '14 2 14 8 20 8' })
      ]),
      row.fileName
    ])
  },
  {
    title: t('analytics.knowledge.file_size'),
    key: 'totalSize',
    width: 120,
    render: (row) => formatFileSize(row.totalSize)
  },
  {
    title: t('analytics.knowledge.status'),
    key: 'status',
    width: 120,
    render: (row) => h(NTag, { type: getStatusType(row.status) as any, bordered: false }, { default: () => getStatusText(row.status) })
  },
  {
    title: t('analytics.knowledge.visibility'),
    key: 'isPublic',
    width: 100,
    render: (row) => h(NTag, { type: row.isPublic ? 'success' : 'default', size: 'small', bordered: false }, {
      default: () => row.isPublic ? t('analytics.knowledge.public') : t('analytics.knowledge.private')
    })
  },
  {
    title: t('analytics.knowledge.upload_time'),
    key: 'createdAt',
    width: 180,
    render: (row) => formatDate(row.createdAt)
  },
  {
    title: t('commons.operating'),
    key: 'actions',
    width: 100,
    fixed: 'right',
    render: (row) => h(NButton, {
      type: 'error',
      size: 'small',
      text: true,
      onClick: () => handleDelete(row)
    }, { default: () => t('commons.delete') })
  }
]

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
  dialog.warning({
    title: t('commons.prompt'),
    content: t('analytics.knowledge.delete_confirm', { name: row.fileName }),
    positiveText: t('commons.confirm'),
    negativeText: t('commons.cancel'),
    onPositiveClick: async () => {
      try {
        await deleteFile(row.id)
        message.success(t('commons.delete_success'))
        await loadFileList()
      } catch (error: any) {
        message.error(resolveKnowledgeErrorMessage(error, t, 'commons.delete_failed'))
      }
    }
  })
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
