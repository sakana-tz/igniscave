package com.evernight.base.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 论坛评论表 数据库操作接口
 */
public interface ForumCommentMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据CommentId更新
	 */
	 Integer updateByCommentId(@Param("bean") T t,@Param("commentId") Integer commentId);


	/**
	 * 根据CommentId删除
	 */
	 Integer deleteByCommentId(@Param("commentId") Integer commentId);


	/**
	 * 根据CommentId获取对象
	 */
	 T selectByCommentId(@Param("commentId") Integer commentId);

	/**
	 * 查询评论列表（含评论用户名）
	 */
	 List<T> selectListWithUsername(@Param("query") P p);

}
