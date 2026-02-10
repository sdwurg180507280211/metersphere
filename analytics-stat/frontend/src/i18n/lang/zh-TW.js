/**
 * 中文繁体语言包
 */
import el from "metersphere-frontend/src/i18n/lang/ele-zh-TW";
import fu from "fit2cloud-ui/src/locale/lang/zh-TW";
import mf from "metersphere-frontend/src/i18n/lang/zh-TW";

const message = {
  analytics: {
    title: "分析統計",
    menu: {
      home: "工作台",
      sql_console: "SQL查詢台",
      data_dictionary: "數據字典"
    },
    home: "工作台",
    dashboard: "數據概覽",
    sql_console: "SQL查詢台",
    data_dictionary: "數據字典",
    execute: "執行",
    clear: "清空",
    export: "導出",
    table_name: "表名",
    column_name: "欄位名",
    data_type: "數據類型",
    comment: "註釋",
    query_result: "查詢結果",
    no_data: "暫無數據",
    query_success: "查詢成功",
    query_failed: "查詢失敗",
    rows_affected: "影響行數",
    execution_time: "執行時間"
  },
  commons: {
    analytics_stat: "分析統計"
  }
};

export default {
  ...el,
  ...fu,
  ...mf,
  ...message
};
