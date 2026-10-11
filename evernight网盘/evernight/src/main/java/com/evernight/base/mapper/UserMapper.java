package com.evernight.base.mapper;

import com.evernight.base.entity.po.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * 用户表 数据库操作接口
 */
public interface UserMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据UserId更新
     */
    Integer updateByUserId(@Param("bean") T t, @Param("userId") Integer userId);


    /**
     * 根据UserId删除
     */
    Integer deleteByUserId(@Param("userId") Integer userId);


    /**
     * 根据UserId获取对象
     */
    T selectByUserId(@Param("userId") Integer userId);


    /**
     * 根据Email更新
     */
    Integer updateByEmail(@Param("bean") T t, @Param("email") String email);


    /**
     * 根据Email删除
     */
    Integer deleteByEmail(@Param("email") String email);


    /**
     * 根据Email获取对象
     */
    T selectByEmail(@Param("email") String email);


    /**
     * 根据Email和密码查询
     */
    @Select("select * from user where email = #{email} and password = #{password}")
    User selectByEmailAndPassword(@Param("email") String email, @Param("password") String password);
}
