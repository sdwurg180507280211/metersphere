/**
 * micro-app 动态 publicPath 配置
 *
 * 作用：在 micro-app 环境下，动态设置 webpack 的 publicPath
 * 确保子应用的静态资源（JS、CSS、图片等）能够正确加载
 *
 * 原理：
 * - micro-app 会注入 __MICRO_APP_PUBLIC_PATH__ 变量
 * - 该变量包含子应用的实际访问路径
 * - 通过设置 __webpack_public_path__，webpack 会使用该路径加载资源
 */

// webpack 打包公共文件路径
// webpack 打包公共文件路径
import { getApps } from 'metersphere-frontend/src/api/apps';
import { isMicroAppEnv, getMicroAppPublicPath } from 'metersphere-frontend/src/utils/micro-app-env';

if (isMicroAppEnv()) {
  // micro-app 环境下，使用 micro-app 自动注入的公共路径
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
