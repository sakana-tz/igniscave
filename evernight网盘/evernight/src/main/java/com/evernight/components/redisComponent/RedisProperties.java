package com.evernight.components.redisComponent;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Redis键配置类
 */
@Component
@Getter
public class RedisProperties {
    @Value("${spring.data.redis.consumer.base-project-name:}")
    private String baseProjectName;
    @Value("${spring.data.redis.consumer.system-setting.user-session.key:}")
    private String userSessionKey;
    @Value("${spring.data.redis.consumer.system-setting.user-session.timeout:}")
    private Integer userSessionTimeout;
    @Value("${spring.data.redis.consumer.system-setting.captcha.key:}")
    private String captchaKey;
    @Value("${spring.data.redis.consumer.system-setting.captcha.timeout:}")
    private Integer captchaTimeout;
    @Value("${spring.data.redis.consumer.system-setting.captcha.email-timeout:}")
    private Integer captchaEmailTimeout;
    @Value("${spring.data.redis.consumer.system-setting.user-traffic.key:}")
    private String userTrafficKey;
    @Value("${spring.data.redis.consumer.system-setting.user-traffic.timeout:}")
    private Integer userTrafficTimeout;
    @Value("${spring.data.redis.consumer.system-setting.user-traffic.max-traffic:}")
    private Long userTrafficMaxTraffic;
    @Value("${spring.data.redis.consumer.system-setting.user-fileList.key:}")
    private String userFileListKey;
    @Value("${spring.data.redis.consumer.system-setting.user-fileList.timeout:}")
    private Integer userFileListTimeout;
    @Value("${spring.data.redis.consumer.system-setting.file-share.key:}")
    private String fileShareKey;
    @Value("${spring.data.redis.consumer.system-setting.file-chunkUpload.key}")
    private String fileChunkUploadKey;
    @Value("${spring.data.redis.consumer.system-setting.file-chunkUpload.timeout:}")
    private Integer fileChunkUploadTimeout;
    @Value("${spring.data.redis.consumer.system-setting.file-forum-post.key}")
    private String fileForumPostKey;
    @Value("${spring.data.redis.consumer.system-setting.file-forum-post.timeout:}")
    private Integer fileForumPostTimeout;


}
