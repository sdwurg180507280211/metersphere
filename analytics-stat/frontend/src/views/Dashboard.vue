<template>
  <div class="dashboard">
    <el-card class="welcome-card">
      <h1>欢迎使用分析统计模块</h1>
      <p>这是一个基于 Vue 3 + Vite + TypeScript 构建的微前端应用</p>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>SQL查询台</span>
            </div>
          </template>
          <div class="card-content">
            <el-icon :size="40" color="#409EFF"><Document /></el-icon>
            <p>执行自定义SQL查询，支持历史记录</p>
            <el-button type="primary" @click="$router.push('/analytics-stat/sql-console')">
              进入查询台
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>数据字典</span>
            </div>
          </template>
          <div class="card-content">
            <el-icon :size="40" color="#67C23A"><Collection /></el-icon>
            <p>管理系统数据字典配置</p>
            <el-button type="success" @click="$router.push('/analytics-stat/data-dictionary')">
              查看字典
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>统计图表</span>
            </div>
          </template>
          <div class="card-content">
            <el-icon :size="40" color="#E6A23C"><DataAnalysis /></el-icon>
            <p>多维度数据统计与可视化</p>
            <el-button type="warning" disabled>
              即将上线
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
// Dashboard 页面
</script>

<style scoped>
.dashboard {
  width: 100%;
}

.welcome-card {
  text-align: center;
  padding: 40px 0;
}

.welcome-card h1 {
  font-size: 32px;
  margin-bottom: 20px;
  color: #303133;
}

.welcome-card p {
  font-size: 16px;
  color: #606266;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}

.card-content {
  text-align: center;
  padding: 20px 0;
}

.card-content p {
  margin: 20px 0;
  color: #606266;
}
</style>
