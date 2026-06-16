import { get, post } from 'metersphere-frontend/src/plugins/request';

const BASE_URL = '/workstation/sql-query';

export function getSqlQueryStatus() {
  return get(`${BASE_URL}/status`);
}

export function executeSqlQuery(sql, limit = 1000) {
  return post(`${BASE_URL}/execute`, { sql, limit });
}
