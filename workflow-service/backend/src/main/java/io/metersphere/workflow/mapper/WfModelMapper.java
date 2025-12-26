package io.metersphere.workflow.mapper;

import io.metersphere.workflow.domain.WfModel;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WfModelMapper {

    @Select("SELECT COALESCE(MAX(version), 0) FROM wf_model WHERE category=#{category} AND model_key=#{modelKey}")
    int selectMaxVersion(@Param("category") String category, @Param("modelKey") String modelKey);

    @Update("UPDATE wf_model SET latest=0, updated_by=#{updatedBy}, updated_time=#{updatedTime} WHERE category=#{category} AND model_key=#{modelKey} AND latest=1")
    int markNotLatest(@Param("category") String category,
                      @Param("modelKey") String modelKey,
                      @Param("updatedBy") String updatedBy,
                      @Param("updatedTime") long updatedTime);

    @Insert("INSERT INTO wf_model(id, category, model_key, name, version, latest, xml, svg, created_by, created_time, updated_by, updated_time) " +
            "VALUES(#{id}, #{category}, #{modelKey}, #{name}, #{version}, #{latest}, #{xml}, #{svg}, #{createdBy}, #{createdTime}, #{updatedBy}, #{updatedTime})")
    int insert(WfModel model);

    @Select("SELECT id, category, model_key as modelKey, name, version, latest, xml, svg, created_by as createdBy, created_time as createdTime, updated_by as updatedBy, updated_time as updatedTime " +
            "FROM wf_model WHERE id=#{id}")
    WfModel selectById(@Param("id") String id);

    @Select({
            "<script>",
            "SELECT id, category, model_key as modelKey, name, version, latest, created_by as createdBy, created_time as createdTime, updated_by as updatedBy, updated_time as updatedTime ",
            "FROM wf_model ",
            "WHERE 1=1 ",
            "<if test=\"category!=null and category!=''\"> AND category=#{category} </if>",
            "<if test=\"keyword!=null and keyword!=''\"> AND (name LIKE CONCAT('%', #{keyword}, '%') OR model_key LIKE CONCAT('%', #{keyword}, '%')) </if>",
            "<if test=\"latestOnly!=null and latestOnly\"> AND latest=1 </if>",
            "ORDER BY updated_time DESC ",
            "</script>"
    })
    List<WfModel> list(@Param("category") String category,
                       @Param("keyword") String keyword,
                       @Param("latestOnly") Boolean latestOnly);
}
