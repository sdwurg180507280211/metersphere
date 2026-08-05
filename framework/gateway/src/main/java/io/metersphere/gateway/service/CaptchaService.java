package io.metersphere.gateway.service;

import io.metersphere.commons.utils.CaptchaUtil;
import io.metersphere.dto.CaptchaVO;
import jakarta.annotation.Resource;
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

    public CaptchaVO generate() {
        return CaptchaUtil.createCaptcha(stringRedisTemplate);
    }

    public boolean verify(String captchaId, String code) {
        return CaptchaUtil.verifyCaptcha(stringRedisTemplate, captchaId, code);
    }
}
