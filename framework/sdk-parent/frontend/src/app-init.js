/**
 * 应用初始化模块
 *
 * 本文件负责主应用启动时的核心初始化工作：
 * 1. 创建全局 EventBus 事件总线，供组件间通信使用
 * 2. 从网关获取所有微服务列表
 * 3. 将服务信息写入 sessionStorage，供子应用跨模块嵌入和端口映射使用
 * 4. 调用 preFetchApps() 在浏览器空闲时预加载子应用资源
 *
 * 【迁移说明】
 * 本文件从原 micro-app.js（qiankun 残留代码）中提取而来，
 * 移除了所有 qiankun 相关逻辑（registerMicroApps、start、isMigrated 过滤），
 * 仅保留服务列表获取、sessionStorage 写入和预加载调用。
 */
import Vue from 'vue';
import { getApps } from './api/apps';
import { preFetchApps } from './micro-app-setup';

// 创建全局事件总线
// 通过 Vue.prototype 挂载，所有 Vue 组件可通过 this.$EventBus 访问
// 用于主应用内部组件间的事件通信（如项目切换、工作空间切换等）
Vue.prototype.$EventBus = new Vue();

// 从网关获取所有已注册的微服务列表
getApps()
  .then(res => {
    // modules: 记录所有子应用模块（排除网关），值为 true，用于左侧菜单显示判断
    // microPorts: 记录各子应用的后端端口号，用于跨模块嵌入时的端口映射
    let modules = {};
    let microPorts = {};

    res.data.forEach(svc => {
      let name = svc.serviceId;

      // 网关服务不是子应用，排除
      if (name === 'gateway') {
        return;
      }

      // 记录模块名和端口信息
      modules[name] = true;
      microPorts[name] = svc.port;
    });

    // 写入 sessionStorage，供子应用读取
    // micro_apps: 子应用模块列表，如 { "api-test": true, "track": true, ... }
    // micro_ports: 端口映射，如 { "api-test": 8004, "track": 8003, ... }
    sessionStorage.setItem('micro_apps', JSON.stringify(modules));
    sessionStorage.setItem('micro_ports', JSON.stringify(microPorts));

    // 调用 micro-app 预加载，在浏览器空闲时预加载子应用静态资源
    // 传入完整的服务列表，preFetchApps 内部会排除 gateway 并计算入口 URL
    preFetchApps(res.data);
  })
  .catch(e => {
    // 服务列表获取失败不应阻塞主应用启动
    // 仅记录错误日志，子应用仍可通过路由按需加载
    console.error('[app-init] 获取服务列表失败:', e);
  });
