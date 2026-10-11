package com.evernight.base.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

import com.evernight.base.entity.enums.PageSizeEnum;
import com.evernight.base.entity.query.UserQuery;
import com.evernight.base.entity.po.User;
import com.evernight.base.entity.vo.PaginationResultVO;
import com.evernight.base.entity.query.SimplePage;
import com.evernight.base.mapper.UserMapper;
import com.evernight.base.service.UserService;
import com.evernight.base.tools.StringTools;


/**
 * 用户表 业务接口实现
 */
@Service("userService")
public class UserServiceImpl implements UserService {

	@Resource
	private UserMapper<User, UserQuery> userMapper;


	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<User> findListByParam(UserQuery param) {
		return this.userMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserQuery param) {
		return this.userMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<User> findListByPage(UserQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSizeEnum.SIZE_15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<User> list = this.findListByParam(param);
		PaginationResultVO<User> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(User bean) {
        bean.setPassword(StringTools.hashWithSHA256(bean.getPassword()));
        bean.setCreatedTime(LocalDateTime.now());
		return this.userMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<User> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<User> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(User bean, UserQuery param) {
		StringTools.checkParam(param);
		return this.userMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserQuery param) {
		StringTools.checkParam(param);
		return this.userMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public User getUserByUserId(Integer userId) {
		return this.userMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserByUserId(User bean, Integer userId) {
        if (bean.getPassword()!=null){
            bean.setPassword(StringTools.hashWithSHA256(bean.getPassword()));
        }
		return this.userMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserByUserId(Integer userId) {
		return this.userMapper.deleteByUserId(userId);
	}


}