package io.metersphere.workflow.controller;

import io.metersphere.controller.handler.ResultHolder;
import io.metersphere.workflow.dto.DeployResultView;
import io.metersphere.workflow.dto.ModelView;
import io.metersphere.workflow.dto.SaveModelRequest;
import io.metersphere.workflow.service.WorkflowModelService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/models")
public class WorkflowModelController {

    @Resource
    private WorkflowModelService workflowModelService;

    @PostMapping
    public ResultHolder save(@RequestBody SaveModelRequest request) {
        try {
            ModelView data = workflowModelService.save(request);
            return ResultHolder.success(data);
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    @GetMapping
    public ResultHolder list(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "latestOnly", required = false) Boolean latestOnly) {
        try {
            List<ModelView> data = workflowModelService.list(keyword, latestOnly);
            return ResultHolder.success(data);
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResultHolder get(@PathVariable("id") String id) {
        try {
            if (!StringUtils.hasText(id)) {
                return ResultHolder.error("id is required");
            }
            ModelView data = workflowModelService.get(id);
            if (data == null) {
                return ResultHolder.error("model not found");
            }
            return ResultHolder.success(data);
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/deploy")
    public ResultHolder deploy(@PathVariable("id") String id) {
        try {
            if (!StringUtils.hasText(id)) {
                return ResultHolder.error("id is required");
            }
            DeployResultView data = workflowModelService.deploy(id);
            return ResultHolder.success(data);
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }
}
