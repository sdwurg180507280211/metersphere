/**
 * micro-app 环境检测工具
 *
 * 【问题背景】
 * micro-app 的 inline 模式下，子应用 JS 在主应用 window 上下文中执行，
 * __MICRO_APP_ENVIRONMENT__ 不会注入到 window 上，而是存在于 __MICRO_APP_PROXY_WINDOW__ 中。
 * 因此直接检查 window.__MICRO_APP_ENVIRONMENT__ 会返回 undefined。
 *
 * 【解决方案】
 * 提供统一的检测函数，同时检查 window 和 __MICRO_APP_PROXY_WINDOW__ 两个位置，
 * 兼容 inline 模式和非 inline 模式。
 *
 * 【使用方式】
 * import { isMicroAppEnv } from 'metersphere-frontend/src/utils/micro-app-env';
 * if (isMicroAppEnv()) { ... }
 */

/**
 * 判断当前是否运行在 micro-app 子应用环境中
 *
 * 兼容两种场景：
 * 1. 非 inline 模式：__MICRO_APP_ENVIRONMENT__ 直接在 window 上
 * 2. inline 模式：__MICRO_APP_ENVIRONMENT__ 在 __MICRO_APP_PROXY_WINDOW__ 中
 *
 * @returns {boolean} 是否在 micro-app 环境中
 */
export function isMicroAppEnv() {
  return !!(
    window.__MICRO_APP_ENVIRONMENT__
    || (window.__MICRO_APP_PROXY_WINDOW__ && window.__MICRO_APP_PROXY_WINDOW__.__MICRO_APP_ENVIRONMENT__)
  );
}

/**
 * 判断当前是否运行在微前端环境中
 *
 * 已完成 qiankun → micro-app 迁移，仅检测 micro-app 环境
 *
 * @returns {boolean} 是否在微前端环境中
 */
export function isMicroEnv() {
  return isMicroAppEnv();
}

/**
 * 获取 micro-app 注入的公共路径
 *
 * inline 模式下从 __MICRO_APP_PROXY_WINDOW__ 中获取
 *
 * @returns {string|undefined} 子应用公共路径
 */
export function getMicroAppPublicPath() {
  return window.__MICRO_APP_PUBLIC_PATH__
    || (window.__MICRO_APP_PROXY_WINDOW__ && window.__MICRO_APP_PROXY_WINDOW__.__MICRO_APP_PUBLIC_PATH__);
}
