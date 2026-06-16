package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SqlConnectionStatus {

    private Boolean connected;

    private String database;

    private String host;

    private String message;
}
