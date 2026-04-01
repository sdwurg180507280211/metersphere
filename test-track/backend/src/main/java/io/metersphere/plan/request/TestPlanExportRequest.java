package io.metersphere.plan.request;

import lombok.Data;

import java.io.Serial;

/**
 * 测试计划导出参数
 */
@Data
public class TestPlanExportRequest extends QueryTestPlanRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否全选
     * 注意：exportIds 字段在父类 QueryTestPlanRequest 中定义
     */
    private Boolean isSelectAll;
}
