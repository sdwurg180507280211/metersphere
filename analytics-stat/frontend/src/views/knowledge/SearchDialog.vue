<template>
  <n-modal
    v-model:show="visible"
    preset="dialog"
    :title="t('analytics.knowledge.search_dialog_title')"
    style="width: 720px"
    :mask-closable="false"
    @after-leave="handleClose"
  >
    <div class="search-bar">
      <n-input
        v-model:value="query"
        :placeholder="t('analytics.knowledge.search_placeholder')"
        clearable
        size="large"
        @keyup.enter="doSearch"
      >
        <template #suffix>
          <n-button :loading="loading" @click="doSearch" type="primary" size="small">
            {{ t('analytics.knowledge.search') }}
          </n-button>
        </template>
      </n-input>
      <div class="search-options">
        <span class="option-label">{{ t('analytics.knowledge.result_count_label') }}</span>
        <n-input-number v-model:value="topK" :min="1" :max="50" size="small" />
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
          <n-tag v-if="item.isPublic" size="small" type="success" :bordered="false">{{ t('analytics.knowledge.public') }}</n-tag>
          <span class="result-score">{{ t('analytics.knowledge.score_label') }}: {{ item.score?.toFixed(4) }}</span>
        </div>
        <div class="result-content">{{ item.textContent }}</div>
      </div>
    </div>

    <n-empty
      v-else-if="searched && !loading"
      :description="t('analytics.knowledge.no_result')"
    />
  </n-modal>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { NModal, NInput, NButton, NInputNumber, NTag, NEmpty, useMessage } from 'naive-ui'
import { EMPTY_QUERY_ERROR, useKnowledgeSearch } from '@/composables/useKnowledgeSearch'
import { resolveKnowledgeErrorMessage } from '@/composables/useKnowledgeErrorMessage'

const visible = defineModel<boolean>({ default: false })
const { t } = useI18n()
const message = useMessage()

const { query, topK, loading, searched, results, search, reset } = useKnowledgeSearch()

async function doSearch() {
  try {
    const searchResult = await search()
    if (searchResult.length === 0) {
      message.info(t('analytics.knowledge.no_result'))
    }
  } catch (e: any) {
    if (e?.message === EMPTY_QUERY_ERROR) {
      message.warning(t('analytics.knowledge.input_required'))
      return
    }
    message.error(resolveKnowledgeErrorMessage(e, t, 'analytics.knowledge.search_failed'))
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
