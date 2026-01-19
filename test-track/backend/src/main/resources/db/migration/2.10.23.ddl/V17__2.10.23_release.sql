SET SESSION innodb_lock_wait_timeout = 7200;

-- ============================================
-- 用例模板1、2、3 完整初始化脚本
-- 按照逻辑顺序组织：表结构修改 -> 模板插入（包含全局系统模板设置和Excel文件关联） -> 字段插入 -> 关联关系 -> 字段修复
-- ============================================

-- ============================================
-- 步骤1：修改表结构
-- ============================================
-- 为用例模板表添加Excel模板文件字段, 用于关联静态Excel模板文件
ALTER TABLE `test_case_template` ADD COLUMN `excel_template_file` varchar(255) DEFAULT NULL COMMENT 'Excel template file name' AFTER `project_id`;

-- ============================================
-- 步骤2：插入用例模板数据
-- ============================================
-- 用例模板1（设置为全局系统模板，关联Excel模板文件）
INSERT INTO `test_case_template` VALUES ('57b8c3a2-8217-46c4-8375-e50458d5ac0e', '用例模板1', 'functional', '', '', 1, 1, '', '', '', '', 1764140447288, 1764143584672, 'TEXT', '[{\"num\":1,\"desc\":\"\",\"result\":\"\"}]', 'admin', NULL, 'testcase-template-1.xlsx');

-- 用例模板2（设置为全局系统模板，关联Excel模板文件）
INSERT INTO `test_case_template` VALUES ('3619d893-9560-4454-8b3c-aa20cbb5e222', '用例模板2', 'functional', '', '', 1, 1, '', '', '', '', 1764145376004, 1764145547728, 'TEXT', '[{\"num\":1,\"desc\":\"\",\"result\":\"\"}]', 'admin', NULL, 'testcase-template-2.xlsx');

-- 用例模板3（设置为全局系统模板，关联Excel模板文件）
INSERT INTO `test_case_template` VALUES ('02e89340-22e0-4cd3-9402-a79c1e22f088', '用例模板3', 'functional', '', '', 1, 1, '', '', '', '', 1764652703781, 1764753699000, 'TEXT', '[{\"num\":1,\"desc\":\"\",\"result\":\"\"}]', 'admin', NULL, 'testcase-template-3.xlsx');

-- ============================================
-- 步骤3：插入自定义字段数据（设置为系统字段：system=1, global=1, project_id=NULL）
-- ============================================
-- 用例模板1的字段
INSERT INTO `custom_field` VALUES ('004d5d04-8db0-4a72-9b12-4e0dc24401e5', '用例编写人', 'TEST_CASE', 'member', '用例模板1', '[]', 1, 1, 1764143652241, 1764145127237, 'admin', NULL, 0);
INSERT INTO `custom_field` VALUES ('06aaedea-d828-4b87-8e36-5ed32b8b0df0', '是否通过', 'TEST_CASE', 'select', '用例模板1', '[{\"text\":\"否\",\"value\":\"cd57ddec\"},{\"text\":\"是\",\"value\":\"0efe1b03\"}]', 1, 1, 1764141088639, 1764145151764, 'admin', NULL, 0);
INSERT INTO `custom_field` VALUES ('5e5781b1-7319-4626-87b5-3c3ede952fbb', '复测是否通过', 'TEST_CASE', 'select', '用例模板1', '[{\"text\":\"否\",\"value\":\"b66be595\"},{\"text\":\"是\",\"value\":\"79202fa8\"}]', 1, 1, 1764143510516, 1764145134533, 'admin', NULL, 0);
INSERT INTO `custom_field` VALUES ('22c2c157-c922-4664-a6cd-868979076f01', '备注', 'TEST_CASE', 'input', '', '[]', 1, 1, 1764646024850, 1764842194000, 'admin', NULL, 0);
INSERT INTO `custom_field` VALUES ('7c7ef9bb-0bf0-4d70-b991-82328be55db0', '测试数据', 'TEST_CASE', 'input', '用例模板1', '[]', 1, 1, 1764645913169, 1764842194000, 'admin', NULL, 0);
INSERT INTO `custom_field` VALUES ('704c8470-118b-4ca4-af2c-65532b1c375b', '执行结果', 'TEST_CASE', 'input', '用例模板1', '[]', 1, 1, 1764645896574, 1764842194000, 'admin', NULL, 0);
INSERT INTO `custom_field` VALUES ('eaba2b94-33cb-4f28-a797-8b59b4abc3ef', '复测数据', 'TEST_CASE', 'input', '用例模板1', '[]', 1, 1, 1764652083309, 1764842194000, 'admin', NULL, 0);

-- 用例模板2的字段
INSERT INTO `custom_field` VALUES ('55cfe8dc-379a-4467-bc20-f6f15a608fb0', '设计人', 'TEST_CASE', 'member', '用例模板2', '[]', 1, 1, 1764145105697, 1764145164754, 'admin', NULL, 0);
INSERT INTO `custom_field` VALUES ('cefe53a0-649f-4764-b7b6-50d4e2fe8cd9', '用例结论', 'TEST_CASE', 'textarea', '用例模板2', '[]', 1, 1, 1764213600062, 1764901394000, 'admin', NULL, 0);

-- 用例模板3的字段
INSERT INTO `custom_field` VALUES ('528f6c90-edd4-46aa-ae29-f1f9279f3381', '执行方式', 'TEST_CASE', 'select', '用例模版3', '[{\"text\":\"手工\",\"value\":\"c87920ea\"}]', 1, 1, 1764653208469, 1764653208469, 'admin', NULL, 0);
INSERT INTO `custom_field` VALUES ('c64ef326-5b8e-4c32-9ca2-dc8421b77da2', '输入数据', 'TEST_CASE', 'richText', '用例模版3', '[]', 1, 1, 1764653132455, 1764653132455, 'admin', NULL, 0);
INSERT INTO `custom_field` VALUES ('e88cd73f-d318-4474-adc0-9556ce3b6904', '执行时间', 'TEST_CASE', 'datetime', '用例模版3', '[]', 1, 1, 1764653224995, 1764653224995, 'admin', NULL, 0);

-- ============================================
-- 步骤4：插入模板字段关联关系
-- ============================================
-- 用例模板1的字段关联关系
INSERT INTO `custom_field_template` VALUES ('3b363381-c8a2-4125-a4e0-3f2330c699f3', '06aaedea-d828-4b87-8e36-5ed32b8b0df0', '57b8c3a2-8217-46c4-8375-e50458d5ac0e', 'TEST_CASE', 0, 20000, '\"\"', NULL, 'C');
INSERT INTO `custom_field_template` VALUES ('8e98e98b-48f0-4497-8681-5efd41598ed6', '5e5781b1-7319-4626-87b5-3c3ede952fbb', '57b8c3a2-8217-46c4-8375-e50458d5ac0e', 'TEST_CASE', 0, 25000, '\"\"', NULL, 'D');
INSERT INTO `custom_field_template` VALUES ('917987d2-3fc8-4b53-a9b0-b62cd72604a0', '004d5d04-8db0-4a72-9b12-4e0dc24401e5', '57b8c3a2-8217-46c4-8375-e50458d5ac0e', 'TEST_CASE', 0, 45000, 'null', NULL, 'H');
INSERT INTO `custom_field_template` VALUES ('9cb3dca4-bc4c-4978-901f-40ece6897c18', '22c2c157-c922-4664-a6cd-868979076f01', '57b8c3a2-8217-46c4-8375-e50458d5ac0e', 'TEST_CASE', 0, 30000, '""', NULL, 'E');
INSERT INTO `custom_field_template` VALUES ('6ca5c78c-1399-48b9-9759-b09210f6f9dc', '7c7ef9bb-0bf0-4d70-b991-82328be55db0', '57b8c3a2-8217-46c4-8375-e50458d5ac0e', 'TEST_CASE', 0, 35000, 'null', NULL, 'F');
INSERT INTO `custom_field_template` VALUES ('a6b765fc-f3c7-4663-aba8-8b15ede2a8d8', '704c8470-118b-4ca4-af2c-65532b1c375b', '57b8c3a2-8217-46c4-8375-e50458d5ac0e', 'TEST_CASE', 0, 40000, 'null', NULL, 'G');
INSERT INTO `custom_field_template` VALUES ('c1ebaffa-e9bf-45d1-875f-b195dbcafa0f', 'eaba2b94-33cb-4f28-a797-8b59b4abc3ef', '57b8c3a2-8217-46c4-8375-e50458d5ac0e', 'TEST_CASE', 0, 50000, NULL, NULL, 'I');

-- 用例模板2的字段关联关系
INSERT INTO `custom_field_template` VALUES ('742856e6-5460-47e7-bc3a-2cd5f750318e', '55cfe8dc-379a-4467-bc20-f6f15a608fb0', '3619d893-9560-4454-8b3c-aa20cbb5e222', 'TEST_CASE', 0, 20000, 'null', NULL, 'C');
INSERT INTO `custom_field_template` VALUES ('5a6ffea7-d181-11f0-a2f8-cead5f5242ae', 'cefe53a0-649f-4764-b7b6-50d4e2fe8cd9', '3619d893-9560-4454-8b3c-aa20cbb5e222', 'TEST_CASE', 0, 20000, NULL, NULL, 'C');

-- 用例模板3的字段关联关系
INSERT INTO `custom_field_template` VALUES ('b10d2465-d71f-4bb1-999d-1362d5449780', '528f6c90-edd4-46aa-ae29-f1f9279f3381', '02e89340-22e0-4cd3-9402-a79c1e22f088', 'TEST_CASE', 0, 25000, '\"\"', NULL, 'D');
INSERT INTO `custom_field_template` VALUES ('1969e989-98ac-4667-8278-c836b6a48de6', 'c64ef326-5b8e-4c32-9ca2-dc8421b77da2', '02e89340-22e0-4cd3-9402-a79c1e22f088', 'TEST_CASE', 0, 30000, '\"\"', NULL, 'E');
INSERT INTO `custom_field_template` VALUES ('f2cc9f94-1b46-461e-acf0-0f201f6e173d', 'e88cd73f-d318-4474-adc0-9556ce3b6904', '02e89340-22e0-4cd3-9402-a79c1e22f088', 'TEST_CASE', 0, 35000, '\"\"', NULL, 'F');
INSERT INTO `custom_field_template` VALUES ('627d6f77-d19b-11f0-a2f8-cead5f5242ae', '22c2c157-c922-4664-a6cd-868979076f01', '02e89340-22e0-4cd3-9402-a79c1e22f088', 'TEST_CASE', 0, 40000, '""', NULL, 'G');

SET SESSION innodb_lock_wait_timeout = DEFAULT;



