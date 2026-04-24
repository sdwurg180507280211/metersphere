package io.metersphere.requirement.pool.service;

import io.metersphere.base.domain.RequirementPool;
import io.metersphere.base.domain.TestPlan;
import io.metersphere.base.domain.TestPlanNode;
import io.metersphere.base.domain.TestPlanNodeExample;
import io.metersphere.base.mapper.TestPlanNodeMapper;
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
    @Resource
    private TestPlanNodeMapper testPlanNodeMapper;

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
        requirementPool.setActName(trimToNull(request.getActName()));
        requirementPool.setOperationType(trimToNull(request.getOperationType()));
        requirementPool.setParentWfinstCode(trimToNull(request.getParentWfinstCode()));
        requirementPool.setSystemName(trimToNull(request.getSystemName()));
        requirementPool.setReqManagerName(trimToNull(request.getReqManagerName()));
        requirementPool.setReqFatherClass(trimToNull(request.getReqFatherClass()));
        requirementPool.setReqSonClass(trimToNull(request.getReqSonClass()));
        requirementPool.setAssigneeName(trimToNull(request.getAssigneeName()));
        requirementPool.setCreatedDept(trimToNull(request.getCreatedDept()));
        requirementPool.setCreateUser1(trimToNull(request.getCreateUser1()));
        requirementPool.setDeptName(trimToNull(request.getDeptName()));
        requirementPool.setStartUserName(trimToNull(request.getStartUserName()));
        requirementPool.setUpTime(request.getUpTime() != null ? request.getUpTime() : now);
        requirementPool.setCreateTime(now);
        requirementPool.setLastSyncTime(now);
        requirementPool.setCreatedAt(now);
        requirementPool.setUpdatedAt(now);
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
        testPlanRequest.setId(UUID.randomUUID().toString().replace("-", ""));
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
        String[] nodeInfo = resolveSubModuleNode(request, requirement.getDmpNum(), requirement.getRequirementName(), projectId);
        testPlanRequest.setNodeId(nodeInfo[0]);
        testPlanRequest.setNodePath(nodeInfo[1]);
        if (StringUtils.isNotBlank(request.getPrincipalId())) {
            testPlanRequest.setPrincipals(Collections.singletonList(request.getPrincipalId()));
        }
        return testPlanRequest;
    }

    // private String getNodeId(CreateTestPlanFromRequirementRequest request, String projectId) {
    //     if (StringUtils.isNotBlank(request.getNodeId())) {
    //         return request.getNodeId();
    //     }
    //     testPlanNodeService.checkDefaultNode(projectId);
    //     TestPlanNode defaultNode = testPlanNodeService.getDefaultNode(projectId);
    //     if (defaultNode == null) {
    //         MSException.throwException("默认测试计划系统不存在");
    //     }
    //     return defaultNode.getId();
    // }
    //
    // private String getNodePath(CreateTestPlanFromRequirementRequest request, String projectId) {
    //     if (StringUtils.isNotBlank(request.getNodePath())) {
    //         return request.getNodePath();
    //     }
    //     testPlanNodeService.checkDefaultNode(projectId);
    //     TestPlanNode defaultNode = testPlanNodeService.getDefaultNode(projectId);
    //     if (defaultNode == null) {
    //         MSException.throwException("默认测试计划系统不存在");
    //     }
    //     return defaultNode.getName();
    // }

    /**
     * 解析测试计划所属模块：在用户选择的模块下自动创建 "需求编号+需求名称" 子模块
     * 例如：用户选择模块"系统X"，需求编号001，需求名称"需求1"，则创建子模块"001需求1"
     *
     * @return String[0]=nodeId, String[1]=nodePath
     */
    private String[] resolveSubModuleNode(CreateTestPlanFromRequirementRequest request, String dmpNum, String requirementName, String projectId) {
        String parentNodeId = request.getNodeId();
        String parentNodePath = request.getNodePath();

        // 未选择模块，使用默认节点
        if (StringUtils.isBlank(parentNodeId)) {
            testPlanNodeService.checkDefaultNode(projectId);
            TestPlanNode defaultNode = testPlanNodeService.getDefaultNode(projectId);
            if (defaultNode == null) {
                MSException.throwException("默认测试计划系统不存在");
            }
            return new String[]{defaultNode.getId(), defaultNode.getName()};
        }

        // 查询父节点信息
        TestPlanNode parentNode = testPlanNodeMapper.selectByPrimaryKey(parentNodeId);
        if (parentNode == null) {
            MSException.throwException("所选模块不存在");
        }

        // 构造子模块名：需求编号+需求名称
        String subModuleName = dmpNum + requirementName;

        // 查找是否已存在同名子模块
        TestPlanNodeExample example = new TestPlanNodeExample();
        example.createCriteria()
                .andProjectIdEqualTo(projectId)
                .andParentIdEqualTo(parentNodeId)
                .andNameEqualTo(subModuleName);
        List<TestPlanNode> existingNodes = testPlanNodeMapper.selectByExample(example);

        if (!existingNodes.isEmpty()) {
            // 子模块已存在，直接复用
            TestPlanNode existingNode = existingNodes.get(0);
            String subNodePath = parentNodePath + "/" + subModuleName;
            return new String[]{existingNode.getId(), subNodePath};
        }

        // 创建新子模块
        TestPlanNode subNode = new TestPlanNode();
        subNode.setProjectId(projectId);
        subNode.setName(subModuleName);
        subNode.setParentId(parentNodeId);
        subNode.setLevel(parentNode.getLevel() + 1);
        String subNodeId = testPlanNodeService.addNode(subNode);

        String subNodePath = parentNodePath + "/" + subModuleName;
        return new String[]{subNodeId, subNodePath};
    }
}
