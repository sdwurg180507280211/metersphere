-- ==========================================
-- 清理 custom_field_template 表中的重复字段关联
-- ==========================================
-- 问题：同一个 field_id 在同一个 template_id 中被关联了多次
-- 原因：可能是数据迁移或模板复制时产生的重复数据
-- 影响：导出缺陷模板时，转换为 Map 时出现 Duplicate key 错误
-- ==========================================

-- 1. 查看所有重复数据（执行前检查）
SELECT 
    field_id,
    template_id,
    COUNT(*) as count,
    GROUP_CONCAT(`order` ORDER BY `order`) as orders,
    GROUP_CONCAT(`key` ORDER BY `order`) as key_list,
    GROUP_CONCAT(id ORDER BY `order`) as id_list
FROM custom_field_template
GROUP BY field_id, template_id
HAVING COUNT(*) > 1
ORDER BY count DESC, field_id;

-- 2. 查看重复字段的详细信息
SELECT 
    cf.id,
    cf.name,
    cf.scene,
    cf.type,
    cf.`system`,
    COUNT(DISTINCT cft.template_id) as template_count,
    SUM(CASE WHEN dup.field_id IS NOT NULL THEN 1 ELSE 0 END) as duplicate_count
FROM custom_field cf
LEFT JOIN custom_field_template cft ON cf.id = cft.field_id
LEFT JOIN (
    SELECT field_id, template_id
    FROM custom_field_template
    GROUP BY field_id, template_id
    HAVING COUNT(*) > 1
) dup ON cf.id = dup.field_id
WHERE cf.id IN (
    SELECT field_id
    FROM custom_field_template
    GROUP BY field_id, template_id
    HAVING COUNT(*) > 1
)
GROUP BY cf.id, cf.name, cf.scene, cf.type, cf.`system`
ORDER BY duplicate_count DESC;

-- 3. 查看具体的重复记录详情
SELECT 
    cft.id,
    cft.field_id,
    cf.name as field_name,
    cft.template_id,
    it.name as template_name,
    cft.`order`,
    cft.`key`,
    cft.default_value,
    cft.required
FROM custom_field_template cft
INNER JOIN custom_field cf ON cft.field_id = cf.id
LEFT JOIN issue_template it ON cft.template_id = it.id
WHERE (cft.field_id, cft.template_id) IN (
    SELECT field_id, template_id
    FROM custom_field_template
    GROUP BY field_id, template_id
    HAVING COUNT(*) > 1
)
ORDER BY cft.field_id, cft.template_id, cft.`order`;

-- ==========================================
-- 清理方案：使用通用脚本自动清理所有重复数据
-- ==========================================
-- 策略：保留每个 (field_id, template_id) 组合中的第一条记录
-- 判断标准：
--   1. 如果 order 不为 NULL，保留 order 最小的记录
--   2. 如果 order 都为 NULL，保留 id 最小的记录
-- ==========================================

-- 4. 创建临时表存储要保留的记录
-- 注意：这是破坏性操作，执行前请备份数据！
-- 备份命令：mysqldump -h 127.0.0.1 -u root -p metersphere_dev custom_field_template > backup_custom_field_template.sql

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

-- 5. 查看将要保留的记录（验证用）
SELECT 
    cft.id,
    cft.field_id,
    cf.name as field_name,
    cft.template_id,
    cft.`order`,
    cft.`key`,
    cft.default_value
FROM custom_field_template cft
INNER JOIN custom_field cf ON cft.field_id = cf.id
INNER JOIN temp_keep_records tkr ON 
    cft.field_id = tkr.field_id 
    AND cft.template_id = tkr.template_id
    AND CONCAT(
        CASE 
            WHEN cft.`order` IS NOT NULL THEN LPAD(cft.`order`, 10, '0')
            ELSE '9999999999'
        END,
        cft.id
    ) = tkr.sort_key
WHERE (cft.field_id, cft.template_id) IN (
    SELECT field_id, template_id
    FROM custom_field_template
    GROUP BY field_id, template_id
    HAVING COUNT(*) > 1
)
ORDER BY cft.field_id, cft.template_id;

-- 6. 查看将要删除的记录（验证用）
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

-- 7. 执行删除操作（取消注释后执行）
-- 警告：这是破坏性操作，执行前请确保已备份数据！
/*
DELETE FROM custom_field_template
WHERE (field_id, template_id) IN (
    SELECT field_id, template_id
    FROM custom_field_template
    GROUP BY field_id, template_id
    HAVING COUNT(*) > 1
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
*/

-- 8. 验证清理结果
SELECT 
    field_id,
    template_id,
    COUNT(*) as count
FROM custom_field_template
GROUP BY field_id, template_id
HAVING COUNT(*) > 1;

-- 9. 清理临时表
DROP TEMPORARY TABLE IF EXISTS temp_keep_records;

-- ==========================================
-- 执行步骤总结
-- ==========================================
-- 1. 执行步骤 1-3，查看重复数据的情况
-- 2. 备份数据库：
--    mysqldump -h 127.0.0.1 -u root -p metersphere_dev custom_field_template > backup_custom_field_template.sql
-- 3. 执行步骤 4-6，查看将要保留和删除的记录
-- 4. 确认无误后，取消步骤 7 的注释，执行删除操作
-- 5. 执行步骤 8，验证清理结果（应该返回空结果）
-- 6. 执行步骤 9，清理临时表
-- ==========================================

-- ==========================================
-- 受影响的字段统计（当前数据）
-- ==========================================
-- 1. 严重程度 (d392af07-fdfe-4475-a459-87d59f0b1626) - 3个模板重复
-- 2. 处理人 (a577bc60-75fe-47ec-8aa6-32dca23bf3d6) - 3个模板重复
-- 3. 状态 (beb57501-19c8-4ca3-8dfb-2cef7c0ea087) - 3个模板重复
-- 4. 缺陷所属系统 (issue-associated-system-field-2024-12-16-001) - 3个模板重复
-- 5. 复测次数 (9f7a4f4f-2b11-4df6-8e4b-6d3c3d2a6b4a) - 1个模板重复
-- 6. 用例等级 (4619cc23-9d1d-11eb-b418-0242ac120002) - 1个模板重复
-- 
-- 总计：6个字段，14条重复记录
-- ==========================================
