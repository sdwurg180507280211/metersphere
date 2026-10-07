import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

// Import dependency-free production logic without changing this Vue 2 application's module type.
const source = await readFile(new URL('../src/business/sql-query/query-workbench.js', import.meta.url), 'utf8');
const { scanStatements, executionSelection, resultAsTsv, workbenchMessage, createWorkbenchCore } =
  await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);

function deferred() {
  let resolve, reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}
const result = () => ({ data: { columns: [{ key: 'col_1', label: 'value' }], rows: [{ col_1: 1 }], rowCount: 1 } });

// A contract harness for the base component's public methods. It is not a Vue/browser mount.
function makeVm(execute = async () => result(), save = async payload => ({ data: { ...payload, id: payload.id || 'saved-1', saved: true } })) {
  const notices = [];
  const storage = new Map();
  globalThis.localStorage = { getItem: key => storage.get(key) || null, setItem: (key, value) => storage.set(key, value) };
  let nextId = 0;
  const base = { methods: {
    handleKeydown(event) { this.forwardedKey = event.key; },
    setResultCollapsed(value) { this.resultCollapsed = value; },
    createLocalDraftId() { return `local-${++nextId}`; },
    createLocalDraft(sql = '') { return { id: '', localId: this.createLocalDraftId(), sql, localDraft: true, saved: false, timestamp: Date.now() }; },
    isSameHistoryItem(left, right) { return !!(left && right && (left.localId ? left.localId === right.localId : left.id === right.id)); },
    useHistorySql(item) { this.suppressDraftSync = true; this.sql = item.sql || ''; this.selectedHistory = item; this.$nextTick(() => { this.suppressDraftSync = false; }); },
    resolveCurrentHistory(sql) { return this.selectedHistory || this.savedHistory.find(item => item.sql === sql); },
    closeResult() { this.result = null; this.error = ''; this.resultCollapsed = true; },
    exportXlsx() { this.exportedResult = this.result; },
    normalizeHistoryTitle(title) { return (title || '').trim(); },
    normalizeHistoryTitleKey(title) { return (title || '').trim().toLowerCase(); },
    normalizeSavedHistoryItem(item) { return { ...item, timestamp: Date.now(), saved: true }; }
  } };
  const options = createWorkbenchCore(base, execute, save);
  const vm = {
    ...options.data(), sql: '', limit: 1000, timeoutSeconds: 30, loading: false, result: null, error: '',
    localHistory: [], savedHistory: [], history: [], selectedHistory: null, suppressDraftSync: false,
    currentPage: 1, resultCollapsed: false, editorHeight: 240, savingHistory: false,
    $refs: { sqlEditor: { selectionStart: 0, selectionEnd: 0 } },
    $i18n: { locale: 'zh-CN' }, $t: key => key, $nextTick: fn => fn(),
    $message: Object.fromEntries(['warning', 'success', 'error'].map(level => [level, text => notices.push({ level, text })])),
    $confirm: async () => {}, notices, storage
  };
  for (const [name, fn] of Object.entries({ ...base.methods, ...options.methods })) vm[name] = fn.bind(vm);
  for (const [name, fn] of Object.entries(options.computed)) Object.defineProperty(vm, name, { get: () => fn.call(vm) });
  Object.defineProperty(vm, 'hasRows', { get: () => !!(vm.result && vm.result.rows.length) });
  vm.destroy = () => { vm.workbenchDisposed = true; vm.queryGeneration++; clearInterval(vm.executionTimer); };
  vm.edit = sql => { vm.sql = sql; vm.persistLocalDraft(sql); };
  return vm;
}

test('statement scanner ignores semicolons in strings, identifiers and comments', () => {
  const sql = "SELECT 'a;b', `c;d`, \"e;f\"; -- comment ;\n SELECT 2 /* ; */; # ;\n SELECT 3;";
  assert.equal(scanStatements(sql).ranges.length, 3);
});
test('scanner supports doubled quotes, escaped quotes and MySQL dash-comment rule', () => {
  assert.equal(scanStatements("SELECT 'it''s;x', 'a\\\';b'; SELECT 2--1;").ranges.length, 2);
  assert.equal(scanStatements(' -- ;\n # ;\n /* ; */ ').ranges.length, 0);
});
test('selection, caret and whole-script execution are distinct', () => {
  const sql = 'USE metersphere;\nSELECT 1;\nSELECT 2;';
  assert.equal(executionSelection(sql, 17, 17).sql, 'SELECT 1;');
  assert.equal(executionSelection(sql, 17, 26).scope, 'selectedScope');
  assert.equal(executionSelection(sql, 0, 1, 'all').sql, sql);
  assert.equal(executionSelection(sql, 17, 34, 'current').sql, 'SELECT 1;');
});
test('caret at EOF or a semicolon selects the correct statement', () => {
  const sql = 'SELECT 1;\nSELECT 2;\n';
  assert.equal(executionSelection(sql, 8, 8).sql, 'SELECT 1;');
  assert.equal(executionSelection(sql, sql.length, sql.length).sql, 'SELECT 2;');
});
test('blank/comment-only selection does not fall back to running other SQL', () => {
  assert.equal(executionSelection('SELECT 1;   ', 9, 12).sql, '');
  assert.equal(executionSelection('-- comment only', 0, 15).sql, '');
});
test('incomplete current/selected SQL is rejected, but an earlier complete statement can run', () => {
  assert.throws(() => executionSelection("SELECT 'unfinished", 0, 0), /incomplete/);
  assert.throws(() => executionSelection('SELECT /* unfinished', 0, 20), /incomplete/);
  assert.equal(executionSelection("SELECT 1; SELECT 'unfinished", 0, 0).sql, 'SELECT 1;');
  assert.equal(executionSelection("SELECT 'unfinished", 0, 0, 'all').scope, 'allScope');
});
test('query request and execution history both use the execution-time snapshot', async () => {
  const pending = deferred();
  const requests = [];
  const vm = makeVm((...args) => { requests.push(args); return pending.promise; });
  vm.insertSqlAsDraft('SELECT 1;');
  const first = vm.execute('all');
  vm.edit('SELECT 2;');
  await vm.execute('all');
  assert.equal(requests.length, 1);
  pending.resolve(result());
  await first;
  assert.equal(vm.lastExecution.sql, 'SELECT 1;');
  assert.equal(vm.executionLog[0].sql, 'SELECT 1;');
  assert.equal(vm.selectedHistory.sql, 'SELECT 2;');
  assert.equal(vm.resultIsStale, true);
  assert.equal(vm.loading, false);
});
test('a late response cannot replace the results of another selected query', async () => {
  const pending = deferred();
  const vm = makeVm(() => pending.promise);
  vm.insertSqlAsDraft('SELECT 1;');
  const first = vm.execute('all');
  vm.insertSqlAsDraft('SELECT 2;');
  pending.resolve(result());
  await first;
  assert.equal(vm.sql, 'SELECT 2;');
  assert.equal(vm.result, null);
  assert.equal(vm.lastExecution, null);
  assert.equal(vm.executionLog[0].sql, 'SELECT 1;');
});
test('a late failure is recorded without appearing in another query', async () => {
  const pending = deferred();
  const vm = makeVm(() => pending.promise);
  vm.insertSqlAsDraft('SELECT bad;');
  const first = vm.execute();
  vm.insertSqlAsDraft('SELECT 2;');
  pending.reject(new Error('unknown column'));
  await first;
  assert.equal(vm.error, '');
  assert.equal(vm.executionLog[0].success, false);
  assert.equal(vm.executionLog[0].error, 'unknown column');
});
test('closing results invalidates in-flight display without pretending to cancel the query', async () => {
  const pending = deferred();
  const vm = makeVm(() => pending.promise);
  vm.insertSqlAsDraft('SELECT 1;');
  const first = vm.execute();
  vm.closeResult();
  assert.equal(vm.loading, true);
  pending.resolve(result());
  await first;
  assert.equal(vm.resultCollapsed, true);
  assert.equal(vm.result, null);
});
test('destroyed consoles ignore both results and history writes from pending requests', async () => {
  const pending = deferred();
  const vm = makeVm(() => pending.promise);
  vm.insertSqlAsDraft('SELECT 1;');
  const first = vm.execute();
  const before = JSON.stringify(vm.localHistory);
  vm.destroy();
  pending.resolve(result());
  await first;
  assert.equal(vm.result, null);
  assert.equal(JSON.stringify(vm.localHistory), before);
});
test('editing saved SQL creates a recoverable working copy and retains the original save target', () => {
  const vm = makeVm();
  const saved = { id: 'saved-1', title: 'Report', sql: 'SELECT 1;', saved: true };
  vm.savedHistory = [saved];
  vm.useHistorySql(saved);
  vm.edit('SELECT 2;');
  assert.equal(saved.sql, 'SELECT 1;');
  assert.equal(vm.selectedHistory.sourceId, 'saved-1');
  assert.equal(vm.resolveCurrentHistory(vm.sql).id, 'saved-1');
  assert.match(vm.storage.get('workstation-sql-query-history'), /SELECT 2;/);
});
test('navigation preserves an edit even before the Vue SQL watcher has run', () => {
  const vm = makeVm();
  const saved = { id: 'saved-1', title: 'Report', sql: 'SELECT 1;', saved: true };
  vm.savedHistory = [saved];
  vm.useHistorySql(saved);
  vm.sql = 'SELECT 9;';
  vm.useHistorySql({ id: 'saved-2', sql: 'SELECT 2;', saved: true });
  assert(vm.localHistory.some(item => item.localDraft && item.sql === 'SELECT 9;'));
});
test('clearing history retains all drafts and saved queries', () => {
  const vm = makeVm();
  vm.savedHistory = [{ id: 's', sql: 'SELECT 7;', saved: true }];
  vm.insertSqlAsDraft('SELECT 1;');
  vm.insertSqlAsDraft('SELECT 2;');
  vm.saveHistory('SELECT 3;', { sql: 'SELECT 3;', success: false, startedAt: 1 });
  vm.clearHistory();
  assert.equal(vm.localHistory.length, 2);
  assert(vm.localHistory.every(item => item.localDraft));
  assert.equal(vm.savedHistory.length, 1);
  assert.equal(vm.sql, 'SELECT 2;');
});
test('execution retention never evicts drafts, including two drafts with identical SQL', () => {
  const vm = makeVm();
  vm.insertSqlAsDraft('SELECT 1;');
  vm.insertSqlAsDraft('SELECT 1;');
  for (let i = 0; i < 60; i++) vm.saveHistory(`SELECT ${i};`, { sql: `SELECT ${i};`, success: true, startedAt: i });
  assert.equal(vm.localHistory.filter(item => item.localDraft).length, 2);
  assert.equal(vm.executionLog.length, 50);
  assert.equal(new Set(vm.localHistory.map(item => item.localId)).size, vm.localHistory.length);
});
test('a browser storage quota failure does not mark a successful query as failed', async () => {
  const vm = makeVm();
  vm.insertSqlAsDraft('SELECT 1;');
  globalThis.localStorage.setItem = () => { throw new Error('quota'); };
  await vm.execute();
  assert.equal(vm.lastExecution.success, true);
  assert.equal(vm.error, '');
  assert.equal(vm.storageWriteFailed, true);
  assert.equal(vm.notices.filter(item => item.level === 'warning').length, 1);
});
test('saved queries can be saved from their working copies without leaving duplicate drafts', async () => {
  const vm = makeVm();
  const saved = { id: 's', title: 'Report', sql: 'SELECT 1;', saved: true };
  vm.savedHistory = [saved];
  vm.useHistorySql(saved);
  vm.edit('SELECT 2;');
  vm.historyForm = { id: 's', title: 'Report', sql: 'SELECT 2;', description: '' };
  await vm.saveHistoryToDatabase();
  assert.equal(vm.selectedHistory.id, 's');
  assert.equal(vm.sql, 'SELECT 2;');
  assert.equal(vm.localHistory.filter(item => item.localDraft).length, 0);
});
test('late save does not remove a different draft or overwrite a newly selected query', async () => {
  const pending = deferred();
  const vm = makeVm(undefined, () => pending.promise);
  vm.insertSqlAsDraft('SELECT 1;');
  vm.historyForm = { id: '', title: 'Report', sql: 'SELECT 1;', description: '' };
  const first = vm.saveHistoryToDatabase();
  vm.insertSqlAsDraft('SELECT 2;');
  pending.resolve({ data: { id: 's', title: 'Report', sql: 'SELECT 1;' } });
  await first;
  assert.equal(vm.sql, 'SELECT 2;');
  assert(vm.localHistory.some(item => item.sql === 'SELECT 2;' && item.localDraft));
});
test('editing the editor or save form while saving retains the newer text', async () => {
  const pending = deferred();
  const vm = makeVm(undefined, () => pending.promise);
  vm.insertSqlAsDraft('SELECT 1;');
  vm.historyForm = { id: '', title: 'Report', sql: 'SELECT 1;', description: '' };
  vm.historyDialogVisible = true;
  const first = vm.saveHistoryToDatabase();
  vm.edit('SELECT 2;');
  vm.historyForm.sql = 'SELECT 3;';
  pending.resolve({ data: { id: 's', title: 'Report', sql: 'SELECT 1;' } });
  await first;
  assert.equal(vm.sql, 'SELECT 2;');
  assert.equal(vm.historyForm.sql, 'SELECT 3;');
  assert.equal(vm.historyForm.id, 's');
  assert.equal(vm.historyDialogVisible, true);
});
test('an export confirmation cannot export another query that appeared meanwhile', async () => {
  const pending = deferred();
  const vm = makeVm();
  vm.result = { ...result().data, truncated: true };
  vm.$confirm = () => pending.promise;
  const first = vm.exportXlsx();
  vm.result = result().data;
  pending.resolve();
  await first;
  assert.equal(vm.exportedResult, undefined);
});
test('malformed query responses become a clear error rather than a broken result table', async () => {
  const vm = makeVm(async () => ({ data: {} }));
  vm.insertSqlAsDraft('SELECT 1;');
  await vm.execute();
  assert.equal(vm.result, null);
  assert.equal(vm.executionLog[0].success, false);
});
test('Ctrl/Cmd+Shift+Enter runs the whole script and ordinary keys retain base behavior', async () => {
  const vm = makeVm();
  let mode;
  vm.execute = value => { mode = value; };
  vm.handleKeydown({ key: 'Enter', ctrlKey: true, shiftKey: true, preventDefault() {} });
  assert.equal(mode, 'all');
  vm.handleKeydown({ key: 'Enter', metaKey: true, preventDefault() {} });
  assert.equal(mode, 'auto');
  vm.handleKeydown({ key: 'Tab', preventDefault() {} });
  assert.equal(vm.forwardedKey, 'Tab');
});
test('TSV handles duplicate labels, quotes, tabs, NULL and zero without losing columns', () => {
  const text = resultAsTsv([{ key: 'a', label: 'same' }, { key: 'b', label: 'same' }], [{ a: 'a\tb"c', b: 0 }, { a: null, b: '' }]);
  assert.equal(text, 'same\tsame\n"a\tb""c"\t0\nNULL\t');
});
test('workbench messages support the application locales and parameter substitution', () => {
  assert.match(workbenchMessage('en-US', 'truncated', { count: 123 }), /123/);
  assert.equal(workbenchMessage('zh-TW', 'all'), '執行全部');
  assert.equal(workbenchMessage('zh-CN', 'all'), '运行全部');
});

test('bulk spreadsheet copy treats formula-like strings as text without changing numeric values', () => {
  assert.equal(resultAsTsv(['value'], [{ value: '=1+1' }, { value: '  @SUM(A1)' }, { value: -3 }]),
    "value\n'=1+1\n'  @SUM(A1)\n-3");
});
test('collapsed result areas reopen to the original editor height when executing or creating drafts', async () => {
  const vm = makeVm();
  vm.insertSqlAsDraft('SELECT 1;');
  vm.closeResult();
  vm.editorHeight = 900;
  await vm.execute('all');
  assert.equal(vm.resultCollapsed, false);
  assert.equal(vm.editorHeight, 240);
  vm.closeResult();
  vm.startNewHistory();
  assert.equal(vm.resultCollapsed, false);
});
test('opening an existing query clears both its predecessor result and error', () => {
  const vm = makeVm();
  vm.result = result().data;
  vm.error = 'old error';
  vm.lastExecution = { sql: 'SELECT 1;', editorSql: 'SELECT 1;' };
  vm.useHistorySql({ id: 's', saved: true, sql: 'SELECT 2;' });
  assert.equal(vm.result, null);
  assert.equal(vm.error, '');
  assert.equal(vm.lastExecution, null);
});
test('a confirmed truncated-result export still exports the captured result', async () => {
  const vm = makeVm();
  vm.result = { ...result().data, truncated: true };
  const expected = vm.result;
  await vm.exportXlsx();
  assert.equal(vm.exportedResult, expected);
});

test('typing before the initial history request completes creates a protected draft', () => {
  const vm = makeVm();
  vm.edit('SELECT 42;');
  assert.equal(vm.selectedHistory.localDraft, true);
  assert.equal(vm.selectedHistory.sql, 'SELECT 42;');
  assert.match(vm.storage.get('workstation-sql-query-history'), /SELECT 42;/);
});
