<template>
  <!-- 知识库主页面 -->
  <div class="knowledge-base-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h3 class="page-title">{{ t('analytics.knowledge.title') }}</h3>
      <div class="header-actions">
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

    <!-- 检索对话框 -->
    <SearchDialog v-model="showSearch" />
  </div>
</template>

<script setup lang="ts">
/**
 * 知识库主页面
 *
 * Phase 1: 提供检索入口，通过 SearchDialog 执行混合检索
 * Phase 2: 将增加文件上传、文档管理等功能
 */
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Document, Lock } from '@element-plus/icons-vue'
import SearchDialog from './knowledge/SearchDialog.vue'

const { t } = useI18n()
const showSearch = ref(false)
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

.feature-cards {
  margin-top: 16px;
}

.feature-card {
  height: 160px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.card-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}
</style>
