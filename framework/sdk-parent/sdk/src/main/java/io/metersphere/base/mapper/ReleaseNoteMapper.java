package io.metersphere.base.mapper;

import io.metersphere.base.domain.ReleaseNote;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 需求上线记录 MyBatis Mapper 接口
 * 提供 release_note 表的标准 CRUD 操作及自定义查询方法
 */
public interface ReleaseNoteMapper {

    /** 插入一条上线记录 */
    int insert(ReleaseNote record);

    /** 按主键选择性更新（仅更新非 null 字段） */
    int updateByPrimaryKeySelective(ReleaseNote record);

    /** 按主键删除上线记录 */
    int deleteByPrimaryKey(String id);

    /** 按主键查询单条上线记录 */
    ReleaseNote selectByPrimaryKey(String id);

    /** 查询所有上线记录（配合 PageHelper 实现分页），按 create_time 倒序 */
    List<ReleaseNote> selectAll();

    /** 自定义 SQL 查询最近 N 条上线记录，按 create_time 倒序 */
    List<ReleaseNote> selectRecent(@Param("limit") int limit);
}
