package io.metersphere.workstation.service;

import io.metersphere.workstation.dto.FieldMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 字段元数据服务
 * 
 * 为高级检索功能提供字段元数据
 * 根据业务模块返回系统字段和自定义字段
 * 
 * @author MeterSphere
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class FieldMetadataService {
    
    /**
     * 获取字段元数据
     * 
     * 我在做：根据业务模块和项目ID返回可用的筛选字段
     * 目的是：前端根据字段元数据动态渲染筛选条件输入控件
     * 如果不这样做：前端无法知道有哪些字段可以筛选
     * 
     * @param module 业务模块（TEST_CASE, ISSUE, TEST_PLAN, TEST_CASE_REVIEW）
     * @param projectId 项目ID（可选，传入时返回该项目的自定义字段）
     * @return 字段元数据（包含系统字段、自定义字段、字段分组）
     */
    public Map<String, Object> getFieldMetadata(String module, String projectId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取系统字段
        List<FieldMetadata> systemFields = getSystemFields(module);
        result.put("systemFields", systemFields);
        
        // 获取自定义字段
        List<FieldMetadata> customFields = new ArrayList<>();
        if (projectId != null && !projectId.isEmpty()) {
            // 单项目模式：返回该项目的自定义字段
            customFields = getCustomFields(module, projectId);
        } else {
            // 跨项目模式：只返回全局自定义字段
            customFields = getGlobalCustomFields(module);
        }
        result.put("customFields", customFields);
        
        // 字段分组
        List<Map<String, String>> fieldGroups = Arrays.asList(
            createGroup("basic", "基础信息"),
            createGroup("module", "模块专属"),
            createGroup("audit", "审计追踪"),
            createGroup("custom", "自定义字段")
        );
        result.put("fieldGroups", fieldGroups);
        
        return result;
    }
    
    /**
     * 获取系统字段
     * 
     * @param module 业务模块
     * @return 系统字段列表
     */
    private List<FieldMetadata> getSystemFields(String module) {
        List<FieldMetadata> fields = new ArrayList<>();
        
        // 通用系统字段（所有模块都有）
        fields.add(createField("name", "名称", "text", Arrays.asList("like", "=", "!="), "basic", null));
        fields.add(createField("num", "编号", "text", Arrays.asList("=", "like"), "basic", null));
        fields.add(createField("status", "状态", "select", Arrays.asList("in", "not_in"), "basic", getStatusOptions(module)));
        fields.add(createField("createUser", "创建人", "user", Arrays.asList("in"), "audit", null));
        fields.add(createField("updateUser", "最后更新人", "user", Arrays.asList("in"), "audit", null));
        fields.add(createField("createTime", "创建时间", "date", Arrays.asList("between", ">", "<"), "audit", null));
        fields.add(createField("updateTime", "更新时间", "date", Arrays.asList("between", ">", "<"), "audit", null));
        
        // 根据业务模块添加专属字段
        switch (module) {
            case "TEST_CASE":
                fields.add(createField("priority", "优先级", "select", Arrays.asList("in"), "module", getPriorityOptions()));
                fields.add(createField("maintainer", "维护人", "user", Arrays.asList("in"), "module", null));
                fields.add(createField("type", "用例类型", "select", Arrays.asList("in"), "module", getTestCaseTypeOptions()));
                fields.add(createField("method", "用例方式", "select", Arrays.asList("in"), "module", getTestCaseMethodOptions()));
                fields.add(createField("nodeId", "所属模块", "treeSelect", Arrays.asList("in"), "module", null));
                fields.add(createField("reviewStatus", "评审状态", "select", Arrays.asList("in"), "module", getReviewStatusOptions()));
                break;
                
            case "ISSUE":
                fields.add(createField("assignee", "指派给", "user", Arrays.asList("in"), "module", null));
                fields.add(createField("severity", "严重程度", "select", Arrays.asList("in"), "module", getSeverityOptions()));
                fields.add(createField("platform", "缺陷平台", "select", Arrays.asList("in"), "module", getPlatformOptions()));
                fields.add(createField("resourceId", "关联用例ID", "text", Arrays.asList("="), "module", null));
                break;
                
            case "TEST_PLAN":
                fields.add(createField("principal", "负责人", "user", Arrays.asList("in"), "module", null));
                fields.add(createField("stage", "计划阶段", "select", Arrays.asList("in"), "module", getTestPlanStageOptions()));
                fields.add(createField("plannedStartTime", "计划开始日期", "date", Arrays.asList("between", ">", "<"), "module", null));
                fields.add(createField("plannedEndTime", "计划结束日期", "date", Arrays.asList("between", ">", "<"), "module", null));
                fields.add(createField("actualStartTime", "实际开始日期", "date", Arrays.asList("between", ">", "<"), "module", null));
                fields.add(createField("actualEndTime", "实际结束日期", "date", Arrays.asList("between", ">", "<"), "module", null));
                break;
                
            case "TEST_CASE_REVIEW":
                fields.add(createField("reviewer", "评审人", "user", Arrays.asList("in"), "module", null));
                fields.add(createField("reviewStatus", "评审状态", "select", Arrays.asList("in"), "module", getReviewStatusOptions()));
                fields.add(createField("endTime", "评审截止日期", "date", Arrays.asList("between", ">", "<"), "module", null));
                break;
        }
        
        return fields;
    }
    
    /**
     * 获取自定义字段
     * 
     * 我在做：查询指定项目的自定义字段
     * 目的是：在单项目模式下，用户可以使用该项目的自定义字段进行筛选
     * 如果不这样做：用户无法使用项目特有的自定义字段
     * 
     * @param module 业务模块
     * @param projectId 项目ID
     * @return 自定义字段列表
     */
    private List<FieldMetadata> getCustomFields(String module, String projectId) {
        List<FieldMetadata> fields = new ArrayList<>();
        
        // TODO: 实现数据库查询
        // 查询逻辑：
        // 1. 从 custom_field 表查询 project_id = projectId 且 scene = module 的字段
        // 2. 或者通过 custom_field_template 关联查询项目模板的自定义字段
        // 3. 解析 options 字段（JSON 格式）转换为 FieldMetadata.Option 列表
        // 4. 根据 type 字段设置对应的操作符
        
        // 示例 SQL（需要在 Mapper 中实现）：
        // SELECT cf.id, cf.name, cf.type, cf.options
        // FROM custom_field cf
        // WHERE cf.project_id = #{projectId}
        //   AND cf.scene = #{scene}
        //   AND cf.system = 0
        // ORDER BY cf.create_time DESC
        
        return fields;
    }
    
    /**
     * 获取全局自定义字段
     * 
     * 我在做：查询全局自定义字段（global=1）
     * 目的是：在跨项目模式下，用户可以使用全局自定义字段进行筛选
     * 如果不这样做：跨项目查询时无法使用任何自定义字段
     * 
     * @param module 业务模块
     * @return 全局自定义字段列表
     */
    private List<FieldMetadata> getGlobalCustomFields(String module) {
        List<FieldMetadata> fields = new ArrayList<>();
        
        // TODO: 实现数据库查询
        // 查询逻辑：
        // 1. 从 custom_field 表查询 global = 1 且 scene = module 的字段
        // 2. 解析 options 字段（JSON 格式）转换为 FieldMetadata.Option 列表
        // 3. 根据 type 字段设置对应的操作符
        
        // 示例 SQL（需要在 Mapper 中实现）：
        // SELECT cf.id, cf.name, cf.type, cf.options
        // FROM custom_field cf
        // WHERE cf.global = 1
        //   AND cf.scene = #{scene}
        //   AND cf.system = 0
        // ORDER BY cf.create_time DESC
        
        return fields;
    }
    
    /**
     * 创建字段元数据对象
     */
    private FieldMetadata createField(String field, String label, String type, 
                                     List<String> operators, String group, List<FieldMetadata.Option> options) {
        FieldMetadata metadata = new FieldMetadata();
        metadata.setField(field);
        metadata.setLabel(label);
        metadata.setType(type);
        metadata.setOperators(operators);
        metadata.setGroup(group);
        metadata.setOptions(options);
        
        // 用户类型字段的特殊配置
        if ("user".equals(type)) {
            metadata.setMultiple(true);
            metadata.setMaxSelection(10);
        }
        
        return metadata;
    }
    
    /**
     * 创建字段分组
     */
    private Map<String, String> createGroup(String key, String label) {
        Map<String, String> group = new HashMap<>();
        group.put("key", key);
        group.put("label", label);
        return group;
    }
    
    /**
     * 创建选项
     */
    private FieldMetadata.Option createOption(String value, String label) {
        FieldMetadata.Option option = new FieldMetadata.Option();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }
    
    // ==================== 选项数据 ====================
    
    /**
     * 获取状态选项（根据业务模块）
     */
    private List<FieldMetadata.Option> getStatusOptions(String module) {
        List<FieldMetadata.Option> options = new ArrayList<>();
        switch (module) {
            case "TEST_CASE":
                options.add(createOption("Prepare", "未开始"));
                options.add(createOption("Pass", "通过"));
                options.add(createOption("Failure", "失败"));
                options.add(createOption("Blocking", "阻塞"));
                options.add(createOption("Skip", "跳过"));
                break;
            case "ISSUE":
                options.add(createOption("new", "新建"));
                options.add(createOption("resolved", "已解决"));
                options.add(createOption("closed", "已关闭"));
                break;
            case "TEST_PLAN":
                options.add(createOption("Prepare", "未开始"));
                options.add(createOption("Underway", "进行中"));
                options.add(createOption("Completed", "已完成"));
                options.add(createOption("Archived", "已归档"));
                break;
        }
        return options;
    }
    
    /**
     * 获取优先级选项
     */
    private List<FieldMetadata.Option> getPriorityOptions() {
        return Arrays.asList(
            createOption("P0", "P0"),
            createOption("P1", "P1"),
            createOption("P2", "P2"),
            createOption("P3", "P3")
        );
    }
    
    /**
     * 获取测试用例类型选项
     */
    private List<FieldMetadata.Option> getTestCaseTypeOptions() {
        return Arrays.asList(
            createOption("functional", "功能测试"),
            createOption("performance", "性能测试"),
            createOption("api", "接口测试")
        );
    }
    
    /**
     * 获取测试用例方式选项
     */
    private List<FieldMetadata.Option> getTestCaseMethodOptions() {
        return Arrays.asList(
            createOption("manual", "手动"),
            createOption("auto", "自动")
        );
    }
    
    /**
     * 获取评审状态选项
     */
    private List<FieldMetadata.Option> getReviewStatusOptions() {
        return Arrays.asList(
            createOption("Prepare", "未评审"),
            createOption("Pass", "通过"),
            createOption("UnPass", "未通过")
        );
    }
    
    /**
     * 获取严重程度选项
     */
    private List<FieldMetadata.Option> getSeverityOptions() {
        return Arrays.asList(
            createOption("blocker", "阻断"),
            createOption("critical", "严重"),
            createOption("major", "一般"),
            createOption("minor", "轻微"),
            createOption("trivial", "提示")
        );
    }
    
    /**
     * 获取缺陷平台选项
     */
    private List<FieldMetadata.Option> getPlatformOptions() {
        return Arrays.asList(
            createOption("Local", "本地"),
            createOption("Jira", "Jira"),
            createOption("Tapd", "Tapd"),
            createOption("Zentao", "禅道")
        );
    }
    
    /**
     * 获取测试计划阶段选项
     */
    private List<FieldMetadata.Option> getTestPlanStageOptions() {
        return Arrays.asList(
            createOption("smoke", "冒烟测试"),
            createOption("system", "系统测试"),
            createOption("regression", "回归测试"),
            createOption("uat", "验收测试")
        );
    }
}
