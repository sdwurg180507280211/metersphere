package io.metersphere.excel.domain;

import lombok.Data;

import java.util.List;

@Data
public class ExcelResponse<T> {

    private Boolean success;
    private List<ExcelErrData<T>> errList;
    private Boolean isUpdated;  //是否有更新过用例
    private Integer createdCount;
    private Integer updatedCount;
    private Integer failedCount;
    private String importRootPath;
    private String importRootType;


}
