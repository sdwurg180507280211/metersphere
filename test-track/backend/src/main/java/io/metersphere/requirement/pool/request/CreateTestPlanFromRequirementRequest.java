package io.metersphere.requirement.pool.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class CreateTestPlanFromRequirementRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dmpNum;

    private String projectId;

    private String workspaceId;

    private List<String> principalIds;

    private String stage;

    private Long plannedStartTime;

    private Long plannedEndTime;

    private String description;

    private Boolean automaticStatusUpdate;

    private Boolean repeatCase;

    private String nodeId;

    private String nodePath;

    private String tags;

    /** 用例所属系统节点ID */
    private String caseModuleId;

    /** 用例所属系统节点路径 */
    private String caseModulePath;
}
