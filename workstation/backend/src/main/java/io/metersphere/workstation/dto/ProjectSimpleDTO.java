package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 项目简要信息 DTO
 * 
 * 用于项目选择器的数据展示
 * 
 * @author MeterSphere
 */
@Getter
@Setter
public class ProjectSimpleDTO {
    
    /**
     * 项目ID
     */
    private String id;
    
    /**
     * 项目名称
     */
    private String name;
    
    /**
     * 所属工作空间ID
     */
    private String workspaceId;
    
    /**
     * 项目描述
     */
    private String description;
}
