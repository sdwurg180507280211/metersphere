<template>
  <el-card class="dashboard-card">
    <div class="main-info-card">
      <div style="padding: 24px;">
        <div class="dashboard-title">数据量统计</div>
        <div class="common-amount" style="margin-top: 16px;">
          <span style="font-size: 32px; font-weight: 500; color: #1f2329;">{{ volume }}</span>
          <span style="margin-left: 8px; font-size: 18px; color: #8f959e;">{{ unit }}</span>
        </div>
        <div style="margin-top: 8px; color: #8f959e; font-size: 14px;">
          总数据量
        </div>
      </div>
    </div>
  </el-card>
</template>

<script>
/**
 * 数据量统计卡片
 * 
 * 功能：
 * 1. 展示数据量
 * 2. 展示单位（MB/GB/TB）
 */
export default {
  name: "DataVolumeCard",
  
  props: {
    // 数据量
    volume: {
      type: Number,
      default: 0
    },
    // 单位：'MB' | 'GB' | 'TB'
    unit: {
      type: String,
      default: 'MB',
      validator: (value) => ['MB', 'GB', 'TB'].includes(value)
    }
  }
};
</script>

<style scoped>
.dashboard-card {
  height: 208px;
  border: 0;
}

.main-info-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
</style>
