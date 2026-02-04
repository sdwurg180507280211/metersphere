-- ==========================================
-- 一键清理 custom_field_template 表中的重复字段关联
-- ==========================================
-- 使用说明：
-- 1. 先备份数据库（必须！）
-- 2. 直接执行本脚本即可
-- ==========================================

-- 备份命令（在执行本脚本前，先在终端执行）：
-- mysqldump -h 127.0.0.1 -u root -p metersphere_dev custom_field_template > backup_custom_field_template_$(date +%Y%m%d_%H%M%S).sql

-- 创建临时表存储要保留的记录
DROP TEMPORARY TABLE IF EXISTS temp_keep_records;

CREATE TEMPORARY TABLE temp_keep_records AS
SELECT 
    MIN(
        CASE 
            WHEN `order` IS NOT NULL THEN CONCAT(LPAD(`order`, 10, '0'), id)
            ELSE CONCAT('9999999999', id)
        END
    ) as sort_key,
    field_id,
    template_id
FROM custom_field_template
GROUP BY field_id, template_id;

-- 显示将要删除的记录（供确认）
SELECT 
    cft.id,
    cft.field_id,
    cf.name as field_name,
    cft.template_id,
    cft.`order`,
    cft.`key`,
    cft.default_value,
    '将被删除' as action
FROM custom_field_template cft
INNER JOIN custom_field cf ON cft.field_id = cf.id
WHERE (cft.field_id, cft.template_id) IN (
    SELECT field_id, template_id
    FROM custom_field_template
    GROUP BY field_id, template_id
    HAVING COUNT(*) > 1
)
AND NOT EXISTS (
    SELECT 1
    FROM temp_keep_records tkr
    WHERE cft.field_id = tkr.field_id 
        AND cft.template_id = tkr.template_id
        AND CONCAT(
            CASE 
                WHEN cft.`order` IS NOT NULL THEN LPAD(cft.`order`, 10, '0')
                ELSE '9999999999'
            END,
            cft.id
        ) = tkr.sort_key
)
ORDER BY cft.field_id, cft.template_id, cft.`order`;

-- 执行删除操作
DELETE FROM custom_field_template
WHERE (field_id, template_id) IN (
    SELECT field_id, template_id
    FROM (
        SELECT field_id, template_id
        FROM custom_field_template
        GROUP BY field_id, template_id
        HAVING COUNT(*) > 1
    ) AS dup
)
AND NOT EXISTS (
    SELECT 1
    FROM temp_keep_records tkr
    WHERE custom_field_template.field_id = tkr.field_id 
        AND custom_field_template.template_id = tkr.template_id
        AND CONCAT(
            CASE 
                WHEN custom_field_template.`order` IS NOT NULL THEN LPAD(custom_field_template.`order`, 10, '0')
                ELSE '9999999999'
            END,
            custom_field_template.id
        ) = tkr.sort_key
);

-- 验证清理结果（应该返回空结果）
SELECT 
    field_id,
    template_id,
    COUNT(*) as count
FROM custom_field_template
GROUP BY field_id, template_id
HAVING COUNT(*) > 1;

-- 清理临时表
DROP TEMPORARY TABLE IF EXISTS temp_keep_records;

-- 完成提示
SELECT '清理完成！如果上面的查询返回空结果，说明所有重复数据已清理。' as message;
