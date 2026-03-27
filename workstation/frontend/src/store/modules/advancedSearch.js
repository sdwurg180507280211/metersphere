/**
 * 高级检索 Pinia Store
 * 
 * 管理高级检索功能的状态，包括：
 * - 查询条件（业务模块、工作空间、项目、筛选条件）
 * - 查询模式（可视化/JQL）
 * - 查询结果
 * - 视图模式（列表/详情）
 * - 字段元数据缓存
 */

export default {
  id: 'advancedSearch',
  
  state: () => ({
    // 当前选中的业务模块
    currentModule: 'TEST_CASE',
    
    // 已选工作空间ID列表
    selectedWorkspaces: [],
    
    // 已选项目ID列表
    selectedProjects: [],
    
    // 查询模式：'visual'（可视化）| 'jql'（JQL语法）
    queryMode: 'visual',
    
    // JQL查询字符串
    jqlQuery: '',
    
    // 筛选条件（combine 格式，用于可视化模式）
    combine: {},
    
    // 过滤条件（filters 格式）
    filters: {},
    
    // 排序条件
    orders: [{ name: 'update_time', type: 'desc' }],
    
    // 查询结果
    results: {
      total: 0,
      list: [],
      pageNum: 1,
      pageSize: 20
    },
    
    // 视图模式: 'list'（列表）| 'split'（分屏详情）
    viewMode: 'list',
    
    // 当前选中的详情项
    selectedItem: null,
    
    // 字段元数据缓存（按模块和项目ID缓存）
    fieldMetadata: {
      systemFields: [],
      customFields: [],
      fieldGroups: []
    },
    
    // 列配置（按模块保存）
    columnConfig: {
      TEST_CASE: [],
      ISSUE: [],
      TEST_PLAN: [],
      TEST_CASE_REVIEW: []
    },
    
    // 加载状态
    loading: false,
    
    // 错误信息
    error: null
  }),
  
  getters: {
    /**
     * 是否为单项目模式
     * 只有选择单个项目时才能使用项目自定义字段
     */
    isSingleProjectMode: (state) => {
      return state.selectedProjects.length === 1;
    },
    
    /**
     * 当前单选的项目ID
     */
    singleProjectId: (state) => {
      return state.selectedProjects.length === 1 ? state.selectedProjects[0] : null;
    },
    
    /**
     * 可用的筛选字段（根据项目选择动态变化）
     * 跨项目模式：仅系统字段
     * 单项目模式：系统字段 + 该项目的自定义字段
     */
    availableFields: (state) => {
      if (state.selectedProjects.length === 1) {
        return [...state.fieldMetadata.systemFields, ...state.fieldMetadata.customFields];
      }
      return state.fieldMetadata.systemFields || [];
    },
    
    /**
     * 当前模块的列配置
     */
    currentColumnConfig: (state) => {
      return state.columnConfig[state.currentModule] || [];
    },
    
    /**
     * 是否有查询条件
     */
    hasConditions: (state) => {
      if (state.queryMode === 'jql') {
        return state.jqlQuery && state.jqlQuery.trim().length > 0;
      }
      return Object.keys(state.combine).length > 0 || Object.keys(state.filters).length > 0;
    }
  },
  
  actions: {
    /**
     * 执行查询
     */
    async executeQuery(pageNum = 1, pageSize = 20) {
      // 实际的查询逻辑在组件中调用 API
      // 这里只更新分页参数
      this.results.pageNum = pageNum;
      this.results.pageSize = pageSize;
    },
    
    /**
     * 切换查询模式
     */
    switchQueryMode(mode) {
      this.queryMode = mode;
      if (mode === 'jql') {
        // 将当前 combine 条件转换为 JQL
        this.jqlQuery = this.convertCombineToJQL();
      } else {
        // 清空 JQL，使用可视化条件
        this.jqlQuery = '';
      }
    },
    
    /**
     * 将 combine 条件转换为 JQL
     */
    convertCombineToJQL() {
      const conditions = [];
      
      // 遍历 combine 对象，转换为 JQL 语法
      for (const [field, condition] of Object.entries(this.combine)) {
        const { operator, value } = condition;
        
        switch (operator) {
          case 'like':
            conditions.push(`${field} ~ "${value}"`);
            break;
          case '=':
            conditions.push(`${field} = "${value}"`);
            break;
          case '!=':
            conditions.push(`${field} != "${value}"`);
            break;
          case 'in': {
            const values = Array.isArray(value) ? value : [value];
            const quotedValues = values.map(v => `"${v}"`).join(', ');
            conditions.push(`${field} IN (${quotedValues})`);
            break;
          }
          case 'between':
            if (Array.isArray(value) && value.length === 2) {
              conditions.push(`${field} >= "${value[0]}" AND ${field} <= "${value[1]}"`);
            }
            break;
          case '>':
            conditions.push(`${field} > "${value}"`);
            break;
          case '>=':
            conditions.push(`${field} >= "${value}"`);
            break;
          case '<':
            conditions.push(`${field} < "${value}"`);
            break;
          case '<=':
            conditions.push(`${field} <= "${value}"`);
            break;
        }
      }
      
      return conditions.join(' AND ');
    },
    
    /**
     * 添加筛选条件
     */
    addCondition(field, operator, value) {
      this.combine[field] = { operator, value };
    },
    
    /**
     * 移除筛选条件
     */
    removeCondition(field) {
      delete this.combine[field];
    },
    
    /**
     * 清空所有条件
     */
    clearConditions() {
      this.combine = {};
      this.filters = {};
      this.jqlQuery = '';
    },
    
    /**
     * 切换视图模式
     */
    setViewMode(mode) {
      this.viewMode = mode;
    },
    
    /**
     * 加载字段元数据
     */
    loadFieldMetadata(metadata) {
      this.fieldMetadata = metadata;
    },
    
    /**
     * 切换模块时重置状态
     */
    switchModule(module) {
      this.currentModule = module;
      this.combine = {};
      this.filters = {};
      this.jqlQuery = '';
      this.results = { total: 0, list: [], pageNum: 1, pageSize: 20 };
      this.selectedItem = null;
    },
    
    /**
     * 项目选择变更时更新可用字段
     */
    onProjectChange(projectIds) {
      this.selectedProjects = projectIds;
      // 清除不再可用的筛选条件
      this.clearInvalidConditions();
    },
    
    /**
     * 清除不再可用的筛选条件
     * 当从单项目模式切换到跨项目模式时，移除项目自定义字段的条件
     */
    clearInvalidConditions() {
      if (this.selectedProjects.length !== 1) {
        // 跨项目模式，移除自定义字段条件
        const systemFieldNames = this.fieldMetadata.systemFields.map(f => f.field);
        const newCombine = {};
        
        for (const [field, condition] of Object.entries(this.combine)) {
          if (systemFieldNames.includes(field)) {
            newCombine[field] = condition;
          }
        }
        
        this.combine = newCombine;
      }
    },
    
    /**
     * 更新查询结果
     */
    updateResults(results) {
      this.results = results;
    },
    
    /**
     * 选中详情项
     */
    selectItem(item) {
      this.selectedItem = item;
    },
    
    /**
     * 更新列配置
     */
    updateColumnConfig(columns) {
      this.columnConfig[this.currentModule] = columns;
    },
    
    /**
     * 设置加载状态
     */
    setLoading(loading) {
      this.loading = loading;
    },
    
    /**
     * 设置错误信息
     */
    setError(error) {
      this.error = error;
    }
  },
  
  // 持久化配置（使用 pinia-plugin-persistedstate）
  persist: {
    key: 'advanced-search',
    paths: ['viewMode', 'columnConfig', 'queryMode']
  }
};
