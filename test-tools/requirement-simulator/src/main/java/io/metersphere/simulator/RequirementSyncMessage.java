package io.metersphere.simulator;

public class RequirementSyncMessage {
    private String dmpNum;
    private String name1;
    private String operationType;
    private String reqManagerName;
    private String actName;
    private Long createTime;
    private String parentWfinstCode;
    private String reqFatherClass;
    private String reqSonClass;
    private String systemName;
    private Long upTime;
    private String assigneeName;
    private String createdept;
    private String createUser1;
    private String deptName;
    private String startUserName;
    private Long eventTime;
    private String traceId;

    // Getters and Setters
    public String getDmpNum() { return dmpNum; }
    public void setDmpNum(String dmpNum) { this.dmpNum = dmpNum; }

    public String getName1() { return name1; }
    public void setName1(String name1) { this.name1 = name1; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getReqManagerName() { return reqManagerName; }
    public void setReqManagerName(String reqManagerName) { this.reqManagerName = reqManagerName; }

    public String getActName() { return actName; }
    public void setActName(String actName) { this.actName = actName; }

    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }

    public String getParentWfinstCode() { return parentWfinstCode; }
    public void setParentWfinstCode(String parentWfinstCode) { this.parentWfinstCode = parentWfinstCode; }

    public String getReqFatherClass() { return reqFatherClass; }
    public void setReqFatherClass(String reqFatherClass) { this.reqFatherClass = reqFatherClass; }

    public String getReqSonClass() { return reqSonClass; }
    public void setReqSonClass(String reqSonClass) { this.reqSonClass = reqSonClass; }

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public Long getUpTime() { return upTime; }
    public void setUpTime(Long upTime) { this.upTime = upTime; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public String getCreatedept() { return createdept; }
    public void setCreatedept(String createdept) { this.createdept = createdept; }

    public String getCreateUser1() { return createUser1; }
    public void setCreateUser1(String createUser1) { this.createUser1 = createUser1; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getStartUserName() { return startUserName; }
    public void setStartUserName(String startUserName) { this.startUserName = startUserName; }

    public Long getEventTime() { return eventTime; }
    public void setEventTime(Long eventTime) { this.eventTime = eventTime; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
