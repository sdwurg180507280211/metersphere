-- ==========================================
-- 银保通项目状态字段重复问题修复脚本
-- ==========================================
-- 问题：银保通项目的缺陷存在两个状态字段，导致状态流转异常
-- 原因：项目创建了专属的状态字段（d755edd6-7fc6-4a4c-9de9-977befa4738c），
--       但模板绑定的是全局字段（beb57501-19c8-4ca3-8dfb-2cef7c0ea087）
-- 解决：删除项目专属字段，将数据迁移到全局字段，同步 issues 表的 status 字段
-- ==========================================

-- 1. 备份数据（可选）
CREATE TABLE IF NOT EXISTS custom_field_issues_backup_20250126 AS
SELECT * FROM custom_field_issues
WHERE field_id = 'd755edd6-7fc6-4a4c-9de9-977befa4738c';

-- 2. 将项目字段的值更新到全局字段
UPDATE custom_field_issues cfi
JOIN (
  SELECT resource_id, value
  FROM custom_field_issues
  WHERE field_id = 'd755edd6-7fc6-4a4c-9de9-977befa4738c'
) AS project_field ON cfi.resource_id = project_field.resource_id
SET cfi.value = project_field.value
WHERE cfi.field_id = 'beb57501-19c8-4ca3-8dfb-2cef7c0ea087';

-- 3. 同步更新 issues 表的 status 字段
UPDATE issues i
JOIN custom_field_issues cfi ON i.id = cfi.resource_id
SET i.status = TRIM(BOTH '"' FROM cfi.value)
WHERE cfi.field_id = 'beb57501-19c8-4ca3-8dfb-2cef7c0ea087'
  AND i.project_id = '84d9cda2-0cd0-4e86-a34f-45f062f580cc';

-- 4. 删除项目专属字段的数据
DELETE FROM custom_field_issues
WHERE field_id = 'd755edd6-7fc6-4a4c-9de9-977befa4738c';

-- 5. 删除项目专属字段的模板关联（如果存在）
DELETE FROM custom_field_template
WHERE field_id = 'd755edd6-7fc6-4a4c-9de9-977befa4738c';

-- 6. 删除项目专属字段定义
DELETE FROM custom_field
WHERE id = 'd755edd6-7fc6-4a4c-9de9-977befa4738c';

-- ==========================================
-- 验证脚本
-- ==========================================

-- 验证1：检查银保通项目的缺陷是否只有一个状态字段
SELECT 
  i.id,
  i.title,
  i.status AS issue_status,
  COUNT(cfi.id) AS status_field_count,
  GROUP_CONCAT(cfi.field_id) AS field_ids,
  GROUP_CONCAT(cfi.value) AS field_values
FROM issues i
LEFT JOIN custom_field_issues cfi ON i.id = cfi.resource_id
LEFT JOIN custom_field cf ON cfi.field_id = cf.id AND cf.name = '状态'
WHERE i.project_id = '84d9cda2-0cd0-4e86-a34f-45f062f580cc'
GROUP BY i.id, i.title, i.status;

-- 验证2：检查 issues 表的 status 字段是否与自定义字段一致
SELECT 
  i.id,
  i.title,
  i.status AS issue_status,
  TRIM(BOTH '"' FROM cfi.value) AS custom_field_status,
  CASE 
    WHEN i.status = TRIM(BOTH '"' FROM cfi.value) THEN '一致'
    ELSE '不一致'
  END AS status_check
FROM issues i
JOIN custom_field_issues cfi ON i.id = cfi.resource_id
WHERE i.project_id = '84d9cda2-0cd0-4e86-a34f-45f062f580cc'
  AND cfi.field_id = 'beb57501-19c8-4ca3-8dfb-2cef7c0ea087';

-- 验证3：检查项目专属字段是否已删除
SELECT 
  id,
  name,
  project_id,
  global,
  system
FROM custom_field
WHERE id = 'd755edd6-7fc6-4a4c-9de9-977befa4738c';
-- 应该返回空结果
