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
}
