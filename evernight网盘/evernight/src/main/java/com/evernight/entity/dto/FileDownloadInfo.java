package com.evernight.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件下载信息DTO
 * <br>
 * 包含前端直接下载所需的所有信息
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FileDownloadInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OSS预签名下载URL（前端直接使用）
     */
    private String url;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件MIME类型
     */
    private String contentType;

    /**
     * 推荐分片下载大小（字节），为null表示小文件无需分片
     */
    private Long chunkSize;

    /**
     * 断点续传阈值（字节），文件大小超过此值建议使用分片下载
     */
    private Long chunkThreshold;
}
