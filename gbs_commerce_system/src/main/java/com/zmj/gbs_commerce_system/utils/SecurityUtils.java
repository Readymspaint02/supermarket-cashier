package com.zmj.gbs_commerce_system.utils;

import com.zmj.gbs_commerce_system.entity.User;
import org.apache.shiro.subject.Subject;

public class SecurityUtils {

    public static Subject getSubject() {
        return org.apache.shiro.SecurityUtils.getSubject();
    }

    public static User getCurrentUser() {
        Subject subject = getSubject();
        if (subject != null && subject.getPrincipal() != null) {
            return (User) subject.getPrincipal();
        }
        return null;
    }

    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
}