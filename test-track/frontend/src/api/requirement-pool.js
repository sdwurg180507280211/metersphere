import { post } from "metersphere-frontend/src/plugins/request";

export function addRequirement(param) {
  return post("/requirement-pool/add", param);
}

/**
 * 需求池列表查询
 * @param goPage 页码
 * @param pageSize 每页大小
 * @param condition 查询条件
 * @returns {Promise<any>}
 */
export function getRequirementPoolList(goPage, pageSize, condition) {
  return post(`/requirement-pool/list/${goPage}/${pageSize}`, condition);
}

/**
 * 从需求池创建测试计划
 * @param request 创建请求
 * @returns {Promise<any>}
 */
export function createTestPlanFromRequirement(param) {
  return post('/requirement-pool/create-test-plan', param);
}

/**
 * 回退需求池：撤销已创建的测试计划
 * @param dmpNum 需求编号
 * @returns {Promise<any>}
 */
export function rollbackTestPlan(dmpNum) {
  return post('/requirement-pool/rollback-test-plan', {dmpNum: dmpNum});
}
