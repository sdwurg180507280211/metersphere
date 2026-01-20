package io.metersphere.validate;

import io.metersphere.base.domain.AssociatedSystem;
import io.metersphere.commons.utils.CommonBeanFactory;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.dto.CustomFieldDao;
import io.metersphere.exception.CustomFieldValidateException;
import io.metersphere.i18n.Translator;
import io.metersphere.service.BaseAssociatedSystemService;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 所属系统字段验证器
 * 用于Excel导入时验证和转换所属系统字段的值
 * 
 * @author MeterSphere
 */
public class CustomFieldAssociatedSystemValidator extends AbstractCustomFieldValidator {

    protected Map<String, String> systemIdMap;
    protected Map<String, String> systemNameMap;
    protected Map<String, String> systemDescriptionMap;

    public CustomFieldAssociatedSystemValidator() {
        this.isKVOption = true;
        BaseAssociatedSystemService associatedSystemService = CommonBeanFactory.getBean(BaseAssociatedSystemService.class);
        String workspaceId = SessionUtils.getCurrentWorkspaceId();
        List<AssociatedSystem> systems = associatedSystemService.getAllAssociatedSystems(workspaceId);
        
        // 构建ID映射（Excel中可能直接填写ID）
        systemIdMap = systems.stream()
                .collect(Collectors.toMap(
                        system -> system.getId().toLowerCase(), 
                        AssociatedSystem::getId,
                        (v1, v2) -> v1  // 如果有重复，保留第一个
                ));
        
        // 构建Name映射（Excel中通常填写系统名称）
        systemNameMap = new HashMap<>();
        systems.forEach(system -> {
            if (StringUtils.isNotBlank(system.getName())) {
                systemNameMap.put(system.getName().toLowerCase(), system.getId());
            }
        });
        
        // 构建Description映射（Excel中可能填写系统简称/编码）
        systemDescriptionMap = new HashMap<>();
        systems.forEach(system -> {
            if (StringUtils.isNotBlank(system.getDescription())) {
                systemDescriptionMap.put(system.getDescription().toLowerCase(), system.getId());
            }
        });
    }

    @Override
    public void validate(CustomFieldDao customField, String value) throws CustomFieldValidateException {
        validateRequired(customField, value);
        if (StringUtils.isBlank(value)) {
            return;
        }
        
        String valueLower = value.toLowerCase().trim();
        
        // 检查是否是有效的系统ID、系统名称或系统简称
        if (systemIdMap.containsKey(valueLower) 
                || systemNameMap.containsKey(valueLower) 
                || systemDescriptionMap.containsKey(valueLower)) {
            return;
        }
        
        // 如果都不匹配，抛出验证异常
        throw new CustomFieldValidateException(
                String.format(Translator.get("custom_field_associated_system_tip"), 
                        customField.getName(), 
                        value)
        );
    }

    @Override
    public Object parse2Key(String keyOrValue, CustomFieldDao customField) {
        if (StringUtils.isBlank(keyOrValue)) {
            return keyOrValue;
        }
        
        String valueLower = keyOrValue.toLowerCase().trim();
        
        // 优先匹配ID（如果Excel中直接填写了ID）
        if (systemIdMap.containsKey(valueLower)) {
            return systemIdMap.get(valueLower);
        }
        
        // 其次匹配Description（系统简称/编码）
        if (systemDescriptionMap.containsKey(valueLower)) {
            return systemDescriptionMap.get(valueLower);
        }
        
        // 最后匹配Name（系统名称）
        if (systemNameMap.containsKey(valueLower)) {
            return systemNameMap.get(valueLower);
        }
        
        // 如果都不匹配，返回原值（会在validate阶段报错）
        return keyOrValue;
    }
}


