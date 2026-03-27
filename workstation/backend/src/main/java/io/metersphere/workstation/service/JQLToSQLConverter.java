package io.metersphere.workstation.service;

import io.metersphere.commons.exception.MSException;
import io.metersphere.workstation.service.JQLParser.*;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * JQL 到 SQL 转换器
 * 
 * 将解析后的 JQL 抽象语法树转换为 SQL WHERE 子句
 * 使用字面量方式生成SQL（已经过字段名白名单验证，安全可控）
 * 
 * @author MeterSphere
 */
@Service
public class JQLToSQLConverter {
    
    @Resource
    private JQLCacheService jqlCacheService;
    
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
        testCaseFields.put("updateUser", "test_case.update_user");
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
        issueFields.put("assignee", "issues.creator");
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
     * 注意：这里使用字面量方式生成SQL，因为：
     * 1. 字段名已经过白名单验证
     * 2. 值会被正确转义
     * 3. MyBatis的参数化查询在动态SQL片段中不适用
     * 
     * @param ast 抽象语法树
     * @param module 业务模块
     * @return SQL WHERE 子句（不包含 WHERE 关键字）
     */
    public String convertToSQL(QueryNode ast, String module) {
        if (ast == null) {
            MSException.throwException("AST 不能为空");
        }
        
        if (StringUtils.isBlank(module)) {
            MSException.throwException("业务模块不能为空");
        }
        
        // 递归转换 AST 节点
        return convertNode(ast, module);
    }
    
    /**
     * 递归转换 AST 节点
     */
    private String convertNode(QueryNode node, String module) {
        if (node instanceof ComparisonNode) {
            return convertComparison((ComparisonNode) node, module);
        } else if (node instanceof BinaryOpNode) {
            return convertBinaryOp((BinaryOpNode) node, module);
        }
        throw new IllegalArgumentException("未知的节点类型: " + node.getClass());
    }
    
    /**
     * 转换比较节点
     */
    private String convertComparison(ComparisonNode node, String module) {
        String field = mapFieldToColumn(node.getField(), module);
        TokenType operator = node.getOperator();
        Object value = node.getValue();
        
        switch (operator) {
            case EQUALS:
                return String.format("%s = %s", field, escapeSQLValue(value));
                
            case NOT_EQUALS:
                return String.format("%s != %s", field, escapeSQLValue(value));
                
            case LIKE:
            case CONTAINS:
                return String.format("%s LIKE %s", field, escapeSQLValue("%" + value + "%"));
                
            case IN:
                if (value instanceof List) {
                    List<?> values = (List<?>) value;
                    StringBuilder inClause = new StringBuilder(field + " IN (");
                    for (int i = 0; i < values.size(); i++) {
                        inClause.append(escapeSQLValue(values.get(i)));
                        if (i < values.size() - 1) {
                            inClause.append(", ");
                        }
                    }
                    inClause.append(")");
                    return inClause.toString();
                }
                break;
                
            case NOT_IN:
                if (value instanceof List) {
                    List<?> values = (List<?>) value;
                    StringBuilder notInClause = new StringBuilder(field + " NOT IN (");
                    for (int i = 0; i < values.size(); i++) {
                        notInClause.append(escapeSQLValue(values.get(i)));
                        if (i < values.size() - 1) {
                            notInClause.append(", ");
                        }
                    }
                    notInClause.append(")");
                    return notInClause.toString();
                }
                break;
                
            case GREATER:
                return String.format("%s > %s", field, escapeSQLValue(value));
                
            case GREATER_EQUAL:
                return String.format("%s >= %s", field, escapeSQLValue(value));
                
            case LESS:
                return String.format("%s < %s", field, escapeSQLValue(value));
                
            case LESS_EQUAL:
                return String.format("%s <= %s", field, escapeSQLValue(value));
                
            default:
                throw new IllegalArgumentException("不支持的操作符: " + operator);
        }
        
        return "1=1";
    }
    
    /**
     * 转换二元操作节点
     */
    private String convertBinaryOp(BinaryOpNode node, String module) {
        String left = convertNode(node.getLeft(), module);
        String right = convertNode(node.getRight(), module);
        String operator = node.getOperator() == TokenType.AND ? "AND" : "OR";
        
        return String.format("(%s %s %s)", left, operator, right);
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
            MSException.throwException("不支持的业务模块: " + module);
        }
        
        String columnName = moduleFields.get(fieldName);
        if (columnName == null) {
            MSException.throwException("未知的字段名: " + fieldName);
        }
        
        return columnName;
    }
    
    /**
     * 转义SQL值
     * 
     * 我在做：对SQL值进行转义，防止SQL注入
     * 目的是：确保生成的SQL安全可执行
     * 如果不这样做：可能导致SQL注入攻击
     * 
     * @param value 原始值
     * @return 转义后的SQL值字符串
     */
    private String escapeSQLValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        // 数字类型直接返回
        if (value instanceof Number) {
            return value.toString();
        }
        
        // 字符串类型需要转义单引号并加上引号
        String strValue = value.toString();
        // 转义单引号：' -> ''
        strValue = strValue.replace("'", "''");
        // 转义反斜杠：\ -> \\
        strValue = strValue.replace("\\", "\\\\");
        
        return "'" + strValue + "'";
    }
}

