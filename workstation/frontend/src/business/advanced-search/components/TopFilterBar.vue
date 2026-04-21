<template>
  <div class="top-filter-bar">
    <div class="filter-row">
      <!-- 业务模块选择器 -->
      <el-select 
        v-model="currentModule" 
        class="module-selector"
        @change="handleModuleChange"
      >
        <el-option
          v-for="module in modules"
          :key="module.value"
          :label="$t(module.label)"
          :value="module.value"
        >
          <i :class="module.icon"></i>
          <span>{{ $t(module.label) }}</span>
        </el-option>
      </el-select>
      
      <!-- 工作空间多选 -->
      <el-select
        v-model="selectedWorkspaces"
        multiple
        collapse-tags
        :placeholder="$t('advanced_search.select_workspace')"
        class="workspace-selector"
        @change="handleWorkspaceChange"
      >
        <el-option
          v-for="ws in workspaces"
          :key="ws.id"
          :label="ws.name"
          :value="ws.id"
        />
      </el-select>
      
      <!-- 项目多选 -->
      <el-select
        v-model="selectedProjects"
        multiple
        collapse-tags
        :placeholder="$t('advanced_search.select_project')"
        class="project-selector"
        :disabled="selectedWorkspaces.length === 0"
        @change="handleProjectChange"
      >
        <el-option
          v-for="proj in filteredProjects"
          :key="proj.id"
          :label="proj.name"
          :value="proj.id"
        />
      </el-select>
      
      <!-- 查询模式切换 -->
      <query-mode-switch 
        v-model="queryMode"
        @change="handleQueryModeChange"
      />
      
      <!-- 筛选条件按钮（可视化模式） -->
      <el-popover
        v-if="queryMode === 'visual'"
        placement="bottom-start"
        width="400"
        trigger="click"
      >
        <filter-popover 
          :fields="availableFields"
          @add-condition="handleAddCondition"
        />
        <template #reference>
          <el-button icon="el-icon-plus">
            {{ $t('advanced_search.add_filter') }}
          </el-button>
        </template>
      </el-popover>
      
      <!-- 查询按钮 -->
      <el-button 
        type="primary" 
        icon="el-icon-search"
        @click="handleSearch"
      >
        {{ $t('advanced_search.search') }}
      </el-button>
    </div>
    
    <!-- 跨项目模式提示 -->
    <div v-if="selectedProjects.length > 1" class="cross-project-tip">
      <i class="el-icon-warning"></i>
      <span>{{ $t('advanced_search.cross_project_tip') }}</span>
    </div>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store';
import { getFieldMetadata, getWorkspaces, getProjects } from '@/api/advanced-search';
import FilterPopover from './FilterPopover.vue';
import QueryModeSwitch from './QueryModeSwitch.vue';

export default {
  name: 'TopFilterBar',
  components: {
    FilterPopover,
    QueryModeSwitch
  },
  setup() {
    const store = useAdvancedSearchStore();
    return { store };
  },
  data() {
    return {
      modules: [
        { value: 'TEST_CASE', label: 'advanced_search.test_case', icon: 'el-icon-document' },
        { value: 'ISSUE', label: 'advanced_search.issue', icon: 'el-icon-warning' },
        { value: 'TEST_PLAN', label: 'advanced_search.test_plan', icon: 'el-icon-s-order' },
        { value: 'TEST_CASE_REVIEW', label: 'advanced_search.test_case_review', icon: 'el-icon-view' }
      ],
      workspaces: [],
      projects: []
    };
  },
  computed: {
    currentModule: {
      get() {
        return this.store.currentModule;
      },
      set(value) {
        this.store.currentModule = value;
      }
    },
    selectedWorkspaces: {
      get() {
        return this.store.selectedWorkspaces;
      },
      set(value) {
        this.store.selectedWorkspaces = value;
      }
    },
    selectedProjects: {
      get() {
        return this.store.selectedProjects;
      },
      set(value) {
        this.store.selectedProjects = value;
      }
    },
    queryMode: {
      get() {
        return this.store.queryMode;
      },
      set(value) {
        this.store.queryMode = value;
      }
    },
    availableFields() {
      return this.store.availableFields;
    },
    filteredProjects() {
      if (this.selectedWorkspaces.length === 0) {
        return this.projects;
      }
      return this.projects.filter(p => 
        this.selectedWorkspaces.includes(p.workspaceId)
      );
    }
  },
  mounted() {
    this.loadWorkspaces();
    this.loadProjects();
    this.loadFieldMetadata();
  },
  methods: {
    async loadWorkspaces() {
      try {
        const response = await getWorkspaces();
        if (response.data) {
          this.workspaces = response.data;
        }
      } catch (error) {
        this.$error(this.$t('advanced_search.load_workspaces_failed'));
        console.error('Failed to load workspaces:', error);
      }
    },
    
    async loadProjects() {
      try {
        const response = await getProjects();
        if (response.data) {
          this.projects = response.data;
        }
      } catch (error) {
        this.$error(this.$t('advanced_search.load_projects_failed'));
        console.error('Failed to load projects:', error);
      }
    },
    
    async loadFieldMetadata() {
      try {
        const projectId = this.store.singleProjectId;
        const response = await getFieldMetadata(this.currentModule, projectId);
        this.store.loadFieldMetadata(response.data);
      } catch (error) {
        this.$error(this.$t('advanced_search.load_fields_failed'));
      }
    },
    
    handleModuleChange(module) {
      this.$emit('module-change', module);
      this.loadFieldMetadata();
    },
    
    handleWorkspaceChange() {
      // 过滤掉不属于已选工作空间的项目
      this.selectedProjects = this.selectedProjects.filter(projId => {
        const project = this.projects.find(p => p.id === projId);
        return project && this.selectedWorkspaces.includes(project.workspaceId);
      });
    },
    
    handleProjectChange(projectIds) {
      this.store.onProjectChange(projectIds);
      this.loadFieldMetadata();
    },
    
    handleQueryModeChange(mode) {
      this.store.switchQueryMode(mode);
    },
    
    handleAddCondition(field, operator, value) {
      this.store.addCondition(field, operator, value);
    },
    
    handleSearch() {
      this.$emit('search');
    }
  }
};
</script>

<style scoped>
.top-filter-bar {
  background-color: #fff;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.module-selector {
  width: 180px;
}

.workspace-selector,
.project-selector {
  width: 200px;
}

.cross-project-tip {
  margin-top: 12px;
  padding: 8px 12px;
  background-color: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
  color: #d46b08;
  font-size: 12px;
  display: flex;
  align-items: center;
}

.cross-project-tip i {
  margin-right: 8px;
}
</style>
