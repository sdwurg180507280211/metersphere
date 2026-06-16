<template>
  <div class="sql-query-page">
    <section class="sql-query-main">
      <div class="sql-query-toolbar">
        <div class="toolbar-title">
          <h2>{{ $t('sql_query.title') }}</h2>
          <span :class="['connection-badge', status.connected ? 'is-online' : 'is-offline']">
            <i></i>
            {{ connectionText }}
          </span>
        </div>
        <div class="toolbar-actions">
          <el-button size="small" icon="el-icon-refresh" @click="loadStatus">
            {{ $t('sql_query.refresh') }}
          </el-button>
          <el-button size="small" icon="el-icon-magic-stick" @click="formatSql">
            {{ $t('sql_query.format') }}
          </el-button>
          <el-button size="small" icon="el-icon-delete" @click="clearSql">
            {{ $t('sql_query.clear') }}
          </el-button>
          <el-input-number
            v-model="limit"
            class="limit-input"
            size="small"
            :min="1"
            :max="5000"
            :step="100"
            controls-position="right"/>
          <el-button
            type="primary"
            size="small"
            icon="el-icon-video-play"
            :loading="loading"
            :disabled="!sql.trim()"
            @click="execute">
            {{ $t('sql_query.run') }}
          </el-button>
        </div>
      </div>

      <div class="sql-editor">
        <div class="editor-gutter">
          <span v-for="line in lineNumbers" :key="line">{{ line }}</span>
        </div>
        <textarea
          ref="sqlEditor"
          v-model="sql"
          class="sql-textarea"
          spellcheck="false"
          :placeholder="$t('sql_query.placeholder')"
          @keydown="handleKeydown"/>
      </div>

      <div class="sql-result">
        <div v-if="!result && !error && !loading" class="result-empty">
          <i class="el-icon-data-analysis"></i>
          <p>{{ $t('sql_query.empty') }}</p>
          <div class="quick-hints">
            <span>Ctrl / Cmd + Enter</span>
            <span>{{ $t('sql_query.select_only') }}</span>
            <span>{{ $t('sql_query.max_rows', { count: 5000 }) }}</span>
          </div>
        </div>

        <div v-if="loading && !result" class="result-loading">
          <i class="el-icon-loading"></i>
          <p>{{ $t('sql_query.loading') }}</p>
        </div>

        <div v-if="error" class="result-error">
          <div class="error-title">
            <i class="el-icon-warning-outline"></i>
            {{ $t('sql_query.failed') }}
          </div>
          <pre>{{ error }}</pre>
        </div>

        <div v-if="result" class="result-panel">
          <div class="result-meta">
            <div class="meta-left">
              <el-tag size="mini" type="success">{{ $t('sql_query.success') }}</el-tag>
              <span>{{ $t('sql_query.rows', { count: result.rowCount || 0 }) }}</span>
              <span>{{ $t('sql_query.cost', { time: result.executionTime || 0 }) }}</span>
              <el-tag v-if="result.truncated" size="mini" type="warning">
                {{ $t('sql_query.truncated') }}
              </el-tag>
            </div>
            <div class="meta-right">
              <el-button type="text" size="mini" :disabled="!hasRows" @click="exportCsv">
                {{ $t('sql_query.export_csv') }}
              </el-button>
              <el-button type="text" size="mini" @click="closeResult">
                {{ $t('sql_query.close') }}
              </el-button>
            </div>
          </div>

          <div class="table-wrap">
            <table class="result-table">
              <thead>
              <tr>
                <th class="row-index">#</th>
                <th v-for="column in result.columns" :key="column">{{ column }}</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(row, rowIndex) in pagedRows" :key="rowIndex">
                <td class="row-index">{{ (currentPage - 1) * pageSize + rowIndex + 1 }}</td>
                <td v-for="column in result.columns" :key="column" :title="stringifyValue(row[column])">
                  <span :class="cellClass(row[column])">{{ stringifyValue(row[column]) }}</span>
                </td>
              </tr>
              </tbody>
            </table>
          </div>

          <div v-if="totalPages > 1" class="result-pagination">
            <span>
              {{ $t('sql_query.page_info', {
                start: (currentPage - 1) * pageSize + 1,
                end: Math.min(currentPage * pageSize, result.rows.length),
                total: result.rows.length
              }) }}
            </span>
            <el-pagination
              small
              layout="prev, pager, next, sizes"
              :current-page="currentPage"
              :page-size="pageSize"
              :page-sizes="[50, 100, 200, 500, 1000]"
              :total="result.rows.length"
              @current-change="handleCurrentChange"
              @size-change="handlePageSizeChange"/>
          </div>
        </div>
      </div>
    </section>

    <aside class="sql-history">
      <div class="history-header">
        <h3>{{ $t('sql_query.history') }}</h3>
        <el-button v-if="history.length" type="text" size="mini" @click="clearHistory">
          {{ $t('sql_query.clear_history') }}
        </el-button>
      </div>
      <div class="history-list">
        <div v-if="!history.length" class="history-empty">
          {{ $t('sql_query.no_history') }}
        </div>
        <button
          v-for="(item, index) in history"
          :key="index"
          class="history-card"
          type="button"
          @click="loadHistory(item.sql)">
          <span class="history-sql">{{ item.sql }}</span>
          <span class="history-time">{{ formatTime(item.timestamp) }}</span>
        </button>
      </div>
    </aside>
  </div>
</template>

<script>
import { executeSqlQuery, getSqlQueryStatus } from '@/api/sql-query';

const HISTORY_KEY = 'workstation-sql-query-history';

export default {
  name: 'SqlQuery',
  data() {
    return {
      sql: '',
      limit: 1000,
      loading: false,
      status: {
        connected: false
      },
      result: null,
      error: '',
      history: [],
      currentPage: 1,
      pageSize: 100
    };
  },
  computed: {
    lineNumbers() {
      return Math.max(this.sql.split('\n').length, 1);
    },
    connectionText() {
      if (this.status.connected) {
        return this.status.database || this.$t('sql_query.connected');
      }
      return this.status.message || this.$t('sql_query.disconnected');
    },
    hasRows() {
      return this.result && this.result.rows && this.result.rows.length > 0;
    },
    totalPages() {
      if (!this.hasRows) {
        return 1;
      }
      return Math.max(1, Math.ceil(this.result.rows.length / this.pageSize));
    },
    pagedRows() {
      if (!this.hasRows) {
        return [];
      }
      const start = (this.currentPage - 1) * this.pageSize;
      return this.result.rows.slice(start, start + this.pageSize);
    }
  },
  mounted() {
    this.loadStatus();
    this.loadLocalHistory();
  },
  methods: {
    async loadStatus() {
      try {
        const response = await getSqlQueryStatus();
        this.status = response.data || { connected: false };
      } catch (e) {
        this.status = {
          connected: false,
          message: e.message || this.$t('sql_query.status_failed')
        };
      }
    },
    async execute() {
      if (!this.sql.trim()) {
        return;
      }
      this.loading = true;
      this.error = '';
      this.currentPage = 1;
      try {
        const response = await executeSqlQuery(this.sql, this.limit);
        this.result = response.data;
        this.saveHistory(this.sql);
      } catch (e) {
        this.error = e.message || e.data || this.$t('sql_query.failed');
      } finally {
        this.loading = false;
      }
    },
    handleKeydown(event) {
      if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
        event.preventDefault();
        this.execute();
      }
    },
    formatSql() {
      const keywords = ['select', 'from', 'where', 'and', 'or', 'group by', 'order by', 'limit', 'left join', 'right join', 'inner join', 'on', 'as', 'having', 'in', 'is', 'not', 'null', 'like', 'between'];
      let formatted = this.sql;
      keywords.forEach(keyword => {
        formatted = formatted.replace(new RegExp(`\\b${keyword}\\b`, 'gi'), keyword.toUpperCase());
      });
      this.sql = formatted;
    },
    clearSql() {
      this.sql = '';
      this.error = '';
      this.result = null;
      this.$nextTick(() => {
        if (this.$refs.sqlEditor) {
          this.$refs.sqlEditor.focus();
        }
      });
    },
    closeResult() {
      this.result = null;
      this.error = '';
    },
    loadLocalHistory() {
      try {
        this.history = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]');
      } catch (e) {
        this.history = [];
      }
    },
    saveHistory(sql) {
      const item = {
        sql,
        timestamp: Date.now()
      };
      this.history = [item, ...this.history.filter(history => history.sql !== sql)].slice(0, 50);
      localStorage.setItem(HISTORY_KEY, JSON.stringify(this.history));
    },
    clearHistory() {
      this.history = [];
      localStorage.removeItem(HISTORY_KEY);
    },
    handleCurrentChange(page) {
      this.currentPage = page;
    },
    handlePageSizeChange(size) {
      this.pageSize = size;
      this.currentPage = 1;
    },
    loadHistory(sql) {
      this.sql = sql;
      this.$nextTick(() => {
        if (this.$refs.sqlEditor) {
          this.$refs.sqlEditor.focus();
        }
      });
    },
    exportCsv() {
      if (!this.hasRows) {
        return;
      }
      const headers = this.result.columns;
      const rows = this.result.rows.map(row => headers.map(column => this.csvValue(row[column])).join(','));
      const content = [headers.join(','), ...rows].join('\n');
      const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `sql_result_${Date.now()}.csv`;
      link.click();
      URL.revokeObjectURL(link.href);
    },
    csvValue(value) {
      const text = this.stringifyValue(value);
      if (text.includes(',') || text.includes('"') || text.includes('\n')) {
        return `"${text.replace(/"/g, '""')}"`;
      }
      return text;
    },
    stringifyValue(value) {
      if (value === null || value === undefined) {
        return 'NULL';
      }
      if (typeof value === 'object') {
        return JSON.stringify(value);
      }
      return String(value);
    },
    cellClass(value) {
      if (value === null || value === undefined) {
        return 'cell-null';
      }
      if (typeof value === 'number') {
        return 'cell-number';
      }
      if (typeof value === 'boolean') {
        return 'cell-boolean';
      }
      return '';
    },
    formatTime(timestamp) {
      return new Date(timestamp).toLocaleString();
    }
  }
};
</script>

<style scoped>
.sql-query-page {
  display: flex;
  height: calc(100vh - 57px);
  min-height: 640px;
  background: #f5f7fa;
  color: #1f2933;
}

.sql-query-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #ffffff;
  border-right: 1px solid #e6e6e6;
}

.sql-query-toolbar {
  min-height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 24px;
  border-bottom: 1px solid #e6e6e6;
  background: #ffffff;
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.toolbar-title h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2933;
  white-space: nowrap;
}

.connection-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  max-width: 360px;
  padding: 4px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #606266;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.connection-badge i {
  width: 7px;
  height: 7px;
  flex: none;
  border-radius: 50%;
  background: #f56c6c;
}

.connection-badge.is-online i {
  background: #67c23a;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.limit-input {
  width: 116px;
}

.sql-editor {
  height: 240px;
  display: flex;
  background: #1f2937;
  border-bottom: 1px solid #dcdfe6;
}

.editor-gutter {
  width: 48px;
  padding-top: 16px;
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #111827;
  color: #6b7280;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  user-select: none;
}

.editor-gutter span {
  height: 21px;
  line-height: 21px;
}

.sql-textarea {
  flex: 1;
  min-width: 0;
  padding: 16px;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  color: #f9fafb;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 14px;
  line-height: 1.5;
}

.sql-textarea::placeholder {
  color: #9ca3af;
}

.sql-result {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #ffffff;
}

.result-empty,
.result-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.result-empty i,
.result-loading i {
  font-size: 42px;
  margin-bottom: 14px;
  color: #c0c4cc;
}

.quick-hints {
  display: flex;
  gap: 12px;
  margin-top: 18px;
  flex-wrap: wrap;
  justify-content: center;
}

.quick-hints span {
  padding: 4px 10px;
  border-radius: 4px;
  background: #f2f6fc;
  color: #606266;
  font-size: 12px;
}

.result-error {
  margin: 24px;
  padding: 18px;
  border: 1px solid #fbc4c4;
  border-radius: 4px;
  background: #fef0f0;
  color: #f56c6c;
}

.error-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
}

.result-error pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.result-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.result-meta {
  min-height: 44px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
  font-size: 13px;
  color: #606266;
}

.meta-left,
.meta-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.result-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.result-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 10px 12px;
  text-align: left;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
  color: #606266;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.result-table td {
  max-width: 320px;
  padding: 9px 12px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-table tr:hover td {
  background: #f5f7fa;
}

.row-index {
  width: 52px;
  text-align: center !important;
  color: #909399 !important;
  background: #fafafa;
}

.cell-null {
  color: #909399;
  font-style: italic;
}

.cell-number {
  color: #e6a23c;
}

.cell-boolean {
  color: #409eff;
}

.result-pagination {
  min-height: 44px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-top: 1px solid #ebeef5;
  background: #ffffff;
  color: #606266;
  font-size: 12px;
}

.sql-history {
  width: 300px;
  flex: none;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.history-header {
  min-height: 64px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e6e6;
}

.history-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.history-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
}

.history-card {
  width: 100%;
  display: block;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
}

.history-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.history-sql {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #303133;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
}

.history-time {
  display: block;
  margin-top: 8px;
  color: #909399;
  font-size: 11px;
  text-align: right;
}

.history-empty {
  padding-top: 40px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .sql-query-page {
    height: auto;
    min-height: calc(100vh - 57px);
    flex-direction: column;
  }

  .sql-history {
    width: auto;
    max-height: 260px;
    border-top: 1px solid #e6e6e6;
  }
}
</style>
