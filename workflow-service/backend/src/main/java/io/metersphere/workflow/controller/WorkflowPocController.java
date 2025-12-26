package io.metersphere.workflow.controller;

import io.metersphere.controller.handler.ResultHolder;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/poc")
public class WorkflowPocController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @PostMapping(value = "/deploy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultHolder deploy(@RequestPart("file") MultipartFile file,
                               @RequestParam(value = "name", required = false) String name) {
        if (file == null || file.isEmpty()) {
            return ResultHolder.error("file is required");
        }
        try (InputStream in = file.getInputStream()) {
            String deployName = StringUtils.hasText(name) ? name : file.getOriginalFilename();
            Deployment deployment = repositoryService.createDeployment()
                    .name(deployName)
                    .addInputStream(Objects.requireNonNullElse(file.getOriginalFilename(), "process.bpmn20.xml"), in)
                    .deploy();

            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .latestVersion()
                    .singleResult();

            Map<String, Object> data = new HashMap<>();
            data.put("deploymentId", deployment.getId());
            if (pd != null) {
                data.put("processDefinitionId", pd.getId());
                data.put("processDefinitionKey", pd.getKey());
                data.put("processDefinitionName", pd.getName());
                data.put("processDefinitionVersion", pd.getVersion());
            }
            return ResultHolder.success(data);
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    public static class StartProcessRequest {
        private String processDefinitionKey;
        private String businessKey;
        private Map<String, Object> variables;

        public String getProcessDefinitionKey() {
            return processDefinitionKey;
        }

        public void setProcessDefinitionKey(String processDefinitionKey) {
            this.processDefinitionKey = processDefinitionKey;
        }

        public String getBusinessKey() {
            return businessKey;
        }

        public void setBusinessKey(String businessKey) {
            this.businessKey = businessKey;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public void setVariables(Map<String, Object> variables) {
            this.variables = variables;
        }
    }

    @PostMapping("/start")
    public ResultHolder start(@RequestBody StartProcessRequest request) {
        if (request == null || !StringUtils.hasText(request.getProcessDefinitionKey())) {
            return ResultHolder.error("processDefinitionKey is required");
        }
        try {
            Map<String, Object> vars = request.getVariables() == null ? new HashMap<>() : new HashMap<>(request.getVariables());
            ProcessInstance pi;
            if (StringUtils.hasText(request.getBusinessKey())) {
                pi = runtimeService.startProcessInstanceByKey(request.getProcessDefinitionKey(), request.getBusinessKey(), vars);
            } else {
                pi = runtimeService.startProcessInstanceByKey(request.getProcessDefinitionKey(), vars);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("processInstanceId", pi.getId());
            data.put("processDefinitionId", pi.getProcessDefinitionId());
            data.put("businessKey", pi.getBusinessKey());
            return ResultHolder.success(data);
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    public static class TaskView {
        private String id;
        private String name;
        private String assignee;
        private String processInstanceId;
        private Date createTime;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAssignee() {
            return assignee;
        }

        public void setAssignee(String assignee) {
            this.assignee = assignee;
        }

        public String getProcessInstanceId() {
            return processInstanceId;
        }

        public void setProcessInstanceId(String processInstanceId) {
            this.processInstanceId = processInstanceId;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }
    }

    @GetMapping("/tasks")
    public ResultHolder tasks(@RequestParam(value = "assignee", required = false) String assignee) {
        try {
            List<Task> tasks;
            if (StringUtils.hasText(assignee)) {
                tasks = taskService.createTaskQuery().taskAssignee(assignee).orderByTaskCreateTime().desc().list();
            } else {
                tasks = taskService.createTaskQuery().orderByTaskCreateTime().desc().list();
            }

            List<TaskView> data = new ArrayList<>();
            for (Task t : tasks) {
                TaskView v = new TaskView();
                v.setId(t.getId());
                v.setName(t.getName());
                v.setAssignee(t.getAssignee());
                v.setProcessInstanceId(t.getProcessInstanceId());
                v.setCreateTime(t.getCreateTime());
                data.add(v);
            }
            return ResultHolder.success(data);
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    public static class CompleteTaskRequest {
        private Map<String, Object> variables;

        public Map<String, Object> getVariables() {
            return variables;
        }

        public void setVariables(Map<String, Object> variables) {
            this.variables = variables;
        }
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResultHolder complete(@PathVariable("taskId") String taskId, @RequestBody(required = false) CompleteTaskRequest request) {
        if (!StringUtils.hasText(taskId)) {
            return ResultHolder.error("taskId is required");
        }
        try {
            Map<String, Object> vars = request == null || request.getVariables() == null ? null : request.getVariables();
            if (vars == null || vars.isEmpty()) {
                taskService.complete(taskId);
            } else {
                taskService.complete(taskId, vars);
            }
            return ResultHolder.success(true);
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }
}
