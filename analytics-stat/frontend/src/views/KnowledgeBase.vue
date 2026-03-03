<template>
  <div class="knowledge-base-page">
    <div class="page-header">
      <h3 class="page-title">{{ t('analytics.knowledge.title') }}</h3>
      <div class="header-actions">
        <n-button type="primary" @click="openUploadDialog">{{ t('analytics.knowledge.upload_file') }}</n-button>
        <n-button type="primary" @click="openSearchDialog">{{ t('analytics.knowledge.search') }}</n-button>
        <n-button @click="goToChat">{{ t('analytics.menu.knowledge_chat') }}</n-button>
      </div>
    </div>

    <n-alert class="intro-alert" type="info" :title="t('analytics.knowledge.module_intro_title')">
      {{ t('analytics.knowledge.module_intro_desc') }}
    </n-alert>

    <n-grid :x-gap="16" :y-gap="16" :cols="3" class="feature-cards">
      <n-grid-item>
        <n-card hoverable class="feature-card" :bordered="false">
          <template #header>
            <div class="card-header"><span>{{ t('analytics.knowledge.hybrid_search') }}</span></div>
          </template>
          <p class="card-desc">{{ t('analytics.knowledge.hybrid_search_desc') }}</p>
        </n-card>
      </n-grid-item>
      <n-grid-item>
        <n-card hoverable class="feature-card" :bordered="false">
          <template #header>
            <div class="card-header"><span>{{ t('analytics.knowledge.doc_manage') }}</span></div>
          </template>
          <p class="card-desc">{{ t('analytics.knowledge.doc_manage_desc') }}</p>
        </n-card>
      </n-grid-item>
      <n-grid-item>
        <n-card hoverable class="feature-card" :bordered="false">
          <template #header>
            <div class="card-header"><span>{{ t('analytics.knowledge.permission') }}</span></div>
          </template>
          <p class="card-desc">{{ t('analytics.knowledge.permission_desc') }}</p>
        </n-card>
      </n-grid-item>
    </n-grid>

    <n-grid :x-gap="16" :y-gap="16" :cols="2" class="guide-cards">
      <n-grid-item>
        <n-card class="guide-card" :bordered="false">
          <template #header>
            <div class="card-header"><span>{{ t('analytics.knowledge.workflow_title') }}</span></div>
          </template>
          <ol class="workflow-list">
            <li v-for="step in workflowSteps" :key="step" class="workflow-step">{{ t(step) }}</li>
          </ol>
        </n-card>
      </n-grid-item>
      <n-grid-item>
        <n-card class="guide-card" :bordered="false">
          <template #header>
            <div class="card-header"><span>{{ t('analytics.knowledge.best_practice_title') }}</span></div>
          </template>
          <ul class="practice-list">
            <li v-for="item in bestPracticeItems" :key="item" class="practice-item">{{ t(item) }}</li>
          </ul>
        </n-card>
      </n-grid-item>
    </n-grid>

    <n-card class="file-list-card" :bordered="false">
      <template #header>
        <div class="card-header">
          <span>{{ t('analytics.knowledge.my_files') }}</span>
          <n-button size="small" tertiary @click="refreshFileList">{{ t('commons.refresh') }}</n-button>
        </div>
      </template>
      <FileList ref="fileListRef" />
    </n-card>

    <SearchDialog v-model="showSearch" />
    <UploadDialog v-model="showUpload" @success="handleUploadSuccess" />
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { NButton, NAlert, NGrid, NGridItem, NCard } from 'naive-ui'
import SearchDialog from './knowledge/SearchDialog.vue'
import UploadDialog from './knowledge/UploadDialog.vue'
import FileList from './knowledge/FileList.vue'
import { useKnowledgeBasePage } from '@/composables/useKnowledgeBasePage'
import { KNOWLEDGE_ROUTE_PATHS } from '@/config/knowledge-route'

const { t } = useI18n()
const router = useRouter()
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

const goToChat = () => {
  router.push(KNOWLEDGE_ROUTE_PATHS.knowledgeChat)
}
</script>

<style scoped>
.knowledge-base-page {
  padding: 20px;
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

.feature-cards {
  margin-top: 16px;
}

.intro-alert {
  margin-bottom: 16px;
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
}

.card-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}
</style>
