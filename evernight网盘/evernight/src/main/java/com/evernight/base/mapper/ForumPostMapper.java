package com.evernight.base.mapper;

import com.evernight.entity.dto.HotPostDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 论坛帖子表 数据库操作接口
 */
public interface ForumPostMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据PostId更新
	 */
	 Integer updateByPostId(@Param("bean") T t,@Param("postId") Integer postId);


	/**
	 * 根据PostId删除
	 */
	 Integer deleteByPostId(@Param("postId") Integer postId);


	/**
	 * 根据PostId获取对象
	 */
	 T selectByPostId(@Param("postId") Integer postId);

	/**
	 * 根据PostId获取帖子详情（含评论数）
	 */
	 T selectDetailByPostId(@Param("postId") Integer postId);

	/**
	 * 查询浏览量前10的热门帖子（含发帖用户名和评论数）
	 */
	 List<HotPostDto> selectHotPosts();

	/**
	 * 查询帖子列表（含发帖用户名和评论数）
	 */
	 List<T> selectListWithUsername(@Param("query") P p);

}
