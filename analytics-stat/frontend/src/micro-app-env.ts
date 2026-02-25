/**
 * micro-app 环境检测工具
 *
 * 功能：
 * - 判断当前是否运行在 micro-app 微前端环境中
 * - 获取 micro-app 注入的公共路径
 * - 获取子应用名称
 *
 * 说明：
 * - micro-app 会在子应用 window 上注入 __MICRO_APP_ENVIRONMENT__ 等变量
 * - 独立运行时这些变量不存在，返回 false / undefined
 */

/** 判断是否在 micro-app 微前端环境中 */
export function isMicroAppEnv(): boolean {
  return !!window.__MICRO_APP_ENVIRONMENT__
}

/** 获取 micro-app 注入的子应用公共路径 */
export function getMicroAppPublicPath(): string | undefined {
  return window.__MICRO_APP_PUBLIC_PATH__
}

/** 获取 micro-app 注入的子应用名称 */
export function getMicroAppName(): string | undefined {
  return window.__MICRO_APP_NAME__
}
