package com.zmj.gbs_commerce_system.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * 基于Redis + 滑动窗口实现接口限流
 */
@Aspect
@Component
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(com.zmj.gbs_commerce_system.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        String key = buildKey(rateLimit);
        if (key == null) {
            return joinPoint.proceed();
        }

        long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, rateLimit.windowSeconds(), TimeUnit.SECONDS);
        }

        if (count > rateLimit.value()) {
            throw new RateLimitException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        StringBuilder keyBuilder = new StringBuilder(RATE_LIMIT_PREFIX);

        switch (rateLimit.limitType()) {
            case IP:
                keyBuilder.append("ip:").append(getClientIp(request));
                break;
            case USER:
                String userId = request.getHeader("X-User-Id");
                if (userId == null) {
                    userId = "anonymous";
                }
                keyBuilder.append("user:").append(userId);
                break;
            case IP_AND_USER:
                keyBuilder.append("ip:").append(getClientIp(request));
                String uid = request.getHeader("X-User-Id");
                if (uid != null) {
                    keyBuilder.append(":user:").append(uid);
                }
                break;
        }

        keyBuilder.append(":").append(request.getRequestURI());
        return keyBuilder.toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
