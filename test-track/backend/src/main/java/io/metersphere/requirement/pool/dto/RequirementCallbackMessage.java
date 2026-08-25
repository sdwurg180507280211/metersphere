package io.metersphere.requirement.pool.dto;

import lombok.Data;

@Data
public class RequirementCallbackMessage {
    private String dmpNum;
    private String planId;
    private String planStatus;
    private String assessmentResult;
    private Long plannedStartTime;
    private Long plannedEndTime;
    private Long actualStartTime;
    private Long actualEndTime;
    private String principalUsers;
    private String systemName;
    private String requirementSummary;
    private String relatedUsers;
    private String exemptType;
    private String exemptReason;
    private Long submitTime;
    private String planShareUrl;
    private Long syncTime;
    private String traceId;
}
