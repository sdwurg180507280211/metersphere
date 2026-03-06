/**
 * 参数校验模块
 */
const config = require('../config');

const VALID_SERVICES = Object.keys(config.services);
const VALID_MODULES = [
  'system-setting',
  'project-management',
  'test-track',
  'api-test',
  'performance-test',
  'report-stat',
  'workstation',
  'analytics-stat',
  'sdk-parent'
];

const validator = {
  /**
   * 校验服务 ID
   */
  isValidService(serviceId) {
    return VALID_SERVICES.includes(serviceId);
  },

  /**
   * 校验前端模块
   */
  isValidModule(module) {
    return VALID_MODULES.includes(module);
  },

  /**
   * 获取有效的服务 ID
   */
  getValidService(serviceId) {
    if (!this.isValidService(serviceId)) {
      throw new Error(`无效的服务 ID: ${serviceId}`);
    }
    return config.services[serviceId];
  },

  /**
   * 获取有效的前端模块路径
   */
  getValidModulePath(module) {
    if (!this.isValidModule(module)) {
      throw new Error(`无效的模块: ${module}`);
    }
    const modules = {
      'system-setting': 'system-setting',
      'project-management': 'project-management',
      'test-track': 'test-track',
      'api-test': 'api-test',
      'performance-test': 'performance-test',
      'report-stat': 'report-stat',
      'workstation': 'workstation',
      'analytics-stat': 'analytics-stat',
      'sdk-parent': 'framework/sdk-parent'
    };
    return modules[module];
  },

  /**
   * 校验端口号
   */
  isValidPort(port) {
    const num = parseInt(port, 10);
    return !isNaN(num) && num > 0 && num <= 65535;
  },

  /**
   * 安全地转义 shell 参数
   */
  escapeShellArg(arg) {
    return arg.replace(/[;&|`$(){}[\]\\'"\s]/g, '\\$&');
  }
};

module.exports = validator;
