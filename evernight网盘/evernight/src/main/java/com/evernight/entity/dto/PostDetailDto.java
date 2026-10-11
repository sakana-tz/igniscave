package com.evernight.entity.dto;

import com.evernight.base.entity.po.ForumPost;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帖子详情DTO（包含MD内容，用于前端无感显示和编辑）
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PostDetailDto implements Serializable {

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
    private LocalDateTime updatedTime;

    /**
     * 帖子MD内容（从OSS获取，前端可直接渲染和编辑）
     */
    private String content;

    /**
     * 从ForumPost构造PostDetailDto
     */
    public PostDetailDto(ForumPost post) {
        this.postId = post.getPostId();
        this.userId = post.getUserId();
        this.username = post.getUsername();
        this.title = post.getTitle();
        this.viewCount = post.getViewCount();
        this.commentCount = post.getCommentCount();
        this.postStatus = post.getPostStatus();
        this.updatedTime = post.getUpdatedTime();
    }
}
