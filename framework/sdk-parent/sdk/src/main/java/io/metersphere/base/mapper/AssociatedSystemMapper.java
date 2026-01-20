package io.metersphere.base.mapper;

import io.metersphere.base.domain.AssociatedSystem;
import io.metersphere.base.domain.AssociatedSystemExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AssociatedSystemMapper {
    long countByExample(AssociatedSystemExample example);

    int deleteByExample(AssociatedSystemExample example);

    int deleteByPrimaryKey(String id);

    int insert(AssociatedSystem record);

    int insertSelective(AssociatedSystem record);

    List<AssociatedSystem> selectByExample(AssociatedSystemExample example);

    AssociatedSystem selectByPrimaryKey(String id);

    int updateByExampleSelective(@Param("record") AssociatedSystem record, @Param("example") AssociatedSystemExample example);

    int updateByExample(@Param("record") AssociatedSystem record, @Param("example") AssociatedSystemExample example);

    int updateByPrimaryKeySelective(AssociatedSystem record);

    int updateByPrimaryKey(AssociatedSystem record);
}