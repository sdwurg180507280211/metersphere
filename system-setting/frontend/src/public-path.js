// webpack 打包公共文件路径（micro-app 适配）
// 将 qiankun 的环境变量替换为 micro-app 的环境变量：
// - __POWERED_BY_QIANKUN__ → __MICRO_APP_ENVIRONMENT__
// - __INJECTED_PUBLIC_PATH_BY_QIANKUN__ → __MICRO_APP_PUBLIC_PATH__
import { getApps } from 'metersphere-frontend/src/api/apps';
import { isMicroAppEnv, getMicroAppPublicPath } from 'metersphere-frontend/src/utils/micro-app-env';

if (isMicroAppEnv()) {
  // micro-app 自动注入 __MICRO_APP_PUBLIC_PATH__，用于设置子应用资源基础路径
  // 【关键】inline 模式下需要从 __MICRO_APP_PROXY_WINDOW__ 获取
  // eslint-disable-next-line no-undef
  __webpack_public_path__ = getMicroAppPublicPath();
}

if (!isMicroAppEnv()) {
  // 独立运行时，从网关获取服务列表（与原逻辑一致）
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
