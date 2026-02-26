<template>
  <!-- 知识库检索对话框 -->
  <el-dialog
    v-model="visible"
    title="知识库检索"
    width="720px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 搜索输入区 -->
    <div class="search-bar">
      <el-input
        v-model="query"
        placeholder="输入关键词搜索知识库..."
        clearable
        size="large"
        @keyup.enter="doSearch"
      >
        <template #append>
          <el-button :icon="Search" :loading="loading" @click="doSearch">
            搜索
          </el-button>
        </template>
      </el-input>
      <div class="search-options">
        <span class="option-label">返回数量：</span>
        <el-input-number v-model="topK" :min="1" :max="50" size="small" />
      </div>
    </div>

    <!-- 搜索结果列表 -->
    <div v-if="results.length > 0" class="result-list">
      <div
        v-for="(item, index) in results"
        :key="`${item.fileMd5}-${item.chunkId}`"
        class="result-item"
      >
        <div class="result-header">
          <span class="result-index">#{{ index + 1 }}</span>
          <span class="result-filename">{{ item.fileName || item.fileMd5 }}</span>
          <el-tag v-if="item.isPublic" size="small" type="success">公开</el-tag>
          <span class="result-score">得分: {{ item.score?.toFixed(4) }}</span>
        </div>
        <div class="result-content">{{ item.textContent }}</div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-else-if="searched && !loading"
      description="未找到相关内容"
    />
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * 知识库检索对话框
 *
 * 从 PaiSmart 的 Naive UI SearchDialog 迁移到 Element Plus
 * 主要变化：
 * - NModal → el-dialog
 * - NInput → el-input
 * - NTag → el-tag
 * - NEmpty → el-empty
 */
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { hybridSearch } from '@/api/knowledge'
import type { SearchResult } from '@/api/knowledge'

/** 对话框可见性（v-model） */
const visible = defineModel<boolean>({ default: false })

const query = ref('')
const topK = ref(10)
const loading = ref(false)
const searched = ref(false)
const results = ref<SearchResult[]>([])

/** 执行搜索 */
async function doSearch() {
  const q = query.value.trim()
  if (!q) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  loading.value = true
  searched.value = true
  results.value = []

  try {
    results.value = await hybridSearch(q, topK.value)
    if (results.value.length === 0) {
      ElMessage.info('未找到相关内容')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '检索失败')
  } finally {
    loading.value = false
  }
}

/** 关闭时重置状态 */
function handleClose() {
  query.value = ''
  results.value = []
  searched.value = false
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 16px;
}

.search-options {
  display: flex;
  align-items: center;
  margin-top: 8px;
  gap: 8px;
}

.option-label {
  font-size: 13px;
  color: #606266;
}

.result-list {
  max-height: 400px;
  overflow-y: auto;
}

.result-item {
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  transition: border-color 0.2s;
}

.result-item:hover {
  border-color: #409eff;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
}

.result-index {
  color: #409eff;
  font-weight: 600;
}

.result-filename {
  font-weight: 500;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-score {
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}

.result-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  /* 最多显示6行，超出省略 */
  display: -webkit-box;
  -webkit-line-clamp: 6;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
