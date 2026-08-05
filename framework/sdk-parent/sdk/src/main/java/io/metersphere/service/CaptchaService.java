package io.metersphere.service;

import io.metersphere.commons.utils.CaptchaUtil;
import io.metersphere.dto.CaptchaVO;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
