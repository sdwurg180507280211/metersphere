package io.metersphere.controller;

import io.metersphere.base.domain.TestCase;
import io.metersphere.commons.annotation.AuditLog;
import io.metersphere.service.TestCaseAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 测试用例审计控制器
 * 展示如何在 Controller 层使用审计日志
 */
@Tag(name = "测试用例审计管理")
@RestController
@RequestMapping("/test-case/audit")
public class TestCaseAuditController {
    
    @Resource
    private TestCaseAuditService testCaseAuditService;
    
    /**
     * 创建测试用例
     */
    @Operation(summary = "创建测试用例")
    @PostMapping("/create")
    public TestCase createTestCase(@RequestBody TestCase testCase) {
        return testCaseAuditService.createTestCase(testCase);
    }
    
    /**
     * 更新测试用例
     */
    @Operation(summary = "更新测试用例")
    @PostMapping("/update")
    public TestCase updateTestCase(@RequestBody TestCase testCase) {
        return testCaseAuditService.updateTestCase(testCase);
    }
    
    /**
     * 删除测试用例
     */
    @Operation(summary = "删除测试用例")
    @DeleteMapping("/delete/{testCaseId}")
    public void deleteTestCase(@PathVariable String testCaseId) {
        testCaseAuditService.deleteTestCase(testCaseId);
    }
    
    /**
     * 批量删除测试用例
     */
    @Operation(summary = "批量删除测试用例")
    @PostMapping("/batch-delete")
    public void batchDeleteTestCases(@RequestBody List<String> testCaseIds) {
        testCaseAuditService.batchDeleteTestCases(testCaseIds);
    }
    
    /**
     * 直接在 Controller 使用注解的示例
     */
    @Operation(summary = "复制测试用例")
    @AuditLog(
        module = "TEST_CASE_MANAGEMENT",
        content = "复制测试用例: #{#testCaseId} 到项目: #{#targetProjectId}",
        sourceId = "#{#testCaseId}",
        history = false
    )
    @PostMapping("/copy/{testCaseId}")
    public void copyTestCase(@PathVariable String testCaseId, @RequestParam String targetProjectId) {
        // 复制测试用例的业务逻辑
        // testCaseService.copyTestCase(testCaseId, targetProjectId);
    }
    
    /**
     * 执行测试用例
     */
    @Operation(summary = "执行测试用例")
    @AuditLog(
        module = "TEST_CASE_MANAGEMENT",
        content = "执行测试用例: #{#testCaseId}",
        sourceId = "#{#testCaseId}",
        history = false
    )
    @PostMapping("/execute/{testCaseId}")
    public void executeTestCase(@PathVariable String testCaseId, @RequestParam String result) {
        testCaseAuditService.executeTestCase(testCaseId, result);
    }
}