package io.metersphere.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class OnlyOfficeCaseSessionDTO {
    private String sessionId;
    private String documentServerUrl;
    private Map<String, Object> config;
    private OnlyOfficeCaseSyncResultDTO syncResult;
}
