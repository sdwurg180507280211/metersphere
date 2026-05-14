-- 将测试跟踪模块树默认节点名称从"未规划模块"改为"未规划系统"
UPDATE test_case_node SET name = '未规划系统' WHERE name = '未规划模块';
UPDATE test_case_review_node SET name = '未规划系统' WHERE name = '未规划模块';
UPDATE test_plan_node SET name = '未规划系统' WHERE name = '未规划模块';
