/**
 * webpack 打包公共文件路径（micro-app 适配）
 *
 * 改造说明：
 * 1. 将 __POWERED_BY_QIANKUN__ 替换为 __MICRO_APP_ENVIRONMENT__
 *    - micro-app 框架在子应用运行时自动注入的环境标识
 * 2. 将 __INJECTED_PUBLIC_PATH_BY_QIANKUN__ 替换为 __MICRO_APP_PUBLIC_PATH__
 *    - micro-app 自动注入的资源基础路径，确保静态资源正确加载
 * 3. 独立运行逻辑保持不变，保证开发模式下可独立运行
 *
 * 对应需求：Requirements 2.2, 5.4
 */
import { getApps } from 'metersphere-frontend/src/api/apps';

if (window.__MICRO_APP_ENVIRONMENT__) {
  // micro-app 自动注入 __MICRO_APP_PUBLIC_PATH__，用于设置子应用资源基础路径
  // eslint-disable-next-line no-undef
  __webpack_public_path__ = window.__MICRO_APP_PUBLIC_PATH__;
}

if (!window.__MICRO_APP_ENVIRONMENT__) {
  // 独立运行时，从网关获取服务列表（与原逻辑一致）
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
