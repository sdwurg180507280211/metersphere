package io.metersphere.workstation.service;

import io.metersphere.base.domain.Project;
import io.metersphere.base.domain.ProjectExample;
import io.metersphere.base.domain.UserGroup;
import io.metersphere.base.domain.UserGroupExample;
import io.metersphere.base.mapper.ProjectMapper;
import io.metersphere.base.mapper.UserGroupMapper;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.workstation.dto.ProjectSimpleDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目查询服务
 * 
 * 为高级检索功能提供项目列表查询能力
 * 支持按工作空间过滤，只返回当前用户有权限访问的项目
 * 
 * @author MeterSphere
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProjectQueryService {
    
    @Resource
    private ProjectMapper projectMapper;
    
    @Resource
    private UserGroupMapper userGroupMapper;
    
    /**
     * 获取当前用户有权限访问的项目列表
     * 
     * 我在做：查询当前用户有权限访问的所有项目
     * 目的是：为项目选择器提供数据源
     * 如果不这样做：用户可能看到无权限访问的项目
     * 
     * @param workspaceIds 工作空间ID列表（可选，用于过滤）
     * @return 项目列表
     */
    public List<ProjectSimpleDTO> getUserProjects(List<String> workspaceIds) {
        String userId = SessionUtils.getUserId();
        
        // 查询用户所属的用户组，获取项目ID列表
        UserGroupExample userGroupExample = new UserGroupExample();
        userGroupExample.createCriteria().andUserIdEqualTo(userId);
        List<UserGroup> userGroups = userGroupMapper.selectByExample(userGroupExample);
        
        // 提取项目ID（sourceId 可能是工作空间ID或项目ID）
        List<String> sourceIds = userGroups.stream()
                .map(UserGroup::getSourceId)
                .distinct()
                .collect(Collectors.toList());
        
        if (sourceIds.isEmpty()) {
            return List.of();
        }
        
        // 构建查询条件
        ProjectExample example = new ProjectExample();
        ProjectExample.Criteria criteria = example.createCriteria();
        
        // 查询 sourceId 对应的项目，或者 workspaceId 在 sourceIds 中的项目
        criteria.andIdIn(sourceIds);
        
        // 如果指定了工作空间，则只查询该工作空间下的项目
        if (workspaceIds != null && !workspaceIds.isEmpty()) {
            ProjectExample.Criteria workspaceCriteria = example.or();
            workspaceCriteria.andWorkspaceIdIn(workspaceIds);
        }
        
        example.setOrderByClause("update_time DESC");
        
        List<Project> projects = projectMapper.selectByExample(example);
        
        // 转换为 DTO
        return projects.stream()
                .map(this::convertToSimpleDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将 Project 实体转换为 ProjectSimpleDTO
     * 
     * @param project 项目实体
     * @return 项目简要信息 DTO
     */
    private ProjectSimpleDTO convertToSimpleDTO(Project project) {
        ProjectSimpleDTO dto = new ProjectSimpleDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setWorkspaceId(project.getWorkspaceId());
        dto.setDescription(project.getDescription());
        return dto;
    }
}
