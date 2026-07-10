package io.metersphere.plan.request;

import io.metersphere.base.domain.TestPlanWithBLOBs;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
@Setter
public class AddTestPlanRequest extends TestPlanWithBLOBs {
    private List<String> projectIds;
    private List<String> principals;
    private List<String> follows;

    public void setRequirementNumber(String requirementNumber) {
        super.setRequirementNumber(StringUtils.isBlank(requirementNumber) ? null : requirementNumber);
    }
}
