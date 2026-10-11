package com.evernight.base.entity.query;

import java.time.LocalDateTime;

/**
 * 文件表参数
 */
public class FileQuery extends BaseParam {


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
	 * 文件名-模糊
	 */
	private String fileNameFuzzy;

	/**
	 * 存储路径
	 */
	private String filePath;

	/**
	 * 存储路径-模糊
	 */
	private String filePathFuzzy;

	/**
	 * 文件类型
	 */
	private String fileType;

	/**
	 * 文件类型-模糊
	 */
	private String fileTypeFuzzy;

	/**
	 * 文件大小(字节)
	 */
	private Long fileSize;

	/**
	 * 文件哈希(用于秒传)
	 */
	private String fileHash;

	/**
	 * 文件哈希(用于秒传)-模糊
	 */
	private String fileHashFuzzy;

	/**
	 * 是否删除(0:正常,1:已删除)
	 */
	private Integer isDeleted;

	/**
	 * 创建时间
	 */
	private LocalDateTime createdTime;

	/**
	 * 更新时间
	 */
	private LocalDateTime updatedTime;


	public void setFileId(Long fileId){
		this.fileId = fileId;
	}

	public Long getFileId(){
		return this.fileId;
	}

	public void setUserId(Integer userId){
		this.userId = userId;
	}

	public Integer getUserId(){
		return this.userId;
	}

	public void setFileName(String fileName){
		this.fileName = fileName;
	}

	public String getFileName(){
		return this.fileName;
	}

	public void setFileNameFuzzy(String fileNameFuzzy){
		this.fileNameFuzzy = fileNameFuzzy;
	}

	public String getFileNameFuzzy(){
		return this.fileNameFuzzy;
	}

	public void setFilePath(String filePath){
		this.filePath = filePath;
	}

	public String getFilePath(){
		return this.filePath;
	}

	public void setFilePathFuzzy(String filePathFuzzy){
		this.filePathFuzzy = filePathFuzzy;
	}

	public String getFilePathFuzzy(){
		return this.filePathFuzzy;
	}

	public void setFileType(String fileType){
		this.fileType = fileType;
	}

	public String getFileType(){
		return this.fileType;
	}

	public void setFileTypeFuzzy(String fileTypeFuzzy){
		this.fileTypeFuzzy = fileTypeFuzzy;
	}

	public String getFileTypeFuzzy(){
		return this.fileTypeFuzzy;
	}

	public void setFileSize(Long fileSize){
		this.fileSize = fileSize;
	}

	public Long getFileSize(){
		return this.fileSize;
	}

	public void setFileHash(String fileHash){
		this.fileHash = fileHash;
	}

	public String getFileHash(){
		return this.fileHash;
	}

	public void setFileHashFuzzy(String fileHashFuzzy){
		this.fileHashFuzzy = fileHashFuzzy;
	}

	public String getFileHashFuzzy(){
		return this.fileHashFuzzy;
	}

	public void setIsDeleted(Integer isDeleted){
		this.isDeleted = isDeleted;
	}

	public Integer getIsDeleted(){
		return this.isDeleted;
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

}
