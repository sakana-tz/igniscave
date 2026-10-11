package com.evernight.components.aliOssComponent;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class FileProperties {

    @Value("${file.aliOss.base-project-name}")
    private String baseProjectName;

    @Value("${file.aliOss.user-avatar}")
    private String avatarKey;

    @Value("${file.aliOss.user-file}")
    private String userFileKey;

    @Value("${file.aliOss.forum-post}")
    private String forumPostKey;

    @Value("${file.aliOss.forum-post-cover}")
    private String forumPostCoverKey;

    @Value("${file.aliOss.forum-post-image}")
    private String forumPostImageKey;

    @Value("${file.aliOss.share-file-limit-seconds}")
    private Integer shareLimitSeconds;

    @Value("${file.aliOss.share-file-limit-count}")
    private Integer shareLimitCount;

    @Value("${file.aliOss.max-file-size}")
    private Long maxFileSize;

    @Value("${file.aliOss.chunk-size}")
    private Long chunkSize;

    @Value("${file.aliOss.download-chunk-threshold}")
    private Long downloadChunkThreshold;

    @Value("${file.aliOss.download-chunk-size}")
    private Long downloadChunkSize;


}
