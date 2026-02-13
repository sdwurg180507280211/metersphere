import {registerMicroApps, start} from 'qiankun';
import {getApps} from './api/apps'
import Vue from "vue"
import {isMigrated} from './micro-app-config'
import {preFetchApps} from './micro-app-setup'

const getActiveRule = (hash) => (location) => location.hash.startsWith(hash);

// 添加全局事件总线
let eventBus = new Vue();
Vue.prototype.$EventBus = eventBus;

// 从网关查所有的服务
getApps()
  .then(res => {
    let apps = [], modules = {}, microPorts = {}
    res.data.forEach(svc => {
      let name = svc.serviceId;

      // 网关排除
      if (name === 'gateway') {
        return;
      }

      // 所有模块都需要添加到 modules 中，用于左侧菜单显示
      modules[name] = true;
      microPorts[name] = svc.port;

      // 已迁移到 micro-app 的模块不注册到 qiankun，避免双重注册冲突
      // 但仍然需要在 modules 中保留，以便菜单正常显示
      if (isMigrated(name)) {
        return;
      }

      apps.push({
        name,
        entry: '//127.0.0.1:' + (svc.port - 4000),
        container: '#micro-app',
        activeRule: getActiveRule('#/' + name),
        props: {
          eventBus
        }
      });
    });

    sessionStorage.setItem("micro_apps", JSON.stringify(modules));
    sessionStorage.setItem("micro_ports", JSON.stringify(microPorts));
    sessionStorage.setItem("MICRO_MODE", 'true');

    if (process.env.NODE_ENV !== 'development') {
      apps.forEach(app => {
        // 替换成后端的端口
        app.entry = app.entry.replace(/127\.0\.0\.1:\d+/g, window.location.host + "/" + app.name);
      });
    }
    //注册子应用
    registerMicroApps(apps);
    //启动
    start();

    // 【新增】调用 micro-app 预加载，在浏览器空闲时预加载已迁移模块的静态资源
    // 提升模块切换体验，减少首次加载延迟
    preFetchApps(res.data);
  })
  .catch(e => {
    console.error(e);
  });


