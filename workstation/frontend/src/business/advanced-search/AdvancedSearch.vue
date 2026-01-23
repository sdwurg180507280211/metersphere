<template>
  <div class="advanced-search-container">
    <!-- 侧边栏 -->
    <sidebar class="sidebar" />
    
    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 顶部筛选栏 -->
      <top-filter-bar 
        @search="handleSearch"
        @module-change="handleModuleChange"
      />
      
      <!-- 已选条件标签栏 -->
      <active-tags-bar 
        v-if="queryMode === 'visual' && hasConditions"
        @remove-condition="handleRemoveCondition"
        @clear-all="handleClearAll"
      />
      
      <!-- JQL 查询编辑器 -->
      <jql-editor 
        v-if="queryMode === 'jql'"
        @execute-query="handleSearch"
      />
      
      <!-- 结果工具栏 -->
      <result-toolbar 
        v-if="results.total > 0"
        @view-mode-change="handleViewModeChange"
        @export="handleExport"
      />
      
      <!-- 结果展示区 -->
      <result-view 
        :data="results.list"
        :total="results.total"
        :loading="loading"
        @page-change="handlePageChange"
        @item-select="handleItemSelect"
      />
    </div>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store';
import { queryData, exportExcel } from '@/api/advanced-search';
import Sidebar from './components/Sidebar.vue';
import TopFilterBar from './components/TopFilterBar.vue';
import ActiveTagsBar from './components/ActiveTagsBar.vue';
import JQLEditor from './components/JQLEditor.vue';
import ResultToolbar from './components/ResultToolbar.vue';
import ResultView from './components/ResultView.vue';

export default {
  name: 'AdvancedSearch',
  components: {
    Sidebar,
    TopFilterBar,
    ActiveTagsBar,
    JQLEditor,
    ResultToolbar,
    ResultView
  },
  setup() {
    const store = useAdvancedSearchStore();
    return { store };
  },
  data() {
    return {
      loading: false
    };
  },
  computed: {
    queryMode() {
      return this.store.queryMode;
    },
    hasConditions() {
      return this.store.hasConditions;
    },
    results() {
      return this.store.results;
    }
  },
  methods: {
    /**
     * 执行查询
     */
    async handleSearch(pageNum = 1, pageSize = 20) {
      this.loading = true;
      this.store.setError(null);
      
      try {
        // 构建查询请求
        const request = {
          module: this.store.currentModule,
          workspaceIds: this.store.selectedWorkspaces,
          projectIds: this.store.selectedProjects,
          useJQL: this.store.queryMode === 'jql',
          orders: this.store.orders
        };
        
        // 根据查询模式添加条件
        if (this.store.queryMode === 'jql') {
          request.jql = this.store.jqlQuery;
        } else {
          request.combine = this.store.combine;
          request.filters = this.store.filters;
        }
        
        // 调用 API
        const response = await queryData(request, pageNum, pageSize);
        
        // 更新结果
        this.store.updateResults({
          total: response.data.itemCount,
          list: response.data.listObject,
          pageNum,
          pageSize
        });
        
        // 如果是详情模式且有结果，自动选中第一条
        if (this.store.viewMode === 'split' && response.data.listObject.length > 0) {
          this.store.selectItem(response.data.listObject[0]);
        }
      } catch (error) {
        this.$error(this.$t('advanced_search.query_failed') + ': ' + error.message);
        this.store.setError(error.message);
      } finally {
        this.loading = false;
      }
    },
    
    /**
     * 模块切换
     */
    handleModuleChange(module) {
      this.store.switchModule(module);
      // 清空结果
      this.store.updateResults({ total: 0, list: [], pageNum: 1, pageSize: 20 });
    },
    
    /**
     * 移除筛选条件
     */
    handleRemoveCondition(field) {
      this.store.removeCondition(field);
    },
    
    /**
     * 清空所有条件
     */
    handleClearAll() {
      this.store.clearConditions();
      // 清空结果
      this.store.updateResults({ total: 0, list: [], pageNum: 1, pageSize: 20 });
    },
    
    /**
     * 视图模式切换
     */
    handleViewModeChange(mode) {
      this.store.setViewMode(mode);
    },
    
    /**
     * 分页变更
     */
    handlePageChange(pageNum, pageSize) {
      this.handleSearch(pageNum, pageSize);
    },
    
    /**
     * 选中详情项
     */
    handleItemSelect(item) {
      this.store.selectItem(item);
    },
    
    /**
     * 导出
     */
    async handleExport() {
      try {
        // 构建查询请求（同查询逻辑）
        const request = {
          module: this.store.currentModule,
          workspaceIds: this.store.selectedWorkspaces,
          projectIds: this.store.selectedProjects,
          useJQL: this.store.queryMode === 'jql',
          orders: this.store.orders
        };
        
        if (this.store.queryMode === 'jql') {
          request.jql = this.store.jqlQuery;
        } else {
          request.combine = this.store.combine;
          request.filters = this.store.filters;
        }
        
        // 调用导出 API
        const response = await exportExcel(request);
        
        // 下载文件
        const blob = new Blob([response.data], { 
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
        });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${this.store.currentModule}_${Date.now()}.xlsx`;
        link.click();
        window.URL.revokeObjectURL(url);
        
        this.$success(this.$t('advanced_search.export_success'));
      } catch (error) {
        this.$error(this.$t('advanced_search.export_failed') + ': ' + error.message);
      }
    }
  }
};
</script>

<style scoped>
.advanced-search-container {
  display: flex;
  height: calc(100vh - 60px);
  background-color: #f5f7fa;
}

.sidebar {
  width: 240px;
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
}
</style>
