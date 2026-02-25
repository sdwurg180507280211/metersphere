<template>
  <!-- 查询次数统计卡片 -->
  <el-card class="dashboard-card" shadow="never">
    <div class="card-content">
      <div class="dashboard-title">{{ t('analytics.query_count_stat') }}</div>
      <div class="amount-row">
        <span class="amount-number">{{ count }}</span>
        <span class="trend-icon">
          <!-- 趋势图标：上升/下降/持平 -->
          <el-icon v-if="trend === 'up'" color="#67C23A"><Top /></el-icon>
          <el-icon v-else-if="trend === 'down'" color="#F56C6C"><Bottom /></el-icon>
          <el-icon v-else color="#909399"><Minus /></el-icon>
        </span>
      </div>
      <div class="sub-text">{{ t('analytics.total_query_count') }}</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
/**
 * 查询次数统计卡片
 *
 * Props:
 * - count: 查询次数
 * - trend: 趋势方向 'up' | 'down' | 'stable'
 */
import { useI18n } from 'vue-i18n'
import { Top, Bottom, Minus } from '@element-plus/icons-vue'

const { t } = useI18n()

withDefaults(defineProps<{
  count: number
  trend: 'up' | 'down' | 'stable'
}>(), {
  count: 0,
  trend: 'stable',
})
</script>

<style scoped>
.dashboard-card {
  height: 208px;
  border: 1px solid #dee0e3;
  border-radius: 4px;
}

.card-content {
  padding: 8px;
}

.dashboard-title {
  font-size: 18px;
  font-weight: 500;
  color: #1f2329;
}

.amount-row {
  margin-top: 16px;
  display: flex;
  align-items: center;
}

.amount-number {
  font-size: 32px;
  font-weight: 500;
  color: #1f2329;
}

.trend-icon {
  margin-left: 8px;
  font-size: 20px;
}

.sub-text {
  margin-top: 8px;
  color: #8f959e;
  font-size: 14px;
}
</style>
