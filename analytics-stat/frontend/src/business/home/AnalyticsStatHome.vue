<template>
  <div class="analytics-stat-home">
    <!-- 统计卡片行 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <query-count-card :count="queryCount" :trend="queryTrend" />
      </el-col>
      <el-col :span="12">
        <data-volume-card :volume="dataVolume" :unit="dataUnit" />
      </el-col>
    </el-row>

    <!-- 快捷入口行 -->
    <el-row style="margin-top: 16px">
      <el-col>
        <quick-access-card :items="quickAccessItems" />
      </el-col>
    </el-row>

    <!-- 最近查询列表行 -->
    <el-row style="margin-top: 16px">
      <el-col>
        <recent-query-list :queries="recentQueries" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
/**
 * 分析统计模块工作台首页
 *
 * 功能：
 * 1. 展示查询次数统计（QueryCountCard）
 * 2. 展示数据量统计（DataVolumeCard）
 * 3. 提供快捷入口（QuickAccessCard）
 * 4. 展示最近查询列表（RecentQueryList）
 *
 * 与 Vue 2 版本的差异：
 * - 使用 Composition API（ref / onMounted / onActivated）替代 Options API
 * - 使用 Element Plus 图标组件替代 Element UI 的 i 标签图标
 * - 使用 ElMessage 替代 this.$message
 * - 使用 markRaw 包裹图标组件，避免 Vue 3 响应式代理开销
 */
import { ref, onMounted, onActivated, markRaw } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Document, Collection, Search } from '@element-plus/icons-vue'

import QueryCountCard from './components/QueryCountCard.vue'
import DataVolumeCard from './components/DataVolumeCard.vue'
import QuickAccessCard from './components/QuickAccessCard.vue'
import RecentQueryList from './components/RecentQueryList.vue'
import type { QueryRecord } from './components/RecentQueryList.vue'
import type { QuickAccessItem } from './components/QuickAccessCard.vue'

const { t } = useI18n()

// ========== 响应式状态 ==========

/** 查询次数 */
const queryCount = ref(0)
/** 查询趋势 */
const queryTrend = ref<'up' | 'down' | 'stable'>('stable')
/** 数据量 */
const dataVolume = ref(0)
/** 数据量单位 */
const dataUnit = ref<'MB' | 'GB' | 'TB'>('MB')
/** 最近查询列表 */
const recentQueries = ref<QueryRecord[]>([])

/** 快捷入口配置 */
const quickAccessItems: QuickAccessItem[] = [
  {
    i18nKey: 'analytics.menu.sql_console',
    icon: markRaw(Document),
    path: '/analytics/sql-console',
    descKey: 'analytics.sql_console_desc',
  },
  {
    i18nKey: 'analytics.menu.data_dictionary',
    icon: markRaw(Collection),
    path: '/analytics/data-dictionary',
    descKey: 'analytics.data_dictionary_desc',
  },
  {
    i18nKey: 'analytics.comprehensive_query',
    icon: markRaw(Search),
    path: '/analytics/sql-console',
    descKey: 'analytics.comprehensive_query_desc',
  },
]

// ========== 数据加载方法 ==========

/** 加载查询次数统计 */
async function loadQueryCount() {
  try {
    // TODO: 调用后端 API
    // const res = await api.getQueryCount()
    queryCount.value = 1234
    queryTrend.value = 'up'
  } catch {
    ElMessage.error(t('analytics.load_query_count_failed'))
  }
}

/** 加载数据量统计 */
async function loadDataVolume() {
  try {
    // TODO: 调用后端 API
    dataVolume.value = 1024
    dataUnit.value = 'MB'
  } catch {
    ElMessage.error(t('analytics.load_data_volume_failed'))
  }
}

/** 加载最近查询列表 */
async function loadRecentQueries() {
  try {
    // TODO: 调用后端 API
    recentQueries.value = [
      {
        id: '1',
        name: t('analytics.mock_query_user_stat'),
        type: 'sql',
        createTime: new Date(),
        status: 'success',
      },
      {
        id: '2',
        name: t('analytics.mock_query_project_data'),
        type: 'query',
        createTime: new Date(),
        status: 'success',
      },
    ]
  } catch {
    ElMessage.error(t('analytics.load_recent_queries_failed'))
  }
}

/** 初始化所有数据 */
function initData() {
  loadQueryCount()
  loadDataVolume()
  loadRecentQueries()
}

// 组件挂载时加载数据
onMounted(() => initData())

// keep-alive 激活时刷新数据
onActivated(() => initData())
</script>

<style scoped>
.analytics-stat-home {
  padding: 20px;
  min-height: calc(100vh - 50px);
}
</style>
