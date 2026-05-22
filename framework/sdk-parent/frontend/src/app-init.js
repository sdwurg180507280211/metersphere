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
import { preFetchApps } from './micro-app-setup';
import { syncMicroAppRegistry } from './utils/micro-app-registry';

// 创建全局事件总线
// 通过 Vue.prototype 挂载，所有 Vue 组件可通过 this.$EventBus 访问
// 用于主应用内部组件间的事件通信（如项目切换、工作空间切换等）
Vue.prototype.$EventBus = new Vue();

// 从网关获取所有已注册的微服务列表
syncMicroAppRegistry()
  .then(({ services }) => {
    // 调用 micro-app 预加载，在浏览器空闲时预加载子应用静态资源
    // 传入完整的服务列表，preFetchApps 内部会排除 gateway 并计算入口 URL
    preFetchApps(services);
  });
