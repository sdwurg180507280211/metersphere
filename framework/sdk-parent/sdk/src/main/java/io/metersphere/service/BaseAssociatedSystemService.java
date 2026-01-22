package io.metersphere.service;

import io.metersphere.base.domain.AssociatedSystem;
import io.metersphere.base.domain.AssociatedSystemExample;
import io.metersphere.base.mapper.ext.BaseAssociatedSystemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author : lijiaxin
 * @date 2025-12-18 13:43
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseAssociatedSystemService {

    @Resource
    private BaseAssociatedSystemMapper baseAssociatedSystemMapper;

    public List<AssociatedSystem> getAllAssociatedSystems(String workspaceId) {
        // 不再按工作空间过滤，返回所有所属系统
        return baseAssociatedSystemMapper.getAssociatedSystemWithWorkspaceId(workspaceId);
    }

}
