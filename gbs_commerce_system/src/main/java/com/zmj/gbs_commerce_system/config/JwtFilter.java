package com.zmj.gbs_commerce_system.config;

import com.zmj.gbs_commerce_system.entity.User;
import com.zmj.gbs_commerce_system.service.UserService;
import com.zmj.gbs_commerce_system.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtFilter extends AccessControlFilter {

    private UserService userService;

    @Override
    public void setServletContext(ServletContext servletContext) {
        super.setServletContext(servletContext);
        // 从ServletContext获取WebApplicationContext，然后获取UserService bean
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (context != null) {
            this.userService = context.getBean(UserService.class);
        }
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        // 检查是否为OPTIONS请求，如果是则直接放行
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
                return true;
            }

            // 对于登录、注册，允许匿名访问
            String requestURI = httpRequest.getRequestURI();
            if (requestURI.endsWith("/auth/login") ||
                    requestURI.endsWith("/auth/faceLogin") ||
                    requestURI.endsWith("/auth/register")||
                    requestURI.contains("/api/uploads/")||
                    requestURI.contains("/api/swagger-ui") ||
                    requestURI.contains("/api/v3/api-docs") ||
                    requestURI.contains("/api/swagger-resources") ||
                    requestURI.contains("/api/webjars") ||
                    requestURI.endsWith("/api/doc.html")) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 设置响应字符编码和内容类型
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setContentType("application/json;charset=UTF-8");

        // 设置CORS头部
        httpResponse.setHeader("Access-Control-Allow-Origin", httpRequest.getHeader("Origin"));
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");

        // 检查是否为OPTIONS请求，如果是则直接返回
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        String token = httpRequest.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"code\": 401, \"msg\": \"未提供有效的认证令牌\"}");
            return false;
        }

        token = token.substring(7); // 去掉 "Bearer " 前缀

        try {
            // 验证JWT token
            if (!JwtUtils.validateToken(token)) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().write("{\"code\": 401, \"msg\": \"令牌无效或已过期\"}");
                return false;
            }

            // 从token中获取用户信息
            Claims claims = JwtUtils.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);

            // 检查用户是否存在
            if (userService == null) {
                // 手动从application context获取userService
                WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
                if (context != null) {
                    userService = context.getBean(UserService.class);
                }
            }

            if (userService == null) {
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                httpResponse.getWriter().write("{\"code\": 500, \"msg\": \"无法获取UserService\"}");
                return false;
            }

            User user = userService.findByUsername(username);
            if (user == null) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().write("{\"code\": 401, \"msg\": \"用户不存在\"}");
                return false;
            }

            // 直接设置用户信息到当前Subject，避免触发完整的认证流程
            Subject subject = getSubject(request, response);
            if (subject.isAuthenticated()) {
                // 如果已经认证，检查是否是同一个用户
                User currentUser = (User) subject.getPrincipal();
                if (!currentUser.getId().equals(user.getId())) {
                    subject.logout();
                    // 创建新的已认证Subject
                    bindSubject(user, request, response);
                }
            } else {
                // 创建新的已认证Subject
                bindSubject(user, request, response);
            }

            return true;
        } catch (AuthenticationException e) {
            // 处理认证异常
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"code\": 401, \"msg\": \"认证失败\"}");
            return false;
        } catch (Exception e) {
            // 处理其他异常
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"code\": 401, \"msg\": \"令牌验证失败\"}");
            return false;
        }
    }

    /**
     * 绑定用户信息到Subject
     */
    private void bindSubject(User user, ServletRequest request, ServletResponse response) {
        Subject.Builder builder = new Subject.Builder();
        // 创建PrincipalCollection
        org.apache.shiro.subject.PrincipalCollection principals =
                new org.apache.shiro.subject.SimplePrincipalCollection(user, getName());
        builder.principals(principals);
        builder.authenticated(true);
        Subject subject = builder.buildSubject();

        // 将Subject绑定到当前线程
        ThreadContext.bind(subject);

        // 将Subject绑定到session（如果存在）
        request.setAttribute(DefaultSubjectContext.class.getName(), subject);
    }
}
