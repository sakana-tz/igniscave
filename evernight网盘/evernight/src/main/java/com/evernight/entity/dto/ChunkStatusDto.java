package com.evernight.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)// 忽略未知属性
@Accessors(chain = true)
public class ChunkStatusDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件上传id
     */
    private String uploadId;
    /**
     * 文件名
     */
    private String filename;
    /**
     * 设定单个分片大小
     */
    private long stChunkSize;
    /**
     * 分片总数
     */
    private Integer totalChunks;
    /**
     * 上传状态列表, 0未上传，1已上传
     */
    private List<Integer> chunkStatusList;


}
