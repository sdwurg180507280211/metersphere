package io.metersphere.dto;

import lombok.Data;

@Data
public class CaptchaVO {
    /**
     * 验证码唯一标识，登录时需回传
     */
    private String captchaId;
    /**
     * 验证码图片（base64，含 data:image/png 前缀）
     */
    private String img;
}
