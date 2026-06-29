package io.metersphere.workstation.controller;

import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.controller.handler.ResultHolder;
import io.metersphere.dto.UserDTO;
import io.metersphere.request.SqlQueryHistoryRequest;
import io.metersphere.request.SqlQueryRequest;
import io.metersphere.service.BaseUserService;
import io.metersphere.workstation.service.SqlQueryHistoryService;
import io.metersphere.workstation.service.SqlQueryService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("workstation/sql-query")
@RestController
public class SqlQueryController {

    private static final String SQL_QUERY_ALLOWED_ACCOUNT = "kjls_zhaozhiwei001";

    @Resource
    private SqlQueryService sqlQueryService;

    @Resource
    private SqlQueryHistoryService sqlQueryHistoryService;

    @Resource
    private BaseUserService baseUserService;

    @Value("${metersphere.sql-query.enabled:true}")
    private boolean sqlQueryEnabled;

    @GetMapping("/status")
    public ResultHolder status() {
        try {
            checkAccess();
            return ResultHolder.success(sqlQueryService.status());
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    @PostMapping("/execute")
    public ResultHolder execute(@RequestBody SqlQueryRequest request) {
        try {
            checkAccess();
            return ResultHolder.success(sqlQueryService.query(request.getSql(), request.getLimit(), request.getTimeoutSeconds()));
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResultHolder history() {
        try {
            String userId = checkAccess();
            return ResultHolder.success(sqlQueryHistoryService.listSaved(userId));
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    @PostMapping("/history/save")
    public ResultHolder saveHistory(@RequestBody SqlQueryHistoryRequest request) {
        try {
            String userId = checkAccess();
            return ResultHolder.success(sqlQueryHistoryService.save(userId, request));
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    @PostMapping("/history/delete")
    public ResultHolder deleteHistory(@RequestBody SqlQueryHistoryRequest request) {
        try {
            String userId = checkAccess();
            sqlQueryHistoryService.delete(userId, request.getId());
            return ResultHolder.success(null);
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }

    private String checkAccess() {
        if (!sqlQueryEnabled) {
            MSException.throwException("SQL 查询台未开启");
        }
        String userId = SessionUtils.getUserId();
        if (StringUtils.isBlank(userId) || !baseUserService.isSuperUser(userId)) {
            MSException.throwException("仅超级管理员可使用 SQL 查询台");
        }
        UserDTO user = baseUserService.getUserDTO(userId);
        if (user == null || !isAllowedSqlQueryUser(user)) {
            MSException.throwException("仅指定账号可使用 SQL 查询台");
        }
        return userId;
    }

    private boolean isAllowedSqlQueryUser(UserDTO user) {
        return StringUtils.equals(SQL_QUERY_ALLOWED_ACCOUNT, user.getId());
    }
}
