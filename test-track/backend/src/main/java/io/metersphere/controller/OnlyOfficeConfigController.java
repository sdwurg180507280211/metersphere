package io.metersphere.controller;

import io.metersphere.dto.OnlyOfficeEmbedConfigDTO;
import io.metersphere.dto.OnlyOfficeCaseSessionDTO;
import io.metersphere.dto.OnlyOfficeCaseSyncResultDTO;
import io.metersphere.dto.request.OnlyOfficeCallbackRequest;
import io.metersphere.dto.request.OnlyOfficeCaseSessionRequest;
import io.metersphere.commons.constants.PermissionConstants;
import io.metersphere.controller.handler.annotation.NoResultHolder;
import io.metersphere.security.CheckOwner;
import io.metersphere.service.OnlyOfficeCaseService;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/track/onlyoffice")
public class OnlyOfficeConfigController {

    @Resource
    private OnlyOfficeCaseService onlyOfficeCaseService;

    @Value("${onlyoffice.embed-url:http://localhost:8090}")
    private String embedUrl;

    @Value("${onlyoffice.open-in-new-window:true}")
    private Boolean openInNewWindow;

    @Value("${onlyoffice.load-timeout:20000}")
    private Long loadTimeout;

    @GetMapping("/config")
    public OnlyOfficeEmbedConfigDTO getConfig() {
        OnlyOfficeEmbedConfigDTO config = new OnlyOfficeEmbedConfigDTO();
        config.setEmbedUrl(embedUrl);
        config.setOpenInNewWindow(openInNewWindow);
        config.setLoadTimeout(loadTimeout);
        return config;
    }

    @PostMapping("/case/session")
    @RequiresPermissions(PermissionConstants.PROJECT_TRACK_PLAN_READ)
    @CheckOwner(resourceId = "#request.planId", resourceType = "test_plan")
    public OnlyOfficeCaseSessionDTO createCaseSession(@RequestBody OnlyOfficeCaseSessionRequest request) {
        return onlyOfficeCaseService.createCaseSession(request);
    }

    @GetMapping("/case/session/{sessionId}/state")
    @RequiresPermissions(PermissionConstants.PROJECT_TRACK_PLAN_READ)
    public OnlyOfficeCaseSyncResultDTO getCaseSessionState(@PathVariable String sessionId) {
        return onlyOfficeCaseService.getSyncResult(sessionId);
    }

    @PostMapping("/case/session/{sessionId}/save")
    @RequiresPermissions(PermissionConstants.PROJECT_TRACK_PLAN_READ_EDIT)
    public OnlyOfficeCaseSyncResultDTO saveCaseSession(@PathVariable String sessionId) {
        return onlyOfficeCaseService.forceSaveCaseSession(sessionId);
    }

    @GetMapping("/case/file/{sessionId}")
    public ResponseEntity<byte[]> loadCaseSessionFile(@PathVariable String sessionId,
                                                      @RequestParam String token) throws IOException {
        return onlyOfficeCaseService.loadSessionFile(sessionId, token);
    }

    @PostMapping("/case/callback/{sessionId}")
    @NoResultHolder
    public Map<String, Object> handleCaseCallback(@PathVariable String sessionId,
                                                  @RequestParam String token,
                                                  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                  @RequestBody OnlyOfficeCallbackRequest request) {
        return onlyOfficeCaseService.handleCallback(sessionId, token, authorization, request);
    }
}
