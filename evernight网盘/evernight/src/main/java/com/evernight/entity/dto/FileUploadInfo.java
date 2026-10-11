package com.evernight.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传信息DTO
 * <br>
 * 包含前端直传OSS所需的预签名URL和存储路径
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FileUploadInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OSS预签名上传URL（PUT方式，前端直接使用）
     */
    private String uploadUrl;

    /**
     * 文件在OSS上的存储路径
     */
    private String ossKey;
}
