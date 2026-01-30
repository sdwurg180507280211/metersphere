package io.metersphere.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ProjectRequest {
    private String workspaceId;
    private String projectId;
    private String userId;
    private String name;
    private List<OrderRequest> orders;
    private Map<String, List<String>> filters;
    private Map<String, Object> combine;
    
    /**
     * 工作空间ID列表（用于高级搜索的工作空间筛选）
     * 支持多选工作空间，返回这些工作空间下的所有项目
     */
    private List<String> workspaceIds;
}
