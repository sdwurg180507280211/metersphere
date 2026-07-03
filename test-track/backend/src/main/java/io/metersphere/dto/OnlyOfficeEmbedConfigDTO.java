package io.metersphere.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnlyOfficeEmbedConfigDTO {
    private String embedUrl;
    private Boolean openInNewWindow;
    private Long loadTimeout;
}
