package io.metersphere.base.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class IssueChangeLog implements Serializable {
    private String id;

    private String issueId;

    private String projectId;

    private String operator;

    private String operatorName;

    private String source;

    private Long createTime;

    private static final long serialVersionUID = 1L;
}
