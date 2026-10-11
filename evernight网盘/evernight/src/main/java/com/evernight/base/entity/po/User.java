package com.evernight.base.entity.po;

import java.io.Serial;

import lombok.*;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 用户表
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class User implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L; // 避免序列化时出现版本不一致的问题


	/**
	 * 用户ID
	 */
	private Integer userId;

	/**
	 * 用户名
	 */
	private String username;

	/**
	 * 加密密码(bcrypt)
	 */
	private String password;

	/**
	 * 邮箱
	 */
	private String email;

	/**
	 * 注册时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdTime;

	/**
	 * 最后更新时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedTime;

	/**
	 * 用户状态(0:管理员,1:禁用,2:普通用户)
	 */
	private Integer userStatus;

	/**
	 * 是否删除(0:正常,1:已删除)
	 */
	private Integer isDeleted;

}