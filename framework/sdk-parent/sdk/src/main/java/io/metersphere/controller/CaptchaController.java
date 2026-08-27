package io.metersphere.controller;

import io.metersphere.controller.handler.ResultHolder;
import io.metersphere.service.CaptchaService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class CaptchaController {

    @Resource
    private CaptchaService captchaService;

    @GetMapping("/captcha")
    public ResultHolder getCaptcha() {
        return ResultHolder.success(captchaService.generate());
    }
}
