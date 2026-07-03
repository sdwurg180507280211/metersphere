package io.metersphere.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnlyOfficeCaseSessionRequest {
    private String planId;
    private String projectId;
}
