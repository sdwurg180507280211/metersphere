/**
 * 高级检索 Store
 * 
 * 管理高级检索的状态和业务逻辑
 */

import { defineStore } from 'pinia';
import * as api from '@/api/advanced-search';

export const useAdvancedSearchStore = defineStore('advancedSearch', {
  state: () => ({
    // 当前选择的业务模块
    currentModule: 'TEST_CASE',

    // 选中的工作空间列表
    selectedWorkspaces: [],

    // 选中的项目列表
    selectedProjects: [],

    // 查询模式：'visual'（可视化）或 'jql'（JQL模式）
    queryMode: 'visual',

    // JQL 查询语句
    jqlQuery: '',

    // 可视化模式的查询条件（旧格式，保留兼容）
    combine: {},

    // 可视化模式的条件标签数组（新格式）
    conditions: [],

    // 快速筛选条件
    filters: {},

    // 用户选择的列
    userSelectedColumns: [],

    // 当前选中的行（分屏视图）
    selectedRow: null,

    // 查询结果列表
    results: [],

    // 分页信息
    pagination: {
      current: 1,
      pageSize: 10,
      total: 0
    },

    // 视图模式：'list'（列表）或 'split'（分屏）
    viewMode: 'list',

    // 详情数据
    detailData: null,
    
    // 字段元数据
    fieldMetadata: [],
    
    // 加载状态
    loading: false,
    
    // 工作空间列表
    workspaces: [],
    
    // 项目列表
    projects: []
  }),
  
  getters: {
    /**
     * 是否为单项目模式
     * 单项目模式下可以使用项目自定义字段
     */
    isSingleProjectMode: (state) => {
      return state.selectedProjects.length === 1;
    },

    /**
     * 可用的字段列表
     * 根据是否为单项目模式过滤字段
     */
    availableFields: (state) => {
      const fields = Array.isArray(state.fieldMetadata) ? state.fieldMetadata : [];
      if (state.isSingleProjectMode) {
        // 单项目模式：返回所有字段
        return fields;
      } else {
        // 跨项目模式：只返回系统字段和全局自定义字段
        return fields.filter(field =>
          field && (field.group === 'system' || field.projectSpecific === false)
        );
      }
    },

    /**
     * 从 conditions 构建 combine 查询条件
     */
    combineFromConditions: (state) => {
      const combine = {};
      state.conditions.forEach(cond => {
        if (cond.value && (Array.isArray(cond.value) ? cond.value.length > 0 : cond.value !== '')) {
          combine[cond.field] = {
            operator: cond.op,
            value: cond.value
          };
        }
      });
      return combine;
    }
  },
  
  actions: {
    /**
     * 添加筛选条件
     */
    addCondition(field) {
      if (!this.conditions.some(c => c.field === field.field || c.field === field.value)) {
        // 根据字段类型设置默认操作符
        let defaultOp = '=';
        const fieldType = field.type || 'text';
        if (fieldType === 'text') defaultOp = 'like';
        if (fieldType === 'select' || fieldType === 'user') defaultOp = 'in';
        if (fieldType === 'date') defaultOp = 'between';

        this.conditions.push({
          field: field.field || field.value,
          op: defaultOp,
          value: fieldType === 'select' || fieldType === 'user' ? [] : ''
        });
      }
    },

    /**
     * 移除筛选条件
     */
    removeCondition(index) {
      this.conditions.splice(index, 1);
    },

    /**
     * 清空所有条件
     */
    clearConditions() {
      this.conditions = [];
    },

    /**
     * 执行查询
     */
    async executeQuery() {
      this.loading = true;

      try {
        // 使用新的 conditions 格式构建 combine
        const combine = this.queryMode === 'visual' ? this.combineFromConditions : this.combine;

        const request = {
          module: this.currentModule,
          workspaceIds: this.selectedWorkspaces,
          projectIds: this.selectedProjects,
          useJQL: this.queryMode === 'jql',
          jql: this.jqlQuery,
          combine: combine,
          filters: this.filters
        };
        
        const response = await api.queryData(
          request,
          this.pagination.current,
          this.pagination.pageSize
        );
        
        this.results = response.data.listObject || [];
        this.pagination.total = response.data.itemCount || 0;
        
        return response;
      } catch (error) {
        console.error('查询失败:', error);
        throw error;
      } finally {
        this.loading = false;
      }
    },
    
    /**
     * 加载字段元数据
     */
    async loadFieldMetadata() {
      try {
        const projectId = this.isSingleProjectMode ? this.selectedProjects[0] : null;
        const response = await api.getFieldMetadata(this.currentModule, projectId);
        this.fieldMetadata = response.data || [];
      } catch (error) {
        console.error('加载字段元数据失败:', error);
        throw error;
      }
    },
    
    /**
     * 加载工作空间列表
     */
    async loadWorkspaces() {
      try {
        const response = await api.getWorkspaces();
        this.workspaces = response.data || [];
      } catch (error) {
        console.error('加载工作空间列表失败:', error);
        throw error;
      }
    },
    
    /**
     * 加载项目列表
     */
    async loadProjects() {
      try {
        // 如果没有选择工作空间，清空项目列表
        if (this.selectedWorkspaces.length === 0) {
          this.clearProjects();
          return;
        }
        const workspaceIds = this.selectedWorkspaces.join(',');
        const response = await api.getProjects(workspaceIds);
        this.projects = response.data || [];
      } catch (error) {
        console.error('加载项目列表失败:', error);
        this.projects = [];
        throw error;
      }
    },

    /**
     * 清空项目列表和选择
     */
    clearProjects() {
      this.projects = [];
      this.selectedProjects = [];
    },
    
    /**
     * 项目选择变更处理
     */
    async onProjectChange() {
      // 重新加载字段元数据
      await this.loadFieldMetadata();
      
      // 如果切换到跨项目模式，清理项目自定义字段的筛选条件
      if (!this.isSingleProjectMode) {
        this.clearProjectSpecificFilters();
      }
    },
    
    /**
     * 清理项目自定义字段的筛选条件
     */
    clearProjectSpecificFilters() {
      // 清理 combine 中的项目自定义字段
      const systemFields = this.fieldMetadata
        .filter(f => f.group === 'system' || f.projectSpecific === false)
        .map(f => f.field);
      
      const newCombine = {};
      Object.keys(this.combine).forEach(key => {
        if (systemFields.includes(key)) {
          newCombine[key] = this.combine[key];
        }
      });
      this.combine = newCombine;
      
      // 清理 filters 中的项目自定义字段
      const newFilters = {};
      Object.keys(this.filters).forEach(key => {
        if (systemFields.includes(key)) {
          newFilters[key] = this.filters[key];
        }
      });
      this.filters = newFilters;
    },
    
    /**
     * 切换业务模块
     */
    async switchModule(module) {
      this.currentModule = module;
      this.combine = {};
      this.conditions = [];
      this.filters = {};
      this.jqlQuery = '';
      this.results = [];
      this.pagination.current = 1;
      this.userSelectedColumns = [];
      this.selectedRow = null;

      await this.loadFieldMetadata();
    },
    
    /**
     * 切换查询模式
     */
    switchQueryMode(mode) {
      this.queryMode = mode;
      
      // 切换模式时清空查询条件
      if (mode === 'jql') {
        this.combine = {};
        this.filters = {};
      } else {
        this.jqlQuery = '';
      }
    },
    
    /**
     * 验证 JQL 语法
     */
    async validateJQL(jql) {
      try {
        const response = await api.validateJQL(jql, this.currentModule);
        return response.data;
      } catch (error) {
        console.error('JQL 验证失败:', error);
        throw error;
      }
    },
    
    /**
     * 获取 JQL 智能提示
     */
    async getJQLSuggestions(context, cursorPosition) {
      try {
        const response = await api.getJQLSuggestions(
          context,
          this.currentModule,
          cursorPosition
        );
        return response.data || [];
      } catch (error) {
        console.error('获取 JQL 智能提示失败:', error);
        return [];
      }
    },
    
    /**
     * 获取详情
     */
    async loadDetail(id) {
      try {
        const response = await api.getDetail(this.currentModule, id);
        this.detailData = response.data;
        return response.data;
      } catch (error) {
        console.error('加载详情失败:', error);
        throw error;
      }
    },
    
    /**
     * 导出 Excel
     */
    async exportExcel() {
      try {
        const request = {
          module: this.currentModule,
          workspaceIds: this.selectedWorkspaces,
          projectIds: this.selectedProjects,
          useJQL: this.queryMode === 'jql',
          jql: this.jqlQuery,
          combine: this.combine,
          filters: this.filters
        };
        
        const response = await api.exportExcel(request);
        
        // 创建下载链接
        const url = window.URL.createObjectURL(new Blob([response]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `advanced-search-${Date.now()}.xlsx`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      } catch (error) {
        console.error('导出失败:', error);
        throw error;
      }
    },
    
    /**
     * 切换视图模式
     */
    switchViewMode(mode) {
      this.viewMode = mode;
      if (mode === 'list') {
        this.selectedRow = null;
        this.detailData = null;
      }
    },
    
    /**
     * 重置状态
     */
    reset() {
      this.currentModule = 'TEST_CASE';
      this.selectedWorkspaces = [];
      this.selectedProjects = [];
      this.queryMode = 'visual';
      this.jqlQuery = '';
      this.combine = {};
      this.filters = {};
      this.results = [];
      this.pagination = {
        current: 1,
        pageSize: 10,
        total: 0
      };
      this.selectedRow = null;
      this.detailData = null;
    }
  },
  
  // 持久化配置
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'advanced-search',
        storage: localStorage,
        paths: ['viewMode', 'queryMode']
      }
    ]
  }
});
