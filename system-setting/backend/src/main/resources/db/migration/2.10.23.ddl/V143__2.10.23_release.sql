SET SESSION innodb_lock_wait_timeout = 7200;

-- ============================================
-- 缺陷管理全局系统字段创建
-- 按照逻辑顺序组织：字段插入
-- ============================================

-- ============================================
-- 步骤1：插入自定义字段数据（设置为系统字段：system=1, global=1, project_id=NULL）
-- ============================================
-- 优先级字段
INSERT INTO `custom_field` VALUES ('issue-priority-field-2024-12-01-001', '优先级', 'ISSUE', 'select', '', '[{"value":"重要","text":"重要","system":true},{"value":"致命的","text":"致命的","system":true},{"value":"一般","text":"一般","system":true},{"value":"轻微","text":"轻微","system":true}]', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', NULL, 0);

-- 严重级别字段
INSERT INTO `custom_field` VALUES ('issue-severity-level-field-2024-12-01-002', '严重级别', 'ISSUE', 'select', '', '[{"value":"一般","text":"一般","system":true},{"value":"严重","text":"严重","system":true},{"value":"阻断","text":"阻断","system":true}]', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', NULL, 0);

-- 缺陷产生原因字段
INSERT INTO `custom_field` VALUES ('issue-cause-field-2024-12-01-003', '缺陷产生原因', 'ISSUE', 'select', '', '[{"value":"不是问题","text":"不是问题","system":true},{"value":"操作错误","text":"操作错误","system":true},{"value":"需求变更","text":"需求变更","system":true},{"value":"程序错误","text":"程序错误","system":true},{"value":"优化建议","text":"优化建议","system":true},{"value":"环境原因","text":"环境原因","system":true},{"value":"数据原因","text":"数据原因","system":true},{"value":"新旧核心差异","text":"新旧核心差异","system":true},{"value":"需求理解不一致","text":"需求理解不一致","system":true},{"value":"重复问题","text":"重复问题","system":true},{"value":"需求确认","text":"需求确认","system":true},{"value":"版本编译问题","text":"版本编译问题","system":true},{"value":"需求不明确","text":"需求不明确","system":true}]', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', NULL, 0);

-- 解决时间字段
INSERT INTO `custom_field` VALUES ('issue-resolved-time-field-2024-12-01-006', '解决时间', 'ISSUE', 'datetime', '', '[]', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', NULL, 0);

-- 关闭时间字段
INSERT INTO `custom_field` VALUES ('issue-closed-time-field-2024-12-01-007', '关闭时间', 'ISSUE', 'datetime', '', '[]', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', NULL, 0);

-- 需求号字段
INSERT INTO `custom_field` VALUES ('issue-requirement-number-field-2024-12-01-008', '需求号', 'ISSUE', 'input', '', '[]', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', NULL, 0);


-- 更新缺陷状态字段的options，添加所有8种状态
UPDATE custom_field
SET options = '[{"text":"test_track.issue.status_new","value":"new","system": true},{"text":"test_track.issue.status_accepted","value":"accepted","system": true},{"text":"test_track.issue.status_resolved","value":"resolved","system": true},{"text":"test_track.issue.status_closed","value":"closed","system": true},{"text":"test_track.issue.status_reopened","value":"reopened","system": true},{"text":"test_track.issue.status_invalid","value":"invalid","system": true},{"text":"test_track.issue.status_cancelled","value":"cancelled","system": true},{"text":"test_track.issue.status_on_hold","value":"on_hold","system": true}]',
    update_time = UNIX_TIMESTAMP() * 1000
WHERE id = 'beb57501-19c8-4ca3-8dfb-2cef7c0ea087' AND name = '状态' AND scene = 'ISSUE';

-- 模块级联字段
-- INSERT INTO `custom_field` VALUES (
--   'system-list-cascading-field-2024-12-01-001',
--   '模块',
--   'ISSUE',
--   'cascadingSelect',
--   '',
--   '[  {"text": "瑞众保险智能问答机器人平台","value": "AIQARP"},{"text": "瑞众保险批处理平台","value": "Batch"},{"text": "瑞众保险银保考勤系统","value": "Bit"},
--   {"text": "瑞众保险银保通系统","value": "BITS"},{"text": "瑞众保险销售管理系统","value": "CMS"},{"text": "瑞众保险数金销售管理系统","value": "CMS-Digital"},
--   {"text": "客户管理系统","value": "CRM","children": [{"text": "营销客户中心","value": "SCRM"},{"text": "营销客户分层","value": "ACRM"}]},{"text": "瑞众保险计划书系统","value": "IMRS"},{"text": "瑞众保险电子保单系统(后台应用)","value": "EDOC"},
--   {"text": "瑞众影像平台","value": "EIMP"},{"text": "瑞众保险数金直销官网系统","value": "EMall"},{"text": "瑞众保险电子支付系统","value": "EPS"},
--   {"text": "企业服务总线系统","value": "ESB"},{"text": "人脸识别系统","value": "FRS"},{"text": "瑞众保险团险弹性福利平台","value": "FWP","children": [{"text": "弹服平台-线上服务","value": "FWPSV"},{"text": "弹服管理端","value": "FWPAD"},{"text": "弹服平台-线上出单","value": "FWPUW"}]},
--   {"text": "瑞众保险团险核心业务系统","value": "GICS","children": [{"text": "团险核心-契约","value": "GICS-UW"},{"text": "团险核心-保全","value": "GICS-POS"},{"text": "团险核心-理赔","value": "GICS-CLM"},{"text": "团险核心-财务","value": "GICS-FIN"}]},{"text": "瑞众保险团险微信理赔系统","value": "GWClaim"},{"text": "瑞众保险手写识别系统","value": "HWRS"},
--   {"text": "瑞众保险个险核心业务系统","value": "ICBS","children": [{"text": "瑞众保险个险核心业务系统-新契约","value": "ICBS-UW"},{"text": "瑞众保险个险核心业务系统-综合查询","value": "ICBS-GQS"},{"text": "瑞众保险个险核心业务系统-续期","value": "ICBS-RN"},{"text": "瑞众保险个险核心业务系统-打印","value": "ICBS-PRT"},{"text": "瑞众保险个险核心业务系统-保全","value": "ICBS-POS"},{"text": "瑞众保险个险核心业务系统-清单","value": "ICBS-LS"},{"text": "瑞众保险个险核心业务系统-单证","value": "ICBS-VC"},{"text": "瑞众保险个险核心业务系统-接口","value": "ICBS-LINTF"},{"text": "瑞众保险个险核心业务系统-网关","value": "ICBS-GW"},{"text": "瑞众保险个险核心业务系统-用户管理","value": "ICBS-UM"},{"text": "瑞众保险个险核心业务系统-理赔","value": "ICBS-CLM"},{"text": "瑞众保险个险核心业务系统-收付","value": "ICBS-FIN"},{"text": "瑞众保险个险核心业务系统-影像","value": "ICBS-IM"},{"text": "瑞众保险个险核心系统-缓存","value": "ICBS-Cache"}]},{"text": "图像识别平台","value": "ICR"},{"text": "瑞众保险双录系统V2.0","value": "IDRS2.0"},
--   {"text": "瑞众保险微信增员系统","value": "WeStaffing"},{"text": "瑞众保险自主经营系统","value": "SMGS"},{"text": "瑞众保险资金管理系统","value": "IFMS"},
--   {"text": "各渠道风云榜系统","value": "KPCS"},{"text": "瑞众保险消息中心系统(后台应用)","value": "MC"},{"text": "MIS系统","value": "MIS"},
--   {"text": "移动管理驾驶舱系统","value": "MMCS"},{"text": "瑞众保险消息交换平台","value": "MQ"},{"text": "瑞众保险网电融合系统","value": "NTFS"},
--   {"text": "瑞众保险在线调查平台","value": "OSP"},{"text": "瑞众保险出单中心平台","value": "Phoenix"},{"text": "瑞众保险交易风控系统","value": "RCS"},
--   {"text": "瑞众保险用户行为回溯系统","value": "Review"},{"text": "小叮当魔法袋","value": "已下线"},{"text": "战略渠道招投标信息库系统","value": "6ef2798b5494489c"},
--   {"text": "规则管理平台","value": "RM"},{"text": "瑞众保险魔方系统","value": "RSCS"},{"text": "瑞众官网","value": "RuiInsurance"},
--   {"text": "瑞众保险行销平台","value": "RuiPortalP"},{"text": "中转小程序","value": "RuiVeiw"},{"text": "出单中心（后台应用）","value": "29978be7b1314ee3"},
--   {"text": "短信平台","value": "SMS"},{"text": "瑞众保险天池系统(后台应用)","value": "SPS"},{"text": "瑞众保险短险超市系统","value": "STIS"},
--   {"text": "瑞众保险营销培训管理系统(含考勤)","value": "TMS"},{"text": "瑞众保险电销系统","value": "TS"},{"text": "瑞众保险用户中心系统","value": "UC"},
--   {"text": "瑞众官微","value": "Wechat","children": [{"text": "官微-服务窗","value": "Wechat-SO"},{"text": "官微-瑞众家","value": "Wechat-MHRI"},{"text": "官微-产品街","value": "Wechat-PSRI"}]},{"text": "瑞众保险微信投保系统","value": "WeInsure"},{"text": "瑞众保险数金微信投保系统","value": "DigitalWeInsure"},
--   {"text": "瑞众保险悟空智能助理系统（已下线）","value": "WKAIQAS"},{"text": "瑞众保险微信理赔系统","value": "WxClaim"},{"text": "瑞众保险财务系统","value": "EBS"},
--   {"text": "瑞众保险签约平台","value": "Contract"},{"text": "NLP平台","value": "NLP"},{"text": "瑞众保险鉴权系统","value": "Validation"},
--   {"text": "瑞众保险智能理赔平台","value": "ICP"},{"text": "瑞众保险理赔自动理算系统","value": "CAS"},{"text": "交互式核保系统","value": "IU","children": [{"text": "交互式核保系统","value": "InteractingUndwrting"}]},
--   {"text": "预核保系统","value": "PU","children": [{"text": "预核保系统","value": "PreUnderwitring"}]},{"text": "核保专家助手系统","value": "UA","children": [{"text": "核保专家助手系统","value": "UnderwirtingAssistant"}]},{"text": "瑞众保险微信核保系统","value": "WCU","children": [{"text": "瑞众保险微信核保系统","value": "WCU"}]},
--   {"text": "瑞众保险双录系统V1.0","value": "IDRS1.0"},{"text": "瑞众保险团险微信投保系统","value": "GroupWeInsure"},{"text": "星环数据库平台","value": "ARGODB"},
--   {"text": "监管报送系统","value": "CIRC"},{"text": "监管标准化报送系统","value": "EAST"},{"text": "保单登记管理信息平台","value": "PRMIP"},
--   {"text": "官微智能客服机器人","value": "WISB"},{"text": "数字大屏管理系统","value": "DLSS"},{"text": "瑞众保险物控系统","value": "ASSET"},
--   {"text": "瑞众保险邮件系统","value": "Mail"},{"text": "域控","value": "f39e624f23fe443c"},{"text": "邮件网关","value": "e4811f3f9aba47d2"},
--   {"text": "RTX","value": "c42b90dd7b214a0c"},{"text": "专有云","value": "TCE"},{"text": "瑞众保险综合办公系统","value": "OA"},
--   {"text": "瑞众保险采购商城","value": "EEP"},{"text": "印章系统","value": "IES-H5"},{"text": "瑞众保险企业资产管理系统","value": "HEAM"},
--   {"text": "瑞众保险活动管家系统","value": "ASS"},{"text": "瑞众保险银行专区平台","value": "BZP"},{"text": "瑞众保险保单查询系统","value": "CIPQS"},
--   {"text": "瑞众保险人事信息系统","value": "EHR"},{"text": "瑞众保险电子信函系统","value": "ELetter"},{"text": "瑞众保险链客活动服务系统","value": "LASS"},
--   {"text": "银保专区后台管理系统","value": "726abb98896648e4"},{"text": "定制化展业夹系统","value": "49769ceede884a27"},{"text": "瑞众保险银保业绩查询系统","value": "PhoenixNest","children": [{"text": "已停服","value": "e00e1ecd289d43b4"}]},
--   {"text": "瑞众保险再保系统","value": "RS"},{"text": "瑞众保险预算分析系统","value": "FAS"},{"text": "投资全景系统","value": "2cb05f1c1e174795"},
--   {"text": "瑞众保险费控系统","value": "HEC"},{"text": "瑞众保险自动化运维平台","value": "AIOps"},{"text": "瑞众保险语音处理平台","value": "ISPP"},
--   {"text": "反洗钱系统","value": "AML"},{"text": "瑞众保险资料质检系统","value": "AQIS"},{"text": "95300智能助手","value": "Assistant"},
--   {"text": "瑞众保险中介协议管理系统","value": "BAMP"},{"text": "北京意外险信息平台","value": "BJAIP"},{"text": "北京健康险信息平台","value": "BJHIP"},
--   {"text": "瑞众保险中介云平台","value": "BCP"},{"text": "瑞众微门店系统","value": "BCard"},{"text": "瑞众保险理赔外包系统","value": "CBPOS"},
--   {"text": "瑞众保险投诉管理系统","value": "CCMS"},{"text": "用户统一管理平台","value": "UIAM"},{"text": "门户系统","value": "PORTAL"},
--   {"text": "瑞众保险官微客户体验分析系统","value": "CEWRW"},{"text": "瑞众保险数据核查平台","value": "DataRules"},{"text": "数智品控系统","value": "DIQC"},
--   {"text": "瑞众保险数据链路追踪系统","value": "DLTS"},{"text": "瑞众保险中介风险测评系统（博士测保）","value": "IRAS"},{"text": "数据交换平台","value": "DTP"},
--   {"text": "瑞众保险营销在线培训系统","value": "Edufenghuang"},{"text": "瑞众保险活动量管理平台","value": "EQMP"},{"text": "广东商业保险报送系统","value": "GDCI"},
--   {"text": "湖南意健险平台","value": "HNAHIP"},{"text": "广东省公安厅保险诈骗风险防控平台","value": "41e6c0eaaa624b6b"},{"text": "服务评价系统","value": "22576f6a81494d1f"},
--   {"text": "保单验真系统","value": "12ffeda2ad8e47dc"},{"text": "江苏中介系统","value": "4f1165bf36b8430b"},{"text": "河南消保报送平台","value": "HNCP"},
--   {"text": "瑞众保险宙斯盾系统","value": "IAegis"},{"text": "指标管理系统","value": "IDMS"},{"text": "颉票通验真系统","value": "IMS"},
--   {"text": "投资系统","value": "INVEST"},{"text": "瑞众回音壁系统","value": "IOMP"},{"text": "瑞众保险中介风险保额计算器系统","value": "IRCCS"},
--   {"text": "瑞众保险龙庭大学在线培训系统","value": "Itrain"},{"text": "瑞众保险中介悟空医生系统","value": "IWKDRS"},{"text": "知识库系统","value": "KB"},
--   {"text": "瑞众保险AI训练平台","value": "LEP"},{"text": "瑞众保险朋友圈助手系统","value": "MAS"},{"text": "医疗数据采集引擎系统","value": "MDP"},
--   {"text": "瑞众保险营销风险测评系统（小瑞保贝）","value": "MRAS"},{"text": "瑞众保险营销悟空医生系统","value": "MWKDR"},{"text": "老风控系统","value": "OLDRISK"},
--   {"text": "瑞众保险养老金计算器系统","value": "PCS"},{"text": "瑞众保险中介养老金计算器系统","value": "APCS"},{"text": "瑞众保险产品开发平台","value": "PDP"},
--   {"text": "95300智能质检","value": "QIC"},{"text": "瑞众保险续期催收管理平台","value": "RCMP"},{"text": "瑞众保险续收外呼管理系统","value": "RCMS"},
--   {"text": "准备金系统","value": "RES"},{"text": "呼叫中心系统","value": "CCS","children": [{"text": "自助语音交互服务系统","value": "IVR"},{"text": "人工作业平台","value": "RGZYPT"}]},{"text": "瑞众保险机器人流程自动化平台","value": "RPAP"},
--   {"text": "瑞众保险智慧营销管理平台","value": "SMMP"},{"text": "山东意外险报送平台","value": "SDAIP"},{"text": "瑞众数据平台","value": "SDP"},
--   {"text": "上海人身险报送平台","value": "SHPIP"},{"text": "瑞众保险税控系统","value": "SKSERVER"},{"text": "瑞众保险智能会客厅系统","value": "SmartMeeting"},
--   {"text": "核保加速度系统","value": "UWA"},{"text": "函件自动下发系统","value": "UWLS"},{"text": "瑞众保险增值税平台","value": "VMS"},
--   {"text": "瑞众保险悟空条款系统","value": "WKCAS"},{"text": "风控系统","value": "XRISK"},{"text": "空中柜面","value": "VCS"},
--   {"text": "众创云平台","value": "DSINV"},{"text": "理赔调查平台","value": "CLMSURVEY"},{"text": "数智消保平台","value": "XB"},
--   {"text": "数金服务号","value": "SJSA"},{"text": "中介AI快报系统","value": "ARFB"},{"text": "智能客服助手","value": "ICSA"},
--   {"text": "智能客服知识平台","value": "ICSKP"},{"text": "瑞博士基座平台","value": "AIP"},{"text": "瑞健康平台","value": "RH"},
--   {"text": "团险在线培训系统","value": "GIOTS"},{"text": "CRS报送平台","value": "CRS"},{"text": "数字人销售赋能系统","value": "DHSES"},
--   {"text": "瑞博士官网版","value": "DRFC"},{"text": "瑞博士公文系统","value": "DRFODS"},{"text": "综合监督管理平台","value": "ISMP"},
--   {"text": "团险销售支持系统","value": "GISS","children": [{"text": "团险销售支持系统业务员端","value": "GISS_EMP"},{"text": "团险销售支持系统内勤端","value": "GISS_UW"}]},{"text": "瑞博士（银保版）","value": "DRFBI"},{"text": "核心监控系统","value": "HMonitor"},
--   {"text": "瑞博士（内勤版）","value": "DR-INER"},{"text": "数字人视频创作平台(银保版)","value": "AIHSBI"},{"text": "公有云","value": "PC","children": [{"text": "京东云、阿里云、华为云、腾讯云","value": "JD Cloud、Alibaba Cloud、HuaWei Cloud、Tencent Cloud"}]},
--   {"text": "瑞博士管理系统","value": "DRMS"},{"text": "瑞博士（营销版）","value": "DRFMI"},{"text": "瑞博士智能陪练平台","value": "IT"},
--   {"text": "瑞博士-知识生产运营平台","value": "KGPOP"},{"text": "瑞众保险双录系统V3.0","value": "IDRS3.0"},{"text": "代码质量管理系统","value": "CMS-DMZJ"},
--   {"text": "工作流管理系统","value": "WSS"},{"text": "营销BLP主题增员活动系统","value": "BRA"},{"text": "瑞博士（中介版）","value": "DRFA"},
--   {"text": "计划书配置化V２.０","value": "IMRConfig2.0"},{"text": "瑞众保险全流程智能营销平台","value": "LASS（新）"},{"text": "营销客户分层（掘金宝典）","value": "ACRM"},
--   {"text": "瑞众保险研发全流程管理平台","value": "RDMS"}]',
--   1,
--   1,
--   UNIX_TIMESTAMP() * 1000,
--   UNIX_TIMESTAMP() * 1000,
--   'admin',
--   NULL,
--   0
-- );



 -- ============================================
 -- 步骤2：将新增字段绑定到缺陷管理-Local平台全局默认模板
 -- 仅绑定到 template_id = 5d7c87d2-f405-4ec1-9a3d-71b514cdfda3（global=1, platform=Local）
 -- 幂等处理：使用 NOT EXISTS 防止重复执行迁移时插入重复关联
 -- ============================================
 -- 优先级
 INSERT INTO custom_field_template (id, field_id, template_id, scene, required, `order`, default_value, custom_data, `key`) SELECT UUID(), 'issue-priority-field-2024-12-01-001', '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3', 'ISSUE', 0, 5, '', NULL, NULL WHERE NOT EXISTS (SELECT 1 FROM custom_field_template WHERE field_id = 'issue-priority-field-2024-12-01-001' AND template_id = '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3');
 -- 严重级别
 INSERT INTO custom_field_template (id, field_id, template_id, scene, required, `order`, default_value, custom_data, `key`) SELECT UUID(), 'issue-severity-level-field-2024-12-01-002', '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3', 'ISSUE', 0, 6, '', NULL, NULL WHERE NOT EXISTS (SELECT 1 FROM custom_field_template WHERE field_id = 'issue-severity-level-field-2024-12-01-002' AND template_id = '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3');
 -- 缺陷产生原因
 INSERT INTO custom_field_template (id, field_id, template_id, scene, required, `order`, default_value, custom_data, `key`) SELECT UUID(), 'issue-cause-field-2024-12-01-003', '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3', 'ISSUE', 0, 7, '', NULL, NULL WHERE NOT EXISTS (SELECT 1 FROM custom_field_template WHERE field_id = 'issue-cause-field-2024-12-01-003' AND template_id = '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3');
 -- 解决时间
 INSERT INTO custom_field_template (id, field_id, template_id, scene, required, `order`, default_value, custom_data, `key`) SELECT UUID(), 'issue-resolved-time-field-2024-12-01-006', '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3', 'ISSUE', 0, 8, '', NULL, NULL WHERE NOT EXISTS (SELECT 1 FROM custom_field_template WHERE field_id = 'issue-resolved-time-field-2024-12-01-006' AND template_id = '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3');
 -- 关闭时间
 INSERT INTO custom_field_template (id, field_id, template_id, scene, required, `order`, default_value, custom_data, `key`) SELECT UUID(), 'issue-closed-time-field-2024-12-01-007', '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3', 'ISSUE', 0, 9, '', NULL, NULL WHERE NOT EXISTS (SELECT 1 FROM custom_field_template WHERE field_id = 'issue-closed-time-field-2024-12-01-007' AND template_id = '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3');
 -- 需求号
 INSERT INTO custom_field_template (id, field_id, template_id, scene, required, `order`, default_value, custom_data, `key`) SELECT UUID(), 'issue-requirement-number-field-2024-12-01-008', '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3', 'ISSUE', 0, 10, '', NULL, NULL WHERE NOT EXISTS (SELECT 1 FROM custom_field_template WHERE field_id = 'issue-requirement-number-field-2024-12-01-008' AND template_id = '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3');
 -- 模块（级联）
--  INSERT INTO custom_field_template (id, field_id, template_id, scene, required, `order`, default_value, custom_data, `key`) SELECT UUID(), 'system-list-cascading-field-2024-12-01-001', '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3', 'ISSUE', 0, 11, '', NULL, NULL WHERE NOT EXISTS (SELECT 1 FROM custom_field_template WHERE field_id = 'system-list-cascading-field-2024-12-01-001' AND template_id = '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3');


SET SESSION innodb_lock_wait_timeout = DEFAULT;
