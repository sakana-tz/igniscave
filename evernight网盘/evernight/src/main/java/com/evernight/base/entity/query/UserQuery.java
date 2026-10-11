package com.evernight.base.entity.query;

import java.time.LocalDateTime;

/**
 * 用户表参数
 */
public class UserQuery extends BaseParam {


	/**
	 * 用户ID
	 */
	private Integer userId;

	/**
	 * 用户名
	 */
	private String username;

	/**
	 * 用户名-模糊
	 */
	private String usernameFuzzy;

	/**
	 * 加密密码(bcrypt)
	 */
	private String password;

	/**
	 * 加密密码(bcrypt)-模糊
	 */
	private String passwordFuzzy;

	/**
	 * 邮箱
	 */
	private String email;

	/**
	 * 邮箱-模糊
	 */
	private String emailFuzzy;

	/**
	 * 注册时间
	 */
	private LocalDateTime createdTime;

	/**
	 * 最后更新时间
	 */
	private LocalDateTime updatedTime;

	/**
	 * 用户状态(0:管理员,1:禁用,2:普通用户)
	 */
	private Integer userStatus;

	/**
	 * 是否删除(0:正常,1:已删除)
	 */
	private Integer isDeleted;


	public void setUserId(Integer userId){
		this.userId = userId;
	}

	public Integer getUserId(){
		return this.userId;
	}

	public void setUsername(String username){
		this.username = username;
	}

	public String getUsername(){
		return this.username;
	}

	public void setUsernameFuzzy(String usernameFuzzy){
		this.usernameFuzzy = usernameFuzzy;
	}

	public String getUsernameFuzzy(){
		return this.usernameFuzzy;
	}

	public void setPassword(String password){
		this.password = password;
	}

	public String getPassword(){
		return this.password;
	}

	public void setPasswordFuzzy(String passwordFuzzy){
		this.passwordFuzzy = passwordFuzzy;
	}

	public String getPasswordFuzzy(){
		return this.passwordFuzzy;
	}

	public void setEmail(String email){
		this.email = email;
	}

	public String getEmail(){
		return this.email;
	}

	public void setEmailFuzzy(String emailFuzzy){
		this.emailFuzzy = emailFuzzy;
	}

	public String getEmailFuzzy(){
		return this.emailFuzzy;
	}

	public void setCreatedTime(LocalDateTime createdTime){
		this.createdTime = createdTime;
	}

	public LocalDateTime getCreatedTime(){
		return this.createdTime;
	}

	public void setUpdatedTime(LocalDateTime updatedTime){
		this.updatedTime = updatedTime;
	}

	public LocalDateTime getUpdatedTime(){
		return this.updatedTime;
	}

	public void setUserStatus(Integer userStatus){
		this.userStatus = userStatus;
	}

	public Integer getUserStatus(){
		return this.userStatus;
	}

	public void setIsDeleted(Integer isDeleted){
		this.isDeleted = isDeleted;
	}

	public Integer getIsDeleted(){
		return this.isDeleted;
	}

}
