// webpack 打包公共文件路径（micro-app 适配）
// 【关键】使用 isMicroAppEnv() 兼容 inline 模式，
// inline 模式下 __MICRO_APP_ENVIRONMENT__ 在 __MICRO_APP_PROXY_WINDOW__ 中
import { getApps } from 'metersphere-frontend/src/api/apps';
import { isMicroAppEnv, getMicroAppPublicPath } from 'metersphere-frontend/src/utils/micro-app-env';

// micro-app 环境下，使用 micro-app 自动注入的公共路径
// 【关键】inline 模式下需要从 __MICRO_APP_PROXY_WINDOW__ 获取
if (isMicroAppEnv()) {
  // eslint-disable-next-line no-undef
  __webpack_public_path__ = getMicroAppPublicPath();
}

// 独立运行时，从网关获取服务列表（与原逻辑一致）
if (!isMicroAppEnv()) {
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
