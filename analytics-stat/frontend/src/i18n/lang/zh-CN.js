/**
 * 中文简体语言包
 * 
 * 集成说明：
 * - el: Element UI 组件库的中文翻译
 * - fu: fit2cloud-ui 组件库的中文翻译
 * - mf: metersphere-frontend 公共翻译
 * - message: 本模块特有的翻译
 */
import el from "metersphere-frontend/src/i18n/lang/ele-zh-CN";
import fu from "fit2cloud-ui/src/locale/lang/zh-CN";
import mf from "metersphere-frontend/src/i18n/lang/zh-CN";

// 本模块特有的翻译
const message = {
  analytics: {
    home: "工作台",
    dashboard: "数据概览",
    sql_console: "SQL查询台",
    data_dictionary: "数据字典",
    execute: "执行",
    clear: "清空",
    export: "导出",
    table_name: "表名",
    column_name: "字段名",
    data_type: "数据类型",
    comment: "注释",
    query_result: "查询结果",
    no_data: "暂无数据",
    query_success: "查询成功",
    query_failed: "查询失败",
    rows_affected: "影响行数",
    execution_time: "执行时间"
  },
  commons: {
    analytics_stat: "分析统计"
  }
};

export default {
  ...el,
  ...fu,
  ...mf,
  ...message
};
