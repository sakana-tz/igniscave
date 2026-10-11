package com.evernight.entity.dto;

import com.evernight.base.entity.po.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)// 忽略未知属性
@Accessors(chain = true)
public class UserSessionDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户状态,0:管理员,1:禁用,2:普通用户
     */
    private Integer userStatus = 2;

    /**
     * 用户IP
     */
    private String userIp;

    /**
     * 过期时间（毫秒级绝对时间戳）
     */
    private Long expire;


    public UserSessionDto(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.userStatus = user.getUserStatus();
        this.userIp = null;
        this.expire = null;
    }

}
