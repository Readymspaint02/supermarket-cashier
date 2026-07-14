package com.zmj.gbs_commerce_system.annotation;

public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
