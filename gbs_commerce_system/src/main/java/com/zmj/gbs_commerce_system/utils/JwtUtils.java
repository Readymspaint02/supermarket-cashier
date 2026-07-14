package com.zmj.gbs_commerce_system.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token生成与解析工具
 */
@Component
public class JwtUtils {

    private static JwtPropertiesHolder propertiesHolder;

    public JwtUtils(com.zmj.gbs_commerce_system.config.JwtProperties jwtProperties) {
        propertiesHolder = new JwtPropertiesHolder(jwtProperties);
    }

    private static class JwtPropertiesHolder {
        private final com.zmj.gbs_commerce_system.config.JwtProperties properties;
        private final SecretKey secretKey;

        JwtPropertiesHolder(com.zmj.gbs_commerce_system.config.JwtProperties properties) {
            this.properties = properties;
            this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static JwtPropertiesHolder getHolder() {
        return propertiesHolder;
    }

    public static String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + getHolder().properties.getExpiration()))
                .signWith(getHolder().secretKey)
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getHolder().secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    public static String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    public static Date getExpiration(String token) {
        return parseToken(token).getExpiration();
    }

    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}