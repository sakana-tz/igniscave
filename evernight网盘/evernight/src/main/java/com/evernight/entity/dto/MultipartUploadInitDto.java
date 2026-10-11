package com.evernight.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 大文件分片上传初始化响应DTO
 * <br>
 * 包含前端直传OSS所需的预签名URL列表
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MultipartUploadInitDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分片上传ID
     */
    private String uploadId;

    /**
     * 设定单个分片大小（字节）
     */
    private long chunkSize;

    /**
     * 分片总数
     */
    private int totalChunks;

    /**
     * 每个分片的预签名上传URL列表（PUT方式，前端直接上传到OSS）
     * <br>
     * 下标0对应partNumber=1，下标1对应partNumber=2，以此类推
     */
    private List<String> presignedUrls;
}
