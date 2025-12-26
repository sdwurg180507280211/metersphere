package io.metersphere.workflow.dto;

import lombok.Data;

@Data
public class SaveModelRequest {
    private String modelKey;
    private String name;
    /** 目前只支持 issue */
    private String category;
    private String xml;
    private String svg;
}
