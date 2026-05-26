package io.metersphere.base.mapper.ext;

import io.metersphere.base.domain.RequirementPool;
import io.metersphere.requirement.pool.request.QueryRequirementPoolRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtRequirementPoolMapper {

    int insert(RequirementPool requirementPool);

    List<RequirementPool> list(@Param("request") QueryRequirementPoolRequest request);

    RequirementPool selectByDmpNum(@Param("dmpNum") String dmpNum);

    int updatePoolStatusAndLinkedPlan(@Param("dmpNum") String dmpNum,
                                       @Param("poolStatus") String poolStatus,
                                       @Param("linkedPlanId") String linkedPlanId,
                                       @Param("linkedPlanName") String linkedPlanName);

    int updateCallbackResult(@Param("dmpNum") String dmpNum,
                             @Param("testStatus") String testStatus,
                             @Param("planShareUrl") String planShareUrl,
                             @Param("lastCallbackTime") Long lastCallbackTime);

    /** 根据dmpNum选择性更新需求信息 */
    int updateByDmpNum(RequirementPool requirementPool);
}
