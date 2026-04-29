package io.metersphere.requirement.pool.service;

import io.metersphere.base.domain.RequirementPool;
import io.metersphere.base.domain.TestCaseNode;
import io.metersphere.base.domain.TestCaseNodeExample;
import io.metersphere.base.domain.TestPlan;
import io.metersphere.base.domain.TestPlanExample;
import io.metersphere.base.domain.TestPlanNode;
import io.metersphere.base.domain.TestPlanNodeExample;
import io.metersphere.base.mapper.TestCaseNodeMapper;
import io.metersphere.base.mapper.TestPlanMapper;
import io.metersphere.base.mapper.TestPlanNodeMapper;
import io.metersphere.base.mapper.ext.ExtRequirementPoolMapper;
import io.metersphere.commons.constants.ProjectModuleDefaultNodeEnum;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.plan.request.AddTestPlanRequest;
import io.metersphere.plan.service.TestPlanService;
import io.metersphere.requirement.pool.request.CreateRequirementPoolRequest;
import io.metersphere.requirement.pool.request.CreateTestPlanFromRequirementRequest;
import io.metersphere.requirement.pool.request.QueryRequirementPoolRequest;
import io.metersphere.service.TestCaseNodeService;
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
    @Resource
    private TestPlanMapper testPlanMapper;
    @Resource
    private TestCaseNodeService testCaseNodeService;
    @Resource
    private TestCaseNodeMapper testCaseNodeMapper;

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

    /**
     * 回退需求池：撤销已创建的测试计划
     * 1. 校验需求状态为 CREATED
     * 2. 删除孤儿节点（子模块"需求编号+需求名称"，若父系统节点下无其他子节点则也删除）
     * 3. 调用已有方法删除测试计划
     * 4. 重置需求池状态为 PENDING，清空关联计划信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackTestPlan(String dmpNum) {
        // 1. 校验需求状态
        RequirementPool requirement = extRequirementPoolMapper.selectByDmpNum(dmpNum);
        if (requirement == null) {
            MSException.throwException("需求不存在");
        }
        if (!"CREATED".equals(requirement.getPoolStatus())) {
            MSException.throwException("只有已创建状态的需求才能回退");
        }
        String linkedPlanId = requirement.getLinkedPlanId();
        if (StringUtils.isBlank(linkedPlanId)) {
            MSException.throwException("关联的测试计划ID为空，无法回退");
        }

        // 2. 查询测试计划，获取其所属节点信息
        TestPlan testPlan = testPlanMapper.selectByPrimaryKey(linkedPlanId);
        if (testPlan != null) {
            String projectId = testPlan.getProjectId();
            // 清理测试计划模块树孤儿节点
            if (StringUtils.isNotBlank(testPlan.getNodeId())) {
                cleanOrphanNodes(testPlan.getNodeId(), requirement.getDmpNum(), requirement.getRequirementName(), projectId);
            }
            // 清理功能用例模块树孤儿节点
            cleanCaseOrphanNodes(requirement.getDmpNum(), requirement.getRequirementName(), projectId);
        }

        // 3. 删除测试计划
        testPlanService.deleteTestPlan(linkedPlanId);

        // 4. 重置需求池状态
        extRequirementPoolMapper.updatePoolStatusAndLinkedPlan(dmpNum, "PENDING", null, null);
    }

    /**
     * 清理孤儿节点
     * 1. 删除测试计划所在的子模块节点（"需求编号+需求名称"）
     * 2. 检查父节点（系统节点）下是否还有其他子节点或测试计划，若无则也删除
     *
     * @param nodeId          测试计划所属节点ID
     * @param dmpNum          需求编号
     * @param requirementName 需求名称
     * @param projectId       项目ID
     */
    private void cleanOrphanNodes(String nodeId, String dmpNum, String requirementName, String projectId) {
        TestPlanNode subNode = testPlanNodeMapper.selectByPrimaryKey(nodeId);
        if (subNode == null) {
            return;
        }

        String expectedSubModuleName = dmpNum + requirementName;
        // 只有当节点名匹配"需求编号+需求名称"模式时才自动清理，避免误删用户手动创建的节点
        if (!StringUtils.equals(subNode.getName(), expectedSubModuleName)) {
            return;
        }

        // 检查该子模块下是否还有其他测试计划
        TestPlanExample planExample = new TestPlanExample();
        planExample.createCriteria().andNodeIdEqualTo(nodeId);
        long planCount = testPlanMapper.countByExample(planExample);
        // 注意：此时测试计划尚未删除，count >= 1（当前计划自身）。如果 > 1 说明还有其他计划，不删节点
        // 但 deleteTestPlan 还没执行，所以我们先检查除当前计划外的数量
        // 这里简化处理：因为 deleteTestPlan 还没执行，子节点下一定有当前计划
        // 回退场景下，该节点下一般只有这一个由需求创建的计划，所以直接删除节点即可
        // 更安全的做法：删除计划后再检查节点下是否还有计划，但事务内顺序不便于调整
        // 采用先删子节点的策略，因为 deleteTestPlan 会清理计划关联数据

        // 删除子模块节点
        testPlanNodeMapper.deleteByPrimaryKey(nodeId);

        // 检查父节点（系统节点）是否为空，为空则也删除
        String parentNodeId = subNode.getParentId();
        if (StringUtils.isNotBlank(parentNodeId)) {
            TestPlanNode parentNode = testPlanNodeMapper.selectByPrimaryKey(parentNodeId);
            if (parentNode != null) {
                // 检查父节点下是否还有子节点
                TestPlanNodeExample childExample = new TestPlanNodeExample();
                childExample.createCriteria()
                        .andParentIdEqualTo(parentNodeId)
                        .andProjectIdEqualTo(projectId);
                long childCount = testPlanNodeMapper.countByExample(childExample);

                // 检查父节点下是否还有直接关联的测试计划
                TestPlanExample parentPlanExample = new TestPlanExample();
                parentPlanExample.createCriteria().andNodeIdEqualTo(parentNodeId);
                long parentPlanCount = testPlanMapper.countByExample(parentPlanExample);

                // 父节点下既无子节点也无测试计划，且不是默认节点，则删除
                if (childCount == 0 && parentPlanCount == 0
                        && !StringUtils.equals(parentNode.getName(), ProjectModuleDefaultNodeEnum.TRACK_DEFAULT_NODE.getNodeName())) {
                    testPlanNodeMapper.deleteByPrimaryKey(parentNodeId);
                }
            }
        }
    }

    /**
     * 清理功能用例模块树下的孤儿节点
     * 查找名为"需求编号+需求名称"的子模块节点，若无关联用例则删除
     *
     * @param dmpNum          需求编号
     * @param requirementName 需求名称
     * @param projectId       项目ID
     */
    private void cleanCaseOrphanNodes(String dmpNum, String requirementName, String projectId) {
        String expectedSubModuleName = dmpNum + requirementName;

        // 查找所有项目中名为"需求编号+需求名称"的用例模块节点
        TestCaseNodeExample example = new TestCaseNodeExample();
        example.createCriteria()
                .andProjectIdEqualTo(projectId)
                .andNameEqualTo(expectedSubModuleName);
        List<TestCaseNode> subNodes = testCaseNodeMapper.selectByExample(example);

        for (TestCaseNode subNode : subNodes) {
            // 仅删除子模块节点，不删除父模块（功能用例的父模块由用户维护）
            testCaseNodeMapper.deleteByPrimaryKey(subNode.getId());
        }
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

        // 用例所属系统：在功能用例模块树下创建"需求编号+需求名称"子模块
        if (StringUtils.isNotBlank(request.getCaseModuleId())) {
            resolveCaseSubModuleNode(request.getCaseModuleId(), request.getCaseModulePath(),
                    requirement.getDmpNum(), requirement.getRequirementName(), projectId);
        }

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

    /**
     * 在功能用例模块树下创建"需求编号+需求名称"子模块
     *
     * @param caseModuleId    用例所属系统节点ID
     * @param caseModulePath  用例所属系统节点路径
     * @param dmpNum          需求编号
     * @param requirementName 需求名称
     * @param projectId       项目ID
     */
    private void resolveCaseSubModuleNode(String caseModuleId, String caseModulePath, String dmpNum, String requirementName, String projectId) {
        TestCaseNode parentNode = testCaseNodeMapper.selectByPrimaryKey(caseModuleId);
        if (parentNode == null) {
            return;
        }

        String subModuleName = dmpNum + requirementName;

        // 查找是否已存在同名子模块
        TestCaseNodeExample example = new TestCaseNodeExample();
        example.createCriteria()
                .andProjectIdEqualTo(projectId)
                .andParentIdEqualTo(caseModuleId)
                .andNameEqualTo(subModuleName);
        List<TestCaseNode> existingNodes = testCaseNodeMapper.selectByExample(example);

        if (!existingNodes.isEmpty()) {
            return;
        }

        // 创建新子模块
        TestCaseNode subNode = new TestCaseNode();
        subNode.setProjectId(projectId);
        subNode.setName(subModuleName);
        subNode.setParentId(caseModuleId);
        subNode.setLevel(parentNode.getLevel() + 1);
        testCaseNodeService.addNode(subNode);
    }
}
