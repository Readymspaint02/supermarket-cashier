package com.zmj.gbs_commerce_system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.User;
import java.util.List;
import java.util.Map;

public interface UserService {
    User findByUsername(String username);
    List<String> getPermsByUserId(Long userId);
    boolean registerUser(User user);
    
    // 用户管理相关方法
    List<User> findAllUsers();
    IPage<User> findUsersWithPagination(Page<User> page, Map<String,Object> queryParams);
    User findUserById(Long id);
    boolean updateUser(User user);
    boolean updateUserById(User user);
    boolean deleteUserById(Long id);
    boolean changePassword(Long userId, String newPassword);
    boolean saveUser(User user);
    boolean updateUserPassword(Long id, String oldPassword, String newPassword);
}