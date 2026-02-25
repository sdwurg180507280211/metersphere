<template>
  <div class="sql-console">
    <el-card shadow="never">
      <!-- 卡片头部 -->
      <template #header>
        <div class="header">
          <span>{{ t('analytics.sql_console') }}</span>
          <el-button type="primary" :icon="VideoPlay" @click="executeQuery">
            {{ t('analytics.execute_query') }}
          </el-button>
        </div>
      </template>

      <!-- SQL 编辑器 -->
      <div class="editor-container">
        <el-input
          v-model="sqlQuery"
          type="textarea"
          :rows="10"
          :placeholder="t('analytics.sql_placeholder')"
          class="sql-editor"
        />
      </div>

      <el-divider />

      <!-- 查询结果 -->
      <div class="result-container">
        <h3>{{ t('analytics.query_result') }}</h3>
        <el-table
          v-if="queryResult.length > 0"
          :data="queryResult"
          border
          style="width: 100%; margin-top: 20px;"
        >
          <el-table-column
            v-for="column in resultColumns"
            :key="column"
            :prop="column"
            :label="column"
          />
        </el-table>
        <el-empty v-else :description="t('analytics.no_query_result')" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
/**
 * SQL 查询台页面
 *
 * 功能：
 * 1. 提供 SQL 输入框
 * 2. 执行 SQL 查询
 * 3. 展示查询结果（表格形式）
 *
 * TODO: 后续可集成 Monaco Editor 替代 textarea，提供语法高亮
 */
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'

const { t } = useI18n()

/** SQL 查询语句 */
const sqlQuery = ref('')
/** 查询结果数据 */
const queryResult = ref<Record<string, any>[]>([])
/** 结果列名 */
const resultColumns = ref<string[]>([])

/** 执行 SQL 查询 */
function executeQuery() {
  if (!sqlQuery.value.trim()) {
    ElMessage.warning(t('analytics.sql_empty_warning'))
    return
  }
  // TODO: 调用后端 API 执行查询
  ElMessage.info(t('analytics.feature_in_development'))
}
</script>

<style scoped>
.sql-console {
  width: 100%;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-container {
  margin-bottom: 20px;
}

/* textarea 使用等宽字体 */
:deep(.sql-editor .el-textarea__inner) {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 14px;
}

.result-container {
  margin-top: 20px;
}

.result-container h3 {
  margin-bottom: 10px;
  color: #303133;
  font-size: 16px;
}
</style>
