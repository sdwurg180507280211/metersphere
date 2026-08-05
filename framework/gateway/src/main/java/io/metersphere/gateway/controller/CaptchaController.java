package io.metersphere.gateway.controller;

import io.metersphere.controller.handler.ResultHolder;
import io.metersphere.gateway.service.CaptchaService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
public class CaptchaController {

    @Resource
    private CaptchaService captchaService;

    @GetMapping("/captcha")
    public Mono<ResultHolder> getCaptcha() {
        return Mono.just(ResultHolder.success(captchaService.generate()));
    }
}
