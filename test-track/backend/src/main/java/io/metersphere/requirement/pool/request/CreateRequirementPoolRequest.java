package io.metersphere.requirement.pool.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class CreateRequirementPoolRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dmpNum;

    private String requirementName;

    private String systemName;

    private String reqManagerName;

    private String reqFatherClass;

    private String reqSonClass;

    private String actName;

    private String operationType;

    private String parentWfinstCode;

    private String assigneeName;

    private String createdDept;

    private String createUser1;

    private String deptName;

    private String startUserName;

    private Long upTime;
}
