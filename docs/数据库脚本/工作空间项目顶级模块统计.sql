-- ==========================================
-- 工作空间项目顶级模块统计SQL（递归版本）
-- 功能：查询某个工作空间下每个项目的顶级模块，统计每个顶级模块及其所有子模块的用例和测试计划数量
-- 创建时间：2026-01-29
-- ==========================================

-- 方式一：使用递归CTE统计顶级模块及所有子模块的数据（推荐）
-- 说明：支持无限层级的模块树，MySQL 8.0+

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
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量（含所有子模块）'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN module_tree mt ON mt.project_id = p.id
    LEFT JOIN test_case tc ON mt.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
WHERE 
    w.name = '默认工作空间'  -- 替换为实际工作空间名称
GROUP BY 
    w.name, p.name, mt.top_module_name
ORDER BY 
    p.name, mt.top_module_name;


-- ==========================================
-- 方式二：按工作空间ID查询
-- ==========================================

WITH RECURSIVE module_tree AS (
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
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量（含所有子模块）'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN module_tree mt ON mt.project_id = p.id
    LEFT JOIN test_case tc ON mt.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
WHERE 
    w.id = 'YOUR_WORKSPACE_ID'  -- 替换为实际工作空间ID
GROUP BY 
    w.name, p.name, mt.top_module_name
ORDER BY 
    p.name, mt.top_module_name;


-- ==========================================
-- 方式三：查询所有工作空间的顶级模块统计
-- ==========================================

WITH RECURSIVE module_tree AS (
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
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量（含所有子模块）'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN module_tree mt ON mt.project_id = p.id
    LEFT JOIN test_case tc ON mt.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
GROUP BY 
    w.name, p.name, mt.top_module_name
ORDER BY 
    w.name, p.name, mt.top_module_name;


-- ==========================================
-- 方式四：包含Bug数量的统计
-- ==========================================

WITH RECURSIVE module_tree AS (
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
    COUNT(DISTINCT tp.id) AS '测试计划数量',
    COUNT(DISTINCT tc.id) AS '功能用例数量（含所有子模块）',
    COUNT(DISTINCT i.id) AS 'Bug数量'
FROM 
    workspace w
    INNER JOIN project p ON w.id = p.workspace_id
    LEFT JOIN module_tree mt ON mt.project_id = p.id
    LEFT JOIN test_case tc ON mt.id = tc.node_id AND tc.project_id = p.id
    LEFT JOIN test_plan tp ON p.id = tp.project_id
    LEFT JOIN issues i ON p.id = i.project_id
WHERE 
    w.name = '默认工作空间'  -- 替换为实际工作空间名称
GROUP BY 
    w.name, p.name, mt.top_module_name
ORDER BY 
    p.name, mt.top_module_name;


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
    w.name = '默认工作空间'  -- 替换为实际工作空间名称
ORDER BY 
    p.create_time DESC;


-- ==========================================
-- 辅助查询：查看某个项目的模块树结构
-- ==========================================

SELECT 
    id AS '模块ID',
    name AS '模块名称',
    parent_id AS '父模块ID',
    level AS '层级',
    FROM_UNIXTIME(create_time/1000) AS '创建时间'
FROM 
    test_case_node
WHERE 
    project_id = 'YOUR_PROJECT_ID'  -- 替换为实际项目ID
ORDER BY 
    level, name;


-- ==========================================
-- 说明：
-- 1. 递归CTE (WITH RECURSIVE) 用于遍历模块树的所有层级
-- 2. 锚点查询：选择所有顶级模块（level = 1）
-- 3. 递归查询：通过 parent_id 关联找出所有子模块
-- 4. top_module_id 和 top_module_name 字段用于标识每个模块属于哪个顶级模块
-- 5. 最终统计时按顶级模块分组，包含其所有子孙模块的用例数
-- 6. 测试计划是项目级别的，不直接关联模块
-- 7. 支持MySQL 8.0+版本
-- ==========================================
