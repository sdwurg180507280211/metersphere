SET SESSION innodb_lock_wait_timeout = 7200;

-- ============================================
-- 步骤1：插入缺陷系统字段“复测次数”
-- ============================================
-- 目的：创建全局系统字段，供所有项目模板使用
-- 场景：ISSUE（缺陷），类型：int（整数），system=1（系统字段），global=1（全局字段），project_id=NULL（不归属具体项目）
-- 风险控制：WHERE NOT EXISTS 确保不会重复插入同名同场景的系统字段
INSERT INTO custom_field (
    id,
    name,
    scene,
    type,
    remark,
    options,
    `system`,
    `global`,
    create_time,
    update_time,
    create_user,
    project_id,
    third_part
)
SELECT
    '9f7a4f4f-2b11-4df6-8e4b-6d3c3d2a6b4a',  -- 固定UUID，确保可预测
    '复测次数',                           -- 字段名称（与 SystemCustomField.ISSUE_RETEST_COUNT 保持一致）
    'ISSUE',                             -- 场景：缺陷（ISSUE）/用例（TEST_CASE）/接口（API）
    'int',                               -- 字段类型：整数，用于计数
    '',                                  -- 备注说明
    '[]',                                -- 选项JSON（int 类型无选项）
    1,                                   -- system=1（系统字段，区别于用户自定义字段）
    1,                                   -- global=1（全局字段，所有项目可见）
    unix_timestamp() * 1000,            -- 创建时间（毫秒时间戳）
    unix_timestamp() * 1000,            -- 更新时间（毫秒时间戳）
    'admin',                             -- 创建人
    NULL,                                -- project_id=NULL（全局字段不归属具体项目）
    0                                    -- third_part=0（非第三方平台字段）
    WHERE NOT EXISTS (
    SELECT 1 FROM custom_field
    WHERE name = '复测次数' AND scene = 'ISSUE' AND `system` = 1 AND `global` = 1
);

-- ============================================
-- 步骤2：关联“复测次数”字段到全局 Local 模板
-- ============================================
-- 目的：让全局 Local 平台默认缺陷模板包含“复测次数”字段
-- 模板ID：5d7c87d2-f405-4ec1-9a3d-71b514cdfda3（全局 Local 缺陷模板）
-- 风险控制：WHERE NOT EXISTS 确保不会重复关联同一字段到同一模板
INSERT INTO custom_field_template (id, field_id, template_id, scene, required, `order`, default_value)
SELECT uuid(),
       (SELECT id FROM custom_field WHERE name = '复测次数' AND scene = 'ISSUE' AND `system` = 1 AND global = 1 LIMIT 1),
    '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3',  -- 全局 Local 缺陷模板ID（固定）
    'ISSUE', 0, 2, '0'                     -- scene=ISSUE, required=0（非必填）, order=2（展示顺序）, default_value='0'
WHERE NOT EXISTS (
    SELECT 1
    FROM custom_field_template cft
    WHERE cft.template_id = '5d7c87d2-f405-4ec1-9a3d-71b514cdfda3'
  AND cft.field_id = (SELECT id FROM custom_field WHERE name = '复测次数' AND scene = 'ISSUE' AND `system` = 1 AND global = 1 LIMIT 1)
    );

-- ============================================
-- 步骤3：批量关联“复测次数”到所有项目级缺陷模板（补漏）
-- ============================================
-- 目的：确保所有项目级别的缺陷模板都能看到“复测次数”字段（即使模板创建时没有该字段）
-- 范围：issue_template.global=0（仅项目级模板，不包括全局模板）
-- 风险控制：WHERE NOT EXISTS 确保不会重复关联；LIMIT 1 确保取到唯一的全局字段ID
INSERT INTO custom_field_template (id, field_id, template_id, scene, required, `order`, default_value)
SELECT
    uuid() as id,
    (SELECT id FROM custom_field WHERE name = '复测次数' AND scene = 'ISSUE' AND `system` = 1 AND global = 1 LIMIT 1) as field_id,
    it.id as template_id,
    'ISSUE' as scene,
    0 as required,                        -- 非必填
    2 as `order`,                         -- 展示顺序（可根据需要调整）
    '0' as default_value                  -- 默认值为 0
FROM issue_template it
WHERE it.global = 0                        -- 只处理项目级别的模板（不包括全局模板）
  AND NOT EXISTS (                        -- 幂等：如果已关联则跳过
    SELECT 1
    FROM custom_field_template cft
    WHERE cft.template_id = it.id
  AND cft.field_id = (SELECT id FROM custom_field WHERE name = '复测次数' AND scene = 'ISSUE' AND `system` = 1 AND global = 1 LIMIT 1)
    );




-- ============================================
-- 更新所有模板中"复测次数"字段的默认值为 0
-- ============================================
-- 目的：将全局 Local 模板和所有项目级缺陷模板中的"复测次数"字段默认值从 1 改为 0
-- 范围：custom_field_template 表中所有关联到"复测次数"字段的记录

UPDATE custom_field_template cft
    JOIN issue_template it ON cft.template_id = it.id
SET cft.default_value = '0'
WHERE cft.field_id = (
    SELECT id
    FROM custom_field
    WHERE name = '复测次数'
      AND scene = 'ISSUE'
      AND `system` = 1
      AND `global` = 1
    LIMIT 1
)
  AND cft.default_value != '0';  -- 只更新非 0 的值，避免重复更新

-- 验证更新结果
SELECT
    COUNT(*) as updated_count,
    '所有模板的复测次数默认值已更新为 0' as message
FROM custom_field_template cft
WHERE cft.field_id = (
    SELECT id
    FROM custom_field
    WHERE name = '复测次数'
      AND scene = 'ISSUE'
      AND `system` = 1
      AND `global` = 1
    LIMIT 1
)
  AND cft.default_value = '0';



-- ============================================
-- 更新已存在缺陷的"复测次数"字段值（高级版）
-- ============================================
-- 目的：智能更新已创建缺陷的"复测次数"字段值
-- 策略：
--   1. 如果缺陷没有复测次数记录，插入默认值 0
--   2. 如果缺陷的复测次数为空/NULL/1，更新为 0（将旧默认值 1 统一改为新默认值 0）
--   3. 如果缺陷已有复测次数值（>1），保持不变

-- 步骤1：更新已存在但值为空/NULL/1/"1" 的记录为 0
-- 说明：将空值、NULL 和旧默认值 1（包括字符串"1"和数字1）统一改为新默认值 0
-- 注意：使用 BINARY 强制字符串比较，避免类型转换错误
UPDATE custom_field_issues cfi
SET cfi.value = '0'
WHERE cfi.field_id = (
    SELECT id
    FROM custom_field
    WHERE name = '复测次数'
      AND scene = 'ISSUE'
      AND `system` = 1
      AND `global` = 1
    LIMIT 1
)
AND (
    cfi.value = ''
    OR cfi.value IS NULL
    OR BINARY cfi.value = '1'
    OR BINARY cfi.value = '"1"'
);

-- 步骤2：为没有"复测次数"字段记录的缺陷插入默认值 0
-- 注意：只处理确实存在的缺陷（issues 表中有记录）
INSERT INTO custom_field_issues (resource_id, field_id, value, text_value)
SELECT
    i.id as resource_id,
    cf.id as field_id,
    '0' as value,
    NULL as text_value
FROM issues i
CROSS JOIN (
    SELECT id
    FROM custom_field
    WHERE name = '复测次数'
      AND scene = 'ISSUE'
      AND `system` = 1
      AND `global` = 1
    LIMIT 1
) cf
WHERE NOT EXISTS (
    SELECT 1
    FROM custom_field_issues cfi
    WHERE cfi.resource_id = i.id
      AND cfi.field_id = cf.id
);


-- 恢复默认锁等待时间
SET SESSION innodb_lock_wait_timeout = DEFAULT;



