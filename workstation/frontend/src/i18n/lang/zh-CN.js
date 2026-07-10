import el from "metersphere-frontend/src/i18n/lang/ele-zh-CN"; // 加载element的内容
import fu from "fit2cloud-ui/src/locale/lang/zh-CN"; // 加载fit2cloud的内容
import mf from "metersphere-frontend/src/i18n/lang/zh-CN"
import advancedSearch from "./zh-CN-advanced-search";

const message = {
  sql_query: {
    menu: 'SQL 查询台',
    title: 'SQL 查询台',
    refresh: '刷新连接',
    format: '格式化',
    clear: '清空',
    run: '运行查询',
    limit: '行数',
    timeout_seconds: '超时(s)',
    placeholder: '请输入 SQL，支持多语句和会话变量，Ctrl / Cmd + Enter 快捷运行',
    empty: '准备就绪。输入 SQL 后运行查看结果，页面展示最后一个结果集。',
    select_only: '使用只读账号执行',
    max_rows: '最多 {count} 行',
    loading: '正在查询数据库...',
    failed: '查询执行失败',
    success: '成功',
    rows: '{count} 行',
    cost: '耗时 {time} ms',
    truncated: '结果已截断',
    export_csv: '导出 CSV',
    export_xlsx: '导出 XLSX',
    excel_name: 'Excel 文件名',
    excel_name_placeholder: '不填则使用默认文件名',
    close: '关闭结果',
    page_info: '第 {start}-{end} 条，共 {total} 条',
    detail: '保存',
    pool: '公共池',
    upload_pool: '查询设置',
    pool_search_placeholder: '搜索标题、简介、描述或 SQL',
    pool_only_mine: '只看我上传',
    pool_upload_current: '上传当前 SQL',
    pool_empty: '公共池暂无记录',
    pool_detail_empty: '请选择一条公共 SQL 查看详情',
    pool_no_description: '暂无备注',
    pool_insert_console: '插入到控制台',
    pool_copy_to_my: '复制到我的',
    pool_edit: '编辑',
    pool_offline: '删除',
    pool_upload_title: '上传到公共池',
    pool_edit_title: '编辑公共 SQL',
    pool_summary: '简介',
    pool_summary_placeholder: '选填，用于公共池卡片展示',
    pool_summary_required: '简介不能为空',
    pool_sql_required: 'SQL 不能为空',
    pool_use_count: '使用 {count} 次',
    pool_load_failed: '公共池加载失败',
    pool_save_success: '保存成功',
    pool_upload_success: '上传成功',
    pool_save_failed: '公共池保存失败',
    pool_insert_success: '已插入控制台',
    pool_insert_failed: '插入控制台失败',
    pool_copy_success: '已复制到我的查询历史',
    pool_copy_failed: '复制到我的失败',
    pool_offline_confirm: '删除后其他人将无法在公共池看到这条 SQL，后续可通过同名上传恢复，确认删除吗？',
    pool_offline_success: '删除成功',
    pool_offline_failed: '删除失败',
    pool_restore_title: '恢复已删除记录',
    pool_restore_confirm: '公共池中存在已删除的同名标题“{title}”。继续上传将更新并恢复原记录，是否继续？',
    pool_overwrite_confirm: '当前控制台已有 SQL，插入后会生成新的本地草稿并切换过去，是否继续？',
    history: '查询历史',
    new_history_title: '新增窗口',
    clear_history: '清空历史',
    no_history: '暂无记录',
    saved: '已保存',
    history_detail: 'SQL 详情',
    sql_content: 'SQL 内容',
    history_record_title: '标题',
    history_record_title_placeholder: '请输入右侧卡片显示的标题',
    no_history_record_title: '未填写标题',
    title_required: '标题不能为空',
    title_duplicate: '标题不能重复',
    description: '备注',
    description_placeholder: '请输入 SQL 备注或用途说明',
    cancel: '取消',
    insert: '插入',
    save: '保存',
    save_success: '保存成功',
    save_failed: '保存失败',
    delete: '删除',
    delete_success: '删除成功',
    delete_failed: '删除失败',
    connected: '已连接',
    disconnected: '未连接',
    status_failed: '连接状态获取失败'
  },
  advanced_search: {
    // 导航和标题
    query_center: '查询中心',
    advanced_search: '高级检索',
    my_favorites: '我的收藏',
    recent_views: '最近浏览',
    shared_views: '共享视图',
    no_shared_views: '暂无共享视图',

    // 业务模块
    test_case: '测试用例',
    issue: '缺陷',
    test_plan: '测试计划',
    test_case_review: '用例评审',

    // 筛选条件
    select_workspace: '选择工作空间',
    select_project: '选择项目',
    add_filter: '添加筛选条件',
    search_field: '搜索字段',
    clear_all: '清空',

    // 查询模式
    visual_mode: '可视化',
    jql_mode: 'JQL',
    jql_placeholder: '输入 JQL 查询，例如：project = "电商平台" AND status IN ("Pass", "Prepare")',
    syntax_help: '语法帮助',
    execute_query: '执行查询',
    fix_syntax_error: '请先修复语法错误',

    // JQL 帮助
    jql_help_title: 'JQL 语法帮助',
    jql_help_content: `
      <h4>基础语法</h4>
      <p>project = "电商平台" AND status IN ("Pass", "Prepare")</p>
      <h4>支持的操作符</h4>
      <ul>
        <li>= : 等于</li>
        <li>!= : 不等于</li>
        <li>~ : 模糊匹配</li>
        <li>IN : 包含于列表</li>
        <li>NOT IN : 不包含于列表</li>
        <li>&gt;, &gt;=, &lt;, &lt;= : 比较操作符</li>
        <li>AND, OR : 逻辑操作符</li>
      </ul>
    `,

    // 跨项目提示
    cross_project_tip: '跨项目查询仅支持系统字段筛选，如需使用自定义字段请选择单个项目',

    // 字段分组
    basic_info: '基础信息',
    module_specific: '模块专属',
    audit_trail: '审计追踪',
    custom_fields: '自定义字段',

    // 字段名称
    id: 'ID',
    name: '名称',
    title: '标题',
    status: '状态',
    priority: '优先级',
    creator: '创建人',
    create_time: '创建时间',
    update_time: '更新时间',
    description: '描述',

    // 结果展示
    total_results: '共 {count} 条结果',
    list_view: '列表视图',
    split_view: '分屏视图',
    column_config: '列配置',
    select_columns: '选择显示列',
    reset_default: '恢复默认',
    export: '导出',
    select_item_to_view: '选择一条记录查看详情',

    // 操作提示
    search: '查询',
    query_failed: '查询失败',
    export_success: '导出成功',
    export_failed: '导出失败',
    load_fields_failed: '加载字段失败',
    load_users_failed: '加载用户列表失败',
    load_workspaces_failed: '加载工作空间列表失败',
    load_projects_failed: '加载项目列表失败',

    // 用户选择器
    select_users: '选择用户',
    current_user: '我自己',
    enter_value: '请输入',
    select_value: '请选择',
    to: '至',
    start_date: '开始日期',
    end_date: '结束日期',
    select_date: '选择日期',
    select_module: '选择模块'
  }
}

// 合并 advanced-search 翻译
if (advancedSearch && advancedSearch.advanced_search) {
  message.advanced_search = { ...message.advanced_search, ...advancedSearch.advanced_search };
}

export default {
  ...el,
  ...fu,
  ...mf,
  ...message
};
