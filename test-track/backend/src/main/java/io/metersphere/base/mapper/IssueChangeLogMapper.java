package io.metersphere.base.mapper;

import io.metersphere.base.domain.IssueChangeLog;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IssueChangeLogMapper {
    int insert(IssueChangeLog record);

    IssueChangeLog selectByPrimaryKey(String id);

    List<IssueChangeLog> selectByIssueId(@Param("issueId") String issueId);
}
