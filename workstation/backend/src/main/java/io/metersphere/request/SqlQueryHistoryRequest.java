package io.metersphere.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SqlQueryHistoryRequest {

    private String id;

    private String sql;

    private String description;
}
