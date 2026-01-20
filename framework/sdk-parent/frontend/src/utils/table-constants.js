// 模板
import i18n from "../i18n";
import {AZURE_DEVOPS, LOCAL, TAPD} from "./constants";

export const CUSTOM_FIELD_TYPE_OPTION = [
  {value: 'input', text: 'workspace.custom_filed.input'},
  {value: 'textarea', text: 'workspace.custom_filed.textarea'},
  {value: 'select', text: 'workspace.custom_filed.select', hasOption: true},
  {value: 'multipleSelect', text: 'workspace.custom_filed.multipleSelect', hasOption: true},
  {value: 'radio', text: 'workspace.custom_filed.radio', hasOption: true},
  {value: 'checkbox', text: 'workspace.custom_filed.checkbox', hasOption: true},
  {value: 'associatedSystem', text: 'workspace.custom_filed.associatedSystem', hasOption: true},
  {value: 'multipleAssociatedSystem', text: 'workspace.custom_filed.multipleAssociatedSystem', hasOption: true},
  {value: 'member', text: 'workspace.custom_filed.member', hasOption: true},
  {value: 'multipleMember', text: 'workspace.custom_filed.multipleMember', hasOption: true},
  {value: 'date', text: 'workspace.custom_filed.date'},
  {value: 'datetime', text: 'workspace.custom_filed.datetime'},
  {value: 'richText', text: 'workspace.custom_filed.richText'},
  {value: 'int', text: 'workspace.custom_filed.int'},
  {value: 'float', text: 'workspace.custom_filed.float'},
  {value: 'multipleInput', text: 'workspace.custom_filed.multipleInput'},
  {value: 'cascadingSelect', text: 'workspace.custom_filed.cascadingSelect'}
];

export function CUSTOM_FIELD_TYPE_FILTERS(_this) {
  return [
    {value: 'input', text: _this.$t('workspace.custom_filed.input')},
    {value: 'textarea', text: _this.$t('workspace.custom_filed.textarea')},
    {value: 'select', text: _this.$t('workspace.custom_filed.select')},
    {value: 'multipleSelect', text: _this.$t('workspace.custom_filed.multipleSelect')},
    {value: 'radio', text: _this.$t('workspace.custom_filed.radio')},
    {value: 'checkbox', text: _this.$t('workspace.custom_filed.checkbox')},
    {value: 'member', text: _this.$t('workspace.custom_filed.member')},
    {value: 'multipleMember', text: _this.$t('workspace.custom_filed.multipleMember')},
    {value: 'date', text: _this.$t('workspace.custom_filed.date')},
    {value: 'datetime', text: _this.$t('workspace.custom_filed.datetime')},
    {value: 'richText', text: _this.$t('workspace.custom_filed.richText')},
    {value: 'int', text: _this.$t('workspace.custom_filed.int')},
    {value: 'float', text: _this.$t('workspace.custom_filed.float')},
    {value: 'multipleInput', text: _this.$t('workspace.custom_filed.multipleInput')},
    {value: 'cascadingSelect', text: _this.$t('workspace.custom_filed.cascadingSelect')}
  ];
}

export const UI_ELEMENT_LOCATION_TYPE_OPTION = [
  {value: 'id', text: 'id'},
  {value: 'name', text: 'name'},
  {value: 'className', text: 'className'},
  {value: 'tagName', text: 'tagName'},
  {value: 'linkText', text: 'linkText'},
  {value: 'partialLinkText', text: 'partialLinkText'},
  {value: 'css', text: 'css'},
  {value: 'xpath', text: 'xpath'},
  {value: 'label', text: 'label'},
  {value: 'value', text: 'value'},
  {value: 'index', text: 'index'},
];

export const CUSTOM_FIELD_SCENE_OPTION = [
  {value: 'TEST_CASE', text: 'workspace.case_template_manage'},
  {value: 'ISSUE', text: 'workspace.issue_template_manage'},
  {value: 'API', text: 'workspace.api_template_manage'}
];

export function CASE_TYPE_OPTION(){
  return [
    {value: 'functional', text: i18n.t('api_test.home_page.failed_case_list.table_value.case_type.functional')},
  ];
}

export const ISSUE_PLATFORM_OPTION = [
  {value: LOCAL, text: 'Local'},
  {value: TAPD, text: 'Tapd'},
  {value: AZURE_DEVOPS, text: 'Azure Devops'},
];

export const FIELD_TYPE_MAP = {
  input: 'workspace.custom_filed.input',
  textarea: 'workspace.custom_filed.textarea',
  select: 'workspace.custom_filed.select',
  multipleSelect: 'workspace.custom_filed.multipleSelect',
  radio: 'workspace.custom_filed.radio',
  checkbox: 'workspace.custom_filed.checkbox',
  associatedSystem: 'workspace.custom_filed.associatedSystem',
  multipleAssociatedSystem: 'workspace.custom_filed.multipleAssociatedSystem',
  member: 'workspace.custom_filed.member',
  multipleMember: 'workspace.custom_filed.multipleMember',
  date: 'workspace.custom_filed.date',
  datetime: 'workspace.custom_filed.datetime',
  richText: 'workspace.custom_filed.richText',
  int: 'workspace.custom_filed.int',
  float: 'workspace.custom_filed.float',
  multipleInput: 'workspace.custom_filed.multipleInput',
  cascadingSelect: 'workspace.custom_filed.cascadingSelect'
};

export const SCENE_MAP = {
  ISSUE: 'workspace.issue_template_manage',
  TEST_CASE: 'workspace.case_template_manage',
  PLAN: 'workstation.table_name.track_plan',
  API: 'workspace.api_template_manage'
};

export const SYSTEM_FIELD_NAME_MAP = {
  //用例字段
  用例状态: 'custom_field.case_status',
  责任人: 'custom_field.case_maintainer',
  用例等级: 'custom_field.case_priority',
  备注: 'custom_field.case_remark',
  用例结论: 'test_track.case.conclusion',
  复测数据: 'custom_field.case_retest_data',
  复测是否通过: 'custom_field.case_retest_pass',
  执行方式: 'custom_field.case_execution_method',
  执行时间: 'custom_field.case_execution_time',
  执行结果: 'custom_field.case_execution_result',
  是否通过: 'custom_field.case_pass',
  测试数据: 'custom_field.case_test_data',
  用例编写人: 'custom_field.case_creator',
  用例执行人: 'custom_field.case_executor',
  设计人: 'custom_field.case_designer',
  输入数据: 'custom_field.case_input_data',
  所属系统: 'custom_field.case_associated_system',
  //缺陷字段
  创建人: 'custom_field.issue_creator',
  处理人: 'custom_field.issue_processor',
  状态: 'custom_field.issue_status',
  严重程度: 'custom_field.issue_severity',
  需求号: 'test_track.issue.requirement_number',
  缺陷所属系统: 'custom_field.issue_associated_system',

  优先级: 'test_track.issue.priority',
  严重级别: 'test_track.issue.severity_level',
  缺陷产生原因: 'test_track.issue.issue_cause',
  解决时间: 'test_track.issue.resolved_time',
  关闭时间: 'test_track.issue.closed_time',
  复测次数: 'test_track.issue.retest_count',
  模块: 'test_track.issue.module',
  // 测试计划
  测试阶段: 'test_track.plan.plan_stage'
}


export const ISSUE_STATUS_MAP = {
  'new': i18n.t('test_track.issue.status_new'),
  'closed': i18n.t('test_track.issue.status_closed'),
  'resolved': i18n.t('test_track.issue.status_resolved'),
  'active': i18n.t('test_track.issue.status_active'),
  'delete': i18n.t('test_track.issue.status_delete'),
  'created':i18n.t('test_track.issue.status_new')
}

export const TAPD_ISSUE_STATUS_MAP = {
  'new': i18n.t('test_track.issue.tapd_status_new'),
  'in_progress': i18n.t('test_track.issue.tapd_status_in_progress'),
  'reopened': i18n.t('test_track.issue.tapd_status_reopened'),
  'rejected': i18n.t('test_track.issue.tapd_status_rejected'),
  'verified': i18n.t('test_track.issue.tapd_status_verified'),
  'closed':i18n.t('test_track.issue.tapd_status_closed'),
  'resolved':i18n.t('test_track.issue.tapd_status_resolved'),
  'suspended': i18n.t('test_track.issue.tapd_status_suspended'),
}

export function API_SCENARIO_FILTERS () {
  return {
    STATUS_FILTERS: [
      {text: i18n.t('test_track.plan.plan_status_prepare'), value: 'Prepare'},
      {text: i18n.t('test_track.plan.plan_status_running'), value: 'Underway'},
      {text: i18n.t('test_track.plan.plan_status_completed'), value: 'Completed'},
    ],
    TRASH_FILTERS: [
      {text: i18n.t('test_track.plan.plan_status_trash'), value: 'Trash'},
    ],

    LEVEL_FILTERS: [
      {text: 'P0', value: 'P0'},
      {text: 'P1', value: 'P1'},
      {text: 'P2', value: 'P2'},
      {text: 'P3', value: 'P3'}
    ],
    RESULT_FILTERS: [
      {text: 'Pending', value: 'PENDING'},
      {text: 'Success', value: 'SUCCESS'},
      {text: 'Error', value: 'ERROR'},
      {text: "FakeError", value: 'FAKE_ERROR'},
    ]
  };
  /*LEVEL_FILTERS: [
    {text: 'P0', value: 'P0'},
    {text: 'P1', value: 'P1'},
    {text: 'P2', value: 'P2'},
    {text: 'P3', value: 'P3'}
  ];
  RESULT_FILTERS: [
    {text: i18n.t('api_test.automation.fail'), value: 'Fail'},
    {text: i18n.t('api_test.automation.success'), value: 'Success'}
  ];*/

  /*STATUS_FILTERS: [
    {text: i18n.t('test_track.plan.plan_status_prepare'), value: 'Prepare'},
    {text: i18n.t('test_track.plan.plan_status_running'), value: 'Underway'},
    {text: i18n.t('test_track.plan.plan_status_completed'), value: 'Completed'},
    {text: i18n.t('test_track.plan.plan_status_trash'), value: 'Trash'},
  ],*/
}



export const USER_GROUP_SCOPE = {
  'SYSTEM': 'group.system',
  'WORKSPACE': 'group.workspace',
  'PROJECT': 'group.project',
  'PERSONAL': 'group.personal'
}

export const PROJECT_GROUP_SCOPE = {
  'TRACK': 'permission.other.track',
  'API': 'permission.other.api',
  'UI': 'permission.other.ui',
  'PERFORMANCE': 'permission.other.performance',
  'REPORT': 'permission.other.report'
}
