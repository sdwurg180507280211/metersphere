import { post, get } from "metersphere-frontend/src/plugins/request"

/**
 * 高级检索 API 接口
 * 
 * 提供跨工作空间、跨项目的统一查询能力
 * 支持可视化条件构建和 JQL 查询语法两种模式
 */

/**
 * 执行高级检索查询
 * 
 * @param {Object} request - 查询请求参数
 * @param {string} request.module - 业务模块（TEST_CASE, ISSUE, TEST_PLAN, TEST_CASE_REVIEW）
 * @param {Array<string>} request.workspaceIds - 工作空间ID列表
 * @param {Array<string>} request.projectIds - 项目ID列表
 * @param {boolean} request.useJQL - 是否使用JQL模式
 * @param {string} request.jql - JQL查询字符串（JQL模式下使用）
 * @param {Object} request.combine - 筛选条件（可视化模式下使用）
 * @param {Object} request.filters - 过滤条件
 * @param {Array} request.orders - 排序条件
 * @param {number} goPage - 页码（从1开始）
 * @param {number} pageSize - 每页数量
 * @returns {Promise} 查询结果（包含数据列表和分页信息）
 */
export function queryData(request, goPage, pageSize) {
  return post(`/workstation/advanced-search/query/${goPage}/${pageSize}`, request);
}

/**
 * 获取字段元数据
 * 
 * @param {string} module - 业务模块
 * @param {string} projectId - 项目ID（可选，传入时返回该项目的自定义字段）
 * @returns {Promise} 字段元数据（包含系统字段和自定义字段）
 */
export function getFieldMetadata(module, projectId) {
  const url = projectId 
    ? `/workstation/advanced-search/fields/${module}?projectId=${projectId}`
    : `/workstation/advanced-search/fields/${module}`;
  return get(url);
}

/**
 * 获取用户列表
 * 
 * @param {Object} params - 查询参数
 * @param {string} params.workspaceIds - 工作空间ID列表（逗号分隔）
 * @param {string} params.keyword - 搜索关键词（用户名/姓名）
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 * @returns {Promise} 用户列表
 */
export function getUsers(params) {
  const { workspaceIds, keyword, pageNum = 1, pageSize = 20 } = params;
  let url = `/workstation/advanced-search/users?pageNum=${pageNum}&pageSize=${pageSize}`;
  if (workspaceIds) {
    url += `&workspaceIds=${workspaceIds}`;
  }
  if (keyword) {
    url += `&keyword=${encodeURIComponent(keyword)}`;
  }
  return get(url);
}

/**
 * 获取工作空间列表
 * 
 * @returns {Promise} 工作空间列表
 */
export function getWorkspaces() {
  return get('/workstation/advanced-search/workspaces');
}

/**
 * 获取项目列表
 * 
 * @param {string} workspaceIds - 工作空间ID列表（逗号分隔，可选）
 * @returns {Promise} 项目列表
 */
export function getProjects(workspaceIds) {
  let url = '/workstation/advanced-search/projects';
  if (workspaceIds) {
    url += `?workspaceIds=${workspaceIds}`;
  }
  return get(url);
}

/**
 * 获取详情
 * 
 * @param {string} module - 业务模块
 * @param {string} id - 数据ID
 * @returns {Promise} 详情数据
 */
export function getDetail(module, id) {
  return get(`/workstation/advanced-search/detail/${module}/${id}`);
}

/**
 * 导出查询结果为 Excel
 * 
 * @param {Object} request - 查询请求参数（同 queryData）
 * @returns {Promise} Excel 文件流
 */
export function exportExcel(request) {
  return post('/workstation/advanced-search/export', request, {
    responseType: 'blob'
  });
}

/**
 * 验证 JQL 语法
 * 
 * @param {string} jql - JQL 查询字符串
 * @param {string} module - 业务模块
 * @returns {Promise} 验证结果（包含是否通过、错误信息、修复建议）
 */
export function validateJQL(jql, module) {
  return post('/workstation/advanced-search/jql/validate', { jql, module });
}

/**
 * 获取 JQL 智能提示
 * 
 * @param {string} context - 当前输入上下文
 * @param {string} module - 业务模块
 * @param {number} cursorPosition - 光标位置
 * @returns {Promise} 智能提示列表（字段名、操作符、值等）
 */
export function getJQLSuggestions(context, module, cursorPosition) {
  return post('/workstation/advanced-search/jql/suggestions', {
    context,
    module,
    cursorPosition
  });
}
