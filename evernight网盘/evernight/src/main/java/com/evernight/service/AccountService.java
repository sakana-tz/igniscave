package com.evernight.service;

import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.po.User;
import com.evernight.base.exception.BusinessException;
import com.evernight.base.mapper.UserMapper;
import com.evernight.base.tools.StringTools;
import com.evernight.components.jwtComponent.JwtComponent;
import com.evernight.components.redisComponent.RedisComponent;
import com.evernight.components.redisComponent.RedisProperties;
import com.evernight.entity.dto.UserSessionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AccountService {

    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private RedisProperties redisProperties;
    @Autowired
    private UserMapper<User, Object> userMapper;
    @Autowired
    private JwtComponent jwtComponent;


    /**
     * 邮箱验证或密码登录
     *
     * @param email    邮箱
     * @param password 密码
     * @param ip       登录ip
     */
    public String login(String email, String password, String ip) {
        // mysql查询
        User user = password != null
                ? userMapper.selectByEmailAndPassword(email, StringTools.hashWithSHA256(password))
                : userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_606);
        }
        if (user.getUserStatus() == 1) {
            throw new BusinessException(ResponseCodeEnum.CODE_604);
        }

        // redis存储session
        UserSessionDto userSessionDto = login2Redis(user, ip);

        // jwt生成token
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("userId", userSessionDto.getUserId());
        sessionMap.put("username", userSessionDto.getUsername());
        sessionMap.put("email", userSessionDto.getEmail());
        sessionMap.put("userStatus", userSessionDto.getUserStatus());

        return jwtComponent.generateToken(sessionMap, redisProperties.getUserSessionTimeout());
    }


    /**
     * 登出
     *
     * @param userId 用户id
     */
    public void logout(Integer userId) {
        redisComponent.delete(redisProperties.getUserSessionKey() + userId);
    }


    /**
     * 注册
     *
     * @param username 用户名
     * @param password 密码
     * @param email    邮箱
     */
    public void register(String username, String password, String email) {
        if (userMapper.selectByEmail(email) != null) {
            throw new BusinessException(ResponseCodeEnum.CODE_601);
        }
        userMapper.insert(new User()
                .setUsername(username)
                .setPassword(StringTools.hashWithSHA256(password))
                .setEmail(email)
        );
    }


    /**
     * 更新账户信息
     *
     * @param username       用户名
     * @param password       密码
     * @param userSessionDto 用户session
     */
    public void updateAccount(String username, String password, UserSessionDto userSessionDto) {
        User user = new User();
        // 仅设置有值的字段，未传入的字段保持原值不变
        if (username != null) {
            user.setUsername(username);
        }
        if (password != null && !password.isEmpty()) {
            user.setPassword(StringTools.hashWithSHA256(password));
        }
        userMapper.updateByUserId(user, userSessionDto.getUserId());
    }


    /**
     * 登录状态存储
     */
    private UserSessionDto login2Redis(User user, String ip) {
        UserSessionDto userSessionDto = new UserSessionDto(user);
        userSessionDto.setUserIp(ip).setExpire(System.currentTimeMillis() + (long) redisProperties.getUserSessionTimeout() * 1000);

        redisComponent.setEx(
                redisProperties.getUserSessionKey() + user.getUserId(),
                userSessionDto,
                redisProperties.getUserSessionTimeout(),
                TimeUnit.SECONDS
        );
        return userSessionDto;
    }


    /**
     * 禁用用户
     *
     * @param userId 用户id
     */
    public void disableUser(Integer userId) {
        userMapper.updateByUserId(new User().setUserStatus(1), userId);
    }



}
