package io.metersphere.base.mapper.ext;

import io.metersphere.request.AdvancedSearchRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 高级检索扩展 Mapper 接口
 * 
 * 提供跨工作空间、跨项目的动态查询能力
 * 支持测试用例、缺陷、测试计划、用例评审四种业务模块的查询
 * 
 * @author MeterSphere
 */
public interface ExtAdvancedSearchMapper {
    
    /**
     * 查询测试用例列表
     * 
     * @param request 查询请求参数
     * @return 测试用例列表
     */
    List<Map<String, Object>> queryTestCases(@Param("request") AdvancedSearchRequest request);
    
    /**
     * 查询缺陷列表
     * 
     * @param request 查询请求参数
     * @return 缺陷列表
     */
    List<Map<String, Object>> queryIssues(@Param("request") AdvancedSearchRequest request);
    
    /**
     * 查询测试计划列表
     * 
     * @param request 查询请求参数
     * @return 测试计划列表
     */
    List<Map<String, Object>> queryTestPlans(@Param("request") AdvancedSearchRequest request);
    
    /**
     * 查询用例评审列表
     * 
     * @param request 查询请求参数
     * @return 用例评审列表
     */
    List<Map<String, Object>> queryTestCaseReviews(@Param("request") AdvancedSearchRequest request);
}
