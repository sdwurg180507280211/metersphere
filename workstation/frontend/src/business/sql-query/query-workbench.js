/* Query-console state and SQL selection helpers. No database policy is enforced here. */
const HISTORY_KEY = 'workstation-sql-query-history';
const MAX_EXECUTIONS = 50;

const TEXT = {
  'zh-CN': {
    current: '运行当前语句', all: '运行全部', details: '本次执行 SQL', log: '执行记录',
    executedAt: '执行时间', copyPage: '复制当前页', cell: '单元格详情', copyValue: '复制原值', copyRow: '复制整行',
    showHistory: '展开历史', hideHistory: '收起历史',
    hint: 'Ctrl / Cmd + Enter：选中内容或当前语句；加 Shift：运行全部。含 USE / 会话变量的脚本请运行全部。',
    stale: '当前 SQL 已修改，下方仍是上次执行结果。可点击“本次执行 SQL”核对。',
    truncated: '本次仅返回前 {count} 行，结果还有更多；分页、复制和导出均仅针对已返回的数据。',
    running: '查询执行中，已等待 {seconds} 秒。切换窗口或关闭结果不会停止数据库查询。',
    storage: '浏览器草稿保存失败，修改仍保留在当前页面；离开前请保存到数据库或复制 SQL。',
    preserved: '已清除执行记录；正在编辑的草稿和已保存 SQL 已保留。',
    empty: '当前位置没有可执行的 SQL。请选择完整语句，或使用“运行全部”。',
    incomplete: '选区或当前语句的引号/注释未闭合，请选择完整 SQL；完整脚本可使用“运行全部”。',
    copied: '已复制', copyFailed: '复制失败，请在详情中手动选择并复制。',
    exportScope: '仅导出本次已返回的 {count} 行，不会重新查询数据库。当前 SQL 的修改不会改变这份结果。是否继续？',
    draft: '未命名草稿', workingCopy: '未保存修改', success: '成功', failed: '失败',
    invalidResponse: '查询接口返回的数据格式不正确', selectedScope: '选中内容', currentScope: '当前语句', allScope: '全部脚本',
    lastOnly: '当前后端仍只返回最后一个结果集。', openDraft: '在新草稿打开',
    leave: '存在未能写入浏览器的草稿，离开将丢失这些修改。确认离开吗？'
  },
  'en-US': {
    current: 'Run current statement', all: 'Run all', details: 'Executed SQL', log: 'Execution log',
    executedAt: 'Executed at', copyPage: 'Copy current page', cell: 'Cell details', copyValue: 'Copy raw value', copyRow: 'Copy row',
    showHistory: 'Show history', hideHistory: 'Hide history',
    hint: 'Ctrl / Cmd + Enter: selection or current statement; add Shift: run all. Run the entire script when using USE or session variables.',
    stale: 'The editor has changed. These are the previous results. Open Executed SQL to verify their source.',
    truncated: 'Only the first {count} rows were returned; more rows exist. Paging, copying and export use returned data only.',
    running: 'Query running: {seconds} seconds. Switching queries or closing results does not stop the database query.',
    storage: 'Browser draft storage failed. Your changes remain in this page; save to the database or copy SQL before leaving.',
    preserved: 'Execution records cleared. Drafts and saved SQL have been retained.',
    empty: 'No executable SQL at this position. Select a complete statement or use Run all.',
    incomplete: 'An unterminated quote/comment was found. Select complete SQL or use Run all for the whole script.',
    copied: 'Copied', copyFailed: 'Copy failed. Select and copy the text manually in the details dialog.',
    exportScope: 'Export only the {count} rows already returned, without querying again. Editor changes do not change these results. Continue?',
    draft: 'Untitled draft', workingCopy: 'Unsaved changes', success: 'Success', failed: 'Failed',
    invalidResponse: 'The query API returned an invalid result', selectedScope: 'Selection', currentScope: 'Current statement', allScope: 'Entire script',
    lastOnly: 'The backend currently returns only the last result set.', openDraft: 'Open as new draft',
    leave: 'Some drafts could not be stored in this browser. Leaving will lose these changes. Continue?'
  }
};
TEXT['zh-TW'] = {
  ...TEXT['zh-CN'],
  current: '執行目前語句', all: '執行全部', details: '本次執行 SQL', log: '執行記錄',
  executedAt: '執行時間', copyPage: '複製目前頁', cell: '儲存格詳情', copyValue: '複製原值', copyRow: '複製整列',
  showHistory: '展開歷史', hideHistory: '收起歷史',
  hint: 'Ctrl / Cmd + Enter：選取內容或目前語句；加 Shift：執行全部。含 USE / 工作階段變數的指令碼請執行全部。',
  stale: '目前 SQL 已修改，下方仍是上次執行結果。可點擊「本次執行 SQL」核對。',
  truncated: '本次僅傳回前 {count} 列，結果還有更多；分頁、複製和匯出均僅針對已傳回的資料。',
  running: '查詢執行中，已等待 {seconds} 秒。切換視窗或關閉結果不會停止資料庫查詢。',
  storage: '瀏覽器草稿儲存失敗，修改仍保留在目前頁面；離開前請儲存到資料庫或複製 SQL。',
  preserved: '已清除執行記錄；正在編輯的草稿和已儲存 SQL 已保留。',
  empty: '目前位置沒有可執行的 SQL。請選取完整語句，或使用「執行全部」。',
  incomplete: '選取內容或目前語句的引號/註解未閉合，請選取完整 SQL；完整指令碼可使用「執行全部」。',
  copied: '已複製', copyFailed: '複製失敗，請在詳情中手動選取並複製。',
  exportScope: '僅匯出本次已傳回的 {count} 列，不會重新查詢資料庫。目前 SQL 的修改不會改變這份結果。是否繼續？',
  draft: '未命名草稿', workingCopy: '未儲存修改', success: '成功', failed: '失敗',
  invalidResponse: '查詢介面傳回的資料格式不正確', selectedScope: '選取內容', currentScope: '目前語句', allScope: '全部指令碼',
  lastOnly: '目前後端仍只傳回最後一個結果集。', openDraft: '在新草稿開啟',
  leave: '存在未能寫入瀏覽器的草稿，離開將遺失這些修改。確認離開嗎？'
};

Object.assign(TEXT['zh-CN'], {
  batchDetails: '逐语句执行记录', batchSuccess: '脚本执行完成，共 {count} 条语句。',
  batchError: '第 {index} 条语句失败；已完成的结果已保留，后续语句未执行。',
  batchLimit: '已达到整批返回上限，部分结果或后续语句未完成。请缩小查询范围或拆分执行。',
  resultTab: '语句 {statement} · 结果 {result}', resultSql: '当前结果 SQL',
  filterPlaceholder: '在当前结果内筛选（不会查询数据库）',
  filterCount: '显示 {visible} / 已返回 {total} 行', noMatch: '没有匹配结果，请调整或清空筛选。',
  copyFiltered: '复制筛选结果', exportFiltered: '仅导出当前结果中筛选后的 {count} 行。不会查询数据库或导出其他结果集，是否继续？',
  batchPolicy: '每批最多 20 条语句、20 个 JDBC 结果、合计 5,000 行；超时为整批 JDBC 执行预算，不含建立连接。',
  SUCCESS: '成功', ERROR: '失败', LIMIT: '达到上限', SKIPPED: '未执行',
  statementNumber: '语句序号', returnedRows: '已返回行数', resultCount: '结果集数',
  updateCounts: 'JDBC 更新计数', duration: '耗时（毫秒）', errorDetail: '说明 / 错误',
  emptyBatch: '本批没有表格结果，请查看逐语句执行记录。', legacy: '旧接口结果，仅显示最后一个结果集。'
});
Object.assign(TEXT['en-US'], {
  batchDetails: 'Statement log', batchSuccess: 'Script completed: {count} statements.',
  batchError: 'Statement {index} failed. Completed results are retained; subsequent statements were not executed.',
  batchLimit: 'Batch output limit reached. Some results or subsequent statements are incomplete. Narrow or split the script.',
  resultTab: 'Statement {statement} · Result {result}', resultSql: 'SQL for this result',
  filterPlaceholder: 'Filter this result locally (no database query)', filterCount: 'Showing {visible} / {total} returned rows',
  noMatch: 'No matching rows. Change or clear the filter.', copyFiltered: 'Copy filtered results',
  exportFiltered: 'Export only the {count} filtered rows in this result? This does not query the database or export other result sets.',
  batchPolicy: 'At most 20 statements, 20 JDBC results and 5,000 rows per batch. Timeout covers batch JDBC execution, not connection setup.',
  SUCCESS: 'Success', ERROR: 'Failed', LIMIT: 'Limit reached', SKIPPED: 'Not executed',
  statementNumber: 'Statement', returnedRows: 'Returned rows', resultCount: 'Result sets',
  updateCounts: 'JDBC update counts', duration: 'Time (ms)', errorDetail: 'Details / error',
  emptyBatch: 'No tabular results. Check the statement log.', legacy: 'Legacy response: only the last result set is available.'
});
Object.assign(TEXT['zh-TW'], {
  batchDetails: '逐語句執行記錄', batchSuccess: '指令碼執行完成，共 {count} 條語句。',
  batchError: '第 {index} 條語句失敗；已完成的結果已保留，後續語句未執行。',
  batchLimit: '已達到整批傳回上限，部分結果或後續語句未完成。請縮小查詢範圍或分批執行。',
  resultTab: '語句 {statement} · 結果 {result}', resultSql: '目前結果 SQL',
  filterPlaceholder: '在目前結果內篩選（不會查詢資料庫）', filterCount: '顯示 {visible} / 已傳回 {total} 列',
  noMatch: '沒有符合的結果，請調整或清空篩選。', copyFiltered: '複製篩選結果',
  exportFiltered: '僅匯出目前結果中篩選後的 {count} 列。不會查詢資料庫或匯出其他結果集，是否繼續？',
  batchPolicy: '每批最多 20 條語句、20 個 JDBC 結果、合計 5,000 列；逾時為整批 JDBC 執行預算，不含建立連線。',
  SUCCESS: '成功', ERROR: '失敗', LIMIT: '達到上限', SKIPPED: '未執行',
  statementNumber: '語句序號', returnedRows: '已傳回列數', resultCount: '結果集數',
  updateCounts: 'JDBC 更新計數', duration: '耗時（毫秒）', errorDetail: '說明 / 錯誤',
  emptyBatch: '本批沒有表格結果，請查看逐語句執行記錄。', legacy: '舊介面結果，僅顯示最後一個結果集。'
});

export function workbenchMessage(locale, key, values = {}) {
  const language = TEXT[locale] || (/^zh/i.test(locale || '') ? TEXT['zh-CN'] : TEXT['en-US']);
  return (language[key] || TEXT['en-US'][key] || key).replace(/\{(\w+)\}/g, (match, name) =>
    Object.prototype.hasOwnProperty.call(values, name) ? String(values[name]) : match);
}

// Match the MySQL-style quotes/comments supported by SqlQueryService.splitStatements.
// This is deliberately a selection scanner, NOT an authorization or read-only validator.
export function scanStatements(sql) {
  const ranges = [];
  let start = 0;
  let quote = '';
  let lineComment = false;
  let blockComment = false;
  let token = false;
  for (let i = 0; i < sql.length; i++) {
    const char = sql[i];
    const next = sql[i + 1];
    if (lineComment) {
      if (char === '\n' || char === '\r') lineComment = false;
      continue;
    }
    if (blockComment) {
      if (char === '*' && next === '/') { blockComment = false; i++; }
      continue;
    }
    if (quote) {
      if (char === '\\' && quote !== '`') { i++; continue; }
      if (char === quote) {
        if (next === quote) i++;
        else quote = '';
      }
      continue;
    }
    if (char === '#' || (char === '-' && next === '-' && (i + 2 === sql.length || /\s/.test(sql[i + 2])))) {
      lineComment = true;
      continue;
    }
    if (char === '/' && next === '*') { blockComment = true; i++; continue; }
    if (char === ';') {
      if (token) ranges.push({ start, end: i + 1 });
      start = i + 1;
      token = false;
      continue;
    }
    if (char === "'" || char === '"' || char === '`') quote = char;
    if (!/\s/.test(char)) token = true;
  }
  if (token) ranges.push({ start, end: sql.length });
  return { ranges, complete: !quote && !blockComment };
}

export function executionSelection(sql, selectionStart = 0, selectionEnd = 0, mode = 'auto') {
  const start = Math.max(0, Math.min(sql.length, selectionStart));
  const end = Math.max(start, Math.min(sql.length, selectionEnd));
  if (mode === 'all') return { sql: sql.trim(), scope: 'allScope' };
  if (mode !== 'current' && end > start) {
    const selected = sql.slice(start, end).trim();
    const scanned = scanStatements(selected);
    if (!scanned.complete) throw new Error('incomplete');
    return { sql: scanned.ranges.length ? selected : '', scope: 'selectedScope' };
  }
  const scanned = scanStatements(sql);
  let range = scanned.ranges.find(item => start >= item.start && start < item.end);
  const last = scanned.ranges[scanned.ranges.length - 1];
  if (!range && last && start >= last.end && !sql.slice(last.end).trim()) range = last;
  if (!range) return { sql: '', scope: 'currentScope' };
  if (!scanned.complete && range.end === sql.length) throw new Error('incomplete');
  return { sql: sql.slice(range.start, range.end).trim(), scope: 'currentScope' };
}

export function resultAsTsv(columns, rows) {
  const value = item => {
    let text = item == null ? 'NULL' : (typeof item === 'object' ? JSON.stringify(item) : String(item));
    // Bulk copy targets spreadsheet cells. Preserve literal formulas as text; raw-cell copy is unchanged.
    if (typeof item === 'string' && /^\s*[=+\-@]/.test(text)) text = "'" + text;
    return /[\t\r\n"]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
  };
  const keys = columns.map((column, index) => typeof column === 'object' ? column.key || `col_${index + 1}` : column);
  const labels = columns.map(column => typeof column === 'object' ? column.label : column);
  return [labels.map(value).join('\t'), ...rows.map(row => keys.map(key => value(row[key])).join('\t'))].join('\n');
}

export async function copyText(text) {
  if (typeof navigator !== 'undefined' && navigator.clipboard && navigator.clipboard.writeText) {
    try { await navigator.clipboard.writeText(text); return; } catch (error) { /* HTTP/intranet fallback below. */ }
  }
  const active = document.activeElement;
  const selection = active && typeof active.selectionStart === 'number'
    ? [active.selectionStart, active.selectionEnd, active.selectionDirection] : null;
  const input = document.createElement('textarea');
  input.value = text;
  input.setAttribute('readonly', '');
  input.style.cssText = 'position:fixed;left:-9999px;top:0;';
  document.body.appendChild(input);
  try {
    input.select();
    if (!document.execCommand('copy')) throw new Error('copyFailed');
  } finally {
    document.body.removeChild(input);
    if (active && active.focus) active.focus();
    if (selection && active.setSelectionRange) active.setSelectionRange(...selection);
  }
}

// The new endpoint is opt-in. Legacy response decoding is retained for callers/tests, never for automatic retries.
export function decodeQueryResponse(data) {
  const validResult = result => result && Array.isArray(result.columns) && Array.isArray(result.rows);
  if (data && !data.statements && validResult(data)) {
    return { batch: null, status: 'SUCCESS', totalRows: data.rows.length, statementLog: [],
      results: [{ key: 'legacy', statementIndex: 1, resultIndex: 1, sql: '', data }] };
  }
  const invalid = () => { throw new Error('invalidResponse'); };
  if (!data || data.protocolVersion !== 1 || !['SUCCESS', 'ERROR', 'LIMIT'].includes(data.status)
      || !Array.isArray(data.statements) || !data.statements.length || data.statements.length > 20) invalid();
  const results = [];
  const statementLog = [];
  for (const [position, entry] of data.statements.entries()) {
    if (!entry || entry.index !== position + 1 || typeof entry.sql !== 'string'
        || !['SUCCESS', 'ERROR', 'LIMIT', 'SKIPPED'].includes(entry.status)
        || !Array.isArray(entry.results) || !Array.isArray(entry.updateCounts)
        || !entry.results.every(validResult) || (entry.status === 'SKIPPED' && (entry.results.length || entry.updateCounts.length))) invalid();
    if (data.status === 'SUCCESS' && entry.status !== 'SUCCESS') invalid();
    entry.results.forEach((result, index) => results.push({
      key: `${entry.index}:${index + 1}`, statementIndex: entry.index, resultIndex: index + 1, sql: entry.sql,
      data: { ...result, executionTime: entry.executionTime }
    }));
    statementLog.push({ index: entry.index, status: entry.status, executionTime: entry.executionTime,
      rowCount: entry.results.reduce((count, result) => count + result.rows.length, 0), resultCount: entry.results.length,
      updateCounts: entry.updateCounts, message: entry.message, sqlState: entry.sqlState, errorCode: entry.errorCode });
  }
  const totalRows = results.reduce((count, result) => count + result.data.rows.length, 0);
  const jdbcResults = data.statements.reduce((count, entry) => count + entry.results.length + entry.updateCounts.length, 0);
  const hasError = statementLog.some(entry => entry.status === 'ERROR');
  if (hasError !== (data.status === 'ERROR') || totalRows !== data.totalRows || totalRows > 5000 || jdbcResults > 20) invalid();
  return { batch: data, status: data.status, totalRows, statementLog, results };
}

export function filterQueryResult(result, keyword) {
  if (!result) return null;
  const search = String(keyword || '').trim().toLocaleLowerCase();
  const keys = result.columns.map((column, index) => typeof column === 'object' ? column.key || `col_${index + 1}` : column);
  const rows = !search ? result.rows : result.rows.filter(row => keys.some(key => {
    const value = row[key];
    const text = value == null ? 'NULL' : (typeof value === 'object' ? JSON.stringify(value) : String(value));
    return text.toLocaleLowerCase().includes(search);
  }));
  // Never mutate the source result: clearing a filter must restore the complete returned snapshot.
  return { ...result, rows, rowCount: rows.length };
}

// Keep the existing editor, history dialogs, XLSX writer and public SQL pool in one place.
// The route composes this extended component rather than copying the large base template.
export function createWorkbenchCore(base, executeSqlQuery, saveSqlQueryHistory) {
  return {
    name: 'SqlQueryWorkbenchCore',
    extends: base,
    // Override only this console's obsolete single-result hint, without mutating global translations.
    i18n: {
      silentFallbackWarn: true,
      messages: {
        'zh-CN': { sql_query: { empty: '输入 SQL 后运行查询，可切换查看本次执行的多个结果集。' } },
        'zh-TW': { sql_query: { empty: '輸入 SQL 後執行查詢，可切換查看本次執行的多個結果集。' } },
        'en-US': { sql_query: { empty: 'Run SQL, then switch between the result sets returned by this execution.' } }
      }
    },
    data() {
      return {
        queryGeneration: 0, executionSequence: 0, runningExecution: null, lastExecution: null,
        elapsedSeconds: 0, executionTimer: null, storageWriteFailed: false,
        storageWarningShown: false, workbenchDisposed: false,
        queryBatch: null, resultOptions: [], activeResultKey: '', sourceResult: null, resultFilter: ''
      };
    },
    computed: {
      resultIsStale() { return !!(this.lastExecution && this.sql !== this.lastExecution.editorSql); },
      executionLog() { return this.localHistory.filter(item => item && item.execution).map(item => item.execution); },
      activeResult() { return this.resultOptions.find(item => item.key === this.activeResultKey) || null; },
      batchFailure() { return this.queryBatch && this.queryBatch.statements.find(item => item.status === 'ERROR'); },
      returnedRowCount() { return this.sourceResult ? this.sourceResult.rows.length : 0; }
    },
    mounted() { window.addEventListener('beforeunload', this.guardWorkbenchUnload); },
    beforeDestroy() {
      this.persistLocalDraft(this.sql);
      this.workbenchDisposed = true;
      this.queryGeneration++;
      clearInterval(this.executionTimer);
      window.removeEventListener('beforeunload', this.guardWorkbenchUnload);
    },
    methods: {
      workbenchText(key, values) { return workbenchMessage(this.$i18n && this.$i18n.locale, key, values); },
      guardWorkbenchUnload(event) {
        this.persistLocalDraft(this.sql);
        if (this.storageWriteFailed) { event.preventDefault(); event.returnValue = ''; }
      },
      writeLocalHistory() {
        try {
          localStorage.setItem(HISTORY_KEY, JSON.stringify(this.localHistory));
          this.storageWriteFailed = false;
        } catch (error) {
          this.storageWriteFailed = true;
          if (!this.storageWarningShown) {
            this.storageWarningShown = true;
            this.$message.warning(this.workbenchText('storage'));
          }
        }
      },
      expandResultArea() {
        if (this.resultCollapsed) this.editorHeight = 240;
        this.setResultCollapsed(false);
      },
      invalidateQueryResult() {
        this.queryGeneration++;
        this.result = null;
        this.error = '';
        this.lastExecution = null;
        this.queryBatch = null;
        this.resultOptions = [];
        this.activeResultKey = '';
        this.sourceResult = null;
        this.resultFilter = '';
        this.currentPage = 1;
      },
      async execute(mode = 'auto') {
        if (this.loading || !this.sql.trim() || this.workbenchDisposed) return;
        const editor = this.$refs.sqlEditor;
        let selected;
        try {
          selected = executionSelection(this.sql, editor ? editor.selectionStart : 0,
            editor ? editor.selectionEnd : 0, typeof mode === 'string' ? mode : 'auto');
        } catch (error) {
          this.$message.warning(this.workbenchText(error.message));
          return;
        }
        if (!selected.sql) { this.$message.warning(this.workbenchText('empty')); return; }
        this.persistLocalDraft(this.sql);
        this.expandResultArea();
        this.invalidateQueryResult();
        const snapshot = Object.freeze({
          id: ++this.executionSequence, generation: this.queryGeneration,
          sql: selected.sql, editorSql: this.sql, scope: selected.scope,
          limit: this.limit, timeoutSeconds: this.timeoutSeconds, startedAt: Date.now()
        });
        this.runningExecution = snapshot;
        this.loading = true;
        this.elapsedSeconds = 0;
        this.executionTimer = setInterval(() => {
          this.elapsedSeconds = Math.floor((Date.now() - snapshot.startedAt) / 1000);
        }, 1000);
        let outcome;
        try {
          const response = await executeSqlQuery(snapshot.sql, snapshot.limit, snapshot.timeoutSeconds);
          let decoded;
          try { decoded = decodeQueryResponse(response && response.data); }
          catch (error) { throw new Error(this.workbenchText('invalidResponse')); }
          const failure = decoded.statementLog.find(item => item.status === 'ERROR');
          outcome = { ...snapshot, finishedAt: Date.now(), success: decoded.status === 'SUCCESS',
            status: decoded.status, protocolVersion: decoded.batch ? 1 : 0, rowCount: decoded.totalRows, statementLog: decoded.statementLog,
            error: failure ? failure.message : '' };
          if (!this.workbenchDisposed && snapshot.generation === this.queryGeneration) {
            this.queryBatch = decoded.batch;
            this.resultOptions = decoded.results;
            this.lastExecution = outcome;
            const last = decoded.results[decoded.results.length - 1];
            if (last) this.selectQueryResult(last.key);
            else if (failure) this.error = failure.message || this.$t('sql_query.failed');
            else this.result = { columns: [], rows: [], rowCount: 0, executionTime: decoded.batch.executionTime };
          }
        } catch (error) {
          const detail = error && (error.message || error.data);
          outcome = { ...snapshot, finishedAt: Date.now(), success: false,
            error: typeof detail === 'string' ? detail : this.$t('sql_query.failed') };
          if (!this.workbenchDisposed && snapshot.generation === this.queryGeneration) {
            this.error = outcome.error;
            this.lastExecution = outcome;
          }
        } finally {
          clearInterval(this.executionTimer);
          this.executionTimer = null;
          if (!this.workbenchDisposed) {
            this.runningExecution = null;
            this.loading = false;
            // Storage failures must never turn a successful query into a failed query.
            this.saveHistory(snapshot.sql, outcome);
          }
        }
      },
      selectQueryResult(key) {
        const option = this.resultOptions.find(item => item.key === key);
        if (!option) return;
        this.activeResultKey = key;
        this.sourceResult = option.data;
        this.resultFilter = '';
        this.result = filterQueryResult(this.sourceResult, '');
        this.currentPage = 1;
        this.expandResultArea();
      },
      filterCurrentResult(keyword) {
        if (!this.sourceResult) return;
        this.resultFilter = String(keyword || '');
        this.result = filterQueryResult(this.sourceResult, this.resultFilter);
        this.currentPage = 1;
      },
      handleKeydown(event) {
        if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
          event.preventDefault();
          this.execute(event.shiftKey ? 'all' : 'auto');
          return;
        }
        if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
          event.preventDefault();
          this.openDetailDialog();
          return;
        }
        return base.methods.handleKeydown.call(this, event);
      },
      loadLocalHistory() {
        try {
          const items = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]');
          this.localHistory = (Array.isArray(items) ? items : []).filter(item => item && typeof item.sql === 'string')
            .map(item => item.localDraft && !item.localId ? { ...item, localId: this.createLocalDraftId() } : item);
        } catch (error) { this.localHistory = []; this.storageWriteFailed = true; }
      },
      persistLocalDraft(sql) {
        if (this.suppressDraftSync) return;
        if (!this.selectedHistory) {
          // The history request may still be loading when the user starts typing.
          if (!sql) return;
          const draft = this.createLocalDraft(sql);
          this.localHistory = [draft, ...this.localHistory];
          this.selectedHistory = draft;
          this.writeLocalHistory();
          this.mergeHistory();
          return;
        }
        const selected = this.selectedHistory;
        if (selected.sql === sql) return;
        // Editing saved SQL creates a recoverable working copy without changing the saved record.
        const draft = selected.localDraft ? { ...selected } : {
          ...this.createLocalDraft(sql), title: selected.title || '',
          sourceId: selected.saved ? selected.id : '', description: selected.description || ''
        };
        draft.sql = sql || '';
        draft.timestamp = Date.now();
        this.localHistory = [draft, ...this.localHistory.filter(item => item.localId !== draft.localId)];
        this.selectedHistory = draft;
        this.writeLocalHistory();
        this.mergeHistory();
      },
      useHistorySql(item, focusEditor = true) {
        this.persistLocalDraft(this.sql);
        this.invalidateQueryResult();
        this.expandResultArea();
        return base.methods.useHistorySql.call(this, item, focusEditor);
      },
      insertSqlAsDraft(sql) {
        this.persistLocalDraft(this.sql);
        const draft = this.createLocalDraft(sql || '');
        this.localHistory = [draft, ...this.localHistory];
        this.writeLocalHistory();
        this.mergeHistory();
        this.useHistorySql(draft);
      },
      startNewHistory() {
        this.insertSqlAsDraft('');
        this.historyDialogVisible = false;
      },
      resolveCurrentHistory(sql) {
        const sourceId = this.selectedHistory && this.selectedHistory.sourceId;
        if (sourceId) {
          const saved = this.savedHistory.find(item => item.id === sourceId);
          if (saved) return saved;
        }
        return base.methods.resolveCurrentHistory.call(this, sql);
      },
      async saveHistoryToDatabase() {
        if (this.savingHistory || !this.historyForm.sql || !this.historyForm.sql.trim()) return;
        const form = this.historyForm;
        const title = this.normalizeHistoryTitle(form.title);
        if (!title) { this.$message.error(this.$t('sql_query.title_required')); return; }
        const duplicate = this.savedHistory.find(item => this.normalizeHistoryTitleKey(item.title) === this.normalizeHistoryTitleKey(title));
        if (duplicate && duplicate.id !== form.id) {
          this.$message.error(this.$t('sql_query.title_duplicate'));
          return;
        }
        const payload = { id: form.id, sql: form.sql, title, description: form.description };
        const origin = this.selectedHistory && { ...this.selectedHistory };
        const editorSql = this.sql;
        const generation = this.queryGeneration;
        this.savingHistory = true;
        try {
          const response = await saveSqlQueryHistory(payload);
          if (this.workbenchDisposed) return;
          const saved = this.normalizeSavedHistoryItem(response.data);
          this.applySavedHistoryItem(saved);
          const sameForm = this.historyForm === form && form.sql === payload.sql
            && this.normalizeHistoryTitle(form.title) === payload.title && form.description === payload.description;
          const sameEditor = this.queryGeneration === generation && this.sql === editorSql
            && this.isSameHistoryItem(this.selectedHistory, origin);
          if (sameForm && sameEditor) {
            this.removeLocalDraft(origin);
            this.suppressDraftSync = true;
            this.useHistorySql(saved);
          }
          if (sameForm) this.historyDialogVisible = false;
          else if (this.historyForm === form) this.historyForm.id = saved.id;
          this.$message.success(this.$t('sql_query.save_success'));
        } catch (error) {
          if (!this.workbenchDisposed) this.$message.error((error && (error.message || error.data)) || this.$t('sql_query.save_failed'));
        } finally { if (!this.workbenchDisposed) this.savingHistory = false; }
      },
      saveHistory(sql, execution) {
        const item = { sql, localId: this.createLocalDraftId(), timestamp: execution ? execution.startedAt : Date.now(), execution };
        const drafts = this.localHistory.filter(record => record.localDraft);
        const records = [item, ...this.localHistory.filter(record => !record.localDraft)].slice(0, MAX_EXECUTIONS);
        this.localHistory = [...drafts, ...records];
        this.writeLocalHistory();
        this.mergeHistory();
      },
      mergeHistory() {
        const savedSql = new Set(this.savedHistory.map(item => item.sql));
        const local = this.localHistory.filter(item => item && (item.localDraft || item.sql)
          && (item.localDraft || item.execution || !savedSql.has(item.sql)));
        this.history = [...this.savedHistory, ...local].sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
      },
      applySavedHistoryItem(saved) {
        this.savedHistory = [saved, ...this.savedHistory.filter(item => item.id !== saved.id)];
        this.localHistory = this.localHistory.filter(item => item.localDraft || item.execution || item.sql !== saved.sql);
        this.writeLocalHistory();
        this.mergeHistory();
      },
      removeLocalDraft(target = this.selectedHistory) {
        if (!target || !target.localId) return;
        this.localHistory = this.localHistory.filter(item => item.localId !== target.localId);
        this.writeLocalHistory();
        this.mergeHistory();
        if (this.isSameHistoryItem(this.selectedHistory, target)) this.selectedHistory = null;
      },
      clearHistory() {
        this.persistLocalDraft(this.sql);
        if (this.selectedHistory && !this.selectedHistory.saved && !this.selectedHistory.localDraft) {
          this.insertSqlAsDraft(this.sql);
        }
        this.localHistory = this.localHistory.filter(item => item.localDraft);
        this.writeLocalHistory();
        this.mergeHistory();
        this.$message.success(this.workbenchText('preserved'));
      },
      resolveHistoryTitle(item) {
        if (item.sourceId) return `${item.title || this.workbenchText('draft')} (${this.workbenchText('workingCopy')})`;
        if (item.title) return item.title;
        const summary = (item.sql || '').replace(/\s+/g, ' ').trim().slice(0, 70);
        if (item.execution) return `${this.workbenchText(item.execution.status || (item.execution.success ? 'success' : 'failed'))} · ${summary}`;
        return summary || this.workbenchText('draft');
      },
      closeResult() {
        this.invalidateQueryResult();
        return base.methods.closeResult.call(this);
      },
      async exportXlsx() {
        if (!this.hasRows) return;
        const result = this.result;
        if (result.truncated || this.resultIsStale || this.resultFilter.trim()) {
          try {
            await this.$confirm(this.workbenchText(this.resultFilter.trim() ? 'exportFiltered' : 'exportScope', { count: result.rows.length }),
              this.$t('sql_query.export_xlsx'), { type: 'warning' });
          } catch (error) { return; }
        }
        if (this.result === result) return base.methods.exportXlsx.call(this);
      }
    }
  };
}
