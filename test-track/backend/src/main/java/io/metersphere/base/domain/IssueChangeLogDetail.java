package io.metersphere.base.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class IssueChangeLogDetail implements Serializable {
    private String id;

    private String logId;

    private String fieldType;

    private String fieldKey;

    private String fieldId;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private Long createTime;

    private static final long serialVersionUID = 1L;
}
