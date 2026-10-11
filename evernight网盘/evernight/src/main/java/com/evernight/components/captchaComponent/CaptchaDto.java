package com.evernight.components.captchaComponent;

import lombok.Getter;
import lombok.Setter;

/**
 * 验证码返回结果
 */
@Getter
@Setter
public class CaptchaDto {

    /**
     * 验证码Key
     */
    private String captchaKey;

    /**
     * 验证码图片编码(Base64)
     */
    private String captchaImage;

    /**
     * 验证码答案
     */
    private String captchaAnswer;

    /**
     * 构造函数-全参(仅后端传递)
     *
     * @param captchaKey      验证码Key
     * @param captchaImage    验证码图片编码(Base64)
     * @param captchaAnswer   验证码答案
     */
    public CaptchaDto(String captchaKey, String captchaImage, String captchaAnswer) {
        this.captchaKey = captchaKey;
        this.captchaImage = captchaImage;
        this.captchaAnswer = captchaAnswer;
    }

    /**
     * 构造函数-普通
     *
     * @param captchaKey      验证码Key
     * @param captchaImage    验证码图片编码(Base64)
     */
    public CaptchaDto(String captchaKey, String captchaImage) {
        this.captchaKey = captchaKey;
        this.captchaImage = captchaImage;
    }

    /**
     * 构造函数-邮箱
     */
    public CaptchaDto(String captchaKey) {
        this.captchaKey = captchaKey;
    }

}
