export default {
  advanced_search: {
    // ========== 模組 ==========
    select_module: '選擇業務模組',
    module_test_case: '測試用例',
    module_issue: '缺陷',
    module_test_plan: '測試計劃',
    module_review: '用例評審',
    module_test_case_review: '用例評審',

    // ========== 篩選 ==========
    select_workspace: '選擇工作空間',
    select_project: '選擇項目',
    select_filter_field: '選擇篩選欄位',
    workspace_placeholder: '全部工作空間',
    project_placeholder: '全部項目',
    cross_project_tip: '跨項目查詢僅支援系統欄位，如需使用自訂欄位請選擇單個項目',
    add_filter: '篩選條件',
    clear_all: '清空',
    removed_project_specific: '已移除 {count} 個項目特有欄位的篩選條件',

    // ========== 欄位分組 ==========
    basic_info: '基礎資訊',
    user_related: '人員相關',
    date_related: '時間相關',
    custom_fields: '自訂欄位',
    audit_trail: '審計追蹤',
    column_config: '欄位設定',
    basic_attributes: '基本屬性',

    // ========== 查詢模式 ==========
    mode_visual: '視覺化',
    mode_jql: 'JQL',
    visual_mode_placeholder: '視覺化查詢模式開發中...',
    switch_to_jql: '切換到 JQL 模式',
    switch_to_visual: '切換到視覺化模式',
    mode_switch_warning: '切換模式將清空目前查詢條件，是否繼續？',

    // ========== JQL 編輯器 ==========
    jql_placeholder: '輸入 JQL 查詢語句，例如：status = "Pass" AND priority = "P0"',
    jql_help_title: 'JQL 語法說明',
    jql_operators: '支援的運算子',
    jql_examples: '查詢範例',
    jql_format: '格式化',
    jql_clear: '清空',
    jql_copy: '複製',
    jql_history: '查詢歷史',
    jql_save: '儲存查詢',
    jql_saved: '已儲存查詢',
    jql_examples_title: '範例查詢',

    // ========== 運算子說明 ==========
    op_equals: '等於',
    op_not_equals: '不等於',
    op_like: '模糊匹配',
    op_compare: '比較運算子',
    op_in: '是',
    op_not_in: '不是',
    op_contains: '包含',
    op_greater: '大於',
    op_greater_equal: '大於等於',
    op_less: '小於',
    op_less_equal: '小於等於',
    op_and: '邏輯與',
    op_or: '邏輯或',
    op_between: '範圍',
    op_before: '早於',
    op_after: '晚於',

    // ========== 輸入提示 ==========
    enter_value: '輸入值',
    select_value: '選擇值',
    select_users: '選擇用戶',
    current_user: '我自己',
    start_date: '開始',
    end_date: '結束',
    to: '至',

    // ========== 智慧提示 ==========
    suggestion_field: '欄位',
    suggestion_operator: '運算子',
    suggestion_value: '值',
    suggestion_keyword: '關鍵字',
    suggestion_recent: '最近使用',

    // ========== 檢視 ==========
    view_list: '列表檢視',
    view_split: '分割檢視',
    view_card: '卡片檢視',
    select_row_to_view_detail: '點擊左側列表檢視詳情',
    no_data: '暫無資料',
    no_search_result: '未找到匹配的結果',
    try_adjust_filter: '請嘗試調整篩選條件',

    // ========== 結果 ==========
    result_count: '共 {count} 條結果',
    total_results: '共檢索到 {count} 條資料',
    result_count_selected: '已選擇 {selected} / 共 {total} 條',
    loading: '載入中...',
    search: '查詢',
    search_success: '查詢成功',
    search_failed: '查詢失敗',
    updated_at: '更新於',

    // ========== 匯出 ==========
    export: '匯出',
    export_too_many: '結果數量超過 10000 條，請縮小查詢範圍後再匯出',

    // ========== 分頁 ==========
    page_size: '每頁條數',
    go_to: '跳至',
    page: '頁',

    // ========== 詳情面板 ==========
    detail_title: '詳情',
    detail_close: '關閉',
    detail_copy_id: '複製 ID',
    detail_open_in_new_tab: '在新頁籤開啟',
    detail_loading: '載入詳情中...',

    // ========== 匯出 ==========
    export_title: '匯出資料',
    export_excel: '匯出 Excel',
    export_csv: '匯出 CSV',
    export_all: '匯出全部',
    export_current_page: '匯出目前頁',
    export_selected: '匯出已選取',
    export_success: '匯出成功',
    export_failed: '匯出失敗',
    export_processing: '正在匯出，請稍候...',
    export_limit_warning: '最多匯出 {limit} 條資料',

    // ========== 儲存查詢 ==========
    save_query_title: '儲存查詢',
    save_query_name: '查詢名稱',
    save_query_name_placeholder: '請輸入查詢名稱',
    save_query_share: '分享給團隊',
    save_query_save: '儲存',
    save_query_cancel: '取消',
    save_query_success: '儲存成功',
    save_query_failed: '儲存失敗',
    save_query_name_required: '請輸入查詢名稱',

    // ========== 查詢歷史 ==========
    query_history_title: '查詢歷史',
    query_history_clear: '清空歷史',
    query_history_empty: '暫無查詢歷史',
    query_history_use: '使用',
    query_history_delete: '刪除',

    // ========== 欄位資訊 ==========
    field_name: '欄位名稱',
    field_label: '顯示名稱',
    field_type: '欄位類型',
    field_type_text: '文字',
    field_type_select: '下拉選單',
    field_type_user: '使用者選擇',
    field_type_date: '日期',
    field_type_datetime: '日期時間',
    field_type_number: '數字',
    field_type_multiselect: '多選',
    field_type_tree: '樹狀選擇',

    // ========== 驗證錯誤 ==========
    validation_error: '語法錯誤',
    validation_success: '語法正確',
    error_expected_field: '期望欄位名稱',
    error_expected_operator: '期望運算子',
    error_expected_value: '期望值',
    error_expected_lparen: '期望左括號',
    error_expected_rparen: '期望右括號',
    error_unknown_field: '未知欄位',
    error_unknown_operator: '未知運算子',
    error_invalid_value: '無效的值',
    error_field_not_support_operator: '該欄位不支援此運算子',
    error_position: '位置: 第 {line} 行, 第 {column} 列',

    // ========== 快捷鍵 ==========
    shortcut_execute: '執行查詢',
    shortcut_focus_editor: '聚焦編輯器',
    shortcut_clear: '清空查詢',
    shortcut_toggle_help: '切換說明面板',

    // ========== 工具列 ==========
    refresh: '重新整理',
    column_settings: '欄位設定',
    display_columns: '顯示欄位',
    reset_columns: '重置欄位',

    // ========== 載入/錯誤訊息 ==========
    load_workspaces_failed: '載入工作空間失敗',
    load_projects_failed: '載入項目失敗',
    load_fields_failed: '載入欄位失敗',
    load_history_failed: '載入查詢歷史失敗',
    load_saved_failed: '載入已儲存查詢失敗',
    load_detail_failed: '載入詳情失敗',

    // ========== 範例查詢 ==========
    example_status_priority: '尋找優先級為 P0 且狀態為通過的用例',
    example_date_range: '尋找最近建立的用例',
    example_in_list: '尋找狀態在指定列表中的缺陷',
    example_complex: '複雜組合查詢範例',
    example_contains: '尋找名稱包含特定關鍵字的資料',

    // ========== JQL 範例文字 ==========
    jql_example_1: 'status = "Pass" AND priority = "P0"',
    jql_example_2: 'status IN ("Pass", "Prepare") AND priority NOT IN ("P3", "P4")',
    jql_example_3: 'name ~ "登入" AND createTime >= "2024-01-01"',
    jql_example_4: '(priority = "P0" OR priority = "P1") AND status != "Deprecated"',
    jql_example_5: 'createUser = "admin" AND updateTime > "2024-01-01" AND updateTime < "2024-12-31"',
    jql_example_6: 'name CONTAINS "測試" AND status IN ("Underway", "Completed")',

    // ========== 確認對話框 ==========
    confirm_title: '確認',
    confirm_clear_query: '確定要清空目前查詢條件嗎？',
    confirm_clear_history: '確定要清空查詢歷史嗎？',
    confirm_delete_saved: '確定要刪除此儲存的查詢嗎？',
    confirm_yes: '確定',
    confirm_no: '取消',

    // ========== 其他 ==========
    all: '全部',
    selected: '已選擇',
    clear: '清空',
    apply: '套用',
    cancel: '取消',
    save: '儲存',
    delete: '刪除',
    edit: '編輯',
    copy: '複製',
    copy_success: '複製成功',
    copy_failed: '複製失敗',
    more: '更多',
    expand: '展開',
    collapse: '收起',

    // ========== 模組專屬欄位 ==========
    priority: '優先級',
    maintainer: '維護人',
    case_type: '用例類型',
    review_status: '評審狀態',
    severity: '嚴重程度',
    assignee: '指派給',
    platform: '缺陷平台',
    principal: '負責人',
    stage: '計劃階段',
    reviewer: '評審人'
  }
};
