package com.zmj.gbs_commerce_system.config;

/**
 * ============================================================
 * 【认证-01】JwtFilter - JWT Token验证过滤器
 * ============================================================
 * 
 * 文件作用：
 * 自定义Shiro过滤器，拦截HTTP请求并验证JWT Token，
 * 实现无状态认证（不依赖Session，适合前后端分离架构）。
 * 
 * 技术原理：
 * - 继承Shiro的AccessControlFilter，重写isAccessAllowed和onAccessDenied方法
 * - 从HTTP请求头获取Token：Authorization: Bearer {token}
 * - 使用JwtUtils验证Token签名和过期时间
 * - 解析Token获取用户信息，手动绑定到Shiro的Subject
 * 
 * 业务流程：
 * 1. 前端发起请求，携带JWT Token在请求头
 * 2. JwtFilter拦截请求，判断是否需要认证
 * 3. 需要认证的请求，解析Token验证有效性
 * 4. Token有效，查询数据库获取完整用户信息
 * 5. 手动绑定用户身份到Subject，完成认证
 * 6. 放行请求，后续可通过SecurityUtils.getSubject()获取当前用户
 * 
 * 面试考点：
 * - Q1：Shiro默认基于Session，如何改为JWT无状态认证？
 *   A1：自定义JwtFilter继承AccessControlFilter，在onAccessDenied中
 *       解析JWT Token，手动创建Subject并绑定到ThreadContext，
 *       绕过Session机制实现无状态认证。
 * 
 * - Q2：为什么要查数据库而不是直接用Token里的用户信息？
 *   A2：Token创建时用户状态、角色、权限可能已变化（如被禁用），
 *       需要查询数据库获取最新信息，保证权限控制准确性。
 * 
 * - Q3：Token过期怎么处理？
 *   A3：返回401状态码和错误信息，前端收到后跳转登录页，
 *       用户重新登录获取新Token。
 * 
 * - Q4：如何实现Token自动续期？
 *   A4：可以在Token快过期时（如剩余有效期<5分钟），
 *       在响应头返回新Token，前端保存替换旧Token。
 * 
 * - Q5：为什么OPTIONS请求要直接放行？
 *   A5：OPTIONS是CORS预检请求，用于跨域场景，
 *       浏览器会在实际请求前先发OPTIONS探测服务器是否允许跨域，
 *       此时请求头还没带Token，必须放行否则跨域失败。
 * 
 * 关联文件：
 * - config/ShiroConfig.java（配置过滤器链，将jwt过滤器注册到Shiro）
 * - utils/JwtUtils.java（Token生成、解析、验证）
 * - service/UserService.java（查询用户信息）
 * 
 * 参考文档：
 * - 梳理项目.md 3.1 认证授权模块
 * - 项目难点讲解.txt 难点一：Shiro + JWT 无状态认证
 * ============================================================
 */

import com.zmj.gbs_commerce_system.entity.User;
import com.zmj.gbs_commerce_system.service.TokenBlacklistService;
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
import java.util.Date;

public class JwtFilter extends AccessControlFilter {

    private UserService userService;
    private TokenBlacklistService tokenBlacklistService;
    private static final long RENEW_THRESHOLD = 5 * 60 * 1000L;

    @Override
    public void setServletContext(ServletContext servletContext) {
        super.setServletContext(servletContext);
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (context != null) {
            this.userService = context.getBean(UserService.class);
            this.tokenBlacklistService = context.getBean(TokenBlacklistService.class);
        }
    }

    // 【认证-01-放行判断】判断请求是否允许访问（不需要认证）
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        // 检查是否为OPTIONS请求（CORS预检请求）
        // 面试考点：为什么OPTIONS要放行？
        // 答：跨域场景下，浏览器会先发OPTIONS探测服务器是否允许跨域，
        //    此时请求头还没带Token，必须放行否则跨域失败
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
                return true; // OPTIONS请求直接放行
            }

            // 匿名访问接口（不需要Token验证）
            // 面试考点：哪些接口需要匿名访问？
            // 答：登录、注册、静态资源、API文档等
            String requestURI = httpRequest.getRequestURI();
            if (requestURI.endsWith("/auth/login") ||           // 登录接口
                    requestURI.endsWith("/auth/faceLogin") ||    // 人脸登录
                    requestURI.endsWith("/auth/register")||      // 注册接口
                    requestURI.contains("/uploads/")||           // 上传的静态资源
                    requestURI.contains("/swagger-ui") ||        // Swagger文档
                    requestURI.contains("/v3/api-docs") ||
                    requestURI.contains("/swagger-resources") ||
                    requestURI.contains("/webjars") ||
                    requestURI.endsWith("/doc.html") ||
                    requestURI.contains("/image/") ||            // 图片搜索接口
                    requestURI.contains("/public/")) {           // 公开接口
                return true; // 匿名接口直接放行
            }
        }

        return false; // 其他接口需要认证
    }

    // 【认证-01-核心逻辑】Token验证主流程
    // 当isAccessAllowed返回false时，调用此方法处理认证
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 设置响应字符编码和内容类型（支持中文）
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setContentType("application/json;charset=UTF-8");

        // 设置CORS头部，允许跨域访问
        // 面试考点：为什么要设置CORS头部？
        // 答：前后端分离架构，前端域名和后端API域名不同，
        //    需要CORS头部告诉浏览器允许跨域
        httpResponse.setHeader("Access-Control-Allow-Origin", httpRequest.getHeader("Origin"));
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");

        // 再次检查OPTIONS请求（双重保险）
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        // ========== 步骤1：从请求头获取Token ==========
        String token = httpRequest.getHeader("Authorization");

        // 校验Token格式：必须以"Bearer "开头
        // 面试考点：为什么是"Bearer "？
        // 答：OAuth 2.0标准规定，Bearer Token格式为：Authorization: Bearer {token}
        if (token == null || !token.startsWith("Bearer ")) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401状态码
            httpResponse.getWriter().write("{\"code\": 401, \"msg\": \"未提供有效的认证令牌\"}");
            return false; // 认证失败，拦截请求
        }

        // 去掉"Bearer "前缀，获取实际Token
        // "Bearer "长度为7个字符
        token = token.substring(7);

        try {
            if (!JwtUtils.validateToken(token)) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().write("{\"code\": 401, \"msg\": \"令牌无效或已过期\"}");
                return false;
            }

            if (tokenBlacklistService == null) {
                WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
                if (context != null) {
                    tokenBlacklistService = context.getBean(TokenBlacklistService.class);
                }
            }

            if (tokenBlacklistService != null && tokenBlacklistService.isBlacklisted(token)) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().write("{\"code\": 401, \"msg\": \"令牌已失效\"}");
                return false;
            }

            Claims claims = JwtUtils.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);

            if (userService == null) {
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

            Subject subject = getSubject(request, response);
            if (subject.isAuthenticated()) {
                User currentUser = (User) subject.getPrincipal();
                if (!currentUser.getId().equals(user.getId())) {
                    subject.logout();
                    bindSubject(user, request, response);
                }
            } else {
                bindSubject(user, request, response);
            }

            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            if (remainingTime < RENEW_THRESHOLD && remainingTime > 0) {
                String newToken = JwtUtils.generateToken(userId, username);
                httpResponse.setHeader("X-New-Token", newToken);
            }

            return true;

        } catch (AuthenticationException e) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"code\": 401, \"msg\": \"认证失败\"}");
            return false;
        } catch (Exception e) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"code\": 401, \"msg\": \"令牌验证失败\"}");
            return false;
        }
    }

    private void bindSubject(User user, ServletRequest request, ServletResponse response) {
        Subject.Builder builder = new Subject.Builder();
        org.apache.shiro.subject.PrincipalCollection principals =
                new org.apache.shiro.subject.SimplePrincipalCollection(user, getName());
        builder.principals(principals);
        builder.authenticated(true);
        Subject subject = builder.buildSubject();
        ThreadContext.bind(subject);
        request.setAttribute(DefaultSubjectContext.class.getName(), subject);
    }
}
