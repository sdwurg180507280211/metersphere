package io.metersphere.requirement.pool.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.metersphere.base.domain.RequirementPool;
import io.metersphere.base.domain.TestPlan;
import io.metersphere.commons.utils.PageUtils;
import io.metersphere.commons.utils.Pager;
import io.metersphere.requirement.pool.request.CreateRequirementPoolRequest;
import io.metersphere.requirement.pool.request.CreateTestPlanFromRequirementRequest;
import io.metersphere.requirement.pool.request.QueryRequirementPoolRequest;
import io.metersphere.requirement.pool.service.RequirementPoolService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/requirement-pool")
public class RequirementPoolController {

    @Resource
    private RequirementPoolService requirementPoolService;

    @PostMapping("/add")
    public RequirementPool addRequirement(@RequestBody CreateRequirementPoolRequest request) {
        return requirementPoolService.addRequirement(request);
    }

    @PostMapping("/list/{goPage}/{pageSize}")
    public Pager<List<RequirementPool>> listRequirements(@PathVariable int goPage,
                                                          @PathVariable int pageSize,
                                                          @RequestBody QueryRequirementPoolRequest request) {
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        return PageUtils.setPageInfo(page, requirementPoolService.listRequirements(request));
    }

    @GetMapping("/{dmpNum}")
    public RequirementPool getByDmpNum(@PathVariable String dmpNum) {
        return requirementPoolService.getByDmpNum(dmpNum);
    }

    @PostMapping("/create-test-plan")
    public TestPlan createTestPlanFromRequirement(@RequestBody CreateTestPlanFromRequirementRequest request) {
        return requirementPoolService.createTestPlanFromRequirement(request);
    }

    @PostMapping("/rollback-test-plan")
    public void rollbackTestPlan(@RequestBody Map<String, String> request) {
        requirementPoolService.rollbackTestPlan(request.get("dmpNum"));
    }
}
