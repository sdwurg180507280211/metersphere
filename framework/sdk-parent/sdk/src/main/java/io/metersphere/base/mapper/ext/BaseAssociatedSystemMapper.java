package io.metersphere.base.mapper.ext;

import io.metersphere.base.domain.AssociatedSystem;
import io.metersphere.base.domain.AssociatedSystemExample;
import io.metersphere.dto.ProjectDTO;
import io.metersphere.request.AssociatedSystemRequest;
import io.metersphere.request.ProjectRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author : lijiaxin
 * @date 2025-12-15 15:32
 */

public interface BaseAssociatedSystemMapper {

    List<AssociatedSystem> getAssociatedSystemWithWorkspace(@Param("associatedSystemRequest") AssociatedSystemRequest request);

    List<AssociatedSystem> getAssociatedSystemWithWorkspaceId(@Param("workspaceId") String workspaceId);
}
