<template>
  <!-- 最近查询列表 -->
  <el-card shadow="never">
    <template #header>
      <span class="card-title">{{ t('analytics.recent_queries') }}</span>
    </template>

    <!-- 有数据时展示表格 -->
    <el-table
      v-if="queries.length > 0"
      :data="queries"
      style="width: 100%"
      @row-click="handleRowClick"
    >
      <el-table-column prop="name" :label="t('analytics.query_name')" min-width="200" />
      <el-table-column prop="type" :label="t('analytics.type')" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.type === 'sql'" type="primary" size="small">SQL</el-tag>
          <el-tag v-else type="success" size="small">{{ t('analytics.comprehensive_query') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="t('analytics.create_time')" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" :label="t('analytics.status')" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'success'" type="success" size="small">
            {{ t('analytics.success') }}
          </el-tag>
          <el-tag v-else type="danger" size="small">
            {{ t('analytics.failed') }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!-- 无数据时展示空状态 -->
    <div v-else class="empty-state">
      {{ t('analytics.no_query_records') }}
    </div>
  </el-card>
</template>

<script setup lang="ts">
/**
 * 最近查询列表组件
 *
 * 功能：
 * 1. 展示最近的查询记录（表格形式）
 * 2. 点击行可查看详情（TODO: 接入后端 API）
 *
 * 与 Vue 2 版本的差异：
 * - 使用 #default 插槽语法替代 slot-scope
 * - 使用 dayjs 替代手动格式化时间
 */
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'

const { t } = useI18n()

/** 查询记录类型 */
export interface QueryRecord {
  id: string
  name: string
  type: 'sql' | 'query'
  createTime: Date | string
  status: 'success' | 'failed'
}

defineProps<{
  queries: QueryRecord[]
}>()

/** 格式化时间 */
function formatTime(time: Date | string): string {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

/** 处理行点击 */
function handleRowClick(row: QueryRecord) {
  // TODO: 跳转到查询详情页面
  console.log('查看查询详情:', row)
}
</script>

<style scoped>
.card-title {
  font-size: 18px;
  font-weight: 500;
  color: #1f2329;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #8f959e;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
