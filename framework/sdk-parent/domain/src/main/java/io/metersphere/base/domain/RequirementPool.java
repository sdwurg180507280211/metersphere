package io.metersphere.base.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequirementPool implements Serializable {
    private String id;

    private String dmpNum;

    private String requirementName;

    private String poolStatus;

    private String systemName;

    private String reqManagerName;

    private String reqFatherClass;

    private String reqSonClass;

    private Long createTime;

    private Long upTime;

    private String linkedPlanId;

    private String linkedPlanName;

    private String traceId;

    private static final long serialVersionUID = 1L;
}
