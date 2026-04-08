package io.metersphere.requirement.pool.service;

import io.metersphere.base.domain.RequirementPool;
import io.metersphere.base.domain.TestPlan;
import io.metersphere.base.domain.TestPlanNode;
import io.metersphere.base.mapper.ext.ExtRequirementPoolMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.plan.request.AddTestPlanRequest;
import io.metersphere.plan.service.TestPlanService;
import io.metersphere.requirement.pool.request.CreateRequirementPoolRequest;
import io.metersphere.requirement.pool.request.CreateTestPlanFromRequirementRequest;
import io.metersphere.requirement.pool.request.QueryRequirementPoolRequest;
import io.metersphere.service.TestPlanNodeService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class RequirementPoolService {

    @Resource
    private ExtRequirementPoolMapper extRequirementPoolMapper;
    @Resource
    private TestPlanService testPlanService;
    @Resource
    private TestPlanNodeService testPlanNodeService;

    @Transactional(rollbackFor = Exception.class)
    public RequirementPool addRequirement(CreateRequirementPoolRequest request) {
        String dmpNum = StringUtils.trimToEmpty(request.getDmpNum());
        String requirementName = StringUtils.trimToEmpty(request.getRequirementName());
        if (StringUtils.isBlank(dmpNum)) {
            MSException.throwException("需求编号不能为空");
        }
        if (StringUtils.isBlank(requirementName)) {
            MSException.throwException("需求名称不能为空");
        }
        RequirementPool existing = extRequirementPoolMapper.selectByDmpNum(dmpNum);
        if (existing != null) {
            MSException.throwException("需求编号已存在");
        }

        long now = System.currentTimeMillis();
        RequirementPool requirementPool = new RequirementPool();
        requirementPool.setId(UUID.randomUUID().toString().replace("-", ""));
        requirementPool.setDmpNum(dmpNum);
        requirementPool.setRequirementName(requirementName);
        requirementPool.setPoolStatus("PENDING");
        requirementPool.setSystemName(trimToNull(request.getSystemName()));
        requirementPool.setReqManagerName(trimToNull(request.getReqManagerName()));
        requirementPool.setReqFatherClass(trimToNull(request.getReqFatherClass()));
        requirementPool.setReqSonClass(trimToNull(request.getReqSonClass()));
        requirementPool.setCreateTime(now);
        requirementPool.setUpTime(now);
        requirementPool.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        extRequirementPoolMapper.insert(requirementPool);
        return requirementPool;
    }

    public List<RequirementPool> listRequirements(QueryRequirementPoolRequest request) {
        return extRequirementPoolMapper.list(request);
    }

    public RequirementPool getByDmpNum(String dmpNum) {
        return extRequirementPoolMapper.selectByDmpNum(dmpNum);
    }

    public int updatePoolStatusAndLinkedPlan(String dmpNum, String poolStatus, String linkedPlanId, String linkedPlanName) {
        return extRequirementPoolMapper.updatePoolStatusAndLinkedPlan(dmpNum, poolStatus, linkedPlanId, linkedPlanName);
    }

    @Transactional(rollbackFor = Exception.class)
    public TestPlan createTestPlanFromRequirement(CreateTestPlanFromRequirementRequest request) {
        // 1. 检查需求是否存在且状态为PENDING
        RequirementPool requirement = extRequirementPoolMapper.selectByDmpNum(request.getDmpNum());
        if (requirement == null) {
            MSException.throwException("需求不存在");
        }
        if (!"PENDING".equals(requirement.getPoolStatus())) {
            MSException.throwException("该需求已创建测试计划或已取消");
        }

        // 2. 创建测试计划
        AddTestPlanRequest testPlanRequest = buildTestPlanRequest(request, requirement);
        TestPlan testPlan = testPlanService.addTestPlan(testPlanRequest);

        // 3. 更新需求池状态
        extRequirementPoolMapper.updatePoolStatusAndLinkedPlan(
            request.getDmpNum(),
            "CREATED",
            testPlan.getId(),
            testPlan.getName()
        );

        return testPlan;
    }

    private String trimToNull(String value) {
        return StringUtils.trimToNull(value);
    }

    private AddTestPlanRequest buildTestPlanRequest(CreateTestPlanFromRequirementRequest request, RequirementPool requirement) {
        String projectId = StringUtils.defaultIfBlank(request.getProjectId(), SessionUtils.getCurrentProjectId());
        String workspaceId = StringUtils.defaultIfBlank(request.getWorkspaceId(), SessionUtils.getCurrentWorkspaceId());
        AddTestPlanRequest testPlanRequest = new AddTestPlanRequest();
        testPlanRequest.setId(UUID.randomUUID().toString());
        testPlanRequest.setProjectId(projectId);
        testPlanRequest.setWorkspaceId(workspaceId);
        testPlanRequest.setName(requirement.getRequirementName());
        testPlanRequest.setDescription(trimToNull(request.getDescription()));
        testPlanRequest.setStage(trimToNull(request.getStage()));
        testPlanRequest.setPlannedStartTime(request.getPlannedStartTime());
        testPlanRequest.setPlannedEndTime(request.getPlannedEndTime());
        testPlanRequest.setAutomaticStatusUpdate(request.getAutomaticStatusUpdate());
        testPlanRequest.setRepeatCase(request.getRepeatCase());
        testPlanRequest.setTags(request.getTags());
        testPlanRequest.setRequirementNumber(requirement.getDmpNum());
        testPlanRequest.setNodeId(getNodeId(request, projectId));
        testPlanRequest.setNodePath(getNodePath(request, projectId));
        if (StringUtils.isNotBlank(request.getPrincipalId())) {
            testPlanRequest.setPrincipals(Collections.singletonList(request.getPrincipalId()));
        }
        return testPlanRequest;
    }

    private String getNodeId(CreateTestPlanFromRequirementRequest request, String projectId) {
        if (StringUtils.isNotBlank(request.getNodeId())) {
            return request.getNodeId();
        }
        testPlanNodeService.checkDefaultNode(projectId);
        TestPlanNode defaultNode = testPlanNodeService.getDefaultNode(projectId);
        if (defaultNode == null) {
            MSException.throwException("默认测试计划模块不存在");
        }
        return defaultNode.getId();
    }

    private String getNodePath(CreateTestPlanFromRequirementRequest request, String projectId) {
        if (StringUtils.isNotBlank(request.getNodePath())) {
            return request.getNodePath();
        }
        testPlanNodeService.checkDefaultNode(projectId);
        TestPlanNode defaultNode = testPlanNodeService.getDefaultNode(projectId);
        if (defaultNode == null) {
            MSException.throwException("默认测试计划模块不存在");
        }
        return defaultNode.getName();
    }
}
