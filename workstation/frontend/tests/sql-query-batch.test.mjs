import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const source = await readFile(new URL('../src/business/sql-query/query-workbench.js', import.meta.url), 'utf8');
const { decodeQueryResponse, filterQueryResult, createWorkbenchCore, resultAsTsv, workbenchMessage } =
  await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
const table = (values = [1], label = 'value') => ({ columns: [{ key: 'col_1', label }],
  rows: values.map(col_1 => ({ col_1 })), rowCount: values.length, truncated: false, limit: 1000 });
const statement = (index, results = [table()], status = 'SUCCESS', extra = {}) => ({ index,
  sql: `SELECT ${index}`, results, status, executionTime: index * 10, updateCounts: [], message: null, ...extra });
const batch = (statements = [statement(1), statement(2)], status = 'SUCCESS') => ({ protocolVersion: 1, status,
  statements, executionTime: 30, totalRows: statements.reduce((n, item) => n + item.results.reduce((m, r) => m + r.rows.length, 0), 0),
  maxTotalRows: 5000, maxResults: 20 });
function pending() {
  let resolve; const promise = new Promise(done => { resolve = done; }); return { promise, resolve };
}
// Tests production state methods at the public base-component boundary; not a browser mount.
function vmFor(execute = async () => ({ data: batch() })) {
  let localId = 0;
  globalThis.localStorage = { setItem() {} };
  const base = { methods: {
    setResultCollapsed(value) { this.resultCollapsed = value; },
    createLocalDraftId() { return `draft-${++localId}`; },
    createLocalDraft(sql) { return { sql, localId: this.createLocalDraftId(), localDraft: true }; },
    useHistorySql(item) { this.sql = item.sql; this.selectedHistory = item; },
    exportXlsx() { this.exported = this.result; },
    closeResult() { this.resultCollapsed = true; }
  } };
  const options = createWorkbenchCore(base, execute, async () => {});
  const vm = { ...options.data(), sql: 'SELECT 1;SELECT 2', limit: 1000, timeoutSeconds: 30,
    result: null, loading: false, error: '', currentPage: 1, pageSize: 1, resultCollapsed: false,
    savedHistory: [], localHistory: [], history: [], selectedHistory: null,
    $refs: { sqlEditor: { selectionStart: 0, selectionEnd: 0 } }, $i18n: { locale: 'zh-CN' },
    $message: { warning() {}, success() {}, error() {} }, $t: key => key, $confirm: async () => {} };
  for (const [name, method] of Object.entries({ ...base.methods, ...options.methods })) vm[name] = method.bind(vm);
  for (const [name, get] of Object.entries(options.computed)) Object.defineProperty(vm, name, { get: () => get.call(vm) });
  Object.defineProperty(vm, 'hasRows', { get: () => !!(vm.result && vm.result.rows.length) });
  Object.defineProperty(vm, 'pagedRows', { get: () => vm.result.rows.slice((vm.currentPage - 1) * vm.pageSize, vm.currentPage * vm.pageSize) });
  return vm;
}

test('batch decoding retains all results and distinct statement/result identities', () => {
  const decoded = decodeQueryResponse(batch([statement(1, [table([1]), table([2])]), statement(2)]));
  assert.deepEqual(decoded.results.map(item => item.key), ['1:1', '1:2', '2:1']);
  assert.equal(decoded.totalRows, 3);
  assert.equal(decoded.results[1].sql, 'SELECT 1');
  assert.equal(decoded.results[1].data.executionTime, 10);
});
test('legacy result response remains decodable without requiring a retry', () => {
  const decoded = decodeQueryResponse(table([7]));
  assert.equal(decoded.status, 'SUCCESS'); assert.equal(decoded.batch, null); assert.equal(decoded.totalRows, 1);
});
test('malformed or inconsistent batch envelopes are rejected', () => {
  assert.throws(() => decodeQueryResponse({}), /invalidResponse/);
  assert.throws(() => decodeQueryResponse({ ...batch(), totalRows: 99 }), /invalidResponse/);
  assert.throws(() => decodeQueryResponse(batch([statement(2)])), /invalidResponse/);
  assert.throws(() => decodeQueryResponse(batch([statement(1, [], 'ERROR')])), /invalidResponse/);
  assert.throws(() => decodeQueryResponse(batch([statement(1, [table()], 'SKIPPED')], 'ERROR')), /invalidResponse/);
  assert.throws(() => decodeQueryResponse(batch([statement(1, [table(Array(5001).fill(1))])])), /invalidResponse/);
});
test('local filtering preserves null, false, zero, JSON and source order without mutation', () => {
  const result = table([0, false, null, '', 'ABC', { name: 'alice' }]);
  assert.equal(filterQueryResult(result, '0').rows[0].col_1, 0);
  assert.equal(filterQueryResult(result, 'false').rows[0].col_1, false);
  assert.equal(filterQueryResult(result, 'null').rows[0].col_1, null);
  assert.equal(filterQueryResult(result, 'abc').rows[0].col_1, 'ABC');
  assert.deepEqual(filterQueryResult(result, 'alice').rows[0].col_1, { name: 'alice' });
  assert.equal(filterQueryResult(result, 'missing').rows.length, 0);
  assert.equal(result.rows.length, 6); assert.equal(filterQueryResult(result, '  ').rows.length, 6);
});
test('successful batch selects its last result and logs only metadata, never row data', async () => {
  const vm = vmFor(async () => ({ data: batch([statement(1, [table(['secret-cell'])]), statement(2, [table([9])])]) }));
  await vm.execute('all');
  assert.equal(vm.activeResultKey, '2:1'); assert.equal(vm.result.rows[0].col_1, 9);
  assert.equal(vm.executionLog[0].success, true); assert.equal(vm.executionLog[0].rowCount, 2);
  assert(!JSON.stringify(vm.executionLog).includes('secret-cell'));
});
test('result switching resets filtering and pagination while retaining the immutable source', async () => {
  const vm = vmFor(async () => ({ data: batch([statement(1, [table([1, 2])]), statement(2, [table([3, 4])])]) }));
  await vm.execute('all'); vm.currentPage = 2;
  vm.filterCurrentResult('4'); assert.equal(vm.currentPage, 1); assert.equal(vm.returnedRowCount, 2);
  assert.equal(vm.result.rowCount, 1); assert.equal(vm.result.rows[0].col_1, 4);
  vm.selectQueryResult('1:1'); assert.equal(vm.resultFilter, ''); assert.equal(vm.result.rows.length, 2);
  vm.filterCurrentResult('missing'); assert.equal(vm.hasRows, false);
  vm.filterCurrentResult(''); assert.equal(vm.result.rows.length, 2);
});
test('partial SQL failure is not labelled success and earlier results stay usable', async () => {
  const response = batch([statement(1), statement(2, [], 'ERROR', { message: 'bad SQL', sqlState: '42000', errorCode: 1064 }),
    statement(3, [], 'SKIPPED')], 'ERROR');
  const vm = vmFor(async () => ({ data: response })); await vm.execute('all');
  assert.equal(vm.hasRows, true); assert.equal(vm.activeResultKey, '1:1');
  assert.equal(vm.batchFailure.index, 2); assert.equal(vm.lastExecution.success, false);
  assert.equal(vm.lastExecution.statementLog[1].errorCode, 1064);
  assert.equal(vm.error, ''); assert.equal(vm.lastExecution.error, 'bad SQL');
});
test('failure before any result displays the error and retains statement status', async () => {
  const vm = vmFor(async () => ({ data: batch([statement(1, [], 'ERROR', { message: 'denied' })], 'ERROR') }));
  await vm.execute('all'); assert.equal(vm.result, null); assert.equal(vm.error, 'denied');
  assert.equal(vm.queryBatch.status, 'ERROR');
});
test('limit responses are distinguishable from success and SQL errors', async () => {
  const vm = vmFor(async () => ({ data: batch([statement(1), statement(2, [], 'SKIPPED')], 'LIMIT') }));
  await vm.execute('all'); assert.equal(vm.lastExecution.status, 'LIMIT'); assert.equal(vm.lastExecution.success, false);
  assert.equal(vm.hasRows, true); assert.equal(vm.batchFailure, undefined);
});
test('an empty SELECT remains a selectable result, unlike SET without a result set', async () => {
  const vm = vmFor(async () => ({ data: batch([statement(1, [], 'SUCCESS', { updateCounts: [0] }), statement(2, [table([])])]) }));
  await vm.execute('all'); assert.equal(vm.resultOptions.length, 1); assert.equal(vm.activeResultKey, '2:1');
  assert.equal(vm.sourceResult.rows.length, 0); assert.equal(vm.lastExecution.statementLog[0].resultCount, 0);
});
test('scripts with only session commands get an explicit successful empty response', async () => {
  const vm = vmFor(async () => ({ data: batch([statement(1, [], 'SUCCESS', { updateCounts: [0] })]) }));
  await vm.execute('all'); assert.equal(vm.resultOptions.length, 0); assert.equal(vm.error, '');
  assert.equal(vm.result.rows.length, 0); assert.equal(vm.lastExecution.success, true);
});
test('late batch responses cannot populate a different draft or its filter state', async () => {
  const request = pending(); const vm = vmFor(() => request.promise); const run = vm.execute('all');
  vm.insertSqlAsDraft('SELECT 99'); request.resolve({ data: batch() }); await run;
  assert.equal(vm.queryBatch, null); assert.equal(vm.sourceResult, null); assert.equal(vm.resultOptions.length, 0);
  assert.equal(vm.lastExecution, null); assert.equal(vm.executionLog.length, 1);
});
test('filtered exports use filtered rows and ask before exporting the reduced scope', async () => {
  const vm = vmFor(async () => ({ data: batch([statement(1, [table([1, 2])])]) })); await vm.execute('all');
  vm.filterCurrentResult('2'); let message;
  vm.$confirm = async text => { message = text; }; await vm.exportXlsx();
  assert.match(message, /筛选后的 1 行/); assert.deepEqual(vm.exported.rows, [{ col_1: 2 }]);
  assert.equal(vm.sourceResult.rows.length, 2);
});
test('switching results or filters during export confirmation cancels the stale export', async () => {
  const vm = vmFor(); await vm.execute('all'); vm.filterCurrentResult('1');
  const confirmation = pending(); vm.$confirm = () => confirmation.promise;
  const exporting = vm.exportXlsx(); vm.selectQueryResult('1:1'); confirmation.resolve(); await exporting;
  assert.equal(vm.exported, undefined);
  vm.filterCurrentResult('1'); const another = pending(); vm.$confirm = () => another.promise;
  const again = vm.exportXlsx(); vm.filterCurrentResult(''); another.resolve(); await again;
  assert.equal(vm.exported, undefined);
});
test('copying a filtered page uses the selected result and keeps spreadsheet formula protection', async () => {
  const vm = vmFor(async () => ({ data: batch([statement(1, [table(['=2+2', 'other'], 'formula')])]) }));
  await vm.execute('all'); vm.filterCurrentResult('=2');
  assert.equal(resultAsTsv(vm.result.columns, vm.pagedRows), "formula\n'=2+2");
});
test('all new batch/filter status strings resolve in all supported locales', () => {
  for (const locale of ['zh-CN', 'zh-TW', 'en-US']) {
    for (const key of ['batchDetails', 'batchError', 'batchLimit', 'resultTab', 'filterPlaceholder', 'exportFiltered', 'LIMIT', 'SKIPPED']) {
      assert.notEqual(workbenchMessage(locale, key), key);
    }
  }
});
