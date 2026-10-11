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
 * 文件表
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class File implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L; // 避免序列化时出现版本不一致的问题


	/**
	 * 文件ID
	 */
	private Long fileId;

	/**
	 * 所属用户ID
	 */
	private Integer userId;

	/**
	 * 文件名
	 */
	private String fileName;

	/**
	 * 存储路径
	 */
	private String filePath;

	/**
	 * 文件类型
	 */
	private String fileType;

	/**
	 * 文件大小(字节)
	 */
	private Long fileSize;

	/**
	 * 文件哈希(用于秒传)
	 */
	private String fileHash;

	/**
	 * 是否删除(0:正常,1:已删除)
	 */
	private Integer isDeleted;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdTime;

	/**
	 * 更新时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedTime;

}