package com.evernight.base.service;

import java.util.List;

import com.evernight.base.entity.query.ForumPostQuery;
import com.evernight.base.entity.po.ForumPost;
import com.evernight.base.entity.vo.PaginationResultVO;


/**
 * 论坛帖子表 业务接口
 */
public interface ForumPostService {

	/**
	 * 根据条件查询列表
	 */
	List<ForumPost> findListByParam(ForumPostQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(ForumPostQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<ForumPost> findListByPage(ForumPostQuery param);

	/**
	 * 新增
	 */
	Integer add(ForumPost bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<ForumPost> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<ForumPost> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(ForumPost bean,ForumPostQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(ForumPostQuery param);

	/**
	 * 根据PostId查询对象
	 */
	ForumPost getForumPostByPostId(Integer postId);


	/**
	 * 根据PostId修改
	 */
	Integer updateForumPostByPostId(ForumPost bean,Integer postId);


	/**
	 * 根据PostId删除
	 */
	Integer deleteForumPostByPostId(Integer postId);


}