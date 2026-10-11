package com.evernight.controller;


import com.evernight.base.controller.ABaseController;
import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.exception.BusinessException;
import com.evernight.components.jwtComponent.JwtComponent;
import com.evernight.components.redisComponent.RedisComponent;
import com.evernight.components.redisComponent.RedisProperties;
import com.evernight.entity.annotation.UserInfoValidation;
import com.evernight.entity.dto.UserSessionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 用户信息参数解析器
 * 实现HandlerMethodArgumentResolver接口，用于自动解析和校验带有@UserInfoValidation注解的参数
 */
@Component
public class UserInfoCheckController extends ABaseController implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger(UserInfoCheckController.class);

    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private RedisProperties redisProperties;
    @Autowired
    private JwtComponent jwtComponent;


    /**
     * 判断是否支持当前参数的解析
     * 只处理带有@UserInfoValidation注解的参数
     *
     * @param parameter 方法参数信息
     * @return 如果参数带有@UserInfoValidation注解则返回true，否则返回false
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(UserInfoValidation.class);
    }

    /**
     * 解析并校验参数值
     *
     * @param parameter     方法参数信息
     * @param mavContainer  ModelAndView容器
     * @param webRequest    当前请求信息
     * @param binderFactory 数据绑定工厂
     * @return 解析后的用户信息对象
     * @throws Exception 解析或校验过程中可能出现的异常
     */
    @Override
    public UserSessionDto resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {

        // 获取参数上的@UserInfoValidation注解
        UserInfoValidation validation = parameter.getParameterAnnotation(UserInfoValidation.class);
        if (validation == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_401);
        }

        // 从JWT中解析用户信息
        String token = getToken(USER_TOKEN_JWT);
        if (token == null || !jwtComponent.validateToken(token)) {
            throw new BusinessException(ResponseCodeEnum.CODE_401);
        }

        // 根据userId从Redis中获取UserSessionDto
        Integer userId = (Integer) jwtComponent.parseToken(token).get("userId");
        Object sessionObj = redisComponent.get(redisProperties.getUserSessionKey() + userId);
        if (sessionObj == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_401);
        }
        // 防御性检查：Redis序列化格式变更可能导致旧缓存反序列化为LinkedHashMap
        if (!(sessionObj instanceof UserSessionDto)) {
            redisComponent.delete(redisProperties.getUserSessionKey() + userId);
            throw new BusinessException(ResponseCodeEnum.CODE_401);
        }
        UserSessionDto userSessionDto = (UserSessionDto) sessionObj;

        // 检测用户IP变化（仅记录日志，不阻断请求）
        String currentIp = getIpAddr();
        if (validation.required() && userSessionDto.getUserIp() != null
                && !userSessionDto.getUserIp().equals(currentIp)) {
            log.warn("用户IP发生变化, userId={}, 登录IP={}, 当前IP={}",
                    userSessionDto.getUserId(), userSessionDto.getUserIp(), currentIp);
            // 更新Redis中存储的用户IP为当前IP
            userSessionDto.setUserIp(currentIp);
            long remainSeconds = (userSessionDto.getExpire() - System.currentTimeMillis()) / 1000;
            if (remainSeconds > 0) {
                redisComponent.setEx(
                        redisProperties.getUserSessionKey() + userSessionDto.getUserId(),
                        userSessionDto,
                        remainSeconds,
                        java.util.concurrent.TimeUnit.SECONDS
                );
            }
        }

        // 校验用户状态
        if (userSessionDto.getUserStatus() != null && userSessionDto.getUserStatus() == 1) {
            throw new BusinessException(ResponseCodeEnum.CODE_401);
        }

        return userSessionDto;
    }
}
