package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 工作空间简要信息 DTO
 * 
 * 用于工作空间选择器的数据展示
 * 
 * @author MeterSphere
 */
@Getter
@Setter
public class WorkspaceSimpleDTO {
    
    /**
     * 工作空间ID
     */
    private String id;
    
    /**
     * 工作空间名称
     */
    private String name;
    
    /**
     * 工作空间描述
     */
    private String description;
}
