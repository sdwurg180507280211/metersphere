<template>
  <div class="sidebar-container">
    <!-- 查询中心 -->
    <div class="nav-group">
      <div class="group-title">{{ $t('advanced_search.query_center') }}</div>
      <div 
        class="nav-item"
        :class="{ active: currentNav === 'advanced-search' }"
        @click="handleNavClick('advanced-search')"
      >
        <i class="el-icon-search"></i>
        <span>{{ $t('advanced_search.advanced_search') }}</span>
      </div>
      <div 
        class="nav-item"
        :class="{ active: currentNav === 'my-favorites' }"
        @click="handleNavClick('my-favorites')"
      >
        <i class="el-icon-star-off"></i>
        <span>{{ $t('advanced_search.my_favorites') }}</span>
      </div>
      <div 
        class="nav-item"
        :class="{ active: currentNav === 'recent-views' }"
        @click="handleNavClick('recent-views')"
      >
        <i class="el-icon-time"></i>
        <span>{{ $t('advanced_search.recent_views') }}</span>
      </div>
    </div>
    
    <!-- 共享视图（预留功能） -->
    <div class="nav-group">
      <div class="group-title">{{ $t('advanced_search.shared_views') }}</div>
      <div class="empty-tip">
        <i class="el-icon-folder-opened"></i>
        <span>{{ $t('advanced_search.no_shared_views') }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Sidebar',
  data() {
    return {
      currentNav: 'advanced-search'
    };
  },
  methods: {
    handleNavClick(nav) {
      this.currentNav = nav;
      this.$emit('nav-change', nav);
    }
  }
};
</script>

<style scoped>
.sidebar-container {
  padding: 16px 0;
}

.nav-group {
  margin-bottom: 24px;
}

.group-title {
  padding: 0 16px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: 500;
}

.nav-item {
  padding: 10px 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #606266;
  transition: all 0.3s;
}

.nav-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.nav-item.active {
  background-color: #ecf5ff;
  color: #409eff;
  border-right: 2px solid #409eff;
}

.nav-item i {
  margin-right: 8px;
  font-size: 16px;
}

.empty-tip {
  padding: 16px;
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
}

.empty-tip i {
  display: block;
  font-size: 32px;
  margin-bottom: 8px;
}
</style>
