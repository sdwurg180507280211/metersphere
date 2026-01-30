-- ==========================================
-- 工作空间项目顶级模块统计SQL（递归版本）
-- 功能：查询某个工作空间下每个项目的顶级模块，统计每个顶级模块及其所有子模块的用例和测试计划数量
-- 创建时间：2026-01-29
-- 更新时间：2026-01-30 - 新增回收站数据过滤(tc.delete_time IS NULL)
-- 更新时间：2026-01-30 - 新增测试计划按模块统计SQL
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


-- SQL 3: 统计每个项目的功能用例数量、测试计划数量、缺陷数量(汇总统计-优化版)
-- 说明: 使用子查询分别统计,避免大表JOIN导致的性能问题
SELECT
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    COALESCE(tc_count.case_count, 0) AS '功能用例总数(排除回收站)',
    COALESCE(tp_count.plan_count, 0) AS '测试计划总数',
    COALESCE(i_count.issue_count, 0) AS '缺陷总数'
FROM
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    -- 子查询1: 统计功能用例数量(排除回收站)
    LEFT JOIN (
        SELECT
            project_id,
            COUNT(id) AS case_count
        FROM test_case
        WHERE delete_time IS NULL
        GROUP BY project_id
    ) tc_count ON p.id = tc_count.project_id
    -- 子查询2: 统计测试计划数量
    LEFT JOIN (
        SELECT
            project_id,
            COUNT(id) AS plan_count
        FROM test_plan
        GROUP BY project_id
    ) tp_count ON p.id = tp_count.project_id
    -- 子查询3: 统计缺陷数量
    LEFT JOIN (
        SELECT
            project_id,
            COUNT(id) AS issue_count
        FROM issues
        GROUP BY project_id
    ) i_count ON p.id = i_count.project_id
WHERE
    w.name = '功能测试工作空间'  -- 替换为实际工作空间名称
ORDER BY
    p.name;


-- SQL 4: 统计每个项目下各状态的测试计划数量(不考虑模块)
-- 说明: 测试计划状态包括 Prepare(未开始)、Underway(进行中)、Completed(已完成)、Finished(已结束)、Archived(已归档)
SELECT
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    COUNT(DISTINCT tp.id) AS '测试计划总数',
    COUNT(DISTINCT CASE WHEN tp.status = 'Prepare' THEN tp.id END) AS '未开始',
    COUNT(DISTINCT CASE WHEN tp.status = 'Underway' THEN tp.id END) AS '进行中',
    COUNT(DISTINCT CASE WHEN tp.status = 'Completed' THEN tp.id END) AS '已完成',
    COUNT(DISTINCT CASE WHEN tp.status = 'Finished' THEN tp.id END) AS '已结束',
    COUNT(DISTINCT CASE WHEN tp.status = 'Archived' THEN tp.id END) AS '已归档'
FROM
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
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
-- ==========================================
