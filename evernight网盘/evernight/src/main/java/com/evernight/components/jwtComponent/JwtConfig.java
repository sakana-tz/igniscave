package com.evernight.components.jwtComponent;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

/**
 * JWT配置类
 */
@Configuration
public class JwtConfig {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 获取JWT密钥
     *
     * @return SecretKey
     */
    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * 获取JWT过期时间
     *
     * @return 剩余过期时间（秒）
     */
    public Integer getExpiration() {
        return jwtProperties.getExpiration();
    }
}
