/**
 * micro-app 环境检测工具
 *
 * micro-app 框架在子应用运行时自动注入 __MICRO_APP_ENVIRONMENT__ 到子应用的沙箱 window 上。
 * 非微前端环境（独立运行）时该变量不存在。
 *
 * 【使用方式】
 * import { isMicroAppEnv } from 'metersphere-frontend/src/utils/micro-app-env';
 * if (isMicroAppEnv()) { ... }
 */

/**
 * 判断当前是否运行在 micro-app 子应用环境中
 *
 * @returns {boolean} 是否在 micro-app 环境中
 */
export function isMicroAppEnv() {
  return !!window.__MICRO_APP_ENVIRONMENT__;
}

/**
 * 获取 micro-app 注入的公共路径
 *
 * @returns {string|undefined} 子应用公共路径
 */
export function getMicroAppPublicPath() {
  return window.__MICRO_APP_PUBLIC_PATH__;
}
