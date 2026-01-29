import {get} from 'metersphere-frontend/src/plugins/request';

/**
 * 获取用户在项目中的用户组ID
 * @param {String} projectId - 项目ID
 * @param {String} userId - 用户ID
 * @returns {Promise} 返回用户组ID字符串（如 'developer', 'tester'），如果不属于任何用户组则返回null
 */
export function getUserGroupProject(projectId, userId) {
  return get(`/issues/user/group/${projectId}/${userId}`);
}
