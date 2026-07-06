import i18n from "../i18n";

export const TEST_CASE_LIST = 'test_case_list'
export const TEST_CASE_REVIEW_LIST = 'test_case_review_list'
export const API_LIST = 'api_list'
export const API_CASE_LIST = 'api_case_list'
export const API_SCENARIO_LIST = 'api_scenario_list'
export const TEST_CASE_REVIEW_CASE_LIST = 'test_case_review_case_list'
export const TEST_PLAN_LIST = 'test_plan_list'
export const TEST_PLAN_FUNCTION_TEST_CASE = 'test_plan_function_test_case'
export const TEST_PLAN_API_CASE = 'test_plan_api_case'
export const TEST_PLAN_LOAD_CASE = 'test_plan_load_case'
export const TEST_PLAN_SCENARIO_CASE = 'test_plan_scenario_case'
export const TEST_PLAN_UI_SCENARIO_CASE = 'test_plan_ui_scenario_case'

export const TokenKey = "Admin-Token" // 自行修改
export const LicenseKey = 'License';
export const PROJECT_VERSION_ENABLE = 'PROJECT_VERSION_ENABLE';
export const DEFAULT_LANGUAGE = 'default_language';
export const CURRENT_LANGUAGE = 'current_language';
export const UPLOAD_LIMIT = "upload_limit";

export const ROLE_ADMIN = 'admin';
export const ROLE_ORG_ADMIN = 'org_admin';
export const ROLE_TEST_MANAGER = 'test_manager';
export const ROLE_TEST_USER = 'test_user';
export const ROLE_TEST_VIEWER = 'test_viewer';
export const SUPER_GROUP = 'super_group';

export const ORGANIZATION_ID = 'organization_id';
export const WORKSPACE_ID = 'workspace_id';
export const CURRENT_PROJECT = 'current_project';
export const PROJECT_ID = 'project_id';
export const PROJECT_NAME = 'project_name';

export const REFRESH_SESSION_USER_URL = 'user/refresh';
export const WORKSPACE = 'workspace';
export const ORGANIZATION = 'organization';
export const DEFAULT = 'default';

export const ZH_CN = 'zh_CN';
export const ZH_TW = 'zh_TW';
export const EN_US = 'en_US';

export const TAPD = 'Tapd';
export const ZEN_TAO = 'Zentao';
export const LOCAL = 'Local';
export const AZURE_DEVOPS = 'AzureDevops';

export const GROUP_SYSTEM = 'SYSTEM';
export const GROUP_WORKSPACE = 'WORKSPACE';
export const GROUP_PROJECT = 'PROJECT';

export const GROUP_TYPE = {
  SYSTEM: 'SYSTEM',
  WORKSPACE: 'WORKSPACE',
  PROJECT: 'PROJECT'
}

export const SCHEDULE_TYPE = {
  API_TEST: 'API_TEST',
  PERFORMANCE_TEST: 'PERFORMANCE_TEST'
}

export const REQUEST_HEADERS = [
  {value: 'Accept'},
  {value: 'Accept-Charset'},
  {value: 'Accept-Language'},
  {value: 'Accept-Datetime'},
  {value: 'Authorization'},
  {value: 'Cache-Control'},
  {value: 'Connection'},
  {value: 'Cookie'},
  {value: 'Content-Length'},
  {value: 'Content-MD5'},
  {value: 'Content-Type'},
  {value: 'Date'},
  {value: 'Expect'},
  {value: 'From'},
  {value: 'Host'},
  {value: 'If-Match'},
  {value: 'If-Modified-Since'},
  {value: 'If-None-Match'},
  {value: 'If-Range'},
  {value: 'If-Unmodified-Since'},
  {value: 'Max-Forwards'},
  {value: 'Origin'},
  {value: 'Pragma'},
  {value: 'Proxy-Authorization'},
  {value: 'Range'},
  {value: 'Referer'},
  {value: 'TE'},
  {value: 'User-Agent'},
  {value: 'Upgrade'},
  {value: 'Via'},
  {value: 'Warning'}
]

const translateMockFunction = (key, fallback) => {
  const value = i18n.t(key);
  return value === key ? fallback : value;
};

export const MOCKJS_FUNC = [
  {name: '@boolean', des: i18n.t('api_test.request.boolean'), ex: true},
  {name: '@natural', des: i18n.t('api_test.request.natural'), ex: 72834},
  {name: '@integer', des: i18n.t('api_test.request.integer'), ex: 79750},
  {name: '@float', des: i18n.t('api_test.request.float'), ex: 24.2},
  {name: '@character', des: i18n.t('api_test.request.character'), ex: "k"},
  {name: '@string', des: i18n.t('api_test.request.string'), ex: "hello"},
  {name: '@range', des: i18n.t('api_test.request.range'), ex: "org.mozilla.javascript.NavicatArray@1739f809"},
  {name: '@date', des: i18n.t('api_test.request.date'), ex: "1973-01-08"},
  {name: '@time', des: i18n.t('api_test.request.time'), ex: "06:15:27"},
  {name: '@datetime', des: i18n.t('api_test.request.datetime'), ex: "1975-10-12 02:32:04"},
  {name: '@now', des: i18n.t('api_test.request.now'), ex: (new Date()).toLocaleTimeString().toLocaleString()},
  {name: '@img', des: i18n.t('api_test.request.img'), ex: "http://dummyimage.com/120x60"},
  {name: '@color', des: i18n.t('api_test.request.color'), ex: "#b479f2"},
  {name: '@hex', des: i18n.t('api_test.request.hex'), ex: "#f27984"},
  {name: '@rgb', des: i18n.t('api_test.request.rgb'), ex: "rgb(203, 242, 121)"},
  {name: '@rgba', des: i18n.t('api_test.request.rgba'), ex: "rgba(242, 121, 238, 0.66)"},
  {name: '@hsl', des: i18n.t('api_test.request.hsl'), ex: "hsl(164, 82, 71)"},
  {name: '@paragraph', des: i18n.t('api_test.request.paragraph'), ex: "Iwvh qxuvn uigzjw xijvntv dfidxtof"},
  {name: '@sentence', des: i18n.t('api_test.request.sentence'), ex: "Hfi fpqnqerrs sghxldx oqpghvnmy"},
  {name: '@word', des: i18n.t('api_test.request.word'), ex: "shnjlyazvi"},
  {name: '@title', des: i18n.t('api_test.request.title'), ex: "Tefsdc Vhs Ujx"},
  {name: '@cparagraph', des: translateMockFunction('api_test.request.cparagraph', '段落'), ex: "色青元处才不米拉律消叫别金如上。"},
  {name: '@csentence', des: translateMockFunction('api_test.request.csentence', '句子'), ex: "与形府部速她运改织图集料进完。"},
  {name: '@cword', des: translateMockFunction('api_test.request.cword', '词语'), ex: "满"},
  {name: '@ctitle', des: i18n.t('api_test.request.ctitle'), ex: "运满前省快"},
  {name: '@first', des: i18n.t('api_test.request.first'), ex: "Mary"},
  {name: '@last', des: i18n.t('api_test.request.last'), ex: "Miller"},
  {name: '@name', des: i18n.t('api_test.request.name'), ex: "Robert Lee"},
  {name: '@cfirst', des: i18n.t('api_test.request.cfirst'), ex: "龚"},
  {name: '@clast', des: i18n.t('api_test.request.clast'), ex: "刚"},
  {name: '@cname', des: i18n.t('api_test.request.cname'), ex: "江娟"},
  {name: '@url', des: i18n.t('api_test.request.url'), ex: "wais://jopnwwj.bh/lqnhn"},
  {name: '@domain', des: i18n.t('api_test.request.domain'), ex: "rsh.bt"},
  {name: '@protocol', des: i18n.t('api_test.request.protocol'), ex: "rlogin"},
  {name: '@tld', des: i18n.t('api_test.request.tld'), ex: "sa"},
  {name: '@email', des: i18n.t('api_test.request.email'), ex: "d.somdg@edntlm.cd"},
  {name: '@ip', des: i18n.t('api_test.request.ip'), ex: "22.151.93.255"},
  {name: '@region', des: i18n.t('api_test.request.region'), ex: "东北"},
  {name: '@province', des: i18n.t('api_test.request.province'), ex: "陕西省"},
  {name: '@city', des: i18n.t('api_test.request.city'), ex: "珠海市"},
  {name: '@county', des: i18n.t('api_test.request.county'), ex: "正宁县"},
  {name: '@zip', des: i18n.t('api_test.request.zip'), ex: 873247},
  {name: '@capitalize', des: i18n.t('api_test.request.capitalize'), ex: "Undefined"},
  {name: '@upper', des: i18n.t('api_test.request.upper'), ex: "UNDEFINED"},
  {name: '@lower', des: i18n.t('api_test.request.lower'), ex: "undefined"},
  {name: '@pick', des: i18n.t('api_test.request.pick'), ex: "None example"},
  {name: '@shuffle', des: i18n.t('api_test.request.shuffle'), ex: "org.mozilla.javascript.NavicatArray@2264545d"},
  {name: '@guid', des: i18n.t('api_test.request.guid'), ex: "4f9CeC2c-8d59-40f6-ec4F-2Abbc5C94Ddf"},
  {name: '@id', des: i18n.t('api_test.request.id'), ex: "450000197511051762"},
  {name: '@increment', des: i18n.t('api_test.request.increment'), ex: 1}
]

export const JMETER_FUNC = [
  {type: "信息", name: "${__threadNum}", description: "获取线程号"},
  {type: "信息", name: "${__threadGroupName}", description: "获取线程组名称"},
  {type: "信息", name: "${__samplerName}", description: "获取取样器名称（标签）"},
  {type: "信息", name: "${__machineIP}", description: "获取本机 IP 地址"},
  {type: "信息", name: "${__machineName}", description: "获取本机名称"},
  {type: "信息", name: "${__time}", description: "以多种格式返回当前时间"},
  {
    type: "信息",
    name: "${__timeShift}",
    description: "返回指定增加秒/分/时/天后的日期，支持多种格式"
  },
  {type: "信息", name: "${__log}", description: "记录（或显示）一条消息（并返回该值）"},
  {type: "信息", name: "${__logn}", description: "记录（或显示）一条消息（返回值为空）"},
  {type: "输入", name: "${__StringFromFile}", description: "从文件中读取一行"},
  {type: "输入", name: "${__FileToString}", description: "读取整个文件"},
  {type: "输入", name: "${__CSVRead}", description: "从 CSV 分隔文件中读取"},
  {type: "输入", name: "${__XPath}", description: "使用 XPath 表达式从文件中读取"},
  {type: "输入", name: "${__StringToFile}", description: "将字符串写入文件"},
  {type: "计算", name: "${__counter}", description: "生成递增数字"},
  {
    type: "格式化",
    name: "${__dateTimeConvert}",
    description: "将日期或时间从源格式转换为目标格式"
  },
  {type: "计算", name: "${__digest}", description: "生成摘要（SHA-1、SHA-256、MD5 等）"},
  {type: "计算", name: "${__intSum}", description: "整数相加"},
  {type: "计算", name: "${__longSum}", description: "长整数相加"},
  {type: "计算", name: "${__Random}", description: "生成随机数"},
  {type: "计算", name: "${__RandomDate}", description: "在指定日期范围内生成随机日期"},
  {
    type: "计算",
    name: "${__RandomFromMultipleVars}",
    description: "从以 | 分隔的一组变量值中随机提取一个元素"
  },
  {type: "计算", name: "${__RandomString}", description: "生成随机字符串"},
  {type: "计算", name: "${__UUID}", description: "生成随机类型 4 的 UUID"},
  {type: "脚本", name: "${__groovy}", description: "运行 Apache Groovy 脚本"},
  {type: "脚本", name: "${__BeanShell}", description: "运行 BeanShell 脚本"},
  {type: "脚本", name: "${__javaScript}", description: "执行 JavaScript（Nashorn）"},
  {type: "脚本", name: "${__jexl2}", description: "计算 Commons Jexl2 表达式"},
  {type: "脚本", name: "${__jexl3}", description: "计算 Commons Jexl3 表达式"},
  {type: "属性", name: "${__isPropDefined}", description: "判断属性是否存在"},
  {type: "属性", name: "${__property}", description: "读取属性"},
  {type: "属性", name: "${__P}", description: "读取属性（简写方法）"},
  {type: "属性", name: "${__setProperty}", description: "设置 JMeter 属性"},
  {type: "变量", name: "${__split}", description: "将字符串拆分为多个变量"},
  {type: "变量", name: "${__eval}", description: "计算变量表达式"},
  {type: "变量", name: "${__evalVar}", description: "计算存储在变量中的表达式"},
  {type: "属性", name: "${__isVarDefined}", description: "判断变量是否存在"},
  {type: "变量", name: "${__V}", description: "计算变量名"},
  {type: "字符串", name: "${__char}", description: "从数字列表生成 Unicode 字符"},
  {type: "字符串", name: "${__changeCase}", description: "按不同模式转换大小写"},
  {type: "字符串", name: "${__escapeHtml}", description: "对字符串进行 HTML 编码"},
  {type: "字符串", name: "${__escapeOroRegexpChars}", description: "转义 ORO 正则表达式中的元字符"},
  {type: "字符串", name: "${__escapeXml}", description: "对字符串进行 XML 编码"},
  {type: "字符串", name: "${__regexFunction}", description: "使用正则表达式解析上次响应"},
  {type: "字符串", name: "${__unescape}", description: "处理包含 Java 转义字符的字符串（如 \n 和 \t）"},
  {type: "字符串", name: "${__unescapeHtml}", description: "解码 HTML 编码的字符串"},
  {type: "字符串", name: "${__urldecode}", description: "解码 application/x-www-form-urlencoded 字符串"},
  {
    type: "字符串",
    name: "${__urlencode}",
    description: "将字符串编码为 application/x-www-form-urlencoded 格式"
  },
  {type: "字符串", name: "${__TestPlanName}", description: "返回当前测试计划名称"},
]

export const ORIGIN_COLOR = '#783887';
export const ORIGIN_COLOR_SHALLOW = '#595591';
export const COUNT_NUMBER = '#6C317C';
export const COUNT_NUMBER_SHALLOW = '#CDB9D2';
export const PRIMARY_COLOR = '#783887';

export const CONFIG_TYPE = {
  NOT: "NOT",
  NORMAL: "NORMAL",
  ABNORMAL: "ABNORMAL"
}

export const WORKSTATION = {
  UPCOMING: "upcoming",
  FOCUS: "focus",
  NODE: "node"
}

export const ENV_TYPE = {
  JSON: "JSON",
  GROUP: "GROUP"
}

export const DEFAULT_XSS_ATTR = ['style', 'class'];

export const SECOND_LEVEL_ROUTE_PERMISSION_MAP = {
  API: [
    {router: '/api/home', permission: ['PROJECT_API_HOME:READ']},
    {router: '/api/definition', permission: ['PROJECT_API_DEFINITION:READ']},
    {router: '/api/automation', permission: ['PROJECT_API_SCENARIO:READ']},
    {router: '/api/automation/report', permission: ['PROJECT_API_REPORT:READ']},
  ],
  TRACK: [
    {router: '/track/home', permission: ['PROJECT_TRACK_HOME:READ']},
    {router: '/track/case/all', permission: ['PROJECT_TRACK_CASE:READ']},
    {router: '/track/review/all', permission: ['PROJECT_TRACK_REVIEW:READ']},
    {router: '/track/plan/all', permission: ['PROJECT_TRACK_PLAN:READ']},
    {router: '/track/issue', permission: ['PROJECT_TRACK_ISSUE:READ']},
    {router: '/track/testPlan/reportList', permission: ['PROJECT_TRACK_REPORT:READ']},
  ],
  LOAD: [
    {router: '/performance/home', permission: ['PROJECT_PERFORMANCE_HOME:READ']},
    {router: '/performance/test/all', permission: ['PROJECT_PERFORMANCE_TEST:READ']},
    {router: '/performance/report/all', permission: ['PROJECT_PERFORMANCE_REPORT:READ']},
  ],
  UI: [
    {router: '/ui/automation', permission: ['PROJECT_UI_SCENARIO:READ']},
    {router: '/ui/element', permission: ['PROJECT_UI_ELEMENT:READ']},
    {router: '/ui/report', permission: ['PROJECT_UI_REPORT:READ']},
  ],
  REPORT: [
    {router: '/report/projectStatistics', permission: ['PROJECT_REPORT_ANALYSIS:READ']},
    {
      router: '/report/projectReport',
      permission: [
        'PROJECT_ENTERPRISE_REPORT:READ+EXPORT', 'PROJECT_ENTERPRISE_REPORT:READ+CREATE',
        'PROJECT_ENTERPRISE_REPORT:READ+DELETE', 'PROJECT_ENTERPRISE_REPORT:READ+COPY',
        'PROJECT_ENTERPRISE_REPORT:READ+SCHEDULE', 'PROJECT_ENTERPRISE_REPORT:READ+EDIT'
      ]
    }
  ]
}

export const TASK_PATH = [
  "/test/case/add",
  "/test/case/review/save",
  "/test/case/comment/save",
  "/test/plan/add",
  "/test/plan/relevance",
  "issues/add",
  "test/case/issues/relate",
  "/api/definition/create",
  "/api/definition/run/debug",
  "/api/testcase/create",
  "/share/generate/api/document",
  "/api/definition/import",
  "/api/automation/create",
  "/api/automation/schedule/create",
  "/performance/save",
  "/share/generate/expired",
  "/project/add",
  "/project/member/add",
  "/setting/user/project/member/add",
  "/environment/add",
  "/ui/element/add",
  "/ui/automation/create",
  "/ui/automation/run/debug",
];

export const TASK_DATA = [
  {
    id: 1,
    name: "track",
    title: "side_task.test_tracking.title",
    percentage: 14,
    permission: ['PROJECT_MANAGER:READ', 'WORKSPACE_PROJECT_MANAGER:READ', 'PROJECT_TRACK_CASE:READ+CREATE', 'PROJECT_TRACK_REVIEW:READ+CREATE', 'PROJECT_TRACK_REVIEW:READ+COMMENT', 'PROJECT_TRACK_PLAN:READ+CREATE', 'PROJECT_TRACK_PLAN:READ+RELEVANCE_OR_CANCEL', 'PROJECT_TRACK_ISSUE:READ+CREATE', 'PROJECT_TRACK_CASE:READ+BATCH_ADD_PUBLIC'],
    taskData: [
      {
        id: 1,
        name: "side_task.test_tracking.task_1",
        status: 1,
        permission: ['PROJECT_MANAGER:READ', 'WORKSPACE_PROJECT_MANAGER:READ'],
        api: [''],
        path: '/setting/project/:type',
        url: ""
      },
      {
        id: 2,
        name: "side_task.test_tracking.task_2",
        status: 0,
        permission: ['PROJECT_TRACK_CASE:READ+CREATE'],
        api: ["/test/case/add"],
        path: '/track/case/all',
        url: "/assets/guide/track/task-2.gif"
      },
      {
        id: 3,
        name: "side_task.test_tracking.task_3",
        status: 0,
        permission: ['PROJECT_TRACK_REVIEW:READ+CREATE'],
        api: ["/test/case/review/save"],
        path: '/track/review/all',
        url: "/assets/guide/track/task-3.gif"
      },
      {
        id: 4,
        name: "side_task.test_tracking.task_4",
        status: 0,
        permission: ['PROJECT_TRACK_REVIEW:READ+COMMENT'],
        api: ["/test/case/comment/save"],
        path: '/track/review/all',
        url: "/assets/guide/track/task-4.gif"
      },
      {
        id: 5,
        name: "side_task.test_tracking.task_5",
        status: 0,
        permission: ['PROJECT_TRACK_PLAN:READ+CREATE'],
        api: ["/test/plan/add"],
        path: '/track/plan/all',
        url: "/assets/guide/track/task-5.gif"
      },
      {
        id: 6,
        name: "side_task.test_tracking.task_6",
        status: 0,
        permission: ['PROJECT_TRACK_PLAN:READ+RELEVANCE_OR_CANCEL'],
        api: ["/test/plan/relevance"],
        path: '/track/plan/all',
        url: "/assets/guide/track/task-6.gif"
      },
      {
        id: 7,
        name: "side_task.test_tracking.task_7",
        status: 0,
        permission: ['PROJECT_TRACK_ISSUE:READ+CREATE', 'PROJECT_TRACK_CASE:READ+BATCH_ADD_PUBLIC'],
        api: ["issues/add", "test/case/issues/relate"],
        path: '/track/issue',
        url: "/assets/guide/track/task-7.gif"
      },
    ],
    rate: 1,
    status: 0
  },
  {
    id: 2,
    name: "api",
    title: 'side_task.api_test.title',
    percentage: 0,
    permission: ['PROJECT_API_DEFINITION:READ+CREATE_API', 'PROJECT_API_DEFINITION:READ+IMPORT_API', 'PROJECT_API_DEFINITION:READ+DEBUG', 'PROJECT_API_DEFINITION:READ+CREATE_CASE', 'PROJECT_API_DEFINITION:READ', 'PROJECT_API_SCENARIO:READ+CREATE', 'PROJECT_API_SCENARIO:READ+SCHEDULE'],
    taskData: [
      {
        id: 1,
        name: "side_task.api_test.task_1",
        status: 0,
        path: '/api/definition',
        permission: ['PROJECT_API_DEFINITION:READ+CREATE_API'],
        api: ["/api/definition/create"],
        url: "/assets/guide/api/task-1.gif"
      },
      {
        id: 2,
        name: "side_task.api_test.task_2",
        status: 0,
        path: '/api/definition',
        permission: ['PROJECT_API_DEFINITION:READ+IMPORT_API'],
        api: ["/api/definition/import"],
        url: "/assets/guide/api/task-2.gif"
      },
      {
        id: 3,
        name: "side_task.api_test.task_3",
        status: 0,
        path: '/api/definition',
        permission: ['PROJECT_API_DEFINITION:READ+DEBUG'],
        api: ["/api/definition/run/debug"],
        url: "/assets/guide/api/task-3.gif"
      },
      {
        id: 4,
        name: "side_task.api_test.task_4",
        status: 0,
        path: '/api/definition',
        permission: ['PROJECT_API_DEFINITION:READ+CREATE_CASE'],
        api: ["/api/testcase/create"],
        url: "/assets/guide/api/task-4.gif"
      },
      {
        id: 5,
        name: "side_task.api_test.task_5",
        status: 0,
        path: '/api/definition',
        permission: ['PROJECT_API_DEFINITION:READ'],
        api: ["/share/generate/api/document"],
        url: "/assets/guide/api/task-5.gif"
      },
      {
        id: 6,
        name: "side_task.api_test.task_6",
        status: 0,
        path: '/api/automation',
        permission: ['PROJECT_API_SCENARIO:READ+CREATE'],
        api: ["/api/automation/create"],
        url: "/assets/guide/api/task-6.gif"
      },
      {
        id: 7,
        name: "side_task.api_test.task_7",
        status: 0,
        path: '/api/automation',
        permission: ['PROJECT_API_SCENARIO:READ+SCHEDULE'],
        api: ["/api/automation/schedule/create"],
        url: "/assets/guide/api/task-7.gif"
      },
    ],
    rate: 0,
    status: 0
  },
  {
    id: 3,
    name: "performance",
    title: 'side_task.performance_test.title',
    percentage: 0,
    permission: ['PROJECT_API_SCENARIO:READ+CREATE_PERFORMANCE', "PROJECT_API_SCENARIO:READ+CREATE_PERFORMANCE_BATCH", 'PROJECT_PERFORMANCE_REPORT:READ'],
    taskData: [
      {
        id: 1,
        name: 'side_task.performance_test.task_1',
        status: 0,
        path: '/performance/test/all',
        permission: ['PROJECT_API_SCENARIO:READ+CREATE_PERFORMANCE', "PROJECT_API_SCENARIO:READ+CREATE_PERFORMANCE_BATCH"],
        api: ["/performance/save"],
        url: "/assets/guide/performance/task-1.gif"
      },
      {
        id: 2,
        name: 'side_task.performance_test.task_2',
        status: 0,
        path: '/performance/report/all',
        permission: ['PROJECT_PERFORMANCE_REPORT:READ'],
        api: ["/share/generate/expired"],
        url: "/assets/guide/performance/task-2.gif"
      },
    ],
    rate: 0,
    status: 0
  },
  {
    id: 4,
    name: "project",
    title: 'side_task.project_setting.title',
    percentage: 0,
    permission: ['WORKSPACE_PROJECT_MANAGER:READ+CREATE', 'PROJECT_USER:READ+CREATE', 'PROJECT_ENVIRONMENT:READ+CREATE'],
    taskData: [
      {
        id: 1,
        name: 'side_task.project_setting.task_1',
        status: 0,
        permission: ['WORKSPACE_PROJECT_MANAGER:READ+CREATE'],
        api: ["/project/add"],
        path: '/setting/project/:type',
        url: "/assets/guide/project/task-1.gif"
      },
      {
        id: 2,
        name: 'side_task.project_setting.task_2',
        status: 0,
        permission: ['PROJECT_USER:READ+CREATE'],
        api: ["/project/member/add", "/setting/user/project/member/add"],
        path: '/project/member',
        url: "/assets/guide/project/task-2.gif"
      },
      {
        id: 3,
        name: 'side_task.project_setting.task_3',
        status: 0,
        permission: ['PROJECT_ENVIRONMENT:READ+CREATE'],
        api: ["/environment/add"],
        path: '/project/env',
        url: "/assets/guide/project/task-3.gif"
      },
    ],
    rate: 0,
    status: 0
  },
  {
    id: 5,
    name: "ui",
    title: 'side_task.ui_test.title',
    percentage: 0,
    permission: ['PROJECT_UI_ELEMENT:READ+CREATE', 'PROJECT_UI_SCENARIO:READ+CREATE', 'PROJECT_UI_SCENARIO:READ+RUN', 'PROJECT_UI_SCENARIO:READ+DEBUG'],
    taskData: [
      {
        id: 1,
        name: 'side_task.ui_test.task_1',
        status: 0,
        permission: ['PROJECT_UI_ELEMENT:READ+CREATE'],
        api: ["/ui/element/add"],
        path: '/ui/element',
        url: "/assets/guide/ui/task-1.gif"
      },
      {
        id: 2,
        name: 'side_task.ui_test.task_2',
        status: 0,
        permission: ['PROJECT_UI_SCENARIO:READ+CREATE'],
        api: ["/ui/automation/create"],
        path: '/ui/automation',
        url: "/assets/guide/ui/task-2.gif"
      },
      {
        id: 2,
        name: 'side_task.ui_test.task_3',
        status: 0,
        permission: ['PROJECT_UI_SCENARIO:READ+RUN', 'PROJECT_UI_SCENARIO:READ+DEBUG'],
        api: ["/ui/automation/run/debug"],
        path: '/ui/automation',
        url: "/assets/guide/ui/task-3.gif"
      },
    ],
    rate: 0,
    status: 0
  },
]
