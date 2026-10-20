package io.metersphere.commons.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 兼容历史密码格式：旧 MD5 与已存储的 BCrypt。
 * 不自动升级密码，不参与新密码生成与复杂度校验。
 */
public final class PasswordEncoder {
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private PasswordEncoder() {
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || StringUtils.isBlank(storedPassword)) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return BCRYPT.matches(rawPassword, storedPassword);
        }
        return StringUtils.equals(CodingUtil.md5(rawPassword), storedPassword);
    }
}
