package com.evernight.base.service;

import com.evernight.base.entity.po.File;
import com.evernight.base.entity.query.FileQuery;
import com.evernight.base.entity.vo.PaginationResultVO;
import com.evernight.entity.dto.UserSessionDto;

import java.util.List;


/**
 * 文件表 业务接口
 */
public interface FileService {

    /**
     * 查询存储总文件大小
     */
    long getStorageTotalSize(UserSessionDto userSessionDto);

    /**
     * 查询文件总数
     */
    long getFileTotalCount(UserSessionDto userSessionDto);

    /**
     * 根据条件查询列表
     */
    List<File> findListByParam(FileQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(FileQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<File> findListByPage(FileQuery param);

    /**
     * 新增
     */
    Integer add(File bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<File> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<File> listBean);

    /**
     * 多条件更新
     */
    Integer updateByParam(File bean, FileQuery param);

    /**
     * 多条件删除
     */
    Integer deleteByParam(FileQuery param);

    /**
     * 根据FileId查询对象
     */
    File getFileByFileId(Long fileId);


    /**
     * 根据FileId修改
     */
    Integer updateFileByFileId(File bean, Long fileId);


    /**
     * 根据FileId删除
     */
    Integer deleteFileByFileId(Long fileId);


}