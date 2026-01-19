package io.metersphere.controller;

import io.metersphere.base.domain.AssociatedSystem;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.service.BaseAssociatedSystemService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author : lijiaxin
 * @date 2025-12-18 12:08
 */
@RestController
@RequestMapping("/associatedSystem")
public class BaseAssociatedSystemController {
    @Resource
    private BaseAssociatedSystemService baseAssociatedSystemService;

    @GetMapping("/list/all")
    public List<AssociatedSystem> getAllAssociatedSystems() {
        String workspaceId = SessionUtils.getCurrentWorkspaceId();
        return baseAssociatedSystemService.getAllAssociatedSystems(workspaceId);
    }
}
