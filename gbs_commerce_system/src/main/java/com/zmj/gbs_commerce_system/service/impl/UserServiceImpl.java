package com.zmj.gbs_commerce_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmj.gbs_commerce_system.entity.User;
import com.zmj.gbs_commerce_system.mapper.UserMapper;
import com.zmj.gbs_commerce_system.service.UserService;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        if (user != null) {
            user.setRoles(userMapper.selectRolesByUserId(user.getId()));
        }
        return user;
    }

    @Override
    public List<String> getPermsByUserId(Long userId) {
        return userMapper.selectPermsByUserId(userId);
    }

    @Override
    @Transactional
    public boolean registerUser(User user) {
        // 生成盐值
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        user.setSalt(salt);

        // 密码加密
        String encryptedPassword = new SimpleHash(
                "MD5",
                user.getPassword(),
                ByteSource.Util.bytes(salt),
                2
        ).toHex();

        user.setPassword(encryptedPassword);
        user.setStatus(0);

        return userMapper.insert(user) > 0;
    }
    
    @Override
    public List<User> findAllUsers() {
        List<User> users = userMapper.selectList(null);
        // 为每个用户填充角色信息
        for (User user : users) {
            user.setRoles(userMapper.selectRolesByUserId(user.getId()));
        }
        return users;
    }
    
    @Override
    public IPage<User> findUsersWithPagination(Page<User> page, Map<String,Object> queryParams) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(queryParams.containsKey("username"), "username", queryParams.get("username"));
        queryWrapper.like(queryParams.containsKey("nickname"), "nickname", queryParams.get("nickname"));
        queryWrapper.like(queryParams.containsKey("email"), "email", queryParams.get("email"));
        queryWrapper.eq(queryParams.containsKey("status"), "status", queryParams.get("status"));
        IPage<User> userPage = userMapper.selectPage(page, queryWrapper);
        // 为每个用户填充角色信息
        for (User user : userPage.getRecords()) {
            user.setRoles(userMapper.selectRolesByUserId(user.getId()));
        }
        return userPage;
    }
    
    @Override
    public User findUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setRoles(userMapper.selectRolesByUserId(user.getId()));
        }
        return user;
    }
    
    @Override
    @Transactional
    public boolean updateUser(User user) {
        //删除此用户原有的角色
        userMapper.deleteUserRolesByUserId(user.getId());
        //获取新的角色id
        List<Long> roleIds = user.getRoles().stream().map(role -> role.getId()).toList();
        //新增新的角色
        roleIds.forEach(roleId -> userMapper.insertUserRole(user.getId(), roleId));
        return userMapper.updateById(user) > 0;
    }
    @Override
    public boolean updateUserById(User user) {
        user.setUpdateTime(new Date());
        return userMapper.updateById(user) > 0;
    }
    @Override
    @Transactional
    public boolean deleteUserById(Long id) {
        userMapper.deleteUserRolesByUserId(id);
        return userMapper.deleteById(id) > 0;
    }
    
    @Override
    @Transactional
    public boolean changePassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        // 生成新的盐值
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        user.setSalt(salt);
        
        // 密码加密
        String encryptedPassword = new SimpleHash(
                "MD5",
                newPassword,
                ByteSource.Util.bytes(salt),
                2
        ).toHex();
        
        user.setPassword(encryptedPassword);
        
        return userMapper.updateById(user) > 0;
    }
    //添加用户
    @Override
    @Transactional
    public boolean saveUser(User user) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());
        if (userMapper.selectOne(queryWrapper) != null) {
            return false;
        }
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        if (user.getStatus() == null){
            user.setStatus(0);
        }
        boolean isRegisted = registerUser(user);
        List<Long> roleIds = user.getRoles().stream().map(role -> role.getId()).toList();
        roleIds.forEach(roleId -> userMapper.insertUserRole(user.getId(), roleId));
        return isRegisted;
    }
    @Override
    @Transactional
    public boolean updateUserPassword(Long id, String oldPassword, String newPassword) {
        User user = findUserById(id);
        if (user != null) {
            // 验证旧密码是否正确
            String encryptedOldPassword = new SimpleHash(
                    "MD5",
                    oldPassword,
                    ByteSource.Util.bytes(user.getSalt()),
                    2
            ).toHex();

            if (user.getPassword().equals(encryptedOldPassword)) {
                // 生成新的盐值
                String newSalt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                user.setSalt(newSalt);

                // 使用新盐值加密新密码
                String encryptedNewPassword = new SimpleHash(
                        "MD5",
                        newPassword,
                        ByteSource.Util.bytes(newSalt),
                        2
                ).toHex();

                user.setPassword(encryptedNewPassword);
                user.setUpdateTime(new Date());
                return userMapper.updateById(user)>0;
            }
        }
        return false;
    }
}