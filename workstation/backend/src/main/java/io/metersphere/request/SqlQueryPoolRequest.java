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
}
