package com.zmj.gbs_commerce_system.config;

import com.zmj.gbs_commerce_system.entity.User;
import com.zmj.gbs_commerce_system.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
@Component
public class UserRealm extends AuthorizingRealm {

    @Autowired
    private UserService userService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof UsernamePasswordToken || token instanceof JwtToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        User user = (User) principals.getPrimaryPrincipal();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        // 添加用户角色
        Set<String> roles = new HashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> roles.add(role.getRoleKey()));
        }
        info.setRoles(roles);

        // 添加用户权限
        Set<String> permissions = new HashSet<>(userService.getPermsByUserId(user.getId()));
        info.setStringPermissions(permissions);
        System.out.println("用户角色：" + info.getRoles());
        System.out.println("用户权限：" + info.getStringPermissions());
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
            throws AuthenticationException {

        if (token instanceof JwtToken) {
            // JWT认证
            JwtToken jwtToken = (JwtToken) token;
            User user = (User) jwtToken.getPrincipal();
            // 对于JWT token，我们直接接受它作为有效凭证，不需要额外验证
            // 返回一个SimpleAuthenticationInfo，其中credentials设为token本身
            return new SimpleAuthenticationInfo(user, jwtToken.getToken(), getName());
        } else {
            // 用户名密码认证
            UsernamePasswordToken upToken = (UsernamePasswordToken) token;
            String username = upToken.getUsername();

            // 查询用户信息
            User user = userService.findByUsername(username);
            if (user == null) {
                throw new UnknownAccountException("用户不存在");
            }

            if (user.getStatus() == 1) {
                throw new LockedAccountException("账号已停用");
            }

            // 使用盐值进行密码验证
            return new SimpleAuthenticationInfo(
                    user,
                    user.getPassword(),
                    ByteSource.Util.bytes(user.getSalt()),
                    getName()
            );
        }
    }
}