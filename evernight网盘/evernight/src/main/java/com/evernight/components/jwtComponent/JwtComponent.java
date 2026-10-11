package com.evernight.components.jwtComponent;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类 - 用于生成和解析JSON Web Tokens
 * 使用HS256算法进行签名和验证
 */
@Component
public class JwtComponent {


    @Autowired
    private JwtConfig jwtConfig;


    /**
     * 生成JWT Token
     *
     * @param claims     自定义声明
     * @param expiration 有效期（单位：秒）
     * @return JWT Token字符串
     */
    public String generateToken(Map<String, Object> claims, Integer expiration) {
        if (claims == null) {
            claims = new HashMap<>();
        }

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + (expiration != null ? expiration : jwtConfig.getExpiration()) * 1000))
                .signWith(jwtConfig.getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从Token中解析声明
     *
     * @param token JWT Token
     * @return 声明集合
     * @throws ExpiredJwtException      Token已过期
     * @throws SignatureException       Token签名验证失败
     * @throws IllegalArgumentException 非法Token
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtConfig.getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return 有效返回true
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Token已过期
            System.err.println("Token已过期: " + e.getMessage());
        } catch (SignatureException e) {
            // 签名验证失败
            System.err.println("签名验证失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // 非法Token
            System.err.println("非法Token: " + e.getMessage());
        } catch (Exception e) {
            // 其他异常
            System.err.println("jwt其他异常: " + e.getMessage());
        }
        return false;
    }
}
