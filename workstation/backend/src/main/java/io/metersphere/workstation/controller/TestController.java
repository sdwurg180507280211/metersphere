package io.metersphere.workstation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试Controller - 用于验证API是否可访问
 * 仅用于开发测试，生产环境应删除
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("message", "pong");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
