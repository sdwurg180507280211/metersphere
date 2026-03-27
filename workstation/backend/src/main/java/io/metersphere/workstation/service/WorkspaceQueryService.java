package io.metersphere.workstation.service;

import io.metersphere.base.domain.Workspace;
import io.metersphere.base.domain.WorkspaceExample;
import io.metersphere.base.domain.UserGroup;
import io.metersphere.base.domain.UserGroupExample;
import io.metersphere.base.mapper.WorkspaceMapper;
import io.metersphere.base.mapper.UserGroupMapper;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.workstation.dto.WorkspaceSimpleDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作空间查询服务
 * 
 * 为高级检索功能提供工作空间列表查询能力
 * 只返回当前用户有权限访问的工作空间
 * 
 * @author MeterSphere
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class WorkspaceQueryService {
    
    @Resource
    private WorkspaceMapper workspaceMapper;
    
    @Resource
    private UserGroupMapper userGroupMapper;
    
    /**
     * 获取当前用户有权限访问的工作空间列表
     * 
     * 我在做：查询当前用户有权限访问的所有工作空间
     * 目的是：为工作空间选择器提供数据源
     * 如果不这样做：用户可能看到无权限访问的工作空间
     * 
     * @return 工作空间列表
     */
    public List<WorkspaceSimpleDTO> getUserWorkspaces() {
        String userId = SessionUtils.getUserId();
        
        // 查询用户所属的用户组，获取工作空间ID列表
        UserGroupExample userGroupExample = new UserGroupExample();
        userGroupExample.createCriteria().andUserIdEqualTo(userId);
        List<UserGroup> userGroups = userGroupMapper.selectByExample(userGroupExample);
        
        // 提取工作空间ID（sourceId 就是工作空间ID或项目ID）
        // 需要过滤出工作空间级别的权限
        List<String> workspaceIds = userGroups.stream()
                .map(UserGroup::getSourceId)
                .distinct()
                .collect(Collectors.toList());
        
        if (workspaceIds.isEmpty()) {
            return List.of();
        }
        
        // 查询工作空间详情
        WorkspaceExample example = new WorkspaceExample();
        example.createCriteria().andIdIn(workspaceIds);
        example.setOrderByClause("update_time DESC");
        
        List<Workspace> workspaces = workspaceMapper.selectByExample(example);
        
        // 转换为 DTO
        return workspaces.stream()
                .map(this::convertToSimpleDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将 Workspace 实体转换为 WorkspaceSimpleDTO
     * 
     * @param workspace 工作空间实体
     * @return 工作空间简要信息 DTO
     */
    private WorkspaceSimpleDTO convertToSimpleDTO(Workspace workspace) {
        WorkspaceSimpleDTO dto = new WorkspaceSimpleDTO();
        dto.setId(workspace.getId());
        dto.setName(workspace.getName());
        dto.setDescription(workspace.getDescription());
        return dto;
    }
}
