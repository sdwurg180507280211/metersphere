package io.metersphere.constants;

public enum TestCaseExportOtherField {
    VERSION("version"),
    COMMEND("commend"),
    EXECUTE_RESULT("executeResult"),
    REVIEW_RESULT("reviewResult"),
    CREATOR("creator"),
    CREATE_TIME("createTime"),
    UPDATE_TIME("updateTime"),
    DEMAND("demand");

    private String value;

    TestCaseExportOtherField(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
