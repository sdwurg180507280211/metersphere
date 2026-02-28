<template>
  <el-dialog
    v-model="visible"
    :title="t('analytics.knowledge.search_dialog_title')"
    width="720px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="search-bar">
      <el-input
        v-model="query"
        :placeholder="t('analytics.knowledge.search_placeholder')"
        clearable
        size="large"
        @keyup.enter="doSearch"
      >
        <template #append>
          <el-button :icon="Search" :loading="loading" @click="doSearch">
            {{ t('analytics.knowledge.search') }}
          </el-button>
        </template>
      </el-input>
      <div class="search-options">
        <span class="option-label">{{ t('analytics.knowledge.result_count_label') }}</span>
        <el-input-number v-model="topK" :min="1" :max="50" size="small" />
      </div>
    </div>

    <div v-if="results.length > 0" class="result-list">
      <div
        v-for="(item, index) in results"
        :key="`${item.fileMd5}-${item.chunkId}`"
        class="result-item"
      >
        <div class="result-header">
          <span class="result-index">#{{ index + 1 }}</span>
          <span class="result-filename">{{ item.fileName || item.fileMd5 }}</span>
          <el-tag v-if="item.isPublic" size="small" type="success">{{ t('analytics.knowledge.public') }}</el-tag>
          <span class="result-score">{{ t('analytics.knowledge.score_label') }}: {{ item.score?.toFixed(4) }}</span>
        </div>
        <div class="result-content">{{ item.textContent }}</div>
      </div>
    </div>

    <el-empty
      v-else-if="searched && !loading"
      :description="t('analytics.knowledge.no_result')"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { EMPTY_QUERY_ERROR, useKnowledgeSearch } from '@/composables/useKnowledgeSearch'
import { resolveKnowledgeErrorMessage } from '@/composables/useKnowledgeErrorMessage'

const visible = defineModel<boolean>({ default: false })
const { t } = useI18n()

const { query, topK, loading, searched, results, search, reset } = useKnowledgeSearch()

async function doSearch() {
  try {
    const searchResult = await search()
    if (searchResult.length === 0) {
      ElMessage.info(t('analytics.knowledge.no_result'))
    }
  } catch (e: any) {
    if (e?.message === EMPTY_QUERY_ERROR) {
      ElMessage.warning(t('analytics.knowledge.input_required'))
      return
    }
    ElMessage.error(resolveKnowledgeErrorMessage(e, t, 'analytics.knowledge.search_failed'))
  }
}

function handleClose() {
  reset()
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
