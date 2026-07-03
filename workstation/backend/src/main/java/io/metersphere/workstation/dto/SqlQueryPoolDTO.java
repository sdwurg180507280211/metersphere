package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SqlQueryPoolDTO {

    private String id;

    private String sql;

    private String title;

    private String summary;

    private String description;

    private String createUser;

    private String createUserName;

    private String updateUser;

    private String updateUserName;

    private Boolean enabled;

    private Long useCount;

    private Long createTime;

    private Long updateTime;
}
