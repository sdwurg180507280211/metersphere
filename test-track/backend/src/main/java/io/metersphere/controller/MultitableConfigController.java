package io.metersphere.controller;

import io.metersphere.dto.ApitableEmbedConfigDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/track/multitable")
public class MultitableConfigController {

    @Value("${apitable.base-url:}")
    private String apitableBaseUrl;

    @Value("${apitable.default-path:/}")
    private String apitableDefaultPath;

    @Value("${apitable.embed-url:}")
    private String apitableEmbedUrl;

    @Value("${apitable.open-in-new-window:true}")
    private Boolean apitableOpenInNewWindow;

    @Value("${apitable.load-timeout:15000}")
    private Long apitableLoadTimeout;

    @GetMapping("/config")
    public ApitableEmbedConfigDTO getConfig() {
        ApitableEmbedConfigDTO config = new ApitableEmbedConfigDTO();
        config.setBaseUrl(apitableBaseUrl);
        config.setDefaultPath(apitableDefaultPath);
        config.setEmbedUrl(apitableEmbedUrl);
        config.setOpenInNewWindow(apitableOpenInNewWindow);
        config.setLoadTimeout(apitableLoadTimeout);
        return config;
    }
}
