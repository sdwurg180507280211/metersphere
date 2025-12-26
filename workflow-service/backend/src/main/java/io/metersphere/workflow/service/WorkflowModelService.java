package io.metersphere.workflow.service;

import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.commons.user.SessionUser;
import io.metersphere.workflow.domain.WfModel;
import io.metersphere.workflow.domain.WfModelDeploy;
import io.metersphere.workflow.dto.DeployResultView;
import io.metersphere.workflow.dto.ModelView;
import io.metersphere.workflow.dto.SaveModelRequest;
import io.metersphere.workflow.mapper.WfModelDeployMapper;
import io.metersphere.workflow.mapper.WfModelMapper;
import io.metersphere.workflow.util.BpmnXmlConverter;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.*;

@Service
@Transactional(rollbackFor = Exception.class)
public class WorkflowModelService {

    public static final String CATEGORY_ISSUE = "issue";

    @Resource
    private WfModelMapper wfModelMapper;

    @Resource
    private WfModelDeployMapper wfModelDeployMapper;

    @Resource
    private RepositoryService repositoryService;

    public ModelView save(SaveModelRequest request) {
        validateSaveRequest(request);

        String category = CATEGORY_ISSUE;
        String modelKey = request.getModelKey().trim();

        long now = System.currentTimeMillis();
        SessionUser user = SessionUtils.getUser();
        String userId = user == null ? null : user.getId();

        int maxVersion = wfModelMapper.selectMaxVersion(category, modelKey);
        int newVersion = maxVersion + 1;

        wfModelMapper.markNotLatest(category, modelKey, userId, now);

        WfModel model = new WfModel();
        model.setId(UUID.randomUUID().toString());
        model.setCategory(category);
        model.setModelKey(modelKey);
        model.setName(request.getName().trim());
        model.setVersion(newVersion);
        model.setLatest(true);
        model.setXml(request.getXml());
        model.setSvg(request.getSvg());
        model.setCreatedBy(userId);
        model.setCreatedTime(now);
        model.setUpdatedBy(userId);
        model.setUpdatedTime(now);

        wfModelMapper.insert(model);
        return toView(model, true);
    }

    public List<ModelView> list(String keyword, Boolean latestOnly) {
        List<WfModel> list = wfModelMapper.list(CATEGORY_ISSUE, keyword, latestOnly == null ? Boolean.TRUE : latestOnly);
        List<ModelView> out = new ArrayList<>();
        for (WfModel m : list) {
            out.add(toView(m, false));
        }
        return out;
    }

    public ModelView get(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        WfModel model = wfModelMapper.selectById(id);
        if (model == null) {
            return null;
        }
        return toView(model, true);
    }

    public DeployResultView deploy(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            throw new IllegalArgumentException("modelId is required");
        }
        WfModel model = wfModelMapper.selectById(modelId);
        if (model == null) {
            throw new IllegalArgumentException("model not found");
        }

        String xmlForDeploy = BpmnXmlConverter.toFlowableForDeployment(model.getXml());

        String resourceName = model.getModelKey() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment()
                .name(model.getName())
                .key(model.getModelKey())
                .addString(resourceName, xmlForDeploy)
                .deploy();

        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .latestVersion()
                .singleResult();

        long now = System.currentTimeMillis();
        WfModelDeploy record = new WfModelDeploy();
        record.setId(UUID.randomUUID().toString());
        record.setModelId(model.getId());
        record.setDeploymentId(deployment.getId());
        if (pd != null) {
            record.setProcessDefinitionId(pd.getId());
            record.setProcessDefinitionKey(pd.getKey());
            record.setProcessDefinitionName(pd.getName());
            record.setProcessDefinitionVersion(pd.getVersion());
        }
        record.setDeployedTime(now);
        wfModelDeployMapper.insert(record);

        DeployResultView out = new DeployResultView();
        out.setModelId(model.getId());
        out.setDeploymentId(deployment.getId());
        if (pd != null) {
            out.setProcessDefinitionId(pd.getId());
            out.setProcessDefinitionKey(pd.getKey());
            out.setProcessDefinitionName(pd.getName());
            out.setProcessDefinitionVersion(pd.getVersion());
        }
        return out;
    }

    private void validateSaveRequest(SaveModelRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!StringUtils.hasText(request.getModelKey())) {
            throw new IllegalArgumentException("modelKey is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("name is required");
        }
        if (!StringUtils.hasText(request.getXml())) {
            throw new IllegalArgumentException("xml is required");
        }
        // category 目前固定 issue，允许前端不传
        if (StringUtils.hasText(request.getCategory()) && !CATEGORY_ISSUE.equals(request.getCategory())) {
            throw new IllegalArgumentException("only category 'issue' is supported");
        }
    }

    private ModelView toView(WfModel model, boolean withContent) {
        ModelView v = new ModelView();
        v.setId(model.getId());
        v.setCategory(model.getCategory());
        v.setModelKey(model.getModelKey());
        v.setName(model.getName());
        v.setVersion(model.getVersion());
        v.setLatest(model.getLatest());
        if (withContent) {
            v.setXml(model.getXml());
            v.setSvg(model.getSvg());
        }
        v.setCreatedBy(model.getCreatedBy());
        v.setCreatedTime(model.getCreatedTime());
        v.setUpdatedBy(model.getUpdatedBy());
        v.setUpdatedTime(model.getUpdatedTime());
        return v;
    }
}
