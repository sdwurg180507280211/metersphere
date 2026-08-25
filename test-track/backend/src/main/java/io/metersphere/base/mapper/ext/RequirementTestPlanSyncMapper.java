package io.metersphere.base.mapper.ext;

import io.metersphere.base.domain.TestPlan;
import io.metersphere.requirement.sync.dto.RequirementSystemMapping;
import org.apache.ibatis.annotations.Param;

public interface RequirementTestPlanSyncMapper {

    TestPlan selectByRequirementNumber(@Param("requirementNumber") String requirementNumber);

    RequirementSystemMapping selectEnabledSystemMapping(@Param("systemKey") String systemKey);

    int insertSyncedTestPlan(@Param("plan") TestPlan plan);

    int updateSyncedTestPlan(@Param("plan") TestPlan plan);

    int cancelSyncedTestPlan(@Param("id") String id,
                             @Param("eventTime") Long eventTime,
                             @Param("updateTime") Long updateTime);

    int updateApproval(@Param("id") String id,
                       @Param("approvalStatus") String approvalStatus,
                       @Param("approvalComment") String approvalComment,
                       @Param("approvalTime") Long approvalTime,
                       @Param("intStage") String intStage,
                       @Param("updateTime") Long updateTime);
}
