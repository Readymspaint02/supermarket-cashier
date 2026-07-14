package com.zmj.gbs_commerce_system.config;

/**
 * ============================================================
 * 【认证-02】ShiroConfig - Shiro安全框架配置
 * ============================================================
 * 
 * 文件作用：
 * 配置Shiro安全框架，包括SecurityManager、过滤器链、密码匹配器等。
 * 核心功能：定义哪些URL需要认证，哪些可以匿名访问。
 * 
 * 技术原理：
 * - SecurityManager：Shiro的核心安全管理器，管理所有Subject
 * - ShiroFilterFactoryBean：过滤器工厂，定义URL拦截规则
 * - HashedCredentialsMatcher：密码匹配器，验证用户密码
 * - LinkedHashMap：过滤器链按顺序匹配，先匹配的生效
 * 
 * 业务流程：
 * 1. 用户访问某个URL
 * 2. ShiroFilter拦截请求，按过滤器链顺序匹配
 * 3. 匹配到 anon → 直接放行（不需要登录）
 * 4. 匹配到 jwt → 调用JwtFilter验证Token
 * 5. Token有效 → 放行；Token无效 → 返回401
 * 
 * 面试考点：
 * - Q1：Shiro的核心组件有哪些？
 *   A1：Subject（当前用户）、SecurityManager（安全管理器）、
 *       Realm（数据源）、SessionManager（会话管理）、
 *       CacheManager（缓存管理）、Realms（认证和授权）。
 * 
 * - Q2：过滤器链为什么用LinkedHashMap？
 *   A2：LinkedHashMap保持插入顺序，Shiro按顺序匹配过滤器，
 *       先匹配的生效。如果用HashMap，顺序不确定，可能导致误拦截。
 *       例如：/** 放在前面会拦截所有请求，包括 /auth/login。
 * 
 * - Q3：anon、jwt、logout这些过滤器是什么意思？
 *   A3：- anon：匿名访问，不需要登录
 *       - jwt：自定义过滤器，验证JWT Token
 *       - logout：登出过滤器，清除Session
 * 
 * - Q4：密码加密方式是什么？为什么迭代2次？
 *   A4：MD5加密，迭代2次是为了增加安全性，防止暴力破解。
 *       MD5本身不安全，迭代多次可以增加破解难度。
 * 
 * - Q5：如何实现权限控制？
 *   A5：两种方式：
 *       1. 过滤器链：filterChain.put("/admin/**", "roles[admin]")
 *       2. 注解方式：@RequiresPermissions("user:create")
 *       我们项目用注解方式，更灵活。
 * 
 * 关联文件：
 * - config/JwtFilter.java（JWT Token验证过滤器）
 * - config/UserRealm.java（用户认证和授权Realm）
 * 
 * 参考文档：
 * - 梳理项目.md 3.1 认证授权模块
 * ============================================================
 */

import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {

    // 【认证-02-核心组件】SecurityManager - Shiro安全管理器
    // 面试考点：SecurityManager的作用？
    // 答：Shiro的核心，管理所有Subject、Realm、Session等组件
    @Bean
    public SecurityManager securityManager(UserRealm userRealm, HashedCredentialsMatcher matcher) {
        // 设置密码匹配器（用于验证用户密码）
        userRealm.setCredentialsMatcher(matcher);
        
        // 创建Web安全管理器
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(userRealm); // 设置Realm（数据源）
        
        return securityManager;
    }

    // 【认证-02-注解支持】开启Shiro注解支持
    // 作用：让@RequiresPermissions、@RequiresRoles等注解生效
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

    // 【认证-02-过滤器链】ShiroFilterFactoryBean - URL拦截规则配置
    // 面试考点：过滤器链的作用？
    // 答：定义哪些URL需要认证，哪些可以匿名访问
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager, JwtFilter jwtFilter) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);

        // 注册自定义过滤器
        // jwt：JWT Token验证过滤器（自定义）
        // roles：角色过滤器（用于权限控制）
        Map<String, Filter> filters = new HashMap<>();
        filters.put("jwt", jwtFilter);                    // 注册JWT过滤器
        filters.put("roles", rolesAuthorizationFilter()); // 注册角色过滤器
        shiroFilterFactoryBean.setFilters(filters);

        // ========== 过滤器链定义（核心配置）==========
        // 面试考点：为什么用LinkedHashMap？
        // 答：LinkedHashMap保持插入顺序，Shiro按顺序匹配，先匹配的生效
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();

        // ===== 1. 匿名访问（不需要登录）=====
        // 认证接口（登录、注册）
        filterChainDefinitionMap.put("/auth/login", "anon");      // 登录接口
        filterChainDefinitionMap.put("/auth/faceLogin", "anon");  // 人脸登录
        filterChainDefinitionMap.put("/auth/register", "anon");   // 注册接口
        filterChainDefinitionMap.put("/logout", "logout");        // 登出接口

        // 人脸识别接口
        filterChainDefinitionMap.put("/face/config", "anon");     // 配置检查接口
        filterChainDefinitionMap.put("/face/check", "anon");      // 人脸注册状态检查
        filterChainDefinitionMap.put("/face/register", "jwt");    // 人脸注册需要登录
        filterChainDefinitionMap.put("/face/verify", "jwt");      // 人脸验证需要登录

        // Swagger接口文档（不需要认证）
        filterChainDefinitionMap.put("/swagger-ui/**", "anon");
        filterChainDefinitionMap.put("/v3/api-docs/**", "anon");
        filterChainDefinitionMap.put("/swagger-resources/**", "anon");
        filterChainDefinitionMap.put("/webjars/**", "anon");
        filterChainDefinitionMap.put("/doc.html", "anon");

        // 静态资源（上传的图片、文件）
        filterChainDefinitionMap.put("/uploads/**", "anon");

        // 公开接口（电子小票查询，无需登录）
        filterChainDefinitionMap.put("/public/order/**", "anon");

        // ===== 2. 需要JWT认证（登录后即可访问）=====
        // 这些接口只需要登录，不需要特定权限
        // 权限控制在Controller的@RequiresPermissions注解中
        // 面试考点：为什么不在过滤器链做权限控制？
        // 答：注解方式更灵活，可以精确到方法级别

        // 系统管理模块
        filterChainDefinitionMap.put("/system/**", "jwt");

        // 商品分类管理（查询不需要权限，增删改需要权限在Controller中控制）
        filterChainDefinitionMap.put("/product/category/**", "jwt");

        // 商品信息管理
        filterChainDefinitionMap.put("/product/**", "jwt");

        // 库存管理
        filterChainDefinitionMap.put("/inventory/**", "jwt");

        // 订单管理
        filterChainDefinitionMap.put("/order/**", "jwt");

        // ===== 3. 其他所有请求都需要JWT认证 =====
        // 面试考点：/** 放在最后，为什么？
        // 答：如果放在前面，会拦截所有请求，包括前面的匿名接口
        filterChainDefinitionMap.put("/**", "jwt");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

        return shiroFilterFactoryBean;
    }

    // 【认证-02-过滤器Bean】创建JwtFilter实例
    @Bean
    public JwtFilter jwtFilter() {
        return new JwtFilter();
    }

    // 【认证-02-过滤器Bean】创建RolesAuthorizationFilter实例
    @Bean
    public RolesAuthorizationFilter rolesAuthorizationFilter() {
        return new RolesAuthorizationFilter();
    }

    // 【认证-02-密码匹配器】HashedCredentialsMatcher - 密码加密验证
    // 面试考点：为什么用MD5迭代2次？
    // 答：增加安全性，防止暴力破解。MD5本身不安全，迭代多次增加难度
    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        HashedCredentialsMatcher matcher = new HashedCredentialsMatcher();
        matcher.setHashAlgorithmName("MD5");         // 加密算法：MD5
        matcher.setHashIterations(2);                // 迭代次数：2次
        matcher.setStoredCredentialsHexEncoded(true); // 密码存储格式：十六进制
        return matcher;
    }
}