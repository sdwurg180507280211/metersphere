package io.metersphere.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SqlQueryRequest {

    private String sql;

    private Integer limit;

    private Integer timeoutSeconds;
}
