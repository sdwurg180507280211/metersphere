package io.metersphere.base.domain;

import java.io.Serializable;
import lombok.Data;

@Data
public class TestPlan implements Serializable {
    private String id;

    private String workspaceId;

    private String reportId;

    private String name;

    private String description;

    private String status;

    private String stage;

    private Long createTime;

    private Long updateTime;

    private Long plannedStartTime;

    private Long plannedEndTime;

    private Long actualStartTime;

    private Long actualEndTime;

    private String creator;

    private String projectId;

    private Integer executionTimes;

    private Boolean automaticStatusUpdate;

    private Boolean repeatCase;

    private String nodeId;

    private String nodePath;

    private String requirementNumber;

    private String requirementDocUrl;

    private String requirementSystemName;

    private Long requirementSyncEventTime;

    private String intStage;

    private String requirementApprovalStatus;

    private String requirementApprovalComment;

    private Long requirementApprovalTime;

    private static final long serialVersionUID = 1L;
}
