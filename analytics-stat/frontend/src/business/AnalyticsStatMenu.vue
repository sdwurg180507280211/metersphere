<template>
  <el-menu 
    menu-trigger="click" 
    :default-active="$route.path"
    :default-openeds="['1']"
    router 
    class="analytics-menu">
    <el-submenu index="1">
      <template v-slot:title>
        <font-awesome-icon class="icon analytics" :icon="['fas', 'chart-bar']" size="lg"/>
        <span>{{ $t('commons.analytics_stat') }}</span>
      </template>
      <el-menu-item 
        v-for="menu in menus" 
        :key="menu.index"
        :index="menu.index" 
        class="menu-item">
        <i :class="menu.icon"></i>
        {{ menu.title }}
      </el-menu-item>
    </el-submenu>
  </el-menu>
</template>

<script>
/**
 * 分析统计左侧菜单组件
 * 
 * 功能：
 * 1. 显示分析统计模块的功能菜单
 * 2. 支持路由跳转
 * 3. 自动高亮当前激活菜单
 * 
 * 参考：system-setting/frontend/src/business/SettingMenu.vue
 */
export default {
  name: "AnalyticsStatMenu",
  data() {
    return {
      // 菜单配置
      menus: [
        {
          index: '/analytics/home',
          title: '工作台',
          icon: 'el-icon-s-home'
        },
        {
          index: '/analytics/sql-console',
          title: 'SQL查询台',
          icon: 'el-icon-document'
        },
        {
          index: '/analytics/data-dictionary',
          title: '数据字典',
          icon: 'el-icon-collection'
        }
      ]
    };
  }
};
</script>

<style scoped>
.analytics-menu {
  border-right: 0;
}

.analytics-menu .menu-item {
  height: 40px;
  line-height: 40px;
}

.icon {
  width: 24px;
  margin-right: 10px;
}

.analytics {
  color: #409EFF;
}

.menu-item i {
  margin-right: 8px;
  font-size: 16px;
  color: #606266;
}
</style>
