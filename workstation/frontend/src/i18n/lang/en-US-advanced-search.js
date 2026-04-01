export default {
  advanced_search: {
    // ========== Module ==========
    select_module: 'Select Module',
    module_test_case: 'Test Case',
    module_issue: 'Issue',
    module_test_plan: 'Test Plan',
    module_review: 'Case Review',
    module_test_case_review: 'Case Review',

    // ========== Filter ==========
    select_workspace: 'Select Workspace',
    select_project: 'Select Project',
    select_filter_field: 'Select Filter Field',
    workspace_placeholder: 'All Workspaces',
    project_placeholder: 'All Projects',
    cross_project_tip: 'Cross-project queries only support system fields. Select a single project to use custom fields',
    add_filter: 'Filter',
    clear_all: 'Clear',
    removed_project_specific: 'Removed {count} project-specific filter conditions',

    // ========== Field Groups ==========
    basic_info: 'Basic Info',
    user_related: 'User Related',
    date_related: 'Date Related',
    custom_fields: 'Custom Fields',
    audit_trail: 'Audit Trail',
    column_config: 'Column Config',
    basic_attributes: 'Basic Attributes',

    // ========== Query Mode ==========
    mode_visual: 'Visual',
    mode_jql: 'JQL',
    visual_mode_placeholder: 'Visual query mode is under development...',
    switch_to_jql: 'Switch to JQL Mode',
    switch_to_visual: 'Switch to Visual Mode',
    mode_switch_warning: 'Switching mode will clear current query conditions. Continue?',

    // ========== JQL Editor ==========
    jql_placeholder: 'Enter JQL query, e.g.: status = "Pass" AND priority = "P0"',
    jql_help_title: 'JQL Syntax Help',
    jql_operators: 'Supported Operators',
    jql_examples: 'Query Examples',
    jql_format: 'Format',
    jql_clear: 'Clear',
    jql_copy: 'Copy',
    jql_history: 'History',
    jql_save: 'Save Query',
    jql_saved: 'Saved Queries',
    jql_examples_title: 'Example Queries',

    // ========== Operator Descriptions ==========
    op_equals: 'Equals',
    op_not_equals: 'Not Equals',
    op_like: 'Like',
    op_compare: 'Comparison Operators',
    op_in: 'Is',
    op_not_in: 'Is Not',
    op_contains: 'Contains',
    op_greater: 'Greater Than',
    op_greater_equal: 'Greater Than or Equal',
    op_less: 'Less Than',
    op_less_equal: 'Less Than or Equal',
    op_and: 'Logical AND',
    op_or: 'Logical OR',
    op_between: 'Between',
    op_before: 'Before',
    op_after: 'After',

    // ========== Input Placeholders ==========
    enter_value: 'Enter value',
    select_value: 'Select value',
    select_users: 'Select users',
    current_user: 'Me',
    start_date: 'Start',
    end_date: 'End',
    to: 'to',

    // ========== Suggestions ==========
    suggestion_field: 'Field',
    suggestion_operator: 'Operator',
    suggestion_value: 'Value',
    suggestion_keyword: 'Keyword',
    suggestion_recent: 'Recent',

    // ========== View ==========
    view_list: 'List View',
    view_split: 'Split View',
    view_card: 'Card View',
    select_row_to_view_detail: 'Click row to view detail',
    no_data: 'No Data',
    no_search_result: 'No matching results found',
    try_adjust_filter: 'Try adjusting your filter criteria',

    // ========== Results ==========
    result_count: 'Total {count} results',
    total_results: 'Found {count} items',
    result_count_selected: '{selected} selected / {total} total',
    loading: 'Loading...',
    search: 'Search',
    search_success: 'Search completed',
    search_failed: 'Search failed',
    updated_at: 'Updated at',

    // ========== Export ==========
    export: 'Export',
    export_too_many: 'More than 10,000 results. Please narrow your search before exporting.',

    // ========== Pagination ==========
    page_size: 'Items per page',
    go_to: 'Go to',
    page: 'Page',

    // ========== Detail Panel ==========
    detail_title: 'Detail',
    detail_close: 'Close',
    detail_copy_id: 'Copy ID',
    detail_open_in_new_tab: 'Open in new tab',
    detail_loading: 'Loading detail...',

    // ========== Export ==========
    export_title: 'Export Data',
    export_excel: 'Export Excel',
    export_csv: 'Export CSV',
    export_all: 'Export All',
    export_current_page: 'Export Current Page',
    export_selected: 'Export Selected',
    export_success: 'Export successful',
    export_failed: 'Export failed',
    export_processing: 'Exporting, please wait...',
    export_limit_warning: 'Maximum {limit} records can be exported',

    // ========== Save Query ==========
    save_query_title: 'Save Query',
    save_query_name: 'Query Name',
    save_query_name_placeholder: 'Enter query name',
    save_query_share: 'Share with team',
    save_query_save: 'Save',
    save_query_cancel: 'Cancel',
    save_query_success: 'Saved successfully',
    save_query_failed: 'Save failed',
    save_query_name_required: 'Please enter query name',

    // ========== Query History ==========
    query_history_title: 'Query History',
    query_history_clear: 'Clear History',
    query_history_empty: 'No query history',
    query_history_use: 'Use',
    query_history_delete: 'Delete',

    // ========== Field Info ==========
    field_name: 'Field Name',
    field_label: 'Display Name',
    field_type: 'Field Type',
    field_type_text: 'Text',
    field_type_select: 'Select',
    field_type_user: 'User Select',
    field_type_date: 'Date',
    field_type_datetime: 'DateTime',
    field_type_number: 'Number',
    field_type_multiselect: 'Multi-select',
    field_type_tree: 'Tree Select',

    // ========== Validation Errors ==========
    validation_error: 'Syntax Error',
    validation_success: 'Syntax Valid',
    error_expected_field: 'Expected field name',
    error_expected_operator: 'Expected operator',
    error_expected_value: 'Expected value',
    error_expected_lparen: 'Expected left parenthesis',
    error_expected_rparen: 'Expected right parenthesis',
    error_unknown_field: 'Unknown field',
    error_unknown_operator: 'Unknown operator',
    error_invalid_value: 'Invalid value',
    error_field_not_support_operator: 'This field does not support this operator',
    error_position: 'Position: Line {line}, Column {column}',

    // ========== Keyboard Shortcuts ==========
    shortcut_execute: 'Execute query',
    shortcut_focus_editor: 'Focus editor',
    shortcut_clear: 'Clear query',
    shortcut_toggle_help: 'Toggle help panel',

    // ========== Toolbar ==========
    refresh: 'Refresh',
    column_settings: 'Column Settings',
    display_columns: 'Display Columns',
    reset_columns: 'Reset Columns',

    // ========== Load/Error Messages ==========
    load_workspaces_failed: 'Failed to load workspaces',
    load_projects_failed: 'Failed to load projects',
    load_fields_failed: 'Failed to load fields',
    load_history_failed: 'Failed to load query history',
    load_saved_failed: 'Failed to load saved queries',
    load_detail_failed: 'Failed to load detail',

    // ========== Example Queries ==========
    example_status_priority: 'Find P0 priority cases that are Passed',
    example_date_range: 'Find recently created cases',
    example_in_list: 'Find issues with status in list',
    example_complex: 'Complex query example',
    example_contains: 'Find data with name containing keyword',

    // ========== JQL Example Texts ==========
    jql_example_1: 'status = "Pass" AND priority = "P0"',
    jql_example_2: 'status IN ("Pass", "Prepare") AND priority NOT IN ("P3", "P4")',
    jql_example_3: 'name ~ "login" AND createTime >= "2024-01-01"',
    jql_example_4: '(priority = "P0" OR priority = "P1") AND status != "Deprecated"',
    jql_example_5: 'createUser = "admin" AND updateTime > "2024-01-01" AND updateTime < "2024-12-31"',
    jql_example_6: 'name CONTAINS "test" AND status IN ("Underway", "Completed")',

    // ========== Confirm Dialogs ==========
    confirm_title: 'Confirm',
    confirm_clear_query: 'Are you sure you want to clear the current query?',
    confirm_clear_history: 'Are you sure you want to clear query history?',
    confirm_delete_saved: 'Are you sure you want to delete this saved query?',
    confirm_yes: 'Yes',
    confirm_no: 'No',

    // ========== Misc ==========
    all: 'All',
    selected: 'Selected',
    clear: 'Clear',
    apply: 'Apply',
    cancel: 'Cancel',
    save: 'Save',
    delete: 'Delete',
    edit: 'Edit',
    copy: 'Copy',
    copy_success: 'Copied successfully',
    copy_failed: 'Copy failed',
    more: 'More',
    expand: 'Expand',
    collapse: 'Collapse',

    // ========== Module Specific Fields ==========
    priority: 'Priority',
    maintainer: 'Maintainer',
    case_type: 'Case Type',
    review_status: 'Review Status',
    severity: 'Severity',
    assignee: 'Assignee',
    platform: 'Platform',
    principal: 'Principal',
    stage: 'Stage',
    reviewer: 'Reviewer'
  }
};
