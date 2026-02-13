import {get} from "metersphere-frontend/src/plugins/request";

// 获取最近 N 条上线记录（供测试跟踪首页展示，按创建时间倒序）
export function getRecentReleaseNotes(limit) {
  return get('/release-note/recent/' + limit);
}

// 获取单条上线记录详情
export function getReleaseNote(id) {
  return get('/release-note/get/' + id);
}
