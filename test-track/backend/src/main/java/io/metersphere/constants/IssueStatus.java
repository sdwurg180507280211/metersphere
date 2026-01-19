package io.metersphere.constants;

import org.apache.commons.lang3.StringUtils;

public enum IssueStatus {
    status_new("new", "status_new"),
    status_created("created", "status_new"),
    status_resolved("resolved", "status_resolved"),
    status_closed("closed", "status_closed"),
    status_active("active", "status_active"),
    status_delete("delete", "status_delete"),
    status_in_progress("in_progress", "status_in_progress"),
    status_rejected("rejected", "status_rejected"),
    status_upcoming("upcoming", "status_upcoming"),
    status_reopened("reopened", "status_reopened"),
    status_verified("verified", "status_verified"),
    status_suspended("suspended", "status_suspended"),
    status_accepted("accepted", "status_accepted"),
    status_invalid("invalid", "status_invalid"),
    status_cancelled("cancelled", "status_cancelled"),
    status_on_hold("on_hold", "status_on_hold");

    private String name;
    private String i18nKey;

    IssueStatus(String name, String i18nKey) {
        this.name = name;
        this.i18nKey = i18nKey;
    }

    public String getName() {
        return name;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    public static IssueStatus getEnumByName(String name) {
        IssueStatus[] issueStatus = IssueStatus.values();
        for (int i = 0; i < issueStatus.length; i++) {
            if (StringUtils.equals(issueStatus[i].getName(), name)) {
                return issueStatus[i];
            }
        }
        return null;
    }

}
