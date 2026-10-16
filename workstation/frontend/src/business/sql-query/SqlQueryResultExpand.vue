<template>
  <section :class="['sql-query-workbench', { 'history-hidden': !historyVisible }]">
    <div class="workbench-actions">
      <div class="workbench-action-group">
        <el-button size="mini" :disabled="!canRun" @click="run('current')">{{ text('current') }}</el-button>
        <el-button size="mini" :disabled="!canRun" @click="run('all')">{{ text('all') }}</el-button>
        <el-button size="mini" :disabled="!execution" @click="showExecution(execution)">{{ text('details') }}</el-button>
        <el-button size="mini" :disabled="!core || !core.executionLog.length" @click="logVisible = true">{{ text('log') }}</el-button>
      </div>
      <div class="workbench-action-group">
        <el-button size="mini" :disabled="!core || !core.hasRows" @click="copyPage">{{ text('copyPage') }}</el-button>
        <el-button size="mini" @click="historyVisible = !historyVisible">{{ text(historyVisible ? 'hideHistory' : 'showHistory') }}</el-button>
      </div>
    </div>
    <div class="workbench-hint">{{ text('hint') }}<br>{{ text('batchPolicy') }}</div>
    <div class="workbench-status" role="status" aria-live="polite">
      <el-alert v-if="core && core.loading" :title="text('running', { seconds: core.elapsedSeconds })" type="info" :closable="false"/>
      <el-alert v-if="core && core.storageWriteFailed" :title="text('storage')" type="error" :closable="false"/>
      <el-alert v-if="core && core.resultIsStale" :title="text('stale')" type="warning" :closable="false"/>
      <el-alert v-if="core && core.result && core.result.truncated" :title="text('truncated', { count: core.returnedRowCount })" type="warning" :closable="false"/>
    </div>
    <div v-if="core && core.queryBatch" class="batch-results-bar">
      <div class="batch-summary" role="status">
        <el-tag size="mini" :type="core.queryBatch.status === 'SUCCESS' ? 'success' : (core.queryBatch.status === 'LIMIT' ? 'warning' : 'danger')">
          {{ text(core.queryBatch.status) }}
        </el-tag>
        <span v-if="core.batchFailure">{{ text('batchError', { index: core.batchFailure.index }) }}</span>
        <span v-else>{{ text(core.queryBatch.status === 'LIMIT' ? 'batchLimit' : 'batchSuccess', { count: core.queryBatch.statements.length }) }}</span>
        <el-button size="mini" type="text" @click="showExecution(core.lastExecution)">{{ text('batchDetails') }}</el-button>
      </div>
      <div v-if="core.resultOptions.length" class="batch-result-tools">
        <div class="batch-result-tabs">
          <el-radio-group :value="core.activeResultKey" size="mini" @input="selectResult">
            <el-radio-button v-for="item in core.resultOptions" :key="item.key" :label="item.key">
              {{ text('resultTab', { statement: item.statementIndex, result: item.resultIndex }) }}
            </el-radio-button>
          </el-radio-group>
        </div>
        <el-button size="mini" type="text" @click="showResultSql">{{ text('resultSql') }}</el-button>
      </div>
      <div v-if="core.sourceResult" class="batch-filter-tools">
        <el-input size="mini" clearable prefix-icon="el-icon-search" :value="core.resultFilter"
          :placeholder="text('filterPlaceholder')" :aria-label="text('filterPlaceholder')" @input="filterResult"/>
        <span>{{ text('filterCount', { visible: core.result.rows.length, total: core.returnedRowCount }) }}</span>
        <el-button size="mini" :disabled="!core.hasRows" @click="copyFiltered">{{ text('copyFiltered') }}</el-button>
        <span v-if="core.resultFilter && !core.hasRows" class="batch-empty-filter">{{ text('noMatch') }}</span>
      </div>
      <span v-if="!core.resultOptions.length" class="batch-empty-filter">{{ text('emptyBatch') }}</span>
    </div>
    <workbench-core ref="console" class="workbench-console" @dblclick.native="showCell"/>

    <el-dialog :title="text('details')" :visible.sync="snapshotVisible" width="76vw" append-to-body>
      <template v-if="snapshot">
        <p class="workbench-snapshot-meta">
          <el-tag size="small">{{ text(snapshot.scope) }}</el-tag>
          <span>{{ formatTime(snapshot.startedAt) }}</span>
          <span>{{ $t('sql_query.limit') }}: {{ snapshot.limit }}</span>
          <span>{{ $t('sql_query.timeout_seconds') }}: {{ snapshot.timeoutSeconds }}</span>
        </p>
        <p class="workbench-hint">{{ text(snapshot.protocolVersion === 1 ? 'batchPolicy' : 'legacy') }}</p>
        <el-table v-if="snapshot.statementLog && snapshot.statementLog.length" :data="snapshot.statementLog" max-height="260" size="mini">
          <el-table-column prop="index" :label="text('statementNumber')" width="90"/>
          <el-table-column width="110">
            <template slot-scope="scope"><el-tag size="mini" :type="scope.row.status === 'ERROR' ? 'danger' : (scope.row.status === 'SUCCESS' ? 'success' : 'warning')">{{ text(scope.row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="rowCount" :label="text('returnedRows')" width="105"/>
          <el-table-column prop="resultCount" :label="text('resultCount')" width="95"/>
          <el-table-column prop="executionTime" :label="text('duration')" width="130"/>
          <el-table-column :label="text('updateCounts')" width="140">
            <template slot-scope="scope">{{ scope.row.updateCounts.join(', ') }}</template>
          </el-table-column>
          <el-table-column :label="text('errorDetail')" min-width="260">
            <template slot-scope="scope">{{ scope.row.message }}<span v-if="scope.row.sqlState"> ({{ scope.row.sqlState }} / {{ scope.row.errorCode }})</span></template>
          </el-table-column>
        </el-table>
        <pre class="workbench-detail-text">{{ snapshot.sql }}</pre>
        <pre v-if="snapshot.error" class="workbench-detail-error">{{ snapshot.error }}</pre>
      </template>
      <div slot="footer">
        <el-button size="small" @click="copy(snapshot ? snapshot.sql : '')">{{ text('copyValue') }}</el-button>
        <el-button size="small" @click="snapshotVisible = false">{{ $t('sql_query.close') }}</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="text('log')" :visible.sync="logVisible" width="86vw" append-to-body>
      <el-table :data="core ? core.executionLog : []" max-height="480">
        <el-table-column :label="$t('sql_query.sql_content')" min-width="260" show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.sql }}</template>
        </el-table-column>
        <el-table-column :label="text('executedAt')" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.startedAt) }}</template>
        </el-table-column>
        <el-table-column width="90">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === 'LIMIT' ? 'warning' : (scope.row.success ? 'success' : 'danger')">{{ text(scope.row.status || (scope.row.success ? 'success' : 'failed')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column min-width="220">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="showExecution(scope.row)">{{ text('details') }}</el-button>
            <el-button type="text" size="mini" @click="openExecutionDraft(scope.row)">{{ text('openDraft') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog :title="text('cell') + (cellLabel ? ' · ' + cellLabel : '')" :visible.sync="cellVisible" width="76vw" append-to-body>
      <pre class="workbench-detail-text">{{ cellText }}</pre>
      <div slot="footer">
        <el-button size="small" @click="copy(cellText)">{{ text('copyValue') }}</el-button>
        <el-button size="small" @click="copy(cellRowText)">{{ text('copyRow') }}</el-button>
        <el-button size="small" @click="cellVisible = false">{{ $t('sql_query.close') }}</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
import SqlQuery from './SqlQuery';
import { executeSqlQueryBatch, saveSqlQueryHistory } from '@/api/sql-query';
import { copyText, createWorkbenchCore, resultAsTsv, workbenchMessage } from './query-workbench';

export default {
  name: 'SqlQueryResultExpand',
  components: { WorkbenchCore: createWorkbenchCore(SqlQuery, executeSqlQueryBatch, saveSqlQueryHistory) },
  data() {
    return {
      core: null, historyVisible: true, snapshotVisible: false, snapshot: null,
      logVisible: false, cellVisible: false, cellLabel: '', cellText: '', cellRowText: ''
    };
  },
  computed: {
    canRun() { return !!(this.core && !this.core.loading && this.core.sql.trim()); },
    execution() { return this.core && (this.core.runningExecution || this.core.lastExecution); }
  },
  mounted() { this.core = this.$refs.console; },
  beforeRouteLeave(to, from, next) {
    if (this.core) this.core.persistLocalDraft(this.core.sql);
    if (!this.core || !this.core.storageWriteFailed) { next(); return; }
    this.$confirm(this.text('leave'), this.$t('sql_query.title'), { type: 'warning' })
      .then(() => next()).catch(() => next(false));
  },
  methods: {
    text(key, values) { return workbenchMessage(this.$i18n && this.$i18n.locale, key, values); },
    run(mode) { if (this.core) this.core.execute(mode); },
    formatTime(value) { return value ? new Date(value).toLocaleString() : ''; },
    showExecution(execution) {
      if (!execution) return;
      this.snapshot = { ...execution };
      this.snapshotVisible = true;
    },
    openExecutionDraft(execution) {
      this.core.insertSqlAsDraft(execution.sql);
      this.logVisible = false;
    },
    async copy(value) {
      try { await copyText(value); this.$message.success(this.text('copied')); }
      catch (error) { this.$message.warning(this.text('copyFailed')); }
    },
    copyPage() {
      if (this.core && this.core.hasRows) this.copy(resultAsTsv(this.core.result.columns, this.core.pagedRows));
    },
    selectResult(key) { if (this.core) this.core.selectQueryResult(key); },
    filterResult(value) { if (this.core) this.core.filterCurrentResult(value); },
    copyFiltered() {
      if (this.core && this.core.hasRows) this.copy(resultAsTsv(this.core.result.columns, this.core.result.rows));
    },
    showResultSql() {
      if (!this.core || !this.core.activeResult) return;
      const selected = this.core.activeResult;
      this.showExecution({ ...this.core.lastExecution, sql: selected.sql || this.core.lastExecution.sql,
        scope: 'currentScope', statementLog: this.core.lastExecution.statementLog.filter(item => item.index === selected.statementIndex) });
    },
    showCell(event) {
      if (!this.core || !this.core.result || !event.target.closest) return;
      const cell = event.target.closest('td');
      const table = this.core.$el.querySelector('.result-table');
      if (!cell || cell.closest('table') !== table || cell.cellIndex < 1) return;
      const row = this.core.pagedRows[cell.parentElement.sectionRowIndex];
      const columnIndex = cell.cellIndex - 1;
      const column = this.core.result.columns[columnIndex];
      if (!row || column == null) return;
      const selected = this.core.activeResult;
      const prefix = selected ? this.text('resultTab', { statement: selected.statementIndex, result: selected.resultIndex }) + ' · ' : '';
      this.cellLabel = prefix + this.core.columnLabel(column);
      this.cellText = this.core.stringifyValue(row[this.core.columnKey(column, columnIndex)]);
      this.cellRowText = resultAsTsv(this.core.result.columns, [row]);
      this.cellVisible = true;
    }
  }
};
</script>

<style scoped>
.sql-query-workbench {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 57px);
  min-height: 640px;
  background: #fff;
}
.workbench-actions {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 16px 4px;
}
.workbench-action-group { display: flex; flex-wrap: wrap; gap: 6px; }
.workbench-action-group .el-button + .el-button { margin-left: 0; }
.workbench-hint { padding: 4px 16px 8px; color: #606266; font-size: 12px; line-height: 1.6; }
.workbench-status { flex: none; }
.batch-results-bar { flex: none; padding: 6px 16px 10px; border-bottom: 1px solid #ebeef5; background: #fafafa; }
.batch-summary, .batch-result-tools, .batch-filter-tools { display: flex; align-items: center; gap: 10px; font-size: 12px; }
.batch-summary, .batch-filter-tools { flex-wrap: wrap; }
.batch-result-tools, .batch-filter-tools { margin-top: 6px; }
.batch-result-tabs { flex: 1; min-width: 0; overflow-x: auto; padding: 2px; }
.batch-result-tabs .el-radio-group { white-space: nowrap; }
.batch-filter-tools .el-input { width: 340px; max-width: 100%; }
.batch-empty-filter { color: #909399; font-size: 12px; }

.sql-query-workbench .workbench-console { flex: 1; height: auto; min-height: 0; }
.history-hidden ::v-deep .sql-history,
.history-hidden ::v-deep .history-width-resizer { display: none; }
.workbench-snapshot-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.workbench-detail-text, .workbench-detail-error {
  max-height: 55vh;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  padding: 14px;
  background: #f5f7fa;
  font-family: Menlo, Consolas, monospace;
  line-height: 1.6;
}
.workbench-detail-error { color: #b42318; }
</style>
