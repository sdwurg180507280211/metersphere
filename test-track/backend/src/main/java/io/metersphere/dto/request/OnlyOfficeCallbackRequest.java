package io.metersphere.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnlyOfficeCallbackRequest {
    private String key;
    private Integer status;
    private String url;
    private String changesurl;
    private Boolean notmodified;
    private String filetype;
    private Integer forcesavetype;
    private String userdata;
}
