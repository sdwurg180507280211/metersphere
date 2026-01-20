package io.metersphere.dto;

import lombok.Data;

@Data
public class IssueChangeLogDetailDTO {
    private String fieldType;

    private String fieldKey;

    private String fieldId;

    private String fieldName;

    private String oldValue;

    private String newValue;
}
