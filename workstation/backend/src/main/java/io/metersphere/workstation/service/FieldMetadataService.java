package io.metersphere.workstation.service;

import io.metersphere.workstation.dto.FieldMetadata;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 字段元数据服务
 * 
 * 提供各业务模块的可筛选字段元数据
 * 支持系统字段和自定义字段
 * 
 * @author MeterSphere
 */
@Service
public class FieldMetadataService {
    
    /**
     * 获取字段元数据
     * 
     * 我在做：根据业务模块和项目ID返回可用的筛选字段列表
     * 目的是：前端根据字段元数据动态渲染筛选条件输入控件
     * 如果不这样做：前端无法知道有哪些字段可以筛选
     * 
     * @param module 业务模块
     * @param projectId 项目ID（可选，传入时返回该项目的自定义字段）
     * @return 字段元数据列表
     */
    public Map<String, Object> getFieldMetadata(String module, String projectId) {
        List<FieldMetadata> systemFields = getSystemFields(module);
        List<FieldMetadata> customFields = new ArrayList<>();
        
        // 如果指定了项目ID，加载该项目的自定义字段
        if (projectId != null && !projectId.isEmpty()) {
            customFields = getCustomFields(module, projectId);
        }
        
        return Map.of(
            "systemFields", systemFields,
            "customFields", customFields,
            "fieldGroups", getFieldGroups()
        );
    }
    
    /**
     * 获取系统字段
     * 
     * 我在做：返回各业务模块的系统字段（所有项目都有的字段）
     * 目的是：提供通用的筛选字段
     * 如果不这样做：跨项目查询时无法筛选
     */
    private List<FieldMetadata> getSystemFields(String module) {
        List<FieldMetadata> fields = new ArrayList<>();
        
        // 通用系统字段（所有模块都有）
        fields.addAll(getCommonSystemFields());
        
        // 模块专属字段
        switch (module) {
            case "TEST_CASE":
                fields.addAll(getTestCaseSystemFields());
                break;
            case "ISSUE":
                fields.addAll(getIssueSystemFields());
                break;
            case "TEST_PLAN":
                fields.addAll(getTestPlanSystemFields());
                break;
            case "TEST_CASE_REVIEW":
                fields.addAll(getTestCaseReviewSystemFields());
                break;
        }
        
        return fields;
    }
    
    /**
     * 获取通用系统字段
     */
    private List<FieldMetadata> getCommonSystemFields() {
        List<FieldMetadata> fields = new ArrayList<>();
        
        // 名称/标题
        FieldMetadata name = new FieldMetadata();
        name.setField("name");
        name.setLabel("名称");
        name.setType("text");
        name.setOperators(Arrays.asList("like", "=", "!="));
        name.setGroup("basic");
        fields.add(name);
        
        // 编号/ID
        FieldMetadata num = new FieldMetadata();
        num.setField("num");
        num.setLabel("编号");
        num.setType("text");
        num.setOperators(Arrays.asList("=", "like"));
        num.setGroup("basic");
        fields.add(num);
        
        // 状态
        FieldMetadata status = new FieldMetadata();
        status.setField("status");
        status.setLabel("状态");
        status.setType("select");
        status.setOperators(Arrays.asList("in", "not_in"));
        status.setGroup("basic");
        fields.add(status);
        
        // 创建人
        FieldMetadata createUser = new FieldMetadata();
        createUser.setField("createUser");
        createUser.setLabel("创建人");
        createUser.setType("user");
        createUser.setOperators(Arrays.asList("in"));
        createUser.setMultiple(true);
        createUser.setMaxSelection(10);
        createUser.setGroup("audit");
        fields.add(createUser);
        
        // 创建时间
        FieldMetadata createTime = new FieldMetadata();
        createTime.setField("createTime");
        createTime.setLabel("创建时间");
        createTime.setType("date");
        createTime.setOperators(Arrays.asList("between", ">", "<", ">=", "<="));
        createTime.setGroup("audit");
        fields.add(createTime);
        
        // 更新时间
        FieldMetadata updateTime = new FieldMetadata();
        updateTime.setField("updateTime");
        updateTime.setLabel("更新时间");
        updateTime.setType("date");
        updateTime.setOperators(Arrays.asList("between", ">", "<", ">=", "<="));
        updateTime.setGroup("audit");
        fields.add(updateTime);
        
        return fields;
    }
    
    /**
     * 获取测试用例专属字段
     */
    private List<FieldMetadata> getTestCaseSystemFields() {
        List<FieldMetadata> fields = new ArrayList<>();
        
        // 优先级
        FieldMetadata priority = new FieldMetadata();
        priority.setField("priority");
        priority.setLabel("优先级");
        priority.setType("select");
        priority.setOperators(Arrays.asList("in"));
        priority.setGroup("module");
        
        List<FieldMetadata.Option> priorityOptions = new ArrayList<>();
        priorityOptions.add(createOption("P0", "P0", "danger"));
        priorityOptions.add(createOption("P1", "P1", "warning"));
        priorityOptions.add(createOption("P2", "P2", "info"));
        priorityOptions.add(createOption("P3", "P3", "success"));
        priority.setOptions(priorityOptions);
        fields.add(priority);
        
        // 维护人
        FieldMetadata maintainer = new FieldMetadata();
        maintainer.setField("maintainer");
        maintainer.setLabel("维护人");
        maintainer.setType("user");
        maintainer.setOperators(Arrays.asList("in"));
        maintainer.setMultiple(true);
        maintainer.setMaxSelection(10);
        maintainer.setGroup("module");
        fields.add(maintainer);
        
        // 用例类型
        FieldMetadata type = new FieldMetadata();
        type.setField("type");
        type.setLabel("用例类型");
        type.setType("select");
        type.setOperators(Arrays.asList("in"));
        type.setGroup("module");
        
        List<FieldMetadata.Option> typeOptions = new ArrayList<>();
        typeOptions.add(createOption("functional", "功能用例", null));
        typeOptions.add(createOption("performance", "性能用例", null));
        typeOptions.add(createOption("api", "接口用例", null));
        type.setOptions(typeOptions);
        fields.add(type);
        
        // 用例方式
        FieldMetadata method = new FieldMetadata();
        method.setField("method");
        method.setLabel("用例方式");
        method.setType("select");
        method.setOperators(Arrays.asList("in"));
        method.setGroup("module");
        
        List<FieldMetadata.Option> methodOptions = new ArrayList<>();
        methodOptions.add(createOption("manual", "手动", null));
        methodOptions.add(createOption("auto", "自动", null));
        method.setOptions(methodOptions);
        fields.add(method);
        
        // 评审状态
        FieldMetadata reviewStatus = new FieldMetadata();
        reviewStatus.setField("reviewStatus");
        reviewStatus.setLabel("评审状态");
        reviewStatus.setType("select");
        reviewStatus.setOperators(Arrays.asList("in"));
        reviewStatus.setGroup("module");
        
        List<FieldMetadata.Option> reviewStatusOptions = new ArrayList<>();
        reviewStatusOptions.add(createOption("Prepare", "未评审", "info"));
        reviewStatusOptions.add(createOption("Pass", "通过", "success"));
        reviewStatusOptions.add(createOption("UnPass", "未通过", "danger"));
        reviewStatus.setOptions(reviewStatusOptions);
        fields.add(reviewStatus);
        
        return fields;
    }
    
    /**
     * 获取缺陷专属字段
     */
    private List<FieldMetadata> getIssueSystemFields() {
        List<FieldMetadata> fields = new ArrayList<>();
        
        // 平台
        FieldMetadata platform = new FieldMetadata();
        platform.setField("platform");
        platform.setLabel("缺陷平台");
        platform.setType("select");
        platform.setOperators(Arrays.asList("in"));
        platform.setGroup("module");
        
        List<FieldMetadata.Option> platformOptions = new ArrayList<>();
        platformOptions.add(createOption("Local", "本地", null));
        platformOptions.add(createOption("Jira", "Jira", null));
        platformOptions.add(createOption("Tapd", "Tapd", null));
        platformOptions.add(createOption("Zentao", "禅道", null));
        platform.setOptions(platformOptions);
        fields.add(platform);
        
        return fields;
    }
    
    /**
     * 获取测试计划专属字段
     */
    private List<FieldMetadata> getTestPlanSystemFields() {
        List<FieldMetadata> fields = new ArrayList<>();
        
        // 负责人
        FieldMetadata principal = new FieldMetadata();
        principal.setField("principal");
        principal.setLabel("负责人");
        principal.setType("user");
        principal.setOperators(Arrays.asList("in"));
        principal.setMultiple(true);
        principal.setMaxSelection(10);
        principal.setGroup("module");
        fields.add(principal);
        
        // 计划阶段
        FieldMetadata stage = new FieldMetadata();
        stage.setField("stage");
        stage.setLabel("计划阶段");
        stage.setType("select");
        stage.setOperators(Arrays.asList("in"));
        stage.setGroup("module");
        
        List<FieldMetadata.Option> stageOptions = new ArrayList<>();
        stageOptions.add(createOption("smoke", "冒烟测试", null));
        stageOptions.add(createOption("functional", "功能测试", null));
        stageOptions.add(createOption("integration", "集成测试", null));
        stageOptions.add(createOption("system", "系统测试", null));
        stageOptions.add(createOption("regression", "回归测试", null));
        stage.setOptions(stageOptions);
        fields.add(stage);
        
        return fields;
    }
    
    /**
     * 获取用例评审专属字段
     */
    private List<FieldMetadata> getTestCaseReviewSystemFields() {
        List<FieldMetadata> fields = new ArrayList<>();
        
        // 评审截止日期
        FieldMetadata endTime = new FieldMetadata();
        endTime.setField("endTime");
        endTime.setLabel("截止日期");
        endTime.setType("date");
        endTime.setOperators(Arrays.asList("between", ">", "<", ">=", "<="));
        endTime.setGroup("module");
        fields.add(endTime);
        
        return fields;
    }
    
    /**
     * 获取自定义字段
     * 
     * @param module 业务模块
     * @param projectId 项目ID
     * @return 自定义字段列表
     */
    private List<FieldMetadata> getCustomFields(String module, String projectId) {
        // TODO: 从数据库查询项目的自定义字段
        // 1. 查询 custom_field 表获取字段定义
        // 2. 转换为 FieldMetadata 对象
        // 3. 标记 projectSpecific = true
        return new ArrayList<>();
    }
    
    /**
     * 获取字段分组
     */
    private List<Map<String, String>> getFieldGroups() {
        return Arrays.asList(
            Map.of("key", "basic", "label", "基础信息"),
            Map.of("key", "module", "label", "模块专属"),
            Map.of("key", "audit", "label", "审计追踪"),
            Map.of("key", "custom", "label", "自定义字段")
        );
    }
    
    /**
     * 创建选项对象
     */
    private FieldMetadata.Option createOption(String value, String label, String color) {
        FieldMetadata.Option option = new FieldMetadata.Option();
        option.setValue(value);
        option.setLabel(label);
        option.setColor(color);
        return option;
    }
}
