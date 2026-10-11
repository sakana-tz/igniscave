package com.evernight.components.aliOssComponent;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 文件模块Redis键配置类
 */
@Component
@Getter
public class FileRedisProperties {
    @Value("${spring.data.redis.consumer.base-project-name:}")
    private String baseProjectName;
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
}
