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
public class ShareFileToUserDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String shareKey;
    private Integer shareLimitCount;
    private Integer shareCount;
    private Long expire;
    private Integer masterId;
    private String fileName;
    private String fileUrl;

    public ShareFileToUserDto(ShareFileDto shareFileDto) {
        this.shareKey = shareFileDto.getShareKey();
        this.shareLimitCount = shareFileDto.getLimitShareCount();
        this.shareCount = shareFileDto.getShareCount();
        this.expire = shareFileDto.getExpire();
        this.masterId = shareFileDto.getMasterId();
        this.fileName = null;
        this.fileUrl = null;
    }

    /**
     * 获取剩余过期时间
     *
     * @return 剩余过期时间, 单位秒
     */
    public Long getRemainSeconds() {
        return expire;
    }
}