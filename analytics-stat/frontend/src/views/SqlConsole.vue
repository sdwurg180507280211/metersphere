<template>
  <div class="sql-console">
    <el-card>
      <!-- 卡片头部 -->
      <div slot="header" class="header">
        <span>SQL查询台</span>
        <el-button type="primary" icon="el-icon-video-play" @click="executeQuery">
          执行查询
        </el-button>
      </div>

      <!-- SQL 编辑器 -->
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

      <!-- 查询结果 -->
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

<script>
/**
 * SQL 查询台页面
 * 
 * 功能：
 * 1. 提供 SQL 输入框
 * 2. 执行 SQL 查询
 * 3. 展示查询结果
 */
export default {
  name: 'SqlConsole',
  data() {
    return {
      // SQL 查询语句
      sqlQuery: '',
      // 查询结果数据
      queryResult: [],
      // 结果列名
      resultColumns: []
    };
  },
  methods: {
    /**
     * 执行 SQL 查询
     */
    executeQuery() {
      if (!this.sqlQuery.trim()) {
        this.$message.warning('请输入SQL查询语句');
        return;
      }

      // TODO: 调用后端 API 执行查询
      this.$message.info('查询功能开发中...');
    }
  }
};
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

.sql-editor >>> .el-textarea__inner {
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
