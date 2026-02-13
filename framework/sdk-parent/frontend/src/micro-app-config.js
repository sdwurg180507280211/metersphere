/**
 * 微前端模块配置表
 *
 * 本文件定义所有子应用的配置信息，用于控制各模块的加载行为。
 *
 * 【配置字段说明】
 * - migrated {boolean}: 是否启用 micro-app 加载（保留字段，当前所有模块均为 true）
 * - isViteApp {boolean}: 是否为 Vue 3 + Vite 构建的子应用
 *   - true: 开启 iframe 沙箱（Vite 输出 <script type="module">，with 沙箱无法拦截）
 *   - false: 使用默认的 with 沙箱（Vue 2 + Webpack 子应用）
 */

const MIGRATED_MODULES = {
  // 统计分析模块（serviceId: analytics，目录: analytics-stat）
  'analytics':      { migrated: true, isViteApp: false },
  // 工作台模块（serviceId: workstation，目录: workstation）
  'workstation':    { migrated: true, isViteApp: false },
  // 报告统计模块（serviceId: report，目录: report-stat）
  'report':         { migrated: true, isViteApp: false },
  // 项目管理模块（serviceId: project，目录: project-management）
  'project':        { migrated: true, isViteApp: false },
  // 系统设置模块（serviceId: setting，目录: system-setting）
  'setting':        { migrated: true, isViteApp: false },
  // 测试跟踪模块（serviceId: track，目录: test-track）
  'track':          { migrated: true, isViteApp: false },
  // 接口测试模块（serviceId: api，目录: api-test）
  'api':            { migrated: true, isViteApp: false },
  // 性能测试模块（serviceId: performance，目录: performance-test）
  'performance':    { migrated: true, isViteApp: false },
  // 未来 Vue 3 + Vite 模块示例（取消注释并设置 isViteApp: true）：
  // 'new-module':      { migrated: true, isViteApp: true },
};

/**
 * 判断指定模块是否已启用 micro-app 加载
 *
 * @param {string} moduleName - 模块名称（即 serviceId），如 'api'
 * @returns {boolean} 是否已启用，未在配置表中的模块返回 false
 */
function isMigrated(moduleName) {
  return MIGRATED_MODULES[moduleName]?.migrated === true;
}

/**
 * 判断指定模块是否为 Vue 3 + Vite 构建的子应用
 *
 * Vite 子应用必须开启 iframe 沙箱，因为 Vite 输出的
 * <script type="module"> 无法被 micro-app 的 with 沙箱拦截。
 *
 * 用于：
 * - App.vue 中设置 <micro-app :iframe="isViteApp(name)">
 * - MicroAppWrapper.vue 中设置按需加载的沙箱模式
 * - preFetchApps() 中为 Vite 子应用预加载设置 iframe: true
 *
 * @param {string} moduleName - 模块名称（即 serviceId），如 'api'
 * @returns {boolean} 是否为 Vite 子应用，未在配置表中的模块返回 false
 */
function isViteApp(moduleName) {
  return MIGRATED_MODULES[moduleName]?.isViteApp === true;
}

export { MIGRATED_MODULES, isMigrated, isViteApp };
