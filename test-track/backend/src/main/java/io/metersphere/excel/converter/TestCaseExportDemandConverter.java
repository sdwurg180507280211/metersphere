package io.metersphere.excel.converter;

import io.metersphere.dto.TestCaseDTO;
import org.apache.commons.lang3.StringUtils;

/**
 * 功能用例导出需求号字段转换器
 */
public class TestCaseExportDemandConverter implements TestCaseExportConverter {

    @Override
    public String parse(TestCaseDTO testCase) {
        // 优先返回需求名称,如果没有则返回需求ID
        if (StringUtils.isNotBlank(testCase.getDemandName())) {
            return testCase.getDemandName();
        }
        if (StringUtils.isNotBlank(testCase.getDemandId())) {
            return testCase.getDemandId();
        }
        return StringUtils.EMPTY;
    }
}
