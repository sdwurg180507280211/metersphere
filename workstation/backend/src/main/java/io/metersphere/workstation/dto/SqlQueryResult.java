package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SqlQueryResult {

    private List<SqlQueryColumn> columns;

    private List<Map<String, Object>> rows;

    private Integer rowCount;

    private Long executionTime;

    private Boolean truncated;

    private Integer limit;
}
