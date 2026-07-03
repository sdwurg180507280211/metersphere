package io.metersphere.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OnlyOfficeCaseSyncResultDTO {
    private String sessionId;
    private String status;
    private Integer documentStatus;
    private Integer totalRows = 0;
    private Integer updatedCases = 0;
    private Integer updatedPlanCases = 0;
    private Integer skippedRows = 0;
    private Long updatedAt;
    private Long saveRequestedAt;
    private Long saveCompletedAt;
    private String saveRequestId;
    private String message;
    private List<String> errors = new ArrayList<>();
}
