package io.metersphere.controller;

import io.metersphere.base.domain.ReleaseNote;
import io.metersphere.commons.constants.PermissionConstants;
import io.metersphere.commons.utils.Pager;
import io.metersphere.service.ReleaseNoteService;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 需求上线记录 Controller
 * 提供上线记录的增删改查 REST API 端点
 * 放在 SDK 层以支持 system-setting 和 test-track 两个模块共享访问
 */
@RestController
@RequestMapping("/release-note")
public class ReleaseNoteController {

    @Resource
    private ReleaseNoteService releaseNoteService;

    /** 新增上线记录（需要系统设置读写权限） */
    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ_EDIT)
    public ReleaseNote add(@RequestBody ReleaseNote releaseNote) {
        return releaseNoteService.add(releaseNote);
    }

    /** 更新上线记录（需要系统设置读写权限） */
    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ_EDIT)
    public void update(@RequestBody ReleaseNote releaseNote) {
        releaseNoteService.update(releaseNote);
    }

    /** 删除上线记录（需要系统设置读写权限） */
    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.SYSTEM_SETTING_READ_EDIT)
    public void delete(@PathVariable String id) {
        releaseNoteService.delete(id);
    }

    /** 分页查询上线记录列表（按 create_time 倒序） */
    @PostMapping("/list/{goPage}/{pageSize}")
    public Pager<List<ReleaseNote>> list(@PathVariable int goPage, @PathVariable int pageSize) {
        return releaseNoteService.list(goPage, pageSize);
    }

    /** 获取最近 N 条上线记录（供测试跟踪首页使用） */
    @GetMapping("/recent/{limit}")
    public List<ReleaseNote> recent(@PathVariable int limit) {
        return releaseNoteService.recent(limit);
    }

    /** 获取单条上线记录详情 */
    @GetMapping("/get/{id}")
    public ReleaseNote get(@PathVariable String id) {
        return releaseNoteService.get(id);
    }
}
