-- ==========================================
-- 工作空间项目模块统计SQL
-- 功能：查询某个工作空间下每个项目包含的模块，各模块下的测试计划数量和功能用例数量
-- 创建时间：2026-01-29
-- ==========================================

-- 方式一：按工作空间ID查询（推荐）
-- 使用说明：将 'YOUR_WORKSPACE_ID' 替换为实际的工作空间ID

SELECT 
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    IFNULL(tcn.name, '无模块') AS '模块名称',
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_case_node tcn ON p.id = tcn.project_id
    LEFT JOIN test_case tc ON tcn.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
WHERE 
    w.id = 'YOUR_WORKSPACE_ID'  -- 替换为实际工作空间ID
GROUP BY 
    w.name, p.name, tcn.name
ORDER BY 
    p.name, tcn.name;


-- ==========================================
-- 方式二：按工作空间名称查询
-- 使用说明：将 '瑞众测试平台' 替换为实际的工作空间名称
-- ==========================================

SELECT 
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    IFNULL(tcn.name, '无模块') AS '模块名称',
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_case_node tcn ON p.id = tcn.project_id
    LEFT JOIN test_case tc ON tcn.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
WHERE 
    w.name = '瑞众测试平台'  -- 替换为实际工作空间名称
GROUP BY 
    w.name, p.name, tcn.name
ORDER BY 
    p.name, tcn.name;


-- ==========================================
-- 方式三：查询所有工作空间的统计（用于全局查看）
-- ==========================================

SELECT 
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    IFNULL(tcn.name, '无模块') AS '模块名称',
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_case_node tcn ON p.id = tcn.project_id
    LEFT JOIN test_case tc ON tcn.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
GROUP BY 
    w.name, p.name, tcn.name
ORDER BY 
    w.name, p.name, tcn.name;


-- ==========================================
-- 方式四：按项目汇总（不区分模块）
-- 适用场景：只需要项目级别的统计，不需要细分到模块
-- ==========================================

SELECT 
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    COUNT(DISTINCT tp.id) AS '测试计划总数',
    COUNT(DISTINCT tc.id) AS '功能用例总数',
    COUNT(DISTINCT tcn.id) AS '模块数量'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_case_node tcn ON p.id = tcn.project_id
    LEFT JOIN test_case tc ON p.id = tc.project_id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
WHERE 
    w.name = '瑞众测试平台'  -- 替换为实际工作空间名称
GROUP BY 
    w.name, p.name
ORDER BY 
    p.name;


-- ==========================================
-- 方式五：详细统计（包含Bug数量）
-- 说明：如果需要统计Bug数量，需要关联issues表
-- ==========================================

SELECT 
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    IFNULL(tcn.name, '无模块') AS '模块名称',
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量',
    COUNT(DISTINCT i.id) AS 'Bug数量'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_case_node tcn ON p.id = tcn.project_id
    LEFT JOIN test_case tc ON tcn.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
    LEFT JOIN issues i ON p.id = i.project_id
WHERE 
    w.name = '瑞众测试平台'  -- 替换为实际工作空间名称
GROUP BY 
    w.name, p.name, tcn.name
ORDER BY 
    p.name, tcn.name;


-- ==========================================
-- 辅助查询：查看所有工作空间列表
-- ==========================================

SELECT 
    id AS '工作空间ID',
    name AS '工作空间名称',
    description AS '描述',
    FROM_UNIXTIME(create_time/1000) AS '创建时间'
FROM 
    workspace
ORDER BY 
    create_time DESC;


-- ==========================================
-- 辅助查询：查看指定工作空间下的所有项目
-- ==========================================

SELECT 
    p.id AS '项目ID',
    p.name AS '项目名称',
    w.name AS '所属工作空间',
    p.description AS '描述',
    FROM_UNIXTIME(p.create_time/1000) AS '创建时间'
FROM 
    project p
    INNER JOIN workspace w ON p.workspace_id = w.id
WHERE 
    w.name = '瑞众测试平台'  -- 替换为实际工作空间名称
ORDER BY 
    p.create_time DESC;


-- ==========================================
-- 方式六：只统计顶级模块（level = 1）
-- 说明：顶级模块是和"未规划用例"同级的一级模块
-- 适用场景：只需要看项目下的顶级模块分类，不需要看子模块
-- ==========================================

SELECT 
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    IFNULL(tcn.name, '无模块') AS '顶级模块名称',
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_case_node tcn ON p.id = tcn.project_id AND tcn.level = 1  -- 只查询顶级模块
    LEFT JOIN test_case tc ON tcn.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
WHERE 
    w.name = '瑞众测试平台'  -- 替换为实际工作空间名称
GROUP BY 
    w.name, p.name, tcn.name
ORDER BY 
    p.name, tcn.name;


-- ==========================================
-- 方式七：顶级模块统计（包含所有层级用例）
-- 说明：统计顶级模块及其所有子模块下的用例总数
-- 适用场景：需要看顶级模块的完整数据（包括子模块的用例）
-- 技术说明：使用子查询找出顶级模块的所有子孙模块
-- ==========================================

SELECT 
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    tcn_top.name AS '顶级模块名称',
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量（含子模块）'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_case_node tcn_top ON p.id = tcn_top.project_id AND tcn_top.level = 1  -- 顶级模块
    LEFT JOIN test_case_node tcn_all ON (
        tcn_all.id = tcn_top.id  -- 顶级模块本身
        OR tcn_all.parent_id = tcn_top.id  -- 二级子模块
        OR tcn_all.parent_id IN (  -- 三级及以下子模块
            SELECT id FROM test_case_node WHERE parent_id = tcn_top.id
        )
    ) AND tcn_all.project_id = p.id
    LEFT JOIN test_case tc ON tcn_all.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
WHERE 
    w.name = '瑞众测试平台'  -- 替换为实际工作空间名称
GROUP BY 
    w.name, p.name, tcn_top.name
ORDER BY 
    p.name, tcn_top.name;


-- ==========================================
-- 方式八：顶级模块统计（递归CTE版本，支持无限层级）
-- 说明：使用递归公用表表达式(CTE)统计所有层级的子模块用例
-- 适用场景：模块层级较深（超过3层）时使用
-- 注意：MySQL 8.0+ 支持递归CTE
-- ==========================================

WITH RECURSIVE module_tree AS (
    -- 锚点：顶级模块
    SELECT id, project_id, name, parent_id, level, id as top_module_id
    FROM test_case_node
    WHERE level = 1
    
    UNION ALL
    
    -- 递归：查找所有子模块
    SELECT tcn.id, tcn.project_id, tcn.name, tcn.parent_id, tcn.level, mt.top_module_id
    FROM test_case_node tcn
    INNER JOIN module_tree mt ON tcn.parent_id = mt.id
)
SELECT 
    w.name AS '工作空间名称',
    p.name AS '项目名称',
    tcn_top.name AS '顶级模块名称',
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量（含所有子模块）'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN test_case_node tcn_top ON p.id = tcn_top.project_id AND tcn_top.level = 1
    LEFT JOIN module_tree mt ON mt.top_module_id = tcn_top.id
    LEFT JOIN test_case tc ON mt.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
WHERE 
    w.name = '瑞众测试平台'  -- 替换为实际工作空间名称
GROUP BY 
    w.name, p.name, tcn_top.name
ORDER BY 
    p.name, tcn_top.name;


-- ==========================================
-- 说明：
-- 1. test_case_node 表存储功能用例的模块（树形结构）
--    - level 字段：1表示顶级模块，2表示二级模块，以此类推
--    - parent_id 字段：NULL表示顶级模块，否则指向父模块ID
--    - 注意：该表没有 node_path 字段，需要通过 parent_id 递归查询子模块
-- 2. test_case 表存储功能用例，通过 node_id 关联到模块
-- 3. test_plan 表存储测试计划，通过 project_id 关联到项目
-- 4. 如果模块下没有用例，会显示 0
-- 5. 测试计划是项目级别的，不直接关联模块
-- 6. 时间戳字段存储的是毫秒级时间戳，需要除以1000转换
-- 7. 方式六只统计直接挂在顶级模块下的用例（不含子模块）
-- 8. 方式七统计顶级模块及其子模块下的用例（支持3层以内）
-- 9. 方式八使用递归CTE统计所有层级的用例（MySQL 8.0+，支持无限层级）
-- ==========================================
