package io.metersphere.service;

import io.metersphere.base.domain.TestCase;
import io.metersphere.base.mapper.TestCaseMapper;
import io.metersphere.commons.service.AuditLogService;
import io.metersphere.commons.utils.JSON;
import io.metersphere.commons.utils.SessionUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 测试用例审计服务示例
 * 展示如何在测试用例管理中使用编程方式记录审计日志
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TestCaseAuditService {
    
    @Resource
    private TestCaseMapper testCaseMapper;
    
    @Resource
    private AuditLogService auditLogService;
    
    /**
     * 创建测试用例
     */
    public TestCase createTestCase(TestCase testCase) {
        try {
            // 执行创建操作
            testCaseMapper.insert(testCase);
            
            // 记录审计日志
            auditLogService.sendUserOperationLog(
                "ADD",
                "TEST_CASE_MANAGEMENT",
                "创建测试用例: " + testCase.getName(),
                testCase.getId(),
                null,
                JSON.toJSONString(testCase)
            );
            
            return testCase;
            
        } catch (Exception e) {
            // 记录失败日志
            auditLogService.sendSystemOperationLog(
                "ADD",
                "TEST_CASE_MANAGEMENT",
                "创建测试用例失败: " + e.getMessage(),
                testCase.getId()
            );
            throw e;
        }
    }
    
    /**
     * 更新测试用例
     */
    public TestCase updateTestCase(TestCase testCase) {
        try {
            // 获取原始数据
            TestCase originalTestCase = testCaseMapper.selectByPrimaryKey(testCase.getId());
            
            // 执行更新操作
            testCaseMapper.updateByPrimaryKey(testCase);
            
            // 记录审计日志
            auditLogService.sendUserOperationLog(
                "UPDATE",
                "TEST_CASE_MANAGEMENT",
                "更新测试用例: " + testCase.getName(),
                testCase.getId(),
                JSON.toJSONString(originalTestCase),
                JSON.toJSONString(testCase)
            );
            
            return testCase;
            
        } catch (Exception e) {
            auditLogService.sendSystemOperationLog(
                "UPDATE",
                "TEST_CASE_MANAGEMENT",
                "更新测试用例失败: " + e.getMessage(),
                testCase.getId()
            );
            throw e;
        }
    }
    
    /**
     * 删除测试用例
     */
    public void deleteTestCase(String testCaseId) {
        try {
            // 获取要删除的测试用例信息
            TestCase testCase = testCaseMapper.selectByPrimaryKey(testCaseId);
            
            // 执行删除操作
            testCaseMapper.deleteByPrimaryKey(testCaseId);
            
            // 记录审计日志
            auditLogService.sendUserOperationLog(
                "DELETE",
                "TEST_CASE_MANAGEMENT",
                "删除测试用例: " + testCase.getName(),
                testCaseId,
                JSON.toJSONString(testCase),
                null
            );
            
        } catch (Exception e) {
            auditLogService.sendSystemOperationLog(
                "DELETE",
                "TEST_CASE_MANAGEMENT",
                "删除测试用例失败: " + e.getMessage(),
                testCaseId
            );
            throw e;
        }
    }
    
    /**
     * 批量删除测试用例
     */
    public void batchDeleteTestCases(List<String> testCaseIds) {
        try {
            for (String testCaseId : testCaseIds) {
                TestCase testCase = testCaseMapper.selectByPrimaryKey(testCaseId);
                testCaseMapper.deleteByPrimaryKey(testCaseId);
                
                // 为每个测试用例记录审计日志
                auditLogService.sendUserOperationLog(
                    "DELETE",
                    "TEST_CASE_MANAGEMENT",
                    "批量删除测试用例: " + testCase.getName(),
                    testCaseId,
                    JSON.toJSONString(testCase),
                    null
                );
            }
            
            // 记录批量操作汇总日志
            auditLogService.sendSystemOperationLog(
                "DELETE",
                "TEST_CASE_MANAGEMENT",
                "批量删除测试用例完成，共删除 " + testCaseIds.size() + " 个用例",
                String.join(",", testCaseIds)
            );
            
        } catch (Exception e) {
            auditLogService.sendSystemOperationLog(
                "DELETE",
                "TEST_CASE_MANAGEMENT",
                "批量删除测试用例失败: " + e.getMessage() + "，用例数量: " + testCaseIds.size(),
                String.join(",", testCaseIds)
            );
            throw e;
        }
    }
    
    /**
     * 执行测试用例
     */
    public void executeTestCase(String testCaseId, String result) {
        try {
            TestCase testCase = testCaseMapper.selectByPrimaryKey(testCaseId);
            
            // 更新执行结果
            testCase.setLastResult(result);
            testCase.setLastExecuteTime(System.currentTimeMillis());
            testCaseMapper.updateByPrimaryKey(testCase);
            
            // 记录执行日志
            auditLogService.sendSystemOperationLog(
                "EXECUTE",
                "TEST_CASE_MANAGEMENT",
                "执行测试用例: " + testCase.getName() + "，结果: " + result,
                testCaseId
            );
            
        } catch (Exception e) {
            auditLogService.sendSystemOperationLog(
                "EXECUTE",
                "TEST_CASE_MANAGEMENT",
                "执行测试用例失败: " + e.getMessage(),
                testCaseId
            );
            throw e;
        }
    }
}