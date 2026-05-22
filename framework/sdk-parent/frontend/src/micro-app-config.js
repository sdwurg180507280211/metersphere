import { getMicroPorts } from './utils/micro-app-storage';

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
 * 这里使用显式映射表，而不是按后缀猜测，避免新增模块命名变化时误判。
 */
const EMBED_APP_PREFIX = 'embed-';
const EMBED_APP_SEPARATOR = '__';

const MICRO_APP_MODULES = [
  // 统计分析模块（serviceId: analytics-stat，路由前缀: analytics）
  // Vue 3 + Vite 构建，需要 iframe 沙箱
  { serviceId: 'analytics-stat', moduleName: 'analytics', migrated: true, isViteApp: true },
  // 工作台模块（serviceId: workstation，路由前缀: workstation）
  { serviceId: 'workstation', moduleName: 'workstation', migrated: true, isViteApp: false },
  // 报告统计模块（serviceId: report-stat，路由前缀: report）
  { serviceId: 'report-stat', moduleName: 'report', migrated: true, isViteApp: false },
  // 项目管理模块（serviceId: project-management，路由前缀: project）
  { serviceId: 'project-management', moduleName: 'project', migrated: true, isViteApp: false },
  // 系统设置模块（serviceId: system-setting，路由前缀: setting）
  { serviceId: 'system-setting', moduleName: 'setting', migrated: true, isViteApp: false },
  // 测试跟踪模块（serviceId: test-track，路由前缀: track）
  { serviceId: 'test-track', moduleName: 'track', migrated: true, isViteApp: false },
  // 接口测试模块（serviceId: api-test，路由前缀: api）
  { serviceId: 'api-test', moduleName: 'api', migrated: true, isViteApp: false },
  // 性能测试模块（serviceId: performance-test，路由前缀: performance）
  { serviceId: 'performance-test', moduleName: 'performance', migrated: true, isViteApp: false },
];

const SERVICE_ID_TO_MODULE = MICRO_APP_MODULES.reduce((result, item) => {
  result[item.serviceId] = item.moduleName;
  return result;
}, {});

const MIGRATED_MODULES = MICRO_APP_MODULES.reduce((result, item) => {
  result[item.moduleName] = {
    migrated: item.migrated,
    isViteApp: item.isViteApp,
  };
  return result;
}, {});

function toShortName(serviceId) {
  if (!serviceId) return serviceId;
  return SERVICE_ID_TO_MODULE[serviceId] || serviceId;
}

function toEmbedMicroAppName(serviceId, uid) {
  return `${EMBED_APP_PREFIX}${toShortName(serviceId)}${EMBED_APP_SEPARATOR}${uid}`;
}

function toMicroAppModuleName(appName) {
  if (!appName) return appName;
  if (appName.startsWith(EMBED_APP_PREFIX)) {
    return appName.slice(EMBED_APP_PREFIX.length).split(EMBED_APP_SEPARATOR)[0];
  }
  return toShortName(appName);
}

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

/**
 * 获取主应用路由子应用运行策略
 *
 * 主应用容器复用子应用实例，保持当前 destroy=false、inline、禁用 memory router、禁用 scoped css 的行为。
 *
 * @param {string} moduleName - 模块名称，如 'api' 或 'api-test'
 * @returns {Object} micro-app 运行策略
 */
function getMainMicroAppRuntimePolicy(moduleName) {
  return {
    iframe: isViteApp(moduleName),
    fiber: true,
    destroy: false,
    inline: true,
    disableMemoryRouter: true,
    disableScopecss: true,
  };
}

/**
 * 获取嵌入式子应用运行策略
 *
 * 嵌入式容器用于按需加载场景，保持当前 destroy=true、clearData=true 的 disposable 行为。
 *
 * @param {string} moduleName - 模块名称，如 'api' 或 'api-test'
 * @returns {Object} micro-app 运行策略
 */
function getEmbedMicroAppRuntimePolicy(moduleName) {
  return {
    iframe: isViteApp(moduleName),
    fiber: true,
    destroy: true,
    clearData: true,
  };
}

export {
  MIGRATED_MODULES,
  SERVICE_ID_TO_MODULE,
  toShortName,
  toEmbedMicroAppName,
  toMicroAppModuleName,
  isMigrated,
  isViteApp,
  getMainMicroAppRuntimePolicy,
  getEmbedMicroAppRuntimePolicy,
};

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
  const microPorts = getMicroPorts();
  const pathName = toShortName(name);
  const port = microPorts[name] != null ? microPorts[name] : microPorts[pathName];

  if (process.env.NODE_ENV === 'development') {
    return '//127.0.0.1:' + (port - 4000);
  }
  return window.location.origin + '/' + pathName;
}
