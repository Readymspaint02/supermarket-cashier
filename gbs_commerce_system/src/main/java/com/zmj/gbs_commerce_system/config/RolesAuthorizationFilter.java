package com.zmj.gbs_commerce_system.config;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.AuthorizationFilter;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RolesAuthorizationFilter extends AuthorizationFilter {

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws IOException {
        // 检查是否为OPTIONS请求，如果是则直接放行
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
                return true;
            }
        }

        Subject subject = getSubject(request, response);

        // mappedValue包含配置的角色数组
        String[] rolesArray = (String[]) mappedValue;

        // 如果没有配置角色，则允许访问
        if (rolesArray == null || rolesArray.length == 0) {
            return true;
        }

        // 检查用户是否拥有任一角色（OR逻辑）
        for (String role : rolesArray) {
            if (subject.hasRole(role)) {
                return true;
            }
        }

        // 用户不拥有任何指定角色
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 设置CORS头部
        httpResponse.setHeader("Access-Control-Allow-Origin", httpRequest.getHeader("Origin"));
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");

        // 设置响应字符编码和内容类型
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setContentType("application/json;charset=UTF-8");
        httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        httpResponse.getWriter().write("{\"code\": 403, \"msg\": \"您没有权限访问该资源\"}");
        return false;
    }
}