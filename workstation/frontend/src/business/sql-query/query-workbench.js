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

// Keep the existing editor, history dialogs, XLSX writer and public SQL pool in one place.
// The route composes this extended component rather than copying the large base template.
export function createWorkbenchCore(base, executeSqlQuery, saveSqlQueryHistory) {
  return {
    name: 'SqlQueryWorkbenchCore',
    extends: base,
    data() {
      return {
        queryGeneration: 0, executionSequence: 0, runningExecution: null, lastExecution: null,
        elapsedSeconds: 0, executionTimer: null, storageWriteFailed: false,
        storageWarningShown: false, workbenchDisposed: false
      };
    },
    computed: {
      resultIsStale() { return !!(this.lastExecution && this.sql !== this.lastExecution.editorSql); },
      executionLog() { return this.localHistory.filter(item => item && item.execution).map(item => item.execution); }
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
          const result = response && response.data;
          if (!result || !Array.isArray(result.rows) || !Array.isArray(result.columns)) {
            throw new Error(this.workbenchText('invalidResponse'));
          }
          outcome = { ...snapshot, finishedAt: Date.now(), success: true, rowCount: result.rows.length };
          if (!this.workbenchDisposed && snapshot.generation === this.queryGeneration) {
            this.result = result;
            this.lastExecution = outcome;
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
        if (item.execution) return `${this.workbenchText(item.execution.success ? 'success' : 'failed')} · ${summary}`;
        return summary || this.workbenchText('draft');
      },
      closeResult() {
        this.invalidateQueryResult();
        return base.methods.closeResult.call(this);
      },
      async exportXlsx() {
        if (!this.hasRows) return;
        const result = this.result;
        if (result.truncated || this.resultIsStale) {
          try {
            await this.$confirm(this.workbenchText('exportScope', { count: result.rows.length }),
              this.$t('sql_query.export_xlsx'), { type: 'warning' });
          } catch (error) { return; }
        }
        if (this.result === result) return base.methods.exportXlsx.call(this);
      }
    }
  };
}
