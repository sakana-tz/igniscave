package com.evernight.base.controller;

import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.vo.ResponseVO;
import com.evernight.base.exception.BusinessException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class ABaseController {

    protected static final String STATUS_SUCCESS = "success";
    protected static final String STATUS_ERROR = "error";

    /**
     * 用户TokenJwt名称
     */
    protected static final String USER_TOKEN_JWT = "userTokenJwt";
    /**
     * IP探测请求头
     */
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "X-Real-IP"
    };

    protected <T> ResponseVO getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUS_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    protected <T> ResponseVO getBusinessErrorResponseVO(BusinessException e, T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUS_ERROR);
        if (e.getCode() == null) {
            vo.setCode(ResponseCodeEnum.CODE_600.getCode());
        } else {
            vo.setCode(e.getCode());
        }
        vo.setInfo(e.getMessage());
        vo.setData(t);
        return vo;
    }

    protected <T> ResponseVO getServerErrorResponseVO(T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUS_ERROR);
        vo.setCode(ResponseCodeEnum.CODE_500.getCode());
        vo.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        vo.setData(t);
        return vo;
    }

    /**
     * 获取客户端真实IP（自动处理代理场景）
     */
    protected String getIpAddr() {
        HttpServletRequest request = ((ServletRequestAttributes)
                Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                return processMultiIp(ip);
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 验证IP地址是否有效
     */
    private boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip.trim());
    }

    /**
     * 处理多个IP地址的情况，取第一个非内网IP
     */
    private String processMultiIp(String ip) {
        int idx = ip.indexOf(",");
        if (idx > 0) {
            // 分割多个IP地址，取第一个有效的
            String[] ips = ip.split(",");
            for (String individualIp : ips) {
                String trimmedIp = individualIp.trim();
                if (!isInnerIP(trimmedIp) && isValidIp(trimmedIp)) {
                    return trimmedIp;
                }
            }
        }
        return ip.split(",")[0].trim(); // 返回第一个IP
    }

    /**
     * 判断是否为内网IP
     */
    private boolean isInnerIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        // 判断是否为内网IP地址
        return ip.startsWith("10.") ||
                ip.startsWith("172.") &&
                        Integer.parseInt(ip.split("\\.")[1]) >= 16 &&
                        Integer.parseInt(ip.split("\\.")[1]) <= 31 ||
                ip.startsWith("192.168.");
    }

    /**
     * 保存Token到Cookie
     *
     * @param tokenName token名称
     * @param tokenValue token值
     * @param timeout Token有效期（单位：秒）
     */
    protected void saveToken2Cookie(String tokenName, String tokenValue, Integer timeout) {
        HttpServletResponse response = ((ServletRequestAttributes)
                Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse();

        Cookie cookie = new Cookie(tokenName, tokenValue);
        cookie.setMaxAge(timeout);
        cookie.setPath("/");
        // 设置HttpOnly防止XSS攻击
        cookie.setHttpOnly(true);
        // 设置Secure标志（如果使用HTTPS）
        cookie.setSecure(false); // 根据实际环境设置
        response.addCookie(cookie);
    }

    /**
     * 获取Token参数
     *
     * @param token Token名称
     * @return Token值
     */
    protected String getToken(String token) {
        HttpServletRequest request = ((ServletRequestAttributes)
                Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        return Optional.ofNullable(request.getCookies())
                // 优先从 Cookie 获取 Token
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(c -> token.equals(c.getName()))
                        .findFirst()
                        .map(Cookie::getValue))
                // 其次从 Header 获取 Token
                .orElseGet(() -> {
                    // 尝试从标准Authorization头获取Bearer token
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        return authHeader.substring(7); // 移除 "Bearer " 前缀
                    }
                    // 如果没有找到Authorization头，则尝试使用token名称作为header名称查找
                    return request.getHeader(token);
                });
    }

}
