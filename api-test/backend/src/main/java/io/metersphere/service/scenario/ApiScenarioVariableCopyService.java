package io.metersphere.service.scenario;

import io.metersphere.api.dto.automation.ApiScenarioDTO;
import io.metersphere.api.dto.automation.SaveApiScenarioRequest;
import io.metersphere.api.dto.automation.ScenarioVariableOverrideDTO;
import io.metersphere.api.dto.definition.request.MsScenario;
import io.metersphere.api.dto.definition.request.variable.ScenarioVariable;
import io.metersphere.base.domain.ApiScenarioWithBLOBs;
import io.metersphere.base.mapper.ApiScenarioMapper;
import io.metersphere.base.mapper.ext.ExtApiScenarioVariableOverrideMapper;
import io.metersphere.commons.constants.MsTestElementConstants;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.GenerateHashTreeUtil;
import io.metersphere.commons.utils.JSON;
import io.metersphere.plugin.core.MsTestElement;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApiScenarioVariableCopyService {

    @Resource
    private ApiScenarioMapper apiScenarioMapper;
    @Resource
    private ExtApiScenarioVariableOverrideMapper variableOverrideMapper;

    /**
     * 场景详情保持原有 JSON 结构，只把当前用户的副本无感覆盖到公共变量上。
     */
    public void applyToScenarioDto(ApiScenarioDTO scenario, String userId) {
        if (scenario == null || StringUtils.isBlank(scenario.getScenarioDefinition())) {
            return;
        }
        MsScenario definition = GenerateHashTreeUtil.parseScenarioDefinition(scenario.getScenarioDefinition());
        if (definition == null) {
            return;
        }
        List<ScenarioVariable> effectiveVariables = getEffectiveVariables(
                scenario.getId(), definition.getVariables(), userId, true);
        effectiveVariables.forEach(variable -> variable.setSourceScenarioVersion(scenario.getVersion()));
        definition.setVariables(effectiveVariables);
        scenario.setScenarioDefinition(JSON.toJSONString(definition));
    }

    /**
     * 复制场景时把当前用户看到的有效值作为新场景公共值，但不携带来源标记。
     */
    public void prepareCreate(SaveApiScenarioRequest request) {
        if (request == null || !(request.getScenarioDefinition() instanceof MsScenario)) {
            return;
        }
        MsScenario scenario = (MsScenario) request.getScenarioDefinition();
        scenario.setExecutionUserId(null);
        safeList(scenario.getVariables()).forEach(this::clearCopyMarker);
    }

    /**
     * 运行场景时使用执行用户的有效变量，副本字段不会写入公共场景定义。
     */
    public void applyToScenario(ApiScenarioWithBLOBs scenario, String userId) {
        if (scenario == null || StringUtils.isBlank(scenario.getScenarioDefinition()) || StringUtils.isBlank(userId)) {
            return;
        }
        MsScenario definition = GenerateHashTreeUtil.parseScenarioDefinition(scenario.getScenarioDefinition());
        if (definition == null) {
            return;
        }
        applyToElement(definition, scenario.getId(), userId, true);
        scenario.setScenarioDefinition(JSON.toJSONString(definition));
    }

    /**
     * 调试请求保留页面上尚未保存的顶层变量，只解析其中引用场景的用户副本。
     */
    public void applyToTestElement(MsTestElement element, String scenarioId, String userId) {
        if (element == null || StringUtils.isBlank(userId)) {
            return;
        }
        applyToElement(element, scenarioId, userId, false);
    }

    /**
     * 引用场景加载最新源定义后，继续把执行用户传递到其内部嵌套引用。
     */
    public void applyToElements(List<MsTestElement> elements, String userId) {
        if (CollectionUtils.isEmpty(elements) || StringUtils.isBlank(userId)) {
            return;
        }
        for (MsTestElement element : elements) {
            applyToElement(element, null, userId, false);
        }
    }

    /**
     * 供引用场景在生成脚本时按显式执行用户解析变量，不依赖线程上下文。
     */
    public List<ScenarioVariable> resolveExecutionVariables(String scenarioId, List<ScenarioVariable> publicVariables,
                                                             String userId) {
        return getEffectiveVariables(scenarioId, publicVariables, userId, false);
    }

    private void applyToElement(MsTestElement element, String fallbackScenarioId, String userId, boolean root) {
        if (element instanceof MsScenario) {
            MsScenario scenario = (MsScenario) element;
            scenario.setExecutionUserId(userId);
            String currentScenarioId = StringUtils.defaultIfBlank(scenario.getId(), fallbackScenarioId);
            boolean referenced = StringUtils.equals(scenario.getReferenced(), MsTestElementConstants.REF.name());
            if (root) {
                scenario.setVariables(getEffectiveVariables(currentScenarioId, scenario.getVariables(), userId, false));
            } else if (referenced) {
                scenario.setVariables(getEffectiveVariables(currentScenarioId,
                        loadPublicVariables(currentScenarioId, scenario.getVariables()), userId, false));
            }
        }
        if (CollectionUtils.isNotEmpty(element.getHashTree())) {
            for (MsTestElement child : element.getHashTree()) {
                applyToElement(child, fallbackScenarioId, userId, false);
            }
        }
    }

    private List<ScenarioVariable> loadPublicVariables(String scenarioId, List<ScenarioVariable> fallbackVariables) {
        if (StringUtils.isBlank(scenarioId)) {
            return fallbackVariables;
        }
        ApiScenarioWithBLOBs scenario = apiScenarioMapper.selectByPrimaryKey(scenarioId);
        if (scenario == null || StringUtils.isBlank(scenario.getScenarioDefinition())) {
            return fallbackVariables;
        }
        MsScenario definition = GenerateHashTreeUtil.parseScenarioDefinition(scenario.getScenarioDefinition());
        return definition == null ? fallbackVariables : definition.getVariables();
    }

    /**
     * 保存场景前拆分公共变量与当前用户副本。
     * 新增变量进入公共定义；编辑公共变量写入用户副本；删除公共变量并删除所有用户副本。
     */
    public void reconcileUpdate(SaveApiScenarioRequest request, String userId) {
        if (request == null || StringUtils.isBlank(request.getId()) || request.getScenarioDefinition() == null
                || StringUtils.isBlank(userId) || !(request.getScenarioDefinition() instanceof MsScenario)) {
            return;
        }
        MsScenario incomingScenario = (MsScenario) request.getScenarioDefinition();
        incomingScenario.setExecutionUserId(null);
        List<ScenarioVariable> incomingVariables = safeList(incomingScenario.getVariables());

        // 与原场景更新处于同一事务，避免多人同时保存时公共变量列表被最后一次写入覆盖。
        variableOverrideMapper.lockScenario(request.getId());
        ApiScenarioWithBLOBs storedScenario = apiScenarioMapper.selectByPrimaryKey(request.getId());
        if (storedScenario == null || StringUtils.isBlank(storedScenario.getScenarioDefinition())) {
            return;
        }
        MsScenario storedDefinition = GenerateHashTreeUtil.parseScenarioDefinition(storedScenario.getScenarioDefinition());
        if (storedDefinition == null) {
            return;
        }
        List<ScenarioVariable> publicVariables = cloneVariables(storedDefinition.getVariables());
        ensureVariableIds(request.getId(), publicVariables);
        Map<String, ScenarioVariable> publicVariableMap = publicVariables.stream()
                .filter(variable -> variable != null && StringUtils.isNotBlank(variable.getId()))
                .collect(Collectors.toMap(ScenarioVariable::getId, variable -> variable,
                        (left, right) -> left, LinkedHashMap::new));

        Set<String> submittedSourceIds = incomingVariables.stream()
                .map(variable -> resolveSourceVariableId(variable, publicVariableMap))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean hasDeletion = publicVariables.stream()
                .filter(variable -> variable != null && StringUtils.isNotBlank(variable.getId()))
                .anyMatch(variable -> !submittedSourceIds.contains(variable.getId()));
        boolean hasAddition = incomingVariables.stream()
                .filter(Objects::nonNull)
                .anyMatch(variable -> StringUtils.isBlank(resolveSourceVariableId(variable, publicVariableMap)));
        boolean hasExistingVariableEdit = incomingVariables.stream().anyMatch(variable -> {
            String sourceId = resolveSourceVariableId(variable, publicVariableMap);
            return StringUtils.isNotBlank(sourceId)
                    && !sameVariable(sanitize(variable), publicVariableMap.get(sourceId));
        });
        boolean copyOnWriteRequest = incomingVariables.stream().anyMatch(this::hasCopyMarker)
                || (incomingVariables.isEmpty() && !publicVariables.isEmpty())
                || hasDeletion || hasAddition || hasExistingVariableEdit;
        if (!copyOnWriteRequest) {
            return;
        }

        Integer markerVersion = incomingVariables.stream()
                .filter(Objects::nonNull)
                .map(ScenarioVariable::getSourceScenarioVersion)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
        Integer submittedVersion = request.getVersion();
        if (markerVersion != null && (submittedVersion == null || markerVersion > submittedVersion)) {
            submittedVersion = markerVersion;
        }
        boolean publicVariableChanged = hasDeletion || hasAddition;
        if (publicVariableChanged && storedScenario.getVersion() != null
                && !Objects.equals(submittedVersion, storedScenario.getVersion())) {
            MSException.throwException("场景变量已被其他人修改，请刷新后重试");
        }

        validateUniqueNames(incomingVariables);
        List<String> deletedVariableIds = publicVariables.stream()
                .filter(variable -> variable != null && StringUtils.isNotBlank(variable.getId()))
                .map(ScenarioVariable::getId)
                .filter(id -> !submittedSourceIds.contains(id))
                .collect(Collectors.toList());

        List<ScenarioVariable> mergedPublicVariables = publicVariables.stream()
                .filter(variable -> variable != null && !deletedVariableIds.contains(variable.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
        Set<String> publicNames = mergedPublicVariables.stream()
                .map(ScenarioVariable::getName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(HashSet::new));

        for (ScenarioVariable incoming : incomingVariables) {
            if (incoming == null) {
                continue;
            }
            String sourceId = resolveSourceVariableId(incoming, publicVariableMap);
            if (StringUtils.isNotBlank(sourceId)) {
                ScenarioVariable source = publicVariableMap.get(sourceId);
                if (source == null || deletedVariableIds.contains(sourceId)) {
                    MSException.throwException("场景变量已被删除，请刷新后重试");
                }
                ScenarioVariable copy = sanitize(incoming);
                copy.setId(source.getId());
                if (Boolean.TRUE.equals(incoming.getPersonalCopy()) || !sameVariable(copy, source)) {
                    saveUserCopy(request.getId(), source.getId(), userId, copy);
                }
            } else {
                ScenarioVariable newPublicVariable = sanitize(incoming);
                if (StringUtils.isBlank(newPublicVariable.getId())) {
                    newPublicVariable.setId(UUID.randomUUID().toString());
                }
                if (StringUtils.isNotBlank(newPublicVariable.getName()) && !publicNames.add(newPublicVariable.getName())) {
                    MSException.throwException("场景变量名称重复：" + newPublicVariable.getName());
                }
                mergedPublicVariables.add(newPublicVariable);
            }
        }

        if (CollectionUtils.isNotEmpty(deletedVariableIds)) {
            variableOverrideMapper.deleteByScenarioAndVariableIds(request.getId(), deletedVariableIds);
        }

        mergedPublicVariables.forEach(this::clearCopyMarker);
        incomingScenario.setVariables(mergedPublicVariables);
        if (storedScenario.getVersion() != null) {
            request.setVersion(storedScenario.getVersion());
        }
    }

    public void deleteByScenarioId(String scenarioId) {
        if (StringUtils.isNotBlank(scenarioId)) {
            variableOverrideMapper.deleteByScenarioId(scenarioId);
        }
    }

    private List<ScenarioVariable> getEffectiveVariables(String scenarioId, List<ScenarioVariable> publicVariables,
                                                          String userId, boolean includeMarkers) {
        List<ScenarioVariable> normalizedPublicVariables = cloneVariables(publicVariables);
        ensureVariableIds(scenarioId, normalizedPublicVariables);
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(scenarioId)) {
            normalizedPublicVariables.forEach(this::clearCopyMarker);
            return normalizedPublicVariables;
        }

        Map<String, ScenarioVariable> copies = new HashMap<>();
        List<ScenarioVariableOverrideDTO> records = variableOverrideMapper.selectByScenarioAndUser(scenarioId, userId);
        if (CollectionUtils.isNotEmpty(records)) {
            for (ScenarioVariableOverrideDTO record : records) {
                try {
                    ScenarioVariable copy = JSON.parseObject(record.getVariableJson(), ScenarioVariable.class);
                    copies.put(record.getVariableId(), copy);
                } catch (Exception ignore) {
                    // 单条异常副本不影响其他公共变量展示和执行。
                }
            }
        }

        List<ScenarioVariable> effectiveVariables = new ArrayList<>();
        for (ScenarioVariable publicVariable : normalizedPublicVariables) {
            if (publicVariable == null) {
                continue;
            }
            ScenarioVariable effective = copies.containsKey(publicVariable.getId())
                    ? cloneVariable(copies.get(publicVariable.getId()))
                    : cloneVariable(publicVariable);
            effective.setId(publicVariable.getId());
            if (includeMarkers) {
                effective.setSourceVariableId(publicVariable.getId());
                effective.setPersonalCopy(copies.containsKey(publicVariable.getId()));
            } else {
                clearCopyMarker(effective);
            }
            effectiveVariables.add(effective);
        }
        return effectiveVariables;
    }

    private void saveUserCopy(String scenarioId, String variableId, String userId, ScenarioVariable variable) {
        long now = System.currentTimeMillis();
        ScenarioVariableOverrideDTO record = new ScenarioVariableOverrideDTO();
        record.setId(UUID.randomUUID().toString());
        record.setScenarioId(scenarioId);
        record.setVariableId(variableId);
        record.setUserId(userId);
        record.setVariableJson(JSON.toJSONString(variable));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        variableOverrideMapper.upsert(record);
    }

    private String resolveSourceVariableId(ScenarioVariable variable, Map<String, ScenarioVariable> publicVariableMap) {
        if (variable == null) {
            return null;
        }
        if (StringUtils.isNotBlank(variable.getSourceVariableId())) {
            return variable.getSourceVariableId();
        }
        if (StringUtils.isNotBlank(variable.getId()) && publicVariableMap.containsKey(variable.getId())) {
            return variable.getId();
        }
        return null;
    }

    private boolean hasCopyMarker(ScenarioVariable variable) {
        return variable != null && (StringUtils.isNotBlank(variable.getSourceVariableId())
                || variable.getPersonalCopy() != null || variable.getSourceScenarioVersion() != null);
    }

    private boolean sameVariable(ScenarioVariable left, ScenarioVariable right) {
        if (left == null || right == null) {
            return left == right;
        }
        return StringUtils.equals(JSON.toJSONString(sanitize(left)), JSON.toJSONString(sanitize(right)));
    }

    private void validateUniqueNames(List<ScenarioVariable> variables) {
        Set<String> names = new HashSet<>();
        for (ScenarioVariable variable : variables) {
            if (variable != null && StringUtils.isNotBlank(variable.getName()) && !names.add(variable.getName())) {
                MSException.throwException("场景变量名称重复：" + variable.getName());
            }
        }
    }

    private void ensureVariableIds(String scenarioId, List<ScenarioVariable> variables) {
        for (int index = 0; index < variables.size(); index++) {
            ScenarioVariable variable = variables.get(index);
            if (variable != null && StringUtils.isBlank(variable.getId())) {
                String seed = StringUtils.defaultString(scenarioId) + ":" + index + ":"
                        + StringUtils.defaultString(variable.getName());
                variable.setId(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString());
            }
        }
    }

    private List<ScenarioVariable> safeList(List<ScenarioVariable> variables) {
        return variables == null ? new ArrayList<>() : variables;
    }

    private List<ScenarioVariable> cloneVariables(List<ScenarioVariable> variables) {
        if (CollectionUtils.isEmpty(variables)) {
            return new ArrayList<>();
        }
        return variables.stream().filter(variable -> variable != null).map(this::cloneVariable)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ScenarioVariable cloneVariable(ScenarioVariable variable) {
        return JSON.parseObject(JSON.toJSONString(variable), ScenarioVariable.class);
    }

    private ScenarioVariable sanitize(ScenarioVariable variable) {
        ScenarioVariable clean = cloneVariable(variable);
        clearCopyMarker(clean);
        return clean;
    }

    private void clearCopyMarker(ScenarioVariable variable) {
        if (variable != null) {
            variable.setSourceVariableId(null);
            variable.setPersonalCopy(null);
            variable.setSourceScenarioVersion(null);
        }
    }
}
