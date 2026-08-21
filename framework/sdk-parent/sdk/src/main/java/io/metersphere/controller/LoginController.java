package io.metersphere.controller;

import io.metersphere.commons.constants.OperLogConstants;
import io.metersphere.commons.constants.OperLogModule;
import io.metersphere.commons.constants.SessionConstants;
import io.metersphere.commons.constants.UserSource;
import io.metersphere.commons.user.SessionUser;
import io.metersphere.commons.utils.RsaKey;
import io.metersphere.commons.utils.RsaUtil;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.controller.handler.ResultHolder;
import io.metersphere.dto.ServiceDTO;
import io.metersphere.dto.UserDTO;
import io.metersphere.i18n.Translator;
import io.metersphere.log.annotation.MsAuditLog;
import io.metersphere.request.LoginRequest;
import io.metersphere.service.BaseDisplayService;
import io.metersphere.service.BaseUserService;
import io.metersphere.service.CaptchaService;
import io.metersphere.service.SSOLogoutService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping
public class LoginController {

    @Resource
    private BaseUserService baseUserService;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private io.metersphere.service.LoginFailService loginFailService;
    @Resource
    private BaseDisplayService baseDisplayService;
    @Resource
    private SSOLogoutService ssoLogoutService;
    @Value("${spring.application.name}")
    private String serviceId;
    @Value("${server.port}")
    private Integer port;
    @Resource
    private RedisIndexedSessionRepository redisIndexedSessionRepository;
    @Value("${spring.messages.default-locale}")
    private String defaultLocale;


    @GetMapping(value = "/is-login")
    public ResultHolder isLogin(@RequestHeader(name = SessionConstants.HEADER_TOKEN, required = false) String sessionId) throws Exception {
        RsaKey rsaKey = RsaUtil.getRsaKey();
        Object user = redisIndexedSessionRepository.getSessionRedisOperations().opsForHash().get("spring:session:sessions:" + sessionId, "sessionAttr:user");
        if (user != null) {
            UserDTO userDTO = baseUserService.getUserDTO((String) MethodUtils.invokeMethod(user, "getId"));
            if (StringUtils.isBlank(userDTO.getLanguage())) {
                userDTO.setLanguage(defaultLocale);
            }
            baseUserService.autoSwitch(userDTO);
            SessionUser sessionUser = SessionUser.fromUser(userDTO, SessionUtils.getSessionId());
            SessionUtils.putUser(sessionUser);
            // 用户只有工作空间权限
            if (StringUtils.isBlank(sessionUser.getLastProjectId())) {
                sessionUser.setLastProjectId("no_such_project");
            }
            return ResultHolder.success(sessionUser);
        }
        return ResultHolder.error(rsaKey.getPublicKey());
    }

    @PostMapping(value = "/signin")
    @MsAuditLog(module = OperLogModule.AUTH_TITLE, type = OperLogConstants.LOGIN, title = "登录")
    public ResultHolder login(@RequestBody LoginRequest request) {
        // 登录失败锁定检查
        if (loginFailService.isLocked(request.getUsername())) {
            return ResultHolder.error(Translator.get("login_fail_lock"));
        }
        // 登录验证码校验
        if (!captchaService.verify(request.getCaptchaId(), request.getCaptcha())) {
            return ResultHolder.error(Translator.get("captcha_error"));
        }
        SessionUser sessionUser = SessionUtils.getUser();
        if (sessionUser != null) {
            if (!StringUtils.equals(sessionUser.getId(), request.getUsername())) {
                return ResultHolder.error(Translator.get("please_logout_current_user"));
            }
        }
        SecurityUtils.getSubject().getSession().setAttribute("authenticate", UserSource.LOCAL.name());
        try {
            // 在登录（含自动升级）之前检查是否需要强制修改密码
            boolean changePassword = baseUserService.checkWhetherChangePasswordOrNot(request);
            ResultHolder result = baseUserService.login(request);
            if (result.isSuccess()) {
                loginFailService.clearFail(request.getUsername());
                result.setMessage(BooleanUtils.toStringTrueFalse(changePassword));
            } else {
                int failCount = loginFailService.incrementFail(request.getUsername());
                int remaining = 5 - failCount;
                if (remaining > 0) {
                    result.setMessage(String.format(Translator.get("login_fail_attempt_count"), remaining));
                }
            }
            return result;
        } catch (Exception e) {
            int failCount = loginFailService.incrementFail(request.getUsername());
            int remaining = 5 - failCount;
            if (remaining > 0) {
                String msg = e.getMessage() + String.format(Translator.get("login_fail_attempt_count"), remaining);
                return ResultHolder.error(msg);
            }
            return ResultHolder.error(Translator.get("login_fail_lock"));
        }
    }

    @GetMapping(value = "/currentUser")
    public ResultHolder currentUser() {
        return ResultHolder.success(SecurityUtils.getSubject().getSession().getAttribute("user"));
    }

    @GetMapping(value = "/signout")
    @MsAuditLog(module = OperLogModule.AUTH_TITLE, beforeEvent = "#msClass.getUserId(id)", type = OperLogConstants.LOGIN, title = "登出", msClass = SessionUtils.class)
    public void logout(HttpServletResponse response) throws Exception {
        ssoLogoutService.logout(SessionUtils.getSessionId(), response);
        SecurityUtils.getSubject().logout();
    }

    /*Get default language*/
    @GetMapping(value = "/language")
    public String getDefaultLanguage() {
        if (StringUtils.isNotBlank(defaultLocale)) {
            return defaultLocale;
        }
        return baseUserService.getDefaultLanguage();
    }

    @GetMapping("display/file/{imageName}")
    public ResponseEntity<byte[]> image(@PathVariable("imageName") String imageName) throws IOException {
        return baseDisplayService.getImage(imageName);
    }

    @GetMapping("display/file/css")
    public ResponseEntity<byte[]> cssFile() throws IOException {
        return baseDisplayService.getCss();
    }

    @GetMapping(value = "/services")
    public List<ServiceDTO> services() {
        return List.of(new ServiceDTO(serviceId, port));
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
