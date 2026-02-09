<template>
  <el-card class="dashboard-card">
    <div class="main-info-card">
      <div style="padding: 24px;">
        <div class="dashboard-title">查询次数统计</div>
        <div class="common-amount" style="margin-top: 16px;">
          <span style="font-size: 32px; font-weight: 500; color: #1f2329;">{{ count }}</span>
          <span style="margin-left: 8px;">
            <i v-if="trend === 'up'" class="el-icon-top" style="color: #67C23A;"></i>
            <i v-else-if="trend === 'down'" class="el-icon-bottom" style="color: #F56C6C;"></i>
            <i v-else class="el-icon-minus" style="color: #909399;"></i>
          </span>
        </div>
        <div style="margin-top: 8px; color: #8f959e; font-size: 14px;">
          总查询次数
        </div>
      </div>
    </div>
  </el-card>
</template>

<script>
/**
 * 查询次数统计卡片
 * 
 * 功能：
 * 1. 展示查询次数
 * 2. 展示趋势（上升/下降/持平）
 */
export default {
  name: "QueryCountCard",
  
  props: {
    // 查询次数
    count: {
      type: Number,
      default: 0
    },
    // 趋势：'up' | 'down' | 'stable'
    trend: {
      type: String,
      default: 'stable',
      validator: (value) => ['up', 'down', 'stable'].includes(value)
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
