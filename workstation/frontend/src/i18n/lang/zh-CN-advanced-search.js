export default {
  advanced_search: {
    // ========== 模块 ==========
    select_module: '选择业务模块',
    module_test_case: '测试用例',
    module_issue: '缺陷',
    module_test_plan: '测试计划',
    module_review: '用例评审',
    module_test_case_review: '用例评审',

    // ========== 筛选 ==========
    select_workspace: '选择工作空间',
    select_project: '选择项目',
    please_select_workspace_first: '请先选择工作空间',
    project_list_updated: '项目列表已更新',
    select_filter_field: '选择筛选字段',
    workspace_placeholder: '全部工作空间',
    project_placeholder: '全部项目',
    cross_project_tip: '跨项目查询仅支持系统字段，如需使用自定义字段请选择单个项目',
    add_filter: '筛选条件',
    clear_all: '清空',
    removed_project_specific: '已移除 {count} 个项目特有字段的筛选条件',

    // ========== 字段分组 ==========
    basic_info: '基础信息',
    user_related: '人员相关',
    date_related: '时间相关',
    custom_fields: '自定义字段',
    audit_trail: '审计追踪',
    column_config: '列配置',
    basic_attributes: '基本属性',

    // ========== 查询模式 ==========
    mode_visual: '可视化',
    mode_jql: 'JQL',
    visual_mode_placeholder: '可视化查询模式开发中...',
    switch_to_jql: '切换到 JQL 模式',
    switch_to_visual: '切换到可视化模式',
    mode_switch_warning: '切换模式将清空当前查询条件，是否继续？',

    // ========== JQL 编辑器 ==========
    jql_placeholder: '输入 JQL 查询语句，例如：status = "Pass" AND priority = "P0"',
    jql_help_title: 'JQL 语法帮助',
    jql_operators: '支持的操作符',
    jql_examples: '查询示例',
    jql_format: '格式化',
    jql_clear: '清空',
    jql_copy: '复制',
    jql_history: '查询历史',
    jql_save: '保存查询',
    jql_saved: '已保存查询',
    jql_examples_title: '示例查询',

    // ========== 操作符说明 ==========
    op_equals: '等于',
    op_not_equals: '不等于',
    op_like: '模糊匹配',
    op_compare: '比较操作符',
    op_in: '是',
    op_not_in: '不是',
    op_contains: '包含',
    op_greater: '大于',
    op_greater_equal: '大于等于',
    op_less: '小于',
    op_less_equal: '小于等于',
    op_and: '逻辑与',
    op_or: '逻辑或',
    op_between: '范围',
    op_before: '早于',
    op_after: '晚于',

    // ========== 输入提示 ==========
    enter_value: '输入值',
    select_value: '选择值',
    select_users: '选择用户',
    current_user: '我自己',
    start_date: '开始',
    end_date: '结束',
    to: '至',

    // ========== 智能提示 ==========
    suggestion_field: '字段',
    suggestion_operator: '操作符',
    suggestion_value: '值',
    suggestion_keyword: '关键字',
    suggestion_recent: '最近使用',

    // ========== 视图 ==========
    view_list: '列表视图',
    view_split: '分屏视图',
    view_card: '卡片视图',
    select_row_to_view_detail: '点击左侧列表查看详情',
    no_data: '暂无数据',
    no_search_result: '未找到匹配的结果',
    try_adjust_filter: '请尝试调整筛选条件',

    // ========== 结果 ==========
    result_count: '共 {count} 条结果',
    total_results: '共检索到 {count} 条数据',
    result_count_selected: '已选择 {selected} / 共 {total} 条',
    loading: '加载中...',
    search: '查询',
    search_success: '查询成功',
    search_failed: '查询失败',
    updated_at: '更新于',

    // ========== 导出 ==========
    export: '导出',
    export_too_many: '结果数量超过 10000 条，请缩小查询范围后再导出',

    // ========== 分页 ==========
    page_size: '每页条数',
    go_to: '跳至',
    page: '页',

    // ========== 详情面板 ==========
    detail_title: '详情',
    detail_close: '关闭',
    detail_copy_id: '复制 ID',
    detail_open_in_new_tab: '在新标签页打开',
    detail_loading: '加载详情中...',

    // ========== 导出 ==========
    export_title: '导出数据',
    export_excel: '导出 Excel',
    export_csv: '导出 CSV',
    export_all: '导出全部',
    export_current_page: '导出当前页',
    export_selected: '导出已选中',
    export_success: '导出成功',
    export_failed: '导出失败',
    export_processing: '正在导出，请稍候...',
    export_limit_warning: '最多导出 {limit} 条数据',

    // ========== 保存查询 ==========
    save_query_title: '保存查询',
    save_query_name: '查询名称',
    save_query_name_placeholder: '请输入查询名称',
    save_query_share: '共享给团队',
    save_query_save: '保存',
    save_query_cancel: '取消',
    save_query_success: '保存成功',
    save_query_failed: '保存失败',
    save_query_name_required: '请输入查询名称',

    // ========== 查询历史 ==========
    query_history_title: '查询历史',
    query_history_clear: '清空历史',
    query_history_empty: '暂无查询历史',
    query_history_use: '使用',
    query_history_delete: '删除',

    // ========== 字段信息 ==========
    field_name: '字段名',
    field_label: '显示名称',
    field_type: '字段类型',
    field_type_text: '文本',
    field_type_select: '下拉选择',
    field_type_user: '用户选择',
    field_type_date: '日期',
    field_type_datetime: '日期时间',
    field_type_number: '数字',
    field_type_multiselect: '多选',
    field_type_tree: '树形选择',

    // ========== 验证错误 ==========
    validation_error: '语法错误',
    validation_success: '语法正确',
    error_expected_field: '期望字段名',
    error_expected_operator: '期望操作符',
    error_expected_value: '期望值',
    error_expected_lparen: '期望左括号',
    error_expected_rparen: '期望右括号',
    error_unknown_field: '未知字段',
    error_unknown_operator: '未知操作符',
    error_invalid_value: '无效的值',
    error_field_not_support_operator: '该字段不支持此操作符',
    error_position: '位置: 第 {line} 行, 第 {column} 列',

    // ========== 快捷键 ==========
    shortcut_execute: '执行查询',
    shortcut_focus_editor: '聚焦编辑器',
    shortcut_clear: '清空查询',
    shortcut_toggle_help: '切换帮助面板',

    // ========== 工具栏 ==========
    refresh: '刷新',
    column_settings: '列设置',
    display_columns: '显示列',
    reset_columns: '重置列',

    // ========== 加载/错误消息 ==========
    load_workspaces_failed: '加载工作空间失败',
    load_projects_failed: '加载项目失败',
    load_fields_failed: '加载字段失败',
    load_history_failed: '加载查询历史失败',
    load_saved_failed: '加载已保存查询失败',
    load_detail_failed: '加载详情失败',

    // ========== 示例查询 ==========
    example_status_priority: '查找优先级为 P0 且状态为通过的用例',
    example_date_range: '查找最近创建的用例',
    example_in_list: '查找状态在指定列表中的缺陷',
    example_complex: '复杂组合查询示例',
    example_contains: '查找名称包含特定关键词的数据',

    // ========== JQL 示例文本 ==========
    jql_example_1: 'status = "Pass" AND priority = "P0"',
    jql_example_2: 'status IN ("Pass", "Prepare") AND priority NOT IN ("P3", "P4")',
    jql_example_3: 'name ~ "登录" AND createTime >= "2024-01-01"',
    jql_example_4: '(priority = "P0" OR priority = "P1") AND status != "Deprecated"',
    jql_example_5: 'createUser = "admin" AND updateTime > "2024-01-01" AND updateTime < "2024-12-31"',
    jql_example_6: 'name CONTAINS "测试" AND status IN ("Underway", "Completed")',

    // ========== 确认对话框 ==========
    confirm_title: '确认',
    confirm_clear_query: '确定要清空当前查询条件吗？',
    confirm_clear_history: '确定要清空查询历史吗？',
    confirm_delete_saved: '确定要删除此保存的查询吗？',
    confirm_yes: '确定',
    confirm_no: '取消',

    // ========== 其他 ==========
    all: '全部',
    selected: '已选择',
    clear: '清空',
    apply: '应用',
    cancel: '取消',
    save: '保存',
    delete: '删除',
    edit: '编辑',
    copy: '复制',
    copy_success: '复制成功',
    copy_failed: '复制失败',
    more: '更多',
    expand: '展开',
    collapse: '收起',

    // ========== 模块专属字段 ==========
    priority: '优先级',
    maintainer: '维护人',
    case_type: '用例类型',
    review_status: '评审状态',
    severity: '严重程度',
    assignee: '指派给',
    platform: '缺陷平台',
    principal: '负责人',
    stage: '计划阶段',
    reviewer: '评审人'
  }
};
