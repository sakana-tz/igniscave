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
 * 论坛评论表
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ForumComment implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L; // 避免序列化时出现版本不一致的问题


	/**
	 * 评论ID
	 */
	private Integer commentId;

	/**
	 * 帖子ID
	 */
	private Integer postId;

	/**
	 * 评论用户ID
	 */
	private Integer userId;

	/**
	 * 评论内容(限500字)
	 */
	private String content;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdTime;

	/**
	 * 评论用户名（非数据库字段，由联表查询填充）
	 */
	private String username;

}