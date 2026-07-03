import { get, post } from 'metersphere-frontend/src/plugins/request';

const BASE_URL = '/workstation/sql-query';

export function getSqlQueryStatus() {
  return get(`${BASE_URL}/status`);
}

export function executeSqlQuery(sql, limit = 1000, timeoutSeconds = 30) {
  return post(`${BASE_URL}/execute`, { sql, limit, timeoutSeconds });
}

export function getSqlQueryHistory() {
  return get(`${BASE_URL}/history`);
}

export function saveSqlQueryHistory(data) {
  return post(`${BASE_URL}/history/save`, data);
}

export function deleteSqlQueryHistory(id) {
  return post(`${BASE_URL}/history/delete`, { id });
}

export function getSqlQueryPool(data = {}) {
  return post(`${BASE_URL}/pool/list`, data);
}

export function saveSqlQueryPool(data) {
  return post(`${BASE_URL}/pool/save`, data);
}

export function offlineSqlQueryPool(id) {
  return post(`${BASE_URL}/pool/offline`, { id });
}

export function copySqlQueryPoolToHistory(id) {
  return post(`${BASE_URL}/pool/copy-to-history`, { id });
}

export function recordSqlQueryPoolUse(id) {
  return post(`${BASE_URL}/pool/use`, { id });
}
