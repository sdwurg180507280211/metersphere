package io.metersphere.excel.template;

import io.metersphere.commons.constants.TestCaseConstants;
import io.metersphere.excel.domain.TestCaseExcelData;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用例模板1处理器
 * 标准步骤模板，使用步骤编辑模式（STEP）
 *
 * @author metersphere
 */
public class TestCaseTemplate1Handler extends AbstractTestCaseTemplateHandler {

    @Override
    public String getTemplateFileName() {
        return "testcase-template-1.xlsx";
    }

    @Override
    protected Map<String, String> getSpecificHeadMapping() {
        // 模板1可以添加特有的映射规则
        Map<String, String> mapping = new HashMap<>();
        mapping.put("序号", "ID");
        mapping.put("菜单路径/功能点", "所属模块");
        mapping.put("测试点", "用例名称");
        mapping.put("测试场景/用例/步骤", "步骤描述");
        //mapping.put("预期结果", "预期结果");
        return mapping;
    }

    @Override
    public String getDefaultStepModel() {
        return TestCaseConstants.StepModel.STEP.name();
    }

    @Override
    public String validateData(TestCaseExcelData data) {
        StringBuilder errors = new StringBuilder();

        // 模板1的特定验证规则：要求步骤描述不能为空
        if (StringUtils.isBlank(data.getStepDesc())) {
            errors.append("步骤描述不能为空;");
        }

        // 可以添加更多模板1特有的验证规则

        return errors.toString();
    }

    @Override
    protected void doProcessData(TestCaseExcelData data) {
        // 调用父类方法处理所属模块的通用逻辑（如果为空、只填写空格、/，或不以 / 开头，一律按照 '/其他' 来处理）
        super.doProcessData(data);
        // 模板1可以在此添加特有的数据处理逻辑
    }

    @Override
    public String getDescription() {
        return "用例模板1 - 标准步骤模板（STEP模式）";
    }
}

