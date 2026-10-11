package com.evernight.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 热门帖子DTO（含发帖用户名，用于热门帖子列表展示）
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class HotPostDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    private Integer postId;

    /**
     * 发帖用户ID
     */
    private Integer userId;

    /**
     * 发帖用户名
     */
    private String username;

    /**
     * 标题
     */
    private String title;

    /**
     * 浏览数
     */
    private Long viewCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 帖子状态(0:草稿,1:发布)
     */
    private Integer postStatus;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
