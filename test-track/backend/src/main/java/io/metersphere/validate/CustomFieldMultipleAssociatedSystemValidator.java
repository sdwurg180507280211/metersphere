package io.metersphere.validate;

import io.metersphere.dto.CustomFieldDao;

import io.metersphere.exception.CustomFieldValidateException;
import io.metersphere.i18n.Translator;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 多选所属系统字段验证器
 * 用于Excel导入时验证和转换多选所属系统字段的值
 * 
 * @author MeterSphere
 */
public class CustomFieldMultipleAssociatedSystemValidator extends CustomFieldAssociatedSystemValidator {

    @Override
    public void validate(CustomFieldDao customField, String value) throws CustomFieldValidateException {
        validateArrayRequired(customField, value);
        if (StringUtils.isBlank(value)) {
            return;
        }
        
        // 解析多个值（JSON数组格式或逗号分隔）
        List<String> values = parse2Array(customField.getName(), value);
        
        for (String item : values) {
            if (StringUtils.isBlank(item)) {
                continue;
            }
            
            String valueLower = item.toLowerCase().trim();
            
            // 检查每个值是否有效
            if (!systemIdMap.containsKey(valueLower) 
                    && !systemNameMap.containsKey(valueLower) 
                    && !systemDescriptionMap.containsKey(valueLower)) {
                throw new CustomFieldValidateException(
                        String.format(Translator.get("custom_field_associated_system_tip"), 
                                customField.getName(), 
                                item)
                );
            }
        }
    }

    @Override
    public Object parse2Key(String keyOrValuesStr, CustomFieldDao customField) {
        if (StringUtils.isBlank(keyOrValuesStr)) {
            return StringUtils.EMPTY;
        }
        
        // 解析多个值
        List<String> keyOrValues = parse2Array(keyOrValuesStr);
        
        // 将每个值转换为对应的ID
        for (int i = 0; i < keyOrValues.size(); i++) {
            String item = keyOrValues.get(i);
            if (StringUtils.isBlank(item)) {
                continue;
            }
            
            String valueLower = item.toLowerCase().trim();
            
            // 优先匹配ID
            if (systemIdMap.containsKey(valueLower)) {
                keyOrValues.set(i, systemIdMap.get(valueLower));
            }
            // 其次匹配Description（系统简称/编码）
            else if (systemDescriptionMap.containsKey(valueLower)) {
                keyOrValues.set(i, systemDescriptionMap.get(valueLower));
            }
            // 最后匹配Name
            else if (systemNameMap.containsKey(valueLower)) {
                keyOrValues.set(i, systemNameMap.get(valueLower));
            }
            // 如果都不匹配，保持原值（会在validate阶段报错）
        }
        
        return keyOrValues;
    }
}


