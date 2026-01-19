package io.metersphere.service;

import io.metersphere.base.domain.*;
import io.metersphere.base.mapper.AssociatedSystemMapper;
import io.metersphere.base.mapper.ext.BaseAssociatedSystemMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.CommonBeanFactory;
import io.metersphere.i18n.Translator;
import io.metersphere.request.AssociatedSystemRequest;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * @Author : lijiaxin
 * @date 2025-12-15 14:34
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AssociatedSystemService {
    @Resource
    private AssociatedSystemMapper associatedSystemMapper;
    @Resource
    private BaseAssociatedSystemMapper baseAssociatedSystemMapper;

    public AssociatedSystem addAssociatedSystem(AssociatedSystem associatedSystem) {
        CommonBeanFactory.getBean(AssociatedSystemService.class).saveAssociatedSystem(associatedSystem);
        return associatedSystem;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveAssociatedSystem(AssociatedSystem associatedSystem) {
        if (StringUtils.isBlank(associatedSystem.getName())) {
            MSException.throwException(Translator.get("associated_system_name_is_null"));
        }
        AssociatedSystemExample example = new AssociatedSystemExample();
        example.createCriteria().andWorkspaceIdEqualTo(associatedSystem.getWorkspaceId()).andNameEqualTo(associatedSystem.getName());
        if (associatedSystemMapper.countByExample(example) > 0) {
            MSException.throwException(Translator.get("associated_system_already_exists"));
        }
        String pjId = UUID.randomUUID().toString();
        associatedSystem.setId(pjId);

        long createTime = System.currentTimeMillis();
        associatedSystem.setCreateTime(createTime);
        associatedSystem.setUpdateTime(createTime);
        associatedSystemMapper.insertSelective(associatedSystem);
    }

    public void deleteAssociatedSystemId(String associatedSystemId) {

        associatedSystemMapper.deleteByPrimaryKey(associatedSystemId);
    }

    public void updateAssociatedSystemId(AssociatedSystem associatedSystem) {
        associatedSystem.setUpdateTime(System.currentTimeMillis());
        associatedSystemMapper.updateByPrimaryKeySelective(associatedSystem);
    }

    public List<AssociatedSystem> getAssociatedSystemList(AssociatedSystemRequest request) {

        if (StringUtils.isNotBlank(request.getName())) {
            request.setName(StringUtils.wrapIfMissing(request.getName(), "%"));
        }
        request.setOrders(ServiceUtils.getDefaultOrder(request.getOrders()));
        return baseAssociatedSystemMapper.getAssociatedSystemWithWorkspace(request);


    }
}
