/**
 * English language pack
 */
import el from "metersphere-frontend/src/i18n/lang/ele-en-US";
import fu from "fit2cloud-ui/src/locale/lang/en_US";  // 注意：fit2cloud-ui 使用下划线
import mf from "metersphere-frontend/src/i18n/lang/en-US";

const message = {
  analytics: {
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
    execution_time: "Execution Time"
  }
};

export default {
  ...el,
  ...fu,
  ...mf,
  ...message
};
