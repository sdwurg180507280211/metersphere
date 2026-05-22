const MICRO_APPS_STORAGE_KEY = 'micro_apps';
const MICRO_PORTS_STORAGE_KEY = 'micro_ports';

let registryCache = null;

function readStorageJson(key) {
  try {
    return JSON.parse(sessionStorage.getItem(key) || '{}');
  } catch (error) {
    console.warn('[micro-app] 读取微服务缓存失败:', error);
    return {};
  }
}

export function setMicroAppRegistryCache(modules, microPorts) {
  registryCache = { modules, microPorts };
}

export function persistMicroAppRegistry(modules, microPorts) {
  setMicroAppRegistryCache(modules, microPorts);

  try {
    sessionStorage.setItem(MICRO_APPS_STORAGE_KEY, JSON.stringify(modules));
    sessionStorage.setItem(MICRO_PORTS_STORAGE_KEY, JSON.stringify(microPorts));
  } catch (error) {
    console.warn('[micro-app] 写入微服务缓存失败:', error);
  }
}

export function getMicroApps() {
  return registryCache ? registryCache.modules : readStorageJson(MICRO_APPS_STORAGE_KEY);
}

export function getMicroPorts() {
  return registryCache ? registryCache.microPorts : readStorageJson(MICRO_PORTS_STORAGE_KEY);
}
