package io.metersphere.excel.template;

import io.metersphere.excel.domain.TestCaseExcelData;

import java.util.Map;

/**
 * 测试用例Excel模板处理器接口
 * 每个模板都有自己的处理规则，通过实现此接口来定义各自的规则
 * 
 * @author metersphere
 */
public interface TestCaseTemplateHandler {
    
    /**
     * 获取模板文件名
     * @return 模板文件名，如 "testcase-template-1.xlsx"
     */
    String getTemplateFileName();
    
    /**
     * 获取列名映射配置
     * Excel表头名称 -> 内部字段名称的映射
     * 例如：{"用例ID" -> "ID", "用例描述" -> "用例名称"}
     * 
     * @return 列名映射Map
     */
    Map<String, String> getHeadMapping();
    
    /**
     * 获取默认的步骤编辑模式
     * @return STEP 或 TEXT
     */
    String getDefaultStepModel();
    
    /**
     * 验证Excel数据是否符合该模板的规则
     * 
     * @param data Excel数据对象
     * @return 验证错误信息，如果为空则表示验证通过
     */
    String validateData(TestCaseExcelData data);
    
    /**
     * 处理模板特定的数据转换逻辑
     * 在数据解析后，可以在这里进行模板特定的数据处理
     * 
     * @param data Excel数据对象
     */
    void processData(TestCaseExcelData data);
    
    /**
     * 获取模板描述
     * @return 模板描述信息
     */
    String getDescription();
    
    /**
     * 设置模板处理器上下文
     * 用于传递处理过程中需要的上下文信息（如Excel文件名等）
     * 
     * @param context 上下文对象
     */
    default void setContext(TemplateHandlerContext context) {
        // 默认实现为空，子类可以重写
    }
}

