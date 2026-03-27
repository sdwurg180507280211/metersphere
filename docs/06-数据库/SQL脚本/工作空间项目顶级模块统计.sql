-- ==========================================
-- 工作空间项目顶级模块统计SQL（递归版本）
-- 功能：查询某个工作空间下每个项目的顶级模块，统计每个顶级模块及其所有子模块的用例和测试计划数量
-- 创建时间：2026-01-29
-- 更新时间：2026-01-30 - 新增回收站数据过滤(tc.delete_time IS NULL)
-- 更新时间：2026-01-30 - 新增测试计划按模块统计SQL
-- 更新时间：2026-02-06 - SQL3和SQL4添加创建时间范围过滤
-- 更新时间：2026-03-20 - 新增SQL11：按项目统计各状态测试计划数量及已完成或已结束且通过率>95%的计划数量

-- 所有SQL都要加上时间范围
-- ==========================================

-- 方式一：按工作空间名称查询（分离统计,避免笛卡尔积）
-- 说明：支持无限层级的模块树,MySQL 8.0+

-- SQL 1: 统计每个顶级模块的功能用例数量(排除回收站)
WITH RECURSIVE module_tree AS (
    -- 锚点：顶级模块（level = 1）
    SELECT
        id,
        project_id,
        name,
        parent_id,
        level,
        id as top_module_id,
        name as top_module_name
    FROM test_case_node
    WHERE level = 1

    UNION ALL

    -- 递归：查找所有子模块
    SELECT
        tcn.id,
        tcn.project_id,
        tcn.name,
        tcn.parent_id,
        tcn.level,
        mt.top_module_id,
        mt.top_module_name
    FROM test_case_node tcn
    INNER JOIN module_tree mt ON tcn.parent_id = mt.id
)
SELECT
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    mt.top_module_name AS '顶级模块名称',
    COUNT(DISTINCT tc.id) AS '功能用例数量（含所有子模块）'
FROM
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN module_tree mt ON mt.project_id = p.id
    LEFT JOIN test_case tc ON mt.id = tc.node_id AND tc.project_id = p.id AND tc.delete_time IS NULL  -- 排除回收站数据
WHERE
    w.name = '功能测试工作空间'  -- 替换为实际工作空间名称
GROUP BY
    w.name, p.name, mt.top_module_name
ORDER BY
    p.name, mt.top_module_name;

-- SQL 2: 统计每个顶级模块的测试计划数量
WITH RECURSIVE plan_module_tree AS (
    -- 锚点：顶级模块（level = 1）
    SELECT
        id,
        project_id,
        name,
        parent_id,
        level,
        id as top_module_id,
        name as top_module_name
    FROM test_plan_node
    WHERE level = 1

    UNION ALL

    -- 递归：查找所有子模块
    SELECT
        tpn.id,
        tpn.project_id,
        tpn.name,
        tpn.parent_id,
        tpn.level,
        pmt.top_module_id,
        pmt.top_module_name
    FROM test_plan_node tpn
    INNER JOIN plan_module_tree pmt ON tpn.parent_id = pmt.id
)
SELECT
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    pmt.top_module_name AS '顶级模块名称',
    COUNT(DISTINCT tp.id) AS '测试计划数量（含所有子模块）'
FROM
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN plan_module_tree pmt ON pmt.project_id = p.id
    LEFT JOIN test_plan tp ON pmt.id = tp.node_id AND tp.project_id = p.id
WHERE
    w.name = '功能测试工作空间'  -- 替换为实际工作空间名称
GROUP BY
    w.name, p.name, pmt.top_module_name
ORDER BY
    p.name, pmt.top_module_name;


-- SQL 3: 统计每个项目的功能用例数量、测试计划数量、缺陷数量(汇总统计-优化版-带时间范围)
-- 说明: 使用子查询分别统计,避免大表JOIN导致的性能问题
-- 时间范围: 按创建时间过滤（test_case.create_time、test_plan.create_time、issues.create_time）
SELECT
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    COALESCE(tc_count.case_count, 0) AS '功能用例总数(排除回收站)',
    COALESCE(tp_count.plan_count, 0) AS '测试计划总数',
    COALESCE(i_count.issue_count, 0) AS '缺陷总数',
    CONCAT(
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-01-01 00:00:00')),
        ' ~ ',
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-02-06 23:59:59'))
    ) AS '统计时间范围(创建时间)'
FROM
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    -- 子查询1: 统计功能用例数量(排除回收站,按创建时间过滤)
    LEFT JOIN (
        SELECT
            project_id,
            COUNT(id) AS case_count
        FROM test_case
        WHERE delete_time IS NULL
          AND create_time >= UNIX_TIMESTAMP('2026-01-01 00:00:00') * 1000  -- 开始时间（毫秒时间戳）
          AND create_time <= UNIX_TIMESTAMP('2026-02-06 23:59:59') * 1000  -- 结束时间（毫秒时间戳）
        GROUP BY project_id
    ) tc_count ON p.id = tc_count.project_id
    -- 子查询2: 统计测试计划数量(按创建时间过滤)
    LEFT JOIN (
        SELECT
            project_id,
            COUNT(id) AS plan_count
        FROM test_plan
        WHERE create_time >= UNIX_TIMESTAMP('2026-01-01 00:00:00') * 1000  -- 开始时间（毫秒时间戳）
          AND create_time <= UNIX_TIMESTAMP('2026-02-06 23:59:59') * 1000  -- 结束时间（毫秒时间戳）
        GROUP BY project_id
    ) tp_count ON p.id = tp_count.project_id
    -- 子查询3: 统计缺陷数量(按创建时间过滤)
    LEFT JOIN (
        SELECT
            project_id,
            COUNT(id) AS issue_count
        FROM issues
        WHERE create_time >= UNIX_TIMESTAMP('2026-01-01 00:00:00') * 1000  -- 开始时间（毫秒时间戳）
          AND create_time <= UNIX_TIMESTAMP('2026-02-06 23:59:59') * 1000  -- 结束时间（毫秒时间戳）
        GROUP BY project_id
    ) i_count ON p.id = i_count.project_id
WHERE
    w.name = '功能测试工作空间'  -- 替换为实际工作空间名称
ORDER BY
    p.name;


-- SQL 4: 统计每个项目下各状态的测试计划数量(不考虑模块-带时间范围)
-- 说明: 测试计划状态包括 Prepare(未开始)、Underway(进行中)、Completed(已完成)、Finished(已结束)、Archived(已归档)
-- 时间范围: 按创建时间过滤（test_plan.create_time）
SELECT
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    COUNT(DISTINCT tp.id) AS '测试计划总数',
    COUNT(DISTINCT CASE WHEN tp.status = 'Prepare' THEN tp.id END) AS '未开始',
    COUNT(DISTINCT CASE WHEN tp.status = 'Underway' THEN tp.id END) AS '进行中',
    COUNT(DISTINCT CASE WHEN tp.status = 'Completed' THEN tp.id END) AS '已完成',
    COUNT(DISTINCT CASE WHEN tp.status = 'Finished' THEN tp.id END) AS '已结束',
    COUNT(DISTINCT CASE WHEN tp.status = 'Archived' THEN tp.id END) AS '已归档',
    CONCAT(
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-01-01 00:00:00')),
        ' ~ ',
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-02-06 23:59:59'))
    ) AS '统计时间范围(创建时间)'
FROM
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
        AND tp.create_time >= UNIX_TIMESTAMP('2026-01-01 00:00:00') * 1000  -- 开始时间（毫秒时间戳）
        AND tp.create_time <= UNIX_TIMESTAMP('2026-02-06 23:59:59') * 1000  -- 结束时间（毫秒时间戳）
WHERE
    w.name = '功能测试工作空间'  -- 替换为实际工作空间名称
GROUP BY
    w.name, p.name
ORDER BY
    p.name;


-- SQL 5: 统计某个工作空间下某个时间段内执行的功能用例数量和创建的缺陷数量
-- 说明:
--   1. 执行的功能用例：统计 test_plan_test_case 表中 update_time 在时间范围内的记录
--   2. 创建的缺陷：统计 issues 表中 create_time 在时间范围内的记录
--   3. 时间字段为毫秒时间戳，需要转换为日期格式
--   4. 支持按项目分组统计
SELECT
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    COALESCE(exec_count.executed_case_count, 0) AS '执行的功能用例数量',
    COALESCE(issue_count.created_issue_count, 0) AS '创建的缺陷数量',
    CONCAT(
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-01-01 00:00:00')),
        ' ~ ',
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-02-05 23:59:59'))
    ) AS '统计时间范围'
FROM
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    -- 子查询1: 统计时间范围内执行的功能用例数量
    LEFT JOIN (
        SELECT
            tp.project_id,
            COUNT(DISTINCT tptc.id) AS executed_case_count
        FROM test_plan_test_case tptc
        INNER JOIN test_plan tp ON tptc.plan_id = tp.id
        WHERE tptc.update_time >= UNIX_TIMESTAMP('2026-01-01 00:00:00') * 1000  -- 开始时间（毫秒时间戳）
          AND tptc.update_time <= UNIX_TIMESTAMP('2026-02-05 23:59:59') * 1000  -- 结束时间（毫秒时间戳）
          AND tptc.is_del = 0  -- 排除已删除的关联
        GROUP BY tp.project_id
    ) exec_count ON p.id = exec_count.project_id
    -- 子查询2: 统计时间范围内创建的缺陷数量
    LEFT JOIN (
        SELECT
            project_id,
            COUNT(id) AS created_issue_count
        FROM issues
        WHERE create_time >= UNIX_TIMESTAMP('2026-01-01 00:00:00') * 1000  -- 开始时间（毫秒时间戳）
          AND create_time <= UNIX_TIMESTAMP('2026-02-05 23:59:59') * 1000  -- 结束时间（毫秒时间戳）
        GROUP BY project_id
    ) issue_count ON p.id = issue_count.project_id
WHERE
    w.name = '功能测试工作空间'  -- 替换为实际工作空间名称
ORDER BY
    p.name;

-- SQL 6:从项目维度分别统计缺陷的严重级别，要带起止时间条件【缺陷的创建时间】
-- 说明：按“严重级别”字段统计；时间按 issues.create_time（毫秒时间戳）过滤
SELECT
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    COUNT(s.issue_id) AS '缺陷总数',
    COUNT(CASE WHEN s.severity = '阻断' THEN 1 END) AS '阻断',
    COUNT(CASE WHEN s.severity = '严重' THEN 1 END) AS '严重',
    COUNT(CASE WHEN s.severity = '一般' THEN 1 END) AS '一般',
    CONCAT(
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-01-01 00:00:00')),
        ' ~ ',
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-02-06 23:59:59'))
    ) AS '统计时间范围(创建时间)'
FROM
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN (
        SELECT
            i.id AS issue_id,
            i.project_id,
            COALESCE(
                MAX(NULLIF(TRIM(BOTH '"' FROM cfi.text_value), '')),
                MAX(NULLIF(TRIM(BOTH '"' FROM cfi.value), '')),
                '未设置'
            ) AS severity
        FROM issues i
        LEFT JOIN custom_field_issues cfi
            ON cfi.resource_id = i.id
           AND cfi.field_id IN (
               SELECT id
               FROM custom_field
               WHERE scene = 'ISSUE'
                 AND name = '严重级别'
           )
        WHERE i.create_time >= UNIX_TIMESTAMP('2026-01-01 00:00:00') * 1000  -- 开始时间（毫秒时间戳）
          AND i.create_time <= UNIX_TIMESTAMP('2026-02-06 23:59:59') * 1000  -- 结束时间（毫秒时间戳）
        GROUP BY i.id, i.project_id
    ) s ON p.id = s.project_id
WHERE
    w.name = '功能测试工作空间'  -- 替换为实际工作空间名称
GROUP BY
    w.name, p.name
ORDER BY
    p.name;


-- SQL7：从项目维度统计平均复测次数，要带起止时间条件【缺陷的创建时间】
-- 说明：复测次数来自系统字段“复测次数”；兼容 value 为 1、"1"、空值等格式
SELECT
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    COUNT(r.issue_id) AS '缺陷总数',
    COALESCE(SUM(r.retest_count), 0) AS '复测总次数',
    COALESCE(ROUND(AVG(r.retest_count), 2), 0) AS '平均复测次数',
    CONCAT(
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-01-01 00:00:00')),
        ' ~ ',
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-02-06 23:59:59'))
    ) AS '统计时间范围(创建时间)'
FROM
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN (
        SELECT
            i.id AS issue_id,
            i.project_id,
            COALESCE(
                MAX(
                    CASE
                        WHEN TRIM(BOTH '"' FROM COALESCE(NULLIF(cfi.value, ''), NULLIF(cfi.text_value, ''))) REGEXP '^[0-9]+$'
                            THEN CAST(TRIM(BOTH '"' FROM COALESCE(NULLIF(cfi.value, ''), NULLIF(cfi.text_value, ''))) AS UNSIGNED)
                        ELSE NULL
                    END
                ),
                0
            ) AS retest_count
        FROM issues i
        LEFT JOIN custom_field_issues cfi
            ON cfi.resource_id = i.id
           AND cfi.field_id IN (
               SELECT id
               FROM custom_field
               WHERE scene = 'ISSUE'
                 AND name = '复测次数'
           )
        WHERE i.create_time >= UNIX_TIMESTAMP('2026-01-01 00:00:00') * 1000  -- 开始时间（毫秒时间戳）
          AND i.create_time <= UNIX_TIMESTAMP('2026-02-06 23:59:59') * 1000  -- 结束时间（毫秒时间戳）
        GROUP BY i.id, i.project_id
    ) r ON p.id = r.project_id
WHERE
    w.name = '功能测试工作空间'  -- 替换为实际工作空间名称
GROUP BY
    w.name, p.name
ORDER BY
    p.name;

-- SQL8: 从所属系统角度统计功能用例数量、缺陷总数及严重程度分布
-- 时间范围：2020-01-01 至 2026-12-31
-- 更新时间：2026-03-17 - 修复"未分配系统"重复问题，添加TRIM后空值过滤

SELECT
    COALESCE(system_names.name, '未分配系统') AS 所属系统,
    COALESCE(test_case_count, 0) AS 功能用例数量,
    COALESCE(issue_total, 0) AS 缺陷总数,
    COALESCE(severity_阻断, 0) AS 严重程度_阻断,
    COALESCE(severity_严重, 0) AS 严重程度_严重,
    COALESCE(severity_一般, 0) AS 严重程度_一般,
    COALESCE(severity_其他, 0) AS 严重程度_其他,
    COALESCE(project_list, '-') AS 用例所属项目,
    COALESCE(issue_project_list, '-') AS 缺陷所属项目
FROM (
    SELECT DISTINCT
        CASE
            WHEN asys.id IS NULL THEN 'UNASSIGNED'
            ELSE raw_systems.system_id
        END AS system_id
    FROM (
        SELECT DISTINCT
            COALESCE(NULLIF(TRIM(BOTH '"' FROM cftc.value), ''), 'UNASSIGNED') AS system_id
        FROM custom_field_test_case cftc
        INNER JOIN test_case tc ON cftc.resource_id = tc.id
        INNER JOIN project p ON tc.project_id = p.id
        INNER JOIN workspace w ON p.workspace_id = w.id
        WHERE cftc.field_id = 'case-associated-system-field-2024-12-01-002'
            AND tc.delete_time IS NULL
            AND w.name = '功能测试工作空间'
        UNION
        SELECT DISTINCT
            COALESCE(NULLIF(TRIM(BOTH '"' FROM cfi.value), ''), 'UNASSIGNED') AS system_id
        FROM custom_field_issues cfi
        INNER JOIN issues i ON cfi.resource_id = i.id
        INNER JOIN project p ON i.project_id = p.id
        INNER JOIN workspace w ON p.workspace_id = w.id
        WHERE cfi.field_id = 'issue-associated-system-field-2024-12-16-001'
            AND w.name = '功能测试工作空间'
    ) raw_systems
    LEFT JOIN associated_system asys ON raw_systems.system_id = asys.id
) systems
LEFT JOIN (
    SELECT
        TRIM(BOTH '"' FROM cftc.value) AS system_id,
        COUNT(DISTINCT tc.id) AS test_case_count,
        GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') AS project_list
    FROM test_case tc
    INNER JOIN custom_field_test_case cftc
        ON tc.id = cftc.resource_id
    INNER JOIN project p ON tc.project_id = p.id
    INNER JOIN workspace w ON p.workspace_id = w.id
    WHERE cftc.field_id = 'case-associated-system-field-2024-12-01-002'
        AND tc.type = 'functional'
        AND cftc.value IS NOT NULL
        AND cftc.value != ''
        AND tc.delete_time IS NULL
        AND tc.create_time BETWEEN UNIX_TIMESTAMP('2020-01-01 00:00:00') * 1000
                               AND UNIX_TIMESTAMP('2026-12-31 23:59:59') * 1000
        AND w.name = '功能测试工作空间'
    GROUP BY TRIM(BOTH '"' FROM cftc.value)
) tc_stats ON systems.system_id = tc_stats.system_id
LEFT JOIN (
    SELECT
        TRIM(BOTH '"' FROM cfi.value) AS system_id,
        COUNT(DISTINCT i.id) AS issue_total,
        SUM(CASE WHEN TRIM(BOTH '"' FROM cfi_severity.value) = '阻断' THEN 1 ELSE 0 END) AS severity_阻断,
        SUM(CASE WHEN TRIM(BOTH '"' FROM cfi_severity.value) = '严重' THEN 1 ELSE 0 END) AS severity_严重,
        SUM(CASE WHEN TRIM(BOTH '"' FROM cfi_severity.value) = '一般' THEN 1 ELSE 0 END) AS severity_一般,
        SUM(CASE WHEN TRIM(BOTH '"' FROM cfi_severity.value) NOT IN ('阻断', '严重', '一般') OR cfi_severity.value IS NULL THEN 1 ELSE 0 END) AS severity_其他,
        GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') AS issue_project_list
    FROM issues i
    INNER JOIN custom_field_issues cfi
        ON i.id = cfi.resource_id
    INNER JOIN project p ON i.project_id = p.id
    INNER JOIN workspace w ON p.workspace_id = w.id
    LEFT JOIN custom_field_issues cfi_severity
        ON i.id = cfi_severity.resource_id
        AND cfi_severity.field_id = 'issue-severity-level-field-2024-12-01-002'
    WHERE cfi.field_id = 'issue-associated-system-field-2024-12-16-001'
        AND cfi.value IS NOT NULL
        AND cfi.value != ''
        AND i.create_time BETWEEN UNIX_TIMESTAMP('2020-01-01 00:00:00') * 1000
                              AND UNIX_TIMESTAMP('2026-12-31 23:59:59') * 1000
        AND w.name = '功能测试工作空间'
    GROUP BY TRIM(BOTH '"' FROM cfi.value)
) issue_stats ON systems.system_id = issue_stats.system_id
LEFT JOIN associated_system system_names ON systems.system_id = system_names.id
ORDER BY 缺陷总数 DESC, 功能用例数量 DESC;


-- SQL9: 从用例执行人维度统计执行用例数量、创建缺陷数量、缺陷严重等级分布
-- 时间范围：2020-01-01 至 2026-12-31

SELECT
    COALESCE(u.name, testers.tester_id, '未分配') AS 执行人,
    COALESCE(test_case_count, 0) AS 执行用例数量,
    COALESCE(project_list, '-') AS 所属项目,
    COALESCE(issue_count, 0) AS 创建缺陷数量,
    COALESCE(severity_阻断, 0) AS 严重程度_阻断,
    COALESCE(severity_严重, 0) AS 严重程度_严重,
    COALESCE(severity_一般, 0) AS 严重程度_一般,
    COALESCE(severity_其他, 0) AS 严重程度_其他
FROM (
    SELECT DISTINCT tptc.executor AS tester_id
    FROM test_plan_test_case tptc
    INNER JOIN test_plan tp ON tptc.plan_id = tp.id
    INNER JOIN project p ON tp.project_id = p.id
    INNER JOIN workspace w ON p.workspace_id = w.id
    WHERE tptc.executor IS NOT NULL AND tptc.executor != ''
        AND w.name = '功能测试工作空间'
    UNION
    SELECT DISTINCT i.creator AS tester_id
    FROM issues i
    INNER JOIN project p ON i.project_id = p.id
    INNER JOIN workspace w ON p.workspace_id = w.id
    WHERE i.creator IS NOT NULL AND i.creator != ''
        AND w.name = '功能测试工作空间'
) testers
LEFT JOIN (
    SELECT
        tptc.executor AS tester_id,
        COUNT(DISTINCT tptc.id) AS test_case_count,
        GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') AS project_list
    FROM test_plan_test_case tptc
    INNER JOIN test_plan tp ON tptc.plan_id = tp.id
    INNER JOIN project p ON tp.project_id = p.id
    INNER JOIN workspace w ON p.workspace_id = w.id
    INNER JOIN test_case tc ON tptc.case_id = tc.id
    WHERE tptc.executor IS NOT NULL
        AND tptc.executor != ''
        AND tptc.is_del = 0
        AND tc.delete_time IS NULL
        AND tptc.update_time BETWEEN UNIX_TIMESTAMP('2020-01-01 00:00:00') * 1000
                            AND UNIX_TIMESTAMP('2026-12-31 23:59:59') * 1000
        AND w.name = '功能测试工作空间'
    GROUP BY tptc.executor
) tc_stats ON testers.tester_id = tc_stats.tester_id
LEFT JOIN (
    SELECT
        i.creator AS tester_id,
        COUNT(*) AS issue_count,
        SUM(CASE WHEN TRIM(BOTH '"' FROM cfi_severity.value) = '阻断' THEN 1 ELSE 0 END) AS severity_阻断,
        SUM(CASE WHEN TRIM(BOTH '"' FROM cfi_severity.value) = '严重' THEN 1 ELSE 0 END) AS severity_严重,
        SUM(CASE WHEN TRIM(BOTH '"' FROM cfi_severity.value) = '一般' THEN 1 ELSE 0 END) AS severity_一般,
        SUM(CASE WHEN TRIM(BOTH '"' FROM cfi_severity.value) NOT IN ('阻断', '严重', '一般') OR cfi_severity.value IS NULL THEN 1 ELSE 0 END) AS severity_其他
    FROM issues i
    INNER JOIN project p ON i.project_id = p.id
    INNER JOIN workspace w ON p.workspace_id = w.id
    LEFT JOIN custom_field_issues cfi_severity
        ON i.id = cfi_severity.resource_id
        AND cfi_severity.field_id = 'issue-severity-level-field-2024-12-01-002'
    WHERE i.creator IS NOT NULL
        AND i.creator != ''
        AND i.create_time BETWEEN UNIX_TIMESTAMP('2020-01-01 00:00:00') * 1000
                            AND UNIX_TIMESTAMP('2026-12-31 23:59:59') * 1000
        AND w.name = '功能测试工作空间'
    GROUP BY i.creator
) issue_stats ON testers.tester_id = issue_stats.tester_id
LEFT JOIN user u ON testers.tester_id = u.id
ORDER BY 执行用例数量 DESC, 创建缺陷数量 DESC;


-- SQL10: 从创建人维度统计功能用例数量（区分工作空间、排除回收站）
-- 时间范围：2020-01-01 至 2026-12-31

SELECT
    COALESCE(u.name, tc.create_user, '未分配') AS 创建人,
    COUNT(*) AS 功能用例数量
FROM test_case tc
INNER JOIN project p ON tc.project_id = p.id
INNER JOIN workspace w ON p.workspace_id = w.id
LEFT JOIN user u ON tc.create_user = u.id
WHERE tc.type = 'functional'
    AND tc.delete_time IS NULL
    AND tc.create_time BETWEEN UNIX_TIMESTAMP('2020-01-01 00:00:00') * 1000
                           AND UNIX_TIMESTAMP('2026-12-31 23:59:59') * 1000
    AND w.name = '功能测试工作空间'
GROUP BY tc.create_user, u.name
ORDER BY 功能用例数量 DESC;

-- SQL11: 统计每个项目下各状态的测试计划数量及已完成或已结束且通过率>95%的测试计划数量（排除已删除，带时间范围）
-- 说明：
--   1. 各状态统计：Prepare(未开始)、Underway(进行中)、Completed(已完成)、Finished(已结束)、Archived(已归档)
--   2. 通过率计算：Pass状态用例数 / 总用例数 * 100，排除已删除的用例关联(is_del=0)
--   3. 排除已删除的测试计划（status != 'Trash'）
--   4. 时间范围：按测试计划的计划开始时间和计划结束时间过滤（test_plan.planned_start_time / planned_end_time）
-- 创建时间：2026-03-20
SELECT
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    COUNT(DISTINCT tp.id) AS '测试计划总数',
    COUNT(DISTINCT CASE WHEN tp.status = 'Prepare' THEN tp.id END) AS '未开始',
    COUNT(DISTINCT CASE WHEN tp.status = 'Underway' THEN tp.id END) AS '进行中',
    COUNT(DISTINCT CASE WHEN tp.status = 'Completed' THEN tp.id END) AS '已完成',
    COUNT(DISTINCT CASE WHEN tp.status = 'Finished' THEN tp.id END) AS '已结束',
    COUNT(DISTINCT CASE WHEN tp.status = 'Archived' THEN tp.id END) AS '已归档',
    COALESCE(hp.high_pass_count, 0) AS '已完成或已结束且通过率>95%',
    CONCAT(
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-01-01 00:00:00')),
        ' ~ ',
        FROM_UNIXTIME(UNIX_TIMESTAMP('2026-03-20 23:59:59'))
    ) AS '统计时间范围(计划时间)'
FROM
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
        AND tp.status != 'Trash'  -- 排除已删除的计划
        AND tp.planned_start_time >= UNIX_TIMESTAMP('2026-01-01 00:00:00') * 1000  -- 计划开始时间（毫秒时间戳）
        AND tp.planned_end_time <= UNIX_TIMESTAMP('2026-03-20 23:59:59') * 1000  -- 计划结束时间（毫秒时间戳）
    -- 子查询: 统计已完成(Completed)或已结束(Finished)且通过率>95%的测试计划数量
    LEFT JOIN (
        SELECT
            project_id,
            COUNT(*) AS high_pass_count
        FROM (
            SELECT
                tp2.id,
                tp2.project_id,
                COUNT(tptc.id) AS total_cases,
                SUM(CASE WHEN tptc.status = 'Pass' THEN 1 ELSE 0 END) AS pass_cases,
                ROUND(SUM(CASE WHEN tptc.status = 'Pass' THEN 1 ELSE 0 END) / COUNT(tptc.id) * 100, 2) AS pass_rate
            FROM test_plan tp2
            INNER JOIN test_plan_test_case tptc ON tp2.id = tptc.plan_id AND tptc.is_del = 0
            WHERE tp2.status IN ('Completed', 'Finished')  -- 已完成或已结束
              AND tp2.status != 'Trash'  -- 排除已删除的计划
              AND tp2.planned_start_time >= UNIX_TIMESTAMP('2026-01-01 00:00:00') * 1000  -- 计划开始时间（毫秒时间戳）
              AND tp2.planned_end_time <= UNIX_TIMESTAMP('2026-03-20 23:59:59') * 1000  -- 计划结束时间（毫秒时间戳）
            GROUP BY tp2.id, tp2.project_id
            HAVING pass_rate > 95
        ) high_pass_plans
        GROUP BY project_id
    ) hp ON p.id = hp.project_id
WHERE
    w.name = '功能测试工作空间'  -- 替换为实际工作空间名称
GROUP BY
    w.name, p.name
ORDER BY
    p.name;


-- ==========================================
-- 说明：
-- 1. 递归CTE (WITH RECURSIVE) 用于遍历模块树的所有层级
-- 2. 锚点查询：选择所有顶级模块（level = 1）
-- 3. 递归查询：通过 parent_id 关联找出所有子模块
-- 4. top_module_id 和 top_module_name 字段用于标识每个模块属于哪个顶级模块
-- 5. 最终统计时按顶级模块分组,包含其所有子孙模块的用例数/测试计划数
-- 6. Bug是项目级别的,不直接关联模块,需要单独统计
-- 7. 分离统计避免了笛卡尔积导致的重复计数问题
-- 8. 支持MySQL 8.0+版本
-- 9. 回收站过滤：通过 tc.delete_time IS NULL 条件排除已删除的用例
--    - delete_time 为 NULL: 正常用例
--    - delete_time 不为 NULL: 回收站中的用例(已删除)
-- 10. 测试计划模块：test_plan 表通过 node_id 关联到 test_plan_node 表
--     test_plan_node 表结构与 test_case_node 类似,也支持多层级树形结构
-- 11. 时间范围统计（SQL5系列）：
--     - 时间字段为毫秒时间戳（bigint类型）
--     - 转换公式：UNIX_TIMESTAMP('日期时间') * 1000
--     - 执行用例：统计 test_plan_test_case.update_time（用例执行时更新）
--     - 创建缺陷：统计 issues.create_time（缺陷创建时间）
--     - SQL5: 基础统计（总数）
--     - SQL5-1: 按执行状态分组统计
--     - SQL5-2: 按日期分组统计（趋势分析）
-- 12. 用例执行状态：
--     - Pass: 通过
--     - Failure: 失败
--     - Blocking: 阻塞
--     - Skip: 跳过
--     - Prepare: 未执行
-- 13. SQL3和SQL4时间范围过滤（2026-02-06更新）：
--     - SQL3: 按创建时间过滤（test_case.create_time、test_plan.create_time、issues.create_time）
--     - SQL4: 按创建时间过滤（test_plan.create_time）
--     - 统计某时间段内新创建的用例、测试计划、缺陷数量
--     - 适用场景：统计新增数据、分析增长趋势
-- 14. SQL11 通过率计算（2026-03-20新增）：
--     - 通过率 = Pass状态用例数 / 总关联用例数 * 100
--     - 仅统计已完成(Completed)或已结束(Finished)状态的测试计划
--     - 排除已删除的用例关联（test_plan_test_case.is_del = 0）
--     - 排除已删除的测试计划（test_plan.status != 'Trash'）
--     - 使用嵌套子查询：内层按计划维度计算通过率并筛选>95%，外层按项目汇总数量
-- ==========================================
