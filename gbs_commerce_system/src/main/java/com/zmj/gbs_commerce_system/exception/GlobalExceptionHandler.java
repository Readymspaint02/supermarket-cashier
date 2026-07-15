package com.zmj.gbs_commerce_system.exception;

import com.zmj.gbs_commerce_system.annotation.RateLimitException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理Shiro权限认证异常
     */
    @ExceptionHandler({UnauthorizedException.class, AuthorizationException.class})
    public Map<String, Object> handleAuthorizationException(AuthorizationException e) {
        logger.warn("用户权限不足: {}", e.getMessage());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("msg", "您没有权限访问该资源");
        return result;
    }

    /**
     * 处理Shiro未认证异常
     */
    @ExceptionHandler(UnauthenticatedException.class)
    public Map<String, Object> handleUnauthenticatedException(UnauthenticatedException e) {
        logger.warn("用户未认证: {}", e.getMessage());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("msg", "用户未认证，请先登录");
        return result;
    }

    /**
     * 处理限流异常
     */
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> handleRateLimitException(RateLimitException e) {
        logger.warn("触发限流: {}", e.getMessage());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 429);
        result.put("msg", e.getMessage());
        return result;
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(Exception e) {
        logger.error("系统异常: ", e);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("msg", "系统内部错误: " + e.getMessage());
        return result;
    }
}