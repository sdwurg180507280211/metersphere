package io.metersphere.api.dto.automation;

import lombok.Data;

@Data
public class ScenarioVariableOverrideDTO {
    private String id;
    private String scenarioId;
    private String variableId;
    private String userId;
    private String variableJson;
    private Long createTime;
    private Long updateTime;
}
