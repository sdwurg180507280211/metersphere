package io.metersphere.requirement.pool.dto;

import lombok.Data;

@Data
public class RequirementCallbackMessage {
    private String dmpNum;
    private String planStatus;
    private Long plannedStartTime;
    private Long plannedEndTime;
    private Long actualStartTime;
    private Long actualEndTime;
    private String principalUsers;
    private String planShareUrl;
    private Long syncTime;
    private String traceId;
}
