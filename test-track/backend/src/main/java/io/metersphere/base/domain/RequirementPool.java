package io.metersphere.base.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequirementPool implements Serializable {
    private String id;

    private String dmpNum;

    private String requirementName;

    private String poolStatus;

    private String parentWfinstCode;

    private String actName;

    private String operationType;

    private String systemName;

    private String reqManagerName;

    private String assigneeName;

    private String createdDept;

    private String createUser1;

    private String deptName;

    private String startUserName;

    private String reqFatherClass;

    private String reqSonClass;

    private Long createTime;

    private Long upTime;

    private String linkedPlanId;

    private String linkedPlanName;

    private String testStatus;

    private String planShareUrl;

    private Long lastCallbackTime;

    private Long lastSyncTime;

    private Long eventTime;

    private String traceId;

    private Long createdAt;

    private Long updatedAt;

    private static final long serialVersionUID = 1L;
}
