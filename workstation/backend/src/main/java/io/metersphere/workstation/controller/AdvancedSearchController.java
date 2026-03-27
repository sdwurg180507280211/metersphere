package io.metersphere.workstation.controller;

import io.metersphere.commons.utils.Pager;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.request.AdvancedSearchRequest;
import io.metersphere.workstation.dto.JQLSuggestion;
import io.metersphere.workstation.dto.JQLValidationResult;
import io.metersphere.workstation.dto.ProjectSimpleDTO;
import io.metersphere.workstation.dto.UserSimpleDTO;
import io.metersphere.workstation.dto.WorkspaceSimpleDTO;
import io.metersphere.workstation.service.AdvancedSearchService;
import io.metersphere.workstation.service.FieldMetadataService;
import io.metersphere.workstation.service.JQLParser;
import io.metersphere.workstation.service.JQLSuggestionService;
import io.metersphere.workstation.service.ProjectQueryService;
import io.metersphere.workstation.service.UserQueryService;
import io.metersphere.workstation.service.WorkspaceQueryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 高级检索控制器
 * 
 * 提供高级检索相关的 REST API 接口
 * 支持跨工作空间、跨项目的统一查询能力
 * 
 * @author MeterSphere
 */
@RequestMapping("workstation/advanced-search")
@RestController
public class AdvancedSearchController {
    
    @Resource
    private AdvancedSearchService advancedSearchService;
    
    @Resource
    private FieldMetadataService fieldMetadataService;
    
    @Resource
    private JQLParser jqlParser;
    
    @Resource
    private UserQueryService userQueryService;
    
    @Resource
    private WorkspaceQueryService workspaceQueryService;
    
    @Resource
    private ProjectQueryService projectQueryService;
    
    @Resource
    private JQLSuggestionService jqlSuggestionService;
    
    /**
     * 执行高级检索查询
     * 
     * 我在做：接收前端的查询请求，调用 Service 层执行查询
     * 目的是：返回符合条件的数据列表和分页信息
     * 如果不这样做：前端无法获取查询结果
     * 
     * @param request 查询请求参数
     * @param goPage 页码（从 1 开始）
     * @param pageSize 每页数量
     * @return 分页查询结果
     */
    @PostMapping("/query/{goPage}/{pageSize}")
    public Pager<List<Map<String, Object>>> query(
            @RequestBody AdvancedSearchRequest request,
            @PathVariable int goPage,
            @PathVariable int pageSize) {
        return advancedSearchService.query(request, goPage, pageSize);
    }
    
    /**
     * 获取字段元数据
     * 
     * 我在做：返回指定业务模块的可筛选字段列表
     * 目的是：前端根据字段元数据动态渲染筛选条件输入控件
     * 如果不这样做：前端无法知道有哪些字段可以筛选
     * 
     * @param module 业务模块（TEST_CASE, ISSUE, TEST_PLAN, TEST_CASE_REVIEW）
     * @param projectId 项目ID（可选，传入时返回该项目的自定义字段）
     * @return 字段元数据（包含系统字段和自定义字段）
     */
    @GetMapping("/fields/{module}")
    public Map<String, Object> getFieldMetadata(
            @PathVariable String module,
            @RequestParam(required = false) String projectId) {
        return fieldMetadataService.getFieldMetadata(module, projectId);
    }
    
    /**
     * 获取用户列表
     * 
     * 我在做：返回用户列表供用户选择器使用
     * 目的是：支持按创建人、维护人、指派人等用户字段筛选
     * 如果不这样做：无法进行用户维度的筛选
     * 
     * @param workspaceIds 工作空间ID列表（逗号分隔）
     * @param keyword 搜索关键词（用户名/姓名）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 用户列表
     */
    @GetMapping("/users")
    public Pager<List<UserSimpleDTO>> getUsers(
            @RequestParam(required = false) String workspaceIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return userQueryService.getUsers(workspaceIds, keyword, pageNum, pageSize);
    }
    
    /**
     * 获取工作空间列表
     * 
     * 我在做：返回当前用户有权限访问的工作空间列表
     * 目的是：为工作空间选择器提供数据源
     * 如果不这样做：无法进行工作空间筛选
     * 
     * @return 工作空间列表
     */
    @GetMapping("/workspaces")
    public List<WorkspaceSimpleDTO> getWorkspaces() {
        return workspaceQueryService.getUserWorkspaces();
    }
    
    /**
     * 获取项目列表
     * 
     * 我在做：返回当前用户有权限访问的项目列表
     * 目的是：为项目选择器提供数据源，支持按工作空间过滤
     * 如果不这样做：无法进行项目筛选
     * 
     * @param workspaceIds 工作空间ID列表（逗号分隔，可选）
     * @return 项目列表
     */
    @GetMapping("/projects")
    public List<ProjectSimpleDTO> getProjects(
            @RequestParam(required = false) String workspaceIds) {
        List<String> wsIdList = null;
        if (workspaceIds != null && !workspaceIds.isEmpty()) {
            wsIdList = Arrays.asList(workspaceIds.split(","));
        }
        return projectQueryService.getUserProjects(wsIdList);
    }
    
    /**
     * 获取详情
     * 
     * 我在做：返回指定业务数据的详细信息
     * 目的是：支持详情视图模式，显示完整的数据内容
     * 如果不这样做：无法查看数据的详细信息
     * 
     * @param module 业务模块
     * @param id 数据ID
     * @return 详情数据
     */
    @GetMapping("/detail/{module}/{id}")
    public Map<String, Object> getDetail(
            @PathVariable String module,
            @PathVariable String id) {
        return advancedSearchService.getDetail(module, id);
    }
    
    /**
     * 验证 JQL 语法
     * 
     * 我在做：验证用户输入的 JQL 查询语句是否正确
     * 目的是：在用户输入时实时提供语法错误提示
     * 如果不这样做：用户只能在执行查询时才知道语法错误
     * 
     * @param request 验证请求（包含 jql 和 module）
     * @return 验证结果（包含是否通过、错误信息、修复建议）
     */
    @PostMapping("/jql/validate")
    public JQLValidationResult validateJQL(@RequestBody Map<String, String> request) {
        String jql = request.get("jql");
        String module = request.get("module");
        return jqlParser.validateJQL(jql, module);
    }
    
    /**
     * 获取 JQL 智能提示
     * 
     * 我在做：根据当前输入上下文返回智能提示列表
     * 目的是：帮助用户快速构建正确的 JQL 查询语句
     * 如果不这样做：用户需要记住所有字段名和操作符
     * 
     * @param request 提示请求（包含 context, module, cursorPosition）
     * @return 智能提示列表（字段名、操作符、值等）
     */
    @PostMapping("/jql/suggestions")
    public List<JQLSuggestion> getJQLSuggestions(@RequestBody Map<String, Object> request) {
        String context = (String) request.get("context");
        String module = (String) request.get("module");
        Integer cursorPosition = (Integer) request.get("cursorPosition");
        
        return jqlSuggestionService.getSuggestions(context, module, cursorPosition);
    }
    
    /**
     * 导出查询结果
     * 
     * 我在做：将查询结果导出为 Excel 文件
     * 目的是：支持离线分析或分享给他人
     * 如果不这样做：用户只能在线查看数据
     * 
     * @param request 查询请求参数
     */
    @PostMapping("/export")
    public void exportExcel(@RequestBody AdvancedSearchRequest request) {
        // TODO: 实现 Excel 导出逻辑
        // 1. 执行查询获取全部数据（限制最大 10000 条）
        // 2. 使用 EasyExcel 生成 Excel 文件
        // 3. 设置响应头并输出文件流
    }
}
