package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 字段元数据 DTO
 * 
 * 描述可筛选字段的类型、操作符和可选值
 * 用于前端动态渲染筛选条件输入控件
 * 
 * @author MeterSphere
 */
@Getter
@Setter
public class FieldMetadata {
    
    /**
     * 字段名称（对应数据库列名或 combine 的 key）
     * 例如：name, status, priority, createUser
     */
    private String field;
    
    /**
     * 字段显示标签（支持国际化 key）
     * 例如：标题、状态、优先级、创建人
     */
    private String label;
    
    /**
     * 字段类型
     * 可选值：
     * - text: 文本输入框
     * - select: 单选下拉框
     * - multiSelect: 多选下拉框
     * - date: 日期选择器
     * - user: 用户选择器（支持多选）
     * - treeSelect: 树形选择器（如模块选择）
     */
    private String type;
    
    /**
     * 支持的操作符列表
     * 可选值：
     * - like: 模糊匹配（用于文本字段）
     * - =: 等于
     * - !=: 不等于
     * - in: 包含于（用于多选字段）
     * - not_in: 不包含于
     * - between: 区间（用于日期、数字字段）
     * - >: 大于
     * - >=: 大于等于
     * - <: 小于
     * - <=: 小于等于
     * - contains: 包含文本（JQL 语法）
     */
    private List<String> operators;
    
    /**
     * 选项列表（select/multiSelect 类型使用）
     * 例如：状态选项、优先级选项
     */
    private List<Option> options;
    
    /**
     * 字段分组
     * 可选值：
     * - basic: 基础信息
     * - module: 模块专属
     * - audit: 审计追踪
     * - custom: 自定义字段
     */
    private String group;
    
    /**
     * 是否支持多选（user 类型字段使用）
     * true: 可以选择多个用户
     * false: 只能选择单个用户
     */
    private Boolean multiple;
    
    /**
     * 最大选择数量（user 类型字段使用）
     * 例如：10 表示最多选择 10 个用户
     */
    private Integer maxSelection;
    
    /**
     * 是否为项目特有字段（自定义字段标识）
     * true: 该字段仅在单项目模式下可用
     * false: 该字段在跨项目模式下也可用（系统字段或全局自定义字段）
     */
    private Boolean projectSpecific;
    
    /**
     * 字段选项内部类
     */
    @Getter
    @Setter
    public static class Option {
        /**
         * 选项值（存储到数据库的值）
         */
        private String value;
        
        /**
         * 选项显示标签（支持国际化 key）
         */
        private String label;
        
        /**
         * 选项颜色（用于 Tag 显示）
         * 例如：success, warning, danger, info
         */
        private String color;
    }
}
