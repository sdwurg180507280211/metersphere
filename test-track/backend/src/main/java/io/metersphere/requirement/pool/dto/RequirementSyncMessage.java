package io.metersphere.requirement.pool.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 需求同步消息DTO，用于RocketMQ消息传输
 * 字段命名与需求平台保持一致
 */
@Getter
@Setter
public class RequirementSyncMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 需求编号（唯一主键） */
    private String dmpNum;

    /** 需求名称（注意：需求平台字段名为name1） */
    private String name1;

    /** 操作类型：CREATED/UPDATED/CANCELLED */
    private String operationType;

    /** 需求负责人 */
    private String reqManagerName;

    /** 当前环节 */
    private String actName;

    /** 需求提出时间（毫秒时间戳） */
    private Long createTime;

    /** 主流程编码 */
    private String parentWfinstCode;

    /** 需求大类 */
    private String reqFatherClass;

    /** 需求子类 */
    private String reqSonClass;

    /** 所属系统 */
    private String systemName;

    /** 预计上线时间（毫秒时间戳） */
    private Long upTime;

    /** 当前处理人 */
    private String assigneeName;

    /** 需求申请部门（注意：需求平台字段名无下划线） */
    private String createdept;

    /** 需求申请人（注意：后缀为1） */
    private String createUser1;

    /** 需求负责人处室 */
    private String deptName;

    /** 创建人 */
    private String startUserName;

    /** 消息事件时间（毫秒时间戳），用于幂等和乱序判断 */
    private Long eventTime;

    /** 追踪ID，全链路问题排查 */
    private String traceId;
}
