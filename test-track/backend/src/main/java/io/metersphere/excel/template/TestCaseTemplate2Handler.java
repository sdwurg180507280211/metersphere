package io.metersphere.excel.template;

import io.metersphere.commons.constants.TestCaseConstants;
import io.metersphere.excel.domain.TestCaseExcelData;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用例模板2处理器
 * 文本描述模板，使用文本编辑模式（TEXT），包含设计人、执行人等自定义字段
 *
 * @author metersphere
 */
public class TestCaseTemplate2Handler extends AbstractTestCaseTemplateHandler {

    @Override
    public String getTemplateFileName() {
        return "testcase-template-2.xlsx";
    }

    @Override
    protected Map<String, String> getSpecificHeadMapping() {
        Map<String, String> mapping = new HashMap<>();
        // 模板2特有的字段映射（设计人、执行人等）
        mapping.put("用例ID", "ID");
        mapping.put("用例描述", "用例名称");
        mapping.put("操作步骤", "步骤描述");
        mapping.put("模块", "所属模块");
        //mapping.put("预期结果", "预期结果");
        //mapping.put("设计人", "设计人");
        //mapping.put("执行人", "执行人");
        // 可以添加更多模板2特有的映射规则
        return mapping;
    }

    @Override
    public String getDefaultStepModel() {
        return TestCaseConstants.StepModel.TEXT.name();
    }

    @Override
    public String validateData(TestCaseExcelData data) {
        StringBuilder errors = new StringBuilder();

        // 模板2的特定验证规则：要求步骤描述不能为空（文本模式）
        if (StringUtils.isBlank(data.getStepDesc())) {
            errors.append("步骤描述不能为空;");
        }

        // 可以添加模板2特有的验证规则
        // 例如：验证设计人、执行人字段

        return errors.toString();
    }

    @Override
    protected void doProcessData(TestCaseExcelData data) {
        // 先调用父类方法处理所属模块的通用逻辑（如果为空、只填写空格、/，或不以 / 开头，一律按照 '/其他' 来处理）
        super.doProcessData(data);
        // 模板2特有的数据处理逻辑：所属模块字段前面默认加上Excel文件名
        if (context != null && context.getExcelFileName() != null) {
            String nodePath = data.getNodePath();
            // 获取Excel文件名（不含扩展名）
            String fileName = context.getExcelFileNameWithoutExtension();
            if (StringUtils.isNotBlank(fileName)) {
                if (StringUtils.isNotBlank(nodePath)) {
                    // 规范化路径：确保以/开头
                    String normalizedPath = nodePath.startsWith("/") ? nodePath : "/" + nodePath;
                    // 检查路径的第一段是否已经是文件名
                    // 例如："/test/模块1" 或 "/test" 的第一段是 "test"
                    String firstSegment = normalizedPath.substring(1); // 去掉开头的/
                    int firstSlashIndex = firstSegment.indexOf('/');
                    if (firstSlashIndex > 0) {
                        firstSegment = firstSegment.substring(0, firstSlashIndex);
                    }
                    // 如果第一段不是文件名，则在前面加上文件名
                    if (!fileName.equals(firstSegment)) {
                        data.setNodePath("/" + fileName + normalizedPath);
                    } else {
                        // 如果已经是文件名开头，确保路径格式正确
                        data.setNodePath(normalizedPath);
                    }
                } else {
                    // 如果所属模块为空，则设置为文件名
                    data.setNodePath("/" + fileName);
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "用例模板2 - 文本描述模板（TEXT模式，含设计人、执行人字段）";
    }
}

