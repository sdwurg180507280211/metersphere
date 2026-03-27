import el from "metersphere-frontend/src/i18n/lang/ele-en-US";
import fu from "fit2cloud-ui/src/locale/lang/en_US"; // 加载fit2cloud的内容
import mf from "metersphere-frontend/src/i18n/lang/en-US"

const message = {
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

export default {
  ...el,
  ...fu,
  ...mf,
  ...message
};

