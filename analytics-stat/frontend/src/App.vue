<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
/**
 * 分析统计微服务根组件
 * 
 * 作用：作为应用的根容器，承载 router-view
 * 所有页面内容都通过 router-view 渲染
 */
export default {
  name: 'App'
};
</script>

<style>
/* 全局样式重置 */
#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #303133;
  height: 100%;
}
</style>
