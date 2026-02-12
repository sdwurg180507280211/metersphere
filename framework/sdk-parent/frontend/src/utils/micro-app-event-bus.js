import Vue from 'vue';
import microApp from '@micro-zoe/micro-app';

/**
 * 创建兼容 EventBus 的适配器
 *
 * 设计目的：
 * - 子应用内部的 $EventBus.$emit / $on 继续工作（本地事件，如 apiConditionBus、handleSaveCaseWithEvent 等）
 * - micro-app 传来的数据转发到本地 EventBus（跨应用 → 本地）
 * - 主应用通过 setData / setGlobalData 发送的事件，子应用无需改动即可通过 $EventBus.$on 接收
 *
 * 数据格式约定（EventBusData）：
 * - eventType: string  — 固定标识，表示这是一个 EventBus 事件
 * - eventName: string  — 事件名称（如 'projectChange'、'changeWs'）
 * - payload: any       — 事件数据（可选）
 *
 * @returns {Vue} 本地 Vue EventBus 实例，可直接赋值给 Vue.prototype.$EventBus
 */
export function createEventBusAdapter() {
  // 创建本地 Vue 实例作为 EventBus，保持与原 qiankun 方案一致的使用方式
  const localBus = new Vue();

  // 仅在 micro-app 子应用环境下注册监听器
  if (window.__MICRO_APP_ENVIRONMENT__) {
    // 监听主应用通过 data 属性 / setData 传来的数据
    // 场景：主应用向指定子应用发送事件（如路由更新、项目切换通知等）
    window.microApp?.addDataListener((data) => {
      if (data.eventType && data.eventName) {
        localBus.$emit(data.eventName, data.payload);
      }
    });

    // 监听全局广播数据（setGlobalData）
    // 场景：主应用广播到所有子应用的事件（如 projectChange、changeWs）
    window.microApp?.addGlobalDataListener((data) => {
      if (data.eventType && data.eventName) {
        localBus.$emit(data.eventName, data.payload);
      }
    });
  }

  return localBus;
}


/**
 * 全局广播事件到所有子应用
 *
 * 使用 microApp.setGlobalData() 一次性广播到所有已注册 globalDataListener 的子应用，
 * 替代 qiankun 时代遍历各子应用 EventBus 逐个通知的方式。
 *
 * 数据会被包装为 EventBusData 格式（含 eventType + eventName），
 * 子应用的 createEventBusAdapter() 中注册的 addGlobalDataListener 会自动将事件
 * 转发到本地 EventBus，子应用无需任何改动即可通过 $EventBus.$on 接收。
 *
 * @param {Object} eventData - 事件数据，必须包含 type 字段
 * @param {string} eventData.type - 事件类型，如 'projectChange'、'changeWs'
 * @param {...*} eventData.* - 其他事件数据字段（如 projectId、workspaceId 等）
 *
 * @example
 * // 项目切换时广播
 * broadcastEvent({ type: 'projectChange', projectId: 'xxx' });
 *
 * // 工作空间切换时广播
 * broadcastEvent({ type: 'changeWs', workspaceId: 'xxx' });
 */
export function broadcastEvent(eventData) {
  // 将事件数据包装为 EventBusData 格式，确保子应用的 addGlobalDataListener 能识别并转发
  const wrappedData = {
    eventType: 'EventBus',       // 固定标识，表示这是一个需要转发到本地 EventBus 的事件
    eventName: eventData.type,   // 事件名称，对应子应用 $EventBus.$on 的事件名
    payload: eventData,          // 完整的事件数据作为 payload 传递
  };

  // setGlobalData 会通知所有已注册 addGlobalDataListener 的子应用
  microApp.setGlobalData(wrappedData);
}
