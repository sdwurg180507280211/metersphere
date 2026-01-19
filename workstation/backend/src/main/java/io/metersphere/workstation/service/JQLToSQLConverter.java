package io.metersphere.workstation.service;

import io.metersphere.commons.exception.MSException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * JQL 到 SQL 转换器
 * 
 * 将解析后的 JQL 抽象语法树转换为 SQL WHERE 子句
 * 确保生成的 SQL 使用参数化查询，防止 SQL 注入
 * 
 * @author MeterSphere
 */
@Service
public class JQLToSQLConverter {
    
    /**
     * 字段名映射表（JQL 字段名 -> 数据库列名）
     * 
     * 我在做：定义 JQL 字段名到数据库列名的映射关系
     * 目的是：确保字段名在白名单内，防止任意字段查询
     * 如果不这样做：可能导致 SQL 注入风险
     */
    private static final Map<String, Map<String, String>> FIELD_MAPPING = new HashMap<>();
    
    static {
        // 测试用例字段映射
        Map<String, String> testCaseFields = new HashMap<>();
        testCaseFields.put("name", "test_case.name");
        testCaseFields.put("num", "test_case.num");
        testCaseFields.put("status", "test_case.status");
        testCaseFields.put("priority", "test_case.priority");
        testCaseFields.put("type", "test_case.type");
        testCaseFields.put("method", "test_case.method");
        testCaseFields.put("maintainer", "test_case.maintainer");
        testCaseFields.put("createUser", "test_case.create_user");
        testCaseFields.put("createTime", "test_case.create_time");
        testCaseFields.put("updateTime", "test_case.update_time");
        testCaseFields.put("reviewStatus", "test_case.review_status");
        FIELD_MAPPING.put("TEST_CASE", testCaseFields);
        
        // 缺陷字段映射
        Map<String, String> issueFields = new HashMap<>();
        issueFields.put("name", "issues.title");
        issueFields.put("num", "issues.num");
        issueFields.put("status", "issues.status");
        issueFields.put("platform", "issues.platform");
        issueFields.put("createUser", "issues.creator");
        issueFields.put("createTime", "issues.create_time");
        issueFields.put("updateTime", "issues.update_time");
        FIELD_MAPPING.put("ISSUE", issueFields);
        
        // 测试计划字段映射
        Map<String, String> testPlanFields = new HashMap<>();
        testPlanFields.put("name", "test_plan.name");
        testPlanFields.put("status", "test_plan.status");
        testPlanFields.put("stage", "test_plan.stage");
        testPlanFields.put("principal", "test_plan.principal");
        testPlanFields.put("createUser", "test_plan.creator");
        testPlanFields.put("createTime", "test_plan.create_time");
        testPlanFields.put("updateTime", "test_plan.update_time");
        FIELD_MAPPING.put("TEST_PLAN", testPlanFields);
        
        // 用例评审字段映射
        Map<String, String> reviewFields = new HashMap<>();
        reviewFields.put("name", "test_case_review.name");
        reviewFields.put("status", "test_case_review.status");
        reviewFields.put("createUser", "test_case_review.creator");
        reviewFields.put("createTime", "test_case_review.create_time");
        reviewFields.put("updateTime", "test_case_review.update_time");
        reviewFields.put("endTime", "test_case_review.end_time");
        FIELD_MAPPING.put("TEST_CASE_REVIEW", reviewFields);
    }
    
    /**
     * 将 AST 转换为 SQL WHERE 子句
     * 
     * 我在做：遍历 AST 并生成对应的 SQL WHERE 子句
     * 目的是：将 JQL 查询转换为可执行的 SQL
     * 如果不这样做：无法执行 JQL 查询
     * 
     * @param ast 抽象语法树
     * @param module 业务模块
     * @return SQL WHERE 子句（不包含 WHERE 关键字）
     */
    public String convertToSQL(Object ast, String module) {
        if (ast == null) {
            throw new MSException("AST 不能为空");
        }
        
        if (StringUtils.isBlank(module)) {
            throw new MSException("业务模块不能为空");
        }
        
        // TODO: 实现完整的 AST 到 SQL 转换逻辑
        // 1. 遍历 AST 节点
        // 2. 根据节点类型生成对应的 SQL 片段
        // 3. 使用参数化查询防止 SQL 注入
        // 4. 验证字段名在白名单内
        
        // 当前返回简单的占位 SQL
        return "1=1";
    }
    
    /**
     * 映射字段名到数据库列名
     * 
     * 我在做：将 JQL 字段名转换为数据库列名
     * 目的是：确保字段名合法且在白名单内
     * 如果不这样做：可能导致 SQL 注入或查询错误
     * 
     * @param fieldName JQL 字段名
     * @param module 业务模块
     * @return 数据库列名
     */
    public String mapFieldToColumn(String fieldName, String module) {
        Map<String, String> moduleFields = FIELD_MAPPING.get(module);
        if (moduleFields == null) {
            throw new MSException("不支持的业务模块: " + module);
        }
        
        String columnName = moduleFields.get(fieldName);
        if (columnName == null) {
            throw new MSException("未知的字段名: " + fieldName);
        }
        
        return columnName;
    }
    
    /**
     * 构建条件子句
     * 
     * @param fieldName 字段名
     * @param operator 操作符
     * @param value 值
     * @param module 业务模块
     * @return SQL 条件子句
     */
    private String buildConditionClause(String fieldName, String operator, Object value, String module) {
        String columnName = mapFieldToColumn(fieldName, module);
        
        // TODO: 根据操作符类型生成对应的 SQL
        // 使用参数化查询防止 SQL 注入
        // 例如：columnName + " = ?"
        
        return columnName + " = ?";
    }
}
