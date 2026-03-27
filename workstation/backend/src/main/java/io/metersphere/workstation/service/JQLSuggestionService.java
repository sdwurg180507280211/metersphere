package io.metersphere.workstation.service;

import io.metersphere.workstation.dto.JQLSuggestion;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * JQL 智能提示服务
 * 
 * 根据当前输入上下文提供字段名、操作符、值的智能提示
 * 帮助用户快速构建正确的 JQL 查询语句
 * 
 * @author MeterSphere
 */
@Service
public class JQLSuggestionService {
    
    @Resource
    private FieldMetadataService fieldMetadataService;
    
    // 操作符提示
    private static final List<JQLSuggestion> OPERATOR_SUGGESTIONS = Arrays.asList(
        JQLSuggestion.operator("=", "等于"),
        JQLSuggestion.operator("!=", "不等于"),
        JQLSuggestion.operator("~", "模糊匹配"),
        JQLSuggestion.operator(">", "大于"),
        JQLSuggestion.operator(">=", "大于等于"),
        JQLSuggestion.operator("<", "小于"),
        JQLSuggestion.operator("<=", "小于等于"),
        JQLSuggestion.operator("IN", "在列表中"),
        JQLSuggestion.operator("NOT IN", "不在列表中"),
        JQLSuggestion.operator("CONTAINS", "包含")
    );
    
    // 关键字提示
    private static final List<JQLSuggestion> KEYWORD_SUGGESTIONS = Arrays.asList(
        JQLSuggestion.keyword("AND", "逻辑与"),
        JQLSuggestion.keyword("OR", "逻辑或")
    );
    
    // 字段名映射（JQL字段名 -> 中文描述）
    private static final Map<String, Map<String, String>> FIELD_DESCRIPTIONS = new HashMap<>();
    
    static {
        // 测试用例字段描述
        Map<String, String> testCaseFields = new HashMap<>();
        testCaseFields.put("name", "用例名称");
        testCaseFields.put("num", "用例编号");
        testCaseFields.put("status", "用例状态");
        testCaseFields.put("priority", "优先级");
        testCaseFields.put("type", "用例类型");
        testCaseFields.put("method", "测试方式");
        testCaseFields.put("maintainer", "维护人");
        testCaseFields.put("createUser", "创建人");
        testCaseFields.put("updateUser", "更新人");
        testCaseFields.put("createTime", "创建时间");
        testCaseFields.put("updateTime", "更新时间");
        testCaseFields.put("reviewStatus", "评审状态");
        FIELD_DESCRIPTIONS.put("TEST_CASE", testCaseFields);
        
        // 缺陷字段描述
        Map<String, String> issueFields = new HashMap<>();
        issueFields.put("name", "缺陷标题");
        issueFields.put("num", "缺陷编号");
        issueFields.put("status", "缺陷状态");
        issueFields.put("platform", "缺陷平台");
        issueFields.put("assignee", "指派人");
        issueFields.put("createUser", "创建人");
        issueFields.put("createTime", "创建时间");
        issueFields.put("updateTime", "更新时间");
        FIELD_DESCRIPTIONS.put("ISSUE", issueFields);
        
        // 测试计划字段描述
        Map<String, String> testPlanFields = new HashMap<>();
        testPlanFields.put("name", "计划名称");
        testPlanFields.put("status", "计划状态");
        testPlanFields.put("stage", "计划阶段");
        testPlanFields.put("principal", "负责人");
        testPlanFields.put("createUser", "创建人");
        testPlanFields.put("createTime", "创建时间");
        testPlanFields.put("updateTime", "更新时间");
        FIELD_DESCRIPTIONS.put("TEST_PLAN", testPlanFields);
        
        // 用例评审字段描述
        Map<String, String> reviewFields = new HashMap<>();
        reviewFields.put("name", "评审名称");
        reviewFields.put("status", "评审状态");
        reviewFields.put("createUser", "创建人");
        reviewFields.put("createTime", "创建时间");
        reviewFields.put("updateTime", "更新时间");
        reviewFields.put("endTime", "截止时间");
        FIELD_DESCRIPTIONS.put("TEST_CASE_REVIEW", reviewFields);
    }
    
    // 字段值选项（常用值）
    private static final Map<String, Map<String, List<String>>> FIELD_VALUE_OPTIONS = new HashMap<>();
    
    static {
        // 测试用例字段值选项
        Map<String, List<String>> testCaseOptions = new HashMap<>();
        testCaseOptions.put("status", Arrays.asList("Prepare", "Underway", "Completed", "Pass", "Failure"));
        testCaseOptions.put("priority", Arrays.asList("P0", "P1", "P2", "P3", "P4"));
        testCaseOptions.put("type", Arrays.asList("functional", "performance", "api", "ui"));
        testCaseOptions.put("method", Arrays.asList("manual", "auto"));
        testCaseOptions.put("reviewStatus", Arrays.asList("Prepare", "Pass", "UnPass"));
        FIELD_VALUE_OPTIONS.put("TEST_CASE", testCaseOptions);
        
        // 缺陷字段值选项
        Map<String, List<String>> issueOptions = new HashMap<>();
        issueOptions.put("status", Arrays.asList("new", "resolved", "closed", "rejected"));
        issueOptions.put("platform", Arrays.asList("Jira", "Tapd", "Zentao", "Local"));
        FIELD_VALUE_OPTIONS.put("ISSUE", issueOptions);
        
        // 测试计划字段值选项
        Map<String, List<String>> testPlanOptions = new HashMap<>();
        testPlanOptions.put("status", Arrays.asList("Prepare", "Underway", "Completed", "Archived"));
        testPlanOptions.put("stage", Arrays.asList("smoke", "system", "regression"));
        FIELD_VALUE_OPTIONS.put("TEST_PLAN", testPlanOptions);
        
        // 用例评审字段值选项
        Map<String, List<String>> reviewOptions = new HashMap<>();
        reviewOptions.put("status", Arrays.asList("Prepare", "Underway", "Completed", "Archived"));
        FIELD_VALUE_OPTIONS.put("TEST_CASE_REVIEW", reviewOptions);
    }
    
    /**
     * 获取 JQL 智能提示
     * 
     * 我在做：分析当前输入上下文，返回合适的智能提示
     * 目的是：帮助用户快速构建 JQL 查询
     * 如果不这样做：用户需要记住所有字段名和操作符
     * 
     * @param context 当前输入的 JQL 文本
     * @param module 业务模块
     * @param cursorPosition 光标位置
     * @return 智能提示列表
     */
    public List<JQLSuggestion> getSuggestions(String context, String module, Integer cursorPosition) {
        List<JQLSuggestion> suggestions = new ArrayList<>();
        
        // 如果上下文为空，返回字段名提示
        if (StringUtils.isBlank(context)) {
            return getFieldSuggestions(module);
        }
        
        // 获取光标前的文本
        String beforeCursor = context;
        if (cursorPosition != null && cursorPosition < context.length()) {
            beforeCursor = context.substring(0, cursorPosition);
        }
        
        // 分析上下文，确定提示类型
        SuggestionContext ctx = analyzeContext(beforeCursor);
        
        switch (ctx.getType()) {
            case FIELD:
                // 提示字段名
                suggestions = getFieldSuggestions(module);
                // 如果有部分输入，过滤匹配的字段
                if (StringUtils.isNotBlank(ctx.getPartialInput())) {
                    suggestions = filterSuggestions(suggestions, ctx.getPartialInput());
                }
                break;
                
            case OPERATOR:
                // 提示操作符
                suggestions = new ArrayList<>(OPERATOR_SUGGESTIONS);
                break;
                
            case VALUE:
                // 提示值
                suggestions = getValueSuggestions(module, ctx.getFieldName());
                break;
                
            case KEYWORD:
                // 提示关键字（AND, OR）
                suggestions = new ArrayList<>(KEYWORD_SUGGESTIONS);
                break;
                
            default:
                // 默认提示字段名
                suggestions = getFieldSuggestions(module);
        }
        
        // 按优先级排序
        suggestions.sort((a, b) -> b.getPriority().compareTo(a.getPriority()));
        
        return suggestions;
    }
    
    /**
     * 获取字段名提示
     */
    private List<JQLSuggestion> getFieldSuggestions(String module) {
        List<JQLSuggestion> suggestions = new ArrayList<>();
        
        Map<String, String> fieldDescriptions = FIELD_DESCRIPTIONS.get(module);
        if (fieldDescriptions != null) {
            for (Map.Entry<String, String> entry : fieldDescriptions.entrySet()) {
                suggestions.add(JQLSuggestion.field(entry.getKey(), entry.getValue()));
            }
        }
        
        return suggestions;
    }
    
    /**
     * 获取值提示
     */
    private List<JQLSuggestion> getValueSuggestions(String module, String fieldName) {
        List<JQLSuggestion> suggestions = new ArrayList<>();
        
        Map<String, List<String>> moduleOptions = FIELD_VALUE_OPTIONS.get(module);
        if (moduleOptions != null && moduleOptions.containsKey(fieldName)) {
            List<String> values = moduleOptions.get(fieldName);
            for (String value : values) {
                suggestions.add(JQLSuggestion.value(value, value));
            }
        }
        
        return suggestions;
    }
    
    /**
     * 过滤提示（根据部分输入）
     */
    private List<JQLSuggestion> filterSuggestions(List<JQLSuggestion> suggestions, String partialInput) {
        List<JQLSuggestion> filtered = new ArrayList<>();
        String lowerInput = partialInput.toLowerCase();
        
        for (JQLSuggestion suggestion : suggestions) {
            if (suggestion.getValue().toLowerCase().startsWith(lowerInput)) {
                filtered.add(suggestion);
            }
        }
        
        return filtered.isEmpty() ? suggestions : filtered;
    }
    
    /**
     * 分析上下文
     * 
     * 我在做：分析光标前的文本，确定当前应该提示什么类型的内容
     * 目的是：提供精准的智能提示
     * 如果不这样做：提示内容可能不符合用户期望
     */
    private SuggestionContext analyzeContext(String beforeCursor) {
        SuggestionContext ctx = new SuggestionContext();
        
        // 去除首尾空格
        String trimmed = beforeCursor.trim();
        
        // 如果为空，提示字段名
        if (trimmed.isEmpty()) {
            ctx.setType(SuggestionType.FIELD);
            return ctx;
        }
        
        // 如果以 AND 或 OR 结尾，提示字段名
        if (trimmed.endsWith("AND") || trimmed.endsWith("OR")) {
            ctx.setType(SuggestionType.FIELD);
            return ctx;
        }
        
        // 如果包含 = 或其他操作符，但没有值，提示值
        if (trimmed.matches(".*[=!~><]\\s*$")) {
            ctx.setType(SuggestionType.VALUE);
            // 提取字段名
            String fieldName = extractFieldName(trimmed);
            ctx.setFieldName(fieldName);
            return ctx;
        }
        
        // 如果包含 IN 或 NOT IN，提示值
        if (trimmed.matches(".*\\b(IN|NOT\\s+IN)\\s*\\(?\\s*$")) {
            ctx.setType(SuggestionType.VALUE);
            String fieldName = extractFieldName(trimmed);
            ctx.setFieldName(fieldName);
            return ctx;
        }
        
        // 如果最后一个词是完整的值（带引号或不带引号），提示关键字
        if (trimmed.matches(".*[\"'].*[\"']\\s*$") || trimmed.matches(".*\\w+\\s*$")) {
            // 检查是否在括号内（IN 列表）
            if (isInParentheses(trimmed)) {
                ctx.setType(SuggestionType.VALUE);
                String fieldName = extractFieldName(trimmed);
                ctx.setFieldName(fieldName);
            } else {
                ctx.setType(SuggestionType.KEYWORD);
            }
            return ctx;
        }
        
        // 如果最后一个词是部分字段名，提示字段名
        String lastWord = getLastWord(trimmed);
        if (StringUtils.isNotBlank(lastWord) && !lastWord.matches("[\"'].*")) {
            ctx.setType(SuggestionType.FIELD);
            ctx.setPartialInput(lastWord);
            return ctx;
        }
        
        // 默认提示字段名
        ctx.setType(SuggestionType.FIELD);
        return ctx;
    }
    
    /**
     * 提取字段名
     */
    private String extractFieldName(String text) {
        // 简单实现：提取最后一个字段名
        String[] tokens = text.split("\\s+");
        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i];
            if (token.matches("\\w+") && !token.matches("(?i)(AND|OR|IN|NOT|CONTAINS)")) {
                return token;
            }
        }
        return null;
    }
    
    /**
     * 获取最后一个词
     */
    private String getLastWord(String text) {
        String[] tokens = text.split("\\s+");
        return tokens.length > 0 ? tokens[tokens.length - 1] : "";
    }
    
    /**
     * 检查是否在括号内
     */
    private boolean isInParentheses(String text) {
        int openCount = 0;
        for (char c : text.toCharArray()) {
            if (c == '(') openCount++;
            if (c == ')') openCount--;
        }
        return openCount > 0;
    }
    
    /**
     * 提示类型枚举
     */
    private enum SuggestionType {
        FIELD,      // 字段名
        OPERATOR,   // 操作符
        VALUE,      // 值
        KEYWORD     // 关键字（AND, OR）
    }
    
    /**
     * 提示上下文
     */
    private static class SuggestionContext {
        private SuggestionType type;
        private String fieldName;
        private String partialInput;
        
        public SuggestionType getType() { return type; }
        public void setType(SuggestionType type) { this.type = type; }
        
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        
        public String getPartialInput() { return partialInput; }
        public void setPartialInput(String partialInput) { this.partialInput = partialInput; }
    }
}
