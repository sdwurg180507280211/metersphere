package io.metersphere.dto;

import lombok.Data;

import java.util.List;

@Data
public class IssueChangeLogDTO {
    private String id;

    private String issueId;

    private String projectId;

    private String operator;

    private String operatorName;

    private String source;

    private Long createTime;

    private List<IssueChangeLogDetailDTO> details;
}
