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
import io.metersphere.commons.utils.BeanUtils;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.plan.request.AddTestPlanRequest;
import io.metersphere.plan.service.TestPlanService;
import io.metersphere.requirement.pool.dto.RequirementSyncMessage;
import io.metersphere.requirement.pool.producer.RequirementSyncProducer;
import io.metersphere.requirement.pool.request.CreateRequirementPoolRequest;
import io.metersphere.requirement.pool.request.CreateTestPlanFromRequirementRequest;
import io.metersphere.requirement.pool.request.QueryRequirementPoolRequest;
import io.metersphere.service.TestCaseNodeService;
import io.metersphere.service.TestPlanNodeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RequirementPoolService {

    private static final String OPERATION_CREATED = "CREATED";
    private static final String OPERATION_UPDATED = "UPDATED";
    private static final String OPERATION_CANCELLED = "CANCELLED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Resource
    private ExtRequirementPoolMapper extRequirementPoolMapper;
    @Resource
    private TestPlanService testPlanService;
    @Resource
    private RequirementSyncProducer requirementSyncProducer;
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
        requirementPool.setPoolStatus(STATUS_PENDING);
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

    public RequirementPool submitRequirement(CreateRequirementPoolRequest request) throws Exception {
        String dmpNum = StringUtils.trimToEmpty(request.getDmpNum());
        String requirementName = StringUtils.trimToEmpty(request.getRequirementName());
        if (StringUtils.isBlank(dmpNum)) {
            MSException.throwException("需求编号不能为空");
        }
        if (StringUtils.isBlank(requirementName)) {
            MSException.throwException("需求名称不能为空");
        }

        RequirementSyncMessage msg = buildSyncMessage(request);
        log.info("[需求MQ-模拟生产者] 创建按钮触发发送, dmpNum={}, requirementName={}, traceId={}", dmpNum, requirementName, msg.getTraceId());
        requirementSyncProducer.sendSyncMessage(msg);

        RequirementPool result = new RequirementPool();
        result.setDmpNum(dmpNum);
        result.setRequirementName(requirementName);
        return result;
    }

    private RequirementSyncMessage buildSyncMessage(CreateRequirementPoolRequest request) {
        RequirementSyncMessage msg = BeanUtils.copyBean(new RequirementSyncMessage(), request);
        msg.setDmpNum(StringUtils.trimToEmpty(request.getDmpNum()));
        msg.setName1(StringUtils.trimToEmpty(request.getRequirementName()));
        msg.setCreatedept(request.getCreatedDept());
        msg.setOperationType(OPERATION_CREATED);
        msg.setEventTime(System.currentTimeMillis());
        ensureTraceId(msg);
        return msg;
    }

    @Transactional(rollbackFor = Exception.class)
    public TestPlan createTestPlanFromRequirement(CreateTestPlanFromRequirementRequest request) {
        // 1. 检查需求是否存在且状态为PENDING
        RequirementPool requirement = extRequirementPoolMapper.selectByDmpNum(request.getDmpNum());
        if (requirement == null) {
            MSException.throwException("需求不存在");
        }
        if (!STATUS_PENDING.equals(requirement.getPoolStatus())) {
            MSException.throwException("该需求已创建测试计划或已取消");
        }

        // 2. 创建测试计划
        AddTestPlanRequest testPlanRequest = buildTestPlanRequest(request, requirement);
        TestPlan testPlan = testPlanService.addTestPlan(testPlanRequest);

        // 3. 更新需求池状态
        extRequirementPoolMapper.updatePoolStatusAndLinkedPlan(
            request.getDmpNum(),
            STATUS_CREATED,
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
        if (!STATUS_CREATED.equals(requirement.getPoolStatus())) {
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
        extRequirementPoolMapper.updatePoolStatusAndLinkedPlan(dmpNum, STATUS_PENDING, null, null);
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

        // 仅删除自动创建的子模块节点，不删除父模块（测试计划的父模块由用户维护）
        testPlanNodeMapper.deleteByPrimaryKey(nodeId);
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

    // ==================== MQ消息处理 ====================

    /**
     * 统一处理MQ同步消息，根据operationType分发
     * 包含幂等检查：基于dmpNum + eventTime防止重复消费和乱序消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleSyncMessage(RequirementSyncMessage msg) {
        ensureTraceId(msg);
        String dmpNum = StringUtils.trimToEmpty(msg.getDmpNum());
        if (StringUtils.isBlank(dmpNum)) {
            log.warn("[需求MQ-丢弃] dmpNum为空, traceId={}", msg.getTraceId());
            return;
        }

        RequirementPool existing = extRequirementPoolMapper.selectByDmpNum(dmpNum);

        if (existing != null && existing.getEventTime() != null
                && msg.getEventTime() != null && msg.getEventTime() <= existing.getEventTime()) {
            log.info("[需求MQ-幂等丢弃] dmpNum={}, incomingEventTime={}, currentEventTime={}, traceId={}",
                    dmpNum, msg.getEventTime(), existing.getEventTime(), msg.getTraceId());
            return;
        }

        String operationType = StringUtils.trimToEmpty(msg.getOperationType());
        log.info("[需求MQ-业务分发] dmpNum={}, operationType={}, exists={}, traceId={}",
                dmpNum, operationType, existing != null, msg.getTraceId());
        switch (operationType) {
            case OPERATION_CREATED:
                handleCreated(msg, existing);
                break;
            case OPERATION_UPDATED:
                handleUpdated(msg, existing);
                break;
            case OPERATION_CANCELLED:
                handleCancelled(msg, existing);
                break;
            default:
                log.warn("[需求MQ-忽略] 未知operationType, dmpNum={}, operationType={}, traceId={}",
                        dmpNum, operationType, msg.getTraceId());
                break;
        }
    }

    /** 处理CREATED消息：新建需求 */
    public void handleCreated(RequirementSyncMessage msg, RequirementPool existing) {
        if (existing != null) {
            // 已存在则当作更新处理
            handleUpdated(msg, existing);
            return;
        }
        long now = System.currentTimeMillis();
        RequirementPool pool = new RequirementPool();
        pool.setId(UUID.randomUUID().toString().replace("-", ""));
        applySyncMessage(pool, msg, OPERATION_CREATED);
        pool.setPoolStatus(STATUS_PENDING);
        pool.setCreateTime(msg.getCreateTime() != null ? msg.getCreateTime() : now);
        pool.setUpTime(msg.getUpTime() != null ? msg.getUpTime() : now);
        pool.setLastSyncTime(now);
        pool.setEventTime(msg.getEventTime());
        pool.setCreatedAt(now);
        pool.setUpdatedAt(now);
        extRequirementPoolMapper.insert(pool);
        log.info("[需求MQ-落库完成] action=insert, dmpNum={}, poolStatus={}, traceId={}",
                pool.getDmpNum(), pool.getPoolStatus(), pool.getTraceId());
    }

    /** 处理UPDATED消息：更新已有需求 */
    public void handleUpdated(RequirementSyncMessage msg, RequirementPool existing) {
        if (existing == null) {
            // 不存在则当作新建处理
            handleCreated(msg, null);
            return;
        }
        long now = System.currentTimeMillis();
        applySyncMessage(existing, msg, OPERATION_UPDATED);
        if (msg.getCreateTime() != null) {
            existing.setCreateTime(msg.getCreateTime());
        }
        if (msg.getUpTime() != null) {
            existing.setUpTime(msg.getUpTime());
        }
        existing.setLastSyncTime(now);
        existing.setEventTime(msg.getEventTime());
        extRequirementPoolMapper.updateByDmpNum(existing);
        log.info("[需求MQ-落库完成] action=update, dmpNum={}, traceId={}",
                existing.getDmpNum(), existing.getTraceId());
    }

    /** 处理CANCELLED消息：取消需求 */
    public void handleCancelled(RequirementSyncMessage msg, RequirementPool existing) {
        if (existing == null) {
            return;
        }
        applySyncMessage(existing, msg, OPERATION_CANCELLED);
        if (STATUS_PENDING.equals(existing.getPoolStatus())) {
            existing.setPoolStatus(STATUS_CANCELLED);
        } else {
            existing.setPoolStatus(null);
        }
        existing.setLastSyncTime(System.currentTimeMillis());
        extRequirementPoolMapper.updateByDmpNum(existing);
        log.info("[需求MQ-落库完成] action=cancel, dmpNum={}, poolStatus={}, traceId={}",
                existing.getDmpNum(), existing.getPoolStatus(), existing.getTraceId());
    }

    private void applySyncMessage(RequirementPool pool, RequirementSyncMessage msg, String operationType) {
        ensureTraceId(msg);
        BeanUtils.copyBean(pool, msg, "createTime", "upTime");
        pool.setDmpNum(StringUtils.trimToEmpty(msg.getDmpNum()));
        pool.setRequirementName(StringUtils.trimToNull(msg.getName1()));
        pool.setCreatedDept(StringUtils.trimToNull(msg.getCreatedept()));
        pool.setOperationType(operationType);
        pool.setTraceId(msg.getTraceId());
    }

    private void ensureTraceId(RequirementSyncMessage msg) {
        if (StringUtils.isBlank(msg.getTraceId())) {
            msg.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        }
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

        if (request.getPrincipalIds() != null && !request.getPrincipalIds().isEmpty()) {
            testPlanRequest.setPrincipals(request.getPrincipalIds());
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
