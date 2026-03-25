package io.metersphere.requirement.pool.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class CreateTestPlanFromRequirementRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dmpNum;

    private String projectId;

    private String principalId;

    private Long plannedStartTime;

    private Long plannedEndTime;

    private String description;
}
