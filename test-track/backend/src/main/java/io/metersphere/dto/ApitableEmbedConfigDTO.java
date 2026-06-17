package io.metersphere.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApitableEmbedConfigDTO {
    private String baseUrl;
    private String defaultPath;
    private String embedUrl;
    private Boolean openInNewWindow;
    private Long loadTimeout;
}
