package io.metersphere.workflow.mapper;

import io.metersphere.workflow.domain.WfModelDeploy;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfModelDeployMapper {

    @Insert("INSERT INTO wf_model_deploy(id, model_id, deployment_id, process_definition_id, process_definition_key, process_definition_name, process_definition_version, deployed_time) " +
            "VALUES(#{id}, #{modelId}, #{deploymentId}, #{processDefinitionId}, #{processDefinitionKey}, #{processDefinitionName}, #{processDefinitionVersion}, #{deployedTime})")
    int insert(WfModelDeploy deploy);
}
