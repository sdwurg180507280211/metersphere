/**
 * 子应用 public-path.js 改造模板（qiankun → micro-app）
 *
 * 改造说明：
 * 1. 将 `window.__POWERED_BY_QIANKUN__` 替换为 `window.__MICRO_APP_ENVIRONMENT__`
 *    - `__MICRO_APP_ENVIRONMENT__` 是 micro-app 框架在子应用运行时自动注入的环境标识
 *    - 当子应用被 micro-app 加载时，该变量为 true；独立运行时不存在
 *
 * 2. 将 `window.__INJECTED_PUBLIC_PATH_BY_QIANKUN__` 替换为 `window.__MICRO_APP_PUBLIC_PATH__`
 *    - `__MICRO_APP_PUBLIC_PATH__` 是 micro-app 自动注入的资源基础路径
 *    - 确保子应用的静态资源（JS、CSS、图片等）能通过正确的路径加载
 *
 * 3. 独立运行逻辑保持不变
 *    - 非微前端环境下，仍从网关获取服务列表并存入 sessionStorage
 *    - 保证开发模式下子应用可独立运行和调试
 *
 * 适用范围：所有 8 个子应用
 *   - workstation, report-stat, analytics-stat
 *   - project-management, system-setting, performance-test
 *   - test-track, api-test
 *
 * 对应需求：Requirements 2.2, 5.4
 */

// webpack 打包公共文件路径
import { getApps } from 'metersphere-frontend/src/api/apps';

if (window.__MICRO_APP_ENVIRONMENT__) {
  // micro-app 自动注入 __MICRO_APP_PUBLIC_PATH__，用于设置子应用资源基础路径
  // eslint-disable-next-line no-undef
  __webpack_public_path__ = window.__MICRO_APP_PUBLIC_PATH__;
}

if (!window.__MICRO_APP_ENVIRONMENT__) {
  // 独立运行时，从网关获取服务列表（与原 qiankun 逻辑一致）
  // 服务列表用于子应用间的跨模块嵌入和端口映射
  getApps().then(res => {
    let modules = {}, microPorts = {};
    res.data.forEach(svc => {
      let name = svc.serviceId;
      modules[name] = true;
      microPorts[name] = svc.port;
    });
    sessionStorage.setItem('micro_apps', JSON.stringify(modules));
    sessionStorage.setItem('micro_ports', JSON.stringify(microPorts));
  });
}
