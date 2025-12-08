package com.zmj.gbs_commerce_system.config;

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
    @Bean
    public SecurityManager securityManager(UserRealm userRealm, HashedCredentialsMatcher matcher) {
        userRealm.setCredentialsMatcher(matcher);
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(userRealm);
        return securityManager;
    }

    /**
     * 开启Shiro注解支持
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager, JwtFilter jwtFilter) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);

        // 自定义过滤器
        Map<String, Filter> filters = new HashMap<>();
        filters.put("jwt", jwtFilter);
        filters.put("roles", rolesAuthorizationFilter());
        shiroFilterFactoryBean.setFilters(filters);

        // ========== 过滤器链定义 ==========
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();

        // ===== 1. 匿名访问（不需要登录）=====
        // 认证接口
        filterChainDefinitionMap.put("/auth/login", "anon");
        filterChainDefinitionMap.put("/auth/faceLogin", "anon");
        filterChainDefinitionMap.put("/auth/register", "anon");
        filterChainDefinitionMap.put("/logout", "logout");

        // Swagger接口文档
        filterChainDefinitionMap.put("/swagger-ui/**", "anon");
        filterChainDefinitionMap.put("/v3/api-docs/**", "anon");
        filterChainDefinitionMap.put("/swagger-resources/**", "anon");
        filterChainDefinitionMap.put("/webjars/**", "anon");
        filterChainDefinitionMap.put("/doc.html", "anon");

        // 静态资源（上传的图片）
        filterChainDefinitionMap.put("/uploads/**", "anon");

        // ===== 2. 需要JWT认证（登录后即可访问）=====
        // 这些接口只需要登录，不需要特定权限
        // 权限控制在Controller的@RequiresPermissions注解中

        // 系统管理模块（原有的）
        filterChainDefinitionMap.put("/system/**", "jwt");

        // 商品分类管理（查询不需要权限，增删改需要权限在Controller中控制）
        filterChainDefinitionMap.put("/product/category/**", "jwt");

        // 商品信息管理（查询不需要权限，增删改需要权限在Controller中控制）
        filterChainDefinitionMap.put("/product/**", "jwt");

        // 库存管理（查询不需要权限，入库出库需要权限在Controller中控制）
        filterChainDefinitionMap.put("/inventory/**", "jwt");

        // 订单管理（所有操作都需要权限在Controller中控制）
        filterChainDefinitionMap.put("/order/**", "jwt");

        // ===== 3. 其他所有请求都需要JWT认证 =====
        filterChainDefinitionMap.put("/**", "jwt");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

        return shiroFilterFactoryBean;
    }

    @Bean
    public JwtFilter jwtFilter() {
        return new JwtFilter();
    }

    @Bean
    public RolesAuthorizationFilter rolesAuthorizationFilter() {
        return new RolesAuthorizationFilter();
    }

    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        HashedCredentialsMatcher matcher = new HashedCredentialsMatcher();
        matcher.setHashAlgorithmName("MD5");
        matcher.setHashIterations(2);
        matcher.setStoredCredentialsHexEncoded(true);
        return matcher;
    }
}