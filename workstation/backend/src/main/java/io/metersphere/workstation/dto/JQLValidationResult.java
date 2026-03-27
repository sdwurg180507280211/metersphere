package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * JQL 语法验证结果 DTO
 * 
 * 用于返回 JQL 查询语句的语法验证结果
 * 包含验证状态、错误信息和修复建议
 * 
 * @author MeterSphere
 */
@Getter
@Setter
public class JQLValidationResult {
    
    /**
     * 验证是否通过
     * true: JQL 语法正确
     * false: JQL 语法错误
     */
    private Boolean valid;
    
    /**
     * 验证消息
     * 成功时：提示语法正确
     * 失败时：详细的错误描述
     * 例如："JQL 语法正确" 或 "第 1 行第 10 列：未识别的字段名 'xxx'"
     */
    private String message;
    
    /**
     * 修复建议列表
     * 当验证失败时，提供可能的修复方案
     * 例如：["您是否想输入 'status'？", "请检查字段名拼写"]
     */
    private List<String> suggestions;
    
    /**
     * 错误位置 - 行号（从 1 开始）
     */
    private Integer errorLine;
    
    /**
     * 错误位置 - 列号（从 1 开始）
     */
    private Integer errorColumn;
    
    /**
     * 构造函数 - 创建验证成功的结果
     * 
     * @return 验证成功的结果对象
     */
    public static JQLValidationResult success() {
        JQLValidationResult result = new JQLValidationResult();
        result.setValid(true);
        result.setMessage("JQL 语法正确");
        result.setSuggestions(new ArrayList<>());
        return result;
    }
    
    /**
     * 构造函数 - 创建验证失败的结果
     * 
     * @param message 错误消息
     * @param line 错误行号
     * @param column 错误列号
     * @return 验证失败的结果对象
     */
    public static JQLValidationResult error(String message, Integer line, Integer column) {
        JQLValidationResult result = new JQLValidationResult();
        result.setValid(false);
        result.setMessage(message);
        result.setErrorLine(line);
        result.setErrorColumn(column);
        result.setSuggestions(new ArrayList<>());
        return result;
    }
    
    /**
     * 添加修复建议
     * 
     * @param suggestion 建议内容
     */
    public void addSuggestion(String suggestion) {
        if (this.suggestions == null) {
            this.suggestions = new ArrayList<>();
        }
        this.suggestions.add(suggestion);
    }
}
