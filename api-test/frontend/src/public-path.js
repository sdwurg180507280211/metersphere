// webpack 打包公共文件路径
// 迁移说明：将 qiankun 的环境变量替换为 micro-app 的环境变量
// - __POWERED_BY_QIANKUN__ → __MICRO_APP_ENVIRONMENT__
// - __INJECTED_PUBLIC_PATH_BY_QIANKUN__ → __MICRO_APP_PUBLIC_PATH__
import { getApps } from 'metersphere-frontend/src/api/apps';

// micro-app 环境下，使用 micro-app 自动注入的公共路径
if (window.__MICRO_APP_ENVIRONMENT__) {
  __webpack_public_path__ = window.__MICRO_APP_PUBLIC_PATH__;
}

// 独立运行时，从网关获取服务列表（与原逻辑一致）
if (!window.__MICRO_APP_ENVIRONMENT__) {
  getApps().then((res) => {
    let modules = {},
      microPorts = {};
    res.data.forEach((svc) => {
      let name = svc.serviceId;
      modules[name] = true;
      microPorts[name] = svc.port;
    });
    sessionStorage.setItem('micro_apps', JSON.stringify(modules));
    sessionStorage.setItem('micro_ports', JSON.stringify(microPorts));
  });
}
