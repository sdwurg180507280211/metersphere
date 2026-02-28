<template>
  <div class="source-list">
    <div class="source-header">{{ t('analytics.knowledge.chat_sources') }}</div>
    <div v-for="(item, index) in sources" :key="`${item.fileName}-${item.chunkId}-${index}`" class="source-item">
      <div class="source-meta">
        <span class="source-file">{{ item.fileName }}</span>
        <span class="source-score" v-if="typeof item.score === 'number'">{{ t('analytics.knowledge.score_label') }} {{ item.score.toFixed(3) }}</span>
      </div>
      <div class="source-snippet">{{ getSnippet(item, index) }}</div>
      <el-button text size="small" @click="toggleExpand(index)">
        {{ expandedMap[index] ? t('analytics.knowledge.collapse') : t('analytics.knowledge.expand') }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ChatSource } from '@/api/knowledge-chat'

const props = defineProps<{
  sources: ChatSource[]
}>()

const { t } = useI18n()
const expandedMap = reactive<Record<number, boolean>>({})

const getSnippet = (source: ChatSource, index: number) => {
  if (expandedMap[index]) {
    return source.snippet
  }
  if (source.snippet.length <= 100) {
    return source.snippet
  }
  return `${source.snippet.slice(0, 100)}...`
}

const toggleExpand = (index: number) => {
  expandedMap[index] = !expandedMap[index]
}
</script>

<style scoped>
.source-list {
  margin-top: 10px;
  border-top: 1px dashed #dcdfe6;
  padding-top: 8px;
}

.source-header {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.source-item {
  padding: 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 8px;
}

.source-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
}

.source-file {
  font-weight: 600;
  color: #303133;
}

.source-score {
  color: #909399;
}

.source-snippet {
  color: #606266;
  line-height: 1.6;
  font-size: 13px;
}
</style>
