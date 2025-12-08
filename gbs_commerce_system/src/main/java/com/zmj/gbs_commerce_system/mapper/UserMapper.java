package com.zmj.gbs_commerce_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmj.gbs_commerce_system.entity.Role;
import com.zmj.gbs_commerce_system.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT u.* FROM sys_user u WHERE u.username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Role> selectRolesByUserId(Long userId);
    @Select("SELECT DISTINCT m.perms \n" +
            "    FROM sys_menu m\n" +
            "    INNER JOIN sys_role_menu rm ON m.id = rm.menu_id\n" +
            "    INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id\n" +
            "    WHERE ur.user_id = #{userId}\n" +
            "    AND m.status = 0\n" +
            "    AND m.perms IS NOT NULL")
    List<String> selectPermsByUserId(Long userId);

    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(Long userId, Long roleId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRolesByUserId(Long userId);
}