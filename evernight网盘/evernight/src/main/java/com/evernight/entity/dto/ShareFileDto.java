package com.evernight.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)// 忽略未知属性
@Accessors(chain = true)
public class ShareFileDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String shareKey;
    private Integer limitShareCount;
    private Integer shareCount;
    private Long expire;
    private Long fileId;
    private Integer masterId;


    /**
     * 获取剩余过期时间
     *
     * @return 剩余过期时间, 单位秒
     */
    public Long getRemainSeconds() {
        return expire;
    }
}
