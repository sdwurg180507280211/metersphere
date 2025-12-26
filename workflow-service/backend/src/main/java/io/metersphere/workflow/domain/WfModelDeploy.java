package io.metersphere.workflow.domain;

import lombok.Data;

@Data
public class WfModelDeploy {
    private String id;
    private String modelId;
    private String deploymentId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private Integer processDefinitionVersion;
    private Long deployedTime;
}
