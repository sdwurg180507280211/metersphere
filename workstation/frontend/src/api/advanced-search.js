/**
 * 高级检索 API
 * 
 * 提供跨工作空间、跨项目的统一查询能力
 * 支持 JQL 和传统 combine 两种查询模式
 */

import { get, post } from 'metersphere-frontend/src/plugins/request';

const BASE_URL = '/workstation/advanced-search';

/**
 * 执行高级检索查询
 * 
 * @param {Object} request - 查询请求参数
 * @param {number} goPage - 页码（从 1 开始）
 * @param {number} pageSize - 每页数量
 * @returns {Promise} 查询结果（包含数据列表和分页信息）
 */
export function queryData(request, goPage = 1, pageSize = 10) {
  return post(`${BASE_URL}/query/${goPage}/${pageSize}`, request);
}

/**
 * 获取字段元数据
 * 
 * @param {string} module - 业务模块（TEST_CASE, ISSUE, TEST_PLAN, TEST_CASE_REVIEW）
 * @param {string} projectId - 项目ID（可选）
 * @returns {Promise} 字段元数据列表
 */
export function getFieldMetadata(module, projectId) {
  const params = projectId ? { projectId } : {};
  return get(`${BASE_URL}/fields/${module}`, params);
}

/**
 * 获取用户列表
 * 
 * @param {string} workspaceIds - 工作空间ID列表（逗号分隔）
 * @param {string} keyword - 搜索关键词
 * @param {number} pageNum - 页码
 * @param {number} pageSize - 每页数量
 * @returns {Promise} 用户列表
 */
export function getUsers(workspaceIds, keyword, pageNum = 1, pageSize = 20) {
  return get(`${BASE_URL}/users`, {
    workspaceIds,
    keyword,
    pageNum,
    pageSize
  });
}

/**
 * 获取工作空间列表
 * 
 * @returns {Promise} 工作空间列表
 */
export function getWorkspaces() {
  return get(`${BASE_URL}/workspaces`);
}

/**
 * 获取项目列表
 * 
 * @param {string} workspaceIds - 工作空间ID列表（逗号分隔）
 * @returns {Promise} 项目列表
 */
export function getProjects(workspaceIds) {
  return get(`${BASE_URL}/projects`, { workspaceIds });
}

/**
 * 获取详情
 * 
 * @param {string} module - 业务模块
 * @param {string} id - 数据ID
 * @returns {Promise} 详情数据
 */
export function getDetail(module, id) {
  return get(`${BASE_URL}/detail/${module}/${id}`);
}

/**
 * 验证 JQL 语法
 * 
 * @param {string} jql - JQL 查询语句
 * @param {string} module - 业务模块
 * @returns {Promise} 验证结果
 */
export function validateJQL(jql, module) {
  return post(`${BASE_URL}/jql/validate`, { jql, module });
}

/**
 * 获取 JQL 智能提示
 * 
 * @param {string} context - 当前输入的 JQL 文本
 * @param {string} module - 业务模块
 * @param {number} cursorPosition - 光标位置
 * @returns {Promise} 智能提示列表
 */
export function getJQLSuggestions(context, module, cursorPosition) {
  return post(`${BASE_URL}/jql/suggestions`, {
    context,
    module,
    cursorPosition
  });
}

/**
 * 导出查询结果
 * 
 * @param {Object} request - 查询请求参数
 * @returns {Promise} Excel 文件流
 */
export function exportExcel(request) {
  return post(`${BASE_URL}/export`, request, {
    responseType: 'blob'
  });
}
