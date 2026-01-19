package io.metersphere.excel.template;

import io.metersphere.commons.utils.LogUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用例模板处理器工厂
 * 根据模板文件名获取对应的模板处理器
 * 
 * @author metersphere
 */
public class TestCaseTemplateHandlerFactory {
    
    /**
     * 模板处理器缓存
     */
    private static final Map<String, TestCaseTemplateHandler> HANDLER_CACHE = new HashMap<>();
    
    /**
     * 默认处理器（当找不到匹配的模板时使用）
     */
    private static final TestCaseTemplateHandler DEFAULT_HANDLER = new DefaultTestCaseTemplateHandler();
    
    static {
        // 初始化并缓存所有模板处理器
        registerHandler(new TestCaseTemplate1Handler());
        registerHandler(new TestCaseTemplate2Handler());
        registerHandler(new TestCaseTemplate3Handler());
    }
    
    /**
     * 注册模板处理器
     * 
     * @param handler 模板处理器
     */
    private static void registerHandler(TestCaseTemplateHandler handler) {
        HANDLER_CACHE.put(handler.getTemplateFileName(), handler);
    }
    
    /**
     * 根据模板文件名获取对应的模板处理器
     * 
     * @param templateFileName 模板文件名，如 "testcase-template-1.xlsx"
     * @return 模板处理器，如果找不到则返回默认处理器
     */
    public static TestCaseTemplateHandler getHandler(String templateFileName) {
        if (StringUtils.isBlank(templateFileName)) {
            LogUtil.info("模板文件名为空，使用默认处理器");
            return DEFAULT_HANDLER;
        }
        
        // 精确匹配
        TestCaseTemplateHandler handler = HANDLER_CACHE.get(templateFileName);
        if (handler != null) {
            return handler;
        }
        
        // 模糊匹配（支持文件名包含模板标识的情况）
        String templateKey = templateFileName.replace(".xlsx", "");
        for (Map.Entry<String, TestCaseTemplateHandler> entry : HANDLER_CACHE.entrySet()) {
            String handlerKey = entry.getKey().replace(".xlsx", "");
            if (templateFileName.contains(handlerKey) || handlerKey.contains(templateKey)) {
                LogUtil.info("通过模糊匹配找到模板处理器: " + entry.getKey());
                return entry.getValue();
            }
        }
        
        LogUtil.info("未找到匹配的模板处理器，使用默认处理器。模板文件名: " + templateFileName);
        return DEFAULT_HANDLER;
    }
    
    /**
     * 默认模板处理器
     * 当找不到匹配的模板时使用，提供通用的处理逻辑
     */
    private static class DefaultTestCaseTemplateHandler extends AbstractTestCaseTemplateHandler {
        
        @Override
        public String getTemplateFileName() {
            return "default";
        }
        
        @Override
        public String getDefaultStepModel() {
            return "STEP";
        }
        
        @Override
        public String getDescription() {
            return "默认模板处理器";
        }
    }
}

