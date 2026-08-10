package io.metersphere.gateway.controller;

import io.metersphere.base.domain.User;
import io.metersphere.commons.constants.OperLogConstants;
import io.metersphere.commons.constants.OperLogModule;
import io.metersphere.commons.constants.SessionConstants;
import io.metersphere.commons.user.SessionUser;
import io.metersphere.commons.utils.RsaKey;
import io.metersphere.commons.utils.RsaUtil;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.controller.handler.ResultHolder;
import io.metersphere.dto.ServiceDTO;
import io.metersphere.dto.UserDTO;
import io.metersphere.gateway.log.annotation.MsAuditLog;
import io.metersphere.gateway.service.AuthSourceService;
import io.metersphere.gateway.service.BaseDisplayService;
import io.metersphere.gateway.service.CaptchaService;
import io.metersphere.gateway.service.LoginFailService;
import io.metersphere.gateway.service.SystemParameterService;
import io.metersphere.gateway.service.UserLoginService;
import io.metersphere.i18n.Translator;
import io.metersphere.request.LoginRequest;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class LoginController {
    @Value("${spring.messages.default-locale}")
    private String defaultLocale;

    @Resource
    private UserLoginService userLoginService;
    @Resource
    private BaseDisplayService baseDisplayService;
    @Resource
    private DiscoveryClient discoveryClient;
    @Resource
    private AuthSourceService authSourceService;
    @Resource
    private SystemParameterService systemParameterService;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private LoginFailService loginFailService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping(value = "/is-login")
    public Mono<ResultHolder> isLogin(@RequestHeader(name = SessionConstants.HEADER_TOKEN, required = false) String sessionId,
                                      @RequestHeader(name = SessionConstants.CSRF_TOKEN, required = false) String csrfToken) throws Exception {
        RsaKey rsaKey = RsaUtil.getRsaKey();

        if (StringUtils.isNotBlank(sessionId) && StringUtils.isNotBlank(csrfToken)) {
            String userId = userLoginService.validateCsrfToken(sessionId, csrfToken);
            Boolean exist = stringRedisTemplate.opsForHash().hasKey("spring:session:sessions:" + sessionId, "sessionAttr:user");
            if (BooleanUtils.isFalse(exist)) {
                return Mono.just(ResultHolder.error(rsaKey.getPublicKey()));
            }
            // 使用数据库里的最新用户权限，不同的tab sessionId 不变
            UserDTO userDTO = userLoginService.getUserDTO(userId);
            SessionUser sessionUser = SessionUser.fromUser(userDTO, sessionId);
            // 用户只有工作空间权限
            if (StringUtils.isBlank(sessionUser.getLastProjectId())) {
                sessionUser.setLastProjectId("no_such_project");
            }
            return Mono.just(ResultHolder.success(sessionUser));
        } else {
            return Mono.just(ResultHolder.error(rsaKey.getPublicKey()));
        }
    }

    @PostMapping(value = "/signin")
    @MsAuditLog(module = OperLogModule.AUTH_TITLE, type = OperLogConstants.LOGIN, title = "登录")
    public Mono<ResultHolder> login(@RequestBody LoginRequest request, WebSession session, Locale locale) {
        // 登录失败锁定检查
        if (loginFailService.isLocked(request.getUsername())) {
            return Mono.just(ResultHolder.error(Translator.get("login_fail_lock")));
        }
        // 登录验证码校验
        if (!captchaService.verify(request.getCaptchaId(), request.getCaptcha())) {
            return Mono.just(ResultHolder.error(Translator.get("captcha_error")));
        }
        return Mono.fromCallable(() -> userLoginService.loginLocal(request, session, locale))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(user -> loginFailService.clearFail(request.getUsername()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not found user info or invalid password")))
                .map(opt -> {
                    ResultHolder rh = ResultHolder.success(opt);
                    if (opt != null && opt.isPresent()) {
                        // 在 boundedElastic 线程上从 SessionUser 读取标志，避免在 Netty 线程上访问 session
                        boolean changePassword = Boolean.TRUE.equals(opt.get().getNeedChangePassword());
                        rh.setMessage(BooleanUtils.toStringTrueFalse(changePassword));
                    }
                    return rh;
                })
                .onErrorResume(e -> {
                    int failCount = loginFailService.incrementFail(request.getUsername());
                    int remaining = 5 - failCount;
                    if (remaining > 0) {
                        String msg = (e.getMessage() != null ? e.getMessage() : "")
                                + String.format(Translator.get("login_fail_attempt_count"), remaining);
                        return Mono.just(ResultHolder.error(msg));
                    }
                    return Mono.just(ResultHolder.error(Translator.get("login_fail_lock")));
                });
    }

    @GetMapping(value = "/currentUser")
    public Mono<ResultHolder> currentUser(WebSession session) {
        return Mono.justOrEmpty((User) session.getAttribute("user"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You don't have permission!")))
                .map(ResultHolder::success);
    }

    @GetMapping(value = "/signout")
    public Mono<Void> logout(WebSession session) {
        return Mono.just(session)
                .flatMap(WebSession::invalidate);
    }

    @GetMapping("display/file/{imageName}")
    public Mono<ResponseEntity<byte[]>> image(@PathVariable("imageName") String imageName) throws IOException {
        ResponseEntity<byte[]> image = baseDisplayService.getImage(imageName);
        return Mono.just(image);
    }

    @GetMapping("display/file/css")
    public Mono<ResponseEntity<byte[]>> cssFile() throws IOException {
        ResponseEntity<byte[]> css = baseDisplayService.getCss();
        return Mono.just(css);
    }


    @GetMapping("display/info")
    public Mono<ResultHolder> uiInfo() {
        return Mono.just(ResultHolder.success(baseDisplayService.uiInfo("ui")));
    }

    @GetMapping("authsource/list/allenable")
    public Mono<ResultHolder> listAllEnable() {
        return Mono.just(ResultHolder.success(authSourceService.listAllEnable()));
    }

    @GetMapping("authsource/{authId}")
    public Mono<ResultHolder> getAuthSource(@PathVariable("authId") String authId) {
        return Mono.just(ResultHolder.success(authSourceService.getAuthSource(authId)));
    }

    @GetMapping(value = "/services")
    public Mono<ResultHolder> getServices() {
        List<ServiceDTO> result = discoveryClient.getServices().stream()
                .map(service -> new ServiceDTO(service, discoveryClient.getInstances(service).get(0).getPort()))
                .collect(Collectors.toList());
        return Mono.just(ResultHolder.success(result));
    }

    @GetMapping(value = "/language")
    public Mono<ResultHolder> getDefaultLanguage() {
        return Mono.just(ResultHolder.success(systemParameterService.getDefaultLanguage()));
    }

    @GetMapping("/module/list")
    public Mono<ResultHolder> listModules() {
        return Mono.just(ResultHolder.success(systemParameterService.listModules()));
    }

    @GetMapping(value = "/default-locale")
    public String defaultLocale() {
        SessionUser user = SessionUtils.getUser();

        return Optional.ofNullable(user)
                .map(SessionUser::getLanguage)
                .filter(StringUtils::isNotBlank)
                .orElse(defaultLocale);
    }


}
