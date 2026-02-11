/**
 * English language pack
 */
import el from "metersphere-frontend/src/i18n/lang/ele-en-US";
import fu from "fit2cloud-ui/src/locale/lang/en_US";
import mf from "metersphere-frontend/src/i18n/lang/en-US";

const message = {
  analytics: {
    title: "Analytics",
    menu: {
      home: "Home",
      sql_console: "SQL Console",
      data_dictionary: "Data Dictionary"
    },
    home: "Home",
    dashboard: "Dashboard",
    sql_console: "SQL Console",
    data_dictionary: "Data Dictionary",
    execute: "Execute",
    clear: "Clear",
    export: "Export",
    table_name: "Table Name",
    column_name: "Column Name",
    data_type: "Data Type",
    comment: "Comment",
    query_result: "Query Result",
    no_data: "No Data",
    query_success: "Query Success",
    query_failed: "Query Failed",
    rows_affected: "Rows Affected",
    execution_time: "Execution Time",
    // Home page
    query_count_stat: "Query Count",
    total_query_count: "Total Queries",
    data_volume_stat: "Data Volume",
    total_data_volume: "Total Data Volume",
    quick_access: "Quick Access",
    recent_queries: "Recent Queries",
    query_name: "Query Name",
    type: "Type",
    create_time: "Created At",
    status: "Status",
    success: "Success",
    failed: "Failed",
    comprehensive_query: "Comprehensive Query",
    no_query_records: "No query records",
    load_query_count_failed: "Failed to load query count",
    load_data_volume_failed: "Failed to load data volume",
    load_recent_queries_failed: "Failed to load recent queries",
    // Quick access descriptions
    sql_console_desc: "Execute custom SQL queries",
    data_dictionary_desc: "View system data dictionary",
    comprehensive_query_desc: "Multi-dimensional data query",
    // Data Dictionary page
    add_dictionary: "Add Dictionary",
    dict_type: "Dict Type",
    dict_code: "Dict Code",
    dict_label: "Dict Label",
    dict_value: "Dict Value",
    sort_order: "Sort",
    enabled: "Enabled",
    disabled: "Disabled",
    edit: "Edit",
    delete: "Delete",
    feature_in_development: "Feature in development...",
    // SQL Console page
    execute_query: "Execute Query",
    sql_placeholder: "Enter SQL query...",
    sql_empty_warning: "Please enter a SQL query",
    no_query_result: "No query results",
    // Mock data (temporary, remove after API integration)
    mock_query_user_stat: "User Statistics",
    mock_query_project_data: "Project Data Query"
  }
};

export default {
  ...el,
  ...fu,
  ...mf,
  ...message
};
