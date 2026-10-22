package com.xrm.xrm_bs.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400}") // 默认24小时
    private Long expiration;

    // 从JWT中提取用户名
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // 从JWT中提取用户ID
    public Long getUserIdFromToken(String token) {
        final Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    // 从JWT中提取过期时间
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // 获取所有声明
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 检查JWT是否过期
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // 生成JWT - 存储用户名
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof com.xrm.xrm_bs.security.CustomUserDetails) {
            com.xrm.xrm_bs.security.CustomUserDetails customUserDetails =
                    (com.xrm.xrm_bs.security.CustomUserDetails) userDetails;
            claims.put("userId", customUserDetails.getId());
        }
        return createToken(claims, userDetails.getUsername());
    }

    // 创建JWT
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // 验证JWT
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            if (userDetails != null) {
                return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
            } else {
                // 如果没有提供 userDetails，只验证 token 是否有效且未过期
                return (username != null && !isTokenExpired(token));
            }
        } catch (Exception e) {
            return false;
        }
    }

    // 获取签名密钥
    private SecretKey getSigningKey() {
        // 如果密钥长度不够，使用 Keys 类生成一个足够长的密钥
        if (secret == null || secret.length() < 64) {
            // 如果配置的密钥太短，使用一个默认的足够长的密钥
            // 注意：在生产环境中，这应该是一个足够长的固定密钥
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }

        // 将字符串密钥转换为字节数组
        byte[] keyBytes = secret.getBytes();
        // 确保密钥至少64字节（512位）
        if (keyBytes.length < 64) {
            // 如果字节长度不够，使用 Keys 类
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }

        // 创建 SecretKey
        return Keys.hmacShaKeyFor(keyBytes);
    }
}