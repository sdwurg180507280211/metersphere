import {post, get} from 'metersphere-frontend/src/plugins/request';

// 新增上线记录
export function addReleaseNote(data) {
  return post('/release-note/add', data);
}

// 更新上线记录
export function updateReleaseNote(data) {
  return post('/release-note/update', data);
}

// 删除上线记录
export function deleteReleaseNote(id) {
  return get('/release-note/delete/' + id);
}

// 分页查询上线记录列表（按创建时间倒序）
export function listReleaseNotes(goPage, pageSize) {
  return post('/release-note/list/' + goPage + '/' + pageSize, {});
}
