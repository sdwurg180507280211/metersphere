package io.metersphere.workstation.controller;

import io.metersphere.controller.handler.ResultHolder;
import io.metersphere.request.SqlQueryRequest;
import io.metersphere.workstation.service.SqlQueryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("workstation/sql-query")
@RestController
public class SqlQueryController {

    @Resource
    private SqlQueryService sqlQueryService;

    @GetMapping("/status")
    public ResultHolder status() {
        return ResultHolder.success(sqlQueryService.status());
    }

    @PostMapping("/execute")
    public ResultHolder execute(@RequestBody SqlQueryRequest request) {
        try {
            return ResultHolder.success(sqlQueryService.query(request.getSql(), request.getLimit()));
        } catch (Exception e) {
            return ResultHolder.error(e.getMessage());
        }
    }
}
