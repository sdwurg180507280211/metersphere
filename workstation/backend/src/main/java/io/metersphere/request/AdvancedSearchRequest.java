package io.metersphere.request;

import io.metersphere.request.BaseQueryRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 高级检索请求 DTO
 * 
 * 继承 BaseQueryRequest 复用现有的 filters、combine、orders 机制
 * 扩展支持跨工作空间、跨项目查询和 JQL 查询语法
 * 
 * @author MeterSphere
 */
@Getter
@Setter
public class AdvancedSearchRequest extends BaseQueryRequest {
    
    /**
     * 业务模块类型
     * 可选值：TEST_CASE（测试用例）、ISSUE（缺陷）、TEST_PLAN（测试计划）、TEST_CASE_REVIEW（用例评审）
     */
    private String module;
    
    /**
     * 工作空间ID列表
     * 支持跨工作空间查询，为空时查询用户有权限的所有工作空间
     */
    private List<String> workspaceIds;
    
    /**
     * 项目ID列表
     * 支持跨项目查询
     * - 选择多个项目时：仅支持系统字段和全局自定义字段筛选
     * - 选择单个项目时：支持系统字段 + 该项目的全部自定义字段筛选
     */
    private List<String> projectIds;
    
    /**
     * 是否使用 JQL 查询模式
     * true: 使用 jql 字段进行查询
     * false: 使用传统的 combine/filters 机制（默认）
     */
    private Boolean useJQL = false;
    
    /**
     * JQL 查询字符串
     * 类似 Jira JQL 的查询语法，例如：
     * project = "电商平台" AND status IN ("Pass", "Prepare") AND priority = "P0"
     * 
     * 仅在 useJQL = true 时生效
     */
    private String jql;
    
    /**
     * JQL 转换后的 SQL WHERE 子句
     * 由 Service 层设置，Mapper 层使用
     * 内部字段，不对外暴露
     */
    private String jqlWhereClause;
    
    /**
     * 排序字段名称
     * 例如：update_time, create_time, priority
     */
    private String sortField;
    
    /**
     * 排序方向
     * 可选值：ASC（升序）、DESC（降序）
     */
    private String sortOrder;
}
