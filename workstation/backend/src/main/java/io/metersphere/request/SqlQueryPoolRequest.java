package io.metersphere.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SqlQueryPoolRequest {

    private String id;

    private String sql;

    private String title;

    private String summary;

    private String description;

    private String keyword;

    private Boolean onlyMine;

    /**
     * 当同名公共 SQL 已被软删除时，是否确认恢复该记录。
     */
    private Boolean restoreDeleted;
}
