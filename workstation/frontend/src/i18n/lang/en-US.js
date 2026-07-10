import el from "metersphere-frontend/src/i18n/lang/ele-en-US";
import fu from "fit2cloud-ui/src/locale/lang/en_US"; // 加载fit2cloud的内容
import mf from "metersphere-frontend/src/i18n/lang/en-US"
import advancedSearch from "./en-US-advanced-search";

const message = {
  sql_query: {
    menu: 'SQL Query',
    title: 'SQL Query Console',
    refresh: 'Refresh Connection',
    format: 'Format',
    clear: 'Clear',
    run: 'Run Query',
    limit: 'Rows',
    timeout_seconds: 'Timeout(s)',
    placeholder: 'Enter SQL. Multiple statements and session variables are supported. Ctrl / Cmd + Enter to run.',
    empty: 'Ready. Enter SQL and run it to view the last result set.',
    select_only: 'Runs with read-only account',
    max_rows: 'Max {count} rows',
    loading: 'Querying database...',
    failed: 'Query failed',
    success: 'Success',
    rows: '{count} rows',
    cost: '{time} ms',
    truncated: 'Truncated',
    export_csv: 'Export CSV',
    export_xlsx: 'Export XLSX',
    excel_name: 'Excel file name',
    excel_name_placeholder: 'Leave blank to use the default name',
    close: 'Close',
    page_info: '{start}-{end} of {total}',
    detail: 'Save',
    pool: 'Public Pool',
    upload_pool: 'Query Settings',
    pool_search_placeholder: 'Search title, summary, description, or SQL',
    pool_only_mine: 'Only mine',
    pool_upload_current: 'Upload Current SQL',
    pool_empty: 'No public SQL records',
    pool_detail_empty: 'Select a public SQL record to view details',
    pool_no_description: 'No description',
    pool_insert_console: 'Insert to Console',
    pool_copy_to_my: 'Copy to Mine',
    pool_edit: 'Edit',
    pool_offline: 'Delete',
    pool_upload_title: 'Upload to Public Pool',
    pool_edit_title: 'Edit Public SQL',
    pool_summary: 'Summary',
    pool_summary_placeholder: 'Optional, displayed on the public pool card',
    pool_summary_required: 'Summary is required',
    pool_sql_required: 'SQL is required',
    pool_use_count: 'Used {count} times',
    pool_load_failed: 'Failed to load public pool',
    pool_save_success: 'Saved successfully',
    pool_upload_success: 'Uploaded successfully',
    pool_save_failed: 'Failed to save public SQL',
    pool_insert_success: 'Inserted to console',
    pool_insert_failed: 'Failed to insert to console',
    pool_copy_success: 'Copied to my query history',
    pool_copy_failed: 'Failed to copy to mine',
    pool_offline_confirm: 'This SQL will no longer be visible in the public pool. It can be restored later by uploading the same title. Delete it?',
    pool_offline_success: 'Deleted successfully',
    pool_offline_failed: 'Failed to delete',
    pool_restore_title: 'Restore Deleted Record',
    pool_restore_confirm: 'A deleted public SQL named “{title}” already exists. Continuing will update and restore that record. Continue?',
    pool_overwrite_confirm: 'The console already has SQL. Inserting will create and switch to a new local draft. Continue?',
    history: 'Query History',
    new_history_title: 'New Window',
    clear_history: 'Clear',
    no_history: 'No history',
    saved: 'Saved',
    history_detail: 'SQL Details',
    sql_content: 'SQL Content',
    history_record_title: 'Title',
    history_record_title_placeholder: 'Enter the title shown on the history card',
    no_history_record_title: 'Untitled',
    title_required: 'Title is required',
    title_duplicate: 'Title already exists',
    description: 'Description',
    description_placeholder: 'Enter notes or purpose for this SQL',
    cancel: 'Cancel',
    insert: 'Insert',
    save: 'Save',
    save_success: 'Saved successfully',
    save_failed: 'Failed to save',
    delete: 'Delete',
    delete_success: 'Deleted successfully',
    delete_failed: 'Failed to delete',
    connected: 'Connected',
    disconnected: 'Disconnected',
    status_failed: 'Failed to load connection status'
  },
  advanced_search: {
    // Navigation and titles
    query_center: 'Query Center',
    advanced_search: 'Advanced Search',
    my_favorites: 'My Favorites',
    recent_views: 'Recent Views',
    shared_views: 'Shared Views',
    no_shared_views: 'No shared views',

    // Business modules
    test_case: 'Test Case',
    issue: 'Issue',
    test_plan: 'Test Plan',
    test_case_review: 'Test Case Review',

    // Filter conditions
    select_workspace: 'Select Workspace',
    select_project: 'Select Project',
    add_filter: 'Add Filter',
    search_field: 'Search Field',
    clear_all: 'Clear All',

    // Query mode
    visual_mode: 'Visual',
    jql_mode: 'JQL',
    jql_placeholder: 'Enter JQL query, e.g.: project = "E-commerce" AND status IN ("Pass", "Prepare")',
    syntax_help: 'Syntax Help',
    execute_query: 'Execute Query',
    fix_syntax_error: 'Please fix syntax errors first',

    // JQL help
    jql_help_title: 'JQL Syntax Help',
    jql_help_content: `
      <h4>Basic Syntax</h4>
      <p>project = "E-commerce" AND status IN ("Pass", "Prepare")</p>
      <h4>Supported Operators</h4>
      <ul>
        <li>= : Equals</li>
        <li>!= : Not equals</li>
        <li>~ : Like</li>
        <li>IN : In list</li>
        <li>NOT IN : Not in list</li>
        <li>&gt;, &gt;=, &lt;, &lt;= : Comparison operators</li>
        <li>AND, OR : Logical operators</li>
      </ul>
    `,

    // Cross-project tip
    cross_project_tip: 'Cross-project queries only support system fields. Please select a single project to use custom fields',

    // Field groups
    basic_info: 'Basic Info',
    module_specific: 'Module Specific',
    audit_trail: 'Audit Trail',
    custom_fields: 'Custom Fields',

    // Field names
    id: 'ID',
    name: 'Name',
    title: 'Title',
    status: 'Status',
    priority: 'Priority',
    creator: 'Creator',
    create_time: 'Create Time',
    update_time: 'Update Time',
    description: 'Description',

    // Result display
    total_results: 'Total {count} results',
    list_view: 'List View',
    split_view: 'Split View',
    column_config: 'Column Config',
    select_columns: 'Select Columns',
    reset_default: 'Reset Default',
    export: 'Export',
    select_item_to_view: 'Select an item to view details',

    // Action tips
    search: 'Search',
    query_failed: 'Query failed',
    export_success: 'Export success',
    export_failed: 'Export failed',
    load_fields_failed: 'Load fields failed',
    load_users_failed: 'Load users failed',
    load_workspaces_failed: 'Load workspaces failed',
    load_projects_failed: 'Load projects failed',

    // User selector
    select_users: 'Select Users',
    current_user: 'Me',
    enter_value: 'Enter value',
    select_value: 'Select value',
    to: 'to',
    start_date: 'Start Date',
    end_date: 'End Date',
    select_date: 'Select Date',
    select_module: 'Select Module'
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
