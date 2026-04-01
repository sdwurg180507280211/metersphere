<template>
  <div class="advanced-search-page">
    <!-- 顶部筛选栏 -->
    <div class="top-filter-bar">
      <!-- 业务模块选择 -->
      <el-select
        v-model="store.currentModule"
        :placeholder="$t('advanced_search.select_module')"
        @change="onModuleChange"
      >
        <el-option
          value="TEST_CASE"
          :label="$t('advanced_search.module_test_case')"
        />
        <el-option
          value="ISSUE"
          :label="$t('advanced_search.module_issue')"
        />
        <el-option
          value="TEST_PLAN"
          :label="$t('advanced_search.module_test_plan')"
        />
        <el-option
          value="TEST_CASE_REVIEW"
          :label="$t('advanced_search.module_review')"
        />
      </el-select>

      <!-- 工作空间选择 -->
      <el-select
        v-model="store.selectedWorkspaces"
        multiple
        collapse-tags
        :placeholder="$t('advanced_search.select_workspace')"
        @change="onWorkspaceChange"
      >
        <el-option
          v-for="ws in store.workspaces"
          :key="ws.id"
          :value="ws.id"
          :label="ws.name"
        />
      </el-select>

      <!-- 项目选择 -->
      <el-select
        v-model="store.selectedProjects"
        multiple
        collapse-tags
        :placeholder="$t('advanced_search.select_project')"
        @change="onProjectChange"
      >
        <el-option
          v-for="proj in store.projects"
          :key="proj.id"
          :value="proj.id"
          :label="proj.name"
        />
      </el-select>

      <!-- 查询模式切换 -->
      <el-radio-group
        v-model="store.queryMode"
        size="small"
        @change="onQueryModeChange"
      >
        <el-radio-button label="visual">
          {{ $t('advanced_search.mode_visual') }}
        </el-radio-button>
        <el-radio-button label="jql">
          {{ $t('advanced_search.mode_jql') }}
        </el-radio-button>
      </el-radio-group>

      <!-- 查询按钮 -->
      <el-button
        type="primary"
        icon="el-icon-search"
        :loading="store.loading"
        @click="handleSearch"
      >
        {{ $t('commons.search') }}
      </el-button>

      <!-- 导出按钮 -->
      <el-button
        icon="el-icon-download"
        @click="handleExport"
      >
        {{ $t('commons.export') }}
      </el-button>
    </div>

    <!-- 查询条件区域 -->
    <div class="query-condition-area">
      <!-- JQL 模式 -->
      <jql-editor
        v-if="store.queryMode === 'jql'"
        v-model="store.jqlQuery"
      />

      <!-- 可视化模式 -->
      <div v-else class="visual-query">
        <el-alert
          v-if="!store.isSingleProjectMode && store.selectedProjects.length > 1"
          type="info"
          :closable="false"
          show-icon
        >
          {{ $t('advanced_search.cross_project_tip') }}
        </el-alert>

        <!-- 这里可以添加可视化查询条件组件 -->
        <div class="visual-filters">
          <p>{{ $t('advanced_search.visual_mode_placeholder') }}</p>
        </div>
      </div>
    </div>

    <!-- 结果展示区域 -->
    <div class="result-area">
      <!-- 工具栏 -->
      <div class="result-toolbar">
        <span class="result-count">
          {{ $t('advanced_search.result_count', { count: store.pagination.total }) }}
        </span>

        <div class="toolbar-actions">
          <!-- 视图切换 -->
          <el-radio-group
            v-model="store.viewMode"
            size="small"
            @change="onViewModeChange"
          >
            <el-radio-button label="list">
              <i class="el-icon-s-grid"></i>
              {{ $t('advanced_search.view_list') }}
            </el-radio-button>
            <el-radio-button label="split">
              <i class="el-icon-s-unfold"></i>
              {{ $t('advanced_search.view_split') }}
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-if="store.viewMode === 'list'" class="list-view">
        <el-table
          v-loading="store.loading"
          :data="store.results"
          border
          stripe
          @row-click="handleRowClick"
        >
          <el-table-column
            prop="num"
            :label="$t('commons.id')"
            width="100"
          />
          <el-table-column
            prop="name"
            :label="$t('commons.name')"
            min-width="200"
            show-overflow-tooltip
          />
          <el-table-column
            prop="status"
            :label="$t('commons.status')"
            width="120"
          />
          <el-table-column
            prop="projectName"
            :label="$t('commons.project')"
            width="150"
            show-overflow-tooltip
          />
          <el-table-column
            prop="createUserName"
            :label="$t('commons.create_user')"
            width="120"
          />
          <el-table-column
            prop="createTime"
            :label="$t('commons.create_time')"
            width="180"
          >
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <el-pagination
          :current-page="store.pagination.current"
          :page-size="store.pagination.pageSize"
          :total="store.pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>

      <!-- 分屏视图 -->
      <div v-else class="split-view">
        <div class="split-list">
          <el-table
            v-loading="store.loading"
            :data="store.results"
            border
            stripe
            highlight-current-row
            @row-click="handleRowClick"
          >
            <el-table-column
              prop="num"
              :label="$t('commons.id')"
              width="100"
            />
            <el-table-column
              prop="name"
              :label="$t('commons.name')"
              show-overflow-tooltip
            />
          </el-table>
        </div>

        <div class="split-detail">
          <div v-if="store.detailData" class="detail-content">
            <h3>{{ store.detailData.name }}</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('commons.id')">
                {{ store.detailData.num }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('commons.status')">
                {{ store.detailData.status }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('commons.project')">
                {{ store.detailData.projectName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('commons.workspace')">
                {{ store.detailData.workspaceName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('commons.create_user')">
                {{ store.detailData.createUserName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('commons.create_time')">
                {{ formatTime(store.detailData.createTime) }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <div v-else class="detail-placeholder">
            <i class="el-icon-info"></i>
            <p>{{ $t('advanced_search.select_row_to_view_detail') }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store/advancedSearch';
import JQLEditor from './JQLEditor.vue';
import { formatTime } from '@/utils/format';

export default {
  name: 'AdvancedSearch',
  
  components: {
    JQLEditor
  },
  
  data() {
    return {
      store: useAdvancedSearchStore()
    };
  },
  
  async mounted() {
    await this.store.loadWorkspaces();
    await this.store.loadFieldMetadata();
  },
  
  methods: {
    async onModuleChange() {
      await this.store.switchModule(this.store.currentModule);
    },
    
    async onWorkspaceChange() {
      await this.store.loadProjects();
      this.store.selectedProjects = [];
    },
    
    async onProjectChange() {
      await this.store.onProjectChange();
    },
    
    onQueryModeChange() {
      this.store.switchQueryMode(this.store.queryMode);
    },
    
    onViewModeChange() {
      this.store.switchViewMode(this.store.viewMode);
    },
    
    async handleSearch() {
      try {
        await this.store.executeQuery();
        this.$message.success(this.$t('commons.search_success'));
      } catch (error) {
        this.$message.error(this.$t('commons.search_failed'));
      }
    },
    
    async handleExport() {
      try {
        await this.store.exportExcel();
        this.$message.success(this.$t('commons.export_success'));
      } catch (error) {
        this.$message.error(this.$t('commons.export_failed'));
      }
    },
    
    async handleRowClick(row) {
      if (this.store.viewMode === 'split') {
        try {
          await this.store.loadDetail(row.id);
        } catch (error) {
          this.$message.error(this.$t('commons.load_failed'));
        }
      }
    },
    
    handleSizeChange(size) {
      this.store.pagination.pageSize = size;
      this.handleSearch();
    },
    
    handleCurrentChange(page) {
      this.store.pagination.current = page;
      this.handleSearch();
    },
    
    formatTime(timestamp) {
      return formatTime(timestamp);
    }
  }
};
</script>

<style scoped lang="scss">
.advanced-search-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.top-filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  
  .el-select {
    width: 200px;
  }
}

.query-condition-area {
  margin-bottom: 20px;
  padding: 16px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.visual-query {
  .visual-filters {
    padding: 20px;
    text-align: center;
    color: #909399;
  }
}

.result-area {
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  padding: 16px;
}

.result-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  .result-count {
    font-size: 14px;
    color: #606266;
  }
}

.list-view {
  .el-pagination {
    margin-top: 16px;
    text-align: right;
  }
}

.split-view {
  display: flex;
  gap: 16px;
  height: 600px;
  
  .split-list {
    flex: 1;
    overflow-y: auto;
  }
  
  .split-detail {
    flex: 1;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 16px;
    overflow-y: auto;
    
    .detail-content {
      h3 {
        margin: 0 0 16px;
        font-size: 18px;
        color: #303133;
      }
    }
    
    .detail-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      color: #909399;
      
      i {
        font-size: 48px;
        margin-bottom: 16px;
      }
      
      p {
        font-size: 14px;
      }
    }
  }
}
</style>
