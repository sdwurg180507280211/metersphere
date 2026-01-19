# 任务清单

> 原型预览：`docs/功能开发/SQL查询台/metersphere-search-redesign-version3.html`

## 1. 后端基础架构

### 1.1 数据模型和 DTO
- [ ] 1.1.1 创建 `AdvancedSearchRequest.java`（继承 BaseQueryRequest，添加 module/workspaceIds/projectIds/useJQL/jql）
- [ ] 1.1.2 创建 `FieldMetadata.java`（字段元数据：field/label/type/operators/options/projectSpecific）
- [ ] 1.1.3 创建 `AdvancedSearchResult.java`（查询结果包装类）
- [ ] 1.1.4 创建 `UserSimpleDTO.java`（用户简要信息：id/name/email/avatar）
- [ ] 1.1.5 创建 `JQLValidationResult.java`（JQL验证结果：valid/message/suggestions）
- [ ] 1.1.6 创建 `JQLSuggestion.java`（JQL智能提示：type/value/description/insertText）

### 1.2 Mapper 层
- [ ] 1.2.1 创建 `ExtAdvancedSearchMapper.java` 扩展接口
- [ ] 1.2.2 创建 `ExtAdvancedSearchMapper.xml` 动态 SQL
  - 测试用例查询（关联 test_case 表）
  - 缺陷查询（关联 issues 表）
  - 测试计划查询（关联 test_plan 表）
  - 用例评审查询（关联 test_case_review 表）
  - 支持 combine 条件动态拼接
  - 支持用户字段 IN 查询

### 1.3 Service 层
- [ ] 1.3.1 创建 `AdvancedSearchService.java`
  - query() 分页查询方法（支持JQL和combine两种模式）
  - getDetail() 获取详情方法
  - 集成 PageHelper 分页
  - 权限校验逻辑
- [ ] 1.3.2 创建 `FieldMetadataService.java`
  - getSystemFields() 获取系统字段
  - getCustomFields() 根据 projectId 获取自定义字段
  - getGlobalCustomFields() 获取全局自定义字段
- [ ] 1.3.3 创建 `JQLParser.java`
  - parseJQL() JQL语法解析为AST
  - validateJQL() JQL语法验证
  - tokenize() 词法分析
- [ ] 1.3.4 创建 `JQLToSQLConverter.java`
  - convertToSQL() AST转换为SQL WHERE子句
  - buildConditionClause() 构建条件子句
  - mapFieldToColumn() 字段名映射
- [ ] 1.3.5 创建 `JQLCacheService.java`
  - cacheAST() 缓存解析后的AST
  - getCachedAST() 获取缓存的AST
  - generateCacheKey() 生成缓存键

### 1.4 Controller 层
- [ ] 1.4.1 创建 `AdvancedSearchController.java`
  - POST /workstation/advanced-search/query/{goPage}/{pageSize}
  - GET /workstation/advanced-search/fields/{module}
  - GET /workstation/advanced-search/users
  - GET /workstation/advanced-search/detail/{module}/{id}
  - POST /workstation/advanced-search/jql/validate
  - POST /workstation/advanced-search/jql/suggestions

## 2. 前端基础架构

### 2.1 API 接口
- [ ] 2.1.1 创建 `workstation/frontend/src/api/advanced-search.js`
  - queryData(request) 查询接口（支持JQL和combine模式）
  - getFieldMetadata(module, projectId) 字段元数据接口
  - getUsers(workspaceIds, keyword) 用户列表接口
  - getDetail(module, id) 详情接口
  - exportExcel(request) 导出接口
  - validateJQL(jql, module) JQL语法验证接口
  - getJQLSuggestions(context, module, cursorPosition) JQL智能提示接口

### 2.2 Pinia Store
- [ ] 2.2.1 创建 `workstation/frontend/src/store/modules/advancedSearch.js`
  - state: currentModule/selectedProjects/queryMode/jqlQuery/combine/results/viewMode/fieldMetadata
  - actions: executeQuery/loadFieldMetadata/onProjectChange/switchModule/switchQueryMode/validateJQL/getJQLSuggestions
  - getters: isSingleProjectMode/availableFields
  - persist: viewMode/columnConfig/queryMode

### 2.3 路由配置
- [ ] 2.3.1 修改 `workstation/frontend/src/router/modules/workstation.js`
  - 添加 /workstation/advanced-search 路由

## 3. 前端核心组件

### 3.1 主页面
- [ ] 3.1.1 创建 `AdvancedSearch.vue` 主页面（整体布局）

### 3.2 侧边栏
- [ ] 3.2.1 创建 `Sidebar.vue`（查询中心/共享视图导航）

### 3.3 顶部筛选栏
- [ ] 3.3.1 创建 `TopFilterBar.vue`
  - 业务模块选择器
  - 工作空间多选
  - 项目多选（级联过滤）
  - 查询模式切换（可视化/JQL）
  - 筛选条件按钮
  - 跨项目模式提示

### 3.4 筛选条件组件
- [ ] 3.4.1 创建 `FilterPopover.vue`（字段选择弹窗，分组展示）
- [ ] 3.4.2 创建 `ActiveTagsBar.vue`（已选条件标签栏）
- [ ] 3.4.3 创建 `ConditionInput.vue`（条件输入组件，根据类型渲染）

### 3.5 JQL查询编辑器
- [ ] 3.5.1 创建 `JQLEditor.vue`
  - JQL输入框（支持多行）
  - 语法高亮显示
  - 实时语法验证
  - 错误提示显示
- [ ] 3.5.2 创建 `JQLAutoComplete.vue`
  - 智能提示下拉框
  - 字段名提示
  - 操作符提示
  - 值选项提示
- [ ] 3.5.3 创建 `JQLHelper.vue`
  - 语法帮助面板
  - 操作符说明
  - 示例查询
- [ ] 3.5.4 创建 `QueryModeSwitch.vue`
  - 可视化/JQL模式切换
  - 模式转换提示

### 3.6 用户选择器
- [ ] 3.6.1 创建 `UserSelector.vue`
  - 支持搜索（用户名/姓名）
  - 显示头像和名称
  - 多选（最多 10 个）
  - "我自己"快捷选项

### 3.7 结果展示组件
- [ ] 3.7.1 创建 `ResultToolbar.vue`（视图切换/列配置/导出）
- [ ] 3.7.2 创建 `ResultView.vue`（结果展示区容器）
- [ ] 3.7.3 创建 `ListView.vue`（列表视图，el-table）
- [ ] 3.7.4 创建 `SplitView.vue`（分屏详情视图）
- [ ] 3.7.5 创建 `DetailPanel.vue`（详情面板）

### 3.8 列配置
- [ ] 3.8.1 创建 `ColumnConfig.vue`（列配置弹窗）

## 4. 国际化

### 4.1 中文词条
- [ ] 4.1.1 修改 `workstation/frontend/src/i18n/lang/zh-CN.js`

### 4.2 英文词条
- [ ] 4.2.1 修改 `workstation/frontend/src/i18n/lang/en-US.js`

## 5. 功能集成

### 5.1 跨项目字段处理
- [ ] 5.1.1 实现项目选择变更时的字段更新逻辑
- [ ] 5.1.2 实现筛选条件的自动清理（切换到跨项目模式时）

### 5.2 用户维度筛选
- [ ] 5.2.1 实现用户字段的 IN 查询（后端 SQL）
- [ ] 5.2.2 实现"我自己"快捷选项（自动填充当前用户 ID）

### 5.3 JQL功能集成
- [ ] 5.3.1 实现JQL与可视化模式的双向转换
- [ ] 5.3.2 实现JQL语法高亮和错误提示
- [ ] 5.3.3 实现JQL智能提示和自动补全
- [ ] 5.3.4 实现JQL解析结果缓存机制
- [ ] 5.3.5 实现JQL查询历史记录

### 5.4 权限控制
- [ ] 5.4.1 添加工作空间/项目访问权限校验
- [ ] 5.4.2 添加模块功能权限校验
- [ ] 5.4.3 添加JQL字段名白名单验证

### 5.5 数据导出
- [ ] 5.5.1 实现 Excel 导出功能（EasyExcel）
- [ ] 5.5.2 超过 10000 条提示用户缩小范围

## 6. 测试

### 6.1 后端测试
- [ ] 6.1.1 Service 层单元测试
- [ ] 6.1.2 Controller 层接口测试
- [ ] 6.1.3 JQL解析器单元测试
- [ ] 6.1.4 JQL到SQL转换器测试

### 6.2 集成测试
- [ ] 6.2.1 跨工作空间/项目查询测试
- [ ] 6.2.2 用户维度筛选测试
- [ ] 6.2.3 权限控制测试
- [ ] 6.2.4 JQL查询功能测试
- [ ] 6.2.5 JQL语法验证测试
