<template>
  <n-config-provider>
    <n-message-provider>
      <n-dialog-provider>
        <!-- 知识库主页面 -->
        <div class="knowledge-base-page">
          <!-- 页面头部 -->
          <div class="page-header">
            <h3 class="page-title">{{ t('analytics.knowledge.title') }}</h3>
            <div class="header-actions">
              <n-button type="primary" @click="openUploadDialog">
                <template #icon>
                  <n-icon><UploadIcon /></n-icon>
                </template>
                {{ t('analytics.knowledge.upload_file') }}
              </n-button>
              <n-button type="primary" @click="openSearchDialog">
                <template #icon>
                  <n-icon><SearchIcon /></n-icon>
                </template>
                {{ t('analytics.knowledge.search') }}
              </n-button>
            </div>
          </div>

          <n-alert
            class="intro-alert"
            type="info"
            :closable="false"
            :title="t('analytics.knowledge.module_intro_title')"
          >
            {{ t('analytics.knowledge.module_intro_desc') }}
          </n-alert>

          <!-- 功能说明卡片 -->
          <n-grid :cols="3" :x-gap="16" class="feature-cards">
            <n-gi>
              <n-card :bordered="true" class="feature-card" hoverable>
                <template #header>
                  <div class="card-header">
                    <n-icon size="20"><SearchIcon /></n-icon>
                    <span>{{ t('analytics.knowledge.hybrid_search') }}</span>
                  </div>
                </template>
                <p class="card-desc">{{ t('analytics.knowledge.hybrid_search_desc') }}</p>
              </n-card>
            </n-gi>
            <n-gi>
              <n-card :bordered="true" class="feature-card" hoverable>
                <template #header>
                  <div class="card-header">
                    <n-icon size="20"><DocumentIcon /></n-icon>
                    <span>{{ t('analytics.knowledge.doc_manage') }}</span>
                  </div>
                </template>
                <p class="card-desc">{{ t('analytics.knowledge.doc_manage_desc') }}</p>
              </n-card>
            </n-gi>
            <n-gi>
              <n-card :bordered="true" class="feature-card" hoverable>
                <template #header>
                  <div class="card-header">
                    <n-icon size="20"><LockIcon /></n-icon>
                    <span>{{ t('analytics.knowledge.permission') }}</span>
                  </div>
                </template>
                <p class="card-desc">{{ t('analytics.knowledge.permission_desc') }}</p>
              </n-card>
            </n-gi>
          </n-grid>

          <n-grid :cols="2" :x-gap="16" class="guide-cards">
            <n-gi>
              <n-card :bordered="true" class="guide-card">
                <template #header>
                  <div class="card-header">
                    <span>{{ t('analytics.knowledge.workflow_title') }}</span>
                  </div>
                </template>
                <ol class="workflow-list">
                  <li v-for="step in workflowSteps" :key="step" class="workflow-step">
                    {{ t(step) }}
                  </li>
                </ol>
              </n-card>
            </n-gi>
            <n-gi>
              <n-card :bordered="true" class="guide-card">
                <template #header>
                  <div class="card-header">
                    <span>{{ t('analytics.knowledge.best_practice_title') }}</span>
                  </div>
                </template>
                <ul class="practice-list">
                  <li v-for="item in bestPracticeItems" :key="item" class="practice-item">
                    {{ t(item) }}
                  </li>
                </ul>
              </n-card>
            </n-gi>
          </n-grid>

          <!-- 文件列表 -->
          <n-card class="file-list-card" :bordered="true">
            <template #header>
              <div class="card-header">
                <span>{{ t('analytics.knowledge.my_files') }}</span>
                <n-button size="small" @click="refreshFileList">
                  <template #icon>
                    <n-icon><RefreshIcon /></n-icon>
                  </template>
                  {{ t('commons.refresh') }}
                </n-button>
              </div>
            </template>
            <FileList ref="fileListRef" />
          </n-card>

          <!-- 检索对话框 -->
          <SearchDialog v-model="showSearch" />

          <!-- 上传对话框 -->
          <UploadDialog v-model="showUpload" @success="handleUploadSuccess" />
        </div>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { NConfigProvider, NMessageProvider, NDialogProvider, NButton, NIcon, NAlert, NGrid, NGi, NCard } from 'naive-ui'
import { Search as SearchIcon, Document as DocumentIcon, LockClosed as LockIcon, CloudUpload as UploadIcon, Refresh as RefreshIcon } from '@vicons/ionicons5'
import SearchDialog from './knowledge/SearchDialog.vue'
import UploadDialog from './knowledge/UploadDialog.vue'
import FileList from './knowledge/FileList.vue'
import { useKnowledgeBasePage } from '@/composables/useKnowledgeBasePage'

const { t } = useI18n()
const {
  showSearch,
  showUpload,
  fileListRef,
  openSearchDialog,
  openUploadDialog,
  refreshFileList,
  handleUploadSuccess,
} = useKnowledgeBasePage()

const workflowSteps = [
  'analytics.knowledge.workflow_step_upload',
  'analytics.knowledge.workflow_step_parse',
  'analytics.knowledge.workflow_step_search',
]

const bestPracticeItems = [
  'analytics.knowledge.practice_file_naming',
  'analytics.knowledge.practice_content_split',
  'analytics.knowledge.practice_permission',
]
</script>

<style scoped>
.knowledge-base-page {
  padding: 20px;
  min-height: 100%;
  background-color: #f5f6f7;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.intro-alert {
  margin-bottom: 16px;
}

.feature-cards {
  margin-top: 16px;
}

.feature-card {
  height: 160px;
}

.guide-cards {
  margin-top: 16px;
}

.guide-card {
  min-height: 180px;
}

.workflow-list,
.practice-list {
  margin: 0;
  padding-left: 18px;
}

.workflow-step,
.practice-item {
  line-height: 1.8;
  color: #606266;
}

.file-list-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  gap: 8px;
}

.card-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}
</style>
