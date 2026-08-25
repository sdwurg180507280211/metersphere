package io.metersphere.requirement.sync.dto;

import lombok.Data;

@Data
public class RequirementSystemMapping {
    private String id;

    private String systemKey;

    private String systemName;

    private String projectId;

    private String nodeId;

    private String principalId;

    private Boolean enabled;

    private Long createTime;

    private Long updateTime;
}
