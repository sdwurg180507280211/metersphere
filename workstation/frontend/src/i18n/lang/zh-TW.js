import el from "metersphere-frontend/src/i18n/lang/ele-zh-TW";
import fu from "fit2cloud-ui/src/locale/lang/zh-TW";
import mf from "metersphere-frontend/src/i18n/lang/zh-TW"
import advancedSearch from "./zh-TW-advanced-search";

const message = {
  sql_query: {
    menu: 'SQL 查詢台',
    title: 'SQL 查詢台',
    refresh: '重新整理連線',
    format: '格式化',
    clear: '清空',
    run: '執行查詢',
    limit: '行數',
    timeout_seconds: '逾時(s)',
    placeholder: '請輸入 SQL，支援多語句和工作階段變數，Ctrl / Cmd + Enter 快捷執行',
    empty: '準備就緒。輸入 SQL 後執行查看結果，頁面顯示最後一個結果集。',
    select_only: '使用唯讀帳號執行',
    max_rows: '最多 {count} 行',
    loading: '正在查詢資料庫...',
    failed: '查詢執行失敗',
    success: '成功',
    rows: '{count} 行',
    cost: '耗時 {time} ms',
    truncated: '結果已截斷',
    export_csv: '匯出 CSV',
    export_xlsx: '匯出 XLSX',
    excel_name: 'Excel 檔案名稱',
    excel_name_placeholder: '不填則使用預設檔名',
    close: '關閉結果',
    page_info: '第 {start}-{end} 條，共 {total} 條',
    detail: '儲存',
    pool: '公共池',
    upload_pool: '查詢設定',
    pool_search_placeholder: '搜尋標題、簡介、描述或 SQL',
    pool_only_mine: '只看我上傳',
    pool_upload_current: '上傳目前 SQL',
    pool_empty: '公共池暫無記錄',
    pool_detail_empty: '請選擇一條公共 SQL 查看詳情',
    pool_no_description: '暫無備註',
    pool_insert_console: '插入到控制台',
    pool_copy_to_my: '複製到我的',
    pool_edit: '編輯',
    pool_offline: '刪除',
    pool_upload_title: '上傳到公共池',
    pool_edit_title: '編輯公共 SQL',
    pool_summary: '簡介',
    pool_summary_placeholder: '選填，用於公共池卡片顯示',
    pool_summary_required: '簡介不能為空',
    pool_sql_required: 'SQL 不能為空',
    pool_use_count: '使用 {count} 次',
    pool_load_failed: '公共池載入失敗',
    pool_save_success: '儲存成功',
    pool_upload_success: '上傳成功',
    pool_save_failed: '公共池儲存失敗',
    pool_insert_success: '已插入控制台',
    pool_insert_failed: '插入控制台失敗',
    pool_copy_success: '已複製到我的查詢歷史',
    pool_copy_failed: '複製到我的失敗',
    pool_offline_confirm: '刪除後其他人將無法在公共池看到這條 SQL，後續可透過同名上傳恢復，確認刪除嗎？',
    pool_offline_success: '刪除成功',
    pool_offline_failed: '刪除失敗',
    pool_restore_title: '恢復已刪除記錄',
    pool_restore_confirm: '公共池中存在已刪除的同名標題「{title}」。繼續上傳將更新並恢復原記錄，是否繼續？',
    pool_overwrite_confirm: '目前控制台已有 SQL，插入後會生成新的本地草稿並切換過去，是否繼續？',
    history: '查詢歷史',
    new_history_title: '新增視窗',
    clear_history: '清空歷史',
    no_history: '暫無記錄',
    saved: '已儲存',
    history_detail: 'SQL 詳情',
    sql_content: 'SQL 內容',
    history_record_title: '標題',
    history_record_title_placeholder: '請輸入右側卡片顯示的標題',
    no_history_record_title: '未填寫標題',
    title_required: '標題不能為空',
    title_duplicate: '標題不能重複',
    description: '備註',
    description_placeholder: '請輸入 SQL 備註或用途說明',
    cancel: '取消',
    insert: '插入',
    save: '儲存',
    save_success: '儲存成功',
    save_failed: '儲存失敗',
    delete: '刪除',
    delete_success: '刪除成功',
    delete_failed: '刪除失敗',
    connected: '已連線',
    disconnected: '未連線',
    status_failed: '連線狀態取得失敗'
  },
  advanced_search: {
    // 導覽和標題
    query_center: '查詢中心',
    advanced_search: '進階檢索',
    my_favorites: '我的收藏',
    recent_views: '最近瀏覽',
    shared_views: '共享視圖',
    no_shared_views: '暫無共享視圖',

    // 業務模組
    test_case: '測試用例',
    issue: '缺陷',
    test_plan: '測試計劃',
    test_case_review: '用例評審',

    // 篩選條件
    select_workspace: '選擇工作空間',
    select_project: '選擇項目',
    add_filter: '新增篩選條件',
    search_field: '搜尋欄位',
    clear_all: '清空',

    // 查詢模式
    visual_mode: '視覺化',
    jql_mode: 'JQL',
    jql_placeholder: '輸入 JQL 查詢，例如：project = "電商平台" AND status IN ("Pass", "Prepare")',
    syntax_help: '語法說明',
    execute_query: '執行查詢',
    fix_syntax_error: '請先修復語法錯誤',

    // JQL 說明
    jql_help_title: 'JQL 語法說明',
    jql_help_content: `
      <h4>基礎語法</h4>
      <p>project = "電商平台" AND status IN ("Pass", "Prepare")</p>
      <h4>支援的運算子</h4>
      <ul>
        <li>= : 等於</li>
        <li>!= : 不等於</li>
        <li>~ : 模糊匹配</li>
        <li>IN : 包含於列表</li>
        <li>NOT IN : 不包含於列表</li>
        <li>&gt;, &gt;=, &lt;, &lt;= : 比較運算子</li>
        <li>AND, OR : 邏輯運算子</li>
      </ul>
    `,

    // 跨項目提示
    cross_project_tip: '跨項目查詢僅支援系統欄位篩選，如需使用自訂欄位請選擇單一項目',

    // 欄位分組
    basic_info: '基礎資訊',
    module_specific: '模組專屬',
    audit_trail: '審計追蹤',
    custom_fields: '自訂欄位',

    // 欄位名稱
    id: 'ID',
    name: '名稱',
    title: '標題',
    status: '狀態',
    priority: '優先級',
    creator: '建立人',
    create_time: '建立時間',
    update_time: '更新時間',
    description: '描述',

    // 結果展示
    total_results: '共 {count} 條結果',
    list_view: '列表檢視',
    split_view: '分割檢視',
    column_config: '欄位設定',
    select_columns: '選擇顯示欄位',
    reset_default: '恢復預設',
    export: '匯出',
    select_item_to_view: '選擇一筆記錄檢視詳情',

    // 操作提示
    search: '查詢',
    query_failed: '查詢失敗',
    export_success: '匯出成功',
    export_failed: '匯出失敗',
    load_fields_failed: '載入欄位失敗',
    load_users_failed: '載入使用者列表失敗',
    load_workspaces_failed: '載入工作空間列表失敗',
    load_projects_failed: '載入項目列表失敗',

    // 使用者選擇器
    select_users: '選擇使用者',
    current_user: '我自己',
    enter_value: '請輸入',
    select_value: '請選擇',
    to: '至',
    start_date: '開始日期',
    end_date: '結束日期',
    select_date: '選擇日期',
    select_module: '選擇模組'
  }
}

// 合併 advanced-search 翻譯
if (advancedSearch && advancedSearch.advanced_search) {
  message.advanced_search = { ...message.advanced_search, ...advancedSearch.advanced_search };
}

export default {
  ...el,
  ...fu,
  ...mf,
  ...message
};
