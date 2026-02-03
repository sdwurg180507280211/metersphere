<template>
  <div id="menu-bar" v-if="isRouterAlive">
    <el-row type="flex">
      <!-- 二级导航菜单 -->
      <el-col :span="24">
        <el-menu
          class="header-menu"
          :unique-opened="true"
          mode="horizontal"
          router
          :default-active="pathName"
        >
          <el-menu-item
            v-for="menu in menus"
            :key="menu.path"
            :index="menu.path"
          >
            {{ menu.name }}
          </el-menu-item>
        </el-menu>
      </el-col>
    </el-row>
  </div>
</template>

<script>
/**
 * 分析统计二级导航菜单组件
 * 
 * 功能：
 * 1. 显示模块内的二级导航菜单
 * 2. 监听路由变化，自动高亮当前菜单项
 * 3. 支持路由跳转
 */
export default {
  name: 'AnalyticsStatHeaderMenus',
  data() {
    return {
      // 路由是否存活（用于强制刷新）
      isRouterAlive: true,
      // 当前激活的路径
      pathName: '',
      // 二级导航菜单配置
      menus: [
        {
          path: '/analytics-stat/dashboard',
          name: '数据概览'
        },
        {
          path: '/analytics-stat/sql-console',
          name: 'SQL查询台'
        },
        {
          path: '/analytics-stat/data-dictionary',
          name: '数据字典'
        }
      ]
    };
  },
  watch: {
    /**
     * 监听路由变化，更新激活的菜单项
     */
    '$route.path': {
      handler(newPath) {
        this.updateActivePath(newPath);
      },
      immediate: true
    }
  },
  methods: {
    /**
     * 根据当前路径设置激活的菜单项
     * @param {string} path - 当前路由路径
     */
    updateActivePath(path) {
      if (path.indexOf('/analytics-stat/dashboard') >= 0 || path === '/analytics-stat') {
        this.pathName = '/analytics-stat/dashboard';
      } else if (path.indexOf('/analytics-stat/sql-console') >= 0) {
        this.pathName = '/analytics-stat/sql-console';
      } else if (path.indexOf('/analytics-stat/data-dictionary') >= 0) {
        this.pathName = '/analytics-stat/data-dictionary';
      } else {
        this.pathName = path;
      }
    }
  }
};
</script>

<style scoped>
#menu-bar {
  border-bottom: 1px solid #e6e6e6;
  background-color: #fff;
}

.header-menu {
  border-bottom: none;
}

.el-menu-item {
  padding: 0 15px;
  height: 40px;
  line-height: 40px;
}

.el-menu-item.is-active {
  border-bottom: 2px solid #409EFF;
}
</style>
