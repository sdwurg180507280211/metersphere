package io.metersphere.workstation.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.metersphere.base.domain.User;
import io.metersphere.base.domain.UserExample;
import io.metersphere.base.mapper.UserMapper;
import io.metersphere.base.mapper.ext.BaseUserMapper;
import io.metersphere.commons.utils.PageUtils;
import io.metersphere.commons.utils.Pager;
import io.metersphere.workstation.dto.UserSimpleDTO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户查询服务
 * 
 * 为高级检索功能提供用户列表查询能力
 * 支持按工作空间过滤和关键词搜索
 * 
 * @author MeterSphere
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserQueryService {
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private BaseUserMapper baseUserMapper;
    
    /**
     * 查询用户列表
     * 
     * 我在做：根据工作空间和关键词查询用户列表
     * 目的是：为用户选择器提供数据源
     * 如果不这样做：无法进行用户维度的筛选
     * 
     * @param workspaceIds 工作空间ID列表（逗号分隔）
     * @param keyword 搜索关键词（用户名/姓名/邮箱）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 用户列表（分页）
     */
    public Pager<List<UserSimpleDTO>> getUsers(String workspaceIds, String keyword, int pageNum, int pageSize) {
        Page<Object> page = PageHelper.startPage(pageNum, pageSize, true);
        
        // 构建查询条件
        UserExample example = new UserExample();
        UserExample.Criteria criteria = example.createCriteria();
        
        // 如果指定了工作空间，则只查询该工作空间下的用户
        if (StringUtils.isNotBlank(workspaceIds)) {
            List<String> wsIdList = Arrays.asList(workspaceIds.split(","));
            // TODO: 添加工作空间过滤逻辑
            // 需要关联 user_group 表查询指定工作空间下的用户
        }
        
        // 关键词搜索（用户名、姓名、邮箱）
        if (StringUtils.isNotBlank(keyword)) {
            UserExample.Criteria keywordCriteria1 = example.createCriteria();
            keywordCriteria1.andNameLike("%" + keyword + "%");
            
            UserExample.Criteria keywordCriteria2 = example.createCriteria();
            keywordCriteria2.andEmailLike("%" + keyword + "%");
            
            example.or(keywordCriteria1);
            example.or(keywordCriteria2);
        }
        
        // 按更新时间倒序
        example.setOrderByClause("update_time DESC");
        
        // 执行查询
        List<User> users = userMapper.selectByExample(example);
        
        // 转换为 UserSimpleDTO
        List<UserSimpleDTO> userDTOs = users.stream()
                .map(this::convertToSimpleDTO)
                .collect(Collectors.toList());
        
        return PageUtils.setPageInfo(page, userDTOs);
    }
    
    /**
     * 将 User 实体转换为 UserSimpleDTO
     * 
     * @param user 用户实体
     * @return 用户简要信息 DTO
     */
    private UserSimpleDTO convertToSimpleDTO(User user) {
        UserSimpleDTO dto = new UserSimpleDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        // 头像路径（如果有的话）
        // dto.setAvatar(user.getAvatar());
        return dto;
    }
}
