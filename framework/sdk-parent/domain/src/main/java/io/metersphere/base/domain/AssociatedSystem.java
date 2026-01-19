package io.metersphere.base.domain;

import java.io.Serializable;
import lombok.Data;

@Data
public class AssociatedSystem implements Serializable {
    private String id;

    private String workspaceId;

    private String name;

    private String description;

    private Long createTime;

    private Long updateTime;

    private static final long serialVersionUID = 1L;
}