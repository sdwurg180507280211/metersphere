<template>
  <!-- 知识库主页面 -->
  <div class="knowledge-base-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h3 class="page-title">{{ t('analytics.knowledge.title') }}</h3>
      <div class="header-actions">
        <el-button type="primary" :icon="Upload" @click="showUpload = true">
          {{ t('analytics.knowledge.upload_file') }}
        </el-button>
        <el-button type="primary" :icon="Search" @click="showSearch = true">
          {{ t('analytics.knowledge.search') }}
        </el-button>
      </div>
    </div>

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

    <!-- 文件列表 -->
    <el-card class="file-list-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('analytics.knowledge.my_files') }}</span>
          <el-button size="small" :icon="Refresh" @click="fileListRef?.loadFileList()">
            {{ t('commons.refresh') }}
          </el-button>
        </div>
      </template>
      <FileList ref="fileListRef" />
    </el-card>

    <!-- 检索对话框 -->
    <SearchDialog v-model="showSearch" />

    <!-- 上传对话框 -->
    <UploadDialog v-model="showUpload" @success="fileListRef?.loadFileList()" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Document, Lock, Upload, Refresh } from '@element-plus/icons-vue'
import SearchDialog from './knowledge/SearchDialog.vue'
import UploadDialog from './knowledge/UploadDialog.vue'
import FileList from './knowledge/FileList.vue'

const { t } = useI18n()
const showSearch = ref(false)
const showUpload = ref(false)
const fileListRef = ref<InstanceType<typeof FileList>>()
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

.feature-card {
  height: 160px;
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
