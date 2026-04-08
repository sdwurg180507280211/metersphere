package io.metersphere.excel.domain;

import com.alibaba.excel.annotation.ExcelIgnore;
import io.metersphere.dto.CustomFieldDao;
import io.metersphere.excel.constants.TestCaseImportFiled;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Getter
@Setter
public class TestCaseExcelData {
    @ExcelIgnore
    private String id;
    @ExcelIgnore
    private Integer num;
    @ExcelIgnore
    private String customNum;
    @ExcelIgnore
    private String name;
    @ExcelIgnore
    private String nodePath;
    @ExcelIgnore
    private String importModuleError;
    @ExcelIgnore
    private String tags;
    @ExcelIgnore
    private String prerequisite;
    @ExcelIgnore
    private String remark;
    @ExcelIgnore
    private String stepDesc;
    @ExcelIgnore
    private String stepResult;
    @ExcelIgnore
    private String stepModel;
    @ExcelIgnore
    private String demand;

    /**
     * 责任人
     * 用例状态
     * 用例等级
     */
    @ExcelIgnore
    private String status;
    @ExcelIgnore
    private String maintainer;
    @ExcelIgnore
    private String priority;
    @ExcelIgnore
    Map<String, Object> customData = new LinkedHashMap<>();
    @ExcelIgnore
    Set<String> textFieldSet = new HashSet<>(2);

    @ExcelIgnore
    List<String> mergeStepDesc;
    @ExcelIgnore
    List<String> mergeStepResult;
    @ExcelIgnore
    Map<String, String> otherFields;

    public List<List<String>> getHead(boolean needNum, List<CustomFieldDao> customFields) {
        return new ArrayList<>();
    }

    public List<List<String>> getHead(boolean needNum, List<CustomFieldDao> customFields, Locale lang) {
        List<List<String>> heads = new ArrayList<>();
        TestCaseImportFiled[] fields = TestCaseImportFiled.values();
        for (TestCaseImportFiled field : fields) {
            heads.add(Arrays.asList(field.getFiledLangMap().get(lang)));
        }

        Iterator<List<String>> iterator = heads.iterator();

        while (iterator.hasNext()) {
            List<String> head = iterator.next();
            // 移除ID列(当不需要时)
            if (StringUtils.equals(head.get(0), TestCaseImportFiled.ID.getFiledLangMap().get(lang)) && !needNum) {
                iterator.remove();
                continue;
            }
            // 移除责任人列
            if (StringUtils.equals(head.get(0), TestCaseImportFiled.MAINTAINER.getFiledLangMap().get(lang))) {
                iterator.remove();
                continue;
            }
            // 移除用例状态列
            if (StringUtils.equals(head.get(0), TestCaseImportFiled.STATUS.getFiledLangMap().get(lang))) {
                iterator.remove();
                continue;
            }
        }

        if (CollectionUtils.isNotEmpty(customFields)) {
            for (CustomFieldDao dto : customFields) {
                if (StringUtils.equalsAny(dto.getName(),
                        TestCaseImportFiled.PRIORITY.getFiledLangMap().get(Locale.SIMPLIFIED_CHINESE),
                        TestCaseImportFiled.STATUS.getFiledLangMap().get(Locale.SIMPLIFIED_CHINESE),
                        TestCaseImportFiled.MAINTAINER.getFiledLangMap().get(Locale.SIMPLIFIED_CHINESE))) {
                    continue;
                }
                heads.add(new ArrayList<>() {{
                    add(dto.getName());
                }});
            }
        }
        return heads;
    }
}
