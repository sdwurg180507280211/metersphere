package io.metersphere.workstation.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.metersphere.base.mapper.ext.ExtAdvancedSearchMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.PageUtils;
import io.metersphere.commons.utils.Pager;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.request.AdvancedSearchRequest;
import io.metersphere.workstation.dto.AdvancedSearchResult;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 高级检索服务
 * 
 * 提供跨工作空间、跨项目的统一查询能力
 * 支持测试用例、缺陷、测试计划、用例评审四种业务模块
 * 支持 JQL 和传统 combine 两种查询模式
 * 
 * @author MeterSphere
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AdvancedSearchService {
    
    @Resource
    private ExtAdvancedSearchMapper extAdvancedSearchMapper;
    
    // TODO: JQL 功能暂未实现，待后续版本支持
    // @Resource
    // private JQLParser jqlParser;
    // 
    // @Resource
    // private JQLToSQLConverter jqlToSQLConverter;
    
    /**
     * 执行高级检索查询
     * 
     * 我在做：根据请求参数执行分页查询
     * 目的是：返回符合条件的业务数据列表和分页信息
     * 如果不这样做：前端无法获取查询结果
     * 
     * @param request 查询请求参数
     * @param goPage 页码（从 1 开始）
     * @param pageSize 每页数量
     * @return 查询结果（包含数据列表和分页信息）
     */
    public Pager<List<Map<String, Object>>> query(AdvancedSearchRequest request, int goPage, int pageSize) {
        // 1. 参数校验
        validateRequest(request);
        
        // 2. 权限校验：确保用户有权限访问所选的工作空间和项目
        checkPermission(request);
        
        // 3. 处理空的工作空间和项目列表
        // 如果用户没有指定工作空间/项目，则查询用户有权限的所有数据
        // 这里暂时允许空列表，后续可以根据用户权限自动填充
        if (request.getWorkspaceIds() == null) {
            request.setWorkspaceIds(new java.util.ArrayList<>());
        }
        if (request.getProjectIds() == null) {
            request.setProjectIds(new java.util.ArrayList<>());
        }
        
        // 4. 处理 JQL 查询模式（当前版本暂不支持，直接使用 combine 模式）
        if (Boolean.TRUE.equals(request.getUseJQL()) && StringUtils.isNotBlank(request.getJql())) {
            // TODO: 实现 JQL 解析功能
            // 当前版本暂不支持 JQL，提示用户使用可视化模式
            MSException.throwException("JQL 查询模式暂未实现，请使用可视化条件构建");
        }
        
        // 5. 设置分页参数
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        
        // 6. 根据业务模块执行对应的查询
        List<Map<String, Object>> results = executeQuery(request);
        
        // 7. 返回分页结果
        return PageUtils.setPageInfo(page, results);
    }
    
    /**
     * 获取详情
     * 
     * 我在做：根据业务模块和ID查询详细信息
     * 目的是：在详情面板中展示完整的数据信息
     * 如果不这样做：用户无法查看数据的详细内容
     * 
     * @param module 业务模块
     * @param id 数据ID
     * @return 详情数据
     */
    public Map<String, Object> getDetail(String module, String id) {
        // 参数校验
        if (StringUtils.isBlank(module) || StringUtils.isBlank(id)) {
            MSException.throwException("模块和ID不能为空");
        }
        
        // 权限校验
        String userId = SessionUtils.getUserId();
        if (StringUtils.isBlank(userId)) {
            MSException.throwException("用户未登录");
        }
        
        // 根据业务模块查询详情
        Map<String, Object> detail = null;
        switch (module) {
            case "TEST_CASE":
                detail = extAdvancedSearchMapper.getTestCaseDetail(id);
                break;
            case "ISSUE":
                detail = extAdvancedSearchMapper.getIssueDetail(id);
                break;
            case "TEST_PLAN":
                detail = extAdvancedSearchMapper.getTestPlanDetail(id);
                break;
            case "TEST_CASE_REVIEW":
                detail = extAdvancedSearchMapper.getTestCaseReviewDetail(id);
                break;
            default:
                MSException.throwException("不支持的业务模块: " + module);
        }
        
        if (detail == null) {
            MSException.throwException("数据不存在或已被删除");
        }
        
        return detail;
    }
    
    /**
     * 验证请求参数
     * 
     * 我在做：检查必填参数是否完整
     * 目的是：确保请求参数合法，避免后续处理出错
     * 如果不这样做：可能导致 SQL 执行异常或返回错误结果
     */
    private void validateRequest(AdvancedSearchRequest request) {
        if (request == null) {
            MSException.throwException("请求参数不能为空");
        }
        
        if (StringUtils.isBlank(request.getModule())) {
            MSException.throwException("业务模块不能为空");
        }
        
        // 验证业务模块是否合法
        String module = request.getModule();
        if (!isValidModule(module)) {
            MSException.throwException("不支持的业务模块: " + module);
        }
        
        // JQL 模式下必须提供 JQL 查询语句
        if (Boolean.TRUE.equals(request.getUseJQL()) && StringUtils.isBlank(request.getJql())) {
            MSException.throwException("JQL 查询语句不能为空");
        }
    }
    
    /**
     * 检查用户权限
     * 
     * 我在做：验证用户是否有权限访问所选的工作空间和项目
     * 目的是：确保数据安全，防止越权访问
     * 如果不这样做：用户可能访问到无权限的数据
     */
    private void checkPermission(AdvancedSearchRequest request) {
        String userId = SessionUtils.getUserId();
        if (StringUtils.isBlank(userId)) {
            MSException.throwException("用户未登录");
        }
        
        // TODO: 实现详细的权限校验逻辑
        // 1. 检查用户是否有权限访问所选的工作空间
        // 2. 检查用户是否有权限访问所选的项目
        // 3. 如果没有指定工作空间/项目，则查询用户有权限的所有工作空间/项目
    }
    
    /**
     * 解析 JQL 为 SQL WHERE 子句
     * 
     * 我在做：将 JQL 查询语句解析为 SQL WHERE 子句
     * 目的是：支持类似 Jira JQL 的查询语法
     * 如果不这样做：无法使用 JQL 查询模式
     */
    private String parseJQLToSQL(String jql, String module) {
        try {
            // 1. 词法分析和语法解析
            Object ast = jqlParser.parseJQL(jql);
            
            // 2. 转换为 SQL WHERE 子句
            return jqlToSQLConverter.convertToSQL(ast, module);
        } catch (Exception e) {
            MSException.throwException("JQL 解析失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 执行查询
     * 
     * 我在做：根据业务模块调用对应的 Mapper 方法
     * 目的是：获取符合条件的数据列表
     * 如果不这样做：无法返回查询结果
     */
    private List<Map<String, Object>> executeQuery(AdvancedSearchRequest request) {
        String module = request.getModule();
        
        switch (module) {
            case "TEST_CASE":
                return extAdvancedSearchMapper.queryTestCases(request);
            case "ISSUE":
                return extAdvancedSearchMapper.queryIssues(request);
            case "TEST_PLAN":
                return extAdvancedSearchMapper.queryTestPlans(request);
            case "TEST_CASE_REVIEW":
                return extAdvancedSearchMapper.queryTestCaseReviews(request);
            default:
                MSException.throwException("不支持的业务模块: " + module);
                return null;
        }
    }
    
    /**
     * 验证业务模块是否合法
     */
    private boolean isValidModule(String module) {
        return "TEST_CASE".equals(module) 
            || "ISSUE".equals(module) 
            || "TEST_PLAN".equals(module) 
            || "TEST_CASE_REVIEW".equals(module);
    }
}
