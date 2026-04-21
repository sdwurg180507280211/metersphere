package io.metersphere.plan.request;

import io.metersphere.base.domain.TestPlan;
import io.metersphere.request.BaseQueryRequest;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.List;

@Getter
@Setter
public class QueryTestPlanRequest extends BaseQueryRequest {

    @Serial
    private static final long serialVersionUID = -9022330526265056106L;

    private String id;
    private String userId;

    private boolean recent = false;

    private List<String> planIds;

    /**
     * 导出时选中的ID列表
     */
    private List<String> exportIds;

    private String scenarioId;

    private String apiId;

    private String loadId;

    private String projectName;

    /**
     * 执行人或者负责人
     */
    private String executorOrPrincipal;

    /**
     * 是否通过筛选条件查询（这个字段针对我的工作台-页面列表上的筛选做特殊处理）
     */
    private boolean byFilter;

    private List<String> filterStatus;

    /**
     * @since 2.10.10 添加模块树条件, 批量移动条件
     */
    private List<String> nodeIds;
}
