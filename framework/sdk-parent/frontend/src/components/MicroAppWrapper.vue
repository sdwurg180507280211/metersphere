<template>
  <div class="micro-app-wrapper">
    <!--
      micro-app 标签实现按需加载子应用
      - destroy: 组件销毁时强制清除缓存资源，避免内存泄漏（按需加载场景必须开启）
      - clear-data: 卸载时清空通讯缓存，防止下次加载时收到残留数据
      - fiber: 当前关闭，避免实际挂载时被 idle 调度拉长加载链路
      - iframe: 仅 Vue 3 + Vite 子应用开启 iframe 沙箱（Vite 的 <script type="module"> 无法被 with 沙箱拦截）
    -->
    <micro-app
      :name="appName"
      :url="appUrl"
      :data="appData"
      :iframe="microAppPolicy.iframe"
      :destroy="microAppPolicy.destroy"
      :clear-data="microAppPolicy.clearData"
      :fiber="microAppPolicy.fiber"
      @datachange="handleDataChange"
      @mounted="onMounted"
      @unmount="onUnmount"
      @error="onError"
    />
  </div>
</template>

<script>
import { getEmbedMicroAppRuntimePolicy, getEntryUrl, toEmbedMicroAppName } from '../micro-app-config';

/**
 * MicroAppWrapper - 按需加载子应用组件
 *
 * 用于跨模块嵌入场景，如：
 * - test-track 中嵌入 API 场景报告、API 用例结果
 * - TaskCenter 中动态加载不同模块的报告视图
 * - 性能测试报告、UI 场景报告等跨模块展示
 *
 * 提供与旧组件相同的 props 接口（to、service、routeParams、routeName），
 * 内部使用 micro-app 的 <micro-app> 标签实现按需加载。
 */
export default {
  name: 'MicroAppWrapper',
  props: {
    // 目标路由路径，如 '/api/definition/report/123'
    to: String,
    // 服务名称（即 serviceId），如 'api-test'、'performance-test'
    service: String,
    // 路由参数对象，传递给子应用的路由跳转
    routeParams: null,
    // 路由名称，用于命名路由跳转
    routeName: null,
  },
  computed: {
    /**
     * 生成全局唯一的子应用名称
     *
     * micro-app 要求 name 全局唯一，以字母开头，仅允许字母、数字、中划线、下划线。
     * 使用 embed 前缀避免和主应用路由激活的同名子应用冲突，fetch 阶段会解析回模块短名。
     */
    appName() {
      return toEmbedMicroAppName(this.service, this._uid);
    },

    /**
     * 计算子应用入口 URL（委托给 micro-app-config 中的共享实现）
     */
    appUrl() {
      return getEntryUrl(this.service);
    },

    /**
     * 传递给子应用的数据对象
     *
     * 在 UMD 生命周期模式下：
     * - 首次加载时，data 作为 window.mount(data) 的参数传入
     * - 后续变化时，触发子应用的 addDataListener 回调
     */
    appData() {
      return {
        defaultPath: this.to,
        routeParams: this.routeParams,
        routeName: this.routeName,
      };
    },

    /**
     * 嵌入式子应用运行策略
     */
    microAppPolicy() {
      return getEmbedMicroAppRuntimePolicy(this.service);
    },
  },
  methods: {
    /**
     * 处理子应用通过 dispatch 发送的数据
     * e.detail.data 包含子应用发送的数据对象
     */
    handleDataChange(e) {
      const data = e.detail.data;
      this.$emit('datachange', data);
    },

    /** 子应用挂载完成回调 */
    onMounted() {
      this.$emit('mounted');
    },

    /** 子应用卸载完成回调 */
    onUnmount() {
      this.$emit('unmount');
    },

    /**
     * 子应用加载失败回调
     * 记录错误日志并向父组件抛出 error 事件
     */
    onError(e) {
      console.error('[MicroAppWrapper] 子应用加载失败:', this.service, e.detail.error);
      this.$emit('error', e.detail.error);
    },
  },
};
</script>

<style lang="scss" scoped>
/* 保持与原 MicroApp.vue 一致的容器样式 */
</style>
