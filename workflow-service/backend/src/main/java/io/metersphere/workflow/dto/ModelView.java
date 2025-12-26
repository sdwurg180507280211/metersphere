package io.metersphere.workflow.dto;

import lombok.Data;

@Data
public class ModelView {
    private String id;
    private String category;
    private String modelKey;
    private String name;
    private Integer version;
    private Boolean latest;
    private String xml;
    private String svg;
    private String createdBy;
    private Long createdTime;
    private String updatedBy;
    private Long updatedTime;
}
