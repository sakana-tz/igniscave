package com.evernight.controller.admin;

import com.evernight.base.controller.ABaseController;
import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.po.User;

import com.evernight.base.entity.query.UserQuery;
import com.evernight.base.entity.vo.ResponseVO;
import com.evernight.base.exception.BusinessException;
import com.evernight.base.service.UserService;
import com.evernight.entity.annotation.UserInfoValidation;
import com.evernight.entity.dto.UserSessionDto;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 用户表 Controller
 */
@RestController
@RequestMapping("/admin/user")
public class AdminUserController extends ABaseController {

    @Resource
    private UserService userService;

    /**
     * 根据条件分页模糊查询
     */
    @RequestMapping("/loadDataList")
    public ResponseVO loadDataList(
            UserQuery query,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        return getSuccessResponseVO(userService.findListByPage(query));
    }

    /**
     * 新增
     */
    @RequestMapping("/add")
    public ResponseVO add(@RequestBody User bean, @UserInfoValidation UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        userService.add(bean);
        return getSuccessResponseVO(null);
    }


    /**
     * 根据UserId查询对象
     */
    @RequestMapping("/getUserByUserId")
    public ResponseVO getUserByUserId(Integer userId, @UserInfoValidation UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        return getSuccessResponseVO(userService.getUserByUserId(userId));
    }

    /**
     * 根据UserId修改对象
     */
    @RequestMapping("/updateUserByUserId")
    public ResponseVO updateUserByUserId(@RequestBody User bean, Integer userId, @UserInfoValidation UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        userService.updateUserByUserId(bean, userId);
        return getSuccessResponseVO(null);
    }

    /**
     * 根据UserId删除
     */
    @RequestMapping("/deleteUserByUserId")
    public ResponseVO deleteUserByUserId(Integer userId, @UserInfoValidation UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        userService.deleteUserByUserId(userId);
        return getSuccessResponseVO(null);
    }


}