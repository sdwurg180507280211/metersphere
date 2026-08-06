package io.metersphere.gateway.service;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * gateway（WebFlux）不扫描 sdk 的 @Service，因此提供本模块的登录失败限制服务
 */
@Service
public class LoginFailService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = "ms:login:fail:";
    private static final int MAX_FAIL_COUNT = 5;
    private static final long LOCK_SECONDS = 600;

    public boolean isLocked(String username) {
        String countStr = stringRedisTemplate.opsForValue().get(PREFIX + username);
        if (countStr == null) {
            return false;
        }
        try {
            return Integer.parseInt(countStr) >= MAX_FAIL_COUNT;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 登录失败，增加失败次数；达到上限后锁定 10 分钟
     * @return 当前失败次数
     */
    public int incrementFail(String username) {
        String key = PREFIX + username;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count >= 1) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(LOCK_SECONDS));
        }
        return count != null ? count.intValue() : 0;
    }

    /**
     * 获取当前失败次数
     */
    public int getFailCount(String username) {
        String countStr = stringRedisTemplate.opsForValue().get(PREFIX + username);
        if (countStr == null) {
            return 0;
        }
        try {
            return Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void clearFail(String username) {
        stringRedisTemplate.delete(PREFIX + username);
    }
}
