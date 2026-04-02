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

        // 查询用户所属的用户组，获取 sourceId 列表
        UserGroupExample userGroupExample = new UserGroupExample();
        userGroupExample.createCriteria().andUserIdEqualTo(userId);
        List<UserGroup> userGroups = userGroupMapper.selectByExample(userGroupExample);

        if (userGroups.isEmpty()) {
            return List.of();
        }

        // 提取 sourceId 列表（可能是工作空间ID或项目ID）
        List<String> sourceIds = userGroups.stream()
                .map(UserGroup::getSourceId)
                .distinct()
                .collect(Collectors.toList());

        // 查询所有有权限的项目
        ProjectExample example = new ProjectExample();
        ProjectExample.Criteria criteria = example.createCriteria();

        // 第一种情况：用户对工作空间有权限，则该工作空间下的所有项目都有权限
        ProjectExample.Criteria orCriteria = example.or();
        orCriteria.andWorkspaceIdIn(sourceIds);

        // 第二种情况：用户对项目直接有权限
        ProjectExample.Criteria orCriteria2 = example.or();
        orCriteria2.andIdIn(sourceIds);

        example.setOrderByClause("update_time DESC");

        List<Project> projects = projectMapper.selectByExample(example);

        // 转换为 DTO 并去重
        List<ProjectSimpleDTO> result = projects.stream()
                .map(this::convertToSimpleDTO)
                .distinct()
                .collect(Collectors.toList());

        // 如果指定了工作空间，则在结果中进一步过滤
        if (workspaceIds != null && !workspaceIds.isEmpty()) {
            result = result.stream()
                    .filter(p -> workspaceIds.contains(p.getWorkspaceId()))
                    .collect(Collectors.toList());
        }

        return result;
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
