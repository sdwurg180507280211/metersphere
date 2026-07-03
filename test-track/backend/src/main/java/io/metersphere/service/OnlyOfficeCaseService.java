package io.metersphere.service;

import io.metersphere.base.domain.TestCaseExample;
import io.metersphere.base.domain.TestCaseWithBLOBs;
import io.metersphere.base.domain.TestPlanTestCaseExample;
import io.metersphere.base.domain.TestPlanTestCaseWithBLOBs;
import io.metersphere.base.domain.TestPlanWithBLOBs;
import io.metersphere.base.mapper.TestCaseMapper;
import io.metersphere.base.mapper.TestPlanMapper;
import io.metersphere.base.mapper.TestPlanTestCaseMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.JSON;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.constants.DataStatus;
import io.metersphere.dto.OnlyOfficeCaseSessionDTO;
import io.metersphere.dto.OnlyOfficeCaseSyncResultDTO;
import io.metersphere.dto.request.OnlyOfficeCallbackRequest;
import io.metersphere.dto.request.OnlyOfficeCaseSessionRequest;
import io.metersphere.excel.constants.TestPlanTestCaseStatus;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OnlyOfficeCaseService {

    private static final Logger Log = LoggerFactory.getLogger(OnlyOfficeCaseService.class);

    private static final String[] HEADERS = {
            "_plan_case_id", "_case_id", "_project_id", "_case_update_time",
            "用例编号", "自定义编号", "所属系统", "用例名称", "用例等级", "用例状态",
            "前置条件", "步骤描述", "预期结果", "备注",
            "计划执行状态", "执行人", "实际结果", "计划备注"
    };

    private static final int HIDDEN_COLUMN_COUNT = 4;
    private static final Set<String> CASE_STATUSES = Set.of("Prepare", "Underway", "Completed", "Finished");
    private static final Set<String> PLAN_CASE_STATUSES = Set.of(
            TestPlanTestCaseStatus.Prepare.name(),
            TestPlanTestCaseStatus.Pass.name(),
            TestPlanTestCaseStatus.Failure.name(),
            TestPlanTestCaseStatus.Blocking.name(),
            TestPlanTestCaseStatus.Skip.name()
    );
    private static final int ONLYOFFICE_STATUS_EDITING = 1;
    private static final int ONLYOFFICE_STATUS_READY_FOR_SAVE = 2;
    private static final int ONLYOFFICE_STATUS_CLOSED_NO_CHANGES = 4;
    private static final int ONLYOFFICE_STATUS_FORCE_SAVE = 6;
    private static final String SYNC_STATUS_READY = "ready";
    private static final String SYNC_STATUS_SAVING = "saving";
    private static final String SYNC_STATUS_SAVED = "saved";
    private static final String SYNC_STATUS_PARTIAL = "partial";
    private static final String SYNC_STATUS_ERROR = "error";

    @Resource
    private TestPlanMapper testPlanMapper;
    @Resource
    private TestPlanTestCaseMapper testPlanTestCaseMapper;
    @Resource
    private TestCaseMapper testCaseMapper;
    @Resource
    private TestCaseNodeService testCaseNodeService;

    @Value("${onlyoffice.document-server-url:http://localhost:8089}")
    private String documentServerUrl;
    @Value("${onlyoffice.document-server-internal-url:http://localhost:8089}")
    private String documentServerInternalUrl;
    @Value("${onlyoffice.storage-url:http://host.docker.internal:8005}")
    private String storageUrl;
    @Value("${onlyoffice.command-service-path:/command}")
    private String commandServicePath;
    @Value("${onlyoffice.jwt-enabled:true}")
    private Boolean jwtEnabled;
    @Value("${onlyoffice.jwt-secret:metersphere-onlyoffice-poc-secret}")
    private String jwtSecret;
    @Value("${onlyoffice.lang:zh-CN}")
    private String lang;

    private final Map<String, CaseEditSession> sessions = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final DataFormatter dataFormatter = new DataFormatter();

    public OnlyOfficeCaseSessionDTO createCaseSession(OnlyOfficeCaseSessionRequest request) {
        if (StringUtils.isBlank(request.getPlanId())) {
            MSException.throwException("测试计划 ID 不能为空");
        }
        TestPlanWithBLOBs plan = testPlanMapper.selectByPrimaryKey(request.getPlanId());
        if (plan == null) {
            MSException.throwException("测试计划不存在");
        }

        List<CaseExcelRow> rows = loadPlanCaseRows(plan.getId());
        String sessionId = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = "ms-case-" + sessionId.replace("-", "");
        Path filePath = getSessionFilePath(sessionId);
        writeWorkbook(filePath, rows);

        CaseEditSession session = new CaseEditSession();
        session.setSessionId(sessionId);
        session.setToken(token);
        session.setDocumentKey(key);
        session.setPlanId(plan.getId());
        session.setProjectId(plan.getProjectId());
        session.setFilePath(filePath);
        session.setCreatedBy(SessionUtils.getUserId());
        session.setCreatedAt(System.currentTimeMillis());
        session.setLastResult(buildInitialResult(sessionId, rows.size()));
        sessions.put(sessionId, session);

        OnlyOfficeCaseSessionDTO dto = new OnlyOfficeCaseSessionDTO();
        dto.setSessionId(sessionId);
        dto.setDocumentServerUrl(trimTrailingSlash(documentServerUrl));
        dto.setConfig(buildEditorConfig(plan, session));
        dto.setSyncResult(session.getLastResult());
        return dto;
    }

    public ResponseEntity<byte[]> loadSessionFile(String sessionId, String token) throws IOException {
        CaseEditSession session = getSession(sessionId, token);
        byte[] bytes = Files.readAllBytes(session.getFilePath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("metersphere-cases.xlsx", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(bytes);
    }

    public OnlyOfficeCaseSyncResultDTO forceSaveCaseSession(String sessionId) {
        CaseEditSession session = sessions.get(sessionId);
        if (session == null) {
            MSException.throwException("ONLYOFFICE 编辑会话不存在或已过期，请重新生成 Excel");
        }
        if (StringUtils.isNotBlank(session.getCreatedBy()) && !StringUtils.equals(session.getCreatedBy(), SessionUtils.getUserId())) {
            MSException.throwException("只能保存自己创建的 ONLYOFFICE 编辑会话");
        }

        String saveRequestId = UUID.randomUUID().toString();
        OnlyOfficeCaseSyncResultDTO result = buildSavingResult(session, saveRequestId);
        session.setSaveRequestId(saveRequestId);
        session.setSaveRequestedAt(result.getSaveRequestedAt());
        session.setLastResult(result);

        try {
            Map<String, Object> response = sendForceSaveCommand(session, saveRequestId);
            Integer error = getInteger(response.get("error"));
            if (Objects.equals(error, 0)) {
                return result;
            }
            result.setStatus(SYNC_STATUS_ERROR);
            result.setMessage(getCommandErrorMessage(error));
            result.getErrors().add(result.getMessage());
            result.setUpdatedAt(System.currentTimeMillis());
            session.setLastResult(result);
            return result;
        } catch (Exception e) {
            result.setStatus(SYNC_STATUS_ERROR);
            result.setMessage("触发 ONLYOFFICE 保存失败：" + e.getMessage());
            result.getErrors().add(result.getMessage());
            result.setUpdatedAt(System.currentTimeMillis());
            session.setLastResult(result);
            return result;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleCallback(String sessionId, String token, String authorization, OnlyOfficeCallbackRequest request) {
        if (request == null || request.getStatus() == null) {
            return Map.of("error", 0);
        }
        CaseEditSession session = sessions.get(sessionId);
        if (session == null || !StringUtils.equals(session.getToken(), token)) {
            if (isOnlyOfficeStatusWithoutDocumentChanges(request.getStatus())) {
                Log.warn("忽略过期的 ONLYOFFICE 无变更回调，sessionId={}, status={}", sessionId, request.getStatus());
                return Map.of("error", 0);
            }
            Log.warn("ONLYOFFICE 回调会话不存在或 token 无效，sessionId={}, status={}", sessionId, request.getStatus());
            return Map.of("error", 1, "message", "ONLYOFFICE 编辑会话不存在或 token 无效");
        }
        if (Boolean.TRUE.equals(jwtEnabled) && StringUtils.isNotBlank(jwtSecret) && !verifyJwt(authorization)) {
            return Map.of("error", 1, "message", "Invalid ONLYOFFICE token");
        }
        if (Objects.equals(request.getStatus(), ONLYOFFICE_STATUS_CLOSED_NO_CHANGES) && isSessionSaving(session)) {
            OnlyOfficeCaseSyncResultDTO result = buildNoChangeSavedResult(session, request.getStatus(), request.getUserdata());
            session.setLastResult(result);
            return Map.of("error", 0);
        }
        if (isOnlyOfficeStatusWithoutDocumentChanges(request.getStatus())) {
            Log.debug("忽略 ONLYOFFICE 非保存回调，sessionId={}, status={}", sessionId, request.getStatus());
            return Map.of("error", 0);
        }

        OnlyOfficeCaseSyncResultDTO result = new OnlyOfficeCaseSyncResultDTO();
        result.setSessionId(sessionId);
        result.setDocumentStatus(request.getStatus());
        result.setUpdatedAt(System.currentTimeMillis());
        result.setSaveRequestId(firstNotBlank(request.getUserdata(), session.getSaveRequestId()));
        result.setSaveRequestedAt(session.getSaveRequestedAt());

        if ((request.getStatus() == ONLYOFFICE_STATUS_READY_FOR_SAVE || request.getStatus() == ONLYOFFICE_STATUS_FORCE_SAVE)
                && Boolean.TRUE.equals(request.getNotmodified())) {
            result.setStatus(SYNC_STATUS_SAVED);
            result.setSaveCompletedAt(System.currentTimeMillis());
            result.setMessage("Excel 未检测到变更，无需同步到用例库");
        } else if ((request.getStatus() == ONLYOFFICE_STATUS_READY_FOR_SAVE || request.getStatus() == ONLYOFFICE_STATUS_FORCE_SAVE)
                && StringUtils.isNotBlank(request.getUrl())) {
            try {
                byte[] editedFile = downloadEditedDocument(request.getUrl());
                Files.write(session.getFilePath(), editedFile);
                result = syncEditedWorkbook(session, editedFile, request.getStatus());
                result.setSaveRequestId(firstNotBlank(request.getUserdata(), session.getSaveRequestId()));
                result.setSaveRequestedAt(session.getSaveRequestedAt());
                result.setSaveCompletedAt(System.currentTimeMillis());
            } catch (Exception e) {
                result.setStatus(SYNC_STATUS_ERROR);
                result.setMessage(e.getMessage());
                result.getErrors().add(e.getMessage());
                result.setSaveCompletedAt(System.currentTimeMillis());
                session.setLastResult(result);
                return Map.of("error", 1, "message", e.getMessage());
            }
        } else {
            result.setStatus("callback");
            result.setMessage("ONLYOFFICE callback status: " + request.getStatus());
        }

        session.setLastResult(result);
        return Map.of("error", 0);
    }

    private boolean isOnlyOfficeStatusWithoutDocumentChanges(Integer status) {
        return Objects.equals(status, ONLYOFFICE_STATUS_EDITING) || Objects.equals(status, ONLYOFFICE_STATUS_CLOSED_NO_CHANGES);
    }

    public OnlyOfficeCaseSyncResultDTO getSyncResult(String sessionId) {
        CaseEditSession session = sessions.get(sessionId);
        if (session == null) {
            MSException.throwException("ONLYOFFICE 编辑会话不存在或已过期");
        }
        return session.getLastResult();
    }

    private List<CaseExcelRow> loadPlanCaseRows(String planId) {
        TestPlanTestCaseExample planCaseExample = new TestPlanTestCaseExample();
        planCaseExample.createCriteria().andPlanIdEqualTo(planId).andIsDelEqualTo(false);
        planCaseExample.setOrderByClause("`order` ASC");
        List<TestPlanTestCaseWithBLOBs> planCases = testPlanTestCaseMapper.selectByExampleWithBLOBs(planCaseExample);
        if (CollectionUtils.isEmpty(planCases)) {
            return new ArrayList<>();
        }

        List<String> caseIds = planCases.stream()
                .map(TestPlanTestCaseWithBLOBs::getCaseId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        Map<String, TestCaseWithBLOBs> caseMap = loadCaseMap(caseIds);

        List<CaseExcelRow> rows = new ArrayList<>();
        for (TestPlanTestCaseWithBLOBs planCase : planCases) {
            TestCaseWithBLOBs testCase = caseMap.get(planCase.getCaseId());
            if (testCase == null || StringUtils.equals(testCase.getStatus(), DataStatus.TRASH.getValue())) {
                continue;
            }
            CaseExcelRow row = new CaseExcelRow();
            row.setPlanCase(planCase);
            row.setTestCase(testCase);
            rows.add(row);
        }
        return rows;
    }

    private Map<String, TestCaseWithBLOBs> loadCaseMap(List<String> caseIds) {
        if (CollectionUtils.isEmpty(caseIds)) {
            return new HashMap<>();
        }
        TestCaseExample testCaseExample = new TestCaseExample();
        testCaseExample.createCriteria().andIdIn(caseIds);
        return testCaseMapper.selectByExampleWithBLOBs(testCaseExample)
                .stream()
                .collect(Collectors.toMap(TestCaseWithBLOBs::getId, item -> item, (left, right) -> left));
    }

    private void writeWorkbook(Path filePath, List<CaseExcelRow> rows) {
        try {
            Files.createDirectories(filePath.getParent());
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("功能用例");
                Row header = sheet.createRow(0);
                for (int i = 0; i < HEADERS.length; i++) {
                    header.createCell(i).setCellValue(HEADERS[i]);
                    if (i < HIDDEN_COLUMN_COUNT) {
                        sheet.setColumnHidden(i, true);
                    }
                }
                for (int i = HIDDEN_COLUMN_COUNT; i < HEADERS.length; i++) {
                    sheet.setColumnWidth(i, 18 * 256);
                }
                sheet.setColumnWidth(7, 32 * 256);
                sheet.setColumnWidth(10, 36 * 256);
                sheet.setColumnWidth(11, 42 * 256);
                sheet.setColumnWidth(12, 42 * 256);
                sheet.createFreezePane(HIDDEN_COLUMN_COUNT, 1);
                addListValidation(sheet, 9, CASE_STATUSES);
                addListValidation(sheet, 14, PLAN_CASE_STATUSES);

                int rowIndex = 1;
                for (CaseExcelRow item : rows) {
                    TestCaseWithBLOBs testCase = item.getTestCase();
                    TestPlanTestCaseWithBLOBs planCase = item.getPlanCase();
                    Row row = sheet.createRow(rowIndex++);
                    writeCell(row, 0, planCase.getId());
                    writeCell(row, 1, testCase.getId());
                    writeCell(row, 2, testCase.getProjectId());
                    writeCell(row, 3, String.valueOf(testCase.getUpdateTime()));
                    writeCell(row, 4, testCase.getNum() == null ? "" : String.valueOf(testCase.getNum()));
                    writeCell(row, 5, testCase.getCustomNum());
                    writeCell(row, 6, testCase.getNodePath());
                    writeCell(row, 7, testCase.getName());
                    writeCell(row, 8, testCase.getPriority());
                    writeCell(row, 9, testCase.getStatus());
                    writeCell(row, 10, testCase.getPrerequisite());
                    writeCell(row, 11, firstNotBlank(testCase.getStepDescription(), testCase.getSteps()));
                    writeCell(row, 12, testCase.getExpectedResult());
                    writeCell(row, 13, testCase.getRemark());
                    writeCell(row, 14, planCase.getStatus());
                    writeCell(row, 15, planCase.getExecutor());
                    writeCell(row, 16, planCase.getActualResult());
                    writeCell(row, 17, planCase.getRemark());
                }
                workbook.write(outputStream);
                Files.write(filePath, outputStream.toByteArray());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OnlyOfficeCaseSyncResultDTO syncEditedWorkbook(CaseEditSession session, byte[] fileBytes, Integer documentStatus) throws IOException {
        OnlyOfficeCaseSyncResultDTO result = new OnlyOfficeCaseSyncResultDTO();
        result.setSessionId(session.getSessionId());
        result.setDocumentStatus(documentStatus);
        result.setStatus(SYNC_STATUS_SAVED);
        result.setUpdatedAt(System.currentTimeMillis());

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                result.setStatus(SYNC_STATUS_ERROR);
                result.setMessage("Excel 中未找到工作表");
                result.getErrors().add(result.getMessage());
                return result;
            }

            int lastRowNum = sheet.getLastRowNum();
            Log.info("ONLYOFFICE case workbook sync: sessionId={}, bytes={}, lastRowNum={}, physicalRows={}",
                    session.getSessionId(), fileBytes.length, lastRowNum, sheet.getPhysicalNumberOfRows());
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                result.setTotalRows(result.getTotalRows() + 1);
                syncWorkbookRow(session, row, result, i + 1);
            }
        }

        result.setMessage("同步完成：更新用例 " + result.getUpdatedCases() + " 条，更新计划用例 "
                + result.getUpdatedPlanCases() + " 条，跳过 " + result.getSkippedRows() + " 行");
        if (CollectionUtils.isNotEmpty(result.getErrors())) {
            result.setStatus(SYNC_STATUS_PARTIAL);
        }
        return result;
    }

    private void syncWorkbookRow(CaseEditSession session, Row row, OnlyOfficeCaseSyncResultDTO result, int excelRowNum) {
        String planCaseId = getStringCell(row, 0);
        String caseId = getStringCell(row, 1);
        if (StringUtils.isBlank(caseId)) {
            result.setSkippedRows(result.getSkippedRows() + 1);
            result.getErrors().add("第 " + excelRowNum + " 行缺少隐藏用例 ID，已跳过");
            return;
        }

        TestCaseWithBLOBs dbCase = testCaseMapper.selectByPrimaryKey(caseId);
        if (dbCase == null || !StringUtils.equals(dbCase.getProjectId(), session.getProjectId())) {
            result.setSkippedRows(result.getSkippedRows() + 1);
            result.getErrors().add("第 " + excelRowNum + " 行用例不存在或不属于当前项目，已跳过");
            return;
        }

        String caseStatus = normalizeStatus(getStringCell(row, 9), CASE_STATUSES);
        String planStatus = normalizeStatus(getStringCell(row, 14), PLAN_CASE_STATUSES);
        if (StringUtils.isNotBlank(getStringCell(row, 9)) && StringUtils.isBlank(caseStatus)) {
            result.setSkippedRows(result.getSkippedRows() + 1);
            result.getErrors().add("第 " + excelRowNum + " 行用例状态不合法，已跳过");
            return;
        }
        if (StringUtils.isNotBlank(getStringCell(row, 14)) && StringUtils.isBlank(planStatus)) {
            result.setSkippedRows(result.getSkippedRows() + 1);
            result.getErrors().add("第 " + excelRowNum + " 行计划执行状态不合法，已跳过");
            return;
        }

        boolean caseChanged = updateCaseFromRow(dbCase, row, caseStatus);
        if (caseChanged) {
            result.setUpdatedCases(result.getUpdatedCases() + 1);
        }

        if (StringUtils.isNotBlank(planCaseId)) {
            boolean planCaseChanged = updatePlanCaseFromRow(session, planCaseId, caseId, row, planStatus);
            if (planCaseChanged) {
                result.setUpdatedPlanCases(result.getUpdatedPlanCases() + 1);
            }
        }
    }

    private boolean updateCaseFromRow(TestCaseWithBLOBs dbCase, Row row, String caseStatus) {
        TestCaseWithBLOBs update = new TestCaseWithBLOBs();
        update.setId(dbCase.getId());
        boolean changed = false;

        changed |= setIfChanged(update::setCustomNum, dbCase.getCustomNum(), getStringCell(row, 5));
        String nodePath = normalizeNodePath(getStringCell(row, 6));
        if (StringUtils.isNotBlank(nodePath) && !StringUtils.equals(dbCase.getNodePath(), nodePath)) {
            update.setNodePath(nodePath);
            Map<String, String> nodePathMap = testCaseNodeService.createNodes(List.of(nodePath), dbCase.getProjectId());
            update.setNodeId(nodePathMap.get(nodePath));
            changed = true;
        }
        changed |= setIfChanged(update::setName, dbCase.getName(), getStringCell(row, 7));
        changed |= setIfChanged(update::setPriority, dbCase.getPriority(), getStringCell(row, 8));
        if (StringUtils.isNotBlank(caseStatus)) {
            changed |= setIfChanged(update::setStatus, dbCase.getStatus(), caseStatus);
        }
        changed |= setIfChanged(update::setPrerequisite, dbCase.getPrerequisite(), getStringCell(row, 10));
        changed |= updateStepDescriptionIfEdited(update, dbCase, getStringCell(row, 11));
        changed |= setIfChanged(update::setExpectedResult, dbCase.getExpectedResult(), getStringCell(row, 12));
        changed |= setIfChanged(update::setRemark, dbCase.getRemark(), getStringCell(row, 13));

        if (changed) {
            update.setUpdateTime(System.currentTimeMillis());
            testCaseMapper.updateByPrimaryKeySelective(update);
        }
        return changed;
    }

    private boolean updatePlanCaseFromRow(CaseEditSession session, String planCaseId, String caseId, Row row, String planStatus) {
        TestPlanTestCaseWithBLOBs dbPlanCase = testPlanTestCaseMapper.selectByPrimaryKey(planCaseId);
        if (dbPlanCase == null
                || !StringUtils.equals(dbPlanCase.getPlanId(), session.getPlanId())
                || !StringUtils.equals(dbPlanCase.getCaseId(), caseId)) {
            return false;
        }

        TestPlanTestCaseWithBLOBs update = new TestPlanTestCaseWithBLOBs();
        update.setId(planCaseId);
        boolean changed = false;
        if (StringUtils.isNotBlank(planStatus)) {
            changed |= setIfChanged(update::setStatus, dbPlanCase.getStatus(), planStatus);
        }
        changed |= setIfChanged(update::setExecutor, dbPlanCase.getExecutor(), getStringCell(row, 15));
        changed |= setIfChanged(update::setActualResult, dbPlanCase.getActualResult(), getStringCell(row, 16));
        changed |= setIfChanged(update::setRemark, dbPlanCase.getRemark(), getStringCell(row, 17));

        if (changed) {
            update.setUpdateTime(System.currentTimeMillis());
            testPlanTestCaseMapper.updateByPrimaryKeySelective(update);
            if (StringUtils.isNotBlank(update.getStatus())) {
                TestCaseWithBLOBs caseUpdate = new TestCaseWithBLOBs();
                caseUpdate.setId(caseId);
                caseUpdate.setLastExecuteResult(update.getStatus());
                testCaseMapper.updateByPrimaryKeySelective(caseUpdate);
            }
        }
        return changed;
    }

    private Map<String, Object> buildEditorConfig(TestPlanWithBLOBs plan, CaseEditSession session) {
        String fileUrl = buildStorageUrl("/track/onlyoffice/case/file/" + session.getSessionId(), session.getToken());
        String callbackUrl = buildStorageUrl("/track/onlyoffice/case/callback/" + session.getSessionId(), session.getToken());

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileType", "xlsx");
        document.put("key", session.getDocumentKey());
        document.put("title", "MeterSphere-" + safeFileName(plan.getName()) + "-功能用例.xlsx");
        document.put("url", fileUrl);
        document.put("permissions", Map.of("download", true, "edit", true, "print", true));

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("callbackUrl", callbackUrl);
        editorConfig.put("lang", lang);
        editorConfig.put("mode", "edit");
        editorConfig.put("user", Map.of(
                "id", firstNotBlank(session.getCreatedBy(), "metersphere-user"),
                "name", firstNotBlank(session.getCreatedBy(), "MeterSphere 用户")
        ));
        editorConfig.put("customization", Map.of("autosave", false, "forcesave", true));

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("documentType", "cell");
        config.put("document", document);
        config.put("editorConfig", editorConfig);
        if (Boolean.TRUE.equals(jwtEnabled) && StringUtils.isNotBlank(jwtSecret)) {
            config.put("token", signJwt(config));
        }
        return config;
    }

    private String buildStorageUrl(String path, String token) {
        return trimTrailingSlash(storageUrl) + path + "?token=" + token;
    }

    private byte[] downloadEditedDocument(String url) throws IOException, InterruptedException {
        URI uri = rewriteOnlyOfficeDownloadUrl(URI.create(url));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("下载 ONLYOFFICE 编辑文件失败，HTTP " + response.statusCode());
        }
        return response.body();
    }

    private Map<String, Object> sendForceSaveCommand(CaseEditSession session, String saveRequestId) throws IOException, InterruptedException {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("c", "forcesave");
        command.put("key", session.getDocumentKey());
        command.put("userdata", saveRequestId);

        Map<String, Object> payload = new LinkedHashMap<>(command);
        if (Boolean.TRUE.equals(jwtEnabled) && StringUtils.isNotBlank(jwtSecret)) {
            payload.put("token", signJwt(command));
        }

        HttpRequest request = HttpRequest.newBuilder(buildDocumentServerUri(commandServicePath))
                .timeout(Duration.ofSeconds(15))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(payload), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("ONLYOFFICE Command Service HTTP " + response.statusCode());
        }
        Map<String, Object> responseBody = JSON.parseObject(response.body(), Map.class);
        return responseBody == null ? Map.of("error", 1) : responseBody;
    }

    private URI rewriteOnlyOfficeDownloadUrl(URI url) {
        if (!StringUtils.equalsAnyIgnoreCase(url.getHost(), "localhost", "127.0.0.1")) {
            return url;
        }
        String base = trimTrailingSlash(documentServerInternalUrl);
        String pathAndQuery = url.getRawPath() + (StringUtils.isBlank(url.getRawQuery()) ? "" : "?" + url.getRawQuery());
        return URI.create(base + pathAndQuery);
    }

    private URI buildDocumentServerUri(String path) {
        if (StringUtils.startsWithIgnoreCase(path, "http://") || StringUtils.startsWithIgnoreCase(path, "https://")) {
            return URI.create(path);
        }
        return URI.create(trimTrailingSlash(documentServerInternalUrl) + "/" + StringUtils.removeStart(path, "/"));
    }

    private CaseEditSession getSession(String sessionId, String token) {
        CaseEditSession session = sessions.get(sessionId);
        if (session == null || !StringUtils.equals(session.getToken(), token)) {
            MSException.throwException("ONLYOFFICE 编辑会话不存在或 token 无效");
        }
        return session;
    }

    private OnlyOfficeCaseSyncResultDTO buildInitialResult(String sessionId, int rowCount) {
        OnlyOfficeCaseSyncResultDTO result = new OnlyOfficeCaseSyncResultDTO();
        result.setSessionId(sessionId);
        result.setStatus(SYNC_STATUS_READY);
        result.setTotalRows(rowCount);
        result.setUpdatedAt(System.currentTimeMillis());
        result.setMessage("已生成 " + rowCount + " 条功能用例编辑数据");
        return result;
    }

    private OnlyOfficeCaseSyncResultDTO buildSavingResult(CaseEditSession session, String saveRequestId) {
        OnlyOfficeCaseSyncResultDTO result = new OnlyOfficeCaseSyncResultDTO();
        long now = System.currentTimeMillis();
        result.setSessionId(session.getSessionId());
        result.setStatus(SYNC_STATUS_SAVING);
        result.setUpdatedAt(now);
        result.setSaveRequestedAt(now);
        result.setSaveRequestId(saveRequestId);
        result.setMessage("已提交保存请求，等待 ONLYOFFICE 回调并同步到用例库");
        return result;
    }

    private OnlyOfficeCaseSyncResultDTO buildNoChangeSavedResult(CaseEditSession session, Integer documentStatus, String userdata) {
        OnlyOfficeCaseSyncResultDTO result = new OnlyOfficeCaseSyncResultDTO();
        long now = System.currentTimeMillis();
        result.setSessionId(session.getSessionId());
        result.setDocumentStatus(documentStatus);
        result.setStatus(SYNC_STATUS_SAVED);
        result.setUpdatedAt(now);
        result.setSaveRequestedAt(session.getSaveRequestedAt());
        result.setSaveCompletedAt(now);
        result.setSaveRequestId(firstNotBlank(userdata, session.getSaveRequestId()));
        result.setMessage("Excel 未检测到变更，无需同步到用例库");
        return result;
    }

    private void addListValidation(Sheet sheet, int column, Set<String> values) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values.stream().sorted().toArray(String[]::new));
        CellRangeAddressList range = new CellRangeAddressList(1, 5000, column, column);
        DataValidation validation = helper.createValidation(constraint, range);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void writeCell(Row row, int column, String value) {
        row.createCell(column).setCellValue(StringUtils.defaultString(value));
    }

    private String getStringCell(Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            return "";
        }
        return StringUtils.trimToEmpty(dataFormatter.formatCellValue(cell));
    }

    private boolean isBlankRow(Row row) {
        for (int i = HIDDEN_COLUMN_COUNT; i < HEADERS.length; i++) {
            if (StringUtils.isNotBlank(getStringCell(row, i))) {
                return false;
            }
        }
        return true;
    }

    private String normalizeStatus(String value, Set<String> values) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return values.stream()
                .filter(item -> StringUtils.equalsIgnoreCase(item, value))
                .findFirst()
                .orElse("");
    }

    private String normalizeNodePath(String nodePath) {
        if (StringUtils.isBlank(nodePath)) {
            return "";
        }
        String normalized = "/" + StringUtils.strip(nodePath, "/ ");
        return normalized.replaceAll("/+", "/");
    }

    private boolean setIfChanged(java.util.function.Consumer<String> setter, String oldValue, String newValue) {
        String normalized = StringUtils.defaultString(newValue);
        if (!StringUtils.equals(StringUtils.defaultString(oldValue), normalized)) {
            setter.accept(normalized);
            return true;
        }
        return false;
    }

    private boolean updateStepDescriptionIfEdited(TestCaseWithBLOBs update, TestCaseWithBLOBs dbCase, String newValue) {
        String exportedValue = firstNotBlank(dbCase.getStepDescription(), dbCase.getSteps());
        if (StringUtils.equals(StringUtils.defaultString(exportedValue), StringUtils.defaultString(newValue))) {
            return false;
        }
        return setIfChanged(update::setStepDescription, dbCase.getStepDescription(), newValue);
    }

    private String signJwt(Object payload) {
        String header = base64Url(JSON.toJSONString(Map.of("alg", "HS256", "typ", "JWT")));
        String body = base64Url(JSON.toJSONString(payload));
        String signature = sign(header + "." + body);
        return header + "." + body + "." + signature;
    }

    private boolean verifyJwt(String authorization) {
        String token = StringUtils.removeStartIgnoreCase(StringUtils.defaultString(authorization), "Bearer ");
        String[] parts = StringUtils.split(token, '.');
        if (parts == null || parts.length != 3) {
            return false;
        }
        String expected = sign(parts[0] + "." + parts[1]);
        return Objects.equals(expected, parts[2]);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Path getSessionFilePath(String sessionId) {
        return Path.of(System.getProperty("java.io.tmpdir"), "metersphere-onlyoffice-cases", sessionId + ".xlsx");
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String safeFileName(String fileName) {
        return StringUtils.defaultIfBlank(fileName, "测试计划").replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private Integer getInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null && StringUtils.isNumeric(String.valueOf(value))) {
            return Integer.parseInt(String.valueOf(value));
        }
        return null;
    }

    private String getCommandErrorMessage(Integer error) {
        if (Objects.equals(error, 1)) {
            return "ONLYOFFICE 未找到当前文档，请确认 Excel 编辑器已加载完成且未断开连接";
        }
        if (Objects.equals(error, 2)) {
            return "ONLYOFFICE 回调地址不正确，请检查 onlyoffice.storage-url 是否能被 Document Server 访问";
        }
        if (Objects.equals(error, 3)) {
            return "ONLYOFFICE Document Server 内部服务异常";
        }
        if (Objects.equals(error, 4)) {
            return "ONLYOFFICE 当前文档没有待保存变更";
        }
        if (Objects.equals(error, 5)) {
            return "ONLYOFFICE 保存命令不正确";
        }
        if (Objects.equals(error, 6)) {
            return "ONLYOFFICE JWT token 无效，请检查 onlyoffice.jwt-secret 与容器配置是否一致";
        }
        return "ONLYOFFICE 保存命令失败，错误码：" + (error == null ? "unknown" : error);
    }

    private String trimTrailingSlash(String url) {
        return StringUtils.removeEnd(StringUtils.defaultString(url), "/");
    }

    private boolean isSessionSaving(CaseEditSession session) {
        return session.getLastResult() != null && StringUtils.equals(session.getLastResult().getStatus(), SYNC_STATUS_SAVING);
    }

    @lombok.Getter
    @lombok.Setter
    private static class CaseEditSession {
        private String sessionId;
        private String token;
        private String documentKey;
        private String planId;
        private String projectId;
        private String createdBy;
        private Long createdAt;
        private Path filePath;
        private OnlyOfficeCaseSyncResultDTO lastResult;
        private String saveRequestId;
        private Long saveRequestedAt;
    }

    @lombok.Getter
    @lombok.Setter
    private static class CaseExcelRow {
        private TestPlanTestCaseWithBLOBs planCase;
        private TestCaseWithBLOBs testCase;
    }
}
