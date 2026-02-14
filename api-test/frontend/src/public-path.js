// webpack 打包公共文件路径（micro-app 适配）
import { getApps } from 'metersphere-frontend/src/api/apps';
import { isMicroAppEnv, getMicroAppPublicPath } from 'metersphere-frontend/src/utils/micro-app-env';

// micro-app 环境下，使用 micro-app 自动注入的公共路径
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
