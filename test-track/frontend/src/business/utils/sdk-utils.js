export {operationConfirm, removeGoBackListener, handleCtrlSEvent, byteToSize, sizeToByte, resizeTextarea,
  getTypeByFileName, strMapToObj, getUUID, windowPrint, parseTag} from "metersphere-frontend/src/utils";
export {parseMdImage} from "metersphere-frontend/src/utils/mdImgUtils";
export {parseCustomFilesForList, getCustomFieldFilter, buildBatchParam, parseCustomFilesForItem} from "metersphere-frontend/src/utils/tableUtils";
export {sortCustomFields, parseCustomField, buildCustomFields} from "metersphere-frontend/src/utils/custom_field";
export {getCurrentProjectID, getCurrentWorkspaceId, getCurrentUser, setCurrentProjectID} from "metersphere-frontend/src/utils/token";
export {hasLicense, hasPermissions, hasPermission} from "metersphere-frontend/src/utils/permission";
export {get, post, downloadFile, fileDownloadGet, fileDownloadPost, fileUpload, generateShareUrl, generateModuleUrl} from "metersphere-frontend/src/plugins/request";
export {CURRENT_LANGUAGE} from "metersphere-frontend/src/utils/constants";
export {CUSTOM_TABLE_HEADER} from "metersphere-frontend/src/utils/default-table-header";
export {buildTree} from "metersphere-frontend/src/model/NodeTree";



export {generateColumnKey, getAdvSearchCustomField} from "metersphere-frontend/src/components/search/custom-component";
export {TEST_CASE_RELEVANCE_ISSUE_LIST, OPERATORS} from "metersphere-frontend/src/components/search/search-components";


export {getProjectMemberOption} from "metersphere-frontend/src/api/user";
export {deleteMarkDownImgByName, saveMarkDownImg} from "metersphere-frontend/src/api/img";
export {getApiDefinitionById, getApiTestCasePages} from "metersphere-frontend/src/api/environment";
export {getOwnerProjects, getProjectListAll} from "metersphere-frontend/src/api/project";
export {deleteRelationshipEdge} from "metersphere-frontend/src/api/relationship-edge";
export {isProjectVersionEnable, getProjectVersions, getVersionFilters} from "metersphere-frontend/src/api/version";


import {
  getCustomFieldValue,
} from "metersphere-frontend/src/utils/tableUtils";
import i18n from "@/i18n";

export function getCustomFieldValueForTrack(row, field, members, statusProp = 'status') {
  if (field.name === '用例状态' && field.system) {
    return parseStatus(row, field.options, statusProp);
  }
  return getCustomFieldValue(row, field, members);
}

function parseStatus(row, options, prop = 'status') {
  if (options) {
    for (let option of options) {
      if (option.value === row[prop]) {
        return option.system ? i18n.t(option.text) : option.text;
      }
    }
  }
  return row[prop];
}

/**
 * 字段显示配置：定义哪些字段不在创建/编辑页面显示
 * 支持多种匹配方式：
 * - 字段名称（name）
 * - 字段类型（type）
 * - 自定义判断函数（customCheck）
 */
const FIELD_DISPLAY_CONFIG = {
  // 缺陷管理：不在创建/编辑页显示的字段
  issue: {
    // 按字段名称过滤
    excludeNames: ['复测次数'],
    // 按字段类型过滤（可选）
    excludeTypes: [],
    // 自定义判断函数（可选）：接收字段对象，返回 true 表示不显示
    customCheck: null
  },
  // 可以扩展其他场景，如 testCase、api 等
  // testCase: {
  //   excludeNames: [],
  //   excludeTypes: [],
  //   customCheck: null
  // }
};

/**
 * 判断字段是否应该在创建/编辑页面显示
 * @param {Object} field - 字段对象
 * @param {String} scene - 场景类型，如 'issue'、'testCase' 等
 * @returns {Boolean} - true 表示应该显示，false 表示不显示
 */
export function shouldDisplayFieldInForm(field, scene = 'issue') {
  if (!field) {
    return false;
  }

  const config = FIELD_DISPLAY_CONFIG[scene];
  if (!config) {
    // 如果没有配置，默认显示所有字段
    return true;
  }

  // 检查字段名称
  if (config.excludeNames && config.excludeNames.length > 0) {
    if (config.excludeNames.includes(field.name)) {
      return false;
    }
  }

  // 检查字段类型
  if (config.excludeTypes && config.excludeTypes.length > 0) {
    if (config.excludeTypes.includes(field.type)) {
      return false;
    }
  }

  // 自定义判断函数
  if (config.customCheck && typeof config.customCheck === 'function') {
    if (!config.customCheck(field)) {
      return false;
    }
  }

  return true;
}

/**
 * 过滤自定义字段列表，移除不在创建/编辑页面显示的字段
 * @param {Array} customFields - 自定义字段数组
 * @param {String} scene - 场景类型，如 'issue'、'testCase' 等
 * @returns {Array} - 过滤后的字段数组
 */
export function filterFieldsForForm(customFields, scene = 'issue') {
  if (!Array.isArray(customFields)) {
    return [];
  }
  return customFields.filter(field => shouldDisplayFieldInForm(field, scene));
}
