package com.evernight.base.entity.query;

import java.time.LocalDateTime;

/**
 * 论坛评论表参数
 */
public class ForumCommentQuery extends BaseParam {


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
	 * 评论内容(限500字)-模糊
	 */
	private String contentFuzzy;

	/**
	 * 创建时间
	 */
	private LocalDateTime createdTime;


	public void setCommentId(Integer commentId){
		this.commentId = commentId;
	}

	public Integer getCommentId(){
		return this.commentId;
	}

	public void setPostId(Integer postId){
		this.postId = postId;
	}

	public Integer getPostId(){
		return this.postId;
	}

	public void setUserId(Integer userId){
		this.userId = userId;
	}

	public Integer getUserId(){
		return this.userId;
	}

	public void setContent(String content){
		this.content = content;
	}

	public String getContent(){
		return this.content;
	}

	public void setContentFuzzy(String contentFuzzy){
		this.contentFuzzy = contentFuzzy;
	}

	public String getContentFuzzy(){
		return this.contentFuzzy;
	}

	public void setCreatedTime(LocalDateTime createdTime){
		this.createdTime = createdTime;
	}

	public LocalDateTime getCreatedTime(){
		return this.createdTime;
	}

}
