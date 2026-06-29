package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SqlQueryHistoryDTO {

    private String id;

    private String sql;

    private String title;

    private String description;

    private Boolean saved;

    private Long createTime;

    private Long updateTime;
}
