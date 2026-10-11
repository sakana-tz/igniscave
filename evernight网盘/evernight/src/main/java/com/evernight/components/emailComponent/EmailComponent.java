package com.evernight.components.emailComponent;

import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class EmailComponent {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailProperties emailProperties;

    private final ExecutorService emailExecutor = Executors.newCachedThreadPool();

    @PreDestroy
    public void shutdown() {
        emailExecutor.shutdown();
    }

    /**
     * 发送邮件(html格式)
     *
     * @param toEmail 接收邮箱地址
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendEmail(String toEmail, String subject, String content) {
        emailExecutor.submit(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);

                helper.setFrom(emailProperties.getFromEmail());
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(content, true); // true表示内容为HTML格式

                mailSender.send(message);
            } catch (MessagingException e) {
                // 处理邮件发送异常
                throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 发送邮件(html格式)
     *
     * @param toEmail 接收邮箱地址
     * @param content 邮件内容
     */
    public void sendEmail(String toEmail, String content) {
        sendEmail(toEmail, "【ignisCave】通知", content);
    }
}
