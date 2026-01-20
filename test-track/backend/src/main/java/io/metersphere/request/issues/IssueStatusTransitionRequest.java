package io.metersphere.request.issues;

import lombok.Data;

@Data
public class IssueStatusTransitionRequest {
    private String issueId;
    private String toStatus;
    private String comment;
}

