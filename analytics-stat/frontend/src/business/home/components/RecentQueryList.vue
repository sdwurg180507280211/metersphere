<template>
  <el-card>
    <div slot="header" style="padding: 24px;">
      <span class="table-title">最近查询</span>
    </div>
    <div style="padding: 0 24px 24px 24px;">
      <el-table
        :data="queries"
        style="width: 100%"
        @row-click="handleRowClick">
        <el-table-column
          prop="name"
          label="查询名称"
          min-width="200">
        </el-table-column>
        <el-table-column
          prop="type"
          label="类型"
          width="120">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.type === 'sql'" type="primary" size="small">SQL</el-tag>
            <el-tag v-else type="success" size="small">综合查询</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="100">
          <template slot-scope="scope">
            <el-tag 
              v-if="scope.row.status === 'success'" 
              type="success" 
              size="small">
              成功
            </el-tag>
            <el-tag 
              v-else 
              type="danger" 
              size="small">
              失败
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      
      <div v-if="queries.length === 0" style="text-align: center; padding: 40px 0; color: #8f959e;">
        暂无查询记录
      </div>
    </div>
  </el-card>
</template>

<script>
/**
 * 最近查询列表
 * 
 * 功能：
 * 1. 展示最近的查询记录
 * 2. 点击查看详情
 */
export default {
  name: "RecentQueryList",
  
  props: {
    // 查询列表
    queries: {
      type: Array,
      default: () => []
    }
  },
  
  methods: {
    /**
     * 格式化时间
     */
    formatTime(time) {
      if (!time) return '-';
      const date = new Date(time);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    },
    
    /**
     * 处理行点击事件
     */
    handleRowClick(row) {
      // TODO: 跳转到查询详情页面
      console.log('查看查询详情:', row);
    }
  }
};
</script>

<style scoped>
:deep(.el-table__row) {
  cursor: pointer;
}

:deep(.el-table__row:hover) {
  background-color: #f5f6f7;
}
</style>
