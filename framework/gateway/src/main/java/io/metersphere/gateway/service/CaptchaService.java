package io.metersphere.gateway.service;

import io.metersphere.commons.utils.CaptchaUtil;
import io.metersphere.dto.CaptchaVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * gateway（WebFlux）不扫描 sdk 的 @Service，因此提供本模块的验证码服务，
 * 复用 sdk 中的静态工具类 CaptchaUtil
 */
@Service
public class CaptchaService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${login.captcha.enabled:true}")
    private boolean captchaEnabled;

    public CaptchaVO generate() {
        if (!captchaEnabled) {
            CaptchaVO captcha = new CaptchaVO();
            captcha.setEnabled(false);
            return captcha;
        }
        return CaptchaUtil.createCaptcha(stringRedisTemplate);
    }

    public boolean verify(String captchaId, String code) {
        return !captchaEnabled || CaptchaUtil.verifyCaptcha(stringRedisTemplate, captchaId, code);
    }
}
