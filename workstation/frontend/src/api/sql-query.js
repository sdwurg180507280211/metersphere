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
