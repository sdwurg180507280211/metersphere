package io.metersphere.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.metersphere.base.domain.AssociatedSystem;
import io.metersphere.base.domain.Project;
import io.metersphere.commons.constants.MicroServiceName;
import io.metersphere.commons.constants.OperLogConstants;
import io.metersphere.commons.constants.OperLogModule;
import io.metersphere.commons.constants.PermissionConstants;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.commons.utils.PageUtils;
import io.metersphere.commons.utils.Pager;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.dto.ProjectDTO;
import io.metersphere.log.annotation.MsAuditLog;
import io.metersphere.request.AddProjectRequest;
import io.metersphere.request.AssociatedSystemRequest;
import io.metersphere.request.ProjectRequest;
import io.metersphere.service.AssociatedSystemService;
import io.metersphere.service.BaseProjectService;
import io.metersphere.service.SystemProjectService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author : lijiaxin
 * @date 2025-12-15 11:39
 */
@RestController
@RequestMapping(value = "/associatedSystem")
public class AssociatedSystemController {

    @Resource
    private AssociatedSystemService associatedSystemService;

    @PostMapping("/add")
   // @MsAuditLog(module = OperLogModule.PROJECT_PROJECT_MANAGER, type = OperLogConstants.CREATE, content = "#msClass.getLogDetails(#project.id)", msClass = BaseProjectService.class)
    @RequiresPermissions(PermissionConstants.WORKSPACE_PROJECT_MANAGER_READ_CREATE)
    public AssociatedSystem addAssociatedSystem(@RequestBody AssociatedSystem associatedSystem, HttpServletRequest request) {
        return associatedSystemService.addAssociatedSystem(associatedSystem);
    }

    @PostMapping("/list/{goPage}/{pageSize}")
    @RequiresPermissions(PermissionConstants.WORKSPACE_PROJECT_MANAGER_READ)
    public Pager<List<AssociatedSystem>> getAssociatedSystemList(@PathVariable int goPage, @PathVariable int pageSize, @RequestBody AssociatedSystemRequest request) {
        if (StringUtils.isBlank(request.getWorkspaceId())) {
            return new Pager<>();
        }
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        return PageUtils.setPageInfo(page, associatedSystemService.getAssociatedSystemList(request));
    }

    @GetMapping("/delete/{associatedSystemId}")
    //@MsAuditLog(module = OperLogModule.PROJECT_PROJECT_MANAGER, type = OperLogConstants.DELETE, beforeEvent = "#msClass.getLogDetails(#projectId)", msClass = BaseProjectService.class)
    @RequiresPermissions(PermissionConstants.WORKSPACE_PROJECT_MANAGER_READ_DELETE)
    public void deleteAssociatedSystemId(@PathVariable(value = "associatedSystemId") String associatedSystemId) {
        associatedSystemService.deleteAssociatedSystemId(associatedSystemId);
    }

    @PostMapping("/update")
   // @MsAuditLog(module = OperLogModule.PROJECT_PROJECT_MANAGER, type = OperLogConstants.UPDATE, beforeEvent = "#msClass.getLogDetails(#project.id)", content = "#msClass.getLogDetails(#project.id)", msClass = BaseProjectService.class)
    @RequiresPermissions(value = {PermissionConstants.WORKSPACE_PROJECT_MANAGER_READ_EDIT, PermissionConstants.PROJECT_MANAGER_READ_EDIT}, logical = Logical.OR)
    public void updateAssociatedSystemId(@RequestBody AssociatedSystem associatedSystem) {
        associatedSystemService.updateAssociatedSystemId(associatedSystem);
    }

}
