<template>
  <!-- 数据量统计卡片 -->
  <el-card class="dashboard-card" shadow="never">
    <div class="card-content">
      <div class="dashboard-title">{{ t('analytics.data_volume_stat') }}</div>
      <div class="amount-row">
        <span class="amount-number">{{ volume }}</span>
        <span class="amount-unit">{{ unit }}</span>
      </div>
      <div class="sub-text">{{ t('analytics.total_data_volume') }}</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
/**
 * 数据量统计卡片
 *
 * Props:
 * - volume: 数据量数值
 * - unit: 单位 'MB' | 'GB' | 'TB'
 */
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

withDefaults(defineProps<{
  volume: number
  unit: 'MB' | 'GB' | 'TB'
}>(), {
  volume: 0,
  unit: 'MB',
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
  align-items: baseline;
}

.amount-number {
  font-size: 32px;
  font-weight: 500;
  color: #1f2329;
}

.amount-unit {
  margin-left: 8px;
  font-size: 18px;
  color: #8f959e;
}

.sub-text {
  margin-top: 8px;
  color: #8f959e;
  font-size: 14px;
}
</style>
