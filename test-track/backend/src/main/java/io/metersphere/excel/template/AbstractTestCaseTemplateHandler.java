package io.metersphere.excel.template;

import io.metersphere.excel.domain.TestCaseExcelData;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用例模板处理器抽象基类
 * 提供通用的列名映射和数据处理逻辑，减少子类代码重复
 *
 * @author metersphere
 */
public abstract class AbstractTestCaseTemplateHandler implements TestCaseTemplateHandler {

    /**
     * 模板处理器上下文（用于子类访问）
     */
    public TemplateHandlerContext context;

    /**
     * 通用列名映射（所有模板都支持的基础字段）
     */
    private static final Map<String, String> COMMON_HEAD_MAPPING = new HashMap<>();

    static {

    }

    /**
     * 获取通用列名映射
     * 子类可以在此基础上添加模板特定的映射
     *
     * @return 通用列名映射的副本
     */
    protected Map<String, String> getCommonHeadMapping() {
        return new HashMap<>(COMMON_HEAD_MAPPING);
    }

    /**
     * 获取模板特定的列名映射
     * 子类可以重写此方法来添加模板特有的映射规则
     *
     * @return 模板特定的列名映射
     */
    protected Map<String, String> getSpecificHeadMapping() {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getHeadMapping() {
        Map<String, String> mapping = getCommonHeadMapping();
        // 合并模板特定的映射（特定映射可以覆盖通用映射）
        mapping.putAll(getSpecificHeadMapping());
        return mapping;
    }

    @Override
    public void processData(TestCaseExcelData data) {
        // 确保步骤编辑模式正确设置（所有模板的通用逻辑）
        if (StringUtils.isBlank(data.getStepModel())) {
            data.setStepModel(getDefaultStepModel());
        }

        // 所有模板的用例等级默认设置为P2（如果为空）
        if (StringUtils.isBlank(data.getPriority())) {
            data.setPriority("P2");
        }

        // 子类可以重写此方法来添加模板特定的处理逻辑
        doProcessData(data);
    }

    /**
     * 模板特定的数据处理逻辑
     * 子类可以重写此方法来实现模板特定的数据处理
     *
     * @param data Excel数据对象
     */
    protected void doProcessData(TestCaseExcelData data) {
        // 所有模板通用的所属模块处理逻辑：如果所属模块为空，只填写空格，/，或不以 / 开头，一律按照 '/其他' 来处理
        String nodePath = data.getNodePath();
        if (StringUtils.isBlank(nodePath) || StringUtils.equals(nodePath, "/") || !nodePath.startsWith("/")) {
            data.setNodePath("/其他");
        }
    }

    @Override
    public void setContext(TemplateHandlerContext context) {
        this.context = context;
    }

    /**
     * 获取默认的步骤编辑模式
     * 子类必须实现此方法来指定模板的默认编辑模式
     *
     * @return STEP 或 TEXT
     */
    @Override
    public abstract String getDefaultStepModel();

    /**
     * 验证Excel数据是否符合该模板的规则
     * 子类可以重写此方法来实现模板特定的验证逻辑
     *
     * @param data Excel数据对象
     * @return 验证错误信息，如果为空则表示验证通过
     */
    @Override
    public String validateData(TestCaseExcelData data) {
        // 默认不进行特殊验证，子类可以重写
        return "";
    }
}

