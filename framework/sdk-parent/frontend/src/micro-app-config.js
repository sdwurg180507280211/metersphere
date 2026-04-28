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

/**
 * 将完整 serviceId 转换为短名
 *
 * 主应用路由使用短名（如 'api'），但后端 API 返回的 serviceId 可能是完整名（如 'api-test'）。
 * 此函数统一处理两种格式，确保配置查找不会因命名差异而失败。
 *
 * 规则：去掉 '-test'、'-stat'、'-management'、'-service' 后缀
 * - 'api-test' → 'api'
 * - 'performance-test' → 'performance'
 * - 'analytics-stat' → 'analytics'
 * - 'project-management' → 'project'
 * - 'report-stat' → 'report'
 * - 'system-setting' → 'setting'
 * - 'workstation' → 'workstation'（无后缀，原样返回）
 * - 'track' → 'track'（已经是短名，原样返回）
 */
function toShortName(serviceId) {
  if (!serviceId) return serviceId;
  return serviceId.replace(/-(test|stat|management|service)$/, '');
}

const MIGRATED_MODULES = {
  // 统计分析模块（serviceId: analytics，目录: analytics-stat）
  // Vue 3 + Vite 构建，需要 iframe 沙箱
  'analytics':      { migrated: true, isViteApp: true },
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
 * 支持短名（如 'api'）和完整 serviceId（如 'api-test'）两种格式。
 *
 * @param {string} moduleName - 模块名称，如 'api' 或 'api-test'
 * @returns {boolean} 是否已启用，未在配置表中的模块返回 false
 */
function isMigrated(moduleName) {
  const config = MIGRATED_MODULES[moduleName] || MIGRATED_MODULES[toShortName(moduleName)];
  return config?.migrated === true;
}

/**
 * 判断指定模块是否为 Vue 3 + Vite 构建的子应用
 *
 * Vite 子应用必须开启 iframe 沙箱，因为 Vite 输出的
 * <script type="module"> 无法被 micro-app 的 with 沙箱拦截。
 *
 * 支持短名（如 'api'）和完整 serviceId（如 'api-test'）两种格式。
 *
 * @param {string} moduleName - 模块名称，如 'api' 或 'api-test'
 * @returns {boolean} 是否为 Vite 子应用，未在配置表中的模块返回 false
 */
function isViteApp(moduleName) {
  const config = MIGRATED_MODULES[moduleName] || MIGRATED_MODULES[toShortName(moduleName)];
  return config?.isViteApp === true;
}

export { MIGRATED_MODULES, isMigrated, isViteApp };

/**
 * 计算子应用入口 URL（统一入口，避免重复实现）
 *
 * - 开发环境：使用本地地址 //127.0.0.1:{port}
 *   （前端端口约定为后端端口减 4000，从 sessionStorage micro_ports 中读取）
 * - 生产环境：使用当前域名 + 服务路径 {origin}/{name}
 *   （通过网关反向代理访问各子应用静态资源）
 *
 * @param {string} name - 模块名称，如 'api'、'analytics'
 * @returns {string} 子应用入口 URL
 */
export function getEntryUrl(name) {
  const microPorts = JSON.parse(sessionStorage.getItem('micro_ports') || '{}');
  // 端口查找：原名优先（API 返回的原始 serviceId），降级到短名
  const lookupName = microPorts[name] != null ? name : toShortName(name);
  // 路径：配置表 key 优先（短名），降级到短名
  const pathName = MIGRATED_MODULES[name] ? name : toShortName(name);

  if (process.env.NODE_ENV === 'development') {
    return '//127.0.0.1:' + (microPorts[lookupName] - 4000);
  }
  return window.location.origin + '/' + pathName;
}
