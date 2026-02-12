// webpack打包公共文件路径（micro-app 适配）
import {getApps} from "metersphere-frontend/src/api/apps";

// micro-app 自动注入 __MICRO_APP_PUBLIC_PATH__，用于设置子应用资源基础路径
if (window.__MICRO_APP_ENVIRONMENT__) {
  __webpack_public_path__ = window.__MICRO_APP_PUBLIC_PATH__;
}

// 独立运行时，从网关获取服务列表（与原逻辑一致）
if (!window.__MICRO_APP_ENVIRONMENT__) {
  getApps()
    .then(res => {
      let modules = {}, microPorts = {};
      res.data.forEach(svc => {
        let name = svc.serviceId;
        modules[name] = true;
        microPorts[name] = svc.port;
      })
      sessionStorage.setItem("micro_apps", JSON.stringify(modules));
      sessionStorage.setItem("micro_ports", JSON.stringify(microPorts));
    })
}
