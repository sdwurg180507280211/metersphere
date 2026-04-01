package io.metersphere.workstation.constants;

import java.util.Collections;
import java.util.List;

/**
 * 工作台模块常量
 * 定义业务模块类型常量，避免硬编码字符串
 */
public final class ModuleConstants {

    private ModuleConstants() {
        // 工具类不允许实例化
    }

    /**
     * 测试用例模块
     */
    public static final String TEST_CASE = "TEST_CASE";

    /**
     * 缺陷模块
     */
    public static final String ISSUE = "ISSUE";

    /**
     * 测试计划模块
     */
    public static final String TEST_PLAN = "TEST_PLAN";

    /**
     * 用例评审模块
     */
    public static final String TEST_CASE_REVIEW = "TEST_CASE_REVIEW";

    /**
     * 所有支持的模块列表
     */
    public static final List<String> ALL_MODULES = Collections.unmodifiableList(
        List.of(TEST_CASE, ISSUE, TEST_PLAN, TEST_CASE_REVIEW)
    );

    /**
     * 验证模块是否合法
     *
     * @param module 模块名称
     * @return 是否合法
     */
    public static boolean isValidModule(String module) {
        return ALL_MODULES.contains(module);
    }
}
