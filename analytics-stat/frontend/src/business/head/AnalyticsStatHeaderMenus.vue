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

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 路由是否存活（用于强制刷新）
const isRouterAlive = ref(true)

// 当前激活的路径
const pathName = ref('')

// 二级导航菜单配置
const menus = ref([
  {
    path: '/dashboard',
    name: '数据概览'
  },
  {
    path: '/sql-console',
    name: 'SQL查询台'
  },
  {
    path: '/data-dictionary',
    name: '数据字典'
  }
])

// 监听路由变化，更新激活菜单
watch(
  () => route.path,
  (newPath) => {
    // 根据当前路径设置激活的菜单项
    if (newPath.indexOf('/dashboard') >= 0) {
      pathName.value = '/dashboard'
    } else if (newPath.indexOf('/sql-console') >= 0) {
      pathName.value = '/sql-console'
    } else if (newPath.indexOf('/data-dictionary') >= 0) {
      pathName.value = '/data-dictionary'
    } else {
      pathName.value = newPath
    }
  },
  { immediate: true }
)
</script>

<style scoped>
#menu-bar {
  border-bottom: 1px solid #e6e6e6;
  background-color: #fff;
}

.el-menu-item {
  padding: 0 10px;
}
</style>
