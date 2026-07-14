package com.zmj.gbs_commerce_system.service.impl;

import com.zmj.gbs_commerce_system.service.TokenBlacklistService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    public TokenBlacklistServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addToBlacklist(String token, long expirationMillis) {
        String key = BLACKLIST_PREFIX + token;
        long ttl = expirationMillis / 1000;
        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}