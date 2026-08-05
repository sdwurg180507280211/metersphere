package io.metersphere.commons.utils;

import io.metersphere.dto.CaptchaVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录验证码工具类：生成验证码图片并以 Redis 存储校验值
 * 说明：作为静态工具类提供，gateway（WebFlux，不扫描 sdk 的 @Service）与业务服务均可复用
 */
public class CaptchaUtil {

    private static final String CAPTCHA_KEY_PREFIX = "ms:captcha:";
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;
    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;
    // 去除易混淆的 0/O/1/I 等字符
    private static final String CODES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 4;

    private CaptchaUtil() {
    }

    private static String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODES.charAt(random.nextInt(CODES.length())));
        }
        return sb.toString();
    }

    private static String drawBase64(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random random = new Random();
        g.setColor(new Color(246, 243, 248));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(120 + random.nextInt(120), 120 + random.nextInt(120), 120 + random.nextInt(120)));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x1 + random.nextInt(40), y1 + random.nextInt(40));
        }
        // 验证码字符
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(30 + random.nextInt(150), 30 + random.nextInt(150), 30 + random.nextInt(150)));
            int x = 12 + i * 26;
            int y = 32 + random.nextInt(8);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成验证码并存入 Redis
     *
     * @param redisTemplate Redis 客户端
     * @return 验证码标识与图片 base64
     */
    public static CaptchaVO createCaptcha(StringRedisTemplate redisTemplate) {
        String code = generateCode();
        String captchaId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + captchaId, code, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(captchaId);
        vo.setImg(drawBase64(code));
        return vo;
    }

    /**
     * 校验验证码，无论对错都删除，防止暴力重试
     */
    public static boolean verifyCaptcha(StringRedisTemplate redisTemplate, String captchaId, String code) {
        if (StringUtils.isBlank(captchaId) || StringUtils.isBlank(code)) {
            return false;
        }
        String key = CAPTCHA_KEY_PREFIX + captchaId;
        String cached = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (cached == null) {
            return false;
        }
        return cached.equalsIgnoreCase(code.trim());
    }
}
