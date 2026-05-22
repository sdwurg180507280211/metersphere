import { getApps } from '../api/apps';
import { toShortName } from '../micro-app-config';
import { persistMicroAppRegistry } from './micro-app-storage';

let registryPromise = null;

export function syncMicroAppRegistry() {
  if (registryPromise) {
    return registryPromise;
  }

  registryPromise = getApps()
    .then((res) => {
      const services = (Array.isArray(res.data) ? res.data : [])
        .filter((svc) => svc.serviceId && svc.serviceId !== 'gateway');
      const modules = {};
      const microPorts = {};

      services.forEach((svc) => {
        const moduleName = toShortName(svc.serviceId);
        modules[svc.serviceId] = true;
        modules[moduleName] = true;
        microPorts[svc.serviceId] = svc.port;
        microPorts[moduleName] = svc.port;
      });

      persistMicroAppRegistry(modules, microPorts);

      return { services, modules, microPorts };
    })
    .catch((error) => {
      registryPromise = null;
      console.error('[micro-app] 获取微服务列表失败:', error);
      return { services: [], modules: {}, microPorts: {} };
    });

  return registryPromise;
}

