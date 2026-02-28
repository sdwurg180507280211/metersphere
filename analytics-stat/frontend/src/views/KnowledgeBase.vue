<template>
  <!-- 知识库主页面 -->
  <div class="knowledge-base-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h3 class="page-title">{{ t('analytics.knowledge.title') }}</h3>
      <div class="header-actions">
        <el-button type="primary" :icon="Upload" @click="openUploadDialog">
          {{ t('analytics.knowledge.upload_file') }}
        </el-button>
        <el-button type="primary" :icon="Search" @click="openSearchDialog">
          {{ t('analytics.knowledge.search') }}
        </el-button>
        <el-button :icon="ChatDotRound" @click="goToChat">
          {{ t('analytics.menu.knowledge_chat') }}
        </el-button>
      </div>
    </div>

    <el-alert
      class="intro-alert"
      type="info"
      :closable="false"
      :title="t('analytics.knowledge.module_intro_title')"
      :description="t('analytics.knowledge.module_intro_desc')"
    />

    <!-- 功能说明卡片 -->
    <el-row :gutter="16" class="feature-cards">
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card">
          <template #header>
            <div class="card-header">
              <el-icon size="20"><Search /></el-icon>
              <span>{{ t('analytics.knowledge.hybrid_search') }}</span>
            </div>
          </template>
          <p class="card-desc">{{ t('analytics.knowledge.hybrid_search_desc') }}</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card">
          <template #header>
            <div class="card-header">
              <el-icon size="20"><Document /></el-icon>
              <span>{{ t('analytics.knowledge.doc_manage') }}</span>
            </div>
          </template>
          <p class="card-desc">{{ t('analytics.knowledge.doc_manage_desc') }}</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card">
          <template #header>
            <div class="card-header">
              <el-icon size="20"><Lock /></el-icon>
              <span>{{ t('analytics.knowledge.permission') }}</span>
            </div>
          </template>
          <p class="card-desc">{{ t('analytics.knowledge.permission_desc') }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="guide-cards">
      <el-col :span="12">
        <el-card shadow="never" class="guide-card">
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
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="guide-card">
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
        </el-card>
      </el-col>
    </el-row>

    <!-- 文件列表 -->
    <el-card class="file-list-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('analytics.knowledge.my_files') }}</span>
          <el-button size="small" :icon="Refresh" @click="refreshFileList">
            {{ t('commons.refresh') }}
          </el-button>
        </div>
      </template>
      <FileList ref="fileListRef" />
    </el-card>

    <!-- 检索对话框 -->
    <SearchDialog v-model="showSearch" />

    <!-- 上传对话框 -->
    <UploadDialog v-model="showUpload" @success="handleUploadSuccess" />
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Search, Document, Lock, Upload, Refresh, ChatDotRound } from '@element-plus/icons-vue'
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
