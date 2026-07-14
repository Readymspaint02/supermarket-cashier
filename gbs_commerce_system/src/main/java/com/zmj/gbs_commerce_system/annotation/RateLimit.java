package com.zmj.gbs_commerce_system.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * 基于Redis实现，支持IP维度和用户维度限流
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    int value() default 10;

    int windowSeconds() default 60;

    LimitType limitType() default LimitType.IP;

    String message() default "请求过于频繁，请稍后再试";

    enum LimitType {
        IP,
        USER,
        IP_AND_USER
    }
}
