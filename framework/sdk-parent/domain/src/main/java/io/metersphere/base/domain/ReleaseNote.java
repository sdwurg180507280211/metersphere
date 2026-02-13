package io.metersphere.base.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 需求上线记录实体类
 * 对应数据库表 release_note，用于存储每次需求上线的内容记录
 */
@Data
public class ReleaseNote implements Serializable {

    /** UUID 主键 */
    private String id;

    /** 上线标题（最长100字符） */
    private String title;

    /** 内容详情（最长2000字符） */
    private String content;

    /** 创建人 ID */
    private String creator;

    /** 创建时间戳（毫秒） */
    private Long createTime;

    /** 更新时间戳（毫秒） */
    private Long updateTime;

    private static final long serialVersionUID = 1L;
}
