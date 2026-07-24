package io.metersphere.service.scenario;

import io.metersphere.api.dto.automation.ApiScenarioDTO;
import io.metersphere.api.dto.automation.RunScenarioRequest;
import io.metersphere.api.dto.automation.SaveApiScenarioRequest;
import io.metersphere.api.dto.definition.RunDefinitionRequest;
import io.metersphere.api.dto.definition.request.MsScenario;
import io.metersphere.api.dto.definition.request.variable.ScenarioVariable;
import io.metersphere.base.domain.ApiScenarioWithBLOBs;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Aspect
@Component
public class ApiScenarioVariableCopyAspect {

    /**
     * 仅用于 run() 同步查询场景定义阶段；异步及嵌套引用通过 MsScenario.executionUserId 显式传递。
     */
    private static final ThreadLocal<String> EXECUTION_USER = new ThreadLocal<>();

    @Resource
    private ApiScenarioVariableCopyService variableCopyService;
    @Resource
    private PlatformTransactionManager transactionManager;

    /**
     * 直接拦截场景业务服务，避免 Controller 代理顺序或可选文件参数导致切点未命中。
     */
    @AfterReturning(
            value = "execution(* io.metersphere.service.scenario.ApiScenarioService.getNewApiScenario(String)) && args(id)",
            returning = "scenario")
    public void applyCurrentUserCopyToDetails(String id, ApiScenarioDTO scenario) {
        variableCopyService.applyToScenarioDto(scenario, SessionUtils.getUserId());
    }

    @Before("execution(* io.metersphere.service.scenario.ApiScenarioService.create(..)) && args(request,..)")
    public void clearCopyMarkersBeforeCreate(SaveApiScenarioRequest request) {
        variableCopyService.prepareCreate(request);
    }

    /**
     * 把副本写入和原场景更新放入同一事务，任一环节失败时整体回滚。
     */
    @Around("execution(* io.metersphere.service.scenario.ApiScenarioService.update(..)) && args(request,..)")
    public Object reconcileScenarioUpdate(ProceedingJoinPoint joinPoint, SaveApiScenarioRequest request) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> {
            String userId = SessionUtils.getUserId();
            validateUpdateRequest(request, userId);
            normalizeImportedVariables(request);
            variableCopyService.reconcileUpdate(request, userId);
            verifyCopyMarkersCleared(request);
            try {
                return joinPoint.proceed();
            } catch (RuntimeException runtimeException) {
                status.setRollbackOnly();
                throw runtimeException;
            } catch (Throwable throwable) {
                status.setRollbackOnly();
                throw new IllegalStateException(throwable);
            }
        });
    }

    @Around("execution(* io.metersphere.api.exec.scenario.ApiScenarioExecuteService.run(..)) && args(request)")
    public Object bindExecutionUser(ProceedingJoinPoint joinPoint, RunScenarioRequest request) throws Throwable {
        return proceedWithExecutionUser(joinPoint, resolveExecutionUser(request));
    }

    @Around("execution(* io.metersphere.api.exec.scenario.ApiScenarioExecuteService.debug(..)) " +
            "&& args(request,bodyFiles,scenarioFiles)")
    public Object bindDebugUser(ProceedingJoinPoint joinPoint, RunDefinitionRequest request,
                                List<MultipartFile> bodyFiles, List<MultipartFile> scenarioFiles) throws Throwable {
        String userId = SessionUtils.getUserId();
        variableCopyService.applyToTestElement(request.getTestElement(), request.getScenarioId(), userId);
        return joinPoint.proceed();
    }

    @AfterReturning(
            value = "execution(* io.metersphere.base.mapper.ApiScenarioMapper.selectByExampleWithBLOBs(..))",
            returning = "scenarios")
    public void applyCopiesToRunScenarios(List<ApiScenarioWithBLOBs> scenarios) {
        String executionUser = EXECUTION_USER.get();
        if (StringUtils.isBlank(executionUser) || scenarios == null) {
            return;
        }
        scenarios.forEach(scenario -> variableCopyService.applyToScenario(scenario, executionUser));
    }

    @AfterReturning("execution(* io.metersphere.service.scenario.ApiScenarioService.delete(String)) && args(id)")
    public void deleteScenarioCopies(String id) {
        variableCopyService.deleteByScenarioId(id);
    }

    @AfterReturning("execution(* io.metersphere.service.scenario.ApiScenarioService.deleteBatch(java.util.List)) && args(ids)")
    public void deleteBatchScenarioCopies(List<String> ids) {
        if (ids != null) {
            ids.forEach(variableCopyService::deleteByScenarioId);
        }
    }

    private void validateUpdateRequest(SaveApiScenarioRequest request, String userId) {
        if (request == null || StringUtils.isBlank(request.getId())) {
            MSException.throwException("场景变量隔离处理失败：场景ID为空");
        }
        if (StringUtils.isBlank(userId)) {
            MSException.throwException("场景变量隔离处理失败：当前用户为空");
        }
        if (!(request.getScenarioDefinition() instanceof MsScenario)) {
            MSException.throwException("场景变量隔离处理失败：场景定义解析异常");
        }
    }

    private void verifyCopyMarkersCleared(SaveApiScenarioRequest request) {
        MsScenario scenario = (MsScenario) request.getScenarioDefinition();
        if (scenario.getVariables() == null) {
            return;
        }
        boolean unresolved = scenario.getVariables().stream()
                .filter(variable -> variable != null)
                .anyMatch(variable -> StringUtils.isNotBlank(variable.getSourceVariableId())
                        || variable.getPersonalCopy() != null
                        || variable.getSourceScenarioVersion() != null);
        if (unresolved) {
            MSException.throwException("场景变量隔离处理失败，已阻止覆盖公共变量");
        }
    }

    private void normalizeImportedVariables(SaveApiScenarioRequest request) {
        MsScenario scenario = (MsScenario) request.getScenarioDefinition();
        if (scenario.getVariables() == null) {
            return;
        }
        for (ScenarioVariable variable : scenario.getVariables()) {
            if (variable != null && StringUtils.isNotBlank(variable.getSourceVariableId())
                    && !StringUtils.equals(variable.getId(), variable.getSourceVariableId())) {
                variable.setSourceVariableId(null);
                variable.setPersonalCopy(null);
                variable.setSourceScenarioVersion(null);
            }
        }
    }

    private Object proceedWithExecutionUser(ProceedingJoinPoint joinPoint, String executionUser) throws Throwable {
        String previousUser = EXECUTION_USER.get();
        if (StringUtils.isNotBlank(executionUser)) {
            EXECUTION_USER.set(executionUser);
        }
        try {
            return joinPoint.proceed();
        } finally {
            if (StringUtils.isBlank(previousUser)) {
                EXECUTION_USER.remove();
            } else {
                EXECUTION_USER.set(previousUser);
            }
        }
    }

    private String resolveExecutionUser(RunScenarioRequest request) {
        String sessionUserId = SessionUtils.getUserId();
        if (StringUtils.isNotBlank(sessionUserId)) {
            return sessionUserId;
        }
        if (request != null) {
            if (StringUtils.isNotBlank(request.getReportUserID())) {
                return request.getReportUserID();
            }
            if (StringUtils.isNotBlank(request.getRequestOriginator())) {
                return request.getRequestOriginator();
            }
        }
        return null;
    }
}
