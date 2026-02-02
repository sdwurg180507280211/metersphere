<template>
  <div class="sql-console">
    <el-card>
      <template #header>
        <div class="header">
          <span>SQL查询台</span>
          <el-button type="primary" :icon="VideoPlay" @click="executeQuery">
            执行查询
          </el-button>
        </div>
      </template>

      <div class="editor-container">
        <el-input
          v-model="sqlQuery"
          type="textarea"
          :rows="10"
          placeholder="请输入SQL查询语句..."
          class="sql-editor"
        />
      </div>

      <el-divider />

      <div class="result-container">
        <h3>查询结果</h3>
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
        <el-empty v-else description="暂无查询结果" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const sqlQuery = ref('')
const queryResult = ref<any[]>([])
const resultColumns = ref<string[]>([])

const executeQuery = () => {
  if (!sqlQuery.value.trim()) {
    ElMessage.warning('请输入SQL查询语句')
    return
  }

  // TODO: 调用后端API执行查询
  ElMessage.info('查询功能开发中...')
}
</script>

<style scoped>
.sql-console {
  width: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-container {
  margin-bottom: 20px;
}

.sql-editor {
  font-family: 'Courier New', monospace;
}

.result-container {
  margin-top: 20px;
}

.result-container h3 {
  margin-bottom: 10px;
  color: #303133;
}
</style>
