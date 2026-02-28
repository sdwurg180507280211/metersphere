<template>
  <div class="menu-bar">
    <el-row type="flex" justify="space-between" align="middle">
      <!-- 左侧：二级导航菜单 -->
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
      <!-- 右侧：模块标题 -->
      <el-col :span="10" class="right-section">
        <span class="module-title">{{ t('analytics.title') }}</span>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { KNOWLEDGE_ROUTE_BASE, KNOWLEDGE_ROUTE_PATHS } from '@/config/knowledge-route'

const route = useRoute()
const { t } = useI18n()

const menus = [
  { path: KNOWLEDGE_ROUTE_PATHS.knowledge, i18nKey: 'analytics.menu.knowledge' },
  { path: KNOWLEDGE_ROUTE_PATHS.knowledgeChat, i18nKey: 'analytics.menu.knowledge_chat' },
]

const activePath = computed(() => {
  const path = route.path
  // 匹配菜单路径前缀，支持子路径高亮
  const matched = menus.find((m) => path.startsWith(m.path))
  if (matched) return matched.path
  // 根路径默认高亮知识库
  if (path === KNOWLEDGE_ROUTE_BASE) return KNOWLEDGE_ROUTE_PATHS.knowledge
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
