package io.metersphere.commons.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码编码器：兼容旧 MD5，新密码统一使用 BCrypt。
 * 老用户登录时自动从 MD5 升级到 BCrypt。
 * BCrypt hash 以 $2a$/$2b$/$2y$ 开头，无须额外字段区分算法。
 */
public class PasswordEncoder {

    private static final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    /**
     * 使用 BCrypt 加密密码
     */
    public static String encode(String rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    /**
     * 验证密码，按存储格式自动识别算法：
     * - 以 "$2" 开头 → BCrypt
     * - 其他 → MD5（兼容旧数据）
     */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            return bcrypt.matches(rawPassword, storedPassword);
        }
        // 旧数据走 MD5
        return StringUtils.equals(CodingUtil.md5(rawPassword), storedPassword);
    }
}
