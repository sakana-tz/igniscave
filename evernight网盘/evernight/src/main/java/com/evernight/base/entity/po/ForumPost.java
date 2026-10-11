package com.evernight.base.entity.po;

import java.io.Serial;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.NoArgsConstructor;
import java.io.Serializable;


/**
 * 论坛帖子表
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ForumPost implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L; // 避免序列化时出现版本不一致的问题


	/**
	 * 帖子ID
	 */
	private Integer postId;

	/**
	 * 发帖用户ID
	 */
	private Integer userId;

	/**
	 * 标题
	 */
	private String title;

	/**
	 * 浏览数
	 */
	private Long viewCount;

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

	/**
	 * 评论数（非数据库字段，由SQL子查询填充）
	 */
	private Integer commentCount;

	/**
	 * 发帖用户名（非数据库字段，由联表查询填充）
	 */
	private String username;

}