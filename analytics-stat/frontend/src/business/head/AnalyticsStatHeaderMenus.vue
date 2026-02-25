<template>
  <!-- 顶部导航栏：左侧二级菜单 + 右侧用户信息 -->
  <div class="menu-bar">
    <el-row type="flex" justify="space-between" align="middle">
      <!-- 左侧：二级导航菜单（分析统计为全局视角，不需要项目切换） -->
      <el-col :span="14">
        <el-menu
          class="header-menu"
          :unique-opened="true"
          mode="horizontal"
          router
          :default-active="activePath"
          :ellipsis="false"
        >
          <el-menu-item
            v-for="menu in menus"
            :key="menu.path"
            :index="menu.path"
          >
            {{ t(menu.i18nKey) }}
          </el-menu-item>
        </el-menu>
      </el-col>
      <!-- 右侧：模块标题（Vue 3 版本不再依赖 SDK 的 HeaderRightMenus） -->
      <el-col :span="10" class="right-section">
        <span class="module-title">{{ t('analytics.title') }}</span>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
/**
 * 分析统计顶部导航菜单组件
 *
 * 功能：
 * 1. 左侧：二级导航菜单（工作台、SQL查询台、数据字典）
 * 2. 右侧：模块标题
 *
 * 与 Vue 2 版本的差异：
 * - 不再依赖 metersphere-frontend 的 MsHeaderRightMenus 组件
 *   （Vue 3 版本无法使用 Vue 2 SDK 组件，且分析统计为独立模块，
 *    不需要工作空间切换、任务中心等全局功能）
 * - 使用 computed 替代 watch + data 实现路径匹配
 * - 使用 useRoute / useI18n 替代 Options API
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'

const route = useRoute()
const { t } = useI18n()

/** 二级导航菜单配置 */
const menus = [
  { path: '/analytics/home', i18nKey: 'analytics.menu.home' },
  { path: '/analytics/sql-console', i18nKey: 'analytics.menu.sql_console' },
  { path: '/analytics/data-dictionary', i18nKey: 'analytics.menu.data_dictionary' },
]

/**
 * 根据当前路由路径计算激活的菜单项
 * 使用 computed 替代 Vue 2 版本的 watch + updateActivePath 方法
 */
const activePath = computed(() => {
  const path = route.path
  // 匹配菜单路径前缀，支持子路径高亮
  const matched = menus.find((m) => path.startsWith(m.path))
  if (matched) return matched.path
  // 根路径默认高亮工作台
  if (path === '/analytics') return '/analytics/home'
  return path
})
</script>

<style scoped>
.menu-bar {
  border-bottom: 1px solid #e6e6e6;
  background-color: #fff;
  height: 50px;
  line-height: 50px;
}

.header-menu {
  border-bottom: none;
  height: 50px;
}

/* Element Plus 的 el-menu-item 样式覆盖 */
.header-menu .el-menu-item {
  padding: 0 10px;
  height: 50px;
  line-height: 50px;
}

.right-section {
  text-align: right;
  padding-right: 20px;
}

.module-title {
  font-size: 14px;
  color: #606266;
}
</style>
