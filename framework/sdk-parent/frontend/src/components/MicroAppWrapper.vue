<template>
  <div class="micro-app-wrapper">
    <!--
      micro-app 标签替代 qiankun 的 loadMicroApp 按需加载方式
      - destroy: 组件销毁时强制清除缓存资源，避免内存泄漏（按需加载场景必须开启）
      - clear-data: 卸载时清空通讯缓存，防止下次加载时收到残留数据
      - fiber: 异步执行子应用 JS，减少主线程阻塞
      - iframe: 仅 Vue 3 + Vite 子应用开启 iframe 沙箱（Vite 的 <script type="module"> 无法被 with 沙箱拦截）
    -->
    <micro-app
      :name="appName"
      :url="appUrl"
      :data="appData"
      :iframe="isViteApp"
      destroy
      clear-data
      :fiber="true"
      @datachange="handleDataChange"
      @mounted="onMounted"
      @unmount="onUnmount"
      @error="onError"
    />
  </div>
</template>

<script>
import { MIGRATED_MODULES } from '../micro-app-config';

/**
 * MicroAppWrapper - 按需加载子应用组件（替代 qiankun 的 MicroApp.vue）
 *
 * 用于跨模块嵌入场景，如：
 * - test-track 中嵌入 API 场景报告、API 用例结果
 * - TaskCenter 中动态加载不同模块的报告视图
 * - 性能测试报告、UI 场景报告等跨模块展示
 *
 * 与 MicroApp.vue 保持相同的 props 接口（to、service、routeParams、routeName），
 * 内部使用 micro-app 的 <micro-app> 标签替代 qiankun 的 loadMicroApp。
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
     * 使用 embed- 前缀与全局路由激活的子应用名称区分：
     * - 全局路由激活：直接使用 serviceId，如 'api-test'
     * - 按需嵌入加载：使用 'embed-api-test-42' 格式，避免 name 冲突
     * _uid 是 Vue 实例的唯一标识，确保同一 service 的多个 MicroAppWrapper 实例不冲突
     */
    appName() {
      return `embed-${this.service}-${this._uid}`;
    },

    /**
     * 计算子应用入口 URL
     *
     * 开发环境：使用本地端口（后端端口 - 4000 = 前端端口）
     * 生产环境：通过网关反向代理访问（/{serviceId}/）
     */
    appUrl() {
      const microPorts = JSON.parse(sessionStorage.getItem('micro_ports'));
      if (process.env.NODE_ENV === 'development') {
        return `//127.0.0.1:${microPorts[this.service] - 4000}`;
      }
      return `${window.location.origin}/${this.service}`;
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
     * 判断当前 service 是否为 Vue 3 + Vite 子应用
     *
     * Vite 子应用必须开启 iframe 沙箱，因为 Vite 输出的
     * <script type="module"> 无法被 micro-app 的 with 沙箱拦截。
     * Vue 2 + Webpack 子应用使用默认的 with 沙箱即可。
     */
    isViteApp() {
      const config = MIGRATED_MODULES[this.service];
      return config && config.isViteApp || false;
    },
  },
  watch: {
    // 当路由参数变化时，micro-app 的 data 属性是响应式的，
    // Vue 的 computed（appData）会自动触发更新，子应用的 addDataListener 会收到新数据
    routeParams: {
      handler() { this.updateChildData(); },
      deep: true,
    },
    to() { this.updateChildData(); },
  },
  methods: {
    /**
     * 更新子应用数据
     *
     * micro-app 的 data 属性是响应式的，Vue 的 computed 会自动触发更新。
     * 此方法作为 watch 回调的统一入口，当前依赖 Vue 响应式机制即可。
     * 如果未来需要强制更新（数据值未变但需要重新触发），可使用 microApp.forceSetData。
     */
    updateChildData() {
      // micro-app 的 :data 绑定是响应式的，computed appData 变化会自动传递给子应用
    },

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
