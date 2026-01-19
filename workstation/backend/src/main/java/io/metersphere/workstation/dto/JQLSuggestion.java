package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * JQL 智能提示 DTO
 * 
 * 用于 JQL 编辑器的自动补全功能
 * 根据当前输入上下文提供字段名、操作符、值的智能提示
 * 
 * @author MeterSphere
 */
@Getter
@Setter
public class JQLSuggestion {
    
    /**
     * 提示类型
     * 可选值：
     * - field: 字段名提示
     * - operator: 操作符提示
     * - value: 值提示
     * - keyword: 关键字提示（AND, OR, IN 等）
     */
    private String type;
    
    /**
     * 提示值
     * 例如：status, priority, =, IN, "Pass"
     */
    private String value;
    
    /**
     * 提示描述
     * 例如："状态字段"、"等于操作符"、"通过状态"
     */
    private String description;
    
    /**
     * 插入文本
     * 用户选择该提示后实际插入到编辑器的文本
     * 例如：status = 、priority IN ()
     */
    private String insertText;
    
    /**
     * 提示优先级（用于排序）
     * 数值越大优先级越高，越靠前显示
     * 默认为 0
     */
    private Integer priority;
    
    /**
     * 构造函数 - 创建字段提示
     * 
     * @param field 字段名
     * @param description 字段描述
     * @return 字段提示对象
     */
    public static JQLSuggestion field(String field, String description) {
        JQLSuggestion suggestion = new JQLSuggestion();
        suggestion.setType("field");
        suggestion.setValue(field);
        suggestion.setDescription(description);
        suggestion.setInsertText(field);
        suggestion.setPriority(10);
        return suggestion;
    }
    
    /**
     * 构造函数 - 创建操作符提示
     * 
     * @param operator 操作符
     * @param description 操作符描述
     * @return 操作符提示对象
     */
    public static JQLSuggestion operator(String operator, String description) {
        JQLSuggestion suggestion = new JQLSuggestion();
        suggestion.setType("operator");
        suggestion.setValue(operator);
        suggestion.setDescription(description);
        suggestion.setInsertText(operator + " ");
        suggestion.setPriority(8);
        return suggestion;
    }
    
    /**
     * 构造函数 - 创建值提示
     * 
     * @param value 值
     * @param description 值描述
     * @return 值提示对象
     */
    public static JQLSuggestion value(String value, String description) {
        JQLSuggestion suggestion = new JQLSuggestion();
        suggestion.setType("value");
        suggestion.setValue(value);
        suggestion.setDescription(description);
        // 如果值包含空格或特殊字符，用引号包裹
        if (value.contains(" ") || value.contains("-")) {
            suggestion.setInsertText("\"" + value + "\"");
        } else {
            suggestion.setInsertText(value);
        }
        suggestion.setPriority(5);
        return suggestion;
    }
    
    /**
     * 构造函数 - 创建关键字提示
     * 
     * @param keyword 关键字
     * @param description 关键字描述
     * @return 关键字提示对象
     */
    public static JQLSuggestion keyword(String keyword, String description) {
        JQLSuggestion suggestion = new JQLSuggestion();
        suggestion.setType("keyword");
        suggestion.setValue(keyword);
        suggestion.setDescription(description);
        suggestion.setInsertText(keyword + " ");
        suggestion.setPriority(7);
        return suggestion;
    }
}
