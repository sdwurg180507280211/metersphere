package io.metersphere.excel.template;

import io.metersphere.commons.constants.TestCaseConstants;
import io.metersphere.excel.domain.TestCaseExcelData;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用例模板3处理器
 * 扩展模板，支持更多自定义字段和灵活的配置
 *
 * @author metersphere
 */
public class TestCaseTemplate3Handler extends AbstractTestCaseTemplateHandler {

    @Override
    public String getTemplateFileName() {
        return "testcase-template-3.xlsx";
    }

    @Override
    protected Map<String, String> getSpecificHeadMapping() {
        // 模板3可以支持更多扩展字段
        // 只映射那些Excel表头名称与标准字段名称不一致的字段
        // 标准字段名称（如"所属模块"、"预期结果"、"备注"）可以直接通过 excelHeadToFieldNameDic 匹配，无需在此填写
        Map<String, String> mapping = new HashMap<>();
        mapping.put("测试用例编号", "ID");        // Excel中是"测试用例编号"，标准字段是"ID"
        mapping.put("用例简述", "用例名称");      // Excel中是"用例简述"，标准字段是"用例名称"
        mapping.put("预置条件", "前置条件");      // Excel中是"预置条件"，标准字段是"前置条件"
        mapping.put("测试步骤", "步骤描述");      // Excel中是"测试步骤"，标准字段是"步骤描述"
        // "所属模块"、"预期结果"、"备注" 这些表头名称与标准字段名称一致，可以直接通过 excelHeadToFieldNameDic 匹配
        return mapping;
    }

    @Override
    public String getDefaultStepModel() {
        return TestCaseConstants.StepModel.STEP.name();
    }

    @Override
    public String validateData(TestCaseExcelData data) {
        // 可以添加模板3特有的验证规则
        // 例如：验证特定的自定义字段组合
        return "";
    }

    @Override
    protected void doProcessData(TestCaseExcelData data) {
        // 调用父类方法处理所属模块的通用逻辑（如果为空、只填写空格、/，或不以 / 开头，一律按照 '/其他' 来处理）
        super.doProcessData(data);
        // 可以添加模板3特有的数据处理逻辑
        // 例如：复杂的数据转换、默认值填充等
    }

    @Override
    public String getDescription() {
        return "用例模板3 - 扩展模板（支持更多自定义字段）";
    }
}

