package io.metersphere.base.mapper;

import io.metersphere.base.domain.IssueChangeLogDetail;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IssueChangeLogDetailMapper {
    int insert(IssueChangeLogDetail record);

    IssueChangeLogDetail selectByPrimaryKey(String id);

    List<IssueChangeLogDetail> selectByLogIds(@Param("logIds") List<String> logIds);
}
