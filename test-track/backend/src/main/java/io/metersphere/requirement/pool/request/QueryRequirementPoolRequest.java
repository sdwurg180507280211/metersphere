package io.metersphere.requirement.pool.request;

import io.metersphere.base.domain.RequirementPool;
import io.metersphere.request.OrderRequest;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class QueryRequirementPoolRequest extends RequirementPool {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<OrderRequest> orders;

    private Map<String, List<String>> filters;

    private Map<String, Object> combine;

    private String projectId;

    private String workspaceId;

    private String name;
}
