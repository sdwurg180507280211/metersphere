package io.metersphere.workstation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 高级检索查询结果 DTO
 * 
 * 包装查询结果列表和分页信息
 * 泛型 T 表示具体的业务数据类型（测试用例、缺陷、测试计划等）
 * 
 * @author MeterSphere
 * @param <T> 业务数据类型
 */
@Getter
@Setter
public class AdvancedSearchResult<T> {
    
    /**
     * 总记录数（不受分页影响）
     * 用于前端计算总页数和显示统计信息
     */
    private Long total;
    
    /**
     * 当前页数据列表
     * 包含符合筛选条件的业务数据记录
     */
    private List<T> list;
    
    /**
     * 当前页码（从 1 开始）
     */
    private Integer pageNum;
    
    /**
     * 每页数量
     * 默认 20 条，最大 100 条
     */
    private Integer pageSize;
    
    /**
     * 构造函数 - 创建空结果
     */
    public AdvancedSearchResult() {
        this.total = 0L;
        this.pageNum = 1;
        this.pageSize = 20;
    }
    
    /**
     * 构造函数 - 创建带数据的结果
     * 
     * @param list 数据列表
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页数量
     */
    public AdvancedSearchResult(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}
