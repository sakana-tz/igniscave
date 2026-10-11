package com.evernight.components.captchaComponent;

import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;

/**
 * 验证码服务
 */
@Slf4j
@Component
public class CaptchaComponent {

    private static final Font CAPTCHA_FONT = new Font("Arial", Font.PLAIN, 32);


    /**
     * 生成验证码图片
     *
     * @param length 验证码长度
     * @return CaptchaDto 验证码全参
     */
    public CaptchaDto generateCaptcha(int length) {
        Captcha captcha = new SpecCaptcha(130, 48, length);
        captcha.setCharType(Captcha.TYPE_DEFAULT);
        captcha.setFont(CAPTCHA_FONT);

        // 获取计算结果
        String answer = captcha.text();

        // 生成存储键标识
        String captchaKey = getRandomString(8);

        // 返回 key Base64格式的图片 answer
        return new CaptchaDto(
                captchaKey,
                captcha.toBase64(),
                answer
        );
    }


    /**
     * 获取随机字符串
     *
     * @param length-字符串长度
     * @return 随机字符串
     */
    public static String getRandomString(int length) {
        String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

}



