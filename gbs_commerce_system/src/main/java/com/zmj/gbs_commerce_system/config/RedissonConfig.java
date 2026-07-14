package com.zmj.gbs_commerce_system.config;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Autowired
    private RedissonClient redissonClient;

    public RedissonClient getRedissonClient() {
        return redissonClient;
    }
}
