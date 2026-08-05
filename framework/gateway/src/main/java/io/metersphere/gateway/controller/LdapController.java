package io.metersphere.gateway.controller;

import io.metersphere.commons.constants.OperLogConstants;
import io.metersphere.commons.constants.OperLogModule;
import io.metersphere.controller.handler.ResultHolder;
import io.metersphere.gateway.service.CaptchaService;
import io.metersphere.gateway.service.LdapService;
import io.metersphere.gateway.log.annotation.MsAuditLog;
import io.metersphere.i18n.Translator;
import io.metersphere.request.LoginRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.Resource;
import java.util.Locale;

@RestController
@RequestMapping("/ldap")
public class LdapController {

    @Resource
    private LdapService ldapService;
    @Resource
    private CaptchaService captchaService;

    @PostMapping(value = "/signin")
    @MsAuditLog(module = OperLogModule.SYSTEM_PARAMETER_SETTING, type = OperLogConstants.LOGIN, title = "LDAP")
    public Mono<ResultHolder> login(@RequestBody LoginRequest request, WebSession session, Locale locale) {
        // 登录验证码校验
        if (!captchaService.verify(request.getCaptchaId(), request.getCaptcha())) {
            return Mono.just(ResultHolder.error(Translator.get("captcha_error")));
        }
        return Mono.defer(() -> ldapService.login(request, session, locale).map(Mono::just).orElseGet(Mono::empty))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResultHolder::success);
    }

    @GetMapping("/open")
    public Mono<ResultHolder> isOpen() {
        return Mono.just(ldapService.isOpen())
                .map(ResultHolder::success);
    }

}
