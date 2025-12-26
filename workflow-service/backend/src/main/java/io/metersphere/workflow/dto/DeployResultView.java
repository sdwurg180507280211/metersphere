package io.metersphere.workflow.dto;

import lombok.Data;

@Data
public class DeployResultView {
    private String modelId;
    private String deploymentId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private Integer processDefinitionVersion;
}
