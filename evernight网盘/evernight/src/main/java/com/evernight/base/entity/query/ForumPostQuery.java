package com.evernight.base.entity.query;

import java.time.LocalDateTime;

/**
 * 论坛帖子表参数
 */
public class ForumPostQuery extends BaseParam {


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
	 * 标题-模糊
	 */
	private String titleFuzzy;

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
	private LocalDateTime updatedTime;


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

	public void setTitle(String title){
		this.title = title;
	}

	public String getTitle(){
		return this.title;
	}

	public void setTitleFuzzy(String titleFuzzy){
		this.titleFuzzy = titleFuzzy;
	}

	public String getTitleFuzzy(){
		return this.titleFuzzy;
	}

	public void setViewCount(Long viewCount){
		this.viewCount = viewCount;
	}

	public Long getViewCount(){
		return this.viewCount;
	}

	public void setPostStatus(Integer postStatus){
		this.postStatus = postStatus;
	}

	public Integer getPostStatus(){
		return this.postStatus;
	}

	public void setUpdatedTime(LocalDateTime updatedTime){
		this.updatedTime = updatedTime;
	}

	public LocalDateTime getUpdatedTime(){
		return this.updatedTime;
	}

}
