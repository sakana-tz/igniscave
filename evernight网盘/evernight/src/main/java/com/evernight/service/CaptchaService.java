package com.evernight.service;

import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.exception.BusinessException;
import com.evernight.components.captchaComponent.CaptchaComponent;
import com.evernight.components.captchaComponent.CaptchaDto;
import com.evernight.components.emailComponent.EmailComponent;
import com.evernight.components.redisComponent.RedisComponent;
import com.evernight.components.redisComponent.RedisProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {

    private static final Integer CAPTCHA_LENGTH = 4;
    private static final Integer EMAIL_CAPTCHA_LENGTH = 5;
    @Autowired
    private CaptchaComponent captchaComponent;
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private RedisProperties redisProperties;
    @Autowired
    private EmailComponent emailComponent;

    /**
     * 生成 base64图片 验证码
     *
     * @return 验证码
     */
    public CaptchaDto generateCaptcha() {

        // 调用组件生成验证码
        CaptchaDto captchaDto = captchaComponent.generateCaptcha(CAPTCHA_LENGTH);

        // redis存储验证码信息，60秒后过期
        redisComponent.setEx(
                redisProperties.getCaptchaKey() + captchaDto.getCaptchaKey(),
                captchaDto.getCaptchaAnswer(),
                redisProperties.getCaptchaTimeout(),
                TimeUnit.SECONDS
        );
        captchaDto.setCaptchaAnswer(null);
        return captchaDto;
    }

    /**
     * 生成邮箱验证码
     *
     * @param email 邮箱
     */
    public void generateEmailCaptcha(String email) {
        // 验证码已存在
        if (redisComponent.exists(redisProperties.getCaptchaKey() + email)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 生成验证码
        String emailCaptcha = CaptchaComponent.getRandomString(EMAIL_CAPTCHA_LENGTH);
        // 读取HTML邮件模板
        String emailTemplate = getEmailTemplate();
        // 替换模板中的验证码占位符
        String emailContent = emailTemplate.replace("{captcha_code}", emailCaptcha);
        // 发送HTML格式的邮件
        emailComponent.sendEmail(email, "【Evernight】邮箱验证码", emailContent);

        redisComponent.setEx(
                redisProperties.getCaptchaKey() + email,
                emailCaptcha,
                redisProperties.getCaptchaEmailTimeout(),
                TimeUnit.SECONDS
        );
    }

    /**
     * 校验验证码
     *
     * @param captchaKey    验证码key
     * @param captchaAnswer 验证码答案
     * @return true-验证成功 false-验证失败
     */
    public void checkCaptcha(String captchaKey, String captchaAnswer) {
        // 获取redis中的验证码答案
        String redisCaptchaAnswer = redisComponent.get(redisProperties.getCaptchaKey() + captchaKey).toString();

        // 删除redis中的验证码
        redisComponent.delete(redisProperties.getCaptchaKey() + captchaKey);

        // 验证码答案校验
        if (redisCaptchaAnswer == null || !redisCaptchaAnswer.equals(captchaAnswer)) {
            throw new BusinessException(ResponseCodeEnum.CODE_605);
        }
    }


    /**
     * 读取邮件模板文件
     *
     * @return HTML邮件模板内容
     */
    private String getEmailTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("static/email-template.html");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 如果读取模板失败，返回简单的文本格式
            return "您的验证码是：<strong>" + "{captcha_code}" + "</strong><br/>验证码将在5分钟后失效，请及时使用。";
        }
    }

}