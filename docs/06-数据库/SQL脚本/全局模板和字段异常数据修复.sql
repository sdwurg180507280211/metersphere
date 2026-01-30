-- ==========================================
-- 全局模板和字段异常数据修复脚本
-- ==========================================
-- 问题1：5个项目的模板不存在
-- 问题2：4个项目有13个项目系统字段（与全局系统字段重复）
-- 解决：修复模板引用 + 删除项目系统字段（保留项目模板和项目自定义字段）
-- ==========================================
-- 
-- 重要说明：
-- 1. 项目模板是有用的（测试项目和默认项目在使用）
-- 2. 项目模板绑定的都是全局字段（符合最佳实践）
-- 3. 项目系统字段是异常数据（与全局系统字段重复，需要删除）
-- 4. 项目自定义字段是有用的（默认项目有2个字段，共404条数据）
-- ==========================================

-- ==========================================
-- 第一部分：修复模板不存在的项目
-- ==========================================

-- 1. 将模板不存在的项目改为使用全局默认模板
UPDATE project 
SET issue_template_id = '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3'
WHERE issue_template_id IN (
  '034d3d09-8f8f-4e04-855a-c3b56d492b57',  -- lijx_测试项目
  '9981a730-b95a-4c5c-b0c7-f09974fce21e',  -- test_wang
  'fe6f1ff9-2b10-4cd6-86ae-dee10d5226c1',  -- 测试-3
  'bd71a07a-fd67-4d68-a776-85d97419c35f',  -- 银保通项目
  '823f0670-d32b-40ac-9da6-4025f3daf10f'   -- 默认项目
);

SELECT '✓ 已修复5个项目的模板引用' AS message, ROW_COUNT() AS affected_rows;

-- ==========================================
-- 第二部分：修复项目系统字段（测试项目）
-- ==========================================

-- 2.1 备份测试项目的项目系统字段数据
CREATE TABLE IF NOT EXISTS custom_field_issues_backup_test_project_20250126 AS
SELECT cfi.* 
FROM custom_field_issues cfi
WHERE cfi.field_id IN (
  'eb8cb3f5-bcc0-4f8c-8d80-92a88aeee0f9',  -- 处理人
  '6bdf7d05-fc2c-4936-a3e8-0b9859c24915',  -- 状态
  '13181e71-5cda-4231-ba74-157b90b763b9',  -- 缺陷所属系统
  'a0882c52-dd33-4446-b1cc-e53b03a6fcc1'   -- 需求号
);

-- 2.2 将项目字段的数据迁移到对应的全局字段
-- 处理人：eb8cb3f5... -> a577bc60...
UPDATE custom_field_issues cfi
JOIN (
  SELECT resource_id, value
  FROM custom_field_issues
  WHERE field_id = 'eb8cb3f5-bcc0-4f8c-8d80-92a88aeee0f9'
) AS pf ON cfi.resource_id = pf.resource_id
SET cfi.value = pf.value
WHERE cfi.field_id = 'a577bc60-75fe-47ec-8aa6-32dca23bf3d6';

-- 状态：6bdf7d05... -> beb57501...
UPDATE custom_field_issues cfi
JOIN (
  SELECT resource_id, value
  FROM custom_field_issues
  WHERE field_id = '6bdf7d05-fc2c-4936-a3e8-0b9859c24915'
) AS pf ON cfi.resource_id = pf.resource_id
SET cfi.value = pf.value
WHERE cfi.field_id = 'beb57501-19c8-4ca3-8dfb-2cef7c0ea087';

-- 缺陷所属系统：13181e71... -> issue-associated-system-field-2024-12-16-001
UPDATE custom_field_issues cfi
JOIN (
  SELECT resource_id, value
  FROM custom_field_issues
  WHERE field_id = '13181e71-5cda-4231-ba74-157b90b763b9'
) AS pf ON cfi.resource_id = pf.resource_id
SET cfi.value = pf.value
WHERE cfi.field_id = 'issue-associated-system-field-2024-12-16-001';

-- 需求号：a0882c52... -> a516dfcc...
UPDATE custom_field_issues cfi
JOIN (
  SELECT resource_id, value
  FROM custom_field_issues
  WHERE field_id = 'a0882c52-dd33-4446-b1cc-e53b03a6fcc1'
) AS pf ON cfi.resource_id = pf.resource_id
SET cfi.value = pf.value
WHERE cfi.field_id = 'a516dfcc-d19c-11f0-a2f8-cead5f5242ae';

-- 2.3 同步 issues 表的 status 字段
UPDATE issues i
JOIN custom_field_issues cfi ON i.id = cfi.resource_id
SET i.status = TRIM(BOTH '"' FROM cfi.value)
WHERE cfi.field_id = 'beb57501-19c8-4ca3-8dfb-2cef7c0ea087'
  AND i.project_id = '5d75b4c6-a431-4743-b0b3-1876e36f2905';

-- 2.4 删除测试项目的项目系统字段数据
DELETE FROM custom_field_issues
WHERE field_id IN (
  'eb8cb3f5-bcc0-4f8c-8d80-92a88aeee0f9',
  '6bdf7d05-fc2c-4936-a3e8-0b9859c24915',
  '13181e71-5cda-4231-ba74-157b90b763b9',
  'a0882c52-dd33-4446-b1cc-e53b03a6fcc1'
);

-- 2.5 删除测试项目的项目系统字段定义
DELETE FROM custom_field
WHERE id IN (
  'eb8cb3f5-bcc0-4f8c-8d80-92a88aeee0f9',
  '6bdf7d05-fc2c-4936-a3e8-0b9859c24915',
  '13181e71-5cda-4231-ba74-157b90b763b9',
  'a0882c52-dd33-4446-b1cc-e53b03a6fcc1'
);

-- 2.6 测试项目保持使用项目模板（不需要修改）
-- UPDATE project 
-- SET issue_template_id = '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3'
-- WHERE id = '5d75b4c6-a431-4743-b0b3-1876e36f2905';

SELECT '✓ 已修复测试项目的字段' AS message;

-- ==========================================
-- 第三部分：修复项目系统字段（默认项目）
-- ==========================================

-- 3.1 备份默认项目的项目系统字段数据
CREATE TABLE IF NOT EXISTS custom_field_issues_backup_default_project_20250126 AS
SELECT cfi.* 
FROM custom_field_issues cfi
WHERE cfi.field_id = 'cda9d416-1471-4ec3-af05-a260bc3896e3';  -- 状态

-- 3.2 将项目字段的数据迁移到全局字段
-- 状态：cda9d416... -> beb57501...
UPDATE custom_field_issues cfi
JOIN (
  SELECT resource_id, value
  FROM custom_field_issues
  WHERE field_id = 'cda9d416-1471-4ec3-af05-a260bc3896e3'
) AS pf ON cfi.resource_id = pf.resource_id
SET cfi.value = pf.value
WHERE cfi.field_id = 'beb57501-19c8-4ca3-8dfb-2cef7c0ea087';

-- 3.3 同步 issues 表的 status 字段
UPDATE issues i
JOIN custom_field_issues cfi ON i.id = cfi.resource_id
SET i.status = TRIM(BOTH '"' FROM cfi.value)
WHERE cfi.field_id = 'beb57501-19c8-4ca3-8dfb-2cef7c0ea087'
  AND i.project_id = '698ab521-d1a4-11f0-a2f8-cead5f5242ae';

-- 3.4 删除默认项目的项目系统字段数据
DELETE FROM custom_field_issues
WHERE field_id = 'cda9d416-1471-4ec3-af05-a260bc3896e3';

-- 3.5 删除默认项目的项目系统字段定义
DELETE FROM custom_field
WHERE id = 'cda9d416-1471-4ec3-af05-a260bc3896e3';

SELECT '✓ 已修复默认项目的字段' AS message;

-- ==========================================
-- 第四部分：修复项目系统字段（测试-3）
-- ==========================================

-- 4.1 删除测试-3的项目系统字段（无数据，直接删除）
DELETE FROM custom_field
WHERE id IN (
  'a04a6d7c-07d9-41e2-8899-5d541ce0abe7',  -- 处理人
  '22c66cac-f248-4ee8-888b-b2c3f8822528',  -- 状态
  '06103015-692e-4642-9e4a-5a6954c09d28',  -- 缺陷所属系统
  '1824e161-69cc-4bc0-9ca8-561af113657d'   -- 需求号
);

SELECT '✓ 已删除测试-3的项目系统字段' AS message;

-- ==========================================
-- 第五部分：修复项目系统字段（lijx_测试项目）
-- ==========================================

-- 5.1 删除lijx_测试项目的项目系统字段（无数据，直接删除）
DELETE FROM custom_field
WHERE id IN (
  'e5aebb45-d592-41e7-8571-e064c5e5be2b',  -- 处理人
  '076943db-ff08-45e1-8854-305f267ac720',  -- 状态
  '7a783f95-0b1a-4498-aee5-fd6f39f2e9be',  -- 缺陷所属系统
  '75e0bf8a-7459-47cb-815d-69386c6af202'   -- 需求号
);

SELECT '✓ 已删除lijx_测试项目的项目系统字段' AS message;

-- ==========================================
-- 第六部分：验证修复结果
-- ==========================================

-- ==========================================
-- 第六部分：验证修复结果
-- ==========================================

-- 6.1 验证：模板不存在的项目已修复
SELECT 
  p.id,
  p.name AS project_name,
  p.issue_template_id,
  it.name AS template_name,
  it.`global` AS template_global,
  CASE 
    WHEN it.id IS NULL THEN '❌ 模板不存在'
    WHEN it.`global` = 1 THEN '✓ 使用全局模板'
    WHEN it.`global` = 0 THEN '✓ 使用项目模板'
    ELSE '❌ 异常'
  END AS status
FROM project p
LEFT JOIN issue_template it ON p.issue_template_id = it.id
ORDER BY status, p.name;

-- 6.2 验证：不存在项目系统字段
SELECT 
  COUNT(*) AS project_system_field_count,
  CASE 
    WHEN COUNT(*) = 0 THEN '✓ 无项目系统字段'
    ELSE '❌ 仍存在项目系统字段'
  END AS status
FROM custom_field
WHERE `global` = 0 AND `system` = 1 AND scene = 'ISSUE';

-- 6.3 验证：项目模板和项目自定义字段保留
SELECT 
  '项目模板' AS item_type,
  COUNT(*) AS count,
  CASE 
    WHEN COUNT(*) > 0 THEN '✓ 项目模板已保留'
    ELSE '⚠ 无项目模板'
  END AS status
FROM issue_template
WHERE `global` = 0
UNION ALL
SELECT 
  '项目自定义字段' AS item_type,
  COUNT(*) AS count,
  CASE 
    WHEN COUNT(*) > 0 THEN '✓ 项目自定义字段已保留'
    ELSE '⚠ 无项目自定义字段'
  END AS status
FROM custom_field
WHERE `global` = 0 AND `system` = 0 AND scene = 'ISSUE';

-- 6.4 验证：所有缺陷的字段数据完整性
SELECT 
  p.name AS project_name,
  COUNT(DISTINCT i.id) AS issue_count,
  COUNT(DISTINCT CASE WHEN cfi_status.value IS NOT NULL THEN i.id END) AS has_status_count,
  COUNT(DISTINCT CASE WHEN cfi_handler.value IS NOT NULL THEN i.id END) AS has_handler_count,
  CASE 
    WHEN COUNT(DISTINCT i.id) = COUNT(DISTINCT CASE WHEN cfi_status.value IS NOT NULL THEN i.id END) THEN '✓ 状态字段完整'
    ELSE '❌ 状态字段缺失'
  END AS status_integrity
FROM project p
LEFT JOIN issues i ON p.id = i.project_id
LEFT JOIN custom_field_issues cfi_status ON i.id = cfi_status.resource_id 
  AND cfi_status.field_id = 'beb57501-19c8-4ca3-8dfb-2cef7c0ea087'  -- 全局状态字段
LEFT JOIN custom_field_issues cfi_handler ON i.id = cfi_handler.resource_id 
  AND cfi_handler.field_id = 'a577bc60-75fe-47ec-8aa6-32dca23bf3d6'  -- 全局处理人字段
GROUP BY p.name
HAVING issue_count > 0
ORDER BY p.name;

SELECT '✓ 修复完成！' AS message;
