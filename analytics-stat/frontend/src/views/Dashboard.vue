<template>
  <div class="dashboard">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card">
      <h1>欢迎使用分析统计模块</h1>
      <p>这是一个基于 Vue 2 + Vue CLI 构建的微前端应用</p>
    </el-card>

    <!-- 功能入口卡片 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- SQL 查询台 -->
      <el-col :span="8">
        <el-card>
          <div slot="header" class="card-header">
            <span>SQL查询台</span>
          </div>
          <div class="card-content">
            <i class="el-icon-document" style="font-size: 40px; color: #409EFF;"></i>
            <p>执行自定义SQL查询，支持历史记录</p>
            <el-button type="primary" @click="$router.push('/sql-console')">
              进入查询台
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 数据字典 -->
      <el-col :span="8">
        <el-card>
          <div slot="header" class="card-header">
            <span>数据字典</span>
          </div>
          <div class="card-content">
            <i class="el-icon-collection" style="font-size: 40px; color: #67C23A;"></i>
            <p>管理系统数据字典配置</p>
            <el-button type="success" @click="$router.push('/data-dictionary')">
              查看字典
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 统计图表 -->
      <el-col :span="8">
        <el-card>
          <div slot="header" class="card-header">
            <span>统计图表</span>
          </div>
          <div class="card-content">
            <i class="el-icon-data-analysis" style="font-size: 40px; color: #E6A23C;"></i>
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

<script>
/**
 * 数据概览页面
 * 
 * 功能：
 * 1. 展示欢迎信息
 * 2. 提供各功能模块的快捷入口
 */
export default {
  name: 'Dashboard'
};
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
