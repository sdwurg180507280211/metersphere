package io.metersphere.requirement.pool.service;

import io.metersphere.base.domain.RequirementPool;
import io.metersphere.base.mapper.ext.ExtRequirementPoolMapper;
import io.metersphere.requirement.pool.request.CreateTestPlanFromRequirementRequest;
import io.metersphere.requirement.pool.request.QueryRequirementPoolRequest;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RequirementPoolService {

    @Resource
    private ExtRequirementPoolMapper extRequirementPoolMapper;

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
    public String createTestPlanFromRequirement(CreateTestPlanFromRequirementRequest request) {
        // 1. 检查需求是否存在且状态为PENDING
        RequirementPool requirement = extRequirementPoolMapper.selectByDmpNum(request.getDmpNum());
        if (requirement == null) {
            throw new RuntimeException("需求不存在");
        }
        if (!"PENDING".equals(requirement.getPoolStatus())) {
            throw new RuntimeException("该需求已创建测试计划或已取消");
        }

        // 2. 创建测试计划（这里需要调用TestPlanService，暂时返回模拟ID）
        String testPlanId = "plan_" + System.currentTimeMillis();
        String testPlanName = requirement.getRequirementName();

        // 3. 更新需求池状态
        extRequirementPoolMapper.updatePoolStatusAndLinkedPlan(
            request.getDmpNum(),
            "CREATED",
            testPlanId,
            testPlanName
        );

        return testPlanId;
    }
}
