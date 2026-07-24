package io.metersphere.base.mapper.ext;

import io.metersphere.api.dto.automation.ScenarioVariableOverrideDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ExtApiScenarioVariableOverrideMapper {

    @Select("SELECT id FROM api_scenario WHERE id = #{scenarioId} FOR UPDATE")
    String lockScenario(@Param("scenarioId") String scenarioId);

    @Select("SELECT id, scenario_id AS scenarioId, variable_id AS variableId, user_id AS userId, " +
            "variable_json AS variableJson, create_time AS createTime, update_time AS updateTime " +
            "FROM api_scenario_variable_override WHERE scenario_id = #{scenarioId} AND user_id = #{userId}")
    List<ScenarioVariableOverrideDTO> selectByScenarioAndUser(@Param("scenarioId") String scenarioId,
                                                              @Param("userId") String userId);

    @Select("SELECT id, scenario_id AS scenarioId, variable_id AS variableId, user_id AS userId, " +
            "variable_json AS variableJson, create_time AS createTime, update_time AS updateTime " +
            "FROM api_scenario_variable_override WHERE scenario_id = #{scenarioId} AND variable_id = #{variableId}")
    List<ScenarioVariableOverrideDTO> selectByScenarioAndVariable(@Param("scenarioId") String scenarioId,
                                                                   @Param("variableId") String variableId);

    @Insert("INSERT INTO api_scenario_variable_override " +
            "(id, scenario_id, variable_id, user_id, variable_json, create_time, update_time) " +
            "VALUES (#{id}, #{scenarioId}, #{variableId}, #{userId}, #{variableJson}, #{createTime}, #{updateTime}) " +
            "ON DUPLICATE KEY UPDATE variable_json = VALUES(variable_json), update_time = VALUES(update_time)")
    int upsert(ScenarioVariableOverrideDTO record);

    @Delete({
            "<script>",
            "DELETE FROM api_scenario_variable_override",
            "WHERE scenario_id = #{scenarioId}",
            "AND variable_id IN",
            "<foreach collection='variableIds' item='variableId' open='(' separator=',' close=')'>",
            "#{variableId}",
            "</foreach>",
            "</script>"
    })
    int deleteByScenarioAndVariableIds(@Param("scenarioId") String scenarioId,
                                       @Param("variableIds") List<String> variableIds);

    @Delete("DELETE FROM api_scenario_variable_override WHERE scenario_id = #{scenarioId}")
    int deleteByScenarioId(@Param("scenarioId") String scenarioId);
}
