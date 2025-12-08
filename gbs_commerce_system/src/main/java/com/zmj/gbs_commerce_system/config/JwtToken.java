package com.zmj.gbs_commerce_system.config;

import com.zmj.gbs_commerce_system.entity.User;
import org.apache.shiro.authc.AuthenticationToken;

public class JwtToken implements AuthenticationToken {
    private User user;
    private String token;

    public JwtToken(User user, String token) {
        this.user = user;
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return user;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    public String getToken() {
        return token;
    }
}