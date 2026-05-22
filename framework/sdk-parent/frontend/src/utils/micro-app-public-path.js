import { isMicroAppEnv, getMicroAppPublicPath } from './micro-app-env';
import { syncMicroAppRegistry } from './micro-app-registry';

export function initMicroAppPublicPath(setPublicPath) {
  if (isMicroAppEnv()) {
    setPublicPath(getMicroAppPublicPath());
    return;
  }

  syncMicroAppRegistry();
}

export function initWebpackPublicPath() {
  initMicroAppPublicPath((publicPath) => {
    // eslint-disable-next-line no-undef
    __webpack_public_path__ = publicPath;
  });
}
