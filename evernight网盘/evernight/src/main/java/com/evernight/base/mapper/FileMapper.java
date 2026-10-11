package com.evernight.base.mapper;

import com.evernight.base.entity.po.File;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 文件表 数据库操作接口
 */
public interface FileMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据FileId更新
     */
    Integer updateByFileId(@Param("bean") T t, @Param("fileId") Long fileId);


    /**
     * 根据FileId删除
     */
    Integer deleteByFileId(@Param("fileId") Long fileId);


    /**
     * 根据FileId获取对象
     */
    T selectByFileId(@Param("fileId") Long fileId);


    /**
     * 根据用户id获取文件列表
     */
    @Select("select * from file where user_id = #{userId}")
    List<File> selectListByUserId(P userId);

    /**
     * 获取存储总文件大小
     */
    @Select("select sum(file_size) from file")
    Long selectStorageTotalSize();

    /**
     * 获取文件总数
     */
    @Select("select count(*) from file where is_deleted = 0")
    Long selectFileTotalCount();
}
